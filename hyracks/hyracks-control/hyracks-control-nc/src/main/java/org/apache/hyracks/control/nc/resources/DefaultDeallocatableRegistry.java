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
package org.apache.hyracks.control.nc.resources;

import java.util.List;
import java.util.Vector;

import org.apache.hyracks.api.resources.IDeallocatable;
import org.apache.hyracks.api.resources.IDeallocatableRegistry;

public class DefaultDeallocatableRegistry implements IDeallocatableRegistry {
    private final List<IDeallocatable> deallocatables;

    public DefaultDeallocatableRegistry() {
        deallocatables = new Vector<IDeallocatable>();
    }

    @Override
    public void registerDeallocatable(IDeallocatable deallocatable) {
        deallocatables.add(deallocatable);
    }

    public void close() {
        for (IDeallocatable d : deallocatables) {
            try {
                d.deallocate();
            } catch (Exception e) {
                // ignore
            }
        }
    }
}