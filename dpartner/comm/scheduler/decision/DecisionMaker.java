package cn.edu.pku.dpartner.comm.scheduler.decision;

import java.util.*;
import java.util.List;
import java.io.IOException;

import cn.edu.pku.dpartner.comm.scheduler.db.DataBase;

public abstract class DecisionMaker
{
	private int dbSize;
	DataBase dataBase;

	public DecisionMaker(int size)
	{
		dbSize = size;
		dataBase = new DataBase(dbSize);
	}
	

	public boolean getDecision(double size, double bw)
	{
		return true;
	}

	public abstract void updateDataBase(Long dataSize, double bandwidth, boolean isRemoteFaster);
	public abstract void updateDecisionMaker();
}

