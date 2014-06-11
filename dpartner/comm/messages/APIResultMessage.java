package cn.edu.pku.dpartner.comm.messages;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import cn.edu.pku.dpartner.comm.util.APIType;


public final class APIResultMessage extends CommMessage {
	APIType apiType;
	Object result;
	/**
	 * creates a new SynchronizeRequestMessage.
	 */
	public APIResultMessage() {
		super(API_RESULT);
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
	APIResultMessage(final ObjectInputStream input) throws IOException,
			ClassNotFoundException {
		super(API_RESULT);
		apiType = (APIType) input.readObject();
		result = input.readObject();
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
		out.writeObject(apiType);
		out.writeObject(result);
	}
	public APIType getAPIType(){
		return apiType;
	}
	public void setAPIType(APIType apiType){
		this.apiType = apiType;
	}
	public Object getResult(){
		return result;
	}
	public void setResult(Object result){
		this.result = result;
	}
	/**
	 * String representation for debug outputs.
	 */
	public String toString() {
		final StringBuffer buffer = new StringBuffer();
		buffer.append("[API_REQUEST] - XID: "); 
		buffer.append(xid);
		buffer.append(", APIType: ");
		buffer.append(apiType.toString()).append(" ,Result").append(result.getClass().getName());
		return buffer.toString();
	}
}
