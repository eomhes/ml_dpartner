package cn.edu.pku.dpartner.comm.messages;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.SocketException;

import cn.edu.pku.dpartner.comm.impl.RemoteCommException;
import cn.edu.pku.dpartner.comm.util.DumbOutputStream;
public abstract class CommMessage
{

	/**
	 * type code for garbage collection
	 */
	public static final short GC = 1;
	
	/**
	 * type code for invoke method messages.
	 */
	public static final short REMOTE_CALL = 2;

	/**
	 * type code for method result messages.
	 */
	public static final short REMOTE_CALL_RESULT = 3;

	/**
	 * type code for stream request messages.
	 */
	public static final short STREAM_REQUEST = 4;

	/**
	 * type code for stream result messages.
	 */
	public static final short STREAM_RESULT = 5;
	
	/**
	 * type code for stream synchronize request messages.
	 */
	public static final short SYNCHRONIZE_REQUEST = 6;
	
	/**
	 * type code for stream synchronize result messages.
	 */
	public static final short SYNCHRONIZE_RESULT = 7;
	
	/**
	 * type code for API request message 
	 */
	public static final short API_REQUEST = 8;
	
	/**
	 * type code for API response message
	 */
	public static final short API_RESULT = 9;

	private short typeID;

	/**
	 * the transaction id.
	 */
	protected long xid;

	/**
	 * hides the default constructor.
	 */
	CommMessage(final short typID)
	{
		this.typeID = typID;
	}

	/**
	 * get the transaction ID.
	 * 
	 * @return the xid.
	 */
	public final long getXID()
	{
		return xid;
	}

	/**
	 * set the xid.
	 * 
	 * @param xid
	 *            set the xid.
	 */
	public void setXID(final long xid)
	{
		this.xid = xid;
	}

	/**
	 * Get the type code of the message.
	 */
	public final short getTypeID()
	{
		return typeID;
	}

	/**
	 * reads in a network packet and constructs the corresponding subtype of
	 * RemoteMessage from it. The header is:
	 * 
	 * <pre>
	 *   0                   1                   2                   3
	 *   0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
	 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *  |    Version    |         Type-ID           |     XID       |
	 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *  |    XID cntd.  | 
	 *  +-+-+-+-+-+-+-+-+
	 * </pre>
	 * 
	 * the body is processed by the subtype class.
	 * 
	 * @param input
	 *            the DataInput providing the network packet.
	 * @return the RemoteMessage.
	 * @throws ClassNotFoundException
	 * @throws SocketException
	 *             if something goes wrong.
	 */
	public static CommMessage parse(final ObjectInputStream input)
			throws IOException, ClassNotFoundException
	{
		input.readByte(); // version, currently unused
		final short funcID = input.readByte();
		final long xid = input.readLong();
		CommMessage msg;
		switch (funcID)
		{
		case GC:
			msg = new GCMessage(input);
			break;
		case REMOTE_CALL:
			msg = new RemoteCallMessage(input);
			break;
		case REMOTE_CALL_RESULT:
			msg = new RemoteCallResultMessage(input);
			break;
		case STREAM_REQUEST:
			msg = new StreamRequestMessage(input);
			break;
		case STREAM_RESULT:
			msg = new StreamResultMessage(input);
			break;
		case SYNCHRONIZE_REQUEST:
			msg = new SynchronizeRequestMessage(input);
			break;
		case SYNCHRONIZE_RESULT:
			msg = new SynchronizeResultMessage(input);
			break;
		case API_REQUEST:
			msg = new APIRequestMessage(input);
			break;
		case API_RESULT:
			msg = new APIResultMessage(input);
			break;
		
		default:
			throw new RemoteCommException("typeID " + funcID
					+ " not supported.");
		}
		msg.typeID = funcID;
		msg.xid = xid;
		return msg;
	}

	/**
	 * write the RemoteMessage to an output stream.
	 * 
	 * @param out
	 *            the ObjectOutputStream.
	 * @throws IOException
	 *             in case of IO failures.
	 */
	public final void send(final ObjectOutputStream out) throws IOException
	{
		synchronized (out)
		{
			out.write(1);
			out.write(typeID);
			out.writeLong(xid);
			writeBody(out);
			out.reset();
			out.flush();
		}
	}

	protected abstract void writeBody(final ObjectOutputStream output)
			throws IOException;


	protected static byte[] readBytes(final ObjectInputStream input)
			throws IOException
	{
		final int length = input.readInt();
		final byte[] buffer = new byte[length];
		input.readFully(buffer);
		return buffer;
	}

	protected static void writeBytes(final ObjectOutputStream out,
			final byte[] bytes) throws IOException
	{
		out.writeInt(bytes.length);
		if (bytes.length > 0)
		{
			out.write(bytes);
		}
	}

	protected static void writeStringArray(final ObjectOutputStream out,
			final String[] strings) throws IOException
	{
		final short length = (short) strings.length;
		out.writeShort(length);
		for (short i = 0; i < length; i++)
		{
			out.writeUTF(strings[i]);
		}
	}

	protected static String[] readStringArray(final ObjectInputStream in)
			throws IOException
	{
		final short length = in.readShort();
		final String[] result = new String[length];
		for (short i = 0; i < length; i++)
		{
			result[i] = in.readUTF();
		}
		return result;
	}
	public boolean isRequest(){
		return typeID!=GC && typeID%2==0;
	}
	public boolean isResult(){
		return typeID!=GC && typeID%2==1;
	}
	public boolean isGC(){
		return typeID==GC;
	}
	public Short getCategory(){
		return (short) (typeID/2);
	}

	public static short getTotalCategoryNum()
	{
		return 5;
	}

	public final Long getDataSize()
	{
		DumbOutputStream buf = new DumbOutputStream();  
		ObjectOutputStream os = null;  
		int ret;
		try {  
			os = new ObjectOutputStream(buf);  
			this.send(os); 
			ret = buf.count;
			os.close();
		}catch(IOException e){
			ret = -1;
		}
		return new Long(ret);
		
	}
}
