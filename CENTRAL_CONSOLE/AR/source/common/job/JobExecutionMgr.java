/*******************************************

 Author:  Mike Bass
 Year:    2012
 Purpose: Determines which jobs
          to execute, and in what order.

 *******************************************/

 package common.job;


 import java.util.concurrent.ArrayBlockingQueue;

 import org.dcm4che2.net.log_writer;
 import common.manager.BasicManager;
 import common.job.BasicJob;


 public class JobExecutionMgr implements Runnable {

 private String logRoot = null;
 private String logName = null;
 private int pauseTimeBetweenChecks = -1;
 private BasicManager nHawkStarter = null;
 private JobExecutor jobController = null;



 public JobExecutionMgr(String logRoot,
                        String logName,
                        int pauseTimeBetweenChecks,
                        BasicManager nHawkStarter,
                        JobExecutor jobController){
 this.logRoot = logRoot;
 this.logName = logName;
 this.pauseTimeBetweenChecks = pauseTimeBetweenChecks;
 this.nHawkStarter = nHawkStarter;
 this.jobController = jobController;
 }


 public void run(){

 while(true)
  {
   try {
        boolean sth_assigned = false;
        while(this.jobController.checkForExecutionVacancy().equals("busy"))
         {
		  BasicManager.sleepForDesiredTime(this.pauseTimeBetweenChecks);
		 }


        BasicJob nJob = this.checkForPendingJob();
        if(nJob != null)
          {
		   this.jobController.run_job(nJob);
		   sth_assigned = true;
		  }
		else
		  {
		   nJob = this.get_newNightHawkJob_2();
		   if(nJob != null)
		     {
		      this.jobController.run_job(nJob);
		      sth_assigned = true;
		     }
		   else
		     {
			  if(this.nHawkStarter.doAmissionHere)
			    {
			     nJob = this.get_relegatedJob();
			     if(nJob != null)
			       {
			    	//------------------------------------------------------------------------------------------------------------------
			    	log_writer.doLogging_QRmgr(this.logRoot, this.logName, "JobExecutionMgr() got relegated object: "+nJob.get_jobId());
			    	log_writer.doLogging_QRmgr(this.logRoot, this.logName, "JobExecutionMgr() this.nHawkStarter.doAmissionHere: "+this.nHawkStarter.doAmissionHere);
			    	//------------------------------------------------------------------------------------------------------------------

			    	this.jobController.run_job(nJob);
			        sth_assigned = true;
		           }
			    }
			 }
		  }

		if(!sth_assigned)
		  {
	       BasicManager.sleepForDesiredTime(this.pauseTimeBetweenChecks);
	      }

   } catch (Exception e){
            e.printStackTrace();
            log_writer.doLogging_QRmgr(this.logRoot, this.logName, "JobExecutionMgr.run() error: "+e);
   }
  }

 }


 public BasicJob checkForPendingJob(){

 BasicJob nJob = null;

 synchronized(this.nHawkStarter.get_queueLock()){
 try {
      int size = this.nHawkStarter.getRunningQueueObject().size();
      try {
           for(int a = 0; a < size; a++)
	          {
			   BasicJob Job = (BasicJob) this.nHawkStarter.getRunningQueueObject().poll();
			   if(Job != null)
			     {
                  if(this.nHawkStarter.check_1(Job, this.logRoot, this.logName))
				  	{
				  	 log_writer.doLogging_QRmgr(this.logRoot, this.logName,
				  	 "JobExecutionMgr.checkForPendingJob() msg: Cancelled or failed job found here!");
				  	 Job = null;
				    }
                  else if(this.nHawkStarter.check_2(Job, this.logRoot, this.logName))
				  	{
				  	 this.nHawkStarter.storeInQueue_currentlyRunningJob(Job);
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
	           log_writer.doLogging_QRmgr(this.logRoot, this.logName, "JobExecutionMgr.checkForPendingJob()<internal> error: "+e1);
	  }
 } catch (Exception e){
          nJob = null;
          e.printStackTrace();
          log_writer.doLogging_QRmgr(this.logRoot, this.logName, "JobExecutionMgr.checkForPendingJob() error: "+e);
 }
 }
 return nJob;
 }





 public BasicJob get_newNightHawkJob_2(){

 BasicJob nJob = null;

 synchronized(this.nHawkStarter.get_queueLock()){
 try {
      int size = this.nHawkStarter.getArrivingQueueObject().size();
      try {
           for(int a = 0; a < size; a++)
	          {
			   BasicJob Job = (BasicJob) this.nHawkStarter.getArrivingQueueObject().poll();
			   if(Job != null)
			     {
                  if(this.nHawkStarter.check_1(Job, this.logRoot, this.logName))
				  	{
				  	 log_writer.doLogging_QRmgr(this.logRoot, this.logName,
				  	 "JobExecutionMgr.get_newNightHawkJob_2() msg: Cancelled or failed job found here!");
				  	 Job = null;
				    }
                  else if(this.nHawkStarter.check_2(Job, this.logRoot, this.logName))
				  	{
				  	 this.nHawkStarter.storeInQueue_arrivingJob(Job);
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
	           log_writer.doLogging_QRmgr(this.logRoot, this.logName, "JobExecutionMgr.get_newNightHawkJob_2()<internal> error: "+e1);
	  }
 } catch (Exception e){
          nJob = null;
          e.printStackTrace();
          log_writer.doLogging_QRmgr(this.logRoot, this.logName, "JobExecutionMgr.get_newNightHawkJob_2() error: "+e);
 }
 }
 return nJob;
 }




 public BasicJob get_relegatedJob(){

 BasicJob nJob = null;

 synchronized(this.nHawkStarter.get_queueLock()){
 try {
      int size = this.nHawkStarter.relegatedJobs.size();
      try {
	       for(int a = 0; a < size; a++)
	          {
			   BasicJob Job = (BasicJob) this.nHawkStarter.relegatedJobs.poll();
			   if(Job != null)
			     {
                  if(this.nHawkStarter.check_1(Job, this.logRoot, this.logName))
				  	{
				  	 log_writer.doLogging_QRmgr(this.logRoot, this.logName,
				  	 "JobExecutionMgr.get_relegatedJob() msg: Cancelled or failed job found here!");
				  	 Job = null;
				    }
                  else if(this.nHawkStarter.check_2(Job, this.logRoot, this.logName))
				  	{
				  	 this.nHawkStarter.storeInQueue_relegatedJob(Job);
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
	           log_writer.doLogging_QRmgr(this.logRoot, this.logName, "JobExecutionMgr.get_relegatedJob()<internal> error: "+e1);
	  }
 } catch (Exception e){
          nJob = null;
          e.printStackTrace();
          log_writer.doLogging_QRmgr(this.logRoot, this.logName, "JobExecutionMgr.get_relegatedJob() error: "+e);
 }
 }
 return nJob;
 }

 }