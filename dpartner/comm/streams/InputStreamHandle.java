package cn.edu.pku.dpartner.comm.streams;

import java.io.Serializable;

/**
 * Stream handle for InputStreams passed as arguments or return values of remote
 * service invocations.
 */
public class InputStreamHandle implements Serializable {

	/**
	 * the stream id.
	 */
	private final short streamID;

	/**
	 * Create a new stream handle.
	 * 
	 * @param streamID
	 *            the stream id.
	 */
	public InputStreamHandle(final short streamID) {
		this.streamID = streamID;
	}

	/**
	 * Get the stream id.
	 * 
	 * @return the stream id.
	 */
	public short getStreamID() {
		return streamID;
	}

}
