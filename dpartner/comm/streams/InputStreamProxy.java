package cn.edu.pku.dpartner.comm.streams;

import java.io.IOException;
import java.io.InputStream;

import cn.edu.pku.dpartner.comm.NetworkChannel;
import cn.edu.pku.dpartner.comm.impl.EndpointImpl;


public class InputStreamProxy extends InputStream {

	/**
	 * the stream id.
	 */
	private final short streamID;

	/**
	 * the channel endpoint.
	 */
	private final EndpointImpl endpoint;
	
	private NetworkChannel networkChannel;

	/**
	 * Create a new input stream proxy.
	 * @param networkChannel 
	 * 
	 * @param streamID
	 *            the stream id.
	 * @param endpoint
	 *            the channel endpoint.
	 */
	public InputStreamProxy(NetworkChannel networkChannel, final short streamID,
			final EndpointImpl endpoint) {
		this.streamID = streamID;
		this.endpoint = endpoint;
		this.networkChannel = networkChannel;
	}

	/**
	 * Read from the stream.
	 * 
	 * @see java.io.InputStream#read()
	 */
	public int read() throws IOException {
		return endpoint.readStream(networkChannel, streamID);
	}

	/**
	 * Read from the stream.
	 * 
	 * @see java.io.InputStream#read(byte[], int, int)
	 */
	public int read(final byte[] b, final int off, final int len)
			throws IOException {
		return endpoint.readStream(networkChannel, streamID, b, off, len);
	}

}
