package cn.edu.pku.dpartner.comm.impl.http;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.edu.pku.dpartner.comm.APIEndpoint;
import cn.edu.pku.dpartner.comm.ChannelEndpoint;
import cn.edu.pku.dpartner.comm.CommConstants;
import cn.edu.pku.dpartner.comm.NetworkChannel;
import cn.edu.pku.dpartner.comm.impl.AsyncCallback;
import cn.edu.pku.dpartner.comm.impl.RemoteCommException;
import cn.edu.pku.dpartner.comm.impl.SmartObjectInputStream;
import cn.edu.pku.dpartner.comm.impl.SmartObjectOutputStream;
import cn.edu.pku.dpartner.comm.impl.WaitingCallback;
import cn.edu.pku.dpartner.comm.messages.CommMessage;

public class HTTPServerChannel extends HttpServlet implements NetworkChannel
{
	private ChannelEndpoint channelEndpoint; // shared by all incoming requests/responses

	// because the HttpServerChannel/Servlet serves for all the client, so set remoteEndpointAddress to be a specific value for preventing error
	private URI remoteEndpointAddress; 

	private URI localEndpointAddress;

	private final Map callbacks = new HashMap(5);
	

	public HTTPServerChannel(final ChannelEndpoint channelEndpoint)
			throws IOException
	{
		this.channelEndpoint = channelEndpoint;
		try
		{
			if (channelEndpoint instanceof APIEndpoint)
				remoteEndpointAddress = new URI(
					"Dpartner:HttpServerChannel:Api4AllRemotes");
			else
				remoteEndpointAddress = new URI(
					"Dpartner:HttpServerChannel:Localhost4AllRemotes");
			localEndpointAddress = new URI(
					"Dpartner:HttpServerChannel:Localhost");
		}
		catch (URISyntaxException e)
		{
			e.printStackTrace();
		}
	}

	// will be executed in separate a servlet thread
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException
	{
		doPost(req, resp);
	}

	// will be executed in separate a servlet thread
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException
	{
		/**
		 * Note: the "resp" is the only destination for an incoming requests
		 */
		final long clientChannelID = Long.valueOf(req.getHeader("channelID"));
		//final String clientID = req.getProtocol() + req.getRemoteAddr() + clientChannelID;
		
		SmartObjectInputStream input = new SmartObjectInputStream(req.getInputStream());

		Long xid = 0L;
		final WaitingCallback blocking = new WaitingCallback();
		try
		{
			channelEndpoint.updateDeserializingThread(Thread.currentThread(),
					true);
			final CommMessage msg = CommMessage.parse(input);
			xid = msg.getXID();

			synchronized (callbacks)
			{
				callbacks.put(xid + clientChannelID, blocking);
			}
			msg.setXID(xid + clientChannelID); //update ID for preventing naming conflict
			
			channelEndpoint.getChannelFacade().receivedMessage(this, msg);
		}
		catch (final IOException ioe)
		{
			channelEndpoint.getChannelFacade().receivedMessage(this, null);
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

		CommMessage responseMsg = null;

		// wait for the reply
		synchronized (blocking)
		{
			final long timeout = System.currentTimeMillis()
					+ CommConstants.TIMEOUT;
			responseMsg = blocking.getResult();
			try
			{
				while (responseMsg == null && System.currentTimeMillis() < timeout)
				{
					blocking.wait(CommConstants.TIMEOUT);
					responseMsg = blocking.getResult();
				}
			}
			catch (InterruptedException ie)
			{
				throw new RemoteCommException(
						"Interrupted while waiting for callback", ie);
			}
		}

		if (responseMsg != null)
		{
			responseMsg.setXID(responseMsg.getXID() - clientChannelID);//reset ID
			resp.setContentType("multipart/x-dpartner");
			SmartObjectOutputStream output = new SmartObjectOutputStream(resp.getOutputStream());
			try
			{
				channelEndpoint.updateSerializing(Thread.currentThread(), true);
				responseMsg.send(output);
				resp.flushBuffer();
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
		else
		{
			throw new RemoteCommException(
					"Method Invocation failed, timeout exceeded.");
		}
	}

	public void close() throws IOException
	{
		synchronized (callbacks)
		{
			callbacks.clear();
		}

		channelEndpoint = null;
	}

	public String getChannelID()
	{
		return "Dpartner:HttpServerChannel";
	}

	public URI getLocalAddress()
	{
		return localEndpointAddress;
	}

	public String getProtocol()
	{
		return "http";
	}

	public URI getRemoteAddress()
	{
		return remoteEndpointAddress;
	}

	public boolean isConnected()
	{
		return true;
	}

	/**
	 * send message back to the client
	 */
	public void sendMessage(CommMessage resultMsg) throws IOException
	{
		final AsyncCallback callback;
		synchronized (callbacks)
		{
			//receive the resulting message
			callback = (AsyncCallback) callbacks.remove(resultMsg.getXID());
		}
		if (callback != null)
		{
			callback.result(resultMsg);
		}
	}
	
	public ChannelEndpoint getChannelEndpoint(){
		return channelEndpoint;
	}

}
