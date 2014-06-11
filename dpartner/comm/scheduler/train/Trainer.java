package cn.edu.pku.dpartner.comm.scheduler.train;

import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.io.IOException;
import cn.edu.pku.dpartner.comm.AppEndpoint;
import cn.edu.pku.dpartner.comm.EndpointProfiler;
import cn.edu.pku.dpartner.comm.impl.EndpointImpl;
import cn.edu.pku.dpartner.comm.scheduler.Scheduler;
import cn.edu.pku.dpartner.comm.scheduler.decision.IBL;

public class Trainer
{
	private int interval;
	private EndpointImpl appEndpoint;
	private Scheduler scheduler;
	private IBL decisionMaker;

	public Trainer(Scheduler scheduler)
	{
		this.scheduler = scheduler;
	}

	public void pauseScheduler()
	{
		System.out.println("!!!!!!!!!!!!!!!!!pauseScheduler!!!!!!!!!!!!!!!!!!");
		scheduler.pauseScheduler();
	}
	
	public void resumeScheduler()
	{
		System.out.println("!!!!!!!!!!!!!!!!!resumeScheduler!!!!!!!!!!!!!!!!!");
		scheduler.resumeScheduler();
	}

	public void updateDataBase(Long dataSize, double bandwidth, boolean isRemoteFaster)
	{
		System.out.println("Data size: " + dataSize);
		System.out.println("Bandwidth: " + bandwidth);
		System.out.println("Remote is faster?: " + isRemoteFaster);
		System.out.println("Update Database");
		decisionMaker.updateDataBase(dataSize, bandwidth, isRemoteFaster);
		//decisionMaker.updateDecisionMaker();
	}

	public void bindAppEndpoint(AppEndpoint endpoint)
	{
		appEndpoint = (EndpointImpl) endpoint;
	}

	public void bindDecisionMaker(IBL decisionMaker)
	{
		this.decisionMaker = decisionMaker; 
	}
}
