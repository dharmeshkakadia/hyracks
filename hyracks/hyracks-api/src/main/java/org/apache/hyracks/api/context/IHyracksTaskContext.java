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
package org.apache.hyracks.api.context;

import org.apache.hyracks.api.dataflow.TaskAttemptId;
import org.apache.hyracks.api.dataset.IDatasetPartitionManager;
import org.apache.hyracks.api.deployment.DeploymentId;
import org.apache.hyracks.api.io.IWorkspaceFileFactory;
import org.apache.hyracks.api.job.IOperatorEnvironment;
import org.apache.hyracks.api.job.profiling.counters.ICounterContext;
import org.apache.hyracks.api.resources.IDeallocatableRegistry;

public interface IHyracksTaskContext extends IHyracksCommonContext, IWorkspaceFileFactory, IDeallocatableRegistry,
        IOperatorEnvironment {
    public IHyracksJobletContext getJobletContext();

    public TaskAttemptId getTaskAttemptId();

    public ICounterContext getCounterContext();

    public IDatasetPartitionManager getDatasetPartitionManager();

    public void sendApplicationMessageToCC(byte[] message, DeploymentId deploymendId, String nodeId) throws Exception;
}