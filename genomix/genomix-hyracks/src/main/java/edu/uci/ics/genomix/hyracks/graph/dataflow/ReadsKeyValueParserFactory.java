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

package edu.uci.ics.genomix.hyracks.graph.dataflow;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.lib.NLineInputFormat;

import edu.uci.ics.genomix.type.KmerBytesWritable;
import edu.uci.ics.genomix.type.NodeWritable;
import edu.uci.ics.genomix.type.NodeWritable.EDGETYPE;
import edu.uci.ics.genomix.type.PositionListWritable;
import edu.uci.ics.genomix.type.ReadIdListWritable;
import edu.uci.ics.genomix.type.PositionWritable;
import edu.uci.ics.genomix.type.VKmerBytesWritable;
import edu.uci.ics.hyracks.api.comm.IFrameWriter;
import edu.uci.ics.hyracks.api.context.IHyracksTaskContext;
import edu.uci.ics.hyracks.api.dataflow.value.ISerializerDeserializer;
import edu.uci.ics.hyracks.api.dataflow.value.RecordDescriptor;
import edu.uci.ics.hyracks.api.exceptions.HyracksDataException;
import edu.uci.ics.hyracks.dataflow.common.comm.io.ArrayTupleBuilder;
import edu.uci.ics.hyracks.dataflow.common.comm.io.FrameTupleAppender;
import edu.uci.ics.hyracks.dataflow.common.comm.util.FrameUtils;
import edu.uci.ics.hyracks.hdfs.api.IKeyValueParser;
import edu.uci.ics.hyracks.hdfs.api.IKeyValueParserFactory;
import edu.uci.ics.hyracks.hdfs.dataflow.ConfFactory;

public class ReadsKeyValueParserFactory implements IKeyValueParserFactory<LongWritable, Text> {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(ReadsKeyValueParserFactory.class.getName());

    public static final int OutputKmerField = 0;
    public static final int OutputNodeField = 1;

    private final int kmerSize;
    public ConfFactory confFac;
    
    public static final RecordDescriptor readKmerOutputRec = new RecordDescriptor(new ISerializerDeserializer[] { null,
            null });

    public ReadsKeyValueParserFactory(int k, ConfFactory conFac) {
        this.kmerSize = k;
        this.confFac = conFac;
    }

    public enum KmerDir {
        FORWARD,
        REVERSE,
    }

    @Override
    public IKeyValueParser<LongWritable, Text> createKeyValueParser(final IHyracksTaskContext ctx) throws HyracksDataException {
        final ArrayTupleBuilder tupleBuilder = new ArrayTupleBuilder(2);
        final ByteBuffer outputBuffer = ctx.allocateFrame();
        final FrameTupleAppender outputAppender = new FrameTupleAppender(ctx.getFrameSize());
        outputAppender.reset(outputBuffer, true);
        KmerBytesWritable.setGlobalKmerLength(kmerSize);
        
        return new IKeyValueParser<LongWritable, Text>() {

            private PositionWritable positionId = new PositionWritable();
            private ReadIdListWritable readIdList = new ReadIdListWritable();
            private NodeWritable curNode = new NodeWritable();
            private NodeWritable nextNode = new NodeWritable();

            private KmerBytesWritable curForwardKmer = new KmerBytesWritable();
            private KmerBytesWritable curReverseKmer = new KmerBytesWritable();
            private KmerBytesWritable nextForwardKmer = new KmerBytesWritable();
            private KmerBytesWritable nextReverseKmer = new KmerBytesWritable();
            private VKmerBytesWritable edgeVKmer = new VKmerBytesWritable();
            private KmerDir curKmerDir = KmerDir.FORWARD;
            private KmerDir nextKmerDir = KmerDir.FORWARD;

            byte mateId = (byte) 0;
            boolean fastqFormat = false;
//            int lineCount = 0;
            
            @Override
            public void parse(LongWritable key, Text value, IFrameWriter writer,  String fileString) throws HyracksDataException {
                
                String[] tokens = fileString.split("\\.(?=[^\\.]+$)");  // split on the last "." to get the basename and the extension
                if (tokens.length > 2) 
                    throw new IllegalStateException("Parse error trying to parse filename... split extension tokens are: " + tokens.toString());
                String basename = tokens[0];
                String extension = tokens.length == 2 ? tokens[1] : ""; 
                
                if (basename.endsWith("_2")) {
                    mateId = (byte) 1;
                } else {
                    mateId = (byte) 0;
                }
                
                if (extension.contains("fastq") || extension.contains("fq")) {
                    //TODO
//                    if (! (job.getInputFormat() instanceof NLineInputFormat)) {
//                        throw new IllegalStateException("Fastq files require the NLineInputFormat (was " + job.getInputFormat() + " ).");
//                    }
//                    if (job.getInt("mapred.line.input.format.linespermap", -1) % 4 != 0) {
//                        throw new IllegalStateException("Fastq files require the `mapred.line.input.format.linespermap` option to be divisible by 4 (was " + job.get("mapred.line.input.format.linespermap") + ").");
//                    }
                    fastqFormat = true;
                }
                
//                String[] geneLine = value.toString().split("\\t"); // Read the Real Gene Line
//                if (geneLine.length != 2) {
//                    throw new IllegalArgumentException("malformed line found in parser. Two values aren't separated by tabs: " + value.toString());
//                }
//                int readID = 0;
//                try {
//                    readID = Integer.parseInt(geneLine[0]);
//                } catch (NumberFormatException e) {
//                    throw new IllegalArgumentException("Malformed line found in parser: ", e);
//                }
                
//                lineCount++;
                long readID = 0;
                String geneLine;
                if (fastqFormat) {
//                    if ((lineCount - 1) % 4 == 1) {
                        readID = key.get();  // this is actually the offset into the file... will it be the same across all files?? //
                        geneLine = value.toString().trim();
//                    } else {
//                        return;  //skip all other lines
//                    }
                } else {
                    String[] rawLine = value.toString().split("\\t"); // Read the Real Gene Line
                    if (rawLine.length != 2) {
                        throw new HyracksDataException("invalid data");
                    }
                    readID = Long.parseLong(rawLine[0]);
                    geneLine = rawLine[1];
                }
                
                Pattern genePattern = Pattern.compile("[AGCT]+");
                Matcher geneMatcher = genePattern.matcher(geneLine);
                boolean isValid = geneMatcher.matches();
                if (isValid) {
                    SplitReads(readID, geneLine.getBytes(), writer);
                }
            }

            private void SplitReads(long readID, byte[] array, IFrameWriter writer) {
//                boolean verbose = false;
                /*first kmer*/
                if (kmerSize >= array.length) {
                    throw new IllegalArgumentException("kmersize (k="+kmerSize+") is larger than the read length (" + array.length + ")");
                }
                
//                if (readID == 12009721) {
//                    verbose = false;
//                    System.out.println("found it: " + readID);
//                } else if (readID == 11934501) {
//                    verbose = false;
//                    System.out.println("found it: " + readID);
//                } else {
//                    verbose = false;
//                }
                
                curNode.reset();
                nextNode.reset();
                curNode.setAvgCoverage(1);
                nextNode.setAvgCoverage(1);
                curForwardKmer.setFromStringBytes(array, 0);
                curReverseKmer.setReversedFromStringBytes(array, 0);
                curKmerDir = curForwardKmer.compareTo(curReverseKmer) <= 0 ? KmerDir.FORWARD : KmerDir.REVERSE;
                nextForwardKmer.setAsCopy(curForwardKmer);
                nextKmerDir = setNextKmer(nextForwardKmer, nextReverseKmer, array[kmerSize]);
                setThisReadId(mateId, readID, 0);
                if(curKmerDir == KmerDir.FORWARD)
                    curNode.getStartReads().append(positionId);
                else
                    curNode.getEndReads().append(positionId);
                setEdgeAndThreadListForCurAndNextKmer(curKmerDir, curNode, nextKmerDir, nextNode, readIdList);
                
                writeToFrame(curForwardKmer, curReverseKmer, curKmerDir, curNode, writer);
//                if (verbose) {
//                    System.out.println("First kmer emitting:" + curForwardKmer.toString() + '\t' + curReverseKmer + '\t' + curKmerDir + '\t' + curNode);
//                }
                /*middle kmer*/
                int i = kmerSize + 1;
                for (; i < array.length; i++) {
                    curForwardKmer.setAsCopy(nextForwardKmer);
                    curReverseKmer.setAsCopy(nextReverseKmer);
                    curKmerDir = nextKmerDir;
                    curNode.setAsCopy(nextNode);
                    nextNode.reset();
                    nextNode.setAvgCoverage(1);
                    nextKmerDir = setNextKmer(nextForwardKmer, nextReverseKmer, array[i]);
                    setEdgeAndThreadListForCurAndNextKmer(curKmerDir, curNode, nextKmerDir, nextNode, readIdList);
                    writeToFrame(curForwardKmer, curReverseKmer, curKmerDir, curNode, writer);
//                    if (verbose) {
//                        System.out.println("middle kmer emitting:" + curForwardKmer.toString() + '\t' + curReverseKmer + '\t' + curKmerDir + '\t' + curNode);
//                    }
                }

                /*last kmer*/
                writeToFrame(nextForwardKmer, nextReverseKmer, nextKmerDir, nextNode, writer);
//                if (verbose) {
//                    System.out.println("last kmer emitting:" + curForwardKmer.toString() + '\t' + curReverseKmer + '\t' + curKmerDir + '\t' + curNode);
//                }
            }

            public void setThisReadId(byte mateId, long readId, int posId) {
                readIdList.clear();
                readIdList.add(readId);
                positionId.set(mateId, readId, posId);
            }

            public KmerDir setNextKmer(KmerBytesWritable forwardKmer, KmerBytesWritable ReverseKmer,
                    byte nextChar) {
                forwardKmer.shiftKmerWithNextChar(nextChar);
                ReverseKmer.setReversedFromStringBytes(forwardKmer.toString().getBytes(), forwardKmer.getOffset());
                return forwardKmer.compareTo(ReverseKmer) <= 0 ? KmerDir.FORWARD : KmerDir.REVERSE;
            }

            public void writeToFrame(KmerBytesWritable forwardKmer, KmerBytesWritable reverseKmer, KmerDir curKmerDir,
                    NodeWritable node, IFrameWriter writer) {
                switch (curKmerDir) {
                    case FORWARD:
                        InsertToFrame(forwardKmer, node, writer);
                        break;
                    case REVERSE:
                        InsertToFrame(reverseKmer, node, writer);
                        break;
                }
            }

            public void setEdgeAndThreadListForCurAndNextKmer(KmerDir curKmerDir, NodeWritable curNode, KmerDir nextKmerDir,
                    NodeWritable nextNode, ReadIdListWritable readIdList) {
                if (curKmerDir == KmerDir.FORWARD && nextKmerDir == KmerDir.FORWARD) {
                    curNode.getEdgeList(EDGETYPE.FF).put(new VKmerBytesWritable(nextForwardKmer), readIdList);
                    nextNode.getEdgeList(EDGETYPE.RR).put(new VKmerBytesWritable(curForwardKmer), readIdList);
                }
                if (curKmerDir == KmerDir.FORWARD && nextKmerDir == KmerDir.REVERSE) {
                    curNode.getEdgeList(EDGETYPE.FF).put(new VKmerBytesWritable(nextReverseKmer), readIdList);
                    nextNode.getEdgeList(EDGETYPE.RR).put(new VKmerBytesWritable(curForwardKmer), readIdList);
                }
                if (curKmerDir == KmerDir.REVERSE && nextKmerDir == KmerDir.FORWARD) {
                    curNode.getEdgeList(EDGETYPE.FF).put(new VKmerBytesWritable(nextForwardKmer), readIdList);
                    nextNode.getEdgeList(EDGETYPE.RR).put(new VKmerBytesWritable(curReverseKmer), readIdList);
                }
                if (curKmerDir == KmerDir.REVERSE && nextKmerDir == KmerDir.REVERSE) {
                    curNode.getEdgeList(EDGETYPE.FF).put(new VKmerBytesWritable(nextReverseKmer), readIdList);
                    nextNode.getEdgeList(EDGETYPE.RR).put(new VKmerBytesWritable(curReverseKmer), readIdList);
                }
            }

            private void InsertToFrame(KmerBytesWritable kmer, NodeWritable node, IFrameWriter writer) {
                try {
                    tupleBuilder.reset();
                    tupleBuilder.addField(kmer.getBytes(), kmer.getOffset(), kmer.getLength());
                    tupleBuilder.addField(node.marshalToByteArray(), 0, node.getSerializedLength());

                    if (!outputAppender.append(tupleBuilder.getFieldEndOffsets(), tupleBuilder.getByteArray(), 0,
                            tupleBuilder.getSize())) {
                        FrameUtils.flushFrame(outputBuffer, writer);
                        outputAppender.reset(outputBuffer, true);
                        if (!outputAppender.append(tupleBuilder.getFieldEndOffsets(), tupleBuilder.getByteArray(), 0,
                                tupleBuilder.getSize())) {
                            throw new IllegalStateException(
                                    "Failed to copy an record into a frame: the record kmerByteSize is too large.");
                        }
                    }
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }

            @Override
            public void open(IFrameWriter writer) throws HyracksDataException {
            }

            @Override
            public void close(IFrameWriter writer) throws HyracksDataException {
                FrameUtils.flushFrame(outputBuffer, writer);
            }

        };
    }
}
