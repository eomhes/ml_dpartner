package cn.edu.pku.dpartner.comm.impl;


public final class RemoteCommException extends RuntimeException {

	/**
	 * the nested throwable.
	 */
	private transient Throwable nested;

	/**
	 * creates a new RemoteOSGiException from error message.
	 * 
	 * @param message
	 *            the error message.
	 */
	public RemoteCommException(final String message) {
		super(message);
	}

	/**
	 * creates a new RemoteOSGiException that nests a <code>Throwable</code>.
	 * 
	 * @param nested
	 *            the nested <code>Exception</code>.
	 */
	public RemoteCommException(final Throwable nested) {
		super(nested.getMessage());
		this.nested = nested;
	}

	/**
	 * creates a new RemoteOSGiException with error message and nested
	 * <code>Throwable</code>.
	 * 
	 * @param message
	 *            the error message.
	 * @param nested
	 *            the nested <code>Exception</code>.
	 */
	public RemoteCommException(final String message, final Throwable nested) {
		super(message);
		this.nested = nested;
	}

	/**
	 * get the nested exception.
	 * 
	 * @return the nested exception.
	 */
	public Throwable getCause() {
		return nested;
	}
}
