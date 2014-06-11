package cn.edu.pku.dpartner.comm;

import cn.edu.pku.dpartner.comm.messages.CommMessage;;

public interface EndpointHook
{
	public CommMessage beforeMsgSend(CommMessage msg);
	public CommMessage afterMsgSend(CommMessage msg);
	public CommMessage beforeMsgHandled(CommMessage msg);
	public CommMessage afterMsgHandled(CommMessage msg);
	public void setEndpoint(ChannelEndpoint channelEndpoint);
	public Long getDelay();
	public void setDelay(Long delay);
}
