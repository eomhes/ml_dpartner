package cn.edu.pku.dpartner.comm.scheduler.util;

import java.util.List;

public class Estimator
{
	public static double singleMovingAverage(List<Long> data)
	{
		double estimatedValue;
		int sumValue = 0;
		int windowSize = 6;

		if(data.size() < windowSize)
		{
			for(int i = 0; i < data.size(); i++)
			{
		//		System.out.println("Individual data size: " + data.get(i));
				sumValue += data.get(i);
			}
			estimatedValue = (double)sumValue / data.size();
		}
		else
		{
			for(int i = (data.size()-windowSize); i < data.size(); i++)
			{
		//		System.out.println("Individual data size: " + data.get(i));
				sumValue += data.get(i);
			}
			estimatedValue = (double)sumValue / windowSize;
		}

		return estimatedValue;
	}
}
