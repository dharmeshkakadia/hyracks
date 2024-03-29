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
package org.apache.hyracks.algebricks.runtime.evaluators;

import java.io.DataOutput;
import java.io.IOException;

import org.apache.hyracks.algebricks.common.exceptions.AlgebricksException;
import org.apache.hyracks.algebricks.runtime.base.ICopyEvaluator;
import org.apache.hyracks.algebricks.runtime.base.ICopyEvaluatorFactory;
import org.apache.hyracks.data.std.api.IDataOutputProvider;
import org.apache.hyracks.dataflow.common.data.accessors.IFrameTupleReference;

public class ConstantEvalFactory implements ICopyEvaluatorFactory {
    private static final long serialVersionUID = 1L;

    private byte[] value;

    public ConstantEvalFactory(byte[] value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Constant";
    }

    @Override
    public ICopyEvaluator createEvaluator(final IDataOutputProvider output) throws AlgebricksException {
        return new ICopyEvaluator() {

            private DataOutput out = output.getDataOutput();

            @Override
            public void evaluate(IFrameTupleReference tuple) throws AlgebricksException {
                try {
                    out.write(value, 0, value.length);
                } catch (IOException ioe) {
                    throw new AlgebricksException(ioe);
                }
            }
        };
    }

}
