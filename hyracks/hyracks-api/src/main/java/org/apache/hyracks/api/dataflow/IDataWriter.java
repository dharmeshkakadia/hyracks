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
package org.apache.hyracks.api.dataflow;

import org.apache.hyracks.api.exceptions.HyracksDataException;

/**
 * Accepts data from data producers.
 * 
 * @author vinayakb
 */
public interface IDataWriter<T> {
    /**
     * Pushes data to the acceptor.
     * 
     * @param data
     *            - Data pushed to the acceptor. <code>null</code> indicates the
     *            end of stream.
     * @throws HyracksDataException
     */
    public void writeData(T data) throws HyracksDataException;

    /**
     * Indicates that the stream has failed.
     * 
     * @throws HyracksDataException
     */
    public void fail() throws HyracksDataException;

    /**
     * Closes this writer.
     * 
     * @throws Exception
     */
    public void close() throws HyracksDataException;
}