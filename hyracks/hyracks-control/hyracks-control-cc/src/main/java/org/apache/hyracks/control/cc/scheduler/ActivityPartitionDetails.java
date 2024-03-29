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
package org.apache.hyracks.control.cc.scheduler;

import java.util.Arrays;

public class ActivityPartitionDetails {
    private final int nPartitions;

    private final int[] nInputPartitions;

    private final int[] nOutputPartitions;

    public ActivityPartitionDetails(int nPartitions, int[] nInputPartitions, int[] nOutputPartitions) {
        this.nPartitions = nPartitions;
        this.nInputPartitions = nInputPartitions;
        this.nOutputPartitions = nOutputPartitions;
    }

    public int getPartitionCount() {
        return nPartitions;
    }

    public int[] getInputPartitionCounts() {
        return nInputPartitions;
    }

    public int[] getOutputPartitionCounts() {
        return nOutputPartitions;
    }

    @Override
    public String toString() {
        return nPartitions + ":" + (nInputPartitions == null ? "[]" : Arrays.toString(nInputPartitions)) + ":"
                + (nOutputPartitions == null ? "[]" : Arrays.toString(nOutputPartitions));
    }
}