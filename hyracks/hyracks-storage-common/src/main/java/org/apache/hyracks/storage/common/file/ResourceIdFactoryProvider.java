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
package org.apache.hyracks.storage.common.file;

import java.util.List;

import org.apache.hyracks.api.exceptions.HyracksDataException;

public class ResourceIdFactoryProvider {
    private ILocalResourceRepository localResourceRepository;

    public ResourceIdFactoryProvider(ILocalResourceRepository localResourceRepository) {
        this.localResourceRepository = localResourceRepository;
    }

    public ResourceIdFactory createResourceIdFactory() throws HyracksDataException {
        List<LocalResource> localResources = localResourceRepository.getAllResources();
        long largestResourceId = 0;
        for (LocalResource localResource : localResources) {
            if (largestResourceId < localResource.getResourceId()) {
                largestResourceId = localResource.getResourceId();
            }
        }
        return new ResourceIdFactory(largestResourceId);
    }
}
