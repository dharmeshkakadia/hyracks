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
package org.apache.hyracks.algebricks.runtime.operators.std;

import org.apache.hyracks.algebricks.data.IAWriter;
import org.apache.hyracks.algebricks.data.IPrinterFactory;
import org.apache.hyracks.algebricks.runtime.base.IPushRuntime;
import org.apache.hyracks.algebricks.runtime.base.IPushRuntimeFactory;
import org.apache.hyracks.algebricks.runtime.writers.PrinterBasedWriterFactory;
import org.apache.hyracks.api.context.IHyracksTaskContext;
import org.apache.hyracks.api.dataflow.value.RecordDescriptor;

public class PrinterRuntimeFactory implements IPushRuntimeFactory {

    private static final long serialVersionUID = 1L;

    private final int[] printColumns;
    private final IPrinterFactory[] printerFactories;
    private final RecordDescriptor inputRecordDesc;

    public PrinterRuntimeFactory(int[] printColumns, IPrinterFactory[] printerFactories,
            RecordDescriptor inputRecordDesc) {
        this.printColumns = printColumns;
        this.printerFactories = printerFactories;
        this.inputRecordDesc = inputRecordDesc;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("print [");
        for (int i = 0; i < printColumns.length; i++) {
            if (i > 0) {
                buf.append("; ");
            }
            buf.append(printColumns[i]);
        }
        buf.append("]");
        return buf.toString();
    }

    @Override
    public IPushRuntime createPushRuntime(IHyracksTaskContext ctx) {
        IAWriter w = PrinterBasedWriterFactory.INSTANCE.createWriter(printColumns, System.out, printerFactories,
                inputRecordDesc);
        return new SinkWriterRuntime(w, ctx, System.out, inputRecordDesc);
    }
}
