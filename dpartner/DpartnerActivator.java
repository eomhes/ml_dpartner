package cn.edu.pku.dpartner;

import cn.edu.pku.dpartner.comm.AppEndpoint;
import cn.edu.pku.dpartner.comm.Entry;

public class DpartnerActivator
{
  private static AppEndpoint endpoint;

  public static AppEndpoint getEndpoint()
  {
    if (endpoint == null)
    {
      Entry.init("GoGame", "192.168.1.101:1616", "192.168.1.100:1616");
      endpoint = Entry.getEndpoint(false);
    }
    return endpoint;
  }

  public static void main(String[] args)
  {
    if (endpoint == null)
      endpoint = Entry.getEndpoint(true);
  }
}

/* Location:           /Users/mac/Documents/Dpartner/ForUFL/GoGame_Main.jar
 * Qualified Name:     cn.edu.pku.dpartner.DpartnerActivator
 * JD-Core Version:    0.6.2
 */
