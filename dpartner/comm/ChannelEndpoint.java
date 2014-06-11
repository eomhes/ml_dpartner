package cn.edu.pku.dpartner.comm;

import java.net.URI;

import cn.edu.pku.dpartner.comm.impl.RemoteCommException;
import cn.edu.pku.dpartner.comm.messages.CommMessage;
import cn.edu.pku.dpartner.comm.util.EndpointEventType;

public interface ChannelEndpoint
{
	
	/**
	 * if Deserializing is started, then the executing thread will be added to the ChannelEndpoint's specific data structure, with the second parameter be true <br>
	 * if Deserializing is finished, then the executing thread will be removed from the ChannelEndpoint's specific data structure, with the second parameter be false <br>
	 * @param thread
	 * @param addTrue_removeFalse
	 */
	public void updateDeserializingThread(Thread thread, boolean addTrue_removeFalse);
	
	/**
	 * if Serializing is started, then the executing thread will be added to the ChannelEndpoint's specific data structure, with the second parameter be true <br>
	 * if Serializing is finished, then the executing thread will be removed from the ChannelEndpoint's specific data structure, with the second parameter be false <br>
	 * @param thread
	 * @param addTrue_removeFalse
	 */
	public void updateSerializing(Thread thread, boolean addTrue_removeFalse);

	/**
	 * get the remote address of the targetBundle
	 * @param targetBundleSymbolicName
	 * @return
	 */
	public URI getRemoteAddress(String targetBundleSymbolicName);
	
	
	/**
	 * means from which channel, the msg is received. If the msg be processed and returned some results, the results should also send back from the coming channel
	 * @param channel
	 * @param msg
	 */
	public void receivedMessage(NetworkChannel channel, CommMessage msg) throws RemoteCommException;
	
	/**
	 * check if the target bundle is still being connected by this ChannelEndpoint
	 * @param targetBundleSymbolicName
	 * @return
	 */
	public boolean isConnected(String targetBundleSymbolicName);
	
	/**
	 * must bind a NetworkChannelFacade
	 * @param channelFacade
	 */
	public void bindNetworkChannelFacade(NetworkChannelFacade channelFacade);
	
	/**
	 * dispose this ChannelEndpoint
	 */
	public void dispose();
	public NetworkChannelFacade  getChannelFacade();
	public EndpointHook getEndpointHooker();
	public void profileMsg(CommMessage msg, EndpointEventType eventType);
}
