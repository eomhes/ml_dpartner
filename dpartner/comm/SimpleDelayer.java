package cn.edu.pku.dpartner.comm;

public class SimpleDelayer {
	public static long sleepTime=1000L;
	public static void sleep(){
		System.out.println("[SimpleDelayer] sleep "+sleepTime);
		try {
			printTrace(Thread.currentThread().getStackTrace());
			Thread.currentThread().sleep(sleepTime);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return;
	}
	private static void printTrace(StackTraceElement arrT[]) {
		int i;
		for (i=0;i<arrT.length;i++){
			StackTraceElement t = arrT[i];
		
		System.out.println(t.getClassName() + ":" 
							+ t.getMethodName() + "("
							+ t.getLineNumber() + ")");
		}
	}
}
