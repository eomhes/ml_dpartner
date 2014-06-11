package cn.edu.pku.dpartner.comm.util;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

public class DumbOutputStream extends OutputStream {  
	public int count = 0;
	public void write(int b) throws IOException{
		count+=4;
	}
	public void write(byte b) throws IOException{
		count+=1;
	}
	public void write(byte[] b) throws IOException{
		count+= b.length;
	}
}  