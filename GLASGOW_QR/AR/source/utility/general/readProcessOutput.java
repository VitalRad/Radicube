
/**********************************************************

Author:  Mike Bassey
Date:    Aug, 2012
Purpose: reads output from desired started java process.

**********************************************************/

package utility.general;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;


import org.dcm4che2.net.log_writer;
import common.manager.BasicManager;


public class readProcessOutput implements Runnable {

private BasicManager bMgr = null;
private String[] msgToLookFor = null;
private String name = null;
private InputStream is = null;
private Thread thread = null;
private String logRoot = null;
private String logName = null;



public readProcessOutput(BasicManager bMgr,
                         String[] msgToLookFor,
	                     String name,
                         InputStream is,
                         String logRoot,
                         String logName){
this.bMgr = bMgr;
this.msgToLookFor = msgToLookFor;
this.name = name;
this.is = is;
this.logRoot = logRoot;
this.logName = logName;
}


public void start(){

thread = new Thread(this);
thread.start();
}


public void run(){

try {
     InputStreamReader isr = new InputStreamReader(this.is);
     BufferedReader br = new BufferedReader(isr);

boolean continueOp = true;
while(continueOp)
 {
  String s = br.readLine ();
  if(s == null)
    {
	 continueOp = false;
	 break;
	}
  else
    {
     for(int a = 0; a < this.msgToLookFor.length; a++)
        {
         if(s.indexOf(this.msgToLookFor[a]) >= 0)
           {
		    bMgr.diskMapped = true;
		    continueOp = false;
		    break;
		   }
	    }
	}
 }

is.close ();

} catch(Exception e) {
        e.printStackTrace();
        log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<readProcessOutput> run()error: "+e);
}
}

}


