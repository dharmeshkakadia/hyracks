/*
 * Copyright 2009-2010 by The Regents of the University of California
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
package edu.uci.ics.hyracks.algebricks.core.algebra.operators.logical;

import java.util.ArrayList;

import edu.uci.ics.hyracks.algebricks.core.algebra.base.LogicalOperatorTag;
import edu.uci.ics.hyracks.algebricks.core.algebra.base.LogicalVariable;
import edu.uci.ics.hyracks.algebricks.core.algebra.expressions.IVariableTypeEnvironment;
import edu.uci.ics.hyracks.algebricks.core.algebra.properties.VariablePropagationPolicy;
import edu.uci.ics.hyracks.algebricks.core.algebra.typing.ITypingContext;
import edu.uci.ics.hyracks.algebricks.core.algebra.visitors.ILogicalExpressionReferenceTransform;
import edu.uci.ics.hyracks.algebricks.core.algebra.visitors.ILogicalOperatorVisitor;
import edu.uci.ics.hyracks.algebricks.core.api.exceptions.AlgebricksException;

/**
 * @author rico
 * 
 */
public class StatisticsOperator extends AbstractLogicalOperator {

    public StatisticsOperator() {
        super();
        setExecutionMode(AbstractLogicalOperator.ExecutionMode.PARTITIONED);
    }

    @Override
    public void recomputeSchema() throws AlgebricksException {
        schema = new ArrayList<LogicalVariable>(inputs.get(0).getValue().getSchema());
    }

    @Override
    public boolean acceptExpressionTransform(ILogicalExpressionReferenceTransform transform) throws AlgebricksException {
        return false;
    }

    @Override
    public <R, T> R accept(ILogicalOperatorVisitor<R, T> visitor, T arg) throws AlgebricksException {
        return visitor.visitStatsOperator(this, arg);
    }

    @Override
    public boolean isMap() {
        return true;
    }

    @Override
    public VariablePropagationPolicy getVariablePropagationPolicy() {
        return VariablePropagationPolicy.ALL;
    }

    @Override
    public IVariableTypeEnvironment computeOutputTypeEnvironment(ITypingContext ctx) throws AlgebricksException {
        return this.createPropagatingAllInputsTypeEnvironment(ctx);
    }

    @Override
    public LogicalOperatorTag getOperatorTag() {
        return LogicalOperatorTag.STATS;
    }

}
