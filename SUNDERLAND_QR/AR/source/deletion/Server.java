/**********************************************************
 *
 *   Author:  Mike Bassey
 *   Year:    2012
 *   Purpose: Web server to receive new deletion settings
 *            and orders.
 *
 **********************************************************/

package deletion;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.io.InputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.dcm4che2.net.log_writer;




public class Server implements Runnable {

private String serverIp = null;
private int serverPort = -1;
private String logRoot = null;
private String logName = null;
private int BackLog = -1;
public final static String CRLF = "\r\n";
private manager mgr = null;



public Server(String serverIp,
              int serverPort,
              String logRoot,
              String logName,
              int BackLog,
              manager mgr){

this.serverIp = serverIp;
this.serverPort = serverPort;
this.logRoot = logRoot;
this.logName = logName;
this.BackLog = BackLog;
this.mgr = mgr;
}



public String get_logPath(){

return this.logRoot;
}


public String get_logName(){

return this.logName;
}


public void run(){

this.startOp();
}


private void startOp(){

try {
     ServerSocket serverSock = new ServerSocket(this.serverPort,
                                                this.BackLog,
                                                InetAddress.getByName(this.serverIp));

     System.out.println("web server bound on "+this.serverIp+":"+this.serverPort);

     while(true)
      {
	   Socket sock = serverSock.accept();

	   System.out.println("<Server> msg: client socket accepted.");

	   new Thread(new requestHandler(sock, this)).start();
	  }

} catch (Exception e){
	     e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<Server.startOp()> error: "+e);
         System.exit(0);
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
          log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<Server.getValue()> error: "+e);
 }
 return extractedVal;
 }



//to_server.println("GET /?jobstuff=ab&jobtype=timeoutDelete&deletionType=days&value=3&settings=1");
//to_server.println("GET /?jobstuff=ab&jobtype=logDelete&deletionType=days&value=3&settings=1");
//to_server.println("GET /?jobstuff=ab&jobtype=diskSpaceDelete&deletionType=MB&value=10&settings=4");
protected void doExtraction_1(String dataRead, int type){

try {
     String deletionType = null;
     String value = null;
     String settings = null;

     String[] jobValues = dataRead.split("&");

	 for(int x = 0; x < jobValues.length; x++)
		{
		 if(jobValues[x] != null)
		   {
			if(jobValues[x].indexOf("deletionType") >= 0)
			  {
			   deletionType = this.getValue(jobValues[x]);
			  }
			else if(jobValues[x].indexOf("value") >= 0)
			  {
			   value = this.getValue(jobValues[x]);
			  }
            else if(jobValues[x].indexOf("settings") >= 0)
			  {
			   settings = this.getValue(jobValues[x]);
			  }
		   }
		}

     if((deletionType != null)&&
       ( value != null)&&
       ( settings != null))
       {
        this.mgr.set_stats(deletionType,value,settings,type);
	   }

} catch (Exception e){
	     e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<Server.doExtraction_timeout()> error: "+e);
}
}




//to_server.println("GET /?jobstuff=ab&jobtype=rangeDelete&startDate=2012-05-10&endDate=2012-07-05&settings=4");
protected void doExtraction_range(String dataRead){

try {
     String startDate = null;
     String endDate = null;
     String settings = null;

     String[] jobValues = dataRead.split("&");

	 for(int x = 0; x < jobValues.length; x++)
		{
		 if(jobValues[x] != null)
		   {
			if(jobValues[x].indexOf("startDate") >= 0)
			  {
			   startDate = this.getValue(jobValues[x]);
			  }
			else if(jobValues[x].indexOf("endDate") >= 0)
			  {
			   endDate = this.getValue(jobValues[x]);
			  }
            else if(jobValues[x].indexOf("settings") >= 0)
			  {
			   settings = this.getValue(jobValues[x]);
			  }
		   }
		}

     if((startDate != null)&&
       ( endDate != null)&&
       ( settings != null))
       {
        this.mgr.set_rangeStats(startDate,endDate,settings);
	   }

} catch (Exception e){
	     e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<Server.doExtraction_range()> error: "+e);
}
}



//to_server.println("GET /?jobstuff=ab&jobtype=manualDelete&path=C:/mike_work/logs.txt");
protected void doExtraction_manual(String dataRead, boolean filesOnly){

try {
     String path = null;
     String[] jobValues = dataRead.split("&");

	 for(int x = 0; x < jobValues.length; x++)
		{
		 if(jobValues[x] != null)
		   {
			if(jobValues[x].indexOf("path") >= 0)
			  {
			   path = this.getValue(jobValues[x]);
			  }
		   }
		}

     if(path != null)
       {
        this.mgr.doManualDelete(path, filesOnly);
	   }

} catch (Exception e){
	     e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<Server.doExtraction_manual()> error: "+e);
}
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
private Server wServer = null;



public requestHandler(Socket sock,
                      Server wServer){

this.sock = sock;
this.wServer = wServer;
}

public void run(){ this.processRequest();}


private void processRequest(){

try {
     this.inStream = this.sock.getInputStream();
     this.outStream = new BufferedOutputStream(this.sock.getOutputStream());
     this.bReader = new BufferedReader(new InputStreamReader(this.sock.getInputStream()));

     String job_id = null;
	 String opType = null;

     while(true)
      {

	  String dataRead = this.bReader.readLine();

	  ///////////////////////////////////////////
	  System.out.println("Data read: "+dataRead);
	  ///////////////////////////////////////////


      /*

      //to_server.println("GET /?jobstuff=ab&jobtype=timeoutDelete&deletionType=days&value=3&settings=1");
      //to_server.println("GET /?jobstuff=ab&jobtype=logDelete&deletionType=days&value=3&settings=1");
      //to_server.println("GET /?jobstuff=ab&jobtype=diskSpaceDelete&deletionType=MB&value=10&settings=4");
      //to_server.println("GET /?jobstuff=ab&jobtype=rangeDelete&startDate=2012-05-10&endDate=2012-07-05&settings=4");
      //to_server.println("GET /?jobstuff=ab&jobtype=manualDelete&path=C:/mike_work/logs.txt");

      */


      if(dataRead == null)
	    {break;}
	  if((dataRead.equals(Server.CRLF))||(dataRead.equals("")))
	    {break;}
	  else
	    {
         if(dataRead.indexOf("jobtype=timeoutDelete") >= 0)
           {
		    doExtraction_1(dataRead, 1);
		    break;
		   }
         else if(dataRead.indexOf("jobtype=logDelete") >= 0)
           {
		    doExtraction_1(dataRead, 2);
		    break;
		   }
         else if(dataRead.indexOf("jobtype=diskSpaceDelete") >= 0)
           {
		    doExtraction_1(dataRead, 3);
		    break;
		   }
         else if(dataRead.indexOf("jobtype=rangeDelete") >= 0)
           {
		    doExtraction_range(dataRead);
		    break;
		   }
         else if(dataRead.indexOf("jobtype=manualDelete") >= 0)
           {
		    doExtraction_manual(dataRead, false);
		    break;
		   }
         else if(dataRead.indexOf("jobtype=filesOnly") >= 0)
           {
		    doExtraction_manual(dataRead, true);
		    break;
		   }
         else
           {
		    log_writer.doLogging_QRmgr(logRoot, logName, "<Server.requestHandler.processRequest()> msg: No job type found for GET message "+dataRead);
		    break;
		   }
	    }

	 }

} catch (Exception e){
	     e.printStackTrace();
         log_writer.doLogging_QRmgr(logRoot, logName, "<Server.requestHandler.processRequest()> error: "+e);
} finally {
          try {
		       if(this.inStream != null)
		         {
				  this.inStream.close();
				  this.inStream = null;
				 }
			   if(this.outStream != null)
			   	 {
			   	  this.outStream.close();
			   	  this.outStream = null;
				 }
			   if(this.bReader != null)
			     {
				  this.bReader.close();
				  this.bReader = null;
				 }
			   if(this.sock != null)
			     {
				  this.sock.close();
				  this.sock = null;
				 }

		  } catch (Exception e2){
		           e2.printStackTrace();
                   log_writer.doLogging_QRmgr(logRoot, logName, "<Server.requestHandler.processRequest()> socket close error: "+e2);
		  }
}
}

}

}