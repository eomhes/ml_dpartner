
package cn.edu.pku.dpartner.comm.streams;

import java.io.IOException;
import java.io.OutputStream;

import cn.edu.pku.dpartner.comm.NetworkChannel;
import cn.edu.pku.dpartner.comm.impl.EndpointImpl;

/**
 * Output stream proxy.
 */
public class OutputStreamProxy extends OutputStream {

	/**
	 * the stream ID.
	 */
	private final short streamID;

	/**
	 * the channel endpoint.
	 */
	private final EndpointImpl endpoint;
	
	private NetworkChannel networkChannel;

	/**
	 * create a new output stream proxy.
	 * @param networkChannel 
	 * 
	 * @param streamID
	 *            the stream ID.
	 * @param endpoint
	 *            the endpoint.
	 */
	public OutputStreamProxy(NetworkChannel networkChannel, final short streamID,
			final EndpointImpl endpoint) {
		this.streamID = streamID;
		this.endpoint = endpoint;
		this.networkChannel = networkChannel;
	}

	/**
	 * write to the stream.
	 * 
	 * @param b
	 *            the value.
	 * @throws IOException
	 *             in case of IO failures.
	 */
	public void write(final int b) throws IOException {
		endpoint.writeStream(networkChannel, streamID, b);
	}

	/**
	 * write to the stream.
	 * 
	 * @param the
	 *            bytes.
	 * @param off
	 *            the offset.
	 * @param len
	 *            the length.
	 */
	public void write(final byte[] b, final int off, final int len)
			throws IOException {
		endpoint.writeStream(networkChannel, streamID, b, off, len);
	}

}
