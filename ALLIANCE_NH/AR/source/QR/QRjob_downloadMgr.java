/************************************************
 *
 *   Author:  Mike Bassey
 *   Year:    2012
 *   Purpose: Manages the download of requested
 *            scans from PACS
 *
 ************************************************/

 package QR;


 import org.dcm4che2.net.log_writer;
 import common.job.BasicJob;


 public class QRjob_downloadMgr implements Runnable {

 private String logRoot = null;
 private String logName = null;
 private QRjob_downloader[] downloaders = null;
 private QRmanager qrMgr = null;



 public QRjob_downloadMgr(String logRoot,
                          String logName,
                          QRjob_downloader[] downloaders,
                          QRmanager qrMgr){

 this.logRoot = logRoot;
 this.logName = logName;
 this.downloaders = downloaders;
 this.qrMgr = qrMgr;
 }


 public void run(){

 while(true){

 try {
       while(true)
         {
		  this.qrMgr.sleepForDesiredTime(2000); //TODO: take value in from properties file.
		  for(int a = 0; a < this.downloaders.length; a++)
		     {
			  if(this.downloaders[a] != null)
			    {
			     synchronized(this.downloaders[a].get_thisDownloader())
			      {
			       if(this.downloaders[a].getStatus().equals("free"))
			         {
					  BasicJob nJob = this.checkForPendingJob();
					  if(nJob != null)
					    {
					     this.startJob(nJob, a);
					    }
					  else
					    {
					     nJob = this.get_newJob();
					     if(nJob != null)
						   {
						    this.startJob(nJob, a);
						   }
						 else
						   {
                            nJob = this.get_relegatedJob();
							if(nJob != null)
							  {
							   this.startJob(nJob, a);
						      }
						   }
					    }
				     }
			      }
			    }
			 }
		 }

 } catch (Exception e){
		  e.printStackTrace();
		  log_writer.doLogging_QRmgr(logRoot, logName, "<QRjob_downloadMgr.run()> exception: "+e);
 }
 }
 }



 private void startJob(BasicJob nJob, int pos){

 try {
 	  this.downloaders[pos].setStatus("busy"); //just in case..
 	  if(this.downloaders.length <= 1)
 	    {
	     this.downloaders[pos].startOp((QRjob)nJob, true);
	    }
	  else
	    {
         new Thread(new start_cMover(this.downloaders[pos], nJob)).start();
	    }

 } catch (Exception e){
		  e.printStackTrace();
		  log_writer.doLogging_QRmgr(logRoot, logName, "<QRjob_downloadMgr.startJob()> exception: "+e);
 }
 }






 public BasicJob checkForPendingJob(){

 BasicJob nJob = null;

 synchronized(this.qrMgr.get_queueLock()){
 try {
      int size = this.qrMgr.getPendingJobQueue().size();
      try {
	       for(int a = 0; a < size; a++)
	          {
			   BasicJob Job = (BasicJob) this.qrMgr.getPendingJobQueue().poll();
			   if(Job != null)
			     {
                  if(this.qrMgr.check_1(Job, this.logRoot, this.logName))
				  	{
				  	 log_writer.doLogging_QRmgr(this.logRoot, this.logName,
				  	 "QRjob_downloadMgr.checkForPendingJob()msg: Cancelled or failed job found here!");
				  	 Job = null;
				    }
                  else if(this.qrMgr.check_2(Job, this.logRoot, this.logName))
				  	{
				  	 this.qrMgr.storeInDownloaderQueue_pendingJob(Job);
				    }
				  else if(this.qrMgr.check_3(Job, this.logRoot, this.logName))
				    {
                     this.qrMgr.storeInQueue_arrivingJob(Job);
                     Job = null;
					}
				  else
				    {
				     nJob = Job;
				     break;
				    }
				 }
			  }
	  } catch (Exception e1){
	           nJob = null;
	           e1.printStackTrace();
	           log_writer.doLogging_QRmgr(this.logRoot, this.logName, "QRjob_downloadMgr.checkForPendingJob()<internal> error: "+e1);
	  }
 } catch (Exception e){
          nJob = null;
          e.printStackTrace();
          log_writer.doLogging_QRmgr(this.logRoot, this.logName, "QRjob_downloadMgr.checkForPendingJob() error: "+e);
 }
 }
 return nJob;
 }




 public BasicJob get_newJob(){

 BasicJob nJob = null;

 synchronized(this.qrMgr.get_queueLock()){
 try {
      int size = this.qrMgr.getArrivedJobQueue().size();
      try {
	       for(int a = 0; a < size; a++)
	          {
			   BasicJob Job = (BasicJob) this.qrMgr.getArrivedJobQueue().poll();
			   if(Job != null)
			     {
                  if(this.qrMgr.check_1(Job, this.logRoot, this.logName))
				  	{
				  	 log_writer.doLogging_QRmgr(this.logRoot, this.logName,
				  	 "QRjob_downloadMgr.get_newJob()msg: Cancelled or failed job found here!");
				  	 Job = null;
				    }
                  else if(this.qrMgr.check_2(Job, this.logRoot, this.logName))
				  	{
				  	 this.qrMgr.storeInDownloaderQueue_newJob(Job);
				    }
				  else if(this.qrMgr.check_3(Job, this.logRoot, this.logName))
				    {
                     this.qrMgr.storeInQueue_arrivingJob(Job);
                     Job = null;
					}
				  else
				    {
				     nJob = Job;
				     break;
				    }
				 }
			  }
	  } catch (Exception e1){
	           nJob = null;
	           e1.printStackTrace();
	           log_writer.doLogging_QRmgr(this.logRoot, this.logName, "QRjob_downloadMgr.get_newJob()<internal> error: "+e1);
	  }
 } catch (Exception e){
          nJob = null;
          e.printStackTrace();
          log_writer.doLogging_QRmgr(this.logRoot, this.logName, "QRjob_downloadMgr.get_newJob() error: "+e);
 }
 }
 return nJob;
 }




 public BasicJob get_relegatedJob(){

 BasicJob nJob = null;

 synchronized(this.qrMgr.get_queueLock()){
 try {
      int size = this.qrMgr.relegatedJobs.size();
      try {
	       for(int a = 0; a < size; a++)
	          {
			   BasicJob Job = (BasicJob) this.qrMgr.relegatedJobs.poll();
			   if(Job != null)
			     {
                  if(this.qrMgr.check_1(Job, this.logRoot, this.logName))
				  	{
				  	 log_writer.doLogging_QRmgr(this.logRoot, this.logName,
				  	 "QRjob_downloadMgr.get_relegatedJob()msg: Cancelled or failed job found here!");
				  	 Job = null;
				    }
                  else if(this.qrMgr.check_2(Job, this.logRoot, this.logName))
				  	{
				  	 this.qrMgr.storeInQueue_relegatedJob(Job);
				    }
				  else if(this.qrMgr.check_3(Job, this.logRoot, this.logName))
				    {
                     this.qrMgr.storeInQueue_arrivingJob(Job);
                     Job = null;
					}
				  else
				    {
				     nJob = Job;
				     break;
				    }
				 }
			  }
	  } catch (Exception e1){
	           nJob = null;
	           e1.printStackTrace();
	           log_writer.doLogging_QRmgr(this.logRoot, this.logName, "QRjob_downloadMgr.get_relegatedJob()<internal> error: "+e1);
	  }
 } catch (Exception e){
          nJob = null;
          e.printStackTrace();
          log_writer.doLogging_QRmgr(this.logRoot, this.logName, "QRjob_downloadMgr.get_relegatedJob() error: "+e);
 }
 }
 return nJob;
 }





 /////////////////////////////////////////////
 // Starts c-move object on different thread.
 ////////////////////////////////////////////
 class start_cMover implements Runnable {

 private QRjob_downloader qrDwnldr = null;
 private BasicJob qJob = null;

 public start_cMover(QRjob_downloader qrDwnldr,
                     BasicJob qJob){

 this.qrDwnldr = qrDwnldr;
 this.qJob = qJob;
 }


 public void run(){
 try {
      this.qrDwnldr.startOp((QRjob)this.qJob, true);
 } catch (Exception e){
          e.printStackTrace();
          log_writer.doLogging_QRmgr(logRoot, logName, "QRjob_downloadMgr.start_cMover() error: "+e);
 }
 }
 }

 }