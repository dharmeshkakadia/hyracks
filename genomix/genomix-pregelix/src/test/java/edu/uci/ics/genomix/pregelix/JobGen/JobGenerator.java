package edu.uci.ics.genomix.pregelix.JobGen;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

import org.apache.commons.io.FileUtils;

import edu.uci.ics.genomix.config.GenomixJobConf;
import edu.uci.ics.genomix.pregelix.checker.SymmetryCheckerVertex;
import edu.uci.ics.genomix.pregelix.format.CheckerOutputFormat;
import edu.uci.ics.genomix.pregelix.format.NodeToScaffoldingVertexInputFormat;
import edu.uci.ics.genomix.pregelix.format.NodeToVertexInputFormat;
import edu.uci.ics.genomix.pregelix.format.ScaffoldingVertexToNodeOutputFormat;
import edu.uci.ics.genomix.pregelix.format.VertexToNodeOutputFormat;
import edu.uci.ics.genomix.pregelix.io.VertexValueWritable;
import edu.uci.ics.genomix.pregelix.operator.aggregator.StatisticsAggregator;
import edu.uci.ics.genomix.pregelix.operator.bridgeremove.BridgeAddVertex;
import edu.uci.ics.genomix.pregelix.operator.bridgeremove.BridgeRemoveVertex;
import edu.uci.ics.genomix.pregelix.operator.bubblemerge.BubbleAddVertex;
import edu.uci.ics.genomix.pregelix.operator.bubblemerge.ComplexBubbleMergeVertex;
import edu.uci.ics.genomix.pregelix.operator.pathmerge.P1ForPathMergeVertex;
import edu.uci.ics.genomix.pregelix.operator.pathmerge.MapReduceVertex;
import edu.uci.ics.genomix.pregelix.operator.pathmerge.P4ForPathMergeVertex;
import edu.uci.ics.genomix.pregelix.operator.removelowcoverage.RemoveLowCoverageVertex;
import edu.uci.ics.genomix.pregelix.operator.scaffolding.BFSTraverseVertex;
import edu.uci.ics.genomix.pregelix.operator.scaffolding.ScaffoldingAggregator;
import edu.uci.ics.genomix.pregelix.operator.scaffolding.ScaffoldingVertex;
import edu.uci.ics.genomix.pregelix.operator.splitrepeat.SplitRepeatVertex;
import edu.uci.ics.genomix.pregelix.operator.tipremove.TipAddVertex;
import edu.uci.ics.genomix.pregelix.operator.tipremove.TipRemoveVertex;
import edu.uci.ics.genomix.pregelix.operator.unrolltandemrepeat.UnrollTandemRepeat;
import edu.uci.ics.genomix.type.VKmer;
import edu.uci.ics.pregelix.api.job.PregelixJob;

public class JobGenerator {

    public static String outputBase = "src/test/resources/jobs/";
    
    private static void generateUnrollTandemRepeatGraphJob(String jobName, String outputPath) throws IOException {
        PregelixJob job = new PregelixJob(new GenomixJobConf(3), jobName);
        job.setVertexClass(UnrollTandemRepeat.class);
        job.setGlobalAggregatorClass(StatisticsAggregator.class);
        job.setVertexInputFormatClass(NodeToVertexInputFormat.class);
        job.setVertexOutputFormatClass(VertexToNodeOutputFormat.class); 
        job.setDynamicVertexValueSize(true);
        job.setOutputKeyClass(VKmer.class);
        job.setOutputValueClass(VertexValueWritable.class);
        job.getConfiguration().writeXml(new FileOutputStream(new File(outputPath)));
    }

    private static void genUnrollTandemRepeatGraph() throws IOException {
        generateUnrollTandemRepeatGraphJob("UnrollTandemRepeatGraph", outputBase + "UnrollTandemRepeatGraph.xml");
    }
    
    private static void generateMapReduceGraphJob(String jobName, String outputPath) throws IOException {
        PregelixJob job = new PregelixJob(new GenomixJobConf(3), jobName);
        job.setVertexClass(MapReduceVertex.class);
        job.setVertexInputFormatClass(NodeToVertexInputFormat.class);
        job.setVertexOutputFormatClass(VertexToNodeOutputFormat.class); 
        job.setDynamicVertexValueSize(true);
        job.setOutputKeyClass(VKmer.class);
        job.setOutputValueClass(VertexValueWritable.class);
        job.getConfiguration().writeXml(new FileOutputStream(new File(outputPath)));
    }

    private static void genMapReduceGraph() throws IOException {
        generateMapReduceGraphJob("MapReduceGraph", outputBase + "MapReduceGraph.xml");
    }
    
    private static void generateP1ForMergeGraphJob(String jobName, String outputPath) throws IOException {
        PregelixJob job = new PregelixJob(new GenomixJobConf(3), jobName);
        job.setVertexClass(P1ForPathMergeVertex.class);
        job.setGlobalAggregatorClass(StatisticsAggregator.class);
        job.setVertexInputFormatClass(NodeToVertexInputFormat.class); //GraphCleanInputFormat
        job.setVertexOutputFormatClass(VertexToNodeOutputFormat.class);
        job.setDynamicVertexValueSize(true);
        job.setOutputKeyClass(VKmer.class);
        job.setOutputValueClass(VertexValueWritable.class);
        job.getConfiguration().writeXml(new FileOutputStream(new File(outputPath)));
    }

    private static void genP1ForMergeGraph() throws IOException {
        generateP1ForMergeGraphJob("P1ForMergeGraph", outputBase
                + "P1ForMergeGraph.xml");
    }

//    private static void generateP2ForMergeGraphJob(String jobName, String outputPath) throws IOException {
//        PregelixJob job = new PregelixJob(new GenomixJobConf(3), jobName);
//        job.setVertexClass(P2ForPathMergeVertex.class);
//        job.setGlobalAggregatorClass(StatisticsAggregator.class);
//        job.setVertexInputFormatClass(P2InitialGraphCleanInputFormat.class);
//        job.setVertexOutputFormatClass(P2PathMergeOutputFormat.class); 
//        job.setDynamicVertexValueSize(true);
//        job.setOutputKeyClass(VKmerBytesWritable.class);
//        job.setOutputValueClass(VertexValueWritable.class);
//        job.getConfiguration().writeXml(new FileOutputStream(new File(outputPath)));
//    }
//
//    private static void genP2ForMergeGraph() throws IOException {
//        generateP2ForMergeGraphJob("P2ForMergeGraph", outputBase + "P2ForMergeGraph.xml");
//    }
//    
//    private static void generateP3ForMergeGraphJob(String jobName, String outputPath) throws IOException {
//        PregelixJob job = new PregelixJob(new GenomixJobConf(3), jobName);
//        job.setVertexClass(P3ForPathMergeVertex.class);
//        job.setVertexInputFormatClass(NaiveAlgorithmForPathMergeInputFormat.class);
//        job.setVertexOutputFormatClass(GraphCleanOutputFormat.class);
//        job.setDynamicVertexValueSize(true);
//        job.setOutputKeyClass(PositionWritable.class);
//        job.setOutputValueClass(VertexValueWritable.class);
//        job.getConfiguration().setFloat(P3ForPathMergeVertex.PSEUDORATE, 0.3f);
//        job.getConfiguration().setInt(P3ForPathMergeVertex.MAXROUND, 2);
//        job.getConfiguration().writeXml(new FileOutputStream(new File(outputPath)));
//    }
//
//    private static void genP3ForMergeGraph() throws IOException {
//        generateP3ForMergeGraphJob("P3ForMergeGraph", outputBase
//                + "P3ForMergeGraph.xml");
//    }
    
    private static void generateP4ForMergeGraphJob(String jobName, String outputPath) throws IOException {
        PregelixJob job = new PregelixJob(new GenomixJobConf(3), jobName);
        job.setVertexClass(P4ForPathMergeVertex.class);
        job.setGlobalAggregatorClass(StatisticsAggregator.class);
        job.setVertexInputFormatClass(NodeToVertexInputFormat.class);
        job.setVertexOutputFormatClass(VertexToNodeOutputFormat.class);
        job.setDynamicVertexValueSize(true);
        job.setOutputKeyClass(VKmer.class);
        job.setOutputValueClass(VertexValueWritable.class);
        job.getConfiguration().setLong(GenomixJobConf.P4_RANDOM_SEED, new Random().nextLong());
        job.getConfiguration().writeXml(new FileOutputStream(new File(outputPath)));
    }

    private static void genP4ForMergeGraph() throws IOException {
        generateP4ForMergeGraphJob("P4ForMergeGraph", outputBase
                + "P4ForMergeGraph.xml");
    }
    
    private static void generateRemoveLowCoverageGraphJob(String jobName, String outputPath) throws IOException {
        PregelixJob job = new PregelixJob(new GenomixJobConf(3), jobName);
        job.setVertexClass(RemoveLowCoverageVertex.class);
        job.setGlobalAggregatorClass(StatisticsAggregator.class);
        job.setVertexInputFormatClass(NodeToVertexInputFormat.class);
        job.setVertexOutputFormatClass(VertexToNodeOutputFormat.class);
        job.setDynamicVertexValueSize(true);
        job.setOutputKeyClass(VKmer.class);
        job.setOutputValueClass(VertexValueWritable.class);
        job.getConfiguration().writeXml(new FileOutputStream(new File(outputPath)));
    }

    private static void genRemoveLowCoverageGraph() throws IOException {
        generateRemoveLowCoverageGraphJob("RemoveLowCoverageGraph", outputBase
                + "RemoveLowCoverageGraph.xml");
    }
    
    private static void generateTipAddGraphJob(String jobName, String outputPath) throws IOException {
        PregelixJob job = new PregelixJob(new GenomixJobConf(3), jobName);
        job.setVertexClass(TipAddVertex.class);
        job.setGlobalAggregatorClass(StatisticsAggregator.class);
        job.setVertexInputFormatClass(NodeToVertexInputFormat.class);
        job.setVertexOutputFormatClass(VertexToNodeOutputFormat.class);
        job.setDynamicVertexValueSize(true);
        job.setOutputKeyClass(VKmer.class);
        job.setOutputValueClass(VertexValueWritable.class);
        job.getConfiguration().writeXml(new FileOutputStream(new File(outputPath)));
    }

    private static void genTipAddGraph() throws IOException {
        generateTipAddGraphJob("TipAddGraph", outputBase
                + "TipAddGraph.xml");
    }
    
    private static void generateTipRemoveGraphJob(String jobName, String outputPath) throws IOException {
        PregelixJob job = new PregelixJob(new GenomixJobConf(3), jobName);
        job.setVertexClass(TipRemoveVertex.class);
        job.setGlobalAggregatorClass(StatisticsAggregator.class);
        job.setVertexInputFormatClass(NodeToVertexInputFormat.class);
        job.setVertexOutputFormatClass(VertexToNodeOutputFormat.class);
        job.setDynamicVertexValueSize(true);
        job.setOutputKeyClass(VKmer.class);
        job.setOutputValueClass(VertexValueWritable.class);
        job.getConfiguration().writeXml(new FileOutputStream(new File(outputPath)));
    }

    private static void genTipRemoveGraph() throws IOException {
        generateTipRemoveGraphJob("TipRemoveGraph", outputBase
                + "TipRemoveGraph.xml");
    }
    
    private static void generateBridgeAddGraphJob(String jobName, String outputPath) throws IOException {
        PregelixJob job = new PregelixJob(new GenomixJobConf(3), jobName);
        job.setVertexClass(BridgeAddVertex.class);
        job.setGlobalAggregatorClass(StatisticsAggregator.class);
        job.setVertexInputFormatClass(NodeToVertexInputFormat.class);
        job.setVertexOutputFormatClass(VertexToNodeOutputFormat.class);
        job.setDynamicVertexValueSize(true);
        job.setOutputKeyClass(VKmer.class);
        job.setOutputValueClass(VertexValueWritable.class);
        job.getConfiguration().writeXml(new FileOutputStream(new File(outputPath)));
    }

    private static void genBridgeAddGraph() throws IOException {
        generateBridgeAddGraphJob("BridgeAddGraph", outputBase
                + "BridgeAddGraph.xml");
    }

    private static void generateBridgeRemoveGraphJob(String jobName, String outputPath) throws IOException {
        PregelixJob job = new PregelixJob(new GenomixJobConf(3), jobName);
        job.setVertexClass(BridgeRemoveVertex.class);
        job.setGlobalAggregatorClass(StatisticsAggregator.class);
        job.setVertexInputFormatClass(NodeToVertexInputFormat.class);
        job.setVertexOutputFormatClass(VertexToNodeOutputFormat.class);
        job.setDynamicVertexValueSize(true);
        job.setOutputKeyClass(VKmer.class);
        job.setOutputValueClass(VertexValueWritable.class);
        job.getConfiguration().writeXml(new FileOutputStream(new File(outputPath)));
    }

    private static void genBridgeRemoveGraph() throws IOException {
        generateBridgeRemoveGraphJob("BridgeRemoveGraph", outputBase
                + "BridgeRemoveGraph.xml");
    }
    
    private static void generateBubbleAddGraphJob(String jobName, String outputPath) throws IOException {
        PregelixJob job = new PregelixJob(new GenomixJobConf(3), jobName);
        job.setVertexClass(BubbleAddVertex.class);
        job.setGlobalAggregatorClass(StatisticsAggregator.class);
        job.setVertexInputFormatClass(NodeToVertexInputFormat.class);
        job.setVertexOutputFormatClass(VertexToNodeOutputFormat.class);
        job.setDynamicVertexValueSize(true);
        job.setOutputKeyClass(VKmer.class);
        job.setOutputValueClass(VertexValueWritable.class);
        job.getConfiguration().writeXml(new FileOutputStream(new File(outputPath)));
    }

    private static void genBubbleAddGraph() throws IOException {
        generateBubbleAddGraphJob("BubbleAddGraph", outputBase
                + "BubbleAddGraph.xml");
    }
    
    private static void generateBubbleMergeGraphJob(String jobName, String outputPath) throws IOException {
        PregelixJob job = new PregelixJob(new GenomixJobConf(3), jobName);
        job.setVertexClass(ComplexBubbleMergeVertex.class);
        job.setGlobalAggregatorClass(StatisticsAggregator.class);
        job.setVertexInputFormatClass(NodeToVertexInputFormat.class);
        job.setVertexOutputFormatClass(VertexToNodeOutputFormat.class);
        job.setDynamicVertexValueSize(true);
        job.setOutputKeyClass(VKmer.class);
        job.setOutputValueClass(VertexValueWritable.class);
        job.getConfiguration().writeXml(new FileOutputStream(new File(outputPath)));
    }

    private static void genBubbleMergeGraph() throws IOException {
        generateBubbleMergeGraphJob("BubbleMergeGraph", outputBase
                + "BubbleMergeGraph.xml");
    }
    
    private static void generateSplitRepeatGraphJob(String jobName, String outputPath) throws IOException {
        PregelixJob job = new PregelixJob(new GenomixJobConf(3), jobName);
        job.setVertexClass(SplitRepeatVertex.class);
        job.setGlobalAggregatorClass(StatisticsAggregator.class);
        job.setVertexInputFormatClass(NodeToVertexInputFormat.class);
        job.setVertexOutputFormatClass(VertexToNodeOutputFormat.class); 
        job.setDynamicVertexValueSize(true);
        job.setOutputKeyClass(VKmer.class);
        job.setOutputValueClass(VertexValueWritable.class);
        job.getConfiguration().writeXml(new FileOutputStream(new File(outputPath)));
    }

    private static void genSplitRepeatGraph() throws IOException {
        generateSplitRepeatGraphJob("SplitRepeatGraph", outputBase + "SplitRepeatGraph.xml");
    }
    
    private static void generateBFSTraverseGraphJob(String jobName, String outputPath) throws IOException {
        PregelixJob job = new PregelixJob(new GenomixJobConf(3), jobName);
        job.setVertexClass(BFSTraverseVertex.class);
        job.setVertexInputFormatClass(NodeToVertexInputFormat.class);
        job.setVertexOutputFormatClass(VertexToNodeOutputFormat.class);
        job.setDynamicVertexValueSize(true);
        job.setOutputKeyClass(VKmer.class);
        job.setOutputValueClass(VertexValueWritable.class);
        job.getConfiguration().writeXml(new FileOutputStream(new File(outputPath)));
    }

    private static void getBFSTraverseGraph() throws IOException {
        generateBFSTraverseGraphJob("BFSTraversegGraph", outputBase
                + "BFSTraverseGraph.xml");
    }
    
    private static void generateScaffoldingGraphJob(String jobName, String outputPath) throws IOException {
        PregelixJob job = new PregelixJob(new GenomixJobConf(3), jobName);
        job.setVertexClass(ScaffoldingVertex.class);
        job.setGlobalAggregatorClass(ScaffoldingAggregator.class);
        job.setVertexInputFormatClass(NodeToScaffoldingVertexInputFormat.class);
        job.setVertexOutputFormatClass(ScaffoldingVertexToNodeOutputFormat.class);
        job.setDynamicVertexValueSize(true);
        job.setOutputKeyClass(VKmer.class);
        job.setOutputValueClass(VertexValueWritable.class);
        job.getConfiguration().writeXml(new FileOutputStream(new File(outputPath)));
    }

    private static void genScaffoldingGraph() throws IOException {
        generateScaffoldingGraphJob("ScaffoldingGraph", outputBase
                + "ScaffoldingGraph.xml");
    }
    
    private static void generateSymmetryCheckerGraphJob(String jobName, String outputPath) throws IOException {
        PregelixJob job = new PregelixJob(new GenomixJobConf(3), jobName);
        job.setVertexClass(SymmetryCheckerVertex.class);
        job.setGlobalAggregatorClass(StatisticsAggregator.class);
        job.setVertexInputFormatClass(NodeToVertexInputFormat.class);
        job.setVertexOutputFormatClass(CheckerOutputFormat.class);
        job.setDynamicVertexValueSize(true);
        job.setOutputKeyClass(VKmer.class);
        job.setOutputValueClass(VertexValueWritable.class);
        job.getConfiguration().writeXml(new FileOutputStream(new File(outputPath)));
    }

    private static void genSymmetryCheckerGraph() throws IOException {
        generateSymmetryCheckerGraphJob("SymmetryCheckerGraph", outputBase
                + "SymmetryCheckerGraph.xml");
    }
    
    public static void main(String[] args) throws IOException {
        FileUtils.forceMkdir(new File(outputBase));
        genUnrollTandemRepeatGraph();
        genMapReduceGraph();
        genP1ForMergeGraph();
        genP4ForMergeGraph();
        genRemoveLowCoverageGraph();
        genTipAddGraph();
        genTipRemoveGraph();
        genBridgeAddGraph();
        genBridgeRemoveGraph();
        genBubbleAddGraph();
        genBubbleMergeGraph();
        genSplitRepeatGraph();
        getBFSTraverseGraph();
        genScaffoldingGraph();
        genSymmetryCheckerGraph();
    }

}
