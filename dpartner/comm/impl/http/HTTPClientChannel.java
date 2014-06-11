package cn.edu.pku.dpartner.comm.impl.http;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import cn.edu.pku.dpartner.comm.ChannelEndpoint;
import cn.edu.pku.dpartner.comm.CommConstants;
import cn.edu.pku.dpartner.comm.NetworkChannel;
import cn.edu.pku.dpartner.comm.impl.RemoteCommException;
import cn.edu.pku.dpartner.comm.impl.SmartObjectInputStream;
import cn.edu.pku.dpartner.comm.impl.SmartObjectOutputStream;
import cn.edu.pku.dpartner.comm.messages.CommMessage;


public final class HTTPClientChannel implements NetworkChannel
{
	private URI remoteEndpointAddress;

	private URI localEndpointAddress;

	private boolean connected = false;

	private URL url;

	private String host;

	private int port;
	
	private String service;

	private String protocol;

	private ChannelEndpoint channelEndpoint;

	private String channelUID;

	public HTTPClientChannel(final ChannelEndpoint channelEndpoint,
			final URI remoteAddress) throws IOException
	{
		this.channelEndpoint = channelEndpoint;
		this.host = remoteAddress.getHost();
		int port = remoteAddress.getPort();
		if (port == -1)
		{
			port = CommConstants.DPARTNER_HTTP_PORT;
		}
		this.port = port;
		this.protocol = remoteAddress.getScheme();
		if (this.protocol == null)
			this.protocol = HTTPChannelFacade.PROTOCOLS[0];
		String path = remoteAddress.getPath();
		int index = path.lastIndexOf("/");
		if(path.length() > 0)
			service = path.substring(index + 1);
		else
			service = "dpartner";
		this.channelUID = String.valueOf((long)(Math.random() * Integer.MAX_VALUE));
		this.remoteEndpointAddress = remoteAddress;
		init();
	}


	/**
	 * initialize the channel by setting a new URL.
	 * 
	 * @throws IOException
	 *             if something goes wrong.
	 * @throws
	 */
	private void init() throws IOException
	{
		url = new URL(protocol, host, port, "/" + service);
		try
		{
			localEndpointAddress = new URI("localhost");
		}
		catch (URISyntaxException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * reconnect the channel.
	 */
	private void reconnect() throws IOException
	{
		init();
	}

	/**
	 * get the protocol that is implemented by the channel.
	 */
	public String getProtocol()
	{
		return protocol;
	}

	/**
	 * get the String representation of the channel.
	 */
	public String toString()
	{
		return "HttpChannel ([channelUID]: " + channelUID
				+ " - [remoteAdress]:" + getRemoteAddress() + ")";
	}

	public String getChannelID()
	{
		return toString();
	}

	public void close() throws IOException
	{
		url = null;
		remoteEndpointAddress = null;
		localEndpointAddress = null;
		connected = false;
	}

	public URI getLocalAddress()
	{
		return localEndpointAddress;
	}

	public URI getRemoteAddress()
	{
		return remoteEndpointAddress;
	}

	public boolean isConnected()
	{
		return connected;
	}

	/**
	 * send a message through the channel.
	 */
	public void sendMessage(final CommMessage message) throws IOException
	{
		// open a new connection
		final HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type",
				"application/x-dpartner");
		connection.setRequestProperty("channelID", channelUID);
		connection.setUseCaches(false);
		connection.setDoInput(true);
		connection.setDoOutput(true);
		

		connected = true;

		try
		{
			channelEndpoint.updateSerializing(Thread.currentThread(), true);
			message.send(new SmartObjectOutputStream(connection.getOutputStream()));
		}
		catch (IOException e)
		{
			throw e;
		}
		finally
		{
			channelEndpoint.updateSerializing(Thread.currentThread(), false);
		}
		
		Thread thread = new Thread()
		{
			public void run()
			{
				try
				{
					final SmartObjectInputStream in = new SmartObjectInputStream(connection
							.getInputStream());
					channelEndpoint.updateDeserializingThread(Thread.currentThread(),
							true);
					final CommMessage msg = CommMessage.parse(in);
					channelEndpoint.getChannelFacade().receivedMessage(HTTPClientChannel.this, msg);
				}
				catch (IOException e)
				{
					e.printStackTrace();
					channelEndpoint.getChannelFacade().receivedMessage(HTTPClientChannel.this, null);
				}
				catch (final Throwable t)
				{
					t.printStackTrace();
				}
				finally
				{
					channelEndpoint.updateDeserializingThread(Thread.currentThread(),
							false);
				}

				connected = false;
			}
		};
		thread.start();
	}
	public ChannelEndpoint getChannelEndpoint(){
		return channelEndpoint;
	}

}
