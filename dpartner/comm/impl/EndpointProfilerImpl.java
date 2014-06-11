package cn.edu.pku.dpartner.comm.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.edu.pku.dpartner.comm.AppEndpoint;
import cn.edu.pku.dpartner.comm.EndpointProfiler;
import cn.edu.pku.dpartner.comm.Entry;
import cn.edu.pku.dpartner.comm.api.AppApi;
import cn.edu.pku.dpartner.comm.messages.APIResultMessage;
import cn.edu.pku.dpartner.comm.messages.CommMessage;
import cn.edu.pku.dpartner.comm.messages.RemoteCallMessage;
import cn.edu.pku.dpartner.comm.util.APIType;
import cn.edu.pku.dpartner.comm.util.EndpointEventType;

public class EndpointProfilerImpl implements EndpointProfiler
{
	//First Key is MessageCategory
	//Value: first key in value is the XID of the message,Second is the startTime(millisecond), dataSize(Byte)
	private Map<Short,ConcurrentHashMap<Long,Long>> startTimeCache;
	private Map<Short,ConcurrentHashMap<Long,Long>> dataTransferCache;

	//
	private ConcurrentHashMap<String,ConcurrentHashMap<String,List<Long>>> remoteCallExecuteTime ;
	private Map<APIType,List<Long>> apiExecuteTime;
	
	//
	private ConcurrentHashMap<String,ConcurrentHashMap<String,List<Long>>> remoteCallDataTransfer;
	private Map<APIType,List<Long>> apiDataTransfer;
	
	
	
	private Map<Long,String> remoteCallClzName;
	private Map<Long,String> remoteCallMetName;
	
	public EndpointProfilerImpl(){
		remoteCallExecuteTime = new ConcurrentHashMap<String, ConcurrentHashMap<String,List<Long>>>();
		startTimeCache = new HashMap<Short,ConcurrentHashMap<Long,Long>>();
		for (short i=1;i<CommMessage.getTotalCategoryNum();i++)
			startTimeCache.put(i, new ConcurrentHashMap<Long,Long>());
		apiExecuteTime = new HashMap<APIType,List<Long>>();
		for (APIType a : APIType.values()){
			apiExecuteTime.put(a, new ArrayList<Long>());
		}
		
		remoteCallDataTransfer = new ConcurrentHashMap<String, ConcurrentHashMap<String,List<Long>>>();
		dataTransferCache = new HashMap<Short,ConcurrentHashMap<Long,Long>>();
		for (short i=1;i<CommMessage.getTotalCategoryNum();i++)
			dataTransferCache.put(i, new ConcurrentHashMap<Long,Long>());
		
		apiDataTransfer = new HashMap<APIType,List<Long>>();
		for (APIType a : APIType.values()){
			apiDataTransfer.put(a, new ArrayList<Long>());
		}
		
		remoteCallClzName = new HashMap<Long,String>();
		remoteCallMetName = new HashMap<Long,String>();
	}
	@Override
	public void recordMsg(CommMessage message)
	{
	}

	@Override
	public void updateStatistics(CommMessage message,EndpointEventType eventType) throws Throwable
	{
		if (message.isRequest()){
			//request msg, sent from oneside and received for the other.
			startTimeCache.get(message.getCategory()).put(message.getXID(),(new Date().getTime()));
			dataTransferCache.get(message.getCategory()).put(message.getXID(), message.getDataSize());
			if (message.getTypeID() == CommMessage.REMOTE_CALL){
				RemoteCallMessage  rcm = (RemoteCallMessage) message;
				remoteCallClzName.put(message.getXID(),rcm.getTargetClassName()+":"+rcm.getServerObjectID());
				remoteCallMetName.put(message.getXID(),rcm.getMethodSignature());
			}
				
		}
		if (message.isResult()){
			Long durTime = calDurTime(message);
			Long dataSize = calDataSize(message);
			if (durTime!=null){
				durTime = new Date().getTime() - durTime;
				switch(message.getTypeID()){
				case CommMessage.API_RESULT:
					apiExecuteTime.get(((APIResultMessage)message).getAPIType()).add(durTime);
					apiDataTransfer.get(((APIResultMessage) message).getAPIType()).add(dataSize);
					
					break;
				case CommMessage.REMOTE_CALL_RESULT:
					addToRemoteStatics(message,durTime,remoteCallExecuteTime);
					addToRemoteStatics(message,dataSize,remoteCallDataTransfer);
					break;
				//TODO add other two msg results!
				case CommMessage.STREAM_RESULT:
					break;
				case CommMessage.SYNCHRONIZE_RESULT:
					break;
				default:
					System.err.println("Profiler:: Unsupported result message!");
					break;
				}
			}
		}
	}
	


	
	private Long calDurTime(CommMessage message)
	{
		ConcurrentHashMap<Long, Long> cateStartTime;
		cateStartTime =startTimeCache.get(message.getCategory()); 
		Long durTime = cateStartTime.get(message.getXID());
		cateStartTime.remove(message.getXID());
		return durTime;
	}
	private Long calDataSize(CommMessage message)
	{
		ConcurrentHashMap<Long, Long> cateRequestDataSize;
		cateRequestDataSize = dataTransferCache.get(message.getCategory());
		Long dataSize = cateRequestDataSize.get(message.getXID());
		if (dataSize==null) dataSize=0L;
		dataSize += message.getDataSize();
		return dataSize;
	}
	private void addToRemoteStatics(CommMessage message, Long data, ConcurrentHashMap<String,ConcurrentHashMap<String,List<Long>>> target)
	{
		ConcurrentHashMap<String,List<Long>> clzMethodsTime;
		List<Long> executeTimes;
		String clzName = remoteCallClzName.get(message.getXID());
		String metName = remoteCallMetName.get(message.getXID());
		if (clzName==null || metName == null){
			System.out.println("Profiler:: clzName or metName is null!");
			return;
		}
		clzMethodsTime = target.get(clzName);
		if (clzMethodsTime==null){
			clzMethodsTime = new ConcurrentHashMap<String,List<Long>>();
			target.put(clzName,clzMethodsTime);
		}
		executeTimes = clzMethodsTime.get(metName);
		if (executeTimes==null){
			executeTimes = new ArrayList<Long>();
			clzMethodsTime.put(metName, executeTimes);
		}
		executeTimes.add(data);
	}

	public Map<APIType,List<Long>> getApiExecuteTime(){
		return apiExecuteTime;
	}
	
	public ConcurrentHashMap<String,ConcurrentHashMap<String,List<Long>>> getRemoteCallExecuteTime(){
		return remoteCallExecuteTime;
	}
	public Map<APIType,List<Long>> getApiDataTransfer()
	{
		return apiDataTransfer;
	}
	public ConcurrentHashMap<String,ConcurrentHashMap<String,List<Long>>> getRemoteCallDataTransfer()
	{
		return remoteCallDataTransfer;
	}
	public Long getBandWidth(String IP,Integer port, Integer dataSize){
		final int messageHead = 656 * 2;
		AppEndpoint endPoint = Entry.getEndpoint(false);
		byte[] useless = new byte[dataSize];
		Object[] args = new Object[1];
		
		for(int i = 0; i < dataSize; i++)
		{
			useless[i] = Byte.MAX_VALUE;
		}

		args[0] = useless;

		Date startTime = new Date();

		try
		{
			endPoint.invokeMeasurement("Remote", "cn/edu/pku/getRemoteBandWidth", -1L, "", args);
		}
		catch(Throwable e)
		{
			e.printStackTrace();
		}
		
		Date endTime = new Date();

		return (dataSize * 8 * 2 + messageHead) * 1000 
				/ (endTime.getTime() - startTime.getTime());
	}
}

