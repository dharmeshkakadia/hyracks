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

package org.apache.hyracks.storage.am.lsm.invertedindex.dataflow;

import org.apache.hyracks.api.context.IHyracksTaskContext;
import org.apache.hyracks.api.dataflow.IOperatorNodePushable;
import org.apache.hyracks.api.dataflow.value.IBinaryComparatorFactory;
import org.apache.hyracks.api.dataflow.value.IRecordDescriptorProvider;
import org.apache.hyracks.api.dataflow.value.ITypeTraits;
import org.apache.hyracks.api.dataflow.value.RecordDescriptor;
import org.apache.hyracks.api.job.IOperatorDescriptorRegistry;
import org.apache.hyracks.dataflow.std.file.IFileSplitProvider;
import org.apache.hyracks.storage.am.common.api.IIndexLifecycleManagerProvider;
import org.apache.hyracks.storage.am.common.api.IModificationOperationCallbackFactory;
import org.apache.hyracks.storage.am.common.api.ITupleFilterFactory;
import org.apache.hyracks.storage.am.common.dataflow.IIndexDataflowHelperFactory;
import org.apache.hyracks.storage.am.common.impls.NoOpOperationCallbackFactory;
import org.apache.hyracks.storage.am.common.ophelpers.IndexOperation;
import org.apache.hyracks.storage.am.lsm.common.dataflow.LSMIndexInsertUpdateDeleteOperatorNodePushable;
import org.apache.hyracks.storage.am.lsm.invertedindex.tokenizers.IBinaryTokenizerFactory;
import org.apache.hyracks.storage.common.IStorageManagerInterface;
import org.apache.hyracks.storage.common.file.NoOpLocalResourceFactoryProvider;

public class LSMInvertedIndexInsertUpdateDeleteOperator extends AbstractLSMInvertedIndexOperatorDescriptor {

    private static final long serialVersionUID = 1L;

    protected final int[] fieldPermutation;
    protected final IndexOperation op;

    public LSMInvertedIndexInsertUpdateDeleteOperator(IOperatorDescriptorRegistry spec, RecordDescriptor recDesc,
            IStorageManagerInterface storageManager, IFileSplitProvider fileSplitProvider,
            IIndexLifecycleManagerProvider lifecycleManagerProvider, ITypeTraits[] tokenTypeTraits,
            IBinaryComparatorFactory[] tokenComparatorFactories, ITypeTraits[] invListsTypeTraits,
            IBinaryComparatorFactory[] invListComparatorFactories, IBinaryTokenizerFactory tokenizerFactory,
            int[] fieldPermutation, IndexOperation op, IIndexDataflowHelperFactory dataflowHelperFactory,
            ITupleFilterFactory tupleFilterFactory, IModificationOperationCallbackFactory modificationOpCallbackFactory) {
        super(spec, 1, 1, recDesc, storageManager, fileSplitProvider, lifecycleManagerProvider, tokenTypeTraits,
                tokenComparatorFactories, invListsTypeTraits, invListComparatorFactories, tokenizerFactory,
                dataflowHelperFactory, tupleFilterFactory, false, false,
                null, NoOpLocalResourceFactoryProvider.INSTANCE, NoOpOperationCallbackFactory.INSTANCE, modificationOpCallbackFactory);
        this.fieldPermutation = fieldPermutation;
        this.op = op;
    }

    @Override
    public IOperatorNodePushable createPushRuntime(IHyracksTaskContext ctx,
            IRecordDescriptorProvider recordDescProvider, int partition, int nPartitions) {
        return new LSMIndexInsertUpdateDeleteOperatorNodePushable(this, ctx, partition, fieldPermutation,
                recordDescProvider, op);
    }
}