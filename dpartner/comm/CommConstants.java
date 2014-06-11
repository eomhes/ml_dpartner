package cn.edu.pku.dpartner.comm;

public class CommConstants
{
	public static final String Entry_FROM_SERVER = "entry-from-server";
	
	public static final String CONFIG = "config.properties";
	
	public static final String COMM_CHANNEL = "dpartner.comm-channel";
	
	public static final String BSN2URI_PREFIX = "bsn2uri@";
	
	public static final String SERVLET_NAME = "servlet-name";
	
	public static final String BUNDLE_SYMBOLICNAME = "Bundle-SymbolicName";;
	
	public static final boolean SHOW_DEBUG_INFO = Boolean.valueOf(System.getProperty("dpartner.debuginfo", "true"));
	
	public static boolean LOCAL_OPTIMIZE = Boolean.valueOf(System.getProperty("dpartner.localoptimize", "true"));
	
	public static final long SERVER_ID_BEGIN = 4000000000000000000L;
	
	/**
	 * the listening port
	 */
	private static int __DPARTNER_TCP_PORT__ = Integer.valueOf(System.getProperty("dpartner.tcp.port", "1616"));
	public static int DPARTNER_TCP_PORT = __DPARTNER_TCP_PORT__ > 0 ? __DPARTNER_TCP_PORT__ : 1616;
	
	private static int __DPARTNER_TCP_API_PORT__ = Integer.valueOf(System.getProperty("dpartner.tcp.port", "1617"));
	public static int DPARTNER_TCP_API_PORT = __DPARTNER_TCP_API_PORT__ > 0 ? __DPARTNER_TCP_API_PORT__ : 1617;
	
	private static int __DPARTNER_HTTP_PORT__ = Integer.valueOf(System.getProperty("org.osgi.service.http.port", "80"));
	public static int DPARTNER_HTTP_PORT = __DPARTNER_HTTP_PORT__ > 0 ? __DPARTNER_HTTP_PORT__ : 80;
	
	private static int __DPARTNER_HTTP_API_PORT__ = Integer.valueOf(System.getProperty("org.osgi.service.http.port", "1617"));
	public static int DPARTNER_HTTP_API_PORT = __DPARTNER_HTTP_API_PORT__ > 0 ? __DPARTNER_HTTP_API_PORT__ : 1617;
	
	/**
	 * the number of threads for processing the incoming method call message  
	 */
	private static int __MAX_THREADS_PER_ENDPOINT__ = Integer.valueOf(System.getProperty("dpartner.workerthread.all", "5"));
	public static int MAX_THREADS_PER_ENDPOINT = __MAX_THREADS_PER_ENDPOINT__ > 1 ? __MAX_THREADS_PER_ENDPOINT__ : 1;
	
	public static int __MAX_THREADS_FOR_SERVLET_REPLY_MSG_PROCESSING__ = Integer.valueOf(System.getProperty("dpartner.workerthread.http", "2"));
	public static int MAX_THREADS_FOR_SERVLET_REPLY_MSG_PROCESSING = __MAX_THREADS_FOR_SERVLET_REPLY_MSG_PROCESSING__ > 1 ? __MAX_THREADS_FOR_SERVLET_REPLY_MSG_PROCESSING__ : 1;
	/**
	 * the timeout for receiving the result of remote method call
	 */
	public static int __TIMEOUT__ = Integer.valueOf(System.getProperty("dpartner.timeout", "120000"));
	public static final int TIMEOUT = __TIMEOUT__ > 120000 ? __TIMEOUT__ : 120000;

	public static final String DEFAULT_HOOK_NAME = "cn.edu.pku.dpartner.comm.impl.TimeHookImpl";
}
