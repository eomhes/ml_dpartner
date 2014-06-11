package cn.edu.pku.dpartner.comm.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

public final class SmartObjectInputStream extends ObjectInputStream
{
	public SmartObjectInputStream(final InputStream in) throws IOException
	{
		super(in);
		enableResolveObject(true);
	}

	protected Object resolveObject(Object obj) throws IOException
	{
		if(obj instanceof NotSerializableObjWrapper)
		{
			obj = ((NotSerializableObjWrapper)obj).unWrap();
		}
		
		return obj;
	}
}
