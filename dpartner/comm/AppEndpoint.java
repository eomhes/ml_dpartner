package cn.edu.pku.dpartner.comm;

import cn.edu.pku.dpartner.comm.type.ServerObject;
import cn.edu.pku.dpartner.comm.type.Stub;

public interface AppEndpoint
{

	public Object invokeMeasurement(final String targetBundleSymbolicName, final String targetClassName, final long targetServerObjectID, 
			final String methodSignature, final Object[] args) throws Throwable;
	/**
	 * invoke remote(also may be local) method, note: the target class name is not the internal name
	 * @param targetBundleSymbolicName
	 * @param targetClassName
	 * @param targetServerObjectID
	 * @param methodSignature
	 * @param args
	 * @return
	 */
	public Object invokeMethod(final String targetBundleSymbolicName, final String targetClassName, final long targetServerObjectID, 
			final String methodSignature, final Object[] args) throws Throwable;

	
	/**
	 * add the stub to the local AppEndpoit. This stub will be tracked by the AppEndpoit to check if it is garbage collected. 
	 * The information will be send to the server side to make the corresponding ServerObject can also be garbage collected. 
	 * @param stub
	 * @return
	 */
	public void addToLocalStub(Stub stub);
	
	/**
	 * export a serverobject which provide service for remote stub
	 * @param instanceID
	 * @param serverObj
	 * @return
	 */
	public void export(long instanceID, ServerObject serverObj);
	
	/**
	 * check if the app-obj-creation process is in  the comm-package's Deserializing Thread, if it is, and meanwhile if the obj being created is a stub,
	 * then if being deserialized, no remote call in the stub constructor will be executed.
	 * @param thread
	 * @return
	 */
	public boolean isInDeserializingThread(Thread thread);
	
	/**
	 * check if the app-obj-creation process is in  the comm-package's Serializing Thread, if it is, and meanwhile if the obj being created is a stub,
	 * then if being serialized, no remote call in the stub constructor will be executed.
	 * @param thread
	 * @return
	 */
	public boolean isInSerializingThread(Thread thread);
	

}
