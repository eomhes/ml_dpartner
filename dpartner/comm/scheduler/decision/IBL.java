package cn.edu.pku.dpartner.comm.scheduler.decision;

import java.util.*;
import java.util.List;
import java.io.IOException;

import cn.edu.pku.dpartner.comm.scheduler.db.DataBase;
import cn.edu.pku.dpartner.comm.scheduler.db.DataInstance;

public class IBL extends DecisionMaker
{
	private int dbSize;
	private DataBase dataBase;
	private DataInstance dataInstance;

	public IBL(int size)
	{
		super(size);
		dbSize = size;
		dataBase = new DataBase(dbSize);
		dataBase.setDataBase();
	}

	public boolean getDecision(double size, double bw)
	{
		double dataSizeFromDB, bandwidthFromDB;
		double distance, min;
		boolean decision;
		int index = dbSize + 1;
		
		decision = false;
		min = 10000000000.0;
		
		for(int i = 0; i < dbSize; i++)
		{
			dataInstance = dataBase.getDataInstance(i);
			distance = calculateDistance(dataInstance, size, bw);
			
			if(min > distance)
			{
				index = i;
				min = distance;
				decision = dataInstance.getLabel();
			} 
		}
		System.out.println("minimum distance: " + min + "index: " + index);


		return decision;
	}

	public double calculateDistance(DataInstance instance, double size, double bw)
	{
		double distance;
		double dataSizeFromDB, bandwidthFromDB;

		dataSizeFromDB = instance.getDataSize();
		bandwidthFromDB = instance.getBandwidth();

		distance = Math.sqrt(Math.pow(size - dataSizeFromDB, 2) + Math.pow(bw - bandwidthFromDB, 2));

		return distance;
	}

	public void updateDataBase(Long dataSize, double bandwidth, boolean isRemoteFaster)
	{
		System.out.println("IBL DATABASE UPDATE");
		dataBase.addDataInstance(dataSize, bandwidth, isRemoteFaster);
	}
	
	public void updateDecisionMaker()
	{
		System.out.println("DO NOTHING");
	}
}





