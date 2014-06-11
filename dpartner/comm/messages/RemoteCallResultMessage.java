package cn.edu.pku.dpartner.comm.messages;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


public final class RemoteCallResultMessage extends CommMessage {

	/**
	 * the error flag.
	 */
	private byte errorFlag;

	/**
	 * the return value.
	 */
	private Object result;

	/**
	 * the exception.
	 */
	private Throwable exception;

	/**
	 * creates a new MethodResultMessage from InvokeMethodMessage and set the
	 * exception.
	 */
	public RemoteCallResultMessage() {
		super(REMOTE_CALL_RESULT);
	}

	/**
	 * creates a new MethodResultMessage from network packet:
	 * 
	 * <pre>
	 *       0                   1                   2                   3
	 *       0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *      |       header (function = Service = 2)                  |
	 *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *      |  error flag   | result or Exception                           \
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
	 * @throws ClassNotFoundException
	 */
	RemoteCallResultMessage(final ObjectInputStream input) throws IOException,
			ClassNotFoundException {
		super(REMOTE_CALL_RESULT);
		errorFlag = input.readByte();
		if (errorFlag == 0) {
			result = input.readObject();
			exception = null;
		} else {
			exception = (Throwable) input.readObject();
			result = null;
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
		if (exception == null) {
			out.writeByte(0);
			out.writeObject(result);
		} else {
			out.writeByte(1);
			out.writeObject(exception);
		}
	}

	/**
	 * did the method invocation cause an exception ?
	 * 
	 * @return <code>true</code>, if an exception has been thrown on the remote
	 *         side. In this case, the exception can be retrieved through the
	 *         <code>getException</code> method.
	 */
	public boolean causedException() {
		return (errorFlag == 1);
	}

	/**
	 * get the result object.
	 * 
	 * @return the return value of the invoked message.
	 */
	public Object getResult() {
		return result;
	}

	/**
	 * set the result.
	 * 
	 * @param result
	 *            the result.
	 */
	public void setResult(final Object result) {
		this.result = result;
		errorFlag = 0;
	}

	/**
	 * get the exception.
	 * 
	 * @return the exception or <code>null</code> if non was thrown.
	 */
	public Throwable getException() {
		return exception;
	}

	/**
	 * set the exception.
	 * 
	 * @param t
	 *            the exception.
	 */
	public void setException(final Throwable t) {
		exception = t;
		errorFlag = 1;
	}

	/**
	 * String representation for debug outputs.
	 * 
	 * @return a string representation.
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		final StringBuffer buffer = new StringBuffer();
		buffer.append("[REMOTE_CALL_RESULT] - XID: "); 
		buffer.append(xid);
		buffer.append(", errorFlag: "); 
		buffer.append(errorFlag);
		if (causedException()) {
			buffer.append(", exception Message: ");
			buffer.append(exception.getMessage());
		} else {
			buffer.append(", result: "); 
			if(result != null)
				buffer.append(result.getClass().getName());
			else
				buffer.append("void");
		}
		return buffer.toString();
	}
}
