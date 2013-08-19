package edu.uci.ics.genomix.pregelix.operator.pathmerge;

import java.util.Iterator;
import java.util.Random;


import edu.uci.ics.pregelix.api.job.PregelixJob;
import edu.uci.ics.genomix.pregelix.client.Client;
import edu.uci.ics.genomix.pregelix.format.GraphCleanOutputFormat;
import edu.uci.ics.genomix.pregelix.format.InitialGraphCleanInputFormat;
import edu.uci.ics.genomix.pregelix.io.PathMergeMessageWritable;
import edu.uci.ics.genomix.pregelix.io.VertexValueWritable;
import edu.uci.ics.genomix.pregelix.io.VertexValueWritable.State;
import edu.uci.ics.genomix.pregelix.type.MessageFlag;
import edu.uci.ics.genomix.type.VKmerBytesWritable;
import edu.uci.ics.genomix.type.NodeWritable.DirectionFlag;

/*
 * vertexId: BytesWritable
 * vertexValue: ByteWritable
 * edgeValue: NullWritable
 * message: MessageWritable
 * 
 * DNA:
 * A: 00
 * C: 01
 * G: 10
 * T: 11
 * 
 * succeed node
 *  A 00000001 1
 *  G 00000010 2
 *  C 00000100 4
 *  T 00001000 8
 * precursor node
 *  A 00010000 16
 *  G 00100000 32
 *  C 01000000 64
 *  T 10000000 128
 *  
 * For example, ONE LINE in input file: 00,01,10    0001,0010,
 * That means that vertexId is ACG, its succeed node is A and its precursor node is C.
 * The succeed node and precursor node will be stored in vertexValue and we don't use edgeValue.
 * The details about message are in edu.uci.ics.pregelix.example.io.MessageWritable. 
 */
/**
 * Naive Algorithm for path merge graph
 */
public class P4ForPathMergeVertex extends
    BasicPathMergeVertex {
    public static final String RANDSEED = "P4ForPathMergeVertex.randSeed";
    public static final String PROBBEINGRANDOMHEAD = "P4ForPathMergeVertex.probBeingRandomHead";

    private static long randSeed = 1;
    private float probBeingRandomHead = -1;
    private Random randGenerator;
    
    private VKmerBytesWritable curKmer = new VKmerBytesWritable();
    private VKmerBytesWritable nextKmer = new VKmerBytesWritable();
    private VKmerBytesWritable prevKmer = new VKmerBytesWritable();
    private boolean hasNext;
    private boolean hasPrev;
    private boolean curHead;
    private boolean nextHead;
    private boolean prevHead;
    
    /**
     * initiate kmerSize, maxIteration
     */
    public void initVertex() {
        if (kmerSize == -1)
            kmerSize = getContext().getConfiguration().getInt(KMER_SIZE, 5);
        if (maxIteration < 0)
            maxIteration = getContext().getConfiguration().getInt(ITERATIONS, 1000000);
        if(incomingMsg == null)
            incomingMsg = new PathMergeMessageWritable();
        if(outgoingMsg == null)
            outgoingMsg = new PathMergeMessageWritable();
        else
            outgoingMsg.reset();
        if(destVertexId == null)
            destVertexId = new VKmerBytesWritable();
        randSeed = getSuperstep();
        randGenerator = new Random(randSeed);
        if (probBeingRandomHead < 0)
            probBeingRandomHead = getContext().getConfiguration().getFloat("probBeingRandomHead", 0.5f);
        hasNext = false;
        hasPrev = false;
        curHead = false;
        nextHead = false;
        prevHead = false;
        outFlag = (byte)0;
        inFlag = (byte)0;
        // Node may be marked as head b/c it's a real head or a real tail
        headFlag = getHeadFlag();
        headMergeDir = getHeadMergeDir();
        if(repeatKmer == null)
            repeatKmer = new VKmerBytesWritable();
        tmpValue.reset();
    }

    protected boolean isNodeRandomHead(VKmerBytesWritable nodeKmer) {
        // "deterministically random", based on node id
        randGenerator.setSeed((randSeed ^ nodeKmer.hashCode()) * 100000 * getSuperstep());//randSeed + nodeID.hashCode()
        for(int i = 0; i < 500; i++)
            randGenerator.nextFloat();
        return randGenerator.nextFloat() < probBeingRandomHead;
    }
    
    /**
     * set nextKmer to the element that's next (in the node's FF or FR list), returning true when there is a next neighbor
     */
    protected boolean setNextInfo(VertexValueWritable value) {
        if (value.getFFList().getCountOfPosition() > 0) {
            nextKmer.setAsCopy(value.getFFList().get(0).getKey());
            nextHead = isNodeRandomHead(nextKmer);
            return true;
        }
        if (value.getFRList().getCountOfPosition() > 0) {
            nextKmer.setAsCopy(value.getFRList().get(0).getKey());
            nextHead = isNodeRandomHead(nextKmer);
            return true;
        }
        return false;
    }

    /**
     * set prevKmer to the element that's previous (in the node's RR or RF list), returning true when there is a previous neighbor
     */
    protected boolean setPrevInfo(VertexValueWritable value) {
        if (value.getRRList().getCountOfPosition() > 0) {
            prevKmer.setAsCopy(value.getRRList().get(0).getKey());
            prevHead = isNodeRandomHead(prevKmer);
            return true;
        }
        if (value.getRFList().getCountOfPosition() > 0) {
            prevKmer.setAsCopy(value.getRFList().get(0).getKey());
            prevHead = isNodeRandomHead(prevKmer);
            return true;
        }
        return false;
    }
    
    @Override
    public void compute(Iterator<PathMergeMessageWritable> msgIterator) {
        initVertex();
        if (getSuperstep() == 1)
            startSendMsg();
        else if (getSuperstep() == 2)
            initState(msgIterator);
        else if (getSuperstep() % 4 == 3){
            outFlag |= headFlag;
            
            outFlag |= MessageFlag.NO_MERGE;
            setStateAsNoMerge();
            
            // only PATH vertices are present. Find the ID's for my neighbors
            curKmer.setAsCopy(getVertexId());
            
            curHead = isNodeRandomHead(curKmer);
            
            // the headFlag and tailFlag's indicate if the node is at the beginning or end of a simple path. 
            // We prevent merging towards non-path nodes
            hasNext = setNextInfo(getVertexValue()) && (headFlag == 0 || (headFlag > 0 && headMergeDir == MessageFlag.HEAD_SHOULD_MERGEWITHNEXT));
            hasPrev = setPrevInfo(getVertexValue()) && (headFlag == 0 || (headFlag > 0 && headMergeDir == MessageFlag.HEAD_SHOULD_MERGEWITHPREV));
            if (hasNext || hasPrev) {
                if (curHead) {
                    if (hasNext && !nextHead) {
                        // compress this head to the forward tail
                		sendUpdateMsgToPredecessor(); 
                    } else if (hasPrev && !prevHead) {
                        // compress this head to the reverse tail
                        sendUpdateMsgToSuccessor();
                    } 
                }
                else {
                    // I'm a tail
                    if (hasNext && hasPrev) {
                         if ((!nextHead && !prevHead) && (curKmer.compareTo(nextKmer) < 0 && curKmer.compareTo(prevKmer) < 0)) {
                            // tails on both sides, and I'm the "local minimum"
                            // compress me towards the tail in forward dir
                            sendUpdateMsgToPredecessor();
                        }
                    } else if (!hasPrev) {
                        // no previous node
                        if (!nextHead && curKmer.compareTo(nextKmer) < 0) {
                            // merge towards tail in forward dir
                            sendUpdateMsgToPredecessor();
                        }
                    } else if (!hasNext) {
                        // no next node
                        if (!prevHead && curKmer.compareTo(prevKmer) < 0) {
                            // merge towards tail in reverse dir
                            sendUpdateMsgToSuccessor();
                        }
                    }
                }
            }
            this.activate();
        }
        else if (getSuperstep() % 4 == 0){
            //update neighber
            while (msgIterator.hasNext()) {
                incomingMsg = msgIterator.next();
                processUpdate();
                if(isHaltNode())
                    voteToHalt();
                else
                    this.activate();
            }
        } else if (getSuperstep() % 4 == 1){
            //send message to the merge object and kill self
            broadcastMergeMsg();
        } else if (getSuperstep() % 4 == 2){
            //merge tmpKmer
            while (msgIterator.hasNext()) {
                incomingMsg = msgIterator.next();
                selfFlag = (byte) (State.VERTEX_MASK & getVertexValue().getState());
                /** process merge **/
                processMerge();
                /** if it's a tandem repeat, which means detecting cycle **/
                if(isTandemRepeat()){
                    for(byte d : DirectionFlag.values)
                        getVertexValue().getEdgeList(d).reset();
                    getVertexValue().setState(MessageFlag.IS_HALT);
                    voteToHalt();
                }/** head meets head, stop **/ 
                else if((getMsgFlag() == MessageFlag.IS_HEAD && selfFlag == MessageFlag.IS_HEAD)){
                    getVertexValue().setState(MessageFlag.IS_HALT);
                    voteToHalt();
                }
                else
                    this.activate();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        PregelixJob job = new PregelixJob(P4ForPathMergeVertex.class.getSimpleName());
        job.setVertexClass(P4ForPathMergeVertex.class);
        /**
         * BinaryInput and BinaryOutput
         */
        job.setVertexInputFormatClass(InitialGraphCleanInputFormat.class);
        job.setVertexOutputFormatClass(GraphCleanOutputFormat.class);
        job.setDynamicVertexValueSize(true);
        job.setOutputKeyClass(VKmerBytesWritable.class);
        job.setOutputValueClass(VertexValueWritable.class);
        Client.run(args, job);
    }
}
