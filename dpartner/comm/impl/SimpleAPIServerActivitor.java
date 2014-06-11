package cn.edu.pku.dpartner.comm.impl;

import cn.edu.pku.dpartner.comm.AppEndpoint;
import cn.edu.pku.dpartner.comm.Entry;

public class SimpleAPIServerActivitor
{
  private static AppEndpoint endpoint=null;

  public static void init()
  {
    if (endpoint == null)
    {
      Entry.init("GoogleTranslate", "192.168.1.101:1616", "192.168.1.100:1616");
      endpoint = Entry.getEndpoint(false);
    }
  }
}