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

package org.apache.hyracks.storage.am.lsm.invertedindex.inmemory;

import org.apache.hyracks.api.context.IHyracksCommonContext;
import org.apache.hyracks.api.exceptions.HyracksDataException;
import org.apache.hyracks.dataflow.common.data.accessors.ITupleReference;
import org.apache.hyracks.storage.am.btree.api.IBTreeLeafFrame;
import org.apache.hyracks.storage.am.btree.impls.BTree.BTreeAccessor;
import org.apache.hyracks.storage.am.btree.impls.BTreeRangeSearchCursor;
import org.apache.hyracks.storage.am.common.api.IIndexCursor;
import org.apache.hyracks.storage.am.common.api.IIndexOperationContext;
import org.apache.hyracks.storage.am.common.api.ISearchPredicate;
import org.apache.hyracks.storage.am.common.api.IndexException;
import org.apache.hyracks.storage.am.common.impls.NoOpOperationCallback;
import org.apache.hyracks.storage.am.common.ophelpers.IndexOperation;
import org.apache.hyracks.storage.am.lsm.invertedindex.api.IInvertedIndexAccessor;
import org.apache.hyracks.storage.am.lsm.invertedindex.api.IInvertedIndexSearcher;
import org.apache.hyracks.storage.am.lsm.invertedindex.api.IInvertedListCursor;
import org.apache.hyracks.storage.am.lsm.invertedindex.ondisk.OnDiskInvertedIndex.DefaultHyracksCommonContext;
import org.apache.hyracks.storage.am.lsm.invertedindex.ondisk.OnDiskInvertedIndexSearchCursor;
import org.apache.hyracks.storage.am.lsm.invertedindex.search.InvertedIndexSearchPredicate;
import org.apache.hyracks.storage.am.lsm.invertedindex.search.TOccurrenceSearcher;

public class InMemoryInvertedIndexAccessor implements IInvertedIndexAccessor {
    // TODO: This ctx needs to go away.
    protected final IHyracksCommonContext hyracksCtx = new DefaultHyracksCommonContext();
    protected final IInvertedIndexSearcher searcher;
    protected IIndexOperationContext opCtx;
    protected InMemoryInvertedIndex index;
    protected BTreeAccessor btreeAccessor;

    public InMemoryInvertedIndexAccessor(InMemoryInvertedIndex index, IIndexOperationContext opCtx)
            throws HyracksDataException {
        this.opCtx = opCtx;
        this.index = index;
        this.searcher = createSearcher();
        this.btreeAccessor = (BTreeAccessor) index.getBTree().createAccessor(NoOpOperationCallback.INSTANCE,
                NoOpOperationCallback.INSTANCE);
    }

    @Override
    public void insert(ITupleReference tuple) throws HyracksDataException, IndexException {
        opCtx.setOperation(IndexOperation.INSERT);
        index.insert(tuple, btreeAccessor, opCtx);
    }

    @Override
    public void delete(ITupleReference tuple) throws HyracksDataException, IndexException {
        opCtx.setOperation(IndexOperation.DELETE);
        index.delete(tuple, btreeAccessor, opCtx);
    }

    @Override
    public IIndexCursor createSearchCursor(boolean exclusive) {
        return new OnDiskInvertedIndexSearchCursor(searcher, index.getInvListTypeTraits().length);
    }

    @Override
    public void search(IIndexCursor cursor, ISearchPredicate searchPred) throws HyracksDataException, IndexException {
        searcher.search((OnDiskInvertedIndexSearchCursor) cursor, (InvertedIndexSearchPredicate) searchPred, opCtx);
    }

    @Override
    public IInvertedListCursor createInvertedListCursor() {
        return index.createInvertedListCursor();
    }

    @Override
    public void openInvertedListCursor(IInvertedListCursor listCursor, ITupleReference searchKey)
            throws HyracksDataException, IndexException {
        index.openInvertedListCursor(listCursor, searchKey, opCtx);
    }

    @Override
    public IIndexCursor createRangeSearchCursor() {
        IBTreeLeafFrame leafFrame = (IBTreeLeafFrame) index.getBTree().getLeafFrameFactory().createFrame();
        return new BTreeRangeSearchCursor(leafFrame, false);
    }

    @Override
    public void rangeSearch(IIndexCursor cursor, ISearchPredicate searchPred) throws IndexException,
            HyracksDataException {
        btreeAccessor.search(cursor, searchPred);
    }

    public BTreeAccessor getBTreeAccessor() {
        return btreeAccessor;
    }

    @Override
    public void update(ITupleReference tuple) throws HyracksDataException, IndexException {
        throw new UnsupportedOperationException("Update not supported by in-memory inverted index.");
    }

    @Override
    public void upsert(ITupleReference tuple) throws HyracksDataException, IndexException {
        throw new UnsupportedOperationException("Upsert not supported by in-memory inverted index.");
    }

    protected IInvertedIndexSearcher createSearcher() throws HyracksDataException {
        return new TOccurrenceSearcher(hyracksCtx, index);
    }
}
