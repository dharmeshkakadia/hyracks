/*
 * Copyright 2009-2010 by The Regents of the University of California
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
package edu.uci.ics.hyracks.imru.runtime.bootstrap;

import edu.uci.ics.hyracks.api.job.JobId;

public class StateKey {
    private final JobId jobId;
    private final int partition;

    public StateKey(JobId jobId, int partition) {
        this.jobId = jobId;
        this.partition = partition;
    }

    @Override
    public int hashCode() {
        return jobId.hashCode() * partition;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof StateKey))
            return false;
        StateKey key = (StateKey) o;
        return key.jobId.equals(jobId) && key.partition == partition;
    }

    @Override
    public String toString() {
        return jobId.toString() + ":" + partition;
    }
}
