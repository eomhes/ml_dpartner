package cn.edu.pku.dpartner.comm.impl;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;

import cn.edu.pku.dpartner.comm.type.ServerObject;

public final class SmartObjectOutputStream extends ObjectOutputStream
{
	// only work for synchronization
	private boolean synchronizeFlag = false;
	/**
	 * must used when passing obj parameters
	 * @param out
	 * @throws IOException
	 */
	public SmartObjectOutputStream(OutputStream out) throws IOException
	{
		super(out);
		this.enableReplaceObject(true);
	}

	protected Object replaceObject(Object obj) throws IOException
	{
		if (obj instanceof ServerObject && !synchronizeFlag)
		{
			ServerObject so = (ServerObject) obj;
			obj = so.__getStubForSerializing__();
		}
		else if(obj != null && !(obj instanceof Serializable))
		{
			obj = new NotSerializableObjWrapper(obj);
		}
		synchronizeFlag = false;
		return obj;
	}
	
	public void enableSynchronization()
	{
		synchronizeFlag = true;
	}
}
