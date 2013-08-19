package edu.uci.ics.genomix.pregelix.JobRun;

import junit.framework.Test;

public class PathMergeTestSuite extends BasicGraphCleanTestSuite{

    public static Test suite() throws Exception {
        String pattern ="PathMerge";
        String testSet[] = //{"2", "3", "4", "5", "6", "7", "8", "9", "head_6", "head_7"}; 
                {
                "FR",
                "RF",
                "head_FR",
                "head_RF",
                "twohead_FR",
                "twohead_RF",
//                "SimplePath",
//                "ThreeDuplicate",
//                "BridgePath_AfterUnroll"
//                "head_6"
//                "head_7"
                "CyclePath",
//                "SelfPath"
//                "TreePath"
                "HyracksGraphBuild",
                };
        init(pattern, testSet);
        BasicGraphCleanTestSuite testSuite = new BasicGraphCleanTestSuite();
        return makeTestSuite(testSuite);
    }
}