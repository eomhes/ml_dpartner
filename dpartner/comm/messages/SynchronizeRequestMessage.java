package cn.edu.pku.dpartner.comm.messages;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import cn.edu.pku.dpartner.comm.impl.SmartObjectOutputStream;


public final class SynchronizeRequestMessage extends CommMessage {

	/**
	 * the service ID.
	 */
	private long serverobjID;

	private String targetClassName;
	
	/**
	 * 0 for server to local, 1 for local to server
	 **/
	private byte typeFlag; 
	
	/**
	 * maybe no exist, just for local to server case.
	 */
	private Object syncObject;

	/**
	 * creates a new SynchronizeRequestMessage.
	 */
	public SynchronizeRequestMessage() {
		super(SYNCHRONIZE_REQUEST);
	}

	/**
	 * creates a new SynchronizeRequestMessage from network packet:
	 * 
	 * @param input
	 *            an <code>ObjectInputStream</code> that provides the body of a
	 *            R-OSGi network packet.
	 * @throws IOException
	 *             in case of IO failures.
	 * @throws ClassNotFoundException
	 */
	SynchronizeRequestMessage(final ObjectInputStream input) throws IOException,
			ClassNotFoundException {
		super(SYNCHRONIZE_REQUEST);
		targetClassName = input.readUTF();
		serverobjID = input.readLong();
		typeFlag = input.readByte();
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
	public void writeBody(final ObjectOutputStream out) throws IOException {
		out.writeUTF(targetClassName);
		out.writeLong(serverobjID);
		out.writeByte(typeFlag);
		if(out instanceof SmartObjectOutputStream)
		{
			((SmartObjectOutputStream)out).enableSynchronization();
		}
		out.writeObject(syncObject);
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

	public byte getTypeFlag() {
		return typeFlag;
	}

	public void setTypeFlag(final byte typeFlag) {
		this.typeFlag = typeFlag;
	}
	
	public Object getSyncObject() {
		return syncObject;
	}

	public void setSyncObject(final Object result) {
		this.syncObject = result;
	}

	/**
	 * String representation for debug outputs.
	 */
	public String toString() {
		final StringBuffer buffer = new StringBuffer();
		buffer.append("[SYNCHRONIZE_REQUEST] - XID: "); 
		buffer.append(xid);
		buffer.append(", targetClassName: ");
		buffer.append(targetClassName);
		buffer.append(", serverObjectID: "); 
		buffer.append(serverobjID);
		
		buffer.append(", typeFlag: "); 
		if (typeFlag == 0)
		{
			buffer.append("server to local");
		}
		else 
		{
			buffer.append("local to server");
		}
		
		buffer.append(", result: "); 
		if(syncObject != null)
		{
			buffer.append(syncObject.getClass().getName());
		}
		else
		{
			buffer.append("void");
		}
		return buffer.toString();
	}
}
