package edu.uci.ics.pregelix;

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.ByteWritable;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.SequenceFile.CompressionType;

import edu.uci.ics.pregelix.SequenceFile.GenerateSequenceFile;
import edu.uci.ics.pregelix.bitwise.BitwiseOperation;
import edu.uci.ics.pregelix.example.io.LogAlgorithmMessageWritable;
import edu.uci.ics.pregelix.example.io.MessageWritable;
import edu.uci.ics.pregelix.example.io.ValueStateWritable;
import edu.uci.ics.pregelix.hdfs.HDFSOperation;
import edu.uci.ics.pregelix.type.KmerCountValue;

public class GraphVertexOperation {
	public static final int k = 3; //kmer, k: the length of kmer
	static private final Path TMP_DIR = new Path(
			GenerateSequenceFile.class.getSimpleName() + "_INTERIM");
	/**
	 * Single Vertex: in-degree = out-degree = 1
	 * @param vertexValue 
	 */
	public static boolean isPathVertex(ByteWritable vertexValue){
		byte value = vertexValue.get();
		byte[] bit = new byte[8];
		for(int i = 0; i < 8; i++)
			bit[i] = (byte) ((value >> i) & 0x01);
		
		//check out-degree
		if(((bit[0]==1)&&(bit[1]==0)&&(bit[2]==0)&&(bit[3]==0))
				|| ((bit[0]==0)&&(bit[1]==1)&&(bit[2]==0)&&(bit[3]==0))
				|| ((bit[0]==0)&&(bit[1]==0)&&(bit[2]==1)&&(bit[3]==0))
				|| ((bit[0]==0)&&(bit[1]==0)&&(bit[2]==0)&&(bit[3]==1))
				){
			//check in-degree
			if(((bit[4]==1)&&(bit[5]==0)&&(bit[6]==0)&&(bit[7]==0))
					|| ((bit[4]==0)&&(bit[5]==1)&&(bit[6]==0)&&(bit[7]==0))
					|| ((bit[4]==0)&&(bit[5]==0)&&(bit[6]==1)&&(bit[7]==0))
					|| ((bit[4]==0)&&(bit[5]==0)&&(bit[6]==0)&&(bit[7]==1))
					)
				return true;
			else
				return false;
		}
		else
			return false;
	}
	/**
	 * Head Vertex:  out-degree = 1, in-degree != 1
	 * @param vertexValue 
	 */
	public static boolean isHead(ByteWritable vertexValue){
		byte value = vertexValue.get();
		byte[] bit = new byte[8];
		for(int i = 0; i < 8; i++)
			bit[i] = (byte) ((value >> i) & 0x01);
		
		//check out-degree
		if(((bit[0]==1)&&(bit[1]==0)&&(bit[2]==0)&&(bit[3]==0))
				|| ((bit[0]==0)&&(bit[1]==1)&&(bit[2]==0)&&(bit[3]==0))
				|| ((bit[0]==0)&&(bit[1]==0)&&(bit[2]==1)&&(bit[3]==0))
				|| ((bit[0]==0)&&(bit[1]==0)&&(bit[2]==0)&&(bit[3]==1))
				){
			//check in-degree
			if(!((bit[4]==1)&&(bit[5]==0)&&(bit[6]==0)&&(bit[7]==0))
					&& !((bit[4]==0)&&(bit[5]==1)&&(bit[6]==0)&&(bit[7]==0))
					&& !((bit[4]==0)&&(bit[5]==0)&&(bit[6]==1)&&(bit[7]==0))
					&& !((bit[4]==0)&&(bit[5]==0)&&(bit[6]==0)&&(bit[7]==1))
					)
				return true;
			else
				return false;
		}
		else
			return false;
	}
	/**
	 * Rear Vertex:  out-degree != 1, in-degree = 1
	 * @param vertexValue 
	 */
	public static boolean isRear(ByteWritable vertexValue){
		byte value = vertexValue.get();
		byte[] bit = new byte[8];
		for(int i = 0; i < 8; i++)
			bit[i] = (byte) ((value >> i) & 0x01);
		
		//check out-degree
		if(!((bit[0]==1)&&(bit[1]==0)&&(bit[2]==0)&&(bit[3]==0))
				&& !((bit[0]==0)&&(bit[1]==1)&&(bit[2]==0)&&(bit[3]==0))
				&& !((bit[0]==0)&&(bit[1]==0)&&(bit[2]==1)&&(bit[3]==0))
				&& !((bit[0]==0)&&(bit[1]==0)&&(bit[2]==0)&&(bit[3]==1))
				){
			//check in-degree
			if(((bit[4]==1)&&(bit[5]==0)&&(bit[6]==0)&&(bit[7]==0))
					|| ((bit[4]==0)&&(bit[5]==1)&&(bit[6]==0)&&(bit[7]==0))
					|| ((bit[4]==0)&&(bit[5]==0)&&(bit[6]==1)&&(bit[7]==0))
					|| ((bit[4]==0)&&(bit[5]==0)&&(bit[6]==0)&&(bit[7]==1))
					)
				return true;
			else
				return false;
		}
		else
			return false;
	}
	/**
	 * write Kmer to Sequence File for test
	 * @param arrayOfKeys
	 * @param arrayOfValues
	 * @param step
	 * @throws IOException
	 */
	public void writeKmerToSequenceFile(ArrayList<BytesWritable> arrayOfKeys, ArrayList<ByteWritable> arrayOfValues, long step) throws IOException{
		
		Configuration conf = new Configuration();
	    Path outDir = new Path(TMP_DIR, "out");
	    Path outFile = new Path(outDir, "B" + Long.toString(step));
	    FileSystem fileSys = FileSystem.get(conf);
	    SequenceFile.Writer writer = SequenceFile.createWriter(fileSys, conf,
	        outFile, BytesWritable.class, ByteWritable.class, 
	        CompressionType.NONE);
	    
	     //wirte to sequence file
	     for(int i = 0; i < arrayOfKeys.size(); i++)
	    	 writer.append(arrayOfKeys.get(i), arrayOfValues.get(i));
	     writer.close();
	}
	/**
	 * check what kind of succeed node
	 * return 0:A 1:C 2:G 3:T 4:nothing
	 */
	public static int findSucceedNode(byte vertexValue){
		String firstBit = "00000001"; //A
		String secondBit = "00000010"; //C
		String thirdBit = "00000100"; //G
		String fourthBit = "00001000"; //T
		int first = BitwiseOperation.convertBinaryStringToByte(firstBit) & 0xff;
		int second = BitwiseOperation.convertBinaryStringToByte(secondBit) & 0xff;
		int third = BitwiseOperation.convertBinaryStringToByte(thirdBit) & 0xff;
		int fourth = BitwiseOperation.convertBinaryStringToByte(fourthBit) & 0xff;
		int value = vertexValue & 0xff;
		int tmp = value & first;
		if(tmp != 0)
			return 0;
		else{
			tmp = value & second;
			if(tmp != 0)
				return 1;
			else{
				tmp = value & third;
				if(tmp != 0)
					return 2;
				else{
					tmp = value & fourth;
					if(tmp != 0)
						return 3;
					else
						return 4;
				}
			}
		}
	}
	/**
	 * check what kind of precursor node
	 * return 0:A 1:C 2:G 3:T 4:nothing
	 */
	public static int findPrecursorNode(byte vertexValue){
		String firstBit = "00010000"; //A
		String secondBit = "00100000"; //C
		String thirdBit = "01000000"; //G
		String fourthBit = "10000000"; //T
		int first = BitwiseOperation.convertBinaryStringToByte(firstBit) & 0xff;
		int second = BitwiseOperation.convertBinaryStringToByte(secondBit) & 0xff;
		int third = BitwiseOperation.convertBinaryStringToByte(thirdBit) & 0xff;
		int fourth = BitwiseOperation.convertBinaryStringToByte(fourthBit) & 0xff;
		int value = vertexValue & 0xff;
		int tmp = value & first;
		if(tmp != 0)
			return 0;
		else{
			tmp = value & second;
			if(tmp != 0)
				return 1;
			else{
				tmp = value & third;
				if(tmp != 0)
					return 2;
				else{
					tmp = value & fourth;
					if(tmp != 0)
						return 3;
					else
						return 4;
				}
			}
		}
	}
	/**
	 * replace last two bits based on n
	 * Ex. 01 10 00(nothing)	->	01 10 00(A)/01(C)/10(G)/11(T)		
	 */
	public static byte[] replaceLastTwoBits(byte[] vertexId, int n){
		String binaryStringVertexId = BitwiseOperation.convertBytesToBinaryStringKmer(vertexId, k);
		String resultString = "";
		for(int i = 0; i < binaryStringVertexId.length()-2; i++)
			resultString += binaryStringVertexId.charAt(i);
		switch(n){
		case 0:
			resultString += "00";
			break;
		case 1:
			resultString += "01";
			break;
		case 2:
			resultString += "10";
			break;
		case 3:
			resultString += "11";
			break;
		default:
			break;
		}
	
		return BitwiseOperation.convertBinaryStringToBytes(resultString);
	}
	/**
	 * replace first two bits based on n
	 * Ex. 01 10 00(nothing)	->	00(A)/01(C)/10(G)/11(T) 10 00	
	 */
	public static byte[] replaceFirstTwoBits(byte[] vertexId, int n){
		String binaryStringVertexId = BitwiseOperation.convertBytesToBinaryStringKmer(vertexId, k);
		String resultString = "";
		switch(n){
		case 0:
			resultString += "00";
			break;
		case 1:
			resultString += "01";
			break;
		case 2:
			resultString += "10";
			break;
		case 3:
			resultString += "11";
			break;
		default:
			break;
		}
		for(int i = 2; i < binaryStringVertexId.length(); i++)
			resultString += binaryStringVertexId.charAt(i);
		return BitwiseOperation.convertBinaryStringToBytes(resultString);
	}
	/**
	 * find the vertexId of the destination node - left neighber
	 */
	public static byte[] getDestVertexId(byte[] sourceVertexId, byte vertexValue){
		byte[] destVertexId = BitwiseOperation.shiftBitsLeft(sourceVertexId, 2);
		return replaceLastTwoBits(destVertexId, findSucceedNode(vertexValue));
	}
	/**
	 * find the vertexId of the destination node - right neighber
	 */
	public static byte[] getLeftDestVertexId(byte[] sourceVertexId, byte vertexValue){
		byte[] destVertexId = BitwiseOperation.shiftBitsRight(sourceVertexId, 2);
		return replaceFirstTwoBits(destVertexId, findPrecursorNode(vertexValue));
	}
	/**
	 * update the chain vertexId
	 */
	public static byte[] updateChainVertexId(byte[] chainVertexId, int lengthOfChainVertex, byte[] newVertexId){
		return BitwiseOperation.addLastTwoBits(chainVertexId,lengthOfChainVertex,BitwiseOperation.getLastTwoBits(newVertexId,k));
	}
	/**
	 * get the first kmer from chainVertexId
	 */
	public static byte[] getFirstKmer(byte[] chainVertexId){
		String originalVertexId = BitwiseOperation.convertBytesToBinaryString(chainVertexId);
		return BitwiseOperation.convertBinaryStringToBytes(originalVertexId.substring(0,k-1));
	}
	/**
	 * get the last kmer from chainVertexId
	 */
	public static byte[] getLastKmer(byte[] chainVertexId, int lengthOfChainVertex){
		String originalVertexId = BitwiseOperation.convertBytesToBinaryString(chainVertexId);
		return BitwiseOperation.convertBinaryStringToBytes(originalVertexId.substring(2*(lengthOfChainVertex-k),2*lengthOfChainVertex));
	}
	/**
	 * read vertexId from RecordReader
	 */
	public static BytesWritable readVertexIdFromRecordReader(BytesWritable currentKey){
		String finalBinaryString = BitwiseOperation.convertBytesToBinaryStringKmer(currentKey.getBytes(),k);
		return new BytesWritable(BitwiseOperation.convertBinaryStringToBytes(finalBinaryString));
	}
	/**
	 * merge two BytesWritable. Ex. merge two vertexId
	 */
	public static byte[] mergeTwoChainVertex(byte[] b1, int length, byte[] b2){
		String s2 = BitwiseOperation.convertBytesToBinaryString(b2).substring(2*k-2,2*k);
		return BitwiseOperation.mergeTwoBytesArray(b1, length, BitwiseOperation.convertBinaryStringToBytes(s2), 1);
	}
	/**
	 * update right neighber
	 */
	public static byte updateRightNeighber(byte oldVertexValue, byte newVertexValue){
		return BitwiseOperation.replaceLastFourBits(oldVertexValue, newVertexValue);
	}
	/**
	 * update right neighber based on next vertexId
	 */
	public static byte updateRightNeighberByVertexId(byte oldVertexValue, byte[] neighberVertexId){
		String oldVertex = BitwiseOperation.convertByteToBinaryString(oldVertexValue);
		String neighber = BitwiseOperation.convertBytesToBinaryStringKmer(neighberVertexId, k);
		String lastTwoBits = neighber.substring(2*k-2,2*k);
		if(lastTwoBits.compareTo("00") == 0)
			return BitwiseOperation.convertBinaryStringToByte(oldVertex.substring(0,4) + "0001");
		else if(lastTwoBits.compareTo("01") == 0)
			return BitwiseOperation.convertBinaryStringToByte(oldVertex.substring(0,4) + "0010");
		else if(lastTwoBits.compareTo("10") == 0)
			return BitwiseOperation.convertBinaryStringToByte(oldVertex.substring(0,4) + "0100");
		else if(lastTwoBits.compareTo("11") == 0)
			return BitwiseOperation.convertBinaryStringToByte(oldVertex.substring(0,4) + "1000");
		
		return (Byte) null;
	}
	/**
	 * get precursor in vertexValue from gene code
	 */
	public static byte getPrecursorFromGeneCode(byte vertexValue, char precursor){
		String oldVertex = BitwiseOperation.convertByteToBinaryString(vertexValue);
		switch(precursor){
		case 'A':
			return BitwiseOperation.convertBinaryStringToByte("0001" + oldVertex.substring(0,4));
		case 'C':
			return BitwiseOperation.convertBinaryStringToByte("0010" + oldVertex.substring(0,4));
		case 'G':
			return BitwiseOperation.convertBinaryStringToByte("0100" + oldVertex.substring(0,4));
		case 'T':
			return BitwiseOperation.convertBinaryStringToByte("1000" + oldVertex.substring(0,4));
			default:
				return (Byte) null;
		}
	}
	/**
	 * get succeed in vertexValue from gene code
	 */
	public static byte getSucceedFromGeneCode(byte vertexValue, char succeed){
		String oldVertex = BitwiseOperation.convertByteToBinaryString(vertexValue);
		switch(succeed){
		case 'A':
			return BitwiseOperation.convertBinaryStringToByte(oldVertex.substring(0,4) + "0001");
		case 'C':
			return BitwiseOperation.convertBinaryStringToByte(oldVertex.substring(0,4) + "0010");
		case 'G':
			return BitwiseOperation.convertBinaryStringToByte(oldVertex.substring(0,4) + "0100");
		case 'T':
			return BitwiseOperation.convertBinaryStringToByte(oldVertex.substring(0,4) + "1000");
			default:
				return (Byte) null;
		}
	}
	/**
	 * convert gene code to binary string
	 */
	public static String convertGeneCodeToBinaryString(String gene){
		String result = "";
		for(int i = 0; i < gene.length(); i++){
			switch(gene.charAt(i)){
			case 'A':
				result += "00";
				break;
			case 'C':
				result += "01";
				break;
			case 'G':
				result += "10";
				break;
			case 'T':
				result += "11";
				break;
				default:
				break;
			}
		}
		return result;
	}
	/**
	 * flush chainVertexId to file -- local file and hdfs file
	 * @throws IOException 
	 */
	public static void flushChainToFile(byte[] chainVertexId, int lengthOfChain, byte[] vertexId) throws IOException{
		 DataOutputStream out = new DataOutputStream(new 
                 FileOutputStream("data/ChainVertex"));
		 out.write(vertexId);
		 out.writeInt(lengthOfChain);
		 out.write(chainVertexId);
		 out.close();
		 //String srcFile = "data/ChainVertex";
		 //String dstFile = "testHDFS/output/ChainVertex";
		 //HDFSOperation.copyFromLocalFile(srcFile, dstFile);
	}
	/**
	 * convert binaryString to geneCode
	 */
	public static String convertBinaryStringToGenecode(String kmer){
		String result = "";
		for(int i = 0; i < kmer.length() ; ){
			String substring = kmer.substring(i,i+2);
			if(substring.compareTo("00") == 0)
				result += "A";
			else if(substring.compareTo("01") == 0)
				result += "C";
			else if(substring.compareTo("10") == 0)
				result += "G";
			else if(substring.compareTo("11") == 0)
				result += "T";
			i = i+2;
		}
		return result;
	}
	/**
	 *  generate the valid data(byte[]) from BytesWritable
	 */
	public static byte[] generateValidDataFromBytesWritable(BytesWritable bw){
		byte[] wholeBytes = bw.getBytes();
		int validNum = bw.getLength();
		byte[] validBytes = new byte[validNum];
		for(int i = 0; i < validNum; i++)
			validBytes[i] = wholeBytes[i];
		return validBytes;
	}
	/**
	 *  output test for message communication
	 */
	public static void testMessageCommunication(OutputStreamWriter writer, long step, byte[] tmpSourceVertextId,
			byte[] tmpDestVertexId, MessageWritable tmpMsg){
		//test
    	String kmer = BitwiseOperation.convertBytesToBinaryStringKmer(
    			tmpSourceVertextId,GraphVertexOperation.k);
    	try {
    		writer.write("Step: " + step + "\r\n");
			writer.write("Source Key: " + kmer + "\r\n");
		
        	writer.write("Source Code: " + 
		    		GraphVertexOperation.convertBinaryStringToGenecode(kmer) + "\r\n");
        	writer.write("Send Message to: " + 
		    		GraphVertexOperation.convertBinaryStringToGenecode(
		    				BitwiseOperation.convertBytesToBinaryStringKmer(
		    						tmpDestVertexId,GraphVertexOperation.k)) + "\r\n");
        	writer.write("Chain Message: " + 
		    		GraphVertexOperation.convertBinaryStringToGenecode(
		    						BitwiseOperation.convertBytesToBinaryString(
		    								tmpMsg.getChainVertexId())) + "\r\n");
        	writer.write("Chain Length: " + tmpMsg.getLengthOfChain() + "\r\n"); 
        	writer.write("\r\n");
    	} catch (IOException e) { e.printStackTrace(); }
    	return;
	}
	/**
	 *  output test for message communication
	 */
	public static void testMessageCommunication2(OutputStreamWriter writer, long step, byte[] tmpSourceVertextId,
			byte[] tmpDestVertexId, LogAlgorithmMessageWritable tmpMsg, byte[] myownId){
		//test
    	String kmer = BitwiseOperation.convertBytesToBinaryStringKmer(
    			tmpSourceVertextId,GraphVertexOperation.k);
    	try {
    		writer.write("Step: " + step + "\r\n");
			writer.write("Source Key: " + kmer + "\r\n");
		
        	writer.write("Source Code: " + 
		    		GraphVertexOperation.convertBinaryStringToGenecode(kmer) + "\r\n");
        	writer.write("Send Message to: " + 
		    		GraphVertexOperation.convertBinaryStringToGenecode(
		    				BitwiseOperation.convertBytesToBinaryStringKmer(
		    						tmpDestVertexId,GraphVertexOperation.k)) + "\r\n");
        	if(tmpMsg.getLengthOfChain() != 0){
        		writer.write("Chain Message: " + 
		    		GraphVertexOperation.convertBinaryStringToGenecode(
		    						BitwiseOperation.convertBytesToBinaryString(
		    								tmpMsg.getChainVertexId())) + "\r\n");
        		writer.write("Chain Length: " + tmpMsg.getLengthOfChain() + "\r\n"); 
        	}
        	if(myownId != null)
        		writer.write("My own Id is: " + 
        				GraphVertexOperation.convertBinaryStringToGenecode(
	    						BitwiseOperation.convertBytesToBinaryStringKmer(
	    								myownId,GraphVertexOperation.k)) + "\r\n");
        	if(tmpMsg.getMessage() != 0)
        		writer.write("Message is: " + tmpMsg.getMessage() + "\r\n");
        	writer.write("\r\n");
    	} catch (IOException e) { e.printStackTrace(); }
    	return;
	}
	/**
	 *  output test for last message communication -- flush
	 */
	public static void testLastMessageCommunication(OutputStreamWriter writer, long step, byte[] tmpVertextId,
			byte[] tmpSourceVertextId,  MessageWritable tmpMsg){
		String kmer = BitwiseOperation.convertBytesToBinaryStringKmer(
    			tmpVertextId,GraphVertexOperation.k);
    	try {
    		writer.write("Step: " + step + "\r\n");
    		writer.write("Over!" + "\r\n");
			writer.write("Source Key: " + kmer + "\r\n");
			
        	writer.write("Source Code: " + 
		    		GraphVertexOperation.convertBinaryStringToGenecode(kmer) + "\r\n");
        
        	writer.write("Flush Chain Message: " + 
		    		GraphVertexOperation.convertBinaryStringToGenecode(
		    						BitwiseOperation.convertBytesToBinaryString(
		    								tmpMsg.getChainVertexId())) + "\r\n");
        	writer.write("Chain Length: " + tmpMsg.getLengthOfChain() + "\r\n"); 
        	writer.write("\r\n");
    	} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/**
	 *  output test for log message communication
	 */
	public static void testLogMessageCommunication(OutputStreamWriter writer, long step, byte[] tmpSourceVertextId,
			byte[] tmpDestVertexId, LogAlgorithmMessageWritable tmpMsg){
		//test
    	String kmer = BitwiseOperation.convertBytesToBinaryStringKmer(
    			tmpSourceVertextId,GraphVertexOperation.k);
    	try {
    		writer.write("Step: " + step + "\r\n");
			writer.write("Source Key: " + kmer + "\r\n");
		
        	writer.write("Source Code: " + 
		    		GraphVertexOperation.convertBinaryStringToGenecode(kmer) + "\r\n");
        	writer.write("Send Message to: " + 
		    		GraphVertexOperation.convertBinaryStringToGenecode(
		    				BitwiseOperation.convertBytesToBinaryStringKmer(
		    						tmpDestVertexId,GraphVertexOperation.k)) + "\r\n");
        	writer.write("Message is: " +
        			tmpMsg.getMessage() + "\r\n");
        	writer.write("\r\n");
    	} catch (IOException e) { e.printStackTrace(); }
    	return;
	}	
	/**
	 *  test set vertex state
	 */
	public static void testSetVertexState(OutputStreamWriter writer, long step,byte[] tmpSourceVertextId,
			byte[] tmpDestVertexId, LogAlgorithmMessageWritable tmpMsg, ValueStateWritable tmpVal){
		//test
    	String kmer = BitwiseOperation.convertBytesToBinaryStringKmer(
    			tmpSourceVertextId,GraphVertexOperation.k);
    	try {
    		writer.write("Step: " + step + "\r\n");
			writer.write("Source Key: " + kmer + "\r\n");
		
        	writer.write("Source Code: " + 
		    		GraphVertexOperation.convertBinaryStringToGenecode(kmer) + "\r\n");
        	if(tmpDestVertexId != null && tmpMsg != null){
	        	writer.write("Send Message to: " + 
			    		GraphVertexOperation.convertBinaryStringToGenecode(
			    				BitwiseOperation.convertBytesToBinaryStringKmer(
			    						tmpDestVertexId,GraphVertexOperation.k)) + "\r\n");
	        	writer.write("Message is: " +
	        			tmpMsg.getMessage() + "\r\n");
        	}
        	writer.write("Set vertex state to " +
        			tmpVal.getState() + "\r\n");
        	writer.write("\r\n");

    	} catch (IOException e) { e.printStackTrace(); }
    	return;
	}
	/**
	 *  test delete vertex information
	 */
	public static void testDeleteVertexInfo(OutputStreamWriter writer, long step, byte[] vertexId, String reason){
		try {
    		writer.write("Step: " + step + "\r\n");
			writer.write(reason + "\r\n");
			writer.write("delete " + BitwiseOperation.convertBytesToBinaryStringKmer(vertexId, GraphVertexOperation.k) 
				 + "\t" + GraphVertexOperation.convertBinaryStringToGenecode(
		    				BitwiseOperation.convertBytesToBinaryStringKmer(
		    						vertexId,GraphVertexOperation.k)) + "\r\n");
			writer.write("\r\n");
    	} catch (IOException e) { e.printStackTrace(); }
    	return;
	}
	/**
	 *  test merge chain vertex
	 */
	public static void testMergeChainVertex(OutputStreamWriter writer, long step, byte[] mergeChain,
			int lengthOfChain){
		try {
    		writer.write("Step: " + step + "\r\n");
        	writer.write("Merge Chain: " + 
		    		GraphVertexOperation.convertBinaryStringToGenecode(
		    						BitwiseOperation.convertBytesToBinaryString(
		    								mergeChain)) + "\r\n");
        	writer.write("Chain Length: " + lengthOfChain + "\r\n");
			writer.write("\r\n");
    	} catch (IOException e) { e.printStackTrace(); }
    	return;
	}
}
