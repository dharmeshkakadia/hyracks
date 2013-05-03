package edu.uci.ics.genomix.pregelix.operator;

import java.util.Iterator;

import org.apache.hadoop.io.NullWritable;

import edu.uci.ics.pregelix.api.graph.Vertex;
import edu.uci.ics.genomix.pregelix.io.LogAlgorithmMessageWritable;
import edu.uci.ics.genomix.pregelix.io.ValueStateWritable;
import edu.uci.ics.genomix.pregelix.type.Message;
import edu.uci.ics.genomix.pregelix.type.State;
import edu.uci.ics.genomix.pregelix.util.GraphVertexOperation;
import edu.uci.ics.genomix.type.GeneCode;
import edu.uci.ics.genomix.type.KmerBytesWritable;
import edu.uci.ics.genomix.type.VKmerBytesWritable;
import edu.uci.ics.genomix.type.VKmerBytesWritableFactory;

/*
 * vertexId: BytesWritable
 * vertexValue: ValueStateWritable
 * edgeValue: NullWritable
 * message: LogAlgorithmMessageWritable
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
 * For example, ONE LINE in input file: 00,01,10	0001,0010,
 * That means that vertexId is ACG, its succeed node is A and its precursor node is C.
 * The succeed node and precursor node will be stored in vertexValue and we don't use edgeValue.
 * The details about message are in edu.uci.ics.pregelix.example.io.MessageWritable. 
 */
public class LogFilterVertex extends Vertex<KmerBytesWritable, ValueStateWritable, NullWritable, LogAlgorithmMessageWritable>{	
	
	public static final String KMER_SIZE = "TwoStepLogAlgorithmForPathMergeVertex.kmerSize";
	public static final String ITERATIONS = "TwoStepLogAlgorithmForPathMergeVertex.iteration";
	public static int kmerSize = -1;
	private int maxIteration = -1;
	
	private LogAlgorithmMessageWritable msg = new LogAlgorithmMessageWritable();
	
	private VKmerBytesWritableFactory kmerFactory = new VKmerBytesWritableFactory(1);
	private VKmerBytesWritable destVertexId = new VKmerBytesWritable(1); 
	private VKmerBytesWritable chainVertexId = new VKmerBytesWritable(1);
	private VKmerBytesWritable lastKmer = new VKmerBytesWritable(1);
	/**
	 * initiate kmerSize, maxIteration
	 */
	public void initVertex(){
		if(kmerSize == -1)
			kmerSize = getContext().getConfiguration().getInt(KMER_SIZE, 5);
        if (maxIteration < 0) 
            maxIteration = getContext().getConfiguration().getInt(ITERATIONS, 1000000);
	}
	/**
	 * get destination vertex
	 */
	public VKmerBytesWritable getNextDestVertexId(KmerBytesWritable vertexId, byte geneCode){
		return kmerFactory.shiftKmerWithNextCode(vertexId, geneCode);
	}
	
	public VKmerBytesWritable getPreDestVertexId(KmerBytesWritable vertexId, byte geneCode){
		return kmerFactory.shiftKmerWithPreCode(vertexId, geneCode);
	}
	
	public VKmerBytesWritable getNextDestVertexIdFromBitmap(KmerBytesWritable chainVertexId, byte adjMap){
		return getDestVertexIdFromChain(chainVertexId, adjMap);
	}
	
	public VKmerBytesWritable getDestVertexIdFromChain(KmerBytesWritable chainVertexId, byte adjMap){
		lastKmer.set(kmerFactory.getLastKmerFromChain(kmerSize, chainVertexId));
		return getNextDestVertexId(lastKmer, GeneCode.getGeneCodeFromBitMap((byte)(adjMap & 0x0F)));
	}
	/**
	 * head send message to all next nodes
	 */
	public void sendMsgToAllNextNodes(KmerBytesWritable vertexId, byte adjMap){
		for(byte x = GeneCode.A; x<= GeneCode.T ; x++){
			if((adjMap & (1 << x)) != 0){
				destVertexId.set(getNextDestVertexId(vertexId, x));
				sendMsg(destVertexId, msg);
			}
		}
	}
	/**
	 * head send message to all previous nodes
	 */
	public void sendMsgToAllPreviousNodes(KmerBytesWritable vertexId, byte adjMap){
		for(byte x = GeneCode.A; x<= GeneCode.T ; x++){
			if(((adjMap >> 4) & (1 << x)) != 0){
				destVertexId.set(getPreDestVertexId(vertexId, x));
				sendMsg(destVertexId, msg);
			}
		}
	}

	/**
	 * set vertex state
	 */
	public void setState(){
		if(msg.getMessage() == Message.START && 
				(getVertexValue().getState() == State.MID_VERTEX || getVertexValue().getState() == State.END_VERTEX)){
			getVertexValue().setState(State.START_VERTEX);
			setVertexValue(getVertexValue());
		}
		else if(msg.getMessage() == Message.END && getVertexValue().getState() == State.MID_VERTEX){
			getVertexValue().setState(State.END_VERTEX);
			setVertexValue(getVertexValue());
			voteToHalt();
		}
		else
			voteToHalt();
	}
	/**
	 * send start message to next node
	 */
	public void sendStartMsgToNextNode(){
		msg.reset();
		msg.setMessage(Message.START);
		msg.setSourceVertexId(getVertexId());
		sendMsg(destVertexId, msg);
		voteToHalt();
	}
	/**
	 * send end message to next node
	 */
	public void sendEndMsgToNextNode(){
		msg.reset();
		msg.setMessage(Message.END);
		msg.setSourceVertexId(getVertexId());
		sendMsg(destVertexId, msg);
		voteToHalt();
	}
	/**
	 * send non message to next node
	 */
	public void sendNonMsgToNextNode(){
		msg.setMessage(Message.NON);
		msg.setSourceVertexId(getVertexId());
		sendMsg(destVertexId, msg);
	}
	/**
	 * head send message to path
	 */
	public void sendMsgToPathVertex(KmerBytesWritable chainVertexId, byte adjMap){
		if(GeneCode.getGeneCodeFromBitMap((byte)(getVertexValue().getAdjMap() & 0x0F)) == -1
				|| getVertexValue().getState() == State.FINAL_VERTEX) //|| lastKmer == null
			voteToHalt();
		else{
			destVertexId.set(getNextDestVertexIdFromBitmap(chainVertexId, adjMap));
			if(getVertexValue().getState() == State.START_VERTEX){
				sendStartMsgToNextNode();
			}
			else if(getVertexValue().getState() != State.END_VERTEX && getVertexValue().getState() != State.FINAL_DELETE){
				sendEndMsgToNextNode();
			}
		}
	}
	/**
	 * path send message to head 
	 */
	public void responseMsgToHeadVertex(){
		if(getVertexValue().getLengthOfMergeChain() == 0){
			getVertexValue().setMergeChain(getVertexId());
			setVertexValue(getVertexValue());
		}
		destVertexId.set(msg.getSourceVertexId());
		msg.set(null, getVertexValue().getMergeChain(), getVertexValue().getAdjMap(), msg.getMessage());
		setMessageType(msg.getMessage());
		sendMsg(destVertexId,msg);
	}
	/**
	 * set message type
	 */
	public void setMessageType(int message){
		//kill Message because it has been merged by the head
		if(getVertexValue().getState() == State.END_VERTEX || getVertexValue().getState() == State.FINAL_DELETE){
			msg.setMessage(Message.END);
			getVertexValue().setState(State.FINAL_DELETE);
			setVertexValue(getVertexValue());
		}
		else
			msg.setMessage(Message.NON);
		
		if(message == Message.START){
			deleteVertex(getVertexId());
		}
	}
	/**
	 *  set vertexValue's state chainVertexId, value
	 */
	public boolean setVertexValueAttributes(){
		if(msg.getMessage() == Message.END){
			if(getVertexValue().getState() != State.START_VERTEX)
				getVertexValue().setState(State.END_VERTEX);
			else
				getVertexValue().setState(State.FINAL_VERTEX);
		}
			
		if(getSuperstep() == 5)
			chainVertexId.set(getVertexId());
		else
			chainVertexId.set(getVertexValue().getMergeChain());
		lastKmer.set(kmerFactory.getLastKmerFromChain(msg.getLengthOfChain() - kmerSize + 1, msg.getChainVertexId()));
		chainVertexId.set(kmerFactory.mergeTwoKmer(chainVertexId, lastKmer));
		if(GraphVertexOperation.isCycle(getVertexId(), chainVertexId)){
			getVertexValue().setMergeChain(null);
			getVertexValue().setAdjMap(GraphVertexOperation.reverseAdjMap(getVertexValue().getAdjMap(),
					chainVertexId.getGeneCodeAtPosition(kmerSize)));
			getVertexValue().setState(State.CYCLE);
			return false;
		}
		else
			getVertexValue().setMergeChain(chainVertexId);
		
		byte tmpVertexValue = GraphVertexOperation.updateRightNeighber(getVertexValue().getAdjMap(), msg.getAdjMap());
		getVertexValue().setAdjMap(tmpVertexValue);
		return true;
	}
	/**
	 *  send message to self
	 */
	public void sendMsgToSelf(){
		if(msg.getMessage() != Message.END){
			setVertexValue(getVertexValue());
			msg.reset(); //reset
			msg.setAdjMap(getVertexValue().getAdjMap());
			sendMsg(getVertexId(),msg);
		}
	}
	/**
	 * start sending message 
	 */
	public void startSendMsg(){
		if(GraphVertexOperation.isHeadVertex(getVertexValue().getAdjMap())){
			msg.set(null, null, (byte)0, Message.START);
			sendMsgToAllNextNodes(getVertexId(), getVertexValue().getAdjMap());
			voteToHalt();
		}
		if(GraphVertexOperation.isRearVertex(getVertexValue().getAdjMap())){
			msg.set(null, null, (byte)0, Message.END);
			sendMsgToAllPreviousNodes(getVertexId(), getVertexValue().getAdjMap());
			voteToHalt();
		}
		if(GraphVertexOperation.isPathVertex(getVertexValue().getAdjMap())){
			getVertexValue().setState(State.MID_VERTEX);
			setVertexValue(getVertexValue());
		}
	}
	/**
	 *  initiate head, rear and path node
	 */
	public void initState(Iterator<LogAlgorithmMessageWritable> msgIterator){
		while(msgIterator.hasNext()){
			if(!GraphVertexOperation.isPathVertex(getVertexValue().getAdjMap())){
				msgIterator.next();
				voteToHalt();
			}
			else{
				msg = msgIterator.next();
				setState();
			}
		}
	}
	/**
	 * head send message to path
	 */
	public void sendMsgToPathVertex(Iterator<LogAlgorithmMessageWritable> msgIterator){
		if(getSuperstep() == 3){
			sendMsgToPathVertex(getVertexId(), getVertexValue().getAdjMap());
		}
		else{
			if(msgIterator.hasNext()){
				msg = msgIterator.next();
				if(mergeChainVertex(msgIterator))
					sendMsgToPathVertex(getVertexValue().getMergeChain(), getVertexValue().getAdjMap());
				else
					voteToHalt();
			}
			if(getVertexValue().getState() == State.END_VERTEX || getVertexValue().getState() == State.FINAL_DELETE){
				voteToHalt();
			}
			if(getVertexValue().getState() == State.FINAL_VERTEX){
				//String source = getVertexValue().getMergeChain().toString();
				voteToHalt();
			}
		}
	}
	/**
	 * path response message to head
	 */
	public void responseMsgToHeadVertex(Iterator<LogAlgorithmMessageWritable> msgIterator){
		if(msgIterator.hasNext()){		
			msg = msgIterator.next();
			responseMsgToHeadVertex();
		}
		else{
			if(getVertexValue().getState() != State.START_VERTEX 
					&& getVertexValue().getState() != State.END_VERTEX && getVertexValue().getState() != State.FINAL_DELETE){
				deleteVertex(getVertexId());//killSelf because it doesn't receive any message
			}
		}
	}
	/**
	 * merge chainVertex and store in vertexVal.chainVertexId
	 */
	public boolean mergeChainVertex(Iterator<LogAlgorithmMessageWritable> msgIterator){
		return setVertexValueAttributes();
	}
	
	@Override
	public void compute(Iterator<LogAlgorithmMessageWritable> msgIterator) {
		initVertex();
		if (getSuperstep() == 1){
			if(getVertexId().toString().equals("AAGAC") 
					|| getVertexId().toString().equals("AGCAC")){
				startSendMsg();
			}
		}
		else if(getSuperstep() == 2){
			initState(msgIterator);
			if(getVertexValue().getState() == State.NON_VERTEX)
				voteToHalt();
		}
		else if(getSuperstep()%2 == 1 && getSuperstep() <= maxIteration){
			sendMsgToPathVertex(msgIterator);
		}
		else if(getSuperstep()%2 == 0 && getSuperstep() <= maxIteration){
			responseMsgToHeadVertex(msgIterator);
		}
		else
			voteToHalt();
	}
}