/****************************************************************
 *
 *   Author: Mike Bassey
 *   Year: 2012
 *   Purpose: Web server to receive and handle client's request.
 *
 ****************************************************************/

package utility.web.server;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.io.InputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.dcm4che2.net.log_writer;
import common.manager.BasicManager;
import common.job.JobExecutor;



public abstract class webServer {

protected String serverIp = null;
protected int serverPort = -1;
protected String logRoot = null;
protected String logName = null;
protected int BackLog = -1;
public final static String CRLF = "\r\n";
protected JobExecutor jobController = null;
protected BasicManager nHawkStarter = null;


public webServer(String serverIp,
                 int serverPort,
                 String logRoot,
                 String logName,
                 int BackLog,
                 JobExecutor jobController,
                 BasicManager nHawkStarter){

this.serverIp = serverIp;
this.serverPort = serverPort;
this.logRoot = logRoot;
this.logName = logName;
this.BackLog = BackLog;
this.jobController = jobController;
this.nHawkStarter = nHawkStarter;
}




protected void startOp(){

try {
     ServerSocket serverSock = new ServerSocket(this.serverPort,
                                                this.BackLog,
                                                InetAddress.getByName(this.serverIp));

     System.out.println("web server bound on "+this.serverIp+":"+this.serverPort);

     while(true)
      {
	   Socket sock = serverSock.accept();

	   System.out.println("<webServer> msg: client socket accepted.");

	   new Thread(new requestHandler(sock)).start();
	  }

} catch (Exception e){
	     e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<webServer.startOp()> error: "+e);
         System.exit(0);
}
}



public void doExtraction(String dataRead){

 try {
	  String job_id = null;
	  String opType = null;
	  String other = null;
      boolean notAssigned = true;

	  String[]info = dataRead.split("&");

	  ////////////////////////////////////////////////////
	  for(int k = 0; k < info.length; k++)
		 {
		  System.out.println("=== info["+k+"]: "+info[k]);
		 }
      ////////////////////////////////////////////////////

	  String[] value = info[1].split("=");
	  if(value.length >= 2)
	    {
		 job_id = value[1];
	    }

	  value = null;
	  value = info[2].split("=");
	  if(value.length >= 2)
	    {
		 opType = value[1];
	    }

	  if(opType.equals("changeDestination"))
	    {
	     value = null;
		 value = info[3].split("=");
		 if(value.length >= 2)
		   {
		 	other = value[1];
	       }
	    }
	  else if(opType.equals("changeAllDestination"))
	    {
	     value = null;
		 value = info[3].split("=");
		 if(value.length >= 2)
		   {
		 	other = value[1];
	       }

	    notAssigned = false;
	    nHawkStarter.changeAllDest(other);
	    }
	  else if((opType.equals("START_ALL"))||
	         ( opType.equals("STOP_ALL"))||
	         ( opType.equals("PAUSE_ALL"))||
	         ( opType.equals("CANCEL_ALL")))
	         {
		      notAssigned = false;
		      nHawkStarter.doALL(opType);
		     }


      if((job_id != null)&&(notAssigned))
        {
         nHawkStarter.findAndDoOp_nightHawk(job_id, opType, other);
	    }

 } catch (Exception e){
	      e.printStackTrace();
          log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<WebServer.doExtraction_3()> error: "+e);
 }
 }


//Data read: GET /?ghostfield=nothing&jobtype=jobanalysis&jobid=15620& HTTP/1.1


 public void doJobAnalysis(String dataRead){

 try {
	 String[] jobValues = dataRead.split("&");
	 String job_id = null;

	 for(int x = 0; x < jobValues.length; x++)
		{
		 if(jobValues[x] != null)
		   {
			if(jobValues[x].indexOf("jobid") >= 0)
			  {
			   job_id = this.getValue(jobValues[x]);
			   break;
			  }
		   }
		}

      if(job_id != null)
        {
	     this.nHawkStarter.set_focusID(job_id);
	    }
      else
	  	{
	  	 log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<webServer.doJobAnalysis()> msg: Null values passed in arg");
	    }

 } catch (Exception e){
	      e.printStackTrace();
          log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<webServer.doJobAnalysis()> error: "+e);
 }
 }



 private String getValue(String data){

 String extractedVal = null;
 try {
      String[] value = data.split("=");
 	  if(value.length >= 2)
 		{
 		 extractedVal = value[1];
        }
 } catch (Exception e){
	      extractedVal = null;
	      e.printStackTrace();
          log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<webServer.getValue()> error: "+e);
 }
 return extractedVal;
 }




//-----------------------------------------
///////////////////////////////////////////
//
//   handles request received from socket.
//
//////////////////////////////////////////
//-----------------------------------------

class requestHandler implements Runnable {

private Socket sock = null;
private InputStream inStream = null;
private BufferedOutputStream outStream = null;
private BufferedReader bReader = null;



public requestHandler(Socket sock){

this.sock = sock;
}


public void run(){

handle_clientRequest(this.sock);
}
}


public abstract void handle_clientRequest(Socket sock);


}