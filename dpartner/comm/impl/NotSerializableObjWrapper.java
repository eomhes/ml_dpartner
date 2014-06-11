package cn.edu.pku.dpartner.comm.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;


public class NotSerializableObjWrapper implements Serializable
{
	private static final long serialVersionUID = 1L;

	transient private final ByteArrayOutputStream bos;

	transient private Object rootUnserializableObj;

	public NotSerializableObjWrapper(Object rootUnserializableObj)
			throws IOException
	{
		this.rootUnserializableObj = rootUnserializableObj;
		bos = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(bos);
		HashSet<Object> processedObj = new HashSet<Object>();
		processUnserializableObj(out, rootUnserializableObj, processedObj);
		out.flush();
		out.close();
		processedObj.clear();
	}

	private void processUnserializableObj(ObjectOutputStream out, Object obj, HashSet<Object> processedObj)
			throws IOException
	{
		if (obj == null) 
		{
			out.writeByte(0); 
			return;
		}
		
		if(processedObj.contains(obj))
			return;
		
		processedObj.add(obj);
		
		if (obj instanceof Serializable)
		{
			out.writeByte(1);
			out.writeObject(obj);
			return;
		}
		else
		{
			out.writeByte(2);

			Class<?> clazz = obj.getClass();
			out.writeUTF(clazz.getName());

			while (clazz != Object.class)
			{
				// check for native methods
				final Method[] methods = clazz.getDeclaredMethods();
				for (int j = 0; j < methods.length; j++)
				{
					final int mod = methods[j].getModifiers();
					if (Modifier.isNative(mod))
					{
						throw new NotSerializableException(
								"Class "
										+ clazz.getName()
										+ " not serializabl due to contains native methods");
					}
				}

				try
				{
					final Field[] fields = clazz.getDeclaredFields();
					final int fieldCount = fields.length;
					int realFieldCount = 0;
					for (int i = 0; i < fieldCount; i++)
					{
						final int mod = fields[i].getModifiers();
						if (!(Modifier.isStatic(mod) || Modifier
								.isTransient(mod)))
						{
							realFieldCount++;
						}
					}
					out.writeInt(realFieldCount);
					for (int i = 0; i < fieldCount; i++)
					{
						final int mod = fields[i].getModifiers();
						if (Modifier.isStatic(mod) || Modifier.isTransient(mod))
						{
							continue;
						}
						else if (!Modifier.isPublic(mod))
						{
							fields[i].setAccessible(true);
						}
						out.writeUTF(fields[i].getName());
						processUnserializableObj(out, fields[i].get(obj), processedObj);
					}
				}
				catch (final Exception e)
				{
					e.printStackTrace();
					throw new NotSerializableException(
							"Exception while serializing " + clazz.getName()
									+ ":\r\n" + e.getMessage());
				}
				clazz = clazz.getSuperclass();
			}
			out.writeInt(-1);
		}
	}

	
	private void writeObject(ObjectOutputStream out) throws IOException
	{
		out.writeInt(bos.size());
		out.write(bos.toByteArray());
	}

	private void readObject(ObjectInputStream in) throws IOException,
			ClassNotFoundException
	{
		int len = in.readInt();
		byte[] contents = new byte[len];
		in.read(contents);

		ObjectInputStream4NotSerializableObjWrapper ois = new ObjectInputStream4NotSerializableObjWrapper(
				new ByteArrayInputStream(contents));
		rootUnserializableObj = ois.readObject();
		ois.close();
	}

	public Object unWrap()
	{
		return rootUnserializableObj;
	}

	// ##################################################################################################
	// ##################################################################################################
	private static final class ObjectInputStream4NotSerializableObjWrapper
			extends ObjectInputStream
	{

		private final ObjectInputStream in;

		public ObjectInputStream4NotSerializableObjWrapper(final InputStream in)
				throws IOException
		{
			this.in = new ObjectInputStream(in); // enableOverride is set,
			// because we use implicit super() constructor
		}

		protected final Object readObjectOverride() throws IOException,
				ClassNotFoundException
		{

			final byte cat = in.readByte();
			switch (cat)
			{
			case 0:
				return null;
			case 1:
				return in.readObject();
			case 2:
				final String clazzName = in.readUTF();
				Class<?> clazz = Class.forName(clazzName);

				try
				{
					// must have an anonymous constructor
					final Constructor constr = clazz
							.getDeclaredConstructor(null);
					constr.setAccessible(true);
					final Object newInstance = constr.newInstance(null);

					int fieldCount = in.readInt();
					while (fieldCount > -1)
					{
						for (int i = 0; i < fieldCount; i++)
						{
							final String fieldName = in.readUTF();
							final Object value = readObjectOverride();
							final Field field = clazz
									.getDeclaredField(fieldName);

							final int mod = field.getModifiers();
							if (!Modifier.isPublic(mod))
							{
								field.setAccessible(true);
							}

							field.set(newInstance, value);
						}
						clazz = clazz.getSuperclass();
						fieldCount = in.readInt();
					}
					return newInstance;
				}
				catch (final Exception e)
				{
					e.printStackTrace();
					throw new IOException("Error while deserializing "
							+ clazzName + ": \r\n" + e.getMessage());
				}
			default:
				throw new IllegalStateException("Should not get here! " + cat);
			}
		}

		public final int read() throws IOException
		{
			return in.read();
		}

		public final int read(final byte[] buf, final int off, final int len)
				throws IOException
		{
			return in.read(buf, off, len);
		}

		public final int available() throws IOException
		{
			return in.available();
		}

		public final void close() throws IOException
		{
			in.close();
		}

		public final boolean readBoolean() throws IOException
		{
			return in.readBoolean();
		}

		public final byte readByte() throws IOException
		{
			return in.readByte();
		}

		public final int readUnsignedByte() throws IOException
		{
			return in.readUnsignedByte();
		}

		public final char readChar() throws IOException
		{
			return in.readChar();
		}

		public final short readShort() throws IOException
		{
			return in.readShort();
		}

		public final int readUnsignedShort() throws IOException
		{
			return in.readUnsignedShort();
		}

		public final int readInt() throws IOException
		{
			return in.readInt();
		}

		public final long readLong() throws IOException
		{
			return in.readLong();
		}

		public final float readFloat() throws IOException
		{
			return in.readFloat();
		}

		public final double readDouble() throws IOException
		{
			return in.readDouble();
		}

		public final void readFully(final byte[] buf) throws IOException
		{
			in.readFully(buf);
		}

		public final void readFully(final byte[] buf, final int off,
				final int len) throws IOException
		{
			in.readFully(buf, off, len);
		}

		public final int skipBytes(final int len) throws IOException
		{
			return in.skipBytes(len);
		}

		public final String readLine() throws IOException
		{
			return in.readLine();
		}

		public final String readUTF() throws IOException
		{
			return in.readUTF();
		}

	}
}
