package edu.uci.ics.genomix.pregelix.JobRun;

import junit.framework.Test;

public class SplitRepeatTestSuite extends BasicGraphCleanTestSuite{

    public static Test suite() throws Exception {
        String pattern ="SplitRepeat";
        String testSet[] = {"2to1"};//{"SimpleSplitManyTimes", "ComplexSplitManyTimes"};
        init(pattern, testSet);
        BasicGraphCleanTestSuite testSuite = new BasicGraphCleanTestSuite();
        return makeTestSuite(testSuite);
    }
}
