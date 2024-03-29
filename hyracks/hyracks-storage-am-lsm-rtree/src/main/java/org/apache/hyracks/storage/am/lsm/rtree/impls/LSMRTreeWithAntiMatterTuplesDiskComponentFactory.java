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

package org.apache.hyracks.storage.am.lsm.rtree.impls;

import org.apache.hyracks.storage.am.common.api.IndexException;
import org.apache.hyracks.storage.am.lsm.common.api.ILSMComponent;
import org.apache.hyracks.storage.am.lsm.common.api.ILSMComponentFactory;
import org.apache.hyracks.storage.am.lsm.common.api.ILSMComponentFilterFactory;
import org.apache.hyracks.storage.am.lsm.common.impls.LSMComponentFileReferences;
import org.apache.hyracks.storage.am.lsm.common.impls.TreeIndexFactory;
import org.apache.hyracks.storage.am.rtree.impls.RTree;
import org.apache.hyracks.storage.common.buffercache.IBufferCache;

public class LSMRTreeWithAntiMatterTuplesDiskComponentFactory implements ILSMComponentFactory {
    private final TreeIndexFactory<RTree> rtreeFactory;
    private final ILSMComponentFilterFactory filterFactory;

    public LSMRTreeWithAntiMatterTuplesDiskComponentFactory(TreeIndexFactory<RTree> rtreeFactory,
            ILSMComponentFilterFactory filterFactory) {
        this.rtreeFactory = rtreeFactory;
        this.filterFactory = filterFactory;
    }

    @Override
    public ILSMComponent createLSMComponentInstance(LSMComponentFileReferences cfr) throws IndexException {
        return new LSMRTreeDiskComponent(rtreeFactory.createIndexInstance(cfr.getInsertIndexFileReference()), null,
                null, filterFactory == null ? null : filterFactory.createLSMComponentFilter());
    }

    @Override
    public IBufferCache getBufferCache() {
        return rtreeFactory.getBufferCache();
    }
}
