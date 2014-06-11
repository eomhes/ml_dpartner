package cn.edu.pku.dpartner.comm.impl;

import java.io.IOException;

import java.io.NotSerializableException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import android.util.Log;

import cn.edu.pku.dpartner.comm.APIEndpoint;
import cn.edu.pku.dpartner.comm.AppEndpoint;
import cn.edu.pku.dpartner.comm.EndpointProfiler;
import cn.edu.pku.dpartner.comm.NetworkChannel;
import cn.edu.pku.dpartner.comm.NetworkChannelFacade;
import cn.edu.pku.dpartner.comm.SimpleDelayer;
import cn.edu.pku.dpartner.comm.impl.EndpointImpl.Target;
import cn.edu.pku.dpartner.comm.messages.APIRequestMessage;
import cn.edu.pku.dpartner.comm.messages.APIResultMessage;
import cn.edu.pku.dpartner.comm.messages.CommMessage;
import cn.edu.pku.dpartner.comm.util.EndpointEventType;
// uf
import cn.edu.pku.dpartner.comm.scheduler.Scheduler;

public class APIEndpointImpl extends ChannelEndpointImpl implements APIEndpoint
{
	private final Map<String, Runnable> callbacks = new HashMap<String, Runnable>(
			5);

	private EndpointImpl appEndpoint;
	// uf
	private Scheduler scheduler;

	// private NetworkChannelFacade channelFacade;
	public static Set<String> notInServer;

	public EndpointProfiler profiler;

	public APIEndpointImpl(NetworkChannelFacade channelFacade)
	{
		super(channelFacade);
		notInServer = new HashSet<String>();
		profiler = new EndpointProfilerImpl();
		scheduler = new Scheduler(10);
	}

	@Override
	public void receivedMessage(final NetworkChannel channel,
			final CommMessage msg) throws RemoteCommException
	{
		if (msg == null)
		{
			return;
		}
		final AsyncCallback callback;
		synchronized (callbacks)
		{
			callback = (AsyncCallback) callbacks.remove(channel.getChannelID()
					+ msg.getXID());// receive the resulting message
		}
		final Runnable r = new Runnable()
		{ // receiving message coming from the remote client
			public void run()
			{
				final CommMessage reply = handleMessage(msg);
				if (reply != null)
				{
					try
					{
						channelFacade.sendMessage(channel, reply); // sendback
																	// the
																	// result
					}
					catch (final NotSerializableException nse)
					{
						throw new RemoteCommException("Error sending " + reply,
								nse);
					}
					catch (final IOException e)
					{
						e.printStackTrace();
					}
				}
			}
		};
		NotifyWorkQueue(r);
	}

	CommMessage handleMessage(final CommMessage msg) throws RemoteCommException
	{
		System.out.println("Receive an API message\n");
		switch (msg.getTypeID())
		{
		case CommMessage.API_REQUEST:
		{
			return handleAPIRequest((APIRequestMessage) msg);
		}
		case CommMessage.API_RESULT:
		{
			return msg;
		}
		default:
			throw new RemoteCommException("Unimplemented message " + msg);
		}
	}

	private CommMessage handleAPIRequest(APIRequestMessage msg)
	{
		ConcurrentHashMap<String, ConcurrentHashMap<String, List<Long>>> datasize;
		String clzName;
		ConcurrentHashMap<Long, Target> objs;
		ConcurrentHashMap<String, ConcurrentHashMap<Long, EndpointImpl.Target>> tempCache;
		System.out.println("Receive an REQUEST XID:" + msg.getXID() + " "
				+ msg.getAPIType());
		APIResultMessage reMsg = new APIResultMessage();
		reMsg.setXID(msg.getXID());
		reMsg.setAPIType(msg.getAPIType());
		Long objID;
		switch (msg.getAPIType())
		{
		case getTest:
			tempCache = appEndpoint.getServerObjCache();
			reMsg.setResult(tempCache);
			break;
		case getLocalClasses:
			List<String> localClasses = new ArrayList<String>();
			for (String key : appEndpoint.notInServer)
			{
				localClasses.add(key);
			}
			reMsg.setResult(localClasses);
			break;
		case getRemoteClasses:
			Log.d("APIENDPOINT", "getRemoteClasses");
			List<String> remoteClasses = new ArrayList<String>();
			tempCache = appEndpoint.getServerObjCache();
			for (String key : tempCache.keySet())
			{
				for (Long oBJID : tempCache.get(key).keySet())
					if (appEndpoint.notInServer.contains(key + "|" + oBJID))
						continue;
					else 
					{
						//Log.d("APIENDPOINT", key + "|" + oBJID);
						remoteClasses.add(key + "|" + oBJID);
					}
			}
			reMsg.setResult(remoteClasses);
			break;
		case getObjectsId:
			clzName = (String) msg.getAPIArgs().get("clzName");
			objs = appEndpoint.getServerObjCache().get(clzName);
			List<Long> obs = new ArrayList<Long>();
			for (Long ll : objs.keySet())
			{
				obs.add(ll);
			}
			reMsg.setResult(obs);
			break;
		case executeOnRemote:
			datasize = profiler.getRemoteCallDataTransfer();
			clzName = (String) msg.getAPIArgs().get("clzName");
			objID = (Long) msg.getAPIArgs().get("objID");
			appEndpoint.synchronizeLocalToServer(clzName, objID);
			reMsg.setResult(new Boolean(true));
			break;
		case executeOnLocal:
			
			clzName = (String) msg.getAPIArgs().get("clzName");
			objID = (Long) msg.getAPIArgs().get("objID");
			appEndpoint.synchronizeServerToLocal(clzName, objID);
			reMsg.setResult(new Boolean(true));
			break;
		case apiExecuteTime:
			reMsg.setResult(profiler.getApiExecuteTime());
			break;
		case classExecuteTime:
			reMsg.setResult(profiler.getRemoteCallExecuteTime());
			break;
		case apiDataTransfer:
			reMsg.setResult(profiler.getApiDataTransfer());
			break;
		case classDataTransfer:
			reMsg.setResult(profiler.getRemoteCallDataTransfer());
			break;
		case getBandWidth:
			String IP = (String) msg.getAPIArgs().get("IP");
			Integer port = (Integer) msg.getAPIArgs().get("port");
			Integer dataSize = (Integer) msg.getAPIArgs().get("dataSize");
			reMsg.setResult(profiler.getBandWidth(IP, port, dataSize));
			break;
		case bandWidthTest:
			reMsg.setResult(msg.getAPIArgs());
			break;
		case setDelay:
			SimpleDelayer.sleepTime = (Long) msg.getAPIArgs().get("delay");
			// if (getHook() instanceof DelayHookImpl ){
			// ((DelayHookImpl) getHook()).setDelay((Long)
			// msg.getAPIArgs().get("delay"));
			// reMsg.setResult(new Boolean(true));
			// }else{
			// reMsg.setResult(new Boolean(false));
			// }
			break;
		case getDelay:
			reMsg.setResult(new Long(SimpleDelayer.sleepTime));
			break;
		case executeDIY:
			String name = (String)msg.getAPIArgs().get("executeClassName");
			String className="aba.abc";
			
			// //TODO reMsg.setResult(new Integer(BatteryMonitor.getBattery()));
			break;
		default:
			System.out.println("Unimplement API Request Type! "
					+ msg.getAPIType().toString());
			break;
		}
		return reMsg;
	}

	public void bindNetworkChannelFacade(NetworkChannelFacade channelFacade)
	{
		this.channelFacade = channelFacade;
		this.channelFacade.bindChannelEndpoint(this);
	}

	public void bindAppEndpoint(AppEndpoint endpoint)
	{
		appEndpoint = (EndpointImpl) endpoint;
		appEndpoint.apiEndpoint = this;
		// uf
		appEndpoint.trainer = scheduler.getTrainer();
		scheduler.bindAppEndpoint(endpoint);
		scheduler.bindProfiler(profiler);
		scheduler.start();
	}

	@Override
	public void profileMsg(CommMessage msg, EndpointEventType eventType)
	{
		try
		{
			profiler.updateStatistics(msg, eventType);
		}
		catch (Throwable e)
		{
			e.printStackTrace();
		}
	}
}
