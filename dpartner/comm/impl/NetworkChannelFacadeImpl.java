package cn.edu.pku.dpartner.comm.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Hashtable;
import java.util.Properties;

import cn.edu.pku.dpartner.comm.APIEndpoint;
import cn.edu.pku.dpartner.comm.ChannelEndpoint;
import cn.edu.pku.dpartner.comm.CommConstants;
import cn.edu.pku.dpartner.comm.EndpointHook;
import cn.edu.pku.dpartner.comm.Entry;
import cn.edu.pku.dpartner.comm.NetworkChannel;
import cn.edu.pku.dpartner.comm.NetworkChannelFacade;
import cn.edu.pku.dpartner.comm.messages.CommMessage;

public abstract class NetworkChannelFacadeImpl implements NetworkChannelFacade
{
	private ChannelEndpoint channelEndpoint;	 
	
	private ChannelEndpoint apiChannelEndpoint;
	
	private Hashtable<String, URI> bundleSN2URI;
	
	private Hashtable<URI, NetworkChannel> uri2NC;
	
	private String localBSN;
	
	protected NetworkChannelFacadeImpl()
	{
		loadMappingOfBundleSymbolicName2URI();
		uri2NC = new Hashtable<URI, NetworkChannel>();
	}
	
	public final void loadMappingOfBundleSymbolicName2URI()
	{
		uri2NC = new Hashtable<URI, NetworkChannel>();
		bundleSN2URI = new Hashtable<String, URI>();
		localBSN = null;
		Properties bundleSN2URIProp = new Properties();
		FileInputStream in;
		try
		{
			boolean entryFromServer = Boolean.parseBoolean(System.getProperties().get(CommConstants.Entry_FROM_SERVER).toString());
			if (entryFromServer)
			{
				File file = new File(CommConstants.CONFIG);
				in = new FileInputStream(file);
				bundleSN2URIProp.load(in);
				in.close();
			}
			else 
			{
				bundleSN2URIProp = Entry.readProperties();
			}
			
			for (Object key : bundleSN2URIProp.keySet())
			{
				String bundleSN_Key = (String)key;
				if(bundleSN_Key.startsWith(CommConstants.BSN2URI_PREFIX))
				{
					final String bundleSN = bundleSN_Key.substring(CommConstants.BSN2URI_PREFIX.length()); //the actual bundle symbolic name
					String ip = getDefaultProtocol() + "://";
					String uriStr = bundleSN2URIProp.getProperty(bundleSN_Key);
					if(uriStr.startsWith(ip))
						ip = "";
					if(uriStr.indexOf(":") == -1) //no port
						ip += uriStr + ":" + getDefaultPort();
					else
						ip += uriStr;
					URI uri = new URI(ip);
					bundleSN2URI.put(bundleSN, uri);
				}
				else if(bundleSN_Key.equalsIgnoreCase(CommConstants.BUNDLE_SYMBOLICNAME))
				{
					localBSN = bundleSN2URIProp.getProperty(bundleSN_Key);
				}
			}
			
			URI uri = bundleSN2URI.get(localBSN);
			int port = uri.getPort();
			//we can set all the port to be the specified one because only one protocol can be used at a time.
			CommConstants.DPARTNER_TCP_PORT = port;
			//to http, it should ask the user to maintain the port number consistent between the config.property and the .bat file.
			//CommConstants.DPARTNER_HTTP_PORT = port;
			//the property is used by the http service such as Jetty
			//System.setProperty("org.osgi.service.http.port", String.valueOf(port));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private final NetworkChannel getNetworkChannelInCache(String targetBundleSymbolicName)
	{
		URI targetURI = bundleSN2URI.get(targetBundleSymbolicName);
		if(targetURI == null)
			throw null;
		NetworkChannel channel = uri2NC.get(targetURI);
		return channel;
	}
	
	public final NetworkChannel getNetworkChannel(String targetBundleSymbolicName)
	{
		URI targetURI = bundleSN2URI.get(targetBundleSymbolicName);
		if(targetURI == null)
			throw null;
		NetworkChannel channel = getNetworkChannelInCache(targetBundleSymbolicName);
		if(channel == null)
		{
			try
			{
				channel = createClientChannel(channelEndpoint, targetURI);
				uri2NC.put(targetURI, channel);
			}
			catch (Exception e)
			{
				channel = null;
				e.printStackTrace();
			}
		}
		return channel;
	}
	
	protected final void addServerNetworkChannel(NetworkChannel serverChannel)
	{
		URI uri = serverChannel.getRemoteAddress();
		if(!uri2NC.containsKey(uri))
			uri2NC.put(uri, serverChannel);
		else
			System.err.println("already have a uri binding with a channel, no new are allowed at current implementation of Dpartner");
		//if two sites can have more connection, then the above methods are needed to be revised
	}

	/**
	 * must called before this facade can be used
	 */
	public final void bindChannelEndpoint(ChannelEndpoint channelEndpoint)
	{
		if (channelEndpoint instanceof APIEndpoint){
			this.apiChannelEndpoint = channelEndpoint;
		}
		else {
			this.channelEndpoint = channelEndpoint;
			afterChannelEndpointBeSet();
		}
	}
	
	
	/**
	 * this method is for the client to send message to the server<br>
	 * the sending channel is selected by the targetBundleSymbolicName
	 */
	public final void sendMessage(String targetBundleSymbolicName,
			CommMessage message) throws IOException
	{
		NetworkChannel channel = getNetworkChannel(targetBundleSymbolicName);
		if(channel == null)
			throw new RemoteCommException("no URI link of the bundle: " + targetBundleSymbolicName);
		sendMessage(channel, message);
	}

	/**
	 * <li> this method is for sendback the calling results from the server to the client, the channel is the one who receives the coming-message,
	 * the parameter is the calling results message, should be send through the coming-message's channel</li><br>
	 * <li>
	 * this method is also reusd for the client send message to the server, the channel is the client's channel
	 * </li>
	 */
	public final void sendMessage(NetworkChannel channel, CommMessage message)
			throws IOException, RemoteCommException
	{
		EndpointHook endpointHooker = channel.getChannelEndpoint().getEndpointHooker();
		if(channel == null)
			throw new RemoteCommException("The Channel is null");
		
		if(CommConstants.LOCAL_OPTIMIZE && isLocal(channel)){
			//CommMessage msg = endpointHooker.beforeMsgSend(message);
			endpointHooker.beforeMsgHandled(message);
			this.receivedMessage(channel,message);
			
		}
		else{
			endpointHooker.afterMsgHandled(message);
			channel.sendMessage(endpointHooker.beforeMsgSend(message));
			endpointHooker.afterMsgSend(message);
		}
	}

	public final void receivedMessage(NetworkChannel channel, CommMessage message)
			throws RemoteCommException
	{
		ChannelEndpoint endpoint = channel.getChannelEndpoint();
		endpoint.receivedMessage(channel, endpoint.getEndpointHooker().beforeMsgHandled(message));
		
	}
	
	
	private boolean isLocal(NetworkChannel channel) 
	{
		if(localBSN != null && (getNetworkChannelInCache(localBSN) == channel))
			return true;
		return false;
	}

	public void closeAllChannels()
	{
		for (NetworkChannel channel : uri2NC.values())
		{
			try
			{
				channel.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	protected final ChannelEndpoint getChannelEndpoint()
	{
		return channelEndpoint;
	}
	protected final ChannelEndpoint getApiChannelEndpoint(){
		return apiChannelEndpoint;
	}

	public String getLocalBSN()
	{
		return localBSN;
	}

	public abstract int getListeningPort(String protocol);
	
	protected abstract void afterChannelEndpointBeSet();
	
	protected abstract String getDefaultProtocol();
	
	protected abstract int getDefaultPort();
	
	protected abstract NetworkChannel createClientChannel(ChannelEndpoint channelEndpoint, URI targetURI);
}
