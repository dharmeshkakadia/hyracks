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

package org.apache.hyracks.storage.am.lsm.rtree.utils;

import java.util.List;

import org.apache.hyracks.api.dataflow.value.IBinaryComparatorFactory;
import org.apache.hyracks.api.dataflow.value.ILinearizeComparatorFactory;
import org.apache.hyracks.api.dataflow.value.ITypeTraits;
import org.apache.hyracks.api.io.FileReference;
import org.apache.hyracks.data.std.primitive.DoublePointable;
import org.apache.hyracks.data.std.primitive.IntegerPointable;
import org.apache.hyracks.storage.am.bloomfilter.impls.BloomFilterFactory;
import org.apache.hyracks.storage.am.btree.frames.BTreeNSMInteriorFrameFactory;
import org.apache.hyracks.storage.am.btree.frames.BTreeNSMLeafFrameFactory;
import org.apache.hyracks.storage.am.btree.impls.BTree;
import org.apache.hyracks.storage.am.common.api.IPrimitiveValueProviderFactory;
import org.apache.hyracks.storage.am.common.api.ITreeIndexFrameFactory;
import org.apache.hyracks.storage.am.common.api.ITreeIndexMetaDataFrameFactory;
import org.apache.hyracks.storage.am.common.api.TreeIndexException;
import org.apache.hyracks.storage.am.common.frames.LIFOMetaDataFrameFactory;
import org.apache.hyracks.storage.am.common.freepage.LinkedListFreePageManagerFactory;
import org.apache.hyracks.storage.am.common.tuples.TypeAwareTupleWriterFactory;
import org.apache.hyracks.storage.am.lsm.common.api.ILSMIOOperationCallback;
import org.apache.hyracks.storage.am.lsm.common.api.ILSMIOOperationScheduler;
import org.apache.hyracks.storage.am.lsm.common.api.ILSMIndexFileManager;
import org.apache.hyracks.storage.am.lsm.common.api.ILSMMergePolicy;
import org.apache.hyracks.storage.am.lsm.common.api.ILSMOperationTracker;
import org.apache.hyracks.storage.am.lsm.common.api.IVirtualBufferCache;
import org.apache.hyracks.storage.am.lsm.common.frames.LSMComponentFilterFrameFactory;
import org.apache.hyracks.storage.am.lsm.common.impls.BTreeFactory;
import org.apache.hyracks.storage.am.lsm.common.impls.LSMComponentFilterFactory;
import org.apache.hyracks.storage.am.lsm.common.impls.LSMComponentFilterManager;
import org.apache.hyracks.storage.am.lsm.common.impls.TreeIndexFactory;
import org.apache.hyracks.storage.am.lsm.rtree.impls.ExternalRTree;
import org.apache.hyracks.storage.am.lsm.rtree.impls.LSMRTree;
import org.apache.hyracks.storage.am.lsm.rtree.impls.LSMRTreeFileManager;
import org.apache.hyracks.storage.am.lsm.rtree.impls.LSMRTreeWithAntiMatterTuples;
import org.apache.hyracks.storage.am.lsm.rtree.impls.LSMRTreeWithAntiMatterTuplesFileManager;
import org.apache.hyracks.storage.am.lsm.rtree.impls.RTreeFactory;
import org.apache.hyracks.storage.am.lsm.rtree.tuples.LSMRTreeCopyTupleWriterFactory;
import org.apache.hyracks.storage.am.lsm.rtree.tuples.LSMRTreeTupleWriterFactory;
import org.apache.hyracks.storage.am.lsm.rtree.tuples.LSMTypeAwareTupleWriterFactory;
import org.apache.hyracks.storage.am.rtree.frames.RTreeNSMInteriorFrameFactory;
import org.apache.hyracks.storage.am.rtree.frames.RTreeNSMLeafFrameFactory;
import org.apache.hyracks.storage.am.rtree.frames.RTreePolicyType;
import org.apache.hyracks.storage.am.rtree.impls.RTree;
import org.apache.hyracks.storage.am.rtree.linearize.HilbertDoubleComparatorFactory;
import org.apache.hyracks.storage.am.rtree.linearize.ZCurveDoubleComparatorFactory;
import org.apache.hyracks.storage.am.rtree.linearize.ZCurveIntComparatorFactory;
import org.apache.hyracks.storage.common.buffercache.IBufferCache;
import org.apache.hyracks.storage.common.file.IFileMapProvider;

public class LSMRTreeUtils {
    public static LSMRTree createLSMTree(List<IVirtualBufferCache> virtualBufferCaches, FileReference file,
            IBufferCache diskBufferCache, IFileMapProvider diskFileMapProvider, ITypeTraits[] typeTraits,
            IBinaryComparatorFactory[] rtreeCmpFactories, IBinaryComparatorFactory[] btreeCmpFactories,
            IPrimitiveValueProviderFactory[] valueProviderFactories, RTreePolicyType rtreePolicyType,
            double bloomFilterFalsePositiveRate, ILSMMergePolicy mergePolicy, ILSMOperationTracker opTracker,
            ILSMIOOperationScheduler ioScheduler, ILSMIOOperationCallback ioOpCallback,
            ILinearizeComparatorFactory linearizeCmpFactory, int[] rtreeFields, int[] buddyBTreeFields,
            ITypeTraits[] filterTypeTraits, IBinaryComparatorFactory[] filterCmpFactories, int[] filterFields,
            boolean durable) throws TreeIndexException {

        ITypeTraits[] btreeTypeTraits = new ITypeTraits[buddyBTreeFields.length];
        for (int i = 0; i < btreeTypeTraits.length; i++) {
            btreeTypeTraits[i] = typeTraits[buddyBTreeFields[i]];
        }

        LSMTypeAwareTupleWriterFactory rtreeTupleWriterFactory = new LSMTypeAwareTupleWriterFactory(typeTraits, false);
        LSMTypeAwareTupleWriterFactory btreeTupleWriterFactory = new LSMTypeAwareTupleWriterFactory(btreeTypeTraits,
                true);

        ITreeIndexFrameFactory rtreeInteriorFrameFactory = new RTreeNSMInteriorFrameFactory(rtreeTupleWriterFactory,
                valueProviderFactories, rtreePolicyType);
        ITreeIndexFrameFactory rtreeLeafFrameFactory = new RTreeNSMLeafFrameFactory(rtreeTupleWriterFactory,
                valueProviderFactories, rtreePolicyType);

        ITreeIndexFrameFactory btreeInteriorFrameFactory = new BTreeNSMInteriorFrameFactory(btreeTupleWriterFactory);
        ITreeIndexFrameFactory btreeLeafFrameFactory = new BTreeNSMLeafFrameFactory(btreeTupleWriterFactory);

        ITreeIndexMetaDataFrameFactory metaFrameFactory = new LIFOMetaDataFrameFactory();
        LinkedListFreePageManagerFactory freePageManagerFactory = new LinkedListFreePageManagerFactory(diskBufferCache,
                metaFrameFactory);

        TreeIndexFactory<RTree> diskRTreeFactory = new RTreeFactory(diskBufferCache, diskFileMapProvider,
                freePageManagerFactory, rtreeInteriorFrameFactory, rtreeLeafFrameFactory, rtreeCmpFactories,
                typeTraits.length);
        TreeIndexFactory<BTree> diskBTreeFactory = new BTreeFactory(diskBufferCache, diskFileMapProvider,
                freePageManagerFactory, btreeInteriorFrameFactory, btreeLeafFrameFactory, btreeCmpFactories,
                btreeTypeTraits.length);

        int[] comparatorFields = { 0 };
        IBinaryComparatorFactory[] linearizerArray = { linearizeCmpFactory };

        int[] bloomFilterKeyFields = new int[btreeCmpFactories.length];
        for (int i = 0; i < btreeCmpFactories.length; i++) {
            bloomFilterKeyFields[i] = i;
        }
        BloomFilterFactory bloomFilterFactory = new BloomFilterFactory(diskBufferCache, diskFileMapProvider,
                bloomFilterKeyFields);

        LSMComponentFilterFactory filterFactory = null;
        LSMComponentFilterFrameFactory filterFrameFactory = null;
        LSMComponentFilterManager filterManager = null;
        if (filterCmpFactories != null) {
            TypeAwareTupleWriterFactory filterTupleWriterFactory = new TypeAwareTupleWriterFactory(filterTypeTraits);
            filterFactory = new LSMComponentFilterFactory(filterTupleWriterFactory, filterCmpFactories);
            filterFrameFactory = new LSMComponentFilterFrameFactory(filterTupleWriterFactory,
                    diskBufferCache.getPageSize());
            filterManager = new LSMComponentFilterManager(diskBufferCache, filterFrameFactory);
        }
        ILSMIndexFileManager fileNameManager = new LSMRTreeFileManager(diskFileMapProvider, file, diskRTreeFactory,
                diskBTreeFactory);
        LSMRTree lsmTree = new LSMRTree(virtualBufferCaches, rtreeInteriorFrameFactory, rtreeLeafFrameFactory,
                btreeInteriorFrameFactory, btreeLeafFrameFactory, fileNameManager, diskRTreeFactory, diskBTreeFactory,
                bloomFilterFactory, filterFactory, filterFrameFactory, filterManager, bloomFilterFalsePositiveRate,
                diskFileMapProvider, typeTraits.length, rtreeCmpFactories, btreeCmpFactories, linearizeCmpFactory,
                comparatorFields, linearizerArray, mergePolicy, opTracker, ioScheduler, ioOpCallback, rtreeFields,
                buddyBTreeFields, filterFields, durable);
        return lsmTree;
    }

    public static LSMRTreeWithAntiMatterTuples createLSMTreeWithAntiMatterTuples(
            List<IVirtualBufferCache> virtualBufferCaches, FileReference file, IBufferCache diskBufferCache,
            IFileMapProvider diskFileMapProvider, ITypeTraits[] typeTraits,
            IBinaryComparatorFactory[] rtreeCmpFactories, IBinaryComparatorFactory[] btreeCmpFactories,
            IPrimitiveValueProviderFactory[] valueProviderFactories, RTreePolicyType rtreePolicyType,
            ILSMMergePolicy mergePolicy, ILSMOperationTracker opTracker, ILSMIOOperationScheduler ioScheduler,
            ILSMIOOperationCallback ioOpCallback, ILinearizeComparatorFactory linearizerCmpFactory, int[] rtreeFields,
            ITypeTraits[] filterTypeTraits, IBinaryComparatorFactory[] filterCmpFactories, int[] filterFields,
            boolean durable) throws TreeIndexException {
        LSMRTreeTupleWriterFactory rtreeTupleWriterFactory = new LSMRTreeTupleWriterFactory(typeTraits, false);
        LSMRTreeTupleWriterFactory btreeTupleWriterFactory = new LSMRTreeTupleWriterFactory(typeTraits, true);

        LSMRTreeCopyTupleWriterFactory copyTupleWriterFactory = new LSMRTreeCopyTupleWriterFactory(typeTraits);

        ITreeIndexFrameFactory rtreeInteriorFrameFactory = new RTreeNSMInteriorFrameFactory(rtreeTupleWriterFactory,
                valueProviderFactories, rtreePolicyType);
        ITreeIndexFrameFactory rtreeLeafFrameFactory = new RTreeNSMLeafFrameFactory(rtreeTupleWriterFactory,
                valueProviderFactories, rtreePolicyType);

        ITreeIndexFrameFactory btreeInteriorFrameFactory = new BTreeNSMInteriorFrameFactory(btreeTupleWriterFactory);
        ITreeIndexFrameFactory btreeLeafFrameFactory = new BTreeNSMLeafFrameFactory(btreeTupleWriterFactory);

        ITreeIndexFrameFactory copyTupleLeafFrameFactory = new RTreeNSMLeafFrameFactory(copyTupleWriterFactory,
                valueProviderFactories, rtreePolicyType);

        ITreeIndexMetaDataFrameFactory metaFrameFactory = new LIFOMetaDataFrameFactory();
        LinkedListFreePageManagerFactory freePageManagerFactory = new LinkedListFreePageManagerFactory(diskBufferCache,
                metaFrameFactory);

        TreeIndexFactory<RTree> diskRTreeFactory = new RTreeFactory(diskBufferCache, diskFileMapProvider,
                freePageManagerFactory, rtreeInteriorFrameFactory, copyTupleLeafFrameFactory, rtreeCmpFactories,
                typeTraits.length);

        TreeIndexFactory<RTree> bulkLoadRTreeFactory = new RTreeFactory(diskBufferCache, diskFileMapProvider,
                freePageManagerFactory, rtreeInteriorFrameFactory, rtreeLeafFrameFactory, rtreeCmpFactories,
                typeTraits.length);

        // The first field is for the sorted curve (e.g. Hilbert curve), and the
        // second field is for the primary key.
        int[] comparatorFields = new int[btreeCmpFactories.length - rtreeCmpFactories.length + 1];
        IBinaryComparatorFactory[] linearizerArray = new IBinaryComparatorFactory[btreeCmpFactories.length
                - rtreeCmpFactories.length + 1];

        comparatorFields[0] = 0;
        for (int i = 1; i < comparatorFields.length; i++) {
            comparatorFields[i] = rtreeCmpFactories.length - 1 + i;
        }
        linearizerArray[0] = linearizerCmpFactory;
        int j = 1;
        for (int i = rtreeCmpFactories.length; i < btreeCmpFactories.length; i++) {
            linearizerArray[j] = btreeCmpFactories[i];
            j++;
        }

        LSMComponentFilterFactory filterFactory = null;
        LSMComponentFilterFrameFactory filterFrameFactory = null;
        LSMComponentFilterManager filterManager = null;
        if (filterCmpFactories != null) {
            TypeAwareTupleWriterFactory filterTupleWriterFactory = new TypeAwareTupleWriterFactory(filterTypeTraits);
            filterFactory = new LSMComponentFilterFactory(filterTupleWriterFactory, filterCmpFactories);
            filterFrameFactory = new LSMComponentFilterFrameFactory(filterTupleWriterFactory,
                    diskBufferCache.getPageSize());
            filterManager = new LSMComponentFilterManager(diskBufferCache, filterFrameFactory);
        }
        ILSMIndexFileManager fileNameManager = new LSMRTreeWithAntiMatterTuplesFileManager(diskFileMapProvider, file,
                diskRTreeFactory);
        LSMRTreeWithAntiMatterTuples lsmTree = new LSMRTreeWithAntiMatterTuples(virtualBufferCaches,
                rtreeInteriorFrameFactory, rtreeLeafFrameFactory, btreeInteriorFrameFactory, btreeLeafFrameFactory,
                fileNameManager, diskRTreeFactory, bulkLoadRTreeFactory, filterFactory, filterFrameFactory,
                filterManager, diskFileMapProvider, typeTraits.length, rtreeCmpFactories, btreeCmpFactories,
                linearizerCmpFactory, comparatorFields, linearizerArray, mergePolicy, opTracker, ioScheduler,
                ioOpCallback, rtreeFields, filterFields, durable);
        return lsmTree;
    }

    public static ExternalRTree createExternalRTree(FileReference file, IBufferCache diskBufferCache,
            IFileMapProvider diskFileMapProvider, ITypeTraits[] typeTraits,
            IBinaryComparatorFactory[] rtreeCmpFactories, IBinaryComparatorFactory[] btreeCmpFactories,
            IPrimitiveValueProviderFactory[] valueProviderFactories, RTreePolicyType rtreePolicyType,
            double bloomFilterFalsePositiveRate, ILSMMergePolicy mergePolicy, ILSMOperationTracker opTracker,
            ILSMIOOperationScheduler ioScheduler, ILSMIOOperationCallback ioOpCallback,
            ILinearizeComparatorFactory linearizeCmpFactory, int[] buddyBTreeFields, int startWithVersion,
            boolean durable) throws TreeIndexException {

        ITypeTraits[] btreeTypeTraits = new ITypeTraits[buddyBTreeFields.length];
        for (int i = 0; i < btreeTypeTraits.length; i++) {
            btreeTypeTraits[i] = typeTraits[buddyBTreeFields[i]];
        }

        LSMTypeAwareTupleWriterFactory rtreeTupleWriterFactory = new LSMTypeAwareTupleWriterFactory(typeTraits, false);
        LSMTypeAwareTupleWriterFactory btreeTupleWriterFactory = new LSMTypeAwareTupleWriterFactory(btreeTypeTraits,
                true);

        ITreeIndexFrameFactory rtreeInteriorFrameFactory = new RTreeNSMInteriorFrameFactory(rtreeTupleWriterFactory,
                valueProviderFactories, rtreePolicyType);
        ITreeIndexFrameFactory rtreeLeafFrameFactory = new RTreeNSMLeafFrameFactory(rtreeTupleWriterFactory,
                valueProviderFactories, rtreePolicyType);

        ITreeIndexFrameFactory btreeInteriorFrameFactory = new BTreeNSMInteriorFrameFactory(btreeTupleWriterFactory);
        ITreeIndexFrameFactory btreeLeafFrameFactory = new BTreeNSMLeafFrameFactory(btreeTupleWriterFactory);

        ITreeIndexMetaDataFrameFactory metaFrameFactory = new LIFOMetaDataFrameFactory();
        LinkedListFreePageManagerFactory freePageManagerFactory = new LinkedListFreePageManagerFactory(diskBufferCache,
                metaFrameFactory);

        TreeIndexFactory<RTree> diskRTreeFactory = new RTreeFactory(diskBufferCache, diskFileMapProvider,
                freePageManagerFactory, rtreeInteriorFrameFactory, rtreeLeafFrameFactory, rtreeCmpFactories,
                typeTraits.length);
        TreeIndexFactory<BTree> diskBTreeFactory = new BTreeFactory(diskBufferCache, diskFileMapProvider,
                freePageManagerFactory, btreeInteriorFrameFactory, btreeLeafFrameFactory, btreeCmpFactories,
                btreeTypeTraits.length);
        int[] comparatorFields = { 0 };
        IBinaryComparatorFactory[] linearizerArray = { linearizeCmpFactory };

        int[] bloomFilterKeyFields = new int[btreeCmpFactories.length];
        for (int i = 0; i < btreeCmpFactories.length; i++) {
            bloomFilterKeyFields[i] = i;
        }
        BloomFilterFactory bloomFilterFactory = new BloomFilterFactory(diskBufferCache, diskFileMapProvider,
                bloomFilterKeyFields);

        ILSMIndexFileManager fileNameManager = new LSMRTreeFileManager(diskFileMapProvider, file, diskRTreeFactory,
                diskBTreeFactory);
        ExternalRTree lsmTree = new ExternalRTree(rtreeInteriorFrameFactory, rtreeLeafFrameFactory,
                btreeInteriorFrameFactory, btreeLeafFrameFactory, fileNameManager, diskRTreeFactory, diskBTreeFactory,
                bloomFilterFactory, bloomFilterFalsePositiveRate, diskFileMapProvider, typeTraits.length,
                rtreeCmpFactories, btreeCmpFactories, linearizeCmpFactory, comparatorFields, linearizerArray,
                mergePolicy, opTracker, ioScheduler, ioOpCallback, buddyBTreeFields, startWithVersion, durable);
        return lsmTree;
    }

    public static ILinearizeComparatorFactory proposeBestLinearizer(ITypeTraits[] typeTraits, int numKeyFields)
            throws TreeIndexException {
        for (int i = 0; i < numKeyFields; i++) {
            if (!(typeTraits[i].getClass().equals(typeTraits[0].getClass()))) {
                throw new TreeIndexException("Cannot propose linearizer if dimensions have different types");
            }
        }

        if (numKeyFields / 2 == 2 && (typeTraits[0].getClass() == DoublePointable.TYPE_TRAITS.getClass())) {
            return new HilbertDoubleComparatorFactory(2);
        } else if (typeTraits[0].getClass() == DoublePointable.TYPE_TRAITS.getClass()) {
            return new ZCurveDoubleComparatorFactory(numKeyFields / 2);
        } else if (typeTraits[0].getClass() == IntegerPointable.TYPE_TRAITS.getClass()) {
            return new ZCurveIntComparatorFactory(numKeyFields / 2);
        }

        throw new TreeIndexException("Cannot propose linearizer");
    }
}
