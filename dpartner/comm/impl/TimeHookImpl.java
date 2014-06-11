package cn.edu.pku.dpartner.comm.impl;

import cn.edu.pku.dpartner.comm.EndpointHook;
import cn.edu.pku.dpartner.comm.ChannelEndpoint;
import cn.edu.pku.dpartner.comm.messages.CommMessage;
import cn.edu.pku.dpartner.comm.util.EndpointEventType;

public class TimeHookImpl implements EndpointHook
{
	 ChannelEndpoint channelEndpoint;
	 Long delay = 0L;
	@Override
	public CommMessage beforeMsgSend(CommMessage msg)
	{
	//	System.out.println("msg before send, sleep " + delay +" typeid: "+msg.getTypeID()+" xid:"+msg.getXID());
		try
		{
			Thread.currentThread().sleep(delay);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		return msg;
	}

	@Override
	public CommMessage afterMsgSend(CommMessage msg)
	{
		return msg;
	}

	@Override
	public CommMessage beforeMsgHandled(CommMessage msg)
	{
		if (msg!=null){
			if (msg.getTypeID()!=CommMessage.GC){
				channelEndpoint.profileMsg(msg,EndpointEventType.afterMsgHandled);
			}
		}
		return msg;
	}

	@Override
	public CommMessage afterMsgHandled(CommMessage msg)
	{
		if (msg!=null){
			if (msg.getTypeID()!=CommMessage.GC){
				channelEndpoint.profileMsg(msg,EndpointEventType.afterMsgHandled);
			}
		}
		return msg;
	}
	
	@Override
	public void setEndpoint(ChannelEndpoint channelEndpoint)
	{
		this.channelEndpoint = channelEndpoint;
	}
	public void setDelay(Long delay){
		this.delay = delay;
	}

	public Long getDelay()
	{
		return delay;
	}

	
}
