package cn.edu.pku.dpartner.comm;

import java.io.IOException;
import java.net.URI;

import cn.edu.pku.dpartner.comm.messages.CommMessage;


public interface NetworkChannel {

	/**
	 * get the protocol that this channel uses on the transport layer
	 */
	String getProtocol();

	/**
	 * get the URI of the remote address
	 */
	URI getRemoteAddress();

	/**
	 * get the URI of the local address
	 */
	URI getLocalAddress();


	/**
	 * close the network channel.
	 */
	void close() throws IOException;
	
	/**
	 * send a message through the channel.
	 */
	void sendMessage(final CommMessage message) throws IOException;
	
	/**
	 * check if the NetworkChannel is still connected with the target address
	 */
	public boolean isConnected();

	public String getChannelID();
	public ChannelEndpoint getChannelEndpoint();

}