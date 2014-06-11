package cn.edu.pku.dpartner.comm.type;

import java.io.Serializable;

public interface Stub extends Serializable
{
	/**
	 * set id of an server obj to the stub
	 * @param id
	 */
	public void setID(long id);
	
	/**
	 * get the id of the stub, for invoking the methods of the corresponding server obj (with the same id)
	 * @return
	 */
	public long getID(); 
	
	
	public String getTargetBundleSymbolicName();
	
	public String getTargetClassName();
}
