package cn.edu.pku.dpartner.comm.messages;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


public final class RemoteCallMessage extends CommMessage {

	/**
	 * the service ID.
	 */
	private long serverobjID;

	/**
	 * the signature of the method that is requested to be invoked.
	 */
	private String methodSignature;

	/**
	 * the argument array of the method call.
	 */
	private Object[] arguments;

	private String targetClassName;

	/**
	 * creates a new InvokeMethodMessage.
	 */
	public RemoteCallMessage() {
		super(REMOTE_CALL);
	}

	/**
	 * creates a new InvokeMethodMessage from network packet:
	 * 
	 * <pre>
	 *       0                   1                   2                   3
	 *       0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *      |       header (function = InvokeMsg = 3)                       |
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *      |   length of &lt;serviceID&gt;     |    &lt;serviceID&gt; String       \
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *      |    length of &lt;MethodSignature&gt;     |     &lt;MethodSignature&gt; String       \
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *      |   number of param blocks      |     Param blocks (if any)     \
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 * </pre>
	 * 
	 * .
	 * 
	 * @param input
	 *            an <code>ObjectInputStream</code> that provides the body of a
	 *           network packet.
	 * @throws IOException
	 *             in case of IO failures.
	 * @throws ClassNotFoundException
	 */
	RemoteCallMessage(final ObjectInputStream input) throws IOException,
			ClassNotFoundException {
		super(REMOTE_CALL);
		
		targetClassName = input.readUTF();
		serverobjID = input.readLong();
		
		methodSignature = input.readUTF();
		final short argLength = input.readShort();
		arguments = new Object[argLength];
		for (short i = 0; i < argLength; i++) {
			arguments[i] = input.readObject();
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
		out.writeUTF(targetClassName);
		out.writeLong(serverobjID);
		
		out.writeUTF(methodSignature);
		out.writeShort(arguments.length);
		for (short i = 0; i < arguments.length; i++) {
			out.writeObject(arguments[i]);
		}
	}

	/**
	 * get the service ID.
	 * 
	 * @return the service ID.
	 */
	public long getServerObjectID() {
		return serverobjID;
	}

	/**
	 * set the service ID.
	 * 
	 * @param serviceobjID
	 *            the service ID.
	 */
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
	 * get the arguments for the invoked method.
	 * 
	 * @return the arguments.
	 */
	public Object[] getArgs() {
		return arguments;
	}

	/**
	 * set the arguments.
	 * 
	 * @param arguments
	 *            the arguments.
	 */
	public void setArgs(final Object[] arguments) {
		this.arguments = arguments;
	}

	/**
	 * get the method signature.
	 * 
	 * @return the method signature.
	 */
	public String getMethodSignature() {
		return methodSignature;
	}

	/**
	 * set the method signature.
	 * 
	 * @param methodSignature
	 *            the method signature.
	 */
	public void setMethodSignature(final String methodSignature) {
		this.methodSignature = methodSignature;
	}

	/**
	 * String representation for debug outputs.
	 */
	public String toString() {
		final StringBuffer buffer = new StringBuffer();
		buffer.append("[REMOTE_CALL] - XID: "); 
		buffer.append(xid);
		buffer.append(", targetClassName: ");
		buffer.append(targetClassName);
		buffer.append(", serverObjectID: "); 
		buffer.append(serverobjID);
		buffer.append(", methodName: "); 
		buffer.append(methodSignature);
		buffer.append(", params: "); 
		if(arguments == null)
			buffer.append("");
		else //because the args may contains a stub obj, therefore, we should not use any "toString" methods to prevent from the stub calling back to execute the "toString" methods
		{
			String delimiter = "";
			buffer.append("[");
			for (Object obj : arguments)
			{
				buffer.append(delimiter + obj.getClass().getName());
				delimiter = ", ";
			}
			buffer.append("]");
		}
		return buffer.toString();
	}
}
