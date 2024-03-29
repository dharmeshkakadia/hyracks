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
package org.apache.hyracks.dataflow.std.base;

import java.util.BitSet;

import org.apache.hyracks.api.job.IConnectorDescriptorRegistry;

public abstract class AbstractMToNConnectorDescriptor extends AbstractConnectorDescriptor {
    private static final long serialVersionUID = 1L;

    public AbstractMToNConnectorDescriptor(IConnectorDescriptorRegistry spec) {
        super(spec);
    }

    @Override
    public void indicateTargetPartitions(int nProducerPartitions, int nConsumerPartitions, int producerIndex,
            BitSet targetBitmap) {
        targetBitmap.clear();
        targetBitmap.set(0, nConsumerPartitions);
    }

    @Override
    public void indicateSourcePartitions(int nProducerPartitions, int nConsumerPartitions, int consumerIndex,
            BitSet sourceBitmap) {
        sourceBitmap.clear();
        sourceBitmap.set(0, nProducerPartitions);
    }
    
    @Override
    public boolean allProducersToAllConsumers(){
        return true;
    }
}