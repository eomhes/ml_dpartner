package cn.edu.pku.dpartner.comm.type;

import java.io.Serializable;

public interface ServerObject extends Serializable
{
	/**
	 * get the server obj id, the stub of the a server obj should have the same id
	 * @return
	 */
	public long __getID__();
	
	/**
	 * get the stub for serializing
	 * @return
	 */
	public Stub __getStubForSerializing__();
}
