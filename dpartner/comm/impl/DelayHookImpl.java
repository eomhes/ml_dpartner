package cn.edu.pku.dpartner.comm.impl;

import cn.edu.pku.dpartner.comm.EndpointHook;
import cn.edu.pku.dpartner.comm.ChannelEndpoint;
import cn.edu.pku.dpartner.comm.messages.CommMessage;

public class DelayHookImpl implements EndpointHook
{
	 ChannelEndpoint channelEndpoint;
	 Long delay = 0L;
	@Override
	public CommMessage beforeMsgSend(CommMessage msg)
	{
		System.out.println("msg before send, sleep " + delay +" typeid: "+msg.getTypeID()+" xid:"+msg.getXID());
		try
		{
			Thread.currentThread().sleep(delay);
		}
		catch (InterruptedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return msg;
	}

	@Override
	public CommMessage afterMsgSend(CommMessage msg)
	{
	//	System.out.println("msg after send, typeid: "+msg.getTypeID()+" xid:"+msg.getXID());
		return msg;
	}

	@Override
	public CommMessage beforeMsgHandled(CommMessage msg)
	{
	//	System.out.println("msg before handled, typeid: "+msg.getTypeID()+" xid:"+msg.getXID());
		return msg;
	}

	@Override
	public CommMessage afterMsgHandled(CommMessage msg)
	{
	//	System.out.println("msg after handled, typeid: "+msg.getTypeID()+" xid:"+msg.getXID());
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
