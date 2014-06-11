package cn.edu.pku.dpartner.comm.messages;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

import cn.edu.pku.dpartner.comm.util.APIType;


public final class APIRequestMessage extends CommMessage {
	APIType apiType;
	Map<String,Object> args;
	/**
	 * creates a new SynchronizeRequestMessage.
	 */
	public APIRequestMessage() {
		super(API_REQUEST);
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
	APIRequestMessage(final ObjectInputStream input) throws IOException,
			ClassNotFoundException {
		super(API_REQUEST);
		apiType = (APIType) input.readObject();
		args = (Map<String,Object>) input.readObject();
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
		out.writeObject(args);
	}
	public APIType getAPIType(){
		return apiType;
	}
	public void setAPIType(APIType apiType){
		this.apiType = apiType;
	}
	
	public Map<String,Object> getAPIArgs(){
		return args;
	}
	
	public void setAPIArgs(Map<String,Object> args){
		this.args = args;
	}
	
	/**
	 * String representation for debug outputs.
	 */
	public String toString() {
		final StringBuffer buffer = new StringBuffer();
		buffer.append("[API_REQUEST] - XID: "); 
		buffer.append(xid);
		buffer.append(", APIType: ");
		buffer.append(apiType.toString());
		return buffer.toString();
	}
}
