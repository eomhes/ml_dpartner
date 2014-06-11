package cn.edu.pku.dpartner.comm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

import android.os.Environment;

import cn.edu.pku.dpartner.comm.impl.APIEndpointImpl;
import cn.edu.pku.dpartner.comm.impl.EndpointImpl;
import cn.edu.pku.dpartner.comm.impl.http.HTTPChannelFacade;
import cn.edu.pku.dpartner.comm.impl.tcp.TCPChannelFacade;

public class Entry
{
	private static AppEndpoint endpoint = null;

	private static APIEndpoint apiEndpoint = null;

	private static String APP_NAME = null;

	private static String LOCAL_ADDRESS = null;

	private static String REMOTE_ADDRESS = null;

	private static String dirPath;

	private static final String FILENAME_STRING = "CommConstants.CONFIG";

	// don't init the COMM_TYPE!!
	private static String COMM_TYPE = "tcp";

	public static void init(String appName, String localAddress,
			String remoteAddress)
	{
		APP_NAME = appName;
		LOCAL_ADDRESS = localAddress;
		REMOTE_ADDRESS = remoteAddress;
		
		// dirPath = "/mnt/sdcard/dpartner/" + APP_NAME;
		dirPath = Environment.getExternalStorageDirectory().getAbsolutePath()
				+ "/dpartner/" + APP_NAME;
		System.out.println("The dpartner path is:" + dirPath);
		File dir = new File(dirPath);
		
		if (!dir.exists())
		{
			dir.mkdirs();
		}

		File file = new File(dir, "CommConstants.CONFIG");
		if (!file.exists())
		{
			System.out.println("CommConstants.CONFIG does not exsits! Create it: "+file.getPath());
			Properties bundleSN2URIProp_default = new Properties();
			bundleSN2URIProp_default.put("Bundle-SymbolicName", "Client");
			bundleSN2URIProp_default.put("bsn2uri@Remote", REMOTE_ADDRESS);
			bundleSN2URIProp_default.put("bsn2uri@Client", LOCAL_ADDRESS);
			bundleSN2URIProp_default.put("Hook-ClassName", CommConstants.DEFAULT_HOOK_NAME);
			try
			{
				FileOutputStream fos = new FileOutputStream(
						file.getAbsolutePath());
				bundleSN2URIProp_default.store(fos, "DPartner Android Default");
				fos.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	public static AppEndpoint getEndpoint(boolean entryFromServer)
	{
		if (endpoint == null)
		{
			System.getProperties().put(CommConstants.Entry_FROM_SERVER,
					entryFromServer);

			NetworkChannelFacade factory = null;
			if (COMM_TYPE.equalsIgnoreCase("http"))
			{
				factory = new HTTPChannelFacade();
			}
			else if (COMM_TYPE.equalsIgnoreCase("tcp"))
			{
				factory = new TCPChannelFacade();
			}

			apiEndpoint = new APIEndpointImpl(factory);
			endpoint = new EndpointImpl(factory);
			apiEndpoint.bindAppEndpoint((EndpointImpl) endpoint);
		}
		return endpoint;
	}

	public static Properties readProperties()
	{
		File file = new File(dirPath, "CommConstants.CONFIG");
		try
		{
			FileInputStream fis = new FileInputStream(file);
			Properties bundleSN2URIProp = new Properties();
			bundleSN2URIProp.load(fis);
			fis.close();
			return bundleSN2URIProp;
		}
		catch (Exception e)
		{
			System.err.println("Config file not exist!");
			return null;
		}
	}
}
