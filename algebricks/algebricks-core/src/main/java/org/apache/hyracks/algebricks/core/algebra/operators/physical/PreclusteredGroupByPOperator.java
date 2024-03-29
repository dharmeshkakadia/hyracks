/*
 * Copyright 2009-2013 by The Regents of the University of California
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * you may obtain a copy of the License from
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hyracks.algebricks.core.algebra.operators.physical;

import java.util.List;

import org.apache.commons.lang3.mutable.Mutable;

import org.apache.hyracks.algebricks.common.exceptions.AlgebricksException;
import org.apache.hyracks.algebricks.common.utils.Pair;
import org.apache.hyracks.algebricks.core.algebra.base.IHyracksJobBuilder;
import org.apache.hyracks.algebricks.core.algebra.base.ILogicalExpression;
import org.apache.hyracks.algebricks.core.algebra.base.ILogicalOperator;
import org.apache.hyracks.algebricks.core.algebra.base.LogicalExpressionTag;
import org.apache.hyracks.algebricks.core.algebra.base.LogicalOperatorTag;
import org.apache.hyracks.algebricks.core.algebra.base.LogicalVariable;
import org.apache.hyracks.algebricks.core.algebra.base.PhysicalOperatorTag;
import org.apache.hyracks.algebricks.core.algebra.expressions.VariableReferenceExpression;
import org.apache.hyracks.algebricks.core.algebra.operators.logical.AbstractLogicalOperator;
import org.apache.hyracks.algebricks.core.algebra.operators.logical.GroupByOperator;
import org.apache.hyracks.algebricks.core.algebra.operators.logical.IOperatorSchema;
import org.apache.hyracks.algebricks.core.jobgen.impl.JobGenContext;
import org.apache.hyracks.algebricks.core.jobgen.impl.JobGenHelper;
import org.apache.hyracks.algebricks.runtime.base.AlgebricksPipeline;
import org.apache.hyracks.algebricks.runtime.operators.aggreg.NestedPlansAccumulatingAggregatorFactory;
import org.apache.hyracks.algebricks.runtime.operators.aggreg.NestedPlansRunningAggregatorFactory;
import org.apache.hyracks.api.dataflow.value.IBinaryComparatorFactory;
import org.apache.hyracks.api.dataflow.value.RecordDescriptor;
import org.apache.hyracks.api.job.IOperatorDescriptorRegistry;
import org.apache.hyracks.dataflow.std.group.IAggregatorDescriptorFactory;
import org.apache.hyracks.dataflow.std.group.preclustered.PreclusteredGroupOperatorDescriptor;

public class PreclusteredGroupByPOperator extends AbstractPreclusteredGroupByPOperator {

    public PreclusteredGroupByPOperator(List<LogicalVariable> columnList) {
        super(columnList);
    }

    @Override
    public PhysicalOperatorTag getOperatorTag() {
        return PhysicalOperatorTag.PRE_CLUSTERED_GROUP_BY;
    }

    @Override
    public boolean isMicroOperator() {
        return false;
    }

    @Override
    public void contributeRuntimeOperator(IHyracksJobBuilder builder, JobGenContext context, ILogicalOperator op,
            IOperatorSchema opSchema, IOperatorSchema[] inputSchemas, IOperatorSchema outerPlanSchema)
            throws AlgebricksException {
        int keys[] = JobGenHelper.variablesToFieldIndexes(columnList, inputSchemas[0]);
        GroupByOperator gby = (GroupByOperator) op;
        int numFds = gby.getDecorList().size();
        int fdColumns[] = new int[numFds];
        int j = 0;
        for (Pair<LogicalVariable, Mutable<ILogicalExpression>> p : gby.getDecorList()) {
            ILogicalExpression expr = p.second.getValue();
            if (expr.getExpressionTag() != LogicalExpressionTag.VARIABLE) {
                throw new AlgebricksException("pre-sorted group-by expects variable references.");
            }
            VariableReferenceExpression v = (VariableReferenceExpression) expr;
            LogicalVariable decor = v.getVariableReference();
            fdColumns[j++] = inputSchemas[0].findVariable(decor);
        }
        // compile subplans and set the gby op. schema accordingly
        AlgebricksPipeline[] subplans = compileSubplans(inputSchemas[0], gby, opSchema, context);
        IAggregatorDescriptorFactory aggregatorFactory;

        if (((AbstractLogicalOperator) (gby.getNestedPlans().get(0).getRoots().get(0).getValue())).getOperatorTag() == LogicalOperatorTag.RUNNINGAGGREGATE) {
            aggregatorFactory = new NestedPlansRunningAggregatorFactory(subplans, keys, fdColumns);
        } else {
            aggregatorFactory = new NestedPlansAccumulatingAggregatorFactory(subplans, keys, fdColumns);
        }

        IOperatorDescriptorRegistry spec = builder.getJobSpec();
        IBinaryComparatorFactory[] comparatorFactories = JobGenHelper.variablesToAscBinaryComparatorFactories(
                columnList, context.getTypeEnvironment(op), context);
        RecordDescriptor recordDescriptor = JobGenHelper.mkRecordDescriptor(context.getTypeEnvironment(op), opSchema,
                context);

        PreclusteredGroupOperatorDescriptor opDesc = new PreclusteredGroupOperatorDescriptor(spec, keys,
                comparatorFactories, aggregatorFactory, recordDescriptor);

        contributeOpDesc(builder, (AbstractLogicalOperator) op, opDesc);

        ILogicalOperator src = op.getInputs().get(0).getValue();
        builder.contributeGraphEdge(src, 0, op, 0);
    }

}
