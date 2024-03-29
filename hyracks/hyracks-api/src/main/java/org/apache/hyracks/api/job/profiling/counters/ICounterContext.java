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
package org.apache.hyracks.api.job.profiling.counters;

/**
 * A namespace that holds named counters.
 * 
 * @author vinayakb
 */
public interface ICounterContext {
    /**
     * Get a counter with the specified name.
     * 
     * @param name
     *            - Name of the counter to get.
     * @param create
     *            - Create if the counter does not exist.
     * @return An existing counter with the given name (if one exists). If a counter with the
     *         said name does not exist, a new one is created if create is set to <code>true</code>, or
     *         else returns <code>null</code>.
     */
    public ICounter getCounter(String name, boolean create);
}