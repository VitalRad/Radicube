/**********************************************

 Author:  Mike Bass
 Year:    2012
 Purpose: Receives and handles request from
          night hawk clients.

 **********************************************/

 package night_hawk;

 import java.net.Socket;
 import java.io.InputStream;
 import java.io.BufferedOutputStream;
 import java.io.BufferedReader;
 import java.io.InputStreamReader;


 import utility.web.server.webServer;
 import org.dcm4che2.net.log_writer;
 import common.manager.BasicManager;
 import common.job.JobExecutor;


 public class nightHawkWebServer extends webServer implements Runnable {


 public nightHawkWebServer(String serverIp,
						   int serverPort,
						   String logRoot,
						   String logName,
						   int BackLog,
						   JobExecutor jobController,
						   BasicManager nHawkStarter){
 super(serverIp,
       serverPort,
       logRoot,
       logName,
       BackLog,
       jobController,
       nHawkStarter);
 }



 public void run(){

 super.startOp();
 }


 public void doExtraction(String dataRead){

 super.doExtraction(dataRead);
 }



public void handle_clientRequest(Socket sock){

InputStream inStream = null;
BufferedOutputStream outStream = null;
BufferedReader bReader = null;

try {
     inStream = sock.getInputStream();
	 outStream = new BufferedOutputStream(sock.getOutputStream());
	 bReader = new BufferedReader(new InputStreamReader(sock.getInputStream()));

     String job_id = null;
	 String opType = null;

     while(true)
      {
	   String dataRead = bReader.readLine();

	   ///////////////////////////////////////////
	   System.out.println("Data read: "+dataRead);
	   ///////////////////////////////////////////

	   if(dataRead == null)
         {break;}
	   if((dataRead.equals(webServer.CRLF))||(dataRead.equals("")))
		 {break;}
	   else
	     {
		  if(dataRead.indexOf("type=jobanalysis") >= 0)
		    {
		  	 super.doJobAnalysis(dataRead);
		  	 break;
		    }
		  else
		    {
		     doExtraction(dataRead);
		     break;
	        }
		 }
	  }

} catch (Exception e){
	     e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<nightHawkWebServer.handle_clientRequest()> error: "+e);
} finally {
          try {
		       if(inStream != null)
		         {
				  inStream.close();
				  inStream = null;
				 }
			   if(outStream != null)
			   	 {
			   	  outStream.close();
			   	  outStream = null;
				 }
			   if(bReader != null)
			     {
				  bReader.close();
				  bReader = null;
				 }
			   if(sock != null)
			     {
				  sock.close();
				  sock = null;
				 }

		  } catch (Exception e2){
		           e2.printStackTrace();
                   log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<nightHawkWebServer.handle_clientRequest()> socket close error: "+e2);
		  }
}
}

}