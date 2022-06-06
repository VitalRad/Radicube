
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





public class readProcOut implements Runnable {

private String name = null;
private InputStream is = null;
private Thread thread = null;
private String logRoot = null;
private String logName = null;
private boolean processOutput = false;
private int processType = -1;
private boolean reProcess = false;
private boolean keepProcess = true;


public readProcOut(String name,
                   InputStream is,
                   String logRoot,
                   String logName){
this.name = name;
this.is = is;
this.logRoot = logRoot;
this.logName = logName;
}


public readProcOut(String name,
                   InputStream is,
                   String logRoot,
                   String logName,
                   boolean processOutput,
                   int processType){
this.name = name;
this.is = is;
this.logRoot = logRoot;
this.logName = logName;
this.processOutput = processOutput;
this.processType = processType;
}



public void start(){

thread = new Thread(this);
thread.start();
}


public void run(){

try {
     InputStreamReader isr = new InputStreamReader(this.is);
     BufferedReader br = new BufferedReader(isr);

while(this.keepProcess)
 {
  String s = br.readLine ();

  System.out.println("<readProcessOutput>: "+s);

  if(s == null)
  {
   this.keepProcess = false;
   break;
  }

  if(this.processOutput)
  {
   this.doProcessing_output(s);
  }

 }

is.close ();

} catch(Exception e) {
        e.printStackTrace();
        log_writer.doLogging_QRmgr(this.logRoot, this.logName,
       "<readProcessOutput> run()error: "+e);
}
}


public boolean reProcess(){

return this.reProcess;
}


private void doProcessing_output(String data)
{
 try
 {
  if(processType == 1)
  {
	if(data.indexOf("Add 0 directory records to existing directory") >= 0)
	{
	 String[] splitData = data.split(" ");
	 if(splitData != null)
	 {
	  if(splitData.length >= 2)
	  {
	   int n_recs_added = Integer.parseInt(splitData[1]);
	   System.out.println("num added: "+n_recs_added);
	   this.reProcess = true;
	   this.keepProcess = false;
	  }
	 }
	}
  }

 } catch(Exception e) {
        e.printStackTrace();
        log_writer.doLogging_QRmgr(this.logRoot, this.logName,
       "<readProcessOutput> doProcessing_output()error: "+e);
 }
}

}


