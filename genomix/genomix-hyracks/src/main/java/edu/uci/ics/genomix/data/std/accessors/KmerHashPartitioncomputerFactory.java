package edu.uci.ics.genomix.data.std.accessors;

import java.nio.ByteBuffer;

import edu.uci.ics.hyracks.api.comm.IFrameTupleAccessor;
import edu.uci.ics.hyracks.api.dataflow.value.ITuplePartitionComputer;
import edu.uci.ics.hyracks.api.dataflow.value.ITuplePartitionComputerFactory;

public class KmerHashPartitioncomputerFactory implements
		ITuplePartitionComputerFactory {

	private static final long serialVersionUID = 1L;

	public static int hashBytes(byte[] bytes, int offset, int length) {
		int hash = 1;
		for (int i = offset; i < offset + length; i++)
			hash = (31 * hash) + (int) bytes[i];
		return hash;
	}

	@Override
	public ITuplePartitionComputer createPartitioner() {
		return new ITuplePartitionComputer() {
			@Override
			public int partition(IFrameTupleAccessor accessor, int tIndex,
					int nParts) {
				int startOffset = accessor.getTupleStartOffset(tIndex);
				int fieldOffset = accessor.getFieldStartOffset(tIndex, 0);
				int slotLength = accessor.getFieldSlotsLength();
				int fieldLength = accessor.getFieldLength(tIndex, 0);

				ByteBuffer buf = accessor.getBuffer();

				int hash = hashBytes(buf.array(), startOffset + fieldOffset
						+ slotLength, fieldLength);
				if (hash < 0){
					hash = -(hash+1);
				}

				return hash % nParts;
			}
		};
	}
}