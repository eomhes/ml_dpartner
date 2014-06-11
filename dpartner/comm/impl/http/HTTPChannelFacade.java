package cn.edu.pku.dpartner.comm.impl.http;


import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.util.Properties;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import cn.edu.pku.dpartner.comm.APIEndpoint;
import cn.edu.pku.dpartner.comm.ChannelEndpoint;
import cn.edu.pku.dpartner.comm.CommConstants;
import cn.edu.pku.dpartner.comm.NetworkChannel;
import cn.edu.pku.dpartner.comm.impl.NetworkChannelFacadeImpl;

public final class HTTPChannelFacade extends NetworkChannelFacadeImpl
{
	public static final String[] PROTOCOLS = {"http", "https"};
	private Server httpServer = null;
	private Server httpApiServer = null;

	static
	{
		HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier()
		{

			public boolean verify(String hostname, SSLSession session)
			{
				return true;
			}

		});
	}

	private String servletName;
	/*
	 * the listeningPort is the APIServer port!
	 * */
	private Integer listeningPort;
	
	protected String getDefaultProtocol()
	{
		return PROTOCOLS[0];
	}
	
	protected int getDefaultPort()
	{
		return CommConstants.DPARTNER_HTTP_PORT;
	}
	
	
	public HTTPChannelFacade()
	{
		super();
		loadServletName();
	}
	
	private void loadServletName()
	{
		boolean entryFromServer = Boolean.parseBoolean(System.getProperties().get(CommConstants.Entry_FROM_SERVER).toString());
		if (entryFromServer)
		{
			Properties bundleSN2URIProp = new Properties();
			FileInputStream in;
			try
			{
				in = new FileInputStream(CommConstants.CONFIG);
				bundleSN2URIProp.load(in);
				in.close();
				Object value = bundleSN2URIProp.get(CommConstants.SERVLET_NAME);
				if(value != null)
				{
					servletName = (String)value;
				}
				else
					servletName = "dpartner";
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public int getListeningPort(String protocol)
	{
		return listeningPort;
	}

	public void closeAllChannels()
	{
		unregisterServerSelvelt();
		super.closeAllChannels();
	}

	private void registerServerSelvelt(ChannelEndpoint channelEndpoint)
	{	
		if (channelEndpoint instanceof APIEndpoint)
			listeningPort = CommConstants.DPARTNER_TCP_API_PORT = getAvailablePort(CommConstants.DPARTNER_TCP_API_PORT);
		else listeningPort = CommConstants.DPARTNER_HTTP_PORT = getAvailablePort(CommConstants.DPARTNER_HTTP_PORT);
		try
		{
			HTTPServerChannel server = new HTTPServerChannel(channelEndpoint);
			addServerNetworkChannel(server);
			Server newServer;
			newServer = new Server(listeningPort);
			ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
			context.setContextPath("/");
			newServer.setHandler(context);
			context.addServlet(new ServletHolder(server), "/"+servletName+"/*");
			newServer.start();
			if (channelEndpoint instanceof APIEndpoint){
				httpApiServer = newServer;
				System.out.println("register api http server on: " + listeningPort);
			}else{
				httpServer = newServer;
				System.out.println("register http server on: " + listeningPort);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private int getAvailablePort(int dPARTNER_HTTP_PORT)
	{
		while (true)
		{
			try
			{
				ServerSocket socket = new ServerSocket(dPARTNER_HTTP_PORT);
				socket.close();
				break;
			}
			catch (Exception e)
			{
				dPARTNER_HTTP_PORT++;
			}
		}
		return dPARTNER_HTTP_PORT;
	}

	private void unregisterServerSelvelt()
	{
		unregisterServerSelvelt(httpServer);
		unregisterServerSelvelt(httpApiServer);
	}
	private void unregisterServerSelvelt(Server server){
		if (server != null)
		{
			try
			{
				server.join();
				server.stop();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	

	protected NetworkChannel createClientChannel(ChannelEndpoint channelEndpoint,
			URI targetURI)
	{
		try
		{
			System.out.println("Create Client Channel to: " + targetURI.toString());
			return new HTTPClientChannel(channelEndpoint, targetURI);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return null;
	}

	protected final void afterChannelEndpointBeSet()
	{
		boolean entryFromServer = Boolean.parseBoolean(System.getProperties().get(CommConstants.Entry_FROM_SERVER).toString());
		if (entryFromServer)
		{
			registerServerSelvelt(getChannelEndpoint());
			
		}
		registerServerSelvelt(getApiChannelEndpoint());
	}

}


