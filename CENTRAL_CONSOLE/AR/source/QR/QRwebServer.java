/**********************************************

 Author:  Mike Bass
 Year:    2012
 Purpose: Receives and handles request from
          QR clients.

 **********************************************/

 package QR;

 import java.net.Socket;
 import java.io.InputStream;
 import java.io.BufferedOutputStream;
 import java.io.BufferedReader;
 import java.io.InputStreamReader;


 import utility.web.server.webServer;
 import org.dcm4che2.net.log_writer;
 import common.manager.BasicManager;
 import common.job.JobExecutor;


 public class QRwebServer extends webServer implements Runnable {

 private String endValue_cMoveString = null;


 public QRwebServer(String serverIp,
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



 public void set_endValue_cMoveString(String endValue){

 this.endValue_cMoveString = endValue;
 }



 public void run(){

 super.startOp();
 }



 public void doExtraction_1(String dataRead){

 QRmanager qrmgr = (QRmanager) nHawkStarter;
 qrmgr.addNewJob(dataRead);

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
          log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<QRWebServer.getValue()> error: "+e);
 }
 return extractedVal;
 }




 public void doExtraction_2(String dataRead){
 try {
	 String[] jobValues = dataRead.split("&");

	 String job_id = null;
	 String opType = null;
	 String source = null;
	 String querytype = null;
	 String searchid = null;
	 String jobstatus = null;

	 for(int x = 0; x < jobValues.length; x++)
		{
		 if(jobValues[x] != null)
		   {
			if(jobValues[x].indexOf("jobid") >= 0)
			  {
			   job_id = this.getValue(jobValues[x]);
			  }
			else if(jobValues[x].indexOf("jobtype") >= 0)
			  {
			   opType = this.getValue(jobValues[x]);
			  }
			else if(jobValues[x].indexOf("source") >= 0)
			  {
			   source = this.getValue(jobValues[x]);
			  }
			else if(jobValues[x].indexOf("querytype") >= 0)
			  {
			   querytype = this.getValue(jobValues[x]);
			  }
			else if(jobValues[x].indexOf("searchid") >= 0)
			  {
			   searchid = this.getValue(jobValues[x]);
			  }
			else if(jobValues[x].indexOf("jobstatus") >= 0)
			  {
			   jobstatus = this.getValue(jobValues[x]);
			  }
		   }
		}

	  if((job_id != null)&&
	    ( querytype != null)&&
	    ( searchid != null))
		{
		 QRmanager qrmgr = (QRmanager) nHawkStarter;
		 qrmgr.start_newSearch(job_id ,
							   opType ,
							   source ,
							   querytype ,
							   searchid ,
							   jobstatus);
		}
       else
	    {
	  	 log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<QRWebServer.doExtraction_2()> msg: Null values passed in arg");
	    }

 } catch (Exception e){
	      e.printStackTrace();
          log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<QRWebServer.doExtraction_2()> error: "+e);
 }
 }



 public void doExtraction_3(String dataRead){

 super.doExtraction(dataRead);
 }



 public void doExtraction_4(String dataRead){

  //getstringurl = '/?ghostfield=nothing&jobid=1&jobtype=worklist&duration=7&source=CD&modalitytype=ALL&jobstatus=NEW_WORKLIST'

  try {
		 String[] jobValues = dataRead.split("&");

		 String job_id = null;
		 String opType = null;
		 String duration = null;
		 String source = null;
		 String modalitytype = null;
		 String jobstatus = null;

		 for(int x = 0; x < jobValues.length; x++)
			{
			 if(jobValues[x] != null)
			   {
				if(jobValues[x].indexOf("jobid") >= 0)
				  {
				   job_id = this.getValue(jobValues[x]);
				  }
				else if(jobValues[x].indexOf("jobtype") >= 0)
				  {
				   opType = this.getValue(jobValues[x]);
				  }
				else if(jobValues[x].indexOf("duration") >= 0)
				  {
				   duration = this.getValue(jobValues[x]);
				  }
				else if(jobValues[x].indexOf("source") >= 0)
				  {
				   source = this.getValue(jobValues[x]);
				  }
				else if(jobValues[x].indexOf("modalitytype") >= 0)
				  {
				   modalitytype = this.getValue(jobValues[x]);
				  }
				else if(jobValues[x].indexOf("jobstatus") >= 0)
				  {
				   jobstatus = this.getValue(jobValues[x]);
				  }
			   }
			}

		  if((job_id != null)&&
			( source != null)&&
			( modalitytype != null)&&
			( duration != null))
			{
			 QRmanager qrmgr = (QRmanager) nHawkStarter;
			 qrmgr.do_newSearch_2(job_id ,
								  source ,
								  modalitytype ,
								  duration );
			}
		  else
		    {
			 log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<QRWebServer.doExtraction_4()> msg: Null values passed in arg");
			}


 } catch (Exception e){
	      e.printStackTrace();
          log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<QRWebServer.doExtraction_4()> error: "+e);
 }
 }




 public void handle_clientRequest(Socket sock){

 InputStream inStream = null;
 BufferedOutputStream outStream = null;
 BufferedReader bReader = null;

 try {
     inStream = sock.getInputStream();
	 outStream = new BufferedOutputStream(sock.getOutputStream());
	 bReader = new BufferedReader(new InputStreamReader(sock.getInputStream()));

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
         if(dataRead.indexOf("type=WORKLIST") >= 0)
		   {
		 	doExtraction_4(dataRead);
		 	break;
		   }
         else if(dataRead.indexOf("type=cMove") >= 0)
           {
		    //doExtraction_1(dataRead);
		    new Thread(new assistInProcessing(dataRead)).start();
		    break;
		   }
         else if(dataRead.indexOf("type=SEARCH") >= 0)
           {
		    doExtraction_2(dataRead);
		    break;
		   }
         else if(dataRead.indexOf("type=jobanalysis") >= 0)
           {
		    super.doJobAnalysis(dataRead);
		    break;
		   }
         else
           {
		    doExtraction_3(dataRead);
		    break;
		   }
	    }
      }

 } catch (Exception e){
	      e.printStackTrace();
          log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<QRWebServer.handle_clientRequest()> error: "+e);
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
                   log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<QRWebServer.handle_clientRequest()> socket close error: "+e2);
		  }
 }
 }



 ////////////////////////////////////////////
 // Conduct lengthy processing in new thread
 // to avoid slowing down other processes.
 ////////////////////////////////////////////

 class assistInProcessing implements Runnable {

 private String msgToProcess = null;

 public assistInProcessing(String msgToProcess){

 this.msgToProcess = msgToProcess;
 }

 public void run(){
 try {
      doExtraction_1(this.msgToProcess);

 } catch (Exception e2){
		  e2.printStackTrace();
          log_writer.doLogging_QRmgr(logRoot, logName,
          "<QRWebServer.assistInProcessing()> socket close error: "+e2);
 }
 }

 }


 }