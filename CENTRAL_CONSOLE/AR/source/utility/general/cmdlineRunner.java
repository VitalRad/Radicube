/**********************************************************

Author:  Mike Bassey
Date:    Aug, 2012
Purpose: Used to run commandline instructions.

**********************************************************/
package utility.general;

import org.dcm4che2.net.log_writer;

public class cmdlineRunner {

public static void runCmd(String lRoot, String lName, String cmd, boolean waitfor)
{
	 try
	 {
		  Runtime rt = Runtime.getRuntime();
		  Process p = rt.exec(cmd);
		  readProcOut s1 = new readProcOut("input",p.getInputStream(),lRoot, lName);
		  readProcOut s2 = new readProcOut("error",p.getErrorStream(),lRoot, lName);
		  s1.start();
		  s2.start();

		  if(waitfor)
		  {
		   p.waitFor();
		  }

	 }  catch (Exception e){
			   e.printStackTrace();
			   log_writer.doLogging_QRmgr(lRoot, lName,
			   "<cmdlineRunner> runCmd() exception: "+e);
	 }
}

}



