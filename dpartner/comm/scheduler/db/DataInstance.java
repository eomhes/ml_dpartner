package cn.edu.pku.dpartner.comm.scheduler.db;

import java.util.List;
import java.io.IOException;

public class DataInstance
{
	private double dataSize;
	private double bandwidth;
	private boolean label;

	public void setDataInstance(double size, double bw, boolean label)
	{
		this.dataSize = size;
		this.bandwidth = bw;
		this.label = label;
	}

	public double getDataSize()
	{
		return dataSize;
	}

	public double getBandwidth()
	{
		return bandwidth;
	}

	public boolean getLabel()
	{
		return label;
	}
}
