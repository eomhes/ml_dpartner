package cn.edu.pku.dpartner.comm.messages;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import cn.edu.pku.dpartner.comm.impl.SmartObjectOutputStream;


public final class SynchronizeResultMessage extends CommMessage {

	private Object syncObject;

	private long serverobjID;
	
	private String targetClassName;
	
	/**
	 * creates a new SynchronizeResultMessage from SynchronizeRequestMessage and set the
	 * exception.
	 */
	public SynchronizeResultMessage() {
		super(SYNCHRONIZE_RESULT);
	}

	/**
	 * creates a new SynchronizeResultMessage from network packet:
	 * 
	 * <pre>
	 *       0                   1                   2                   3
	 *       0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *      |       R-OSGi header (function = Service = 2)                  |
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *      |  error flag   | result or Exception                           \
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 * </pre>
	 * 
	 * .
	 * 
	 * @param input
	 *            an <code>ObjectInputStream</code> that provides the body of a
	 *            R-OSGi network packet.
	 * @throws IOException
	 *             in case of IO failures.
	 * @throws ClassNotFoundException
	 */
	SynchronizeResultMessage(final ObjectInputStream input) throws IOException,
			ClassNotFoundException {
		super(SYNCHRONIZE_RESULT);
		
		targetClassName = input.readUTF();
		serverobjID = input.readLong();
		syncObject = input.readObject();
	}

	/**
	 * write the body of the message to a stream.
	 * 
	 * @param out
	 *            the ObjectOutputStream.
	 * @throws IOException
	 *             in case of IO failures.
	 */
	public void writeBody(final ObjectOutputStream out) throws IOException 
	{
		out.writeUTF(targetClassName);
		out.writeLong(serverobjID);
		
		if(out instanceof SmartObjectOutputStream)
		{
			((SmartObjectOutputStream)out).enableSynchronization();
		}
		out.writeObject(syncObject);
	}

	public Object getSyncObject() {
		return syncObject;
	}

	public void setSyncObject(final Object result) {
		this.syncObject = result;
	}

	public long getServerObjectID() {
		return serverobjID;
	}

	public void setServerObjectID(final long serviceobjID) {
		this.serverobjID = serviceobjID;
	}
	
	public void setTargetClassName(final String targetClassName) {
		this.targetClassName = targetClassName;
	}
	
	public String getTargetClassName() {
		return this.targetClassName;
	}
	
	/**
	 * String representation for debug outputs.
	 * 
	 * @return a string representation.
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		final StringBuffer buffer = new StringBuffer();
		buffer.append("[SYNCHRONIZE_RESULT] - XID: "); 
		buffer.append(xid);
		buffer.append(", targetClassName: ");
		buffer.append(targetClassName);
		buffer.append(", serverObjectID: "); 
		buffer.append(serverobjID);
		buffer.append(", result: "); 
		buffer.append(syncObject.getClass().getName());
		return buffer.toString();
	}
}
