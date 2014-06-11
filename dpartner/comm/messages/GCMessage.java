package cn.edu.pku.dpartner.comm.messages;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Hashtable;

public final class GCMessage extends CommMessage
{
	/**
	 * <classname#instanceID, release-count>
	 */
	private Hashtable<String, Integer> serverobjsToClean;
	
	public GCMessage()
	{
		super(GC);
	}

	GCMessage(final ObjectInputStream input) throws IOException,
			ClassNotFoundException
	{
		super(GC);
		serverobjsToClean = (Hashtable<String, Integer>)input.readObject();
	}

	public void setServerObjsToClean(Hashtable<String, Integer> serverobjsToClean)
	{
		this.serverobjsToClean = serverobjsToClean;
	}
	
	public Hashtable<String, Integer> getServerObjsToClean()
	{
		return this.serverobjsToClean;
	}
	
	protected void writeBody(ObjectOutputStream output) throws IOException
	{
		output.writeObject(serverobjsToClean);
	}

	public String toString() {
		final StringBuffer buffer = new StringBuffer();
		buffer.append("[GC] - XID: "); 
		buffer.append(xid);
		buffer.append(", serverobjects: "); 
		if(serverobjsToClean != null)
		{
			for (String classRealeaseID : serverobjsToClean.keySet())
			{
				buffer.append(classRealeaseID);
				buffer.append(" - ");
				buffer.append(serverobjsToClean.get(classRealeaseID));
			}
		}
		return buffer.toString();
	}
}
