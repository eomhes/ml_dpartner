package cn.edu.pku.dpartner.comm.impl.tcp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.URI;

import cn.edu.pku.dpartner.comm.ChannelEndpoint;
import cn.edu.pku.dpartner.comm.CommConstants;
import cn.edu.pku.dpartner.comm.NetworkChannel;
import cn.edu.pku.dpartner.comm.impl.APIEndpointImpl;
import cn.edu.pku.dpartner.comm.impl.RemoteCommException;
import cn.edu.pku.dpartner.comm.impl.SmartObjectInputStream;
import cn.edu.pku.dpartner.comm.impl.SmartObjectOutputStream;
import cn.edu.pku.dpartner.comm.messages.CommMessage;

public final class TCPChannel implements NetworkChannel
{
	private Socket socket;

	private final URI remoteEndpointAddress;

	private URI localEndpointAddress;

	private ObjectInputStream input;

	private ObjectOutputStream output;

	private ChannelEndpoint channelEndpoint;
	
	

	private boolean connected = true;

	public boolean isConnected()
	{
		return connected;
	}

	/**
	 * create a new TCPChannel.
	 * the endpoint is the initial sender side (i.e., client side)
	 */
	public TCPChannel(final ChannelEndpoint channelEndpoint, final URI remoteAddress)
			throws IOException
	{
		int port = remoteAddress.getPort();
		if (port == -1)
		{
			port = CommConstants.DPARTNER_TCP_PORT;
		}
		this.channelEndpoint = channelEndpoint;
		this.remoteEndpointAddress = remoteAddress;
		
		Socket socket = new Socket(remoteAddress.getHost(), port);
		open(socket);
		new ReceiverThread().start();
	}

	/**
	 * create a new TCPChannel from an existing socket.
	 * the endpoint is the initial receiver side (i.e., server side)
	 */
	public TCPChannel(final ChannelEndpoint channelEndpoint, final Socket socket) throws IOException
	{
		this.channelEndpoint = channelEndpoint;
		this.remoteEndpointAddress = URI.create(getProtocol() + "://" 
				+ socket.getInetAddress().getHostName() + ":"
				+ socket.getPort());
		open(socket);
		new ReceiverThread().start();
	}



	/**
	 * open the channel
	 */
	private void open(final Socket s) throws IOException
	{
		socket = s;
		localEndpointAddress = URI.create(getProtocol() + "://" 
				+ socket.getLocalAddress().getHostName() + ":"
				+ socket.getLocalPort());
		try
		{
			socket.setKeepAlive(true);
		}
		catch (final Throwable t)
		{
			// for 1.2 VMs that do not support the setKeepAlive
		}
		socket.setTcpNoDelay(true);
		output = new SmartObjectOutputStream(new BufferedOutputStream(
				socket.getOutputStream()));
		output.flush();
		input = new SmartObjectInputStream(new BufferedInputStream(socket
				.getInputStream()));
	}

	/**
	 * get the String representation of the channel.
	 */
	public String toString()
	{
		return "TCPChannel (localAddress:" + getLocalAddress() + " <--> remoteAdress:"+ getRemoteAddress() + ")";
	}
	
	public String getChannelID()
	{
		return toString();
	}

	/**
	 * close the channel.
	 */
	public void close() throws IOException
	{
		socket.close();
		connected = false;
	}

	public String getProtocol()
	{
		return TCPChannelFacade.PROTOCOL;
	}

	public URI getRemoteAddress()
	{
		return remoteEndpointAddress;
	}

	public URI getLocalAddress()
	{
		return localEndpointAddress;
	}

	/**
	 * send a message through the channel.
	 */
	public void sendMessage(final CommMessage message)
			throws IOException
	{
		try
		{
			channelEndpoint.updateSerializing(Thread.currentThread(), true);
			message.send(output);
		}
		catch (IOException e)
		{
			throw e;
		}
		finally
		{
			channelEndpoint.updateSerializing(Thread.currentThread(), false);
		}
	}

	/**
	 * the receiver thread continuously tries to receive messages from the
	 * other endpoint.
	 */
	class ReceiverThread extends Thread
	{
		ReceiverThread()
		{
			setName("TCPChannel:ReceiverThread:" + getRemoteAddress()); 
			setDaemon(true);
		}

		public void run()
		{
			while (connected)
			{
				try
				{
					
					channelEndpoint.updateDeserializingThread(Thread
							.currentThread(), true);
					final CommMessage msg = CommMessage
							.parse(input);
					channelEndpoint.getChannelFacade().receivedMessage(TCPChannel.this, msg);
				}
				catch (final IOException ioe)
				{
					if (channelEndpoint instanceof APIEndpointImpl)
						;
					else ioe.printStackTrace();
					try
					{
						channelEndpoint.getChannelFacade().receivedMessage(TCPChannel.this, null);
						close();
					}
					catch (RemoteCommException e1)
					{
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					catch (IOException e1)
					{
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					
				}
				catch (final Throwable t)
				{
					t.printStackTrace();
				}
				finally
				{
					channelEndpoint.updateDeserializingThread(Thread
							.currentThread(), false);
				}
			}
		}
	}
	public ChannelEndpoint getChannelEndpoint(){
		return channelEndpoint;
	}
}