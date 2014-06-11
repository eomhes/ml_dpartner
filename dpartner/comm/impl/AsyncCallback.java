package cn.edu.pku.dpartner.comm.impl;

import cn.edu.pku.dpartner.comm.messages.CommMessage;

/**
 * Internal callback for asynchronous message calls
 */
public interface AsyncCallback {

	public void result(CommMessage msg);

}
