package cn.edu.pku.dpartner.comm;

import java.io.IOException;

import cn.edu.pku.dpartner.comm.impl.RemoteCommException;
import cn.edu.pku.dpartner.comm.messages.CommMessage;

public interface NetworkChannelFacade
{
	/**
	 * send message to the target bundle
	 * @param targetBundleSymbolicName
	 * @param message
	 * @throws IOException
	 */
	public void sendMessage(String targetBundleSymbolicName, CommMessage message) throws RemoteCommException, IOException;
	
	/**
	 * send the message through the channel to the should be destination bundle
	 * @param channel
	 * @param message
	 * @throws IOException
	 */
	public void sendMessage(NetworkChannel channel, CommMessage message) throws RemoteCommException, IOException;
	
	/**
	 * receive the message through the channel
	 * @param channel
	 * @param message
	 * @throws IOException
	 */
	
	public void receivedMessage(NetworkChannel channel, CommMessage message) throws RemoteCommException;
	
	
	/**
	 * get the listening port of this channel facade for incoming connection requests
	 * @param protocol
	 * @return
	 */
	public int getListeningPort(String protocol);
	
	/**
	 * load the initial mapping properties of bundle-symbolic-name to URI <br>
	 * for instance, bundle1=192.168.1.165:1616
	 * bundle2=192.168.1.166:1616
	 */
	public void loadMappingOfBundleSymbolicName2URI();
	
	/**
	 * must called before this facade can be used, return true: binding is OK
	 */
	public void bindChannelEndpoint(ChannelEndpoint channelEndpoint);

	/**
	 * return the corresponding NetworkChannel of the given targetBundleSymbolicName
	 * @param targetBundleSymbolicName
	 * @return
	 */
	public NetworkChannel getNetworkChannel(String targetBundleSymbolicName);
	
	/**
	 * close All Channels
	 */
	public void closeAllChannels();

	/**
	 * get local bundle name
	 */
	public String getLocalBSN();
}
