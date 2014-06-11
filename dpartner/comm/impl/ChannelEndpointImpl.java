package cn.edu.pku.dpartner.comm.impl;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import cn.edu.pku.dpartner.comm.EndpointHook;
import cn.edu.pku.dpartner.comm.ChannelEndpoint;
import cn.edu.pku.dpartner.comm.CommConstants;
import cn.edu.pku.dpartner.comm.Entry;
import cn.edu.pku.dpartner.comm.NetworkChannel;
import cn.edu.pku.dpartner.comm.NetworkChannelFacade;
//import cn.edu.pku.dpartner.comm.impl.EndpointImpl.Target;
import cn.edu.pku.dpartner.comm.messages.CommMessage;
import cn.edu.pku.dpartner.comm.util.EndpointEventType;

public abstract class ChannelEndpointImpl implements ChannelEndpoint
{
	private ReentrantReadWriteLock lock_s = new ReentrantReadWriteLock();

	private ReentrantReadWriteLock lock_d = new ReentrantReadWriteLock();

	private Set<Thread> serializingThreadSet = new HashSet<Thread>();

	private Set<Thread> deserializingThreadSet = new HashSet<Thread>();

	private ThreadGroup threadPool;

	protected ArrayList<Runnable> workQueue = new ArrayList<Runnable>();

	protected NetworkChannelFacade channelFacade;

	private EndpointHook appHooker;

	public ChannelEndpointImpl(NetworkChannelFacade channelFacade)
	{
		String hookName="";
		bindNetworkChannelFacade(channelFacade);
		initThreadPool();
		try
		{
			Properties bundleSN2URIProp = new Properties();
			FileInputStream in;
			boolean entryFromServer = Boolean.parseBoolean(System
					.getProperties().get(CommConstants.Entry_FROM_SERVER)
					.toString());
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
			hookName = (String) bundleSN2URIProp.get("Hook-ClassName");
			appHooker = (EndpointHook) Class.forName(hookName).newInstance();
			System.out.println("Use hook: " + hookName);
		}
		catch (Exception e)
		{
			System.out.println("Failed to use specific hook: " + hookName+", Use default time hook");
			appHooker = new TimeHookImpl();
		}
		appHooker.setEndpoint(this);
	}

	@Override
	public void updateDeserializingThread(Thread thread,
			boolean addTrueRemoveFalse)
	{

		if (addTrueRemoveFalse)// add
		{
			lock_d.writeLock().lock();
			deserializingThreadSet.add(thread);
			lock_d.writeLock().unlock();
		}
		else
		// remove
		{
			lock_d.writeLock().lock();
			deserializingThreadSet.remove(thread);
			lock_d.writeLock().unlock();
		}
	}

	private void initThreadPool()
	{
		threadPool = new ThreadGroup("WorkerThreads");
		for (int i = 0; i < CommConstants.MAX_THREADS_PER_ENDPOINT; i++)
		{
			final Thread t = new Thread(threadPool, "WorkerThread" + i)
			{
				public void run()
				{
					try
					{
						while (!isInterrupted())
						{
							final Runnable r;
							synchronized (workQueue)
							{
								while (workQueue.isEmpty())
								{
									// System.out.println(Thread.currentThread().getName()
									// + "is running!!");
									workQueue.wait();
								}
								// System.out.println(Thread.currentThread().getName()
								// + "An new r is be called!!!!!!!!");
								r = (Runnable) workQueue.remove(0);
							}
							r.run();
						}
					}
					catch (InterruptedException ie)
					{
						ie.printStackTrace();
					}
				}
			};
			t.start();
		}
	}

	@Override
	public void updateSerializing(Thread thread, boolean addTrueRemoveFalse)
	{
		if (addTrueRemoveFalse)// add
		{
			lock_s.writeLock().lock();
			serializingThreadSet.add(thread);
			lock_s.writeLock().unlock();
		}
		else
		// remove
		{
			lock_s.writeLock().lock();
			serializingThreadSet.remove(thread);
			lock_s.writeLock().unlock();
		}

	}

	@Override
	public URI getRemoteAddress(String targetBundleSymbolicName)
	{
		NetworkChannel channel = channelFacade
				.getNetworkChannel(targetBundleSymbolicName);
		if (channel != null)
			return channel.getRemoteAddress();
		return null;
	}

	@Override
	public void receivedMessage(NetworkChannel channel, CommMessage msg)
			throws RemoteCommException
	{

	}

	@Override
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

	@Override
	public void bindNetworkChannelFacade(NetworkChannelFacade channelFacade)
	{
		this.channelFacade = channelFacade;
		this.channelFacade.bindChannelEndpoint(this);
	}

	@Override
	public void dispose()
	{
		if (threadPool != null)
			threadPool.destroy();
		if (workQueue != null)
			workQueue.clear();
		if (channelFacade != null)
			channelFacade.closeAllChannels();

		serializingThreadSet.clear();
		deserializingThreadSet.clear();

	}

	public void NotifyWorkQueue(Runnable r)
	{
		synchronized (workQueue)
		{
			workQueue.add(r);
			workQueue.notify();
		}
	}

	public EndpointHook getEndpointHooker()
	{
		return appHooker;
	}

	public boolean isInSerializingThread(Thread thread)
	{
		lock_s.readLock().lock();
		boolean in = serializingThreadSet.contains(thread);
		lock_s.readLock().unlock();
		return in;
	}

	public boolean isInDeserializingThread(Thread thread)
	{
		lock_d.readLock().lock();
		boolean in = deserializingThreadSet.contains(thread);
		lock_d.readLock().unlock();
		return in;
	}

	public NetworkChannelFacade getChannelFacade()
	{
		return channelFacade;
	}

	@Override
	public abstract void profileMsg(CommMessage msg, EndpointEventType eventType);

	public EndpointHook getHook()
	{
		return appHooker;
	}
}
