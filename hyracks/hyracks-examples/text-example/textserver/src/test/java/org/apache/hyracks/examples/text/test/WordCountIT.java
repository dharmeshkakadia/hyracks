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
package org.apache.hyracks.examples.text.test;

import java.io.File;

import org.junit.Test;

import org.apache.hyracks.examples.text.client.WordCountMain;

public class WordCountIT {
    @Test
    public void runWordCount() throws Exception {
        WordCountMain.main(new String[] { "-host", "localhost", "-infile-splits", getInfileSplits(), "-outfile-splits",
                getOutfileSplits(), "-algo", "-hash" });
    }

    private String getInfileSplits() {
        return "NC1:" + new File("data/file1.txt").getAbsolutePath() + ",NC2:"
                + new File("data/file2.txt").getAbsolutePath();
    }

    private String getOutfileSplits() {
        return "NC1:" + new File("target/wc1.txt").getAbsolutePath() + ",NC2:"
                + new File("target/wc2.txt").getAbsolutePath();
    }
}
