package edu.uci.ics.pathmergingh2;

import java.io.IOException;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;
import edu.uci.ics.genomix.type.GeneCode;
import edu.uci.ics.genomix.type.VKmerBytesWritable;
import edu.uci.ics.genomix.type.VKmerBytesWritableFactory;

@SuppressWarnings("deprecation")
public class MergePathH2Mapper extends MapReduceBase implements
        Mapper<VKmerBytesWritable, MergePathValueWritable, VKmerBytesWritable, MergePathValueWritable> {

    private int KMER_SIZE;
    private VKmerBytesWritableFactory outputKmerFactory;
    private MergePathValueWritable outputValue;
    private VKmerBytesWritable tmpKmer;
    private VKmerBytesWritable outputKmer;

    public void configure(JobConf job) {
        KMER_SIZE = job.getInt("sizeKmer", 0);
        outputKmerFactory = new VKmerBytesWritableFactory(KMER_SIZE);
        outputValue = new MergePathValueWritable();
        tmpKmer = new VKmerBytesWritable(KMER_SIZE);
        outputKmer = new VKmerBytesWritable(KMER_SIZE);
    }

    @Override
    public void map(VKmerBytesWritable key, MergePathValueWritable value,
            OutputCollector<VKmerBytesWritable, MergePathValueWritable> output, Reporter reporter) throws IOException {
        byte precursor = (byte) 0xF0;
        byte succeed = (byte) 0x0F;
        byte adjBitMap = value.getAdjBitMap();
        byte bitFlag = value.getFlag();
        precursor = (byte) (precursor & adjBitMap);
        precursor = (byte) ((precursor & 0xff) >> 4);
        succeed = (byte) (succeed & adjBitMap);
        byte bitStartEnd = (byte) (0x81 & bitFlag);

        switch (bitStartEnd) {
            case 0x01:
                byte succeedCode = GeneCode.getGeneCodeFromBitMap(succeed);
                tmpKmer.set(outputKmerFactory.getLastKmerFromChain(KMER_SIZE, key));
                outputKmer.set(outputKmerFactory.shiftKmerWithNextCode(tmpKmer, succeedCode));

                tmpKmer.set(outputKmerFactory.getFirstKmerFromChain(key.getKmerLength() - (KMER_SIZE - 1), key));
                bitFlag = (byte) (bitFlag | 0x08);
                outputValue.set(tmpKmer, adjBitMap, bitFlag);
                output.collect(outputKmer, outputValue);
                break;
            case (byte) 0x80:
                tmpKmer.set(outputKmerFactory.getFirstKmerFromChain(KMER_SIZE, key));
                outputKmer.set(tmpKmer);//?????
                tmpKmer.set(outputKmerFactory.getLastKmerFromChain(key.getKmerLength() - KMER_SIZE, key));
                bitFlag = (byte) (bitFlag | 0x10);
                outputValue.set(tmpKmer, adjBitMap, bitFlag);
                output.collect(outputKmer, outputValue);
                break;
            case 0x00:
                succeedCode = GeneCode.getGeneCodeFromBitMap(succeed);
                tmpKmer.set(outputKmerFactory.getLastKmerFromChain(KMER_SIZE, key));
                outputKmer.set(outputKmerFactory.shiftKmerWithNextCode(tmpKmer, succeedCode));

                tmpKmer.set(outputKmerFactory.getFirstKmerFromChain(key.getKmerLength() - (KMER_SIZE - 1), key));
                bitFlag = (byte) (bitFlag | 0x08);
                outputValue.set(tmpKmer, adjBitMap, bitFlag);
                output.collect(outputKmer, outputValue);

                bitFlag = (byte) (bitFlag & 0xF7);
                tmpKmer.set(outputKmerFactory.getFirstKmerFromChain(KMER_SIZE, key));
                outputKmer.set(tmpKmer);//?????
                tmpKmer.set(outputKmerFactory.getLastKmerFromChain(key.getKmerLength() - KMER_SIZE, key));
                bitFlag = (byte) (bitFlag | 0x10);
                outputValue.set(tmpKmer, adjBitMap, bitFlag);
                output.collect(outputKmer, outputValue);
                break;
            case (byte) 0x81:
                outputKmer.set(key);
                outputValue.set(null, 0, 0, adjBitMap, bitFlag, 0);//????????
                break;
        }
    }
}
