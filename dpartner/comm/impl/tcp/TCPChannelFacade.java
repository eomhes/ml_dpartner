package cn.edu.pku.dpartner.comm.impl.tcp;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;

import cn.edu.pku.dpartner.comm.APIEndpoint;
import cn.edu.pku.dpartner.comm.ChannelEndpoint;
import cn.edu.pku.dpartner.comm.CommConstants;
import cn.edu.pku.dpartner.comm.NetworkChannel;
import cn.edu.pku.dpartner.comm.impl.APIEndpointImpl;
import cn.edu.pku.dpartner.comm.impl.NetworkChannelFacadeImpl;

public final class TCPChannelFacade extends NetworkChannelFacadeImpl
{
	public static final String PROTOCOL = "dpartner";
	
	private TCPAcceptorThread thread;
	
	private TCPAcceptorThread apiThread;

	private int listeningPort;
	
	public TCPChannelFacade()
	{
		super();
	}
	
	
	protected final String getDefaultProtocol()
	{
		return PROTOCOL;
	}
	
	protected final int getDefaultPort()
	{
		return CommConstants.DPARTNER_TCP_PORT;
	}
	
	protected final NetworkChannel createClientChannel(ChannelEndpoint channelEndpoint, URI targetURI)
	{
		try
		{
			return new TCPChannel(channelEndpoint, targetURI);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return null;
	}

	public int getListeningPort(final String protocol)
	{
		return listeningPort;
	}

	public void closeAllChannels()
	{
		super.closeAllChannels();
		thread.interrupt();
		apiThread.interrupt();
	}

	private final class TCPAcceptorThread extends Thread
	{
		private ServerSocket socket;
		private ChannelEndpoint channelEndpoint;
		
		private TCPAcceptorThread(ChannelEndpoint _channelEndpoint) throws IOException
		{
			channelEndpoint = _channelEndpoint;
			
			setName("TCPChannel:TCPAcceptorThread"); 
			setDaemon(true);

			int e = 0;
			while (true)
			{
				try
				{
					if (channelEndpoint instanceof APIEndpointImpl)
						listeningPort = CommConstants.DPARTNER_TCP_API_PORT + e;
					else
						listeningPort = CommConstants.DPARTNER_TCP_PORT + e;
					if (CommConstants.DPARTNER_TCP_API_PORT==CommConstants.DPARTNER_TCP_PORT)
						System.err.println("API commport and commport are the same!");
					socket = new ServerSocket(listeningPort);
					if (e != 0)
					{
						System.err
								.println("WARNING: Port " 
										+ (listeningPort-e)
										+ " already in use."
										+ "e ="+e+" This instance of Dpartner-Comm is running on port "
										+ listeningPort);
					}
					System.out.println("Dpartner-Comm"+(channelEndpoint instanceof APIEndpointImpl? "API":"")+" listens on port "
							+ listeningPort);
					if (channelEndpoint instanceof APIEndpointImpl)
						CommConstants.DPARTNER_TCP_API_PORT = listeningPort;
					else
						CommConstants.DPARTNER_TCP_PORT = listeningPort;
					
					return;
				}
				catch (final BindException b)
				{
					e++;
				}
			}
		}

		public void run()
		{
			while (!isInterrupted())
			{
				try
				{
					Socket serverSocket = socket.accept();
					NetworkChannel channel = new TCPChannel(channelEndpoint, serverSocket);
					addServerNetworkChannel(channel);
					//if two sites can have more connection, then the above methods are needed to be revised
					//TODO here
				}
				catch (final IOException ioe)
				{
					ioe.printStackTrace();
				}
			}
		}
	}

	protected final void afterChannelEndpointBeSet() 
	{
//		boolean entryFromServer = Boolean.parseBoolean(System.getProperties().get(CommConstants.Entry_FROM_SERVER).toString());
//		if (entryFromServer)
//		{
			try
			{
				thread = new TCPAcceptorThread(getChannelEndpoint());
				thread.start();
				//if (entryFromServer){
				apiThread = new TCPAcceptorThread(getApiChannelEndpoint());
					//Thread.currentThread().sleep(20);
				apiThread.start();
			//	}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
//		}
	}

}
