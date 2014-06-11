package cn.edu.pku.dpartner.comm.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.NotSerializableException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import cn.edu.pku.dpartner.comm.AppEndpoint;
import cn.edu.pku.dpartner.comm.ChannelEndpoint;
import cn.edu.pku.dpartner.comm.CommConstants;
import cn.edu.pku.dpartner.comm.NetworkChannel;
import cn.edu.pku.dpartner.comm.NetworkChannelFacade;
import cn.edu.pku.dpartner.comm.messages.CommMessage;
import cn.edu.pku.dpartner.comm.messages.GCMessage;
import cn.edu.pku.dpartner.comm.messages.RemoteCallMessage;
import cn.edu.pku.dpartner.comm.messages.RemoteCallResultMessage;
import cn.edu.pku.dpartner.comm.messages.StreamRequestMessage;
import cn.edu.pku.dpartner.comm.messages.StreamResultMessage;
import cn.edu.pku.dpartner.comm.messages.SynchronizeRequestMessage;
import cn.edu.pku.dpartner.comm.messages.SynchronizeResultMessage;
import cn.edu.pku.dpartner.comm.streams.InputStreamHandle;
import cn.edu.pku.dpartner.comm.streams.InputStreamProxy;
import cn.edu.pku.dpartner.comm.streams.OutputStreamHandle;
import cn.edu.pku.dpartner.comm.streams.OutputStreamProxy;
import cn.edu.pku.dpartner.comm.type.PrimitiveBox;
import cn.edu.pku.dpartner.comm.type.ServerObject;
import cn.edu.pku.dpartner.comm.type.Stub;
import cn.edu.pku.dpartner.comm.util.ASMTypeUtil;
import cn.edu.pku.dpartner.comm.util.EndpointEventType;
import cn.edu.pku.dpartner.comm.util.SmartReflectUtil4Comm;
//uf
//import cn.edu.pku.dpartner.comm.scheduler.Scheduler;
import cn.edu.pku.dpartner.comm.scheduler.train.Trainer;

public final class EndpointImpl extends ChannelEndpointImpl implements AppEndpoint
{
	/**
	 * the callback register
	 */
	private final Map callbacks = new HashMap(5);

	private final Map streams = new HashMap(5);

	private static long nextXid = (short) Math.round(Math.random()
			* Short.MAX_VALUE);

	private short nextStreamID = 0;

	protected ConcurrentHashMap<String, ConcurrentHashMap<Long, Target>> serverobjCache = new ConcurrentHashMap<String, ConcurrentHashMap<Long, Target>>();

	protected ConcurrentHashMap<String, ConcurrentHashMap<String, Method>> methodCache = new ConcurrentHashMap<String, ConcurrentHashMap<String, Method>>();

	private DGC dgc;

	//uf
	private boolean firstTrain;
	private boolean train;
	private boolean firstLocal, firstRemote;
	private boolean wrongDecision;
	private int numInOneRound;
	private int maxInOneRound;
	private int minInOneRound;
	private int playedNum;
	private int trainedNum;
	private double accuracy;
	private double threshold;

	private EndpointProfilerImpl profiler;
	final private String remoteIP = "192.168.1.141";
	final private Integer remotePort = 1617;
	final private int pingSize = 10;

	public Set<String> notInServer;
	
	// uf
	public Set<String> remotableClasses;
	public boolean bothExecution = false;
	public Trainer trainer;

	public ChannelEndpoint apiEndpoint;
	public EndpointImpl(NetworkChannelFacade channelFacade)
	{
		super(channelFacade);
		notInServer = new HashSet<String>(5);
		//uf
		remotableClasses = new HashSet<String>(5);
		dgc = new DGC(this);
		profiler = new EndpointProfilerImpl();
		//uf
		firstTrain = true;
		train = false;
		firstLocal = false;
		firstRemote = false;
		wrongDecision = false;
		maxInOneRound = 20;
		minInOneRound = 5; //minimum value for plays in one training round is 5;
		playedNum = -4;
		trainedNum = 0;
		accuracy = 0.0;
		threshold = 75.0;
	}

	private static synchronized long nextXid()
	{
		if (nextXid == -1)
		{
			nextXid = 0;
		}
		return (++nextXid);
	}

	public void startBothExecute()
	{
		if(bothExecution == false)
		{
			bothExecution = true;
		}
	}

	public void endBothExecute()
	{
		if(bothExecution == true)
		{
			bothExecution = false;
		}
	}

	/**
	 * process a received message (received through the channel)
	 */
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
		if (callback != null)
		{
			//the msg can be a REMOTE_CALL_RESULT message or REMOTE_CALL message when local-call optimize is enabled (i.e., dpartner.localoptimize=true) 
			//when msg is a REMOTE_CALL message, it means: receive the remote call (the "remote is the local" case) from the client, so handle that message and get the corresponding result. 
			//It is used for performance optimizing in the following case: the stub and the serverobj are in the same network node. 
			//It will call the receiveMessage method directly when invokeMethod is called, so as not go through the network protocol stack (it's slow) and do not need serializing and deserializing (it's also slow) 
			//when msg is a REMOTE_CALL_RESULT message, just return that msg as the reply msg.
			CommMessage reply = handleMessage(msg);
			//Chq add here record local optimize execute time!
			getEndpointHooker().afterMsgHandled(reply);
			callback.result(reply); // receive the resulting message of the corresponding requesting message sending by this client
			return;
		}
		else
		{
			final Runnable r = new Runnable()
			{ // receiving message coming from the remote client
				public void run()
				{
					final CommMessage reply = handleMessage(msg);
					if (reply != null)
					{
						try
						{
							channelFacade.sendMessage(channel, reply); // sendback the result
						}
						catch (final NotSerializableException nse)
						{
							throw new RemoteCommException("Error sending "
									+ reply, nse);
						}
						catch (final IOException e)
						{
							e.printStackTrace();
						}
					}
				}
			};
			synchronized (workQueue)
			{
				workQueue.add(r);
				workQueue.notify();
			}
		}
	}

	public void synchronizeLocalToServer(String clzName, long objID)
	{
		if(CommConstants.SHOW_DEBUG_INFO)
			System.out.println("Synchronize local to server send!");
		
		SynchronizeRequestMessage syncMsg = new SynchronizeRequestMessage();
		syncMsg.setServerObjectID(objID);
		syncMsg.setTargetClassName(clzName);
		syncMsg.setTypeFlag((byte) 1);
		ServerObject syncObject = getExportedServerObject(clzName, objID);
		syncMsg.setSyncObject(syncObject);
		
		NetworkChannel networkChannel = channelFacade.getNetworkChannel("Remote");
		final SynchronizeResultMessage syncResultMsg = (SynchronizeResultMessage) sendAndWait(networkChannel, syncMsg);
		
		if (syncResultMsg.getSyncObject() == null)
		{
			//notInLocal
			//notInServer.remove(clzName+"|"+objID);
		}
		else 
		{ 	
			notInServer.remove(clzName+"|"+objID);
		//	callRemote =  true;
		}
	}
	
	public void synchronizeServerToLocal(String clzName, long objID)
	{
		if(CommConstants.SHOW_DEBUG_INFO)
			System.out.println("Synchronize server to local send!");
		
		SynchronizeRequestMessage syncMsg = new SynchronizeRequestMessage();
		syncMsg.setServerObjectID(objID);
		syncMsg.setTargetClassName(clzName);
		syncMsg.setTypeFlag((byte) 0);
		
		NetworkChannel networkChannel = channelFacade.getNetworkChannel("Remote");
		final SynchronizeResultMessage syncResultMsg = (SynchronizeResultMessage) sendAndWait(networkChannel, syncMsg);
		
		try 
		{
		//	remoteClasses.remove(clzName);
			Class clz = Class.forName(clzName);
			if (!methodCache.containsKey(clzName))
			{
				ConcurrentHashMap<String, Method> mIndex = new ConcurrentHashMap<String, Method>();
				methodCache.put(clzName, mIndex);
				for (Method method : SmartReflectUtil4Comm.getMethods(clz))
				{
					if ((method.getModifiers() & Modifier.PRIVATE) == 0) //not private method
					{
						method.setAccessible(true);
						String key = method.getName()+ ASMTypeUtil.getMethodDescriptor(method);
						if(!mIndex.containsKey(key))
							mIndex.put(key, method);
					}
				}
			}
			
			ServerObject serverObject = (ServerObject)syncResultMsg.getSyncObject();
			export(syncResultMsg.getServerObjectID(), serverObject); 
			notInServer.add(clzName+"|"+objID);
		//	callRemote = false;
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		//	callRemote = true;
		}
	}
	
	public Object invokeMeasurement(final String targetBundleSymbolicName,
			final String targetClassName, final long targetServerObjectID,
			final String methodSignature, final Object[] args) throws Throwable
	{
		for (int i = 0; i < args.length; i++)
		{
			if(args[i] instanceof InputStream)
			{
				args[i] = getInputStreamPlaceholder((InputStream) args[i]);
			}
			else if(args[i] instanceof OutputStream)
			{
				args[i] = getOutputStreamPlaceholder((OutputStream) args[i]);
			}
			else if(args[i] instanceof PrimitiveBox)
			{
				args[i] = ((PrimitiveBox)args[i]).getBoxedObject();
			}
		}
		
		final RemoteCallMessage invokeMsg = new RemoteCallMessage();
		invokeMsg.setTargetClassName(targetClassName);
		invokeMsg.setServerObjectID(targetServerObjectID);
		invokeMsg.setMethodSignature(methodSignature);
		invokeMsg.setArgs(args);

		NetworkChannel networkChannel;

		if(channelFacade.getLocalBSN().equalsIgnoreCase("Remote"))
		{
			networkChannel = channelFacade.getNetworkChannel(targetBundleSymbolicName);
		}
		else
		{
			networkChannel = channelFacade.getNetworkChannel("Remote");
		}

		try
		{
			RemoteCallResultMessage resultMsg = (RemoteCallResultMessage) sendAndWait(networkChannel, invokeMsg);
			
			if(resultMsg.causedException())
			{
				throw resultMsg.getException();
			}

			final Object result = resultMsg.getResult();

			if(result instanceof InputStreamHandle)
			{
				return getInputStreamProxy(networkChannel, (InputStreamHandle) result);
			}
			else if(result instanceof OutputStreamHandle)
			{
				return getOutputStreamProxy(networkChannel, (OutputStreamHandle) result);
			}
			else 
			{
				return result;
			}
		}
		catch(final RemoteCommException e)
		{
			e.printStackTrace();
			throw e;
		}
	}

	public Object invokeMethod(final String targetBundleSymbolicName,
			final String targetClassName, final long targetServerObjectID,
			final String methodSignature, final Object[] args) throws Throwable
	{
		countPlayedNum();
		if(firstTrain == true)
		{
			enterTrainMode();
		}

		// check arguments for streams and replace with placeholder
		for (int i = 0; i < args.length; i++)
		{
			if (args[i] instanceof InputStream)
			{
				args[i] = getInputStreamPlaceholder((InputStream) args[i]);
			}
			else if (args[i] instanceof OutputStream)
			{
				args[i] = getOutputStreamPlaceholder((OutputStream) args[i]);
			}
			else if(args[i] instanceof PrimitiveBox)
			{
				args[i] = ((PrimitiveBox)args[i]).getBoxedObject();
			}
		}

		final RemoteCallMessage invokeMsg = new RemoteCallMessage();
		invokeMsg.setTargetClassName(targetClassName);
		invokeMsg.setServerObjectID(targetServerObjectID);
		invokeMsg.setMethodSignature(methodSignature);
		invokeMsg.setArgs(args);
	
		if(train == true)
		{
			Long dataSize;
			double bandwidth;
			boolean isRemoteFaster;
			
			NetworkChannel networkChannelRemote;
			final NetworkChannel networkChannelLocal;

			countTrainedNum();
		
			if (channelFacade.getLocalBSN().equalsIgnoreCase("Remote"))
			{
				networkChannelRemote = channelFacade.getNetworkChannel("Remote");
				networkChannelLocal = channelFacade.getNetworkChannel("Client"); 
				System.out.println("NetworkChannel:"+targetBundleSymbolicName);
			}
			else
			{
				if (notInServer.contains(targetClassName+"|"+targetServerObjectID)){
				
				}
				else 
				{
					// uf
					if (!remotableClasses.contains(targetClassName+"|"+targetServerObjectID) && !(targetServerObjectID == 0L))
					{
						remotableClasses.add(targetClassName+":"+targetServerObjectID);
					}
				}
				networkChannelRemote = channelFacade.getNetworkChannel("Remote");
				networkChannelLocal = channelFacade.getNetworkChannel("Client");
			}

			dataSize = invokeMsg.getDataSize();
			bandwidth = getBandWidth(remoteIP, remotePort, pingSize);
				
			try
			{
				RemoteCallResultMessage resultMsgFromRemote;
				final RemoteCallResultMessage[] resultMsgFromLocal = new RemoteCallResultMessage[1];
				RemoteCallResultMessage resultMsg;

				// send the message and get a MethodResultMessage in return
				if(firstLocal == false)
				{
					firstLocal = true;
					synchronizeServerToLocal(targetClassName, targetServerObjectID);
				}


				Thread localExecution = new Thread(new Runnable(){
					public void run()
					{
						resultMsgFromLocal[0] = (RemoteCallResultMessage) sendAndWait(networkChannelLocal, invokeMsg);		
					}
				});
				localExecution.start();
				
				if(firstRemote == false)
				{
					firstRemote = true;
					synchronizeLocalToServer(targetClassName, targetServerObjectID);
				}
				
				resultMsgFromRemote = (RemoteCallResultMessage) sendAndWait(networkChannelRemote, invokeMsg);
				
				if(localExecution.isAlive())
				{
					resultMsg = resultMsgFromRemote;
					isRemoteFaster = true; //remote execution is faster
				}
				else 
				{
					resultMsg = resultMsgFromLocal[0];
					isRemoteFaster = false; //local execution is faster
				}
				
				trainer.updateDataBase(dataSize, bandwidth, isRemoteFaster);

				if (resultMsg.causedException())
				{
					throw resultMsg.getException();
				}

				checkTrainMode();
				
				final Object result = resultMsg.getResult();
				if (result instanceof InputStreamHandle)
				{
					return getInputStreamProxy(networkChannelRemote, (InputStreamHandle) result);
				}
				else if (result instanceof OutputStreamHandle)
				{
					return getOutputStreamProxy(networkChannelRemote, (OutputStreamHandle) result);
				}
				else
				{
					return result;
				}
			}
			catch (final RemoteCommException e)
			{
				e.printStackTrace();
				throw e;
			}
		} //if(train == true)
		
		else 
		{
			NetworkChannel networkChannel;
			if (channelFacade.getLocalBSN().equalsIgnoreCase("Remote"))
			{
				//networkChannel = channelFacade.getNetworkChannel(targetBundleSymbolicName);
				networkChannel = channelFacade.getNetworkChannel("Remote");
				System.out.println("NetworkChannel:"+targetBundleSymbolicName);
			}
			else
			{
				
				if (notInServer.contains(targetClassName+"|"+targetServerObjectID))
				{
					networkChannel = channelFacade.getNetworkChannel("Client");
				}
				else 
				{
					if (!remotableClasses.contains(targetClassName+"|"+targetServerObjectID) && !(targetServerObjectID == 0L))
					{
						remotableClasses.add(targetClassName+":"+targetServerObjectID);
					}
					networkChannel = channelFacade.getNetworkChannel("Remote");
				}
			}
			try
			{
				// send the message and get a MethodResultMessage in return
				RemoteCallResultMessage resultMsg = (RemoteCallResultMessage) sendAndWait(networkChannel, invokeMsg);
				if (resultMsg.causedException())
				{
					throw resultMsg.getException();
				}
				
				checkTrainMode();

				final Object result = resultMsg.getResult();
				if (result instanceof InputStreamHandle)
				{
					return getInputStreamProxy(networkChannel, (InputStreamHandle) result);
				}
				else if (result instanceof OutputStreamHandle)
				{
					return getOutputStreamProxy(networkChannel, (OutputStreamHandle) result);
				}
				else
				{
					return result;
				}
			}
			catch (final RemoteCommException e)
			{
				e.printStackTrace();
				throw e;
			}
		} // else
	}

	/**
	 * read a byte from the input stream on the peer identified by id.
	 * 
	 * @param streamID
	 *            the ID of the stream.
	 * @return result of the read operation.
	 * @throws IOException
	 *             when an IOException occurs.
	 */
	public int readStream(NetworkChannel channel, final short streamID)
			throws IOException
	{
		final StreamRequestMessage requestMsg = new StreamRequestMessage();
		requestMsg.setOp(StreamRequestMessage.READ);
		requestMsg.setStreamID(streamID);
		final StreamResultMessage resultMsg = doStreamOp(channel, requestMsg);
		return resultMsg.getResult();
	}

	/**
	 * read to an array from the input stream on the peer identified by id.
	 * 
	 * @param streamID
	 *            the ID of the stream.
	 * @param b
	 *            the array to write the result to.
	 * @param off
	 *            the offset for the destination array.
	 * @param len
	 *            the number of bytes to read.
	 * @return number of bytes actually read.
	 * @throws IOException
	 *             when an IOException occurs.
	 */
	public int readStream(NetworkChannel channel, final short streamID,
			final byte[] b, final int off, final int len) throws IOException
	{
		// handle special cases as defined in InputStream
		if (b == null)
		{
			throw new NullPointerException();
		}
		if ((off < 0) || (len < 0) || (len + off > b.length))
		{
			throw new IndexOutOfBoundsException();
		}
		if (len == 0)
		{
			return 0;
		}
		final StreamRequestMessage requestMsg = new StreamRequestMessage();
		requestMsg.setOp(StreamRequestMessage.READ_ARRAY);
		requestMsg.setStreamID(streamID);
		requestMsg.setLenOrVal(len);
		final StreamResultMessage resultMsg = doStreamOp(channel, requestMsg);
		final int length = resultMsg.getLen();
		// check the length first, could be -1 indicating EOF
		if (length > 0)
		{
			final byte[] readdata = resultMsg.getData();
			// copy result to byte array at correct offset
			System.arraycopy(readdata, 0, b, off, length);
		}
		return length;
	}

	/**
	 * write a byte to the output stream on the peer identified by id.
	 * 
	 * @param streamID
	 *            the ID of the stream.
	 * @param b
	 *            the value.
	 * @throws IOException
	 *             when an IOException occurs.
	 */
	public void writeStream(NetworkChannel channel, final short streamID,
			final int b) throws IOException
	{
		final StreamRequestMessage requestMsg = new StreamRequestMessage();
		requestMsg.setOp(StreamRequestMessage.WRITE);
		requestMsg.setStreamID(streamID);
		requestMsg.setLenOrVal(b);
		// wait for the stream operation to finish
		doStreamOp(channel, requestMsg);
	}

	/**
	 * write bytes from array to output stream on the peer identified by id.
	 * 
	 * @param streamID
	 *            the ID of the stream.
	 * @param b
	 *            the source array.
	 * @param off
	 *            offset into the source array.
	 * @param len
	 *            number of bytes to copy.
	 * @throws IOException
	 *             when an IOException occurs.
	 */
	public void writeStream(NetworkChannel channel, final short streamID,
			final byte[] b, final int off, final int len) throws IOException
	{
		// handle special cases as defined in OutputStream
		if (b == null)
		{
			throw new NullPointerException();
		}
		if ((off < 0) || (len < 0) || (len + off > b.length))
		{
			throw new IndexOutOfBoundsException();
		}
		final byte[] data = new byte[len];
		System.arraycopy(b, off, data, 0, len);

		final StreamRequestMessage requestMsg = new StreamRequestMessage();
		requestMsg.setOp(StreamRequestMessage.WRITE_ARRAY);
		requestMsg.setStreamID(streamID);
		requestMsg.setData(data);
		requestMsg.setLenOrVal(len);
		// wait for the stream operation to finish
		doStreamOp(channel, requestMsg);
	}

	public boolean isConnected(String targetBundleSymbolicName)
	{
		if (targetBundleSymbolicName == null)
			return false;
		NetworkChannel channel = channelFacade
				.getNetworkChannel(targetBundleSymbolicName);
		if (channel != null)
			return channel.isConnected();
		return false;
	}

	void send(String targetBundleSymbolicName, final CommMessage msg)
	{
		NetworkChannel networkChannel = channelFacade
				.getNetworkChannel(targetBundleSymbolicName);
		send(networkChannel, msg);
	}

	/**
	 * send a message.
	 * 
	 * @param targetBundleSymbolicName
	 * 
	 * @param msg
	 *            a message.
	 */
	void send(NetworkChannel networkChannel, final CommMessage msg)
	{
		if (msg.getXID() == 0)
		{
			msg.setXID(nextXid());
		}

		try
		{
			channelFacade.sendMessage(networkChannel, msg);
		}
		catch (final IOException ioe)
		{
			ioe.printStackTrace();
			throw new RemoteCommException("Error sending " + msg, ioe);
		}
	}

	/**
	 * message handler method.
	 * 
	 * @param msg
	 *            the incoming message.
	 * @return if reply is created, null otherwise.
	 * @throws RemoteCommException
	 *             if something goes wrong.
	 */
	CommMessage handleMessage(final CommMessage msg) throws RemoteCommException
	{
		switch (msg.getTypeID())
		{
		// requests
		case CommMessage.GC:
		{
			final GCMessage gcMsg = (GCMessage) msg;
			processGC(gcMsg);
			return null;
		}
		case CommMessage.SYNCHRONIZE_RESULT:
		{
			return msg;
		}
		case CommMessage.SYNCHRONIZE_REQUEST:
		{
			final SynchronizeRequestMessage syncMsg = (SynchronizeRequestMessage) msg;
			long objID = syncMsg.getServerObjectID();
			String clzName = syncMsg.getTargetClassName();
			byte typeFlag = syncMsg.getTypeFlag();
			
			if(CommConstants.SHOW_DEBUG_INFO)
				System.out.println("Handle Sync type" + typeFlag + " Msg: " + clzName + "@" + objID);
			
			SynchronizeResultMessage m = new SynchronizeResultMessage();
			m.setXID(syncMsg.getXID());
			m.setTargetClassName(clzName);
			m.setServerObjectID(objID);
			
			if (typeFlag == 0)
			{
				Object result = getExportedServerObject(clzName, objID);
				m.setSyncObject(result);
				if (!notInServer.contains(clzName+"|"+objID))
					notInServer.add(clzName+"|"+objID);
			}
			else 
			{
				ServerObject serverObject = (ServerObject) syncMsg.getSyncObject();
				ConcurrentHashMap<Long, Target> ref = getExportedServerObjectIndex(clzName);
				ref.put(objID, new Target(serverObject));
				m.setSyncObject(new Boolean(true));
				if (notInServer.contains(clzName+"|"+objID))
					notInServer.remove(clzName+"|"+objID);
			}
			return m;
		}
		case CommMessage.REMOTE_CALL_RESULT:
		{
			return msg;
		}
		case CommMessage.REMOTE_CALL:
		{
			final RemoteCallMessage invMsg = (RemoteCallMessage) msg;
			try
			{
				Object[] args = invMsg.getArgs();
				long objID = invMsg.getServerObjectID();
				String methodSignature = invMsg.getMethodSignature();
				String clzName = invMsg.getTargetClassName();
				
				if(CommConstants.SHOW_DEBUG_INFO)
					System.out.println("Handle Msg: " + clzName + "@" + objID + " --> " + methodSignature);
				
				Class clz = Class.forName(clzName);
				if (!methodCache.containsKey(clzName))
				{
					ConcurrentHashMap<String, Method> mIndex = new ConcurrentHashMap<String, Method>();
					methodCache.put(clzName, mIndex);
					for (Method method : SmartReflectUtil4Comm.getMethods(clz))
					{
						if ((method.getModifiers() & Modifier.PRIVATE) == 0) //not private method
						{
							method.setAccessible(true);
							String key = method.getName()+ ASMTypeUtil.getMethodDescriptor(method);
							if(!mIndex.containsKey(key))
								mIndex.put(key, method);
						}
					}
				}

				Object result = null;
				if (methodSignature.startsWith("<init>"))
				{
					boolean constructorFound = false;
					Constructor<?>[] constructors_1 = clz.getConstructors();
					Constructor<?>[] constructors_2 = clz.getDeclaredConstructors();
					Set<Constructor<?>> constructorSet = new HashSet<Constructor<?>>(Arrays.asList(constructors_1));
					constructorSet.addAll(Arrays.asList(constructors_2));
					
					for (Constructor<?> constructor : constructorSet)
					{
						String cSignature = "<init>" + ASMTypeUtil.getConstructorDescriptor(constructor);
						if(methodSignature.equals(cSignature))
						{
							//corresponding to the handle method in EndpointImpl, the serverobject will be created and exported
							//so the remote stub in other bundle should be tracked (by addtostub in the constructor method)
							constructor.setAccessible(true);
							ServerObject so = (ServerObject) (constructor.newInstance(args));
							long tmpObjId = so.__getID__();
							if (channelFacade.getLocalBSN().equalsIgnoreCase("Remote"))
								tmpObjId += CommConstants.SERVER_ID_BEGIN;
							export(tmpObjId, so);
							result = (Long) tmpObjId;
							constructorFound = true;
							break;
						}
					}
					
					if(!constructorFound)
						throw new Exception("Class :" + clz.getName() + " has no such constructor: " + methodSignature);
				}
				else
				{
					ConcurrentHashMap<String, Method> mIndex = methodCache.get(clzName);
					Method me = mIndex.get(methodSignature);
					result = me.invoke(getExportedServerObject(clzName, objID),	args);
				}

				final RemoteCallResultMessage m = new RemoteCallResultMessage();
				m.setXID(invMsg.getXID());
				if (result instanceof InputStream)
				{
					m.setResult(getInputStreamPlaceholder((InputStream) result));
				}
				else if (result instanceof OutputStream)
				{
					m.setResult(getOutputStreamPlaceholder((OutputStream) result));
				}
				else
				{
					m.setResult(result);
				}
				return m;

			}
			catch (final Throwable t)
			{
				t.printStackTrace();
				final RemoteCallResultMessage m = new RemoteCallResultMessage();
				m.setXID(invMsg.getXID());
				m.setException(t);
				return m;
			}
		}
		case CommMessage.STREAM_REQUEST:
		{
			final StreamRequestMessage reqMsg = (StreamRequestMessage) msg;
			try
			{
				// fetch stream object
				final Object stream = streams.get(new Integer(reqMsg
						.getStreamID()));
				if (stream == null)
				{
					throw new IllegalStateException("Could not get stream with ID " + reqMsg.getStreamID());
				}
				// invoke operation on stream
				switch (reqMsg.getOp())
				{
				case StreamRequestMessage.READ:
				{
					final int result = ((InputStream) stream).read();
					final StreamResultMessage m = new StreamResultMessage();
					m.setXID(reqMsg.getXID());
					m.setResult((short) result);
					return m;
				}
				case StreamRequestMessage.READ_ARRAY:
				{
					final byte[] b = new byte[reqMsg.getLenOrVal()];
					final int len = ((InputStream) stream).read(b, 0, reqMsg.getLenOrVal());
					final StreamResultMessage m = new StreamResultMessage();
					m.setXID(reqMsg.getXID());
					m.setResult(StreamResultMessage.RESULT_ARRAY);
					m.setLen(len);
					if (len > 0)
					{
						m.setData(b);
					}
					return m;
				}
				case StreamRequestMessage.WRITE:
				{
					((OutputStream) stream).write(reqMsg.getLenOrVal());
					final StreamResultMessage m = new StreamResultMessage();
					m.setXID(reqMsg.getXID());
					m.setResult(StreamResultMessage.RESULT_WRITE_OK);
					return m;
				}
				case StreamRequestMessage.WRITE_ARRAY:
				{
					((OutputStream) stream).write(reqMsg.getData());
					final StreamResultMessage m = new StreamResultMessage();
					m.setXID(reqMsg.getXID());
					m.setResult(StreamResultMessage.RESULT_WRITE_OK);
					return m;
				}
				default: throw new RemoteCommException("Unimplemented op code for stream request " + msg);
				}
			}
			catch (final IOException e)
			{
				final StreamResultMessage m = new StreamResultMessage();
				m.setXID(reqMsg.getXID());
				m.setResult(StreamResultMessage.RESULT_EXCEPTION);
				m.setException(e);
				return m;
			}
		}
		default: throw new RemoteCommException("Unimplemented message " + msg);
		}
	}


	/**
	 * send a message and wait for the result.
	 * 
	 * @param targetBundleSymbolicName
	 * 
	 * @param msg
	 *            the message.
	 * @return the result message.
	 */
	private CommMessage sendAndWait(NetworkChannel networkChannel,
			final CommMessage msg)
	{
		if (msg.getXID() == 0)
		{
			msg.setXID(nextXid());
		}

		final WaitingCallback blocking = new WaitingCallback();

		synchronized (callbacks)
		{
			callbacks.put(networkChannel.getChannelID() + msg.getXID(), blocking);
		}
		send(networkChannel, msg);
		// wait for the reply
		synchronized (blocking)
		{
			final long timeout = System.currentTimeMillis() + CommConstants.TIMEOUT;
			CommMessage result = blocking.getResult();
			try
			{
				while (result == null && networkChannel != null && System.currentTimeMillis() < timeout)
				{
					blocking.wait(CommConstants.TIMEOUT);
					result = blocking.getResult();
				}
			}
			catch (InterruptedException ie)
			{
				throw new RemoteCommException("Interrupted while waiting for callback", ie);
			}
			if (result != null)
			{
				return result;
			}
			else if (networkChannel == null)
			{
				throw new RemoteCommException("Channel is closed");
			}
			else
			{
				throw new RemoteCommException("Method Invocation failed, timeout exceeded.");
			}
		}
	}

	/**
	 * perform a stream operation.
	 * 
	 * @param requestMsg
	 *            the request message.
	 * @return the result message.
	 * @throws IOException
	 */
	private StreamResultMessage doStreamOp(NetworkChannel channel,
			final StreamRequestMessage requestMsg) throws IOException
	{
		try
		{
			// send the message and get a StreamResultMessage in return
			final StreamResultMessage result = (StreamResultMessage) sendAndWait(channel, requestMsg);
			if (result.causedException())
			{
				throw result.getException();
			}
			return result;
		}
		catch (final RemoteCommException e)
		{
			throw new RemoteCommException("Invocation of operation "
					+ requestMsg.getOp() + " on stream "
					+ requestMsg.getStreamID() + " failed.", e);
		}
	}

	/**
	 * creates a placeholder for an InputStream that can be sent to the other
	 * party and will be converted to an InputStream proxy there.
	 * 
	 * @param origIS
	 *            the instance of InputStream that needs to be remoted
	 * @return the placeholder object that is sent to the actual client
	 */
	private InputStreamHandle getInputStreamPlaceholder(final InputStream origIS)
	{
		final InputStreamHandle sp = new InputStreamHandle(nextStreamID());
		streams.put(new Integer(sp.getStreamID()), origIS);
		return sp;
	}

	/**
	 * creates a proxy for the input stream that corresponds to the placeholder.
	 * 
	 * @param networkChannel
	 * 
	 * @param placeholder
	 *            the placeholder for the remote input stream
	 * @return the proxy for the input stream
	 */
	private InputStream getInputStreamProxy(NetworkChannel networkChannel,
			final InputStreamHandle placeholder)
	{
		return new InputStreamProxy(networkChannel, placeholder.getStreamID(),
				this);
	}

	/**
	 * creates a placeholder for an OutputStream that can be sent to the other
	 * party and will be converted to an OutputStream proxy there.
	 * 
	 * @param origOS
	 *            the instance of OutputStream that needs to be remoted
	 * @return the placeholder object that is sent to the actual client
	 */
	private OutputStreamHandle getOutputStreamPlaceholder(
			final OutputStream origOS)
	{
		final OutputStreamHandle sp = new OutputStreamHandle(nextStreamID());
		streams.put(new Integer(sp.getStreamID()), origOS);
		return sp;
	}

	/**
	 * creates a proxy for the output stream that corresponds to the
	 * placeholder.
	 * 
	 * @param networkChannel
	 * 
	 * @param placeholder
	 *            the placeholder for the remote output stream
	 * @return the proxy for the output stream
	 */
	private OutputStream getOutputStreamProxy(NetworkChannel networkChannel,
			final OutputStreamHandle placeholder)
	{
		return new OutputStreamProxy(networkChannel, placeholder.getStreamID(),
				this);
	}

	/**
	 * get the next stream wrapper id.
	 * 
	 * @return the next stream wrapper id.
	 */
	private synchronized short nextStreamID()
	{
		if (nextStreamID == -1)
		{
			nextStreamID = 0;
		}
		return (++nextStreamID);
	}

	/**
	 * closes all streams that are still open.
	 */
	private void closeStreams()
	{
		final Object[] s = streams.values().toArray();
		try
		{
			for (int i = 0; i < s.length; i++)
			{
				if (s[i] instanceof InputStream)
				{
					((InputStream) s[i]).close();
				}
				else if (s[i] instanceof OutputStream)
				{
					((OutputStream) s[i]).close();
				}
				else
				{
					System.err.println("Object in input streams map was not an instance of a stream.");
				}
			}
		}
		catch (final IOException e)
		{
		}
	}

	public void addToLocalStub(Stub stub)
	{
		if(shouldTrackStub(stub))
			dgc.trackStub(stub);
		
	}

	private boolean shouldTrackStub(Stub stub)
	{
		String serverobjName = stub.getTargetClassName();
		if(serverobjCache.containsKey(serverobjName)) //stub and serverobj are in the same bundle (or location), such stub should not be tracked
		{
			return false;
		}
		else 
			return true; //serverobj are in the remote location, should track its stub in the current location
	}

	private ServerObject getExportedServerObject(String clzName, long objID)
	{
		ConcurrentHashMap<Long, Target> ref = getExportedServerObjectIndex(clzName);
		Target target = ref.get((Long) objID);
		if (target != null)
			return target.getServerObject();
		else
			return null;
	}

	private ConcurrentHashMap<Long, Target> getExportedServerObjectIndex(
			String clzName)
	{
		ConcurrentHashMap<Long, Target> ref = serverobjCache.get(clzName);
		if (ref == null)
		{
			ref = new ConcurrentHashMap<Long, Target>();
			serverobjCache.put(clzName, ref);
		}
		return ref;
	}

	public void export(long instanceID, ServerObject serverObj)
	{
		String clzName = serverObj.getClass().getName();
		ConcurrentHashMap<Long, Target> ref = getExportedServerObjectIndex(clzName);
		Target target = ref.get((Long) instanceID);
		if (target == null)
		{
			ref.put((Long) instanceID, new Target(serverObj));
		}
		else
		{
			target.increaseCnt();
		}
	}
	
	private void processGC(GCMessage gcMsg)
	{
		Hashtable<String, Integer> serverobjs = gcMsg.getServerObjsToClean();
		if (serverobjs != null)
		{
			for (String serverobjReleaseID : serverobjs.keySet())
			{
				int step = serverobjs.get(serverobjReleaseID);
				String[] idparts = serverobjReleaseID.split("#");
				String clzName = idparts[0];
				Long instance = Long.valueOf(idparts[1]);
				ConcurrentHashMap<Long, Target> ref = getExportedServerObjectIndex(clzName);
				if(ref != null)
				{
					Target target = ref.get(instance);
					if(target != null)
					{
						int cnt = target.decreaseCnt(step);
						if(cnt <= 0)
						{
							ref.remove(instance);
							if(CommConstants.SHOW_DEBUG_INFO)
								System.out.println("<==clear clazz object==>: " + serverobjReleaseID);
						}
					}
					
					if(ref.isEmpty())
					{
						serverobjCache.remove(clzName);
						methodCache.remove(clzName);
						if(CommConstants.SHOW_DEBUG_INFO)
							System.out.println("<==clear clazz==>: " + clzName);
					}
				}
			}
		}
	}

	public class Target
	{
		private ServerObject obj;

		private int cnt = 1;
		
		public Target(ServerObject obj)
		{
			this.obj = obj;
		}

		public int increaseCnt()
		{
			return ++cnt;
		}
		
		public int increaseCnt(int step)
		{
			return (cnt += step);
		}

		public int decreaseCnt()
		{
			return --cnt;
		}
		
		public int decreaseCnt(int step)
		{
			return (cnt -= step);
		}

		public ServerObject getServerObject()
		{
			return this.obj;
		}
	}

	public void dispose()
	{
		super.dispose();
		serverobjCache.clear();
		methodCache.clear();
		dgc.dispose();
	}
	public ConcurrentHashMap<String, ConcurrentHashMap<Long, Target>> getServerObjCache(){
		return serverobjCache;
	}

	public ConcurrentHashMap<String, ConcurrentHashMap<String, Method>> getMethodCache(){
		return methodCache;
	}

	@Override
	public void profileMsg(CommMessage msg, EndpointEventType eventType)
	{
		apiEndpoint.profileMsg(msg,eventType);
	};

	//uf
	private void enterTrainMode()
	{
		System.out.println("!!!!!!!!!!!!!!!!!enterTrainMode!!!!!!!!!!!!!!!!!!");
		if(train == false)
		{
			train = true;
		}
		trainer.pauseScheduler();
	}

	private void exitTrainMode()
	{
		System.out.println("!!!!!!!!!!!!!!!!!exitTrainMode!!!!!!!!!!!!!!!!!!!");
		if(train == true)
		{
			train = false;
		}
		trainer.resumeScheduler();
	}

	private void countPlayedNum()
	{
		playedNum++;
	}

	private void countTrainedNum()
	{
		trainedNum++;
	}

	private void checkTrainMode()
	{
		if(firstTrain == true)
		{
			firstTrain = false;
			playedNum = 0;
			trainedNum = 0;
			numInOneRound = minInOneRound; //initial iteration number in one round
		}
		else
		{
			if(playedNum == numInOneRound)
			{
				numInOneRound = setNextNumInOneRound(wrongDecision, numInOneRound);
				playedNum = 0;
				wrongDecision = false;
				enterTrainMode();
			}
			else //playedNum < numInOneRound
			{
				if(accuracy >= threshold)
				{
					exitTrainMode();
				}
			}
		}
	}

	private int setNextNumInOneRound(int numOfError, int currNumInOneRound)
	{
		int newValue, newNumInOneRound;

		if(numOfError == 0)
		{
			newValue = currNumInOneRound + minInOneRound;
			newNumInOneRound = (newValue >= maxInOneRound) ? maxInOneRound : newValue;
		}
		else
		{
			newValue = currNumInOneRound - (minInOneRound * numOfError);
			newNumInOneRound = (newValue <= 0) ? minInOneRound : newValue;
		}

		return newNumInOneRound;
	}

	private double getBandWidth(String remoteIP, Integer remotePort, int pingSize)
	{
		int pingDataSize = pingSize * 1024;
		Long bwByte = profiler.getBandWidth(remoteIP, remotePort, pingDataSize);
		double bwMByte = bwByte / (double)(1024*1024);

		return bwMByte;
	}
}
