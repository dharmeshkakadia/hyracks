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

package org.apache.hyracks.storage.am.btree.frames;

import org.apache.hyracks.storage.am.btree.api.IBTreeLeafFrame;
import org.apache.hyracks.storage.am.common.api.ITreeIndexFrameFactory;
import org.apache.hyracks.storage.am.common.api.ITreeIndexTupleWriterFactory;

public class BTreeFieldPrefixNSMLeafFrameFactory implements ITreeIndexFrameFactory {

    private static final long serialVersionUID = 1L;

    private final ITreeIndexTupleWriterFactory tupleWriterFactory;

    public BTreeFieldPrefixNSMLeafFrameFactory(ITreeIndexTupleWriterFactory tupleWriterFactory) {
        this.tupleWriterFactory = tupleWriterFactory;
    }

    @Override
    public IBTreeLeafFrame createFrame() {
        return new BTreeFieldPrefixNSMLeafFrame(tupleWriterFactory.createTupleWriter());
    }

    @Override
    public ITreeIndexTupleWriterFactory getTupleWriterFactory() {
        return tupleWriterFactory;
    }
}