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
package org.apache.hyracks.server.process;

import java.util.List;

import org.apache.hyracks.control.cc.CCDriver;
import org.apache.hyracks.control.common.controllers.CCConfig;

public class HyracksCCProcess extends HyracksServerProcess {
    private CCConfig config;

    public HyracksCCProcess(CCConfig config) {
        this.config = config;
    }

    @Override
    protected void addCmdLineArgs(List<String> cList) {
        config.toCommandLine(cList);
    }

    @Override
    protected String getMainClassName() {
        return CCDriver.class.getName();
    }
}