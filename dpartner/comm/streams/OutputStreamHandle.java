package cn.edu.pku.dpartner.comm.streams;

import java.io.Serializable;

/**
 * Output stream handle.
 */
public final class OutputStreamHandle implements Serializable {

	/**
	 * the stream ID.
	 */
	private final short streamID;

	/**
	 * create a new output stream handle.
	 * 
	 * @param streamID
	 *            the stream ID.
	 */
	public OutputStreamHandle(final short streamID) {
		this.streamID = streamID;
	}

	/**
	 * get the stream ID.
	 * 
	 * @return the stream ID.
	 */
	public short getStreamID() {
		return streamID;
	}

}
