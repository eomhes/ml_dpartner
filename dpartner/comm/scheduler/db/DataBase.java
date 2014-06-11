package cn.edu.pku.dpartner.comm.scheduler.db;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.List;
import java.io.IOException;

public class DataBase
{
	private int length;
	private int index;
	private String filepath = "sdcard/database.txt";
	private DataInstance[] dataInstances;

	public DataBase(int size)
	{
		length = size;
		index = 0;
		dataInstances = new DataInstance[length];

		for(int i = 0; i < length; i++)
		{
			dataInstances[i] = new DataInstance();
		}
	}

	public void setDataBase()
	{
		String line = " ";
		String[] temp;
		//int index;

		try
		{
			//index = 0;
			BufferedReader br = new BufferedReader(new FileReader(filepath));

			while((line = br.readLine()) != null)
			{
				temp = line.split(" ");
				dataInstances[index].setDataInstance(Double.parseDouble(temp[0]), Double.parseDouble(temp[1]), Boolean.parseBoolean(temp[2]));
				index++;
				System.out.println(index);
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}

	public void addDataInstance(Long dataSize, double bandwidth, boolean isRemoteFaster)
	{
		int newIndex = index%length;
		System.out.println("Index :" + newIndex);
		dataInstances[newIndex].setDataInstance(dataSize/1024, bandwidth, isRemoteFaster);
		index++;
	}

	public DataInstance getDataInstance(int index)
	{
		return dataInstances[index];
	}
}
