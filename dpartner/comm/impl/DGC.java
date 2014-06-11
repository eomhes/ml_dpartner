package cn.edu.pku.dpartner.comm.impl;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.security.AccessController;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import cn.edu.pku.dpartner.comm.messages.CommMessage;
import cn.edu.pku.dpartner.comm.messages.GCMessage;
import cn.edu.pku.dpartner.comm.type.Stub;

public class DGC
{
	private ReferenceQueue refQueue = new ReferenceQueue();

	/**
	 * <bundlesymbolic-name, <classname#instanceID, stub-release-count>>
	 */
	private Hashtable<String, Hashtable<String, Integer>> pendingCleans = new Hashtable<String, Hashtable<String, Integer>>();
	
	 private Set refSet = new HashSet(5);

	private boolean running = true;

	private CleanTrackRunnable cleanTrackRunnable;

	private Thread cleanTrackThread;

	private EndpointImpl endpoint;

	private final static long CLEAN_INTERVAL = 60000; // 1 minute

	private class PhantomStubRef extends PhantomReference
	{
		private String targetBundleSymbolicName;

		private String targetServerObjReleaseID;

		public PhantomStubRef(Stub ref)
		{
			super(ref, refQueue);
			this.targetBundleSymbolicName = ref.getTargetBundleSymbolicName();
			this.targetServerObjReleaseID = ref.getTargetClassName() + "#"
					+ ref.getID();
		}

		public String getTargetBundleSymbolicName()
		{
			return targetBundleSymbolicName;
		}

		public String getTargetServerObjReleaseID()
		{
			return targetServerObjReleaseID;
		}
	}

	private class CleanTrackRunnable implements Runnable
	{
		public void run()
		{
			do
			{
				PhantomStubRef phantom = null;

				try
				{
					/*
					 * Wait for the phantom references to be enqueued.
					 */
					phantom = (PhantomStubRef) refQueue.remove(CLEAN_INTERVAL);
				}
				catch (InterruptedException e)
				{
				}

				if (phantom != null)
				{
					processPhantomRefs(phantom);
				}

				if (!pendingCleans.isEmpty())
				{
					makeCleanCalls();
				}
			}
			while (running || !pendingCleans.isEmpty());
		}
	}

	private void processPhantomRefs(PhantomStubRef phantom)
	{
		do
		{
			Hashtable<String, Integer> serverobjsToClean = pendingCleans
					.get(phantom.getTargetBundleSymbolicName());
			if (serverobjsToClean == null)
			{
				serverobjsToClean = new Hashtable<String, Integer>();
				pendingCleans.put(phantom.getTargetBundleSymbolicName(),
						serverobjsToClean);
			}
			Integer cnt = serverobjsToClean.get(phantom
					.getTargetServerObjReleaseID());
			if (cnt == null)
				cnt = 0;
			cnt++;
			serverobjsToClean.put(phantom.getTargetServerObjReleaseID(), cnt);
			refSet.remove(phantom);
		}
		while ((phantom = (PhantomStubRef) refQueue.poll()) != null);
	}

	private void makeCleanCalls()
	{
		for (String targetBundleSymbolicName : pendingCleans.keySet())
		{
			Hashtable<String, Integer> serverobjsToClean = pendingCleans
					.get(targetBundleSymbolicName);
			
			GCMessage gcMsg = new GCMessage();
			gcMsg.setServerObjsToClean(serverobjsToClean);
			try
			{
				endpoint.send(targetBundleSymbolicName, (CommMessage) gcMsg);
				pendingCleans.remove(targetBundleSymbolicName);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	public void trackStub(Stub stub)
	{
		refSet.add(new PhantomStubRef(stub));
	}

	public DGC(EndpointImpl endpoint)
	{
		this.endpoint = endpoint;
		cleanTrackThread = (Thread) AccessController
				.doPrivileged(new NewThreadAction(new CleanTrackRunnable(),
						"Clean-" + endpoint, true));
		cleanTrackThread.start();
	}

	public void dispose()
	{
		endpoint = null;
		refQueue = null;
		running = false;
		pendingCleans.clear();
		pendingCleans = null;
		cleanTrackThread.interrupt();
		cleanTrackThread = null;
		refSet.clear();
		refSet = null;
	}

}
