package cn.edu.pku.dpartner.comm.type;

import java.io.Serializable;

public final class PrimitiveBox implements Serializable
{
	/**
	 * the boxed value.
	 */
	private Object boxed;

	public PrimitiveBox(final Object o)
	{
		boxed = o;
	}

	public PrimitiveBox(final int i)
	{
		boxed = new Integer(i);
	}

	public PrimitiveBox(final boolean b)
	{
		boxed = new Boolean(b);
	}

	public PrimitiveBox(final long l)
	{
		boxed = new Long(l);
	}

	public PrimitiveBox(final char c)
	{
		boxed = new Character(c);
	}

	public PrimitiveBox(final double d)
	{
		boxed = new Double(d);
	}

	public PrimitiveBox(final float f)
	{
		boxed = new Float(f);
	}

	public PrimitiveBox(final short s)
	{
		boxed = new Short(s);
	}

	public PrimitiveBox(final byte b)
	{
		boxed = new Byte(b);
	}


	public Object getBoxedObject()
	{
		return boxed;
	}
	

	public boolean equals(final Object o)
	{
		return boxed.equals(o);
	}


	public int hashCode()
	{
		return boxed.hashCode();
	}

}
