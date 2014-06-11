package cn.edu.pku.dpartner.comm.scheduler;

import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.io.IOException;

import cn.edu.pku.dpartner.comm.AppEndpoint;
import cn.edu.pku.dpartner.comm.EndpointProfiler;
import cn.edu.pku.dpartner.comm.impl.EndpointImpl;
import cn.edu.pku.dpartner.comm.impl.EndpointProfilerImpl;
//import cn.edu.pku.dpartner.comm.scheduler.decision.DecisionMaker;
import cn.edu.pku.dpartner.comm.scheduler.decision.IBL;
import cn.edu.pku.dpartner.comm.scheduler.util.Estimator;
import cn.edu.pku.dpartner.comm.scheduler.train.Trainer;

public class Scheduler extends Thread
{
	private int interval;
	private int length; //database length
	private double bw;
	private double estimatedDataSize;
	private boolean decision;
	private boolean paused;
	private final Object LOCK = new Object();

	private String remoteIP = "192.168.1.141";
	private Integer remotePort = 1617;
	private int pingSize = 10;

	private IBL decisionMaker;
	private Trainer trainer;
	private EndpointImpl appEndpoint;
	private EndpointProfilerImpl endpointProfiler;

	ConcurrentHashMap<String, ConcurrentHashMap<String, List<Long>>> dataClass;

	public Scheduler(int size)
	{
		interval = 10000; //currently, use a fixed interval
		length = size;
		paused = false;
		decisionMaker = new IBL(length);
		trainer = new Trainer(this);
		trainer.bindDecisionMaker(decisionMaker);
	}

	public void run()
	{
		while(true)
		{
			if(paused)
			{
				/*
				try
				{
					LOCK.wait();
				}
				catch(InterruptedException e)
				{
					e.printStackTrace();
				}
				*/
			} // if
			else 
			{
				bw = getBandWidth(remoteIP, remotePort, pingSize);
				dataClass = getClassDataSize();
				for(String clzName : dataClass.keySet())
				{
					decision = true;
					if(appEndpoint.remotableClasses.contains(clzName))
					{
						ConcurrentHashMap<String, List<Long>> dataMethod = dataClass.get(clzName);

						for(String methodName : dataMethod.keySet())
						{
							List<Long> dataSizeMethod = dataMethod.get(methodName);
							estimatedDataSize = Estimator.singleMovingAverage(dataSizeMethod)/1024; //unit is kByte
							//System.out.println("Data Size: " + estimatedDataSize);
							if(decisionMaker.getDecision(estimatedDataSize, bw) == true)
							{
								decision = true;
							}
							else
							{
								decision = false;
							}
						}

						String[] splits = clzName.split(":");
						String newClzName = splits[0] + "|" + splits[1];

						if(decision == true)
						{
							System.out.println("==========Scheduled as Remote==========");
							if(appEndpoint.notInServer.contains(newClzName))
							{
								appEndpoint.synchronizeLocalToServer(splits[0], Long.parseLong(splits[1]));
							}
						}
						else
						{
							System.out.println("==========Scheduled as Local==========");
							if(!appEndpoint.notInServer.contains(newClzName))
							{
								appEndpoint.synchronizeServerToLocal(splits[0], Long.parseLong(splits[1]));
							}
						}
					}
				}
				try
				{
					Thread.sleep(interval);
				}
				catch(InterruptedException e)
				{
					e.printStackTrace();
				}
			} //else
		}
	}

	public void bindAppEndpoint(AppEndpoint endpoint)
	{
		appEndpoint = (EndpointImpl) endpoint;
		trainer.bindAppEndpoint(appEndpoint);
	}

	public void bindProfiler(EndpointProfiler profiler)
	{
		endpointProfiler = (EndpointProfilerImpl) profiler;
	}

	public Trainer getTrainer()
	{
		return trainer;
	}

	public void resumeScheduler()
	{
		paused = false;
	}

	public void pauseScheduler()
	{
		paused = true;
	}
		

	private ConcurrentHashMap<String, ConcurrentHashMap<String, List<Long>>> getClassDataSize()
	{
		return endpointProfiler.getRemoteCallDataTransfer();
	}

	private double getBandWidth(String remoteIP, Integer remotePort, int pingSize)
	{
		int pingDataSize = pingSize * 1024;
		Long bwByte = endpointProfiler.getBandWidth(remoteIP, remotePort, pingDataSize);
		double bwMByte = bwByte / (double)(1024*1024);

		return bwMByte;
	}
}
