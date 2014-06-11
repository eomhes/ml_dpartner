package cn.edu.pku.dpartner.comm.impl;

import cn.edu.pku.dpartner.comm.messages.CommMessage;

/**
 * callback that signals when the result has become available.
 */
public class WaitingCallback implements AsyncCallback
{

	private CommMessage result;

	public synchronized void result(CommMessage msg)
	{
		result = msg;
		this.notifyAll();
	}

	public synchronized CommMessage getResult()
	{
		return result;
	}
}