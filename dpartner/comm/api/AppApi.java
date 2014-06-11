package cn.edu.pku.dpartner.comm.api;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.edu.pku.dpartner.comm.impl.SmartObjectInputStream;
import cn.edu.pku.dpartner.comm.impl.SmartObjectOutputStream;
import cn.edu.pku.dpartner.comm.impl.tcp.TCPChannelFacade;

import cn.edu.pku.dpartner.comm.messages.APIRequestMessage;
import cn.edu.pku.dpartner.comm.messages.APIResultMessage;
import cn.edu.pku.dpartner.comm.messages.CommMessage;
import cn.edu.pku.dpartner.comm.util.APIType;

public class AppApi
{
	private URL url;
	private String channelUID;
	private long xid=0;
	private String ipAddress;
	private int port;
	private boolean isHTTP = false;

	private Socket socket = null;

	private ObjectInputStream input;

	private ObjectOutputStream output;

	public AppApi(String ipAddress,int port){
		this.ipAddress = ipAddress;
		this.port = port;
		try
		{
			url = new URL("http",ipAddress,port,"/dpartner");
		}
		catch (MalformedURLException e)
		{
			e.printStackTrace();
		}
		channelUID = String.valueOf((long)(Math.random() * Integer.MAX_VALUE));
	}
	public void setTCP(){
		isHTTP = false;
	};
	public void setHTTP(){
		isHTTP = true;
	};
	private APIResultMessage sendMessage(CommMessage message){
		if (isHTTP) return sendMessageHTTP(message);
		else return sendMessageTCP(message);			
	}
	
	private APIResultMessage sendMessageTCP(CommMessage message) {
		CommMessage msg=null;
		if (socket==null || socket.isClosed())
			openSocket();
		try
		{
			output = new SmartObjectOutputStream(new BufferedOutputStream(
						socket.getOutputStream()));
			output.flush();
			message.send(output);
			input = new SmartObjectInputStream(new BufferedInputStream(socket
						.getInputStream()));
			msg = CommMessage.parse(input);
				socket.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		catch (ClassNotFoundException e)
		{
			e.printStackTrace();
		}
		
		return (APIResultMessage) msg;
	}
	private void openSocket()
	{
		try
		{
			socket = new Socket(ipAddress, port);
			URI.create(getProtocol() + "://" 
					+ socket.getLocalAddress().getHostName() + ":"
					+ socket.getLocalPort());
			socket.setKeepAlive(true);
			socket.setTcpNoDelay(true);
		}
		catch (UnknownHostException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public 	ConcurrentHashMap<String, List<Long>>  getTest(){
		APIResultMessage retMsg;
		APIRequestMessage reqMsg = new APIRequestMessage();
		reqMsg.setAPIType(APIType.getTest);
		reqMsg.setXID(getXID());
		retMsg = sendMessage(reqMsg);
		if (retMsg.getResult() instanceof ConcurrentHashMap<?,?>)
			return (ConcurrentHashMap<String, List<Long>>) retMsg.getResult();
		else return null;
	}
	private long getXID()
	{
		return Long.parseLong(channelUID)+(xid++);
	}
	
	public List<String> getLocalClasses()
	{
		List<String> arr = (List<String>) sendQuery(APIType.getLocalClasses,null);
		return arr;
		
	}
	public List<String> getRemoteClasses()
	{
		List<String> arr = (List<String>) sendQuery(APIType.getRemoteClasses,null);
		return arr;
	}
	private List<String> getAll()
	{
		ArrayList<String> all = (ArrayList<String>)getLocalClasses();
		all.addAll(getRemoteClasses());
		return all;
	}
	
	public Boolean executeOnLocal(String clzName,Long objId)
	{
		HashMap<String,Object> args = new HashMap<String,Object>();
		args.put("clzName", clzName);
		args.put("objID", objId);
		Boolean ret = (Boolean) sendQuery(APIType.executeOnLocal,args);
		return ret;
	}	
	public Boolean executeOnRemote(String clzName, Long objId)
	{
		HashMap<String,Object> args = new HashMap<String,Object>();
		args.put("clzName", clzName);
		args.put("objID", objId);
		Boolean ret = (Boolean)sendQuery(APIType.executeOnRemote,args);
		return ret;
	}
	public Long getDelay(){
		Long ret = (Long) sendQuery(APIType.getDelay,null);
		return ret;
	}
	public Boolean setDelay(Long delay){
		HashMap<String,Object> args = new HashMap<String,Object>();
		args.put("delay", delay);
		Boolean ret = (Boolean)sendQuery(APIType.setDelay,args);
		return ret;
	}
	/**
	 * 
	 * @param clzName
	 * @return
	 */
	public List<Long> getObjIds(String clzName){
		HashMap<String,Object> args = new HashMap<String,Object>();
		args.put("clzName", clzName);
		List<Long> allObj = (List<Long>) sendQuery(APIType.getObjectsId,args);
		return allObj;
	}
	public Map<APIType,List<Long>> getApiExecuteTime(){
		return (Map<APIType,List<Long>>) sendQuery(APIType.apiExecuteTime,null);
	}
	public ConcurrentHashMap<String,ConcurrentHashMap<String,List<Long>>> getClassExecuteTime(){
		return (ConcurrentHashMap<String,ConcurrentHashMap<String,List<Long>>>) sendQuery(APIType.classExecuteTime,null);	
	}
	public Map<APIType,List<Long>> getApiDataTransfer(){
		return (Map<APIType,List<Long>>) sendQuery(APIType.apiDataTransfer,null);
	}
	public ConcurrentHashMap<String,ConcurrentHashMap<String,List<Long>>> getClassDataTransfer(){
		return (ConcurrentHashMap<String,ConcurrentHashMap<String,List<Long>>>) sendQuery(APIType.classDataTransfer,null);
		
	}
	public void getBandWidthTest(int dataSize)
	{
		HashMap<String,Object> args = new HashMap<String,Object>();
		byte [] useless = new byte[dataSize];
		int i;
		for (i=0;i<dataSize;i++) useless[i]=Byte.MAX_VALUE;
		args.put("uselessData", useless);
		sendQuery(APIType.bandWidthTest,args);
		return;
	}
	public Long getBandWidth(String ip,int port, int datasize){
		HashMap<String,Object> args = new HashMap<String,Object>();
		args.put("IP", ip);
		args.put("port", new Integer(port));
		args.put("dataSize", new Integer(datasize));
		return (Long)sendQuery(APIType.getBandWidth,args);
	}
	public Object executeDIYMethod(String name,HashMap<String,Object> args){
		args.put("executeClassName", name);
		return sendQuery(APIType.executeDIY,args);
	}
	private Object sendQuery(APIType type,Map<String,Object> args){
		APIResultMessage retMsg;
		APIRequestMessage reqMsg = new APIRequestMessage();
		reqMsg.setAPIType(type);
		reqMsg.setXID(getXID());
		reqMsg.setAPIArgs(args);
		retMsg = sendMessage(reqMsg);
		if (retMsg!=null)
			return retMsg.getResult();
		else return null;
	}
	private APIResultMessage sendMessageHTTP(CommMessage message){
		APIResultMessage retMsg = null;
		try
		{
			final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type",
					"application/x-dpartner");
			connection.setRequestProperty("channelID", channelUID);
			connection.setUseCaches(false);
			connection.setDoInput(true);
			connection.setDoOutput(true);
			message.send(new SmartObjectOutputStream(connection.getOutputStream()));
			final SmartObjectInputStream in = new SmartObjectInputStream(connection
					.getInputStream());
			retMsg = (APIResultMessage)CommMessage.parse(in);
		}
		catch (ProtocolException e1)
		{
			e1.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		catch (ClassNotFoundException e)
		{
			e.printStackTrace();
		}
		return retMsg;
	}
	public String getProtocol()
	{
		return TCPChannelFacade.PROTOCOL;
	}
	
}
