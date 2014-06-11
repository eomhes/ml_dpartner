package cn.edu.pku.dpartner.comm.messages;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


public final class StreamRequestMessage extends CommMessage {

	/**
	 * operation identifier for simple read operation on stream.
	 */
	public static final byte READ = 0;

	/**
	 * operation identifier for read operation reading more than one byte at
	 * once.
	 */
	public static final byte READ_ARRAY = 1;

	/**
	 * operation identifier for simple write operation on stream.
	 */
	public static final byte WRITE = 2;

	/**
	 * operation identifier for write operation writing more than one byte at
	 * once.
	 */
	public static final byte WRITE_ARRAY = 3;

	/**
	 * stream ID of the target stream.
	 */
	private short streamID;

	/**
	 * operation on the target stream.
	 */
	private byte op;

	/**
	 * length argument (read) or value (write).
	 */
	private int lenOrVal;

	/**
	 * array containing data to write.
	 */
	private byte[] b;

	/**
	 * creates a new StreamRequestMessage.
	 */
	public StreamRequestMessage() {
		super(STREAM_REQUEST);
	}

	/**
	 * creates a new StreamRequestMessage from network packet:
	 * 
	 * <pre>
	 *       0                   1                   2                   3
	 *       0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *      |    header (function = StreamRequestMsg = 10)           |
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *      |              short            |       op      |   lenOrVal    |
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *      |                lenOrVal (ctd.)                |       b       \
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 * </pre>
	 * 
	 * .
	 * 
	 * @param input
	 *            an <code>ObjectInputStream</code> that provides the body of a
	 *            network packet.
	 * @throws IOException
	 *             in case of IO failures.
	 */
	StreamRequestMessage(final ObjectInputStream input) throws IOException {
		super(STREAM_REQUEST);
		streamID = input.readShort();
		op = input.readByte();
		switch (op) {
		case READ:
			lenOrVal = 1;
			b = null;
			break;
		case READ_ARRAY:
		case WRITE:
			lenOrVal = input.readInt();
			b = null;
			break;
		case WRITE_ARRAY:
			lenOrVal = input.readInt();
			b = new byte[lenOrVal];
			int rem = lenOrVal;
			int read;
			while ((rem > 0)
					&& ((read = input.read(b, lenOrVal - rem, rem)) > 0)) {
				rem = rem - read;
			}
			if (rem > 0) {
				throw new IOException("Premature end of input stream."); //$NON-NLS-1$
			}
			break;
		default:
			throw new IllegalArgumentException(
					"op code not within valid range: " + op); //$NON-NLS-1$
		}
	}

	/**
	 * write the body of the message to a stream.
	 * 
	 * @param out
	 *            the ObjectOutputStream.
	 * @throws IOException
	 *             in case of IO failures.
	 */
	public void writeBody(final ObjectOutputStream out) throws IOException {
		out.writeShort(streamID);
		out.writeByte(op);
		if (op != READ) {
			out.writeInt(lenOrVal);
			if (op == WRITE_ARRAY) {
				out.write(b);
			}
		}
	}

	/**
	 * get the ID of the stream.
	 * 
	 * @return the ID of the stream.
	 */
	public short getStreamID() {
		return streamID;
	}

	/**
	 * set the ID of the stream.
	 * 
	 * @param streamID
	 *            the ID of the stream.
	 */
	public void setStreamID(final short streamID) {
		this.streamID = streamID;
	}

	/**
	 * get the operation code.
	 * 
	 * @return the operation code.
	 */
	public byte getOp() {
		return op;
	}

	/**
	 * set the operation code.
	 * 
	 * @param op
	 *            the operation code.
	 */
	public void setOp(final byte op) {
		this.op = op;
	}

	/**
	 * get the length (read op) or value (write op) field.
	 * 
	 * @return the length or value.
	 */
	public int getLenOrVal() {
		return lenOrVal;
	}

	/**
	 * set the length (read op) or value (write op) field.
	 * 
	 * @param lenOrVal
	 *            the length or value.
	 */
	public void setLenOrVal(final int lenOrVal) {
		this.lenOrVal = lenOrVal;
	}

	/**
	 * get the data array.
	 * 
	 * @return the data array.
	 */
	public byte[] getData() {
		return b;
	}

	/**
	 * set the data array.
	 * 
	 * @param b
	 *            the data array to store.
	 */
	public void setData(final byte[] b) {
		this.b = b;
	}

	/**
	 * String representation for debug outputs.
	 * 
	 * @return a string representation.
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		final StringBuffer buffer = new StringBuffer();
		buffer.append("[STREAM_REQUEST] - XID: "); //$NON-NLS-1$
		buffer.append(xid);
		buffer.append(", streamID: "); //$NON-NLS-1$
		buffer.append(streamID);
		buffer.append(", op: "); //$NON-NLS-1$
		buffer.append(op);
		buffer.append(", len: "); //$NON-NLS-1$
		buffer.append(lenOrVal);
		return buffer.toString();
	}

}
