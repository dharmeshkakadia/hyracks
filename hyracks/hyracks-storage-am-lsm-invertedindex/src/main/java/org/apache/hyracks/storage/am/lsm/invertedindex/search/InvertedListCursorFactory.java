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

package org.apache.hyracks.storage.am.lsm.invertedindex.search;

import org.apache.hyracks.storage.am.lsm.invertedindex.api.IInvertedIndex;
import org.apache.hyracks.storage.am.lsm.invertedindex.api.IInvertedListCursor;
import org.apache.hyracks.storage.am.lsm.invertedindex.api.IObjectFactory;

public class InvertedListCursorFactory implements IObjectFactory<IInvertedListCursor> {

    private final IInvertedIndex invIndex;

    public InvertedListCursorFactory(IInvertedIndex invIndex) {
        this.invIndex = invIndex;
    }

    @Override
    public IInvertedListCursor create() {
        return invIndex.createInvertedListCursor();
    }
}
