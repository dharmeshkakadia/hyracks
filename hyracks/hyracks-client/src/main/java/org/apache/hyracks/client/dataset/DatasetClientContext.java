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
package org.apache.hyracks.client.dataset;

import org.apache.hyracks.api.context.IHyracksCommonContext;
import org.apache.hyracks.api.io.IIOManager;
import org.apache.hyracks.control.nc.resources.memory.FrameManager;

public class DatasetClientContext extends FrameManager implements IHyracksCommonContext {
    private final int frameSize;

    public DatasetClientContext(int frameSize) {
        super(frameSize);
        this.frameSize = frameSize;
    }

    @Override
    public IIOManager getIOManager() {
        return null;
    }

}
