package cn.edu.pku.dpartner.comm;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.edu.pku.dpartner.comm.messages.CommMessage;
import cn.edu.pku.dpartner.comm.util.APIType;
import cn.edu.pku.dpartner.comm.util.EndpointEventType;

public interface EndpointProfiler
{
	public void recordMsg(CommMessage message);
	public void updateStatistics(CommMessage message,EndpointEventType eventType) throws Throwable;
	public Map<APIType,List<Long>> getApiExecuteTime();
	public ConcurrentHashMap<String,ConcurrentHashMap<String,List<Long>>> getRemoteCallExecuteTime();
	public Map<APIType,List<Long>> getApiDataTransfer();
	public ConcurrentHashMap<String,ConcurrentHashMap<String,List<Long>>> getRemoteCallDataTransfer();
	public Long getBandWidth(String IP,Integer port, Integer dataSize);
	
//	public ConcurrentHashMap<String, ConcurrentHashMap<String,List<Long>>> getExecuteTime();
//	public ConcurrentHashMap<String,List<Long>> getClassExecuteTime(String clzName);
//	public List<Long> getMethodExecuteTime(String clzName,String methodName);
	
}
