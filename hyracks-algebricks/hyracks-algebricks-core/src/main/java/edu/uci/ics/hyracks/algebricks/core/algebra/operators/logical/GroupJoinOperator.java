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

import java.util.List;

import org.apache.commons.lang3.mutable.Mutable;

import edu.uci.ics.hyracks.algebricks.core.algebra.base.ILogicalExpression;
import edu.uci.ics.hyracks.algebricks.core.algebra.base.ILogicalOperator;
import edu.uci.ics.hyracks.algebricks.core.algebra.base.ILogicalPlan;
import edu.uci.ics.hyracks.algebricks.core.algebra.base.LogicalOperatorTag;
import edu.uci.ics.hyracks.algebricks.core.algebra.expressions.IVariableTypeEnvironment;
import edu.uci.ics.hyracks.algebricks.core.algebra.properties.TypePropagationPolicy;
import edu.uci.ics.hyracks.algebricks.core.algebra.typing.ITypeEnvPointer;
import edu.uci.ics.hyracks.algebricks.core.algebra.typing.ITypingContext;
import edu.uci.ics.hyracks.algebricks.core.algebra.typing.OpRefTypeEnvPointer;
import edu.uci.ics.hyracks.algebricks.core.algebra.typing.PropagatingTypeEnvironment;
import edu.uci.ics.hyracks.algebricks.core.algebra.visitors.ILogicalOperatorVisitor;
import edu.uci.ics.hyracks.algebricks.core.api.exceptions.AlgebricksException;

public class GroupJoinOperator extends AbstractBinaryJoinOperator {

    private AbstractOperatorWithNestedPlans delegateGroupBy;
    
    public GroupJoinOperator(Mutable<ILogicalExpression> condition) {
        super(JoinKind.LEFT_OUTER, condition);
        this.delegateGroupBy = new GroupByOperator();
    }

    public GroupJoinOperator(JoinKind joinKind, Mutable<ILogicalExpression> condition,
            Mutable<ILogicalOperator> input1, Mutable<ILogicalOperator> input2, AbstractOperatorWithNestedPlans gby) throws AlgebricksException {
        super(joinKind, condition, input1, input2);
        if (gby.getOperatorTag() != LogicalOperatorTag.GROUP)
        	throw new AlgebricksException("Delegate operator must be Group By");
        this.delegateGroupBy = gby;
    }

    @Override
    public LogicalOperatorTag getOperatorTag() {
        return LogicalOperatorTag.GROUPJOIN;
    }

    public List<ILogicalPlan> getNestedPlans() {
        return delegateGroupBy.getNestedPlans();
    }
    
    public AbstractOperatorWithNestedPlans getGroupByOperator() {
    	return delegateGroupBy;
    }

    @Override
    public <R, T> R accept(ILogicalOperatorVisitor<R, T> visitor, T arg) throws AlgebricksException {
        return visitor.visitGroupJoinOperator(this, arg);
    }

    @Override
    public IVariableTypeEnvironment computeOutputTypeEnvironment(ITypingContext ctx) throws AlgebricksException {
        if(getJoinKind() == JoinKind.LEFT_OUTER) {
        	int n = inputs.size();
        	ITypeEnvPointer[] envPointers = new ITypeEnvPointer[n];
        	for (int i = 0; i < n; i++) {
        		envPointers[i] = new OpRefTypeEnvPointer(inputs.get(i), ctx);
        	}
        	return new PropagatingTypeEnvironment(ctx.getExpressionTypeComputer(), ctx.getNullableTypeComputer(),
        			ctx.getMetadataProvider(), TypePropagationPolicy.LEFT_OUTER, envPointers);
        }
        else {
            return createPropagatingAllInputsTypeEnvironment(ctx);
        }
    }

}
