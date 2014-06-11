package cn.edu.pku.dpartner.comm.impl;

import javax.servlet.http.HttpServlet;

public interface ServletRegister
{
	public boolean registerServlet(HttpServlet servlet, String servletName);
	
	public boolean unregisterServlet(String servletName);
}
