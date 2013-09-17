package edu.uci.ics.genomix.pregelix.operator.pathmerge;

import java.util.Iterator;

import edu.uci.ics.genomix.pregelix.io.VertexValueWritable;
import edu.uci.ics.genomix.pregelix.io.VertexValueWritable.State;
import edu.uci.ics.genomix.pregelix.io.message.MessageWritable;
import edu.uci.ics.genomix.pregelix.io.message.PathMergeMessageWritable;
import edu.uci.ics.genomix.pregelix.log.LogUtil;
import edu.uci.ics.genomix.pregelix.operator.BasicGraphCleanVertex;
import edu.uci.ics.genomix.pregelix.type.MessageFlag;
import edu.uci.ics.genomix.pregelix.util.VertexUtil;
import edu.uci.ics.genomix.type.EdgeWritable;
import edu.uci.ics.genomix.type.NodeWritable.OutgoingListFlag;
import edu.uci.ics.genomix.type.NodeWritable.IncomingListFlag;
import edu.uci.ics.genomix.type.VKmerBytesWritable;

public abstract class BasicPathMergeVertex<V extends VertexValueWritable, M extends PathMergeMessageWritable> extends
	BasicGraphCleanVertex<V, M>{
    protected static final boolean isP1 = true;
    protected static final boolean isP2 = false;
    protected static final boolean isP4 = true;
    
    public byte getHeadMergeDir(){
        return (byte) (getVertexValue().getState() & State.HEAD_CAN_MERGE_MASK);
    }
    
    public byte getMsgMergeDir(){
        return (byte) (incomingMsg.getFlag() & MessageFlag.HEAD_CAN_MERGE_MASK);
    }
    /**
     * start sending message
     */
    public void startSendMsg() {
        if(isTandemRepeat(getVertexValue())){
            getCopyWithoutTandemRepeats(getVertexValue());
            outFlag = 0;
            sendSettledMsgToAllNeighborNodes(tmpValue);
            voteToHalt();
        } else{
            /** check incoming **/
            // update internal state
            if (VertexUtil.isVertexWithOnlyOneIncoming(getVertexValue())){
                byte state = 0;
                state |= State.HEAD_CAN_MERGEWITHPREV;
                getVertexValue().setState(state);
                activate();
            } 
            // send to neighbors
            else if (VertexUtil.isVertexWithManyIncoming(getVertexValue())){
                outFlag = 0;
                sendSettledMsgs(toPredecessor, getVertexValue());
            }
            
            /** check outgoing **/
            // update internal state
            if (VertexUtil.isVertexWithOnlyOneOutgoing(getVertexValue())){
                byte state = 0;
                state |= State.HEAD_CAN_MERGEWITHNEXT;
                getVertexValue().setState(state);
                activate();
            } 
            // send to neighbors
            else if (VertexUtil.isVertexWithManyOutgoing(getVertexValue())){
                outFlag = 0;
                sendSettledMsgs(toSuccessor, getVertexValue());
            }
            
            if(VertexUtil.isUnMergeVertex(getVertexValue()))
                voteToHalt();
        }
    }

    public void setHeadMergeDir(){
        byte state = 0;
        byte meToNeighborDir = (byte) (incomingMsg.getFlag() & MessageFlag.DIR_MASK);
        byte neighborToMeDir = mirrorDirection(meToNeighborDir);
        switch(neighborToMeDir){
            case MessageFlag.DIR_FF:
            case MessageFlag.DIR_FR:
                state |= State.HEAD_CAN_MERGEWITHPREV;
                break;
            case MessageFlag.DIR_RF:
            case MessageFlag.DIR_RR:
                state |= State.HEAD_CAN_MERGEWITHNEXT;
                break;
        }
        getVertexValue().setState(state);
    }
    
    public boolean isHeadUnableToMerge(){
        byte state = (byte) (getVertexValue().getState() & State.HEAD_CAN_MERGE_MASK);
        return state == State.HEAD_CANNOT_MERGE;
    }
    
    /**
     * initiate head, rear and path node
     */
    public void initState(Iterator<M> msgIterator) {
        if(isInactiveNode())
            voteToHalt();
        else{
            while (msgIterator.hasNext()) {
                incomingMsg = msgIterator.next();
                switch(getHeadMergeDir()){
                    case State.NON_HEAD: // TODO Change name to Path
                        setHeadMergeDir();
                        activate();
                        break;
                    case State.HEAD_CAN_MERGEWITHPREV: // TODO aggregate all the incomingMsgs first, then make a decision about halting
                    case State.HEAD_CAN_MERGEWITHNEXT:
                        if (getHeadFlagAndMergeDir() != getMsgFlagAndMergeDir()){
                            getVertexValue().setState(State.HEAD_CANNOT_MERGE);
                            voteToHalt();
                        }
                        break;
                    case State.HEAD_CANNOT_MERGE:
                        voteToHalt();
                        break;
                }
            }
        }
    }
    
    public void setStateAsMergeDir(boolean mergeWithPre){
        short state = getVertexValue().getState();
        state &= State.CAN_MERGE_CLEAR;
        state |= mergeWithPre ? State.CAN_MERGEWITHPREV : State.CAN_MERGEWITHNEXT;
        getVertexValue().setState(state);
        activate();
    }
    
    public void setStateAsMergeWithPrev(){
        short state = getVertexValue().getState();
        state &= State.CAN_MERGE_CLEAR;
        state |= State.CAN_MERGEWITHPREV;
        getVertexValue().setState(state);
        activate();
    }
    
    public void setStateAsMergeWithNext(){
        short state = getVertexValue().getState();
        state &= State.CAN_MERGE_CLEAR;
        state |= State.CAN_MERGEWITHNEXT;
        getVertexValue().setState(state);
        activate();
    }
    
    /**
     * updateAdjList
     */
    public void processUpdate(M msg){
    	// A -> B -> C with B merging with C
        inFlag = msg.getFlag();
        byte meToNeighborDir = (byte) (inFlag & MessageFlag.DIR_MASK);  // A -> B dir
        byte neighborToMeDir = mirrorDirection(meToNeighborDir);  // B -> A dir
        
        // TODO if you want, this logic could be figured out when sending the update from B
        byte neighborToMergeDir = flipDirection(neighborToMeDir, msg.isFlip());  // A -> C after the merge
        byte replaceDir = mirrorDirection(neighborToMeDir); // C -> A dir
        getVertexValue().getNode().updateEdges(neighborToMeDir, msg.getSourceVertexId(), 
                neighborToMergeDir, replaceDir, msg.getNode(), true);
    }
    
    /**
     * final updateAdjList
     */
    public void processFinalUpdate(){
        inFlag = incomingMsg.getFlag();
        byte meToNeighborDir = (byte) (inFlag & MessageFlag.DIR_MASK);
        byte neighborToMeDir = mirrorDirection(meToNeighborDir);
        
        byte neighborToMergeDir = flipDirection(neighborToMeDir, incomingMsg.isFlip());
        getVertexValue().processFinalUpdates(neighborToMeDir, neighborToMergeDir, incomingMsg.getNode());
    }
    
    /**
     * final updateAdjList
     */
    public void processFinalUpdate2(){
        inFlag = incomingMsg.getFlag();
        byte meToNeighborDir = (byte) (inFlag & MessageFlag.DIR_MASK);
        byte neighborToMeDir = mirrorDirection(meToNeighborDir);
        
        EdgeWritable edge = new EdgeWritable();
        edge.setKey(incomingMsg.getSourceVertexId());
        edge.setReadIDs(incomingMsg.getNode().getEdgeList(meToNeighborDir).getReadIDs(getVertexId()));
        getVertexValue().getEdgeList(neighborToMeDir).unionAdd(edge);
    }
    
    public byte flipHeadMergeDir(byte d, boolean isFlip){
        if(isFlip){
            switch(d){
                case State.HEAD_CAN_MERGEWITHPREV:
                    return State.HEAD_CAN_MERGEWITHNEXT;
                case State.HEAD_CAN_MERGEWITHNEXT:
                    return State.HEAD_CAN_MERGEWITHPREV;
                    default:
                        return 0;
            }
        } else
            return d;
    }
    
    public boolean isDifferentDirWithMergeKmer(byte neighborToMeDir){
        return neighborToMeDir == MessageFlag.DIR_FR || neighborToMeDir == MessageFlag.DIR_RF;
    }
    
    /**
     * check if head receives message from head
     */
    public boolean isHeadMeetsHead(boolean selfFlag){
        boolean msgFlag = (getMsgMergeDir() == MessageFlag.HEAD_CAN_MERGEWITHPREV || getMsgMergeDir() == MessageFlag.HEAD_CAN_MERGEWITHNEXT);
        return selfFlag && msgFlag;
    }
    
    /**
     * check if non-head receives message from head 
     */
    public boolean isNonHeadReceivedFromHead(){
        boolean selfFlag = (getHeadMergeDir() == State.HEAD_CAN_MERGEWITHPREV || getHeadMergeDir() == State.HEAD_CAN_MERGEWITHNEXT);
        boolean msgFlag = (getMsgMergeDir() == MessageFlag.HEAD_CAN_MERGEWITHPREV || getMsgMergeDir() == MessageFlag.HEAD_CAN_MERGEWITHNEXT);
        return selfFlag == false && msgFlag == true;
    }
    /**
     * merge and updateAdjList  having parameter
     */
    public void processMerge(PathMergeMessageWritable msg){
        inFlag = msg.getFlag();
        byte meToNeighborDir = (byte) (inFlag & MessageFlag.DIR_MASK);
        byte neighborToMeDir = mirrorDirection(meToNeighborDir);

        if(isNonHeadReceivedFromHead()){ // TODO? why sepcial-case the path vs heads?  just aggregate your state flags
            short state = getVertexValue().getState();
            state &= State.HEAD_CAN_MERGE_CLEAR;
            byte headMergeDir = flipHeadMergeDir((byte)(inFlag & MessageFlag.HEAD_CAN_MERGE_MASK), isDifferentDirWithMergeKmer(neighborToMeDir));
            state |= headMergeDir;
            getVertexValue().setState(state);
        }
        
        getVertexValue().processMerges(neighborToMeDir, msg.getNode(), kmerSize);
    }
    
    
    /**
     * send UPDATE msg   boolean: true == P4, false == P2
     */
    public void sendUpdateMsg(boolean isP4, boolean toPredecessor){ 
        outgoingMsg.setSourceVertexId(getVertexId());
        // TODO pass in the vertexId rather than isP4 (removes this block）
        if(isP4)
            outgoingMsg.setFlip(ifFlipWithNeighbor(!toPredecessor)); //ifFilpWithSuccessor()
        else 
            outgoingMsg.setFlip(ifFilpWithSuccessor(incomingMsg.getSourceVertexId()));
        
        byte[] mergeDirs = toPredecessor ? OutgoingListFlag.values : IncomingListFlag.values;
        byte[] updateDirs = toPredecessor ? IncomingListFlag.values : OutgoingListFlag.values;
        
        for(byte dir : mergeDirs)
            outgoingMsg.getNode().setEdgeList(dir, getVertexValue().getEdgeList(dir));
        
        for(byte dir : updateDirs){
            kmerIterator = getVertexValue().getEdgeList(dir).getKeys();
            while(kmerIterator.hasNext()){
                outFlag &= MessageFlag.DIR_CLEAR;
                outFlag |= dir;
                outgoingMsg.setFlag(outFlag);
                destVertexId.setAsCopy(kmerIterator.next()); //TODO does destVertexId need deep copy?
                sendMsg(destVertexId, outgoingMsg);
            }
        }
    }

    /**
     * override sendUpdateMsg and use incomingMsg as parameter automatically
     */
    public void sendUpdateMsg(){
        sendUpdateMsgForP2(incomingMsg);
    }
    
    public void sendFinalUpdateMsg(){
        outFlag |= MessageFlag.IS_FINAL;
        sendUpdateMsgForP2(incomingMsg);
    }
    
    /**
     * send update message to neighber for P2
     */
    public void sendUpdateMsgForP2(MessageWritable msg){
        outgoingMsg.reset();
        outgoingMsg.setUpdateMsg(true);
        byte meToNeighborDir = (byte) (msg.getFlag() & MessageFlag.DIR_MASK);
        byte neighborToMeDir = mirrorDirection(meToNeighborDir);
        switch(neighborToMeDir){
            case MessageFlag.DIR_FF:
            case MessageFlag.DIR_FR:
                sendUpdateMsg(isP2, toPredecessor);
                break;
            case MessageFlag.DIR_RF:
            case MessageFlag.DIR_RR: 
                sendUpdateMsg(isP2, toSuccessor);
                break;
        }
    }
    
    public void headSendUpdateMsg(){
        outgoingMsg.reset();
        outgoingMsg.setUpdateMsg(true);
        switch(getVertexValue().getState() & MessageFlag.HEAD_CAN_MERGE_MASK){
            case MessageFlag.HEAD_CAN_MERGEWITHPREV:
                sendUpdateMsg(isP2, toSuccessor);
                break;
            case MessageFlag.HEAD_CAN_MERGEWITHNEXT:
                sendUpdateMsg(isP2, toPredecessor);
                break;
        }
    }
    
    public void sendMergeMsgToSuccessor(){
        setNeighborToMeDir(successorToMe);
        if(ifFlipWithPredecessor())
            outgoingMsg.setFlip(true);
        else
            outgoingMsg.setFlip(false);
        outgoingMsg.setFlag(outFlag);
//        outgoingMsg.setNeighberNode(getVertexValue().getIncomingList());
        for(byte d: IncomingListFlag.values)
        	outgoingMsg.setEdgeList(d, getVertexValue().getEdgeList(d));
        outgoingMsg.setSourceVertexId(getVertexId());
        outgoingMsg.setInternalKmer(getVertexValue().getInternalKmer());
        sendMsg(getNextDestVertexId(), outgoingMsg);
    }
    
    public boolean canMergeWithHead(MessageWritable msg){
        byte meToNeighborDir = (byte) (msg.getFlag() & MessageFlag.DIR_MASK);
        byte neighborToMeDir = mirrorDirection(meToNeighborDir);
        switch(neighborToMeDir){
            case MessageFlag.DIR_FF:
            case MessageFlag.DIR_FR:
                return getVertexValue().outDegree() == 1;
            case MessageFlag.DIR_RF:
            case MessageFlag.DIR_RR:
                return getVertexValue().inDegree() == 1;    
        }
        return false;
    }
    
    public void sendMergeMsgByIncomingMsgDir(){
        byte meToNeighborDir = (byte) (incomingMsg.getFlag() & MessageFlag.DIR_MASK);
        byte neighborToMeDir = mirrorDirection(meToNeighborDir);
        switch(neighborToMeDir){
            case MessageFlag.DIR_FF:
            case MessageFlag.DIR_FR:
                configureMergeMsgForSuccessor(incomingMsg.getSourceVertexId());
                break;
            case MessageFlag.DIR_RF:
            case MessageFlag.DIR_RR:
                configureMergeMsgForPredecessor(incomingMsg.getSourceVertexId()); 
                break; 
        }
    }
    
    /**
     * send MERGE msg
     */
    public void sendMergeMsg(boolean toPredecessor, VKmerBytesWritable mergeDest){
        setNeighborToMeDir(predecessorToMe);
        outgoingMsg.setFlag(outFlag);
        outgoingMsg.setSourceVertexId(getVertexId());
//        for(byte d: OutgoingListFlag.values)
//            outgoingMsg.setEdgeList(d, getVertexValue().getEdgeList(d));
        outgoingMsg.setNode(getVertexValue().getNode());
        sendMsg(mergeDest, outgoingMsg);
    }
    
    /**
     * configure MERGE msg  TODO: delete edgelist, merge configureMergeMsgForPredecessor and configureMergeMsgForPredecessorByIn...
     */
    public void configureMergeMsgForPredecessor(VKmerBytesWritable mergeDest){
        setNeighborToMeDir(predecessorToMe);
        outgoingMsg.setFlag(outFlag);
        outgoingMsg.setSourceVertexId(getVertexId());
//        for(byte d: OutgoingListFlag.values)
//            outgoingMsg.setEdgeList(d, getVertexValue().getEdgeList(d));
        outgoingMsg.setNode(getVertexValue().getNode());
        sendMsg(mergeDest, outgoingMsg);
    }
    
    public void configureMergeMsgForSuccessor(VKmerBytesWritable mergeDest){
        setNeighborToMeDir(successorToMe);
        outgoingMsg.setFlag(outFlag);
        outgoingMsg.setSourceVertexId(getVertexId());
//        for(byte d: IncomingListFlag.values)
//            outgoingMsg.setEdgeList(d, getVertexValue().getEdgeList(d));
        outgoingMsg.setNode(getVertexValue().getNode());
        sendMsg(mergeDest, outgoingMsg);
    }
    
    /**
     * send merge message to neighber for P4, send message to the merge object and kill self
     */
    public void broadcastMergeMsg(boolean isP4){
        outFlag |= getHeadMergeDir();
        switch(getVertexValue().getState() & State.CAN_MERGE_MASK) {
            case State.CAN_MERGEWITHNEXT:
                // configure merge msg for successor
                configureMergeMsgForSuccessor(getNextDestVertexId()); // TODO getDestVertexId(DIRECTION), then remove the switch statement, sendMergeMsg(DIRECTION)
                if(isP4)
                    deleteVertex(getVertexId());
                else{
                    getVertexValue().setState(State.IS_DEAD);
                    activate();
                }
                break;
            case State.CAN_MERGEWITHPREV:
                // configure merge msg for predecessor
                configureMergeMsgForPredecessor(getPrevDestVertexId());
                if(isP4)
                    deleteVertex(getVertexId());
                else{
                    getVertexValue().setState(State.IS_DEAD);
                    activate();
                }
                break; 
        }
    }
    
    public byte revertHeadMergeDir(byte headMergeDir){
        switch(headMergeDir){
            case MessageFlag.HEAD_CAN_MERGEWITHPREV:
                return MessageFlag.HEAD_CAN_MERGEWITHNEXT;
            case MessageFlag.HEAD_CAN_MERGEWITHNEXT:
                return MessageFlag.HEAD_CAN_MERGEWITHPREV;
        }
        return 0;
        
    }
    
    /**
     * Logging the vertexId and vertexValue 
     */
    public void loggingNode(byte loggingType){
        String logMessage = LogUtil.getVertexLog(loggingType, getSuperstep(), getVertexId(), getVertexValue());
        logger.fine(logMessage);
    }
    
    /**
     * Logging message
     */
    public void loggingMessage(byte loggingType, PathMergeMessageWritable msg, VKmerBytesWritable dest){
        String logMessage = LogUtil.getMessageLog(loggingType, getSuperstep(), getVertexId(), msg, dest);
        logger.fine(logMessage);
    }
}
