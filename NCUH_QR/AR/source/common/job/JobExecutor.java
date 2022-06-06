/*************************************************************
 *
 *   Author:  Mike Bassey
 *   Year:    2012
 *   Purpose: Executes assigned job(s).
 *
 *************************************************************/

 package common.job;


 import java.io.File;
 import java.util.Vector;

 import org.dcm4che2.net.log_writer;
 import utility.db_manipulation.updateNightHawk;
 import org.dcm4che2.tool.dcmrcv.DcmRcv;
 import org.dcm4che2.tool.dcmsnd.DcmSnd;
 import utility.general.DcmSnd_starter;
 import org.dcm4che2.tool.dcmrcv.dcm_folderCreator;
 import https.https_clientManager;
 import utility.general.dicomFileParser;
 import common.manager.BasicManager;
 import common.job.BasicJob;
 import common.manager.lateArrivalScanner;

  //new NH..
 import NH.NH_job;




 public class JobExecutor {


 protected BasicManager nHawkStarter = null;
 protected int maxNoOfExecutingJobs = -1;
 protected String logRoot = null;
 protected String logName = null;
 protected conductJobExecution[] nHawkJobExecutors = null;
 protected int dataHandler_snoozing = -1;
 protected int dataHandler_cannotMove = -1;
 protected String[] reAssignArgs_1 = null;
 protected String[] reAssignArgs_2 = null;
 protected Vector<conductJobExecution> temp_jobExec = new Vector<conductJobExecution>(5);
 protected int tempExecCounter = 0;




 public JobExecutor(BasicManager nHawkStarter,
				    int maxNoOfExecutingJobs,
				    String logRoot,
				    String logName,
				    int dataHandler_snoozing,
				    int dataHandler_cannotMove,
				    String[] reAssignArgs_1,
				    String[] reAssignArgs_2){

 this.nHawkStarter = nHawkStarter;
 this.maxNoOfExecutingJobs = maxNoOfExecutingJobs;
 this.logRoot = logRoot;
 this.logName = logName;
 this.dataHandler_snoozing = dataHandler_snoozing;
 this.dataHandler_cannotMove = dataHandler_cannotMove;
 this.reAssignArgs_1 = reAssignArgs_1;
 this.reAssignArgs_2 = reAssignArgs_2;

 try {
      this.nHawkJobExecutors = new conductJobExecution[this.maxNoOfExecutingJobs];
 } catch (Exception e) {
          e.printStackTrace();
          log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<JobExecutor>.constructor() error: "+e);
          System.exit(0);
 }
 }



 public synchronized String checkForAnExecutingThread(){

 String status = "no_executing";
 try {
      for(int a = 0; a < this.nHawkJobExecutors.length; a++)
         {
		  if(this.nHawkJobExecutors[a] == null)
		    {
			}
		  else if(this.nHawkJobExecutors[a].get_status().equals("busy"))
		    {
		     status = "executing";
			 break;
		    }
		 }

     int size = this.temp_jobExec.size();
     for(int a = 0; a < size; a++)
        {
         conductJobExecution nhawkExec = (conductJobExecution) this.temp_jobExec.get(a);
         if(nhawkExec != null)
           {
		    if(nhawkExec.get_status().equals("busy"))
		      {
               status = "executing";
			   break;
			  }
		   }
        }
 } catch (Exception e) {
          status = "executing";
          e.printStackTrace();
          log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<JobExecutor>.checkForAnExecutingThread() error: "+e);
 }
 return status;
 }



 public synchronized String checkForExecutionVacancy(){

 String status = "busy";
 try {
      for(int a = 0; a < this.nHawkJobExecutors.length; a++)
         {
		  if(this.nHawkJobExecutors[a] == null)
		    {
			 status = "free";
			 break;
			}
		  else if(this.nHawkJobExecutors[a].get_status().equals("idle"))
		    {
		     status = "free";
			 break;
		    }
		 }
 } catch (Exception e) {
          status = "busy";
          e.printStackTrace();
          log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<JobExecutor>.checkForExecutionVacancy() error: "+e);
 }
 return status;
 }



 public synchronized void run_job(BasicJob nHawkJob){

 boolean jobAssigned = false;
 int busyState = 0;

 for(int a = 0; a < this.nHawkJobExecutors.length; a++)
    {
	 if(this.nHawkJobExecutors[a] != null)
	   {
	    if((this.nHawkJobExecutors[a].get_status()).equals("idle"))
	      {
		   this.nHawkJobExecutors[a].set_status("busy");
		   this.nHawkJobExecutors[a].setJobObject(nHawkJob);
		   new Thread(this.nHawkJobExecutors[a]).start();
		   jobAssigned = true;
		   break;
		  }
		else
		  {
		   busyState++;
		  }
	   }
	 else
	   {
	    this.nHawkJobExecutors[a] = new conductJobExecution();
	    this.nHawkJobExecutors[a].set_status("busy");
	    this.nHawkJobExecutors[a].setJobObject(nHawkJob);
	    new Thread(this.nHawkJobExecutors[a]).start();
		jobAssigned = true;
		break;
	   }
    }

   if(!jobAssigned)
	 {
	  if(busyState >= this.nHawkJobExecutors.length)
	    {
		 //this should never happen!
		 log_writer.doLogging_QRmgr(this.logRoot, this.logName,
		 "<JobExecutor.run_job() msg: A call to run job even when all threads are engaged!");

		  //lets run as an emergency.
		  conductJobExecution nhawkExec = new conductJobExecution();
		  nhawkExec.setJobObject(nHawkJob, (this.tempExecCounter++));
		  this.temp_jobExec.add(nhawkExec);

		  new Thread(nhawkExec).start();
		}
	  }
 }



public synchronized void startThisJob(BasicJob nHawkJob){

boolean jobAssigned = false;
try{
    if(nHawkJob != null)
      {
		nHawkJob.set_status("pending");

		//any free execution slot?
		for(int a = 0; a < this.nHawkJobExecutors.length; a++)
		   {
			if(this.nHawkJobExecutors[a] == null)
			  {
			   this.nHawkJobExecutors[a] = new conductJobExecution();
			   this.nHawkJobExecutors[a].set_status("busy");
			   this.nHawkJobExecutors[a].setJobObject(nHawkJob);
			   new Thread(this.nHawkJobExecutors[a]).start();
			   jobAssigned = true;
			   break;
			  }
			else if((this.nHawkJobExecutors[a].get_status()).equals("idle"))
			  {
			   this.nHawkJobExecutors[a].set_status("busy");
			   this.nHawkJobExecutors[a].setJobObject(nHawkJob);
			   new Thread(this.nHawkJobExecutors[a]).start();
			   jobAssigned = true;
			   break;
			  }
		   }

	if(!jobAssigned) //must swap positions with an executing job..
	  {
	   for(int a = 0; a < this.nHawkJobExecutors.length; a++)
		  {
		   if(this.nHawkJobExecutors[a] != null)
			 {
			  if((this.nHawkJobExecutors[a].get_status()).equals("busy"))
				{
				 BasicJob currentJob = this.nHawkJobExecutors[a].get_executingJob();
				 if(currentJob != null)
				   {
					currentJob.set_status("pending");
					while(!(this.nHawkJobExecutors[a].get_status().equals("idle")))
					 {
					  BasicManager.sleepForDesiredTime(BasicManager.jobExecCleanUpTime);
					 }
				   }

				 this.nHawkJobExecutors[a].set_status("busy");
				 this.nHawkJobExecutors[a].setJobObject(nHawkJob);
				 new Thread(this.nHawkJobExecutors[a]).start();

				 if(currentJob != null)
				   {
					this.nHawkStarter.storeInQueue_currentlyRunningJob(currentJob);
				   }
				 jobAssigned = true;
				 break;
				}
			 }
		  }
	  }

	 if(!jobAssigned) //This should never happen. We must run this as an emergency.
	   {
		log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<JobExecutor.startThisJob() msg: Failed to swap executing jobs.");

		//lets run as an emergency.
		conductJobExecution nhawkExec = new conductJobExecution();
		nhawkExec.setJobObject(nHawkJob, (this.tempExecCounter++));
		this.temp_jobExec.add(nhawkExec);
		new Thread(nhawkExec).start();
	   }
   }

} catch (Exception e) {
         e.printStackTrace();
         log_writer.doLogging_QRmgr(logRoot, logName, "<JobExecutor>.startThisJob() exception: "+e);
}
}



public synchronized boolean attemptPreempt(BasicJob nHawkJob){

boolean job_ran = false;
try {
     for(int a = 0; a < this.nHawkJobExecutors.length; a++)
	    {
         if(this.nHawkJobExecutors[a] == null){}
         else if((this.nHawkJobExecutors[a].get_status()).equals("idle")){}
         else
          {
           BasicJob currentJob = this.nHawkJobExecutors[a].get_executingJob();
           if(currentJob != null)
             {
			  if((!(currentJob.essentialsSet()))||
			    (   currentJob.get_status().equals("awaiting download")))
			    {
				  //take over from this 'yet-to-start' job..
				  currentJob.set_status("pending");
				  while(!(this.nHawkJobExecutors[a].get_status().equals("idle")))
				   {
				    BasicManager.sleepForDesiredTime(BasicManager.jobExecCleanUpTime);
				   }

				  this.nHawkJobExecutors[a].set_status("busy");
				  this.nHawkJobExecutors[a].setJobObject(nHawkJob);
				  new Thread(this.nHawkJobExecutors[a]).start();
			      this.nHawkStarter.storeInQueue_currentlyRunningJob(currentJob);
				  job_ran = true;
				  break;
				}
			 }
		  }
	    }
} catch (Exception e) {
         job_ran = false;
         e.printStackTrace();
         log_writer.doLogging_QRmgr(logRoot, logName, "<JobExecutor>.attemptPreempt() exception: "+e);
}
return job_ran;
}




//Used for pause, stop and cancel.
public synchronized void stopThisJob(String jobId, String opType){

try {
     for(int a = 0; a < this.nHawkJobExecutors.length; a++)
        {
		 if(this.nHawkJobExecutors[a] != null)
		   {
		    BasicJob nHawkJob = this.nHawkJobExecutors[a].get_executingJob();
		    if(nHawkJob != null)
		      {
			   if(nHawkJob.get_jobId().equals(jobId))
			     {
				  nHawkJob.set_status(opType);
				  if(opType.equals("cancelled")){}
				  else
				   {
				    while(!(this.nHawkJobExecutors[a].get_status().equals("idle")))
				     {
				      BasicManager.sleepForDesiredTime(BasicManager.jobExecCleanUpTime);
				     }
			        this.nHawkStarter.storeInQueue_currentlyRunningJob(nHawkJob);
				   }
				  break;
				 }
			  }
		   }
		}

} catch (Exception e) {
         e.printStackTrace();
         log_writer.doLogging_QRmgr(logRoot, logName, "<JobExecutor>.stopThisJob() exception: "+e);
}
}





protected DcmSnd get_dcmsnd(String cuid, BasicJob nJob){

nHawkStarter.do_dbUpdate(nJob,2, null, "connecting...", logRoot, logName);

DcmSnd dcmsndObject = null;
long startTime = System.currentTimeMillis();
try {
	 int count = 0;
	 boolean continueOp = true;
	 boolean doAdd = false;
     while(continueOp)
      {
		if(!(nJob.get_transferMode().equals("dcmsnd")))
		  {
		   log_writer.doLogging_QRmgr(this.logRoot, this.logName,
		   "<JobExecutor.get_dcmsnd() msg: transfer mode changed mid search for dcmsnd: "+cuid);
		   break;
		  }

		if(!(nJob.get_status().equals("in progress")))
		  {
		   log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<JobExecutor.get_dcmsnd() msg: job status change mid dcmsnd search to: "+nJob.get_status());
		   break;
		  }

         int jobType = nJob.getJobType();

         //=======================
         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<JobExecutor.get_dcmsnd() msg: JOB TYPE = "+jobType);
         //=======================

         if(jobType == 0)
           {
		    dcmsndObject = this.nHawkStarter.get_dcmsnd(cuid, nJob.get_dcmsndMainStats());
	       }
	     else if(jobType == 1)
	       {
		    dcmsndObject = nJob.get_dcmsnd(cuid, nJob.get_dcmsndMainStats());
		   }
		 if(dcmsndObject == null)
		   {
			//-------------------------------------------------------------
			log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<JobExecutor.get_dcmsnd() error: Could not find dcmsnd for: "+cuid);
			//-------------------------------------------------------------

		   doAdd = true;

		   Object[][] cuid_tsuid = new Object[2][2];
		   cuid_tsuid[0][0] = (Object) cuid;
		   cuid_tsuid[1][0] = (Object) this.nHawkStarter.get_aliveSopClass();

		   String[] tsyntaxes = this.nHawkStarter.get_commonTS();
		   cuid_tsuid[0][1] = (Object) tsyntaxes;
		   cuid_tsuid[1][1] = (Object) tsyntaxes;

		   dcmsndObject = new DcmSnd();
		   dcmsndObject.set_alivenessStats(this.reAssignArgs_2);
		   dcmsndObject.setSCandTS(cuid_tsuid, this.nHawkStarter);
		   dcmsndObject.set_objectStatus("temp");

		   dcmsndObject.set_TEL_MSG(this.nHawkStarter.get_TEL_MSG());

		   //-----------------------------------

              if(nHawkStarter.get_managerType() != 2)
                {
				String[] pDemogrhcs = nJob.get_jobDemgraphics();
				if(pDemogrhcs == null)
				  {
				   log_writer.doLogging_QRmgr(logRoot, logName,  "<JobExecutor.get_dcmsnd()msg: JOB DEMOGRAPHICS ARE NULL "+nJob.get_jobId());
				  }
				else
				  {
					String[] jobDetails = new String[8];
					jobDetails[0] = nJob.get_jobId();
					jobDetails[1] = pDemogrhcs[0];
					jobDetails[2] = pDemogrhcs[1];
					jobDetails[3] = pDemogrhcs[5];
					jobDetails[4] = pDemogrhcs[6];
					jobDetails[5] = pDemogrhcs[4];
					jobDetails[6] = pDemogrhcs[7];
					jobDetails[7] = Integer.toString(nJob.get_imagesUploaded());

					nJob.set_detailsForDcmSnd(jobDetails);

					dcmsndObject.set_jobDetails(jobDetails);
				  }
               }
		   //-----------------------------------------------------
		   dcmsndObject.set_mainStats(nJob.get_dcmsndMainStats());

		   String[] sndDetails = nJob.get_dcmsndMainStats();
		   if(sndDetails == null)
		     {
		      log_writer.doLogging_QRmgr(this.logRoot, this.logName,
		      "<JobExecutor.get_dcmsnd() msg: sndDetails is null. ");
		     }
		   else
		     {
		      String alias = this.nHawkStarter.getDestination(sndDetails[1], sndDetails[2]);
		      dcmsndObject.set_nodeAlias(alias);
		     }
		   //-----------------------------------------------------

		   String[] serverDetails = this.nHawkStarter.get_desiredDcmsnd(nJob.get_dcmsndMainStats());
		   String[] newDetails = new String[this.reAssignArgs_1.length];
		   for(int a = 0; a < newDetails.length; a++)
		      {
			   if(a == 11)
			     {
				  newDetails[a] = serverDetails[0]+"@"+serverDetails[1]+":"+serverDetails[2];
				 }
			   else
			     {
				  newDetails[a] = this.reAssignArgs_1[a];
				 }
			  }

		   new Thread(new DcmSnd_starter(dcmsndObject,newDetails,this.logRoot,this.logName)).start();

		   while(dcmsndObject.get_ObjectState().equals("not_yet_set"))
			{
			 BasicManager.sleepForDesiredTime(1000); //TODO: take this value in from properties file.

			//-------------------------------------------------------------
			log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<JobExecutor.get_dcmsnd() msg: DcmSnd not yet set for job: "+(nJob.get_jobId()));
			//-------------------------------------------------------------

			}

		   if(dcmsndObject.get_ObjectState().equals("failed")) //try again..
			 {
			  log_writer.doLogging_QRmgr(this.logRoot, this.logName,
			  "<JobExecutor.get_dcmsnd() error: Couldn't establish connection for: "+cuid+", attempts: "+count);
			  count++;
			  BasicManager.sleepForDesiredTime(this.nHawkStarter.connRetryPauseTime);
			  dcmsndObject = null;
			  if(count >= this.nHawkStarter.maxConnRetryAttempts)
			    {
			     nJob.noOfTimesConnAttemptsMaxed.incrementAndGet();
			     nJob.set_status("relegated");
			    }
			 }
		   else
			 {
			  continueOp = false;
			  break;
			 }
           }
          else
           {
		    continueOp = false;
			break;
		   }
	 }

if((doAdd)&&(dcmsndObject != null))
  {
   int jobType = nJob.getJobType();
   if(jobType == 0)
     {
      this.nHawkStarter.addToTempDcmSnd(dcmsndObject);
     }
   else if(jobType == 1)
     {
	  nJob.addToTempDcmSnd(dcmsndObject);
	 }
  }

long endTime = System.currentTimeMillis()-startTime;

if(nJob != null)
  {
   nJob.addTo_connectionTime(endTime);
  }

} catch (Exception e) {
         dcmsndObject = null;
         e.printStackTrace();
         log_writer.doLogging_QRmgr(logRoot, logName, "<JobExecutor>.get_dcmsnd() exception: "+e);
}

nHawkStarter.do_dbUpdate(nJob,2, null, nJob.get_status(), logRoot, logName);

return dcmsndObject;
}




public synchronized void delete_tempExecObject(int id){

try {
     int size = this.temp_jobExec.size();
     for(int a = 0; a < size; a++)
        {
         conductJobExecution nhawkExec = (conductJobExecution) this.temp_jobExec.get(a);
         if(nhawkExec != null)
           {
		    if(nhawkExec.get_tempExec_id() == id)
		      {
               this.temp_jobExec.remove(a);
               break;
			  }
		   }
        }

} catch (Exception e) {
         e.printStackTrace();
         log_writer.doLogging_QRmgr(logRoot, logName, "<JobExecutor>.delete_tempExecObject() exception: "+e);
}
}



public synchronized BasicJob checkAndGet_permExecObject(String id){

BasicJob found_nJob = null;
try {
     for(int a = 0; a < this.nHawkJobExecutors.length; a++)
        {
	     if(this.nHawkJobExecutors[a] != null)
	       {
		    BasicJob nJob = this.nHawkJobExecutors[a].get_executingJob();
		    if(nJob != null)
		      {
		       if(nJob.get_jobId().equals(id))
			     {
			      found_nJob = nJob;
			      break;
			     }
		      }
		   }
	    }
} catch (Exception e) {
         found_nJob = null;
         e.printStackTrace();
         log_writer.doLogging_QRmgr(logRoot, logName, "<JobExecutor>.checkAndGet_permExecObject() exception: "+e);
}
return found_nJob;
}



public synchronized BasicJob checkAndGet_tempExecObject(String id){

BasicJob found_nJob = null;
try {
     int size = this.temp_jobExec.size();
     for(int a = 0; a < size; a++)
        {
         conductJobExecution nhawkExec = (conductJobExecution) this.temp_jobExec.get(a);
         if(nhawkExec != null)
           {
		    BasicJob nJob = nhawkExec.get_executingJob();
		    if(nJob != null)
		      {
		       if(nJob.get_jobId().equals(id))
		         {
			      found_nJob = nJob;
			      break;
			     }
		      }
		   }
        }
} catch (Exception e) {
         found_nJob = null;
         e.printStackTrace();
         log_writer.doLogging_QRmgr(logRoot, logName, "<JobExecutor>.checkAndGet_tempExecObject() exception: "+e);
}
return found_nJob;
}



public synchronized BasicJob checkAllForNightHawkObject(String id){

BasicJob found_nJob = null;
try {
     found_nJob = this.checkAndGet_permExecObject(id);
     if(found_nJob == null)
       {
	    found_nJob = this.checkAndGet_tempExecObject(id);
	   }
} catch (Exception e) {
         found_nJob = null;
         e.printStackTrace();
         log_writer.doLogging_QRmgr(logRoot, logName, "<JobExecutor>.checkAndGet_tempExecObject() exception: "+e);
}
return found_nJob;
}




public synchronized boolean checkAndDo(String id, String opType){
boolean done = false;
try {
     BasicJob found_nJob = this.checkAndGet_permExecObject(id);
     if(found_nJob == null)
       {
	    found_nJob = this.checkAndGet_tempExecObject(id);
	   }

	 if(found_nJob != null)
	   {
        this.sortJobObjects(opType);
        done = true;
	   }

} catch (Exception e) {
         e.printStackTrace();
         log_writer.doLogging_QRmgr(logRoot, logName, "<JobExecutor>.checkAndDo() exception: "+e);
}
return done;
}







private synchronized void sort_permExecObject(String opType){

try {
     for(int a = 0; a < this.nHawkJobExecutors.length; a++)
        {
	     if(this.nHawkJobExecutors[a] != null)
	       {
		    BasicJob nJob = this.nHawkJobExecutors[a].get_executingJob();
		    if(nJob != null)
		      {
		       this.doOpOnJob(nJob,opType);
		      }
		   }
	    }
} catch (Exception e) {
         e.printStackTrace();
         log_writer.doLogging_QRmgr(logRoot, logName, "<JobExecutor>.sort_permExecObject() exception: "+e);
}
}


private synchronized void sort_tempExecObject(String opType){

try {
     int size = this.temp_jobExec.size();
     for(int a = 0; a < size; a++)
        {
         conductJobExecution nhawkExec = (conductJobExecution) this.temp_jobExec.get(a);
         if(nhawkExec != null)
           {
		    BasicJob nJob = nhawkExec.get_executingJob();
		    if(nJob != null)
		      {
		       this.doOpOnJob(nJob,opType);
		      }
		   }
        }
} catch (Exception e) {
         e.printStackTrace();
         log_writer.doLogging_QRmgr(logRoot, logName, "<JobExecutor>.sort_tempExecObject() exception: "+e);
}
}



private void doOpOnJob(BasicJob nJob, String opType){

try {
	 if((opType.equals("STOP_ALL")) || (opType.equals("STOP")))
	   {
	    nJob.set_status("stopped");

	    if(opType.equals("STOP"))
	      {
	       nHawkStarter.do_dbUpdate(nJob,2, null, "stopped", logRoot, logName);
	      }
	   }
	 else if(opType.equals(("PAUSE_ALL")) || (opType.equals("PAUSE")))
	   {
	    nJob.set_status("paused");

	    if(opType.equals("PAUSE"))
	      {
	       nHawkStarter.do_dbUpdate(nJob,2, null, "paused", logRoot, logName);
	      }
	   }
	 else if((opType.equals("CANCEL_ALL")) || (opType.equals("CANCEL")))
	   {
	    new Thread(new doCancelling(nJob, opType)).start();
	   }
} catch (Exception e) {
         e.printStackTrace();
         log_writer.doLogging_QRmgr(logRoot, logName, "<JobExecutor>.doOpOnJob() exception: "+e);
}
}



////////////////////////////////////////
//   To avoid possible deadlock..
////////////////////////////////////////
class doCancelling implements Runnable {

private BasicJob job = null;
private String opType = null;

public doCancelling(BasicJob job, String opType){

this.job = job;
this.opType = opType;
}


public void run(){
try {
     boolean done = nHawkStarter.cancelOp(this.job, this.opType);
	 if(!done)
	   {
	    this.job.set_status("cancelled");

	    if(this.opType.equals("CANCEL"))
	      {
	       nHawkStarter.do_dbUpdate(this.job,2, null, "cancelled", logRoot, logName);
	       nHawkStarter.do_dbUpdate(this.job,1, "0", null, logRoot, logName);
	      }
       }
} catch (Exception e) {
         e.printStackTrace();
         log_writer.doLogging_QRmgr(logRoot, logName, "<JobExecutor>.doCancelling.run() exception: "+e);
}
}
}



public synchronized void sortJobObjects(String opType){

if((!(opType.equals("START")))  || (!(opType.equals("START_ALL"))))
  {
   this.sort_permExecObject(opType);
   this.sort_tempExecObject(opType);
  }
}


public synchronized void changeDestJobObjects(String[] opType){

this.changeDest_permExecObject(opType);
this.changeDest_tempExecObject(opType);
}



private synchronized void changeDest_permExecObject(String[] opType){

try {
     for(int a = 0; a < this.nHawkJobExecutors.length; a++)
        {
	     if(this.nHawkJobExecutors[a] != null)
	       {
		    BasicJob nJob = this.nHawkJobExecutors[a].get_executingJob();
		    if(nJob != null)
		      {
		       this.nHawkStarter.switchDest(opType, nJob);
		      }
		   }
	    }
} catch (Exception e) {
         e.printStackTrace();
         log_writer.doLogging_QRmgr(logRoot, logName, "<JobExecutor>.changeDest_permExecObject() exception: "+e);
}
}


private synchronized void changeDest_tempExecObject(String[] opType){

try {
     int size = this.temp_jobExec.size();
     for(int a = 0; a < size; a++)
        {
         conductJobExecution nhawkExec = (conductJobExecution) this.temp_jobExec.get(a);
         if(nhawkExec != null)
           {
		    BasicJob nJob = nhawkExec.get_executingJob();
		    if(nJob != null)
		      {
		       this.nHawkStarter.switchDest(opType, nJob);
		      }
		   }
        }
} catch (Exception e) {
         e.printStackTrace();
         log_writer.doLogging_QRmgr(logRoot, logName, "<JobExecutor>.changeDest_tempExecObject() exception: "+e);
}
}


protected JobExecutor get_thisObject(){

return this;
}



public synchronized Object[] getActiveJobs(){

Vector<BasicJob> activeJobs = new Vector<BasicJob>(5);
try {
     for(int a = 0; a < this.nHawkJobExecutors.length; a++)
        {
	     if(this.nHawkJobExecutors[a] != null)
	       {
		    BasicJob nJob = this.nHawkJobExecutors[a].get_executingJob();
		    if(nJob != null)
		      {
		       activeJobs.add(nJob);
		      }
		   }
	    }
} catch (Exception e) {
         e.printStackTrace();
         log_writer.doLogging_QRmgr(logRoot, logName, "<JobExecutor>.getActiveJobs() exception: "+e);
}
return activeJobs.toArray();
}











 //----------------------------------------------------
 //         Executes a job as requested.
 //----------------------------------------------------
 public class conductJobExecution implements Runnable {

 private BasicJob n_job = null;
 private String default_status = "idle";
 private String status = this.default_status;
 private String jobStatus = null;
 private DcmSnd dcmsnd = null;
 private int tempExec_id = -1;

 int counter = 0; //just for testing...



 public conductJobExecution(){}

 public void setJobObject(BasicJob n_job){

 this.n_job = n_job;
 }


 public void setJobObject(BasicJob n_job,
                             int tempExec_id){
 this.n_job = n_job;
 this.tempExec_id = tempExec_id;
 }


 public int get_tempExec_id(){

 return this.tempExec_id;
 }

 public String get_status(){

 return this.status;
 }

 public void set_status(String value){

 this.status = value;
 }


 public BasicJob get_executingJob(){

 return this.n_job;
 }


 public void run(){

 this.status = "busy"; //just in case..

 try {
      boolean continueWithOp = false;

      if(this.n_job != null)
        {
	     //Log this?
	    }


      if(this.n_job == null)
        {
	     log_writer.doLogging_QRmgr(logRoot, logName, "<conductJobExecution>.run() msg: night hawk job is null");
	    }
	  else if((this.n_job.get_status().equals("completed"))||
	         ( this.n_job.get_status().equals("paused"))||
	         ( this.n_job.get_status().equals("stopped"))||
	         ( this.n_job.get_status().equals("cancelled"))||
	         ( this.n_job.get_status().equals("Not found"))||
             ( this.n_job.get_status().indexOf("fail") >= 0))
	    {
		 //log this?
		}
	  else if(this.n_job.get_status().equals("awaiting re-start"))
	    {
	     while(this.n_job.get_status().equals("awaiting re-start"))
		  {
		   DcmRcv.sleepForDesiredTime(1000); //TODO: Take value in from properties file.
		   if(this.n_job.get_status().equals("re-started"))
		     {
		   	  continueWithOp = true; //we can proceed.
		     }
		   else
		   	 {
		      this.check_currentStatus();
		     }
		  }
	    }
	  else if(!(this.n_job.essentialsSet()))
	    {
         this.n_job.set_status("awaiting download");
         nHawkStarter.do_dbUpdate(this.n_job,2, null, this.n_job.get_status(), logRoot, logName);
         while((this.n_job.get_status().equals("awaiting download"))&&
              (!(this.n_job.essentialsSet())))
           {
            DcmRcv.sleepForDesiredTime(1000); //TODO: Take value in from properties file.
		   }
         if(this.n_job.get_status().equals("awaiting download"))
           {
		    continueWithOp = true; //we can proceed.
		   }
		 else
		   {
            this.check_currentStatus();
		   }
	    }
	  else
	    {
	     continueWithOp = true;
	    }

	  if(continueWithOp)
	    {
         String fileLocation = this.n_job.get_fileLocation();
         if(fileLocation == null)
           {
	        log_writer.doLogging_QRmgr(logRoot, logName, "<conductJobExecution>.run() msg: null file location for night hawk job: "+this.n_job.get_jobId());
	       }
	     else
	       {
            this.n_job.set_status("in progress");
            String destination = this.n_job.get_storeLocation()+"/"+dcm_folderCreator.generate_imageStoreName()+"/"+this.n_job.get_jobId();
            destination = destination.replaceAll("\\\\","/");
            destination = destination.replaceAll("//","/");

            boolean created = dcm_folderCreator.createExactlyThatLocation(destination);
            if(!created)
			  {
			   log_writer.doLogging_QRmgr(logRoot, logName, "conductJobExecution <night hawk: "+this.n_job.get_jobId()+">.run() msg: could not create folder move location!");
			   this.n_job.set_status("failed <code:001>");
			  }

            if(created)
              {
			   this.n_job.set_actualFileStore(destination);
			   nHawkStarter.do_dbUpdate(this.n_job,2, null, "in progress", logRoot, logName);
			  }




            //=============================================================================
            NH_job nJOB = this.n_job.get_nhJob();
            if(nJOB == null)
              {
				if(n_job.get_transferMode().equals("dcmsnd"))
				  {
				   this.dcmsnd = get_dcmsnd(this.n_job.get_first_known_cuid(), this.get_executingJob()); //get appropriate dcmsnd..
				   if(this.dcmsnd == null)
					 {
					  if(n_job.get_transferMode().equals("dcmsnd")) //was transfer mode switched mid-search?
						{
						 if(this.n_job.get_status().equals("in progress"))
						   {
							log_writer.doLogging_QRmgr(logRoot, logName, "conductJobExecution<"+this.n_job.get_jobId()+">.run() msg: Failed to get appropriate DcmSnd!");
							this.n_job.set_status("failed <code:002>");
						   }
						}
					 }
				  }
		      }
		      //=============================================================================

            while(this.n_job.get_status().equals("in progress"))
             {
              //nHawkStarter.do_dbUpdate(nJob,2, null, "stopped", logRoot, logName);

              boolean continueOp = this.check_restackQueue();

              if(continueOp)
                {
				  String report = this.moveFiles(fileLocation, destination, "0");

				  nJOB = this.n_job.get_nhJob();
				  if(nJOB != null)
				    {
                      if((report.equals("-100"))||(report.equals("-200")))
						{
						 DcmRcv.sleepForDesiredTime(dataHandler_cannotMove);
						}
				      else if(report.equals("0"))//no file
				        {
						 if(nJOB.taskCompleted())
						   {
                            log_writer.doLogging_QRmgr(logRoot, logName, "conductJobExecution<"+this.n_job.get_jobId()+">.run() msg: NH job completed!");
							this.checkForCompletion();
							nJOB = null;
							break;
						   }
						 else
						  {
						   DcmRcv.sleepForDesiredTime(dataHandler_snoozing);
						  }
						}
				      else
				        {
					     this.doStoring(this.n_job, report);
					    }
					}

				  //=========================================================================
				  else
				    {

					  if(report.equals("0"))//no file
						{
						 if(!(this.n_job.referralCardRetrieved()))
						   {
							DcmRcv.sleepForDesiredTime(dataHandler_snoozing);
						   }
						 else if((this.n_job.get_Association() == null) && (this.n_job.downloadIsComplete()))
						   {
							log_writer.doLogging_QRmgr(logRoot, logName, "conductJobExecution<"+this.n_job.get_jobId()+">.run() msg: Association is null!");
							this.checkForCompletion();
							break;
						   }
						 else if(((this.n_job.get_Association().getSocket()) == null)  && (this.n_job.downloadIsComplete()))
						   {
							log_writer.doLogging_QRmgr(logRoot, logName, "conductJobExecution<"+this.n_job.get_jobId()+">.run() msg: Association's socket is null!");
							this.checkForCompletion();
							break;
						   }
						 else if(((this.n_job.get_Association().getSocket()).isClosed()) && (this.n_job.downloadIsComplete()))
						   {
							log_writer.doLogging_QRmgr(logRoot, logName, "conductJobExecution<"+this.n_job.get_jobId()+">.run() msg: Association's socket is closed!");
							this.checkForCompletion();
							break;
						   }
						 else if((!((this.n_job.get_Association().getSocket()).isConnected()))  && (this.n_job.downloadIsComplete()))
						   {
							log_writer.doLogging_QRmgr(logRoot, logName, "conductJobExecution<"+this.n_job.get_jobId()+">.run() msg: Association's socket not connected!");
							this.checkForCompletion();
							break;
						   }
						 else
						   {
							DcmRcv.sleepForDesiredTime(dataHandler_snoozing);
						   }
						}
					  else if((report.equals("-100"))||(report.equals("-200")))
						{
						 DcmRcv.sleepForDesiredTime(dataHandler_cannotMove);
						}
					  else
						{
                         this.doStoring(this.n_job, report);
						}

				   }//end else, nJob = null;
				   //=============================================================================================

			    }//end continueOp..
			 }//end while..

             this.check_currentStatus();

		   }
	    }

 } catch (Exception e) {
         e.printStackTrace();
         log_writer.doLogging_QRmgr(logRoot, logName, "<conductJobExecution>.run() error: "+e);
 }

 if(this.tempExec_id != (-1))
   {
    delete_tempExecObject(this.get_tempExec_id());
   }

 //synchronized(get_thisObject())
 // {
   this.n_job = null;
   this.set_status(this.default_status);

   counter = 0;
 // }
 }



private void doStoring(BasicJob n_job, String report)
{
 try
 {
	 incrementCount(this.n_job, report);

	 Object[] data = new Object[3];
	 data[0] = report;
	 data[1] = this.n_job;
	 data[2] = new Integer(this.n_job.currentRun.get());


	 String cuid = new dicomFileParser().get_cuid(new File(report), logRoot, logName);
	 if(cuid == null)
	   {
		cuid = this.n_job.get_first_known_cuid();
	   }

	   //TODO: Check that cuid is not null.

	 if(n_job.get_transferMode().equals("dcmsnd"))
	   {
		boolean goFind = true;

		if(this.dcmsnd != null)
		  {
		   if((this.matches(n_job, this.dcmsnd)) &&
			 ( nHawkStarter.findSopClass(this.dcmsnd.get_supportedSopClasses(), cuid))&&
			 (!(this.dcmsnd.socketIsClosed()))&&
			 (  this.dcmsnd.still_retrieving()))
			 {
			  this.dcmsnd.putInStore(data);
			  goFind = false;
			 }
		  }


		if(goFind)
		  {
		   this.dcmsnd = get_dcmsnd(cuid, this.get_executingJob()); //get appropriate dcmsnd..
		   if(this.dcmsnd == null)
			 {
			  if(n_job.get_transferMode().equals("dcmsnd")) //was transfer mode switched mid-search?
				{
				 if(this.n_job.get_status().equals("in progress"))
				   {
					log_writer.doLogging_QRmgr(logRoot, logName, "conductJobExecution<"+this.n_job.get_jobId()+">.run(failed_code:001) msg: Failed to get appropriate DcmSnd!");
					this.n_job.set_status("failed <code:003>");
				   }
				 else
				   {
					this.n_job.storeInPendingQueue(data);
				   }
				}
			  else
				{
				 this.findAndStoreInHttpsController(this.n_job, data);
				}
			 }
		   else
			 {
			  this.dcmsnd.putInStore(data);
			 }
		  }
	   }
	 else
	   {
		this.findAndStoreInHttpsController(this.n_job, data);
	   }

 } catch (Exception e) {
         e.printStackTrace();
         log_writer.doLogging_QRmgr(logRoot, logName, "<conductJobExecution>.doStoring() error: "+e);
 }
}



private void incrementCount(BasicJob job, String report){
try {
	 job.increment_stored_moved(1);
	 job.incrementBytesDownloaded((new File(report)).length());

} catch (Exception e) {
         e.printStackTrace();
         log_writer.doLogging_QRmgr(logRoot, logName, "<conductJobExecution>.incrementCount() error: "+e);
}
}


//++++++++++++++++++++++++++++++++++++++
//  TODO: Re-examine this logic!
//++++++++++++++++++++++++++++++++++++++
public void checkForCompletion(){

try {
	lateArrivalScanner lscanner = nHawkStarter.get_lateScanner();
	if(lscanner != null)
	  {
	   String loc = lscanner.get_storageDest(this.n_job.get_jobId());
	   if(loc == null)
		 {
		  log_writer.doLogging_QRmgr(logRoot, logName,
		  "<conductJobExecution>.checkForCompletion() msg: Failed to create late scanner location for job "+this.n_job.get_jobId());
		 }
	   else
	     {
		  boolean continueOp = true;

		  Object[] argsForScanner = new Object[6];
		  argsForScanner[0] = this.n_job.get_jobId();
		  argsForScanner[1] = this.n_job.get_transferMode();

		  if(this.n_job.get_transferMode().equals("dcmsnd"))
		    {
		     argsForScanner[2] = this.n_job.get_dcmsndMainStats();
		    }
		  else if(this.n_job.get_transferMode().equals("https"))
		    {
			 //TODO: handle later
			 continueOp = false;
			 log_writer.doLogging_QRmgr(logRoot, logName,
			 "<conductJobExecution>.checkForCompletion() msg: {Need for https} Cannot proceed with late scan for job "+this.n_job.get_jobId());
			}

          if(continueOp)
            {
		     argsForScanner[3] = this.n_job.get_fileLocation();
		     argsForScanner[4] = new Long(System.currentTimeMillis());
		     argsForScanner[5] = loc;
		     lscanner.storeInQueue(argsForScanner);
		    }
		 }
	  }
} catch (Exception e) {
         e.printStackTrace();
         log_writer.doLogging_QRmgr(logRoot, logName, "<conductJobExecution>.checkForCompletion() error 1: "+e);
}


try {
	while(true)
	 {
	  if(!(this.n_job.get_status().equals("in progress")) )
		{
		 break;
		}
	  else if((this.n_job.get_stored_moved() == 0) &&
			  (this.n_job.downloadIsComplete()))
		{
		 this.n_job.set_status("completed");
		 break;
		}
	  else
		{
		 this.check_restackQueue();
		 BasicManager.sleepForDesiredTime(BasicManager.completionCheckSleepTime);
		}
	 }
} catch (Exception e) {
         e.printStackTrace();
         log_writer.doLogging_QRmgr(logRoot, logName, "<conductJobExecution>.checkForCompletion() error 2: "+e);
}
}




private void check_currentStatus(){

try {
	 if(this.n_job.get_status().equals("completed"))
	   {
		nHawkStarter.do_dbUpdate(this.n_job,2, null, this.n_job.get_status(), logRoot, logName);
		nHawkStarter.do_dbUpdate(this.n_job,1, "0", null, logRoot, logName);
	   }
	 else if(this.n_job.get_status().equals("Not found"))
	   {
	    nHawkStarter.do_dbUpdate(this.n_job,2, null, this.n_job.get_status(), logRoot, logName);
	    nHawkStarter.do_dbUpdate(this.n_job,1, "0", null, logRoot, logName);
	   }
	 else if(this.n_job.get_status().indexOf("fail") >= 0)
	   {
		nHawkStarter.do_dbUpdate(this.n_job,2, null, this.n_job.get_status(), logRoot, logName);
		nHawkStarter.do_dbUpdate(this.n_job,1, "0", null, logRoot, logName);
	   }
	 else if(this.n_job.get_status().equals("paused"))
	   {
		if(common.manager.BasicManager.check_3(this.n_job, logRoot, logName))
		  {
		   nHawkStarter.storeInQueue_currentlyRunningJob(this.n_job);
	      }
	    else
	      {
		   nHawkStarter.storeInDownloaderQueue_pendingJob(this.n_job);
		  }
	   }
     else if(this.n_job.get_status().equals("stopped"))
	   {
		if(common.manager.BasicManager.check_3(this.n_job, logRoot, logName))
		  {
		   nHawkStarter.storeInQueue_currentlyRunningJob(this.n_job);
		  }
		else
		  {
		   nHawkStarter.storeInDownloaderQueue_pendingJob(this.n_job);
		  }
	   }
	 else if(this.n_job.get_status().equals("cancelled"))
	   {
		//Log info?
	   }
	 else if(this.n_job.get_status().equals("relegated"))
	   {
	    nHawkStarter.findAndStop(this.n_job);
	    nHawkStarter.do_dbUpdate(this.n_job,2, null, "<html><i><b>relegated</b></i></html>", logRoot, logName);
	   }
	 else
	   {
	    nHawkStarter.do_dbUpdate(this.n_job,2, null, this.n_job.get_status(), logRoot, logName);
	   }

     //======================================================
	 //is it an NH job?
	 if((this.n_job.get_status().equals("completed"))&&
	   ( this.n_job.getJobType() == 2))
	   {
        boolean fileDeleted = utility.general.genericJobs.deleteDir(new File(this.n_job.get_actualFileStore()));
        if(!fileDeleted)
          {
		   log_writer.doLogging_QRmgr(logRoot, logName, "<conductJobExecution>.check_currentStatus() error: FAILED TO DELETE DIR "+this.n_job.get_actualFileStore());
		  }
	   }
	 //======================================================


} catch (Exception e) {
         e.printStackTrace();
         log_writer.doLogging_QRmgr(logRoot, logName, "<conductJobExecution>.check_currentStatus() error: "+e);
}
}




private boolean matches(BasicJob njob, DcmSnd snd){

boolean found = false;

try {
     String[] data = njob.get_dcmsndMainStats();
     String[] data_2 = snd.get_mainStats();

	 if((data[0].equals(data_2[0]))&&
	   ( data[1].equals(data_2[1]))&&
	   ( data[2].equals(data_2[2])))
	   {
	    found = true;
	   }

} catch (Exception e) {
         found = false;
         e.printStackTrace();
         log_writer.doLogging_QRmgr(logRoot, logName, "<conductJobExecution>.doComparison() error: "+e);
}
return found;
}



public boolean check_restackQueue(){

boolean continueOp = true;
try {
	  Object[] Data = (Object[]) this.n_job.takeFromPendingQueue();
	  if(Data == null)
		{}
	  else
		{
		 continueOp = false;
		 String storedFile = (String) Data[0];

		 String cuid = new dicomFileParser().get_cuid(new File(storedFile), logRoot, logName);
		 if(cuid == null)
		   {
			cuid = this.n_job.get_first_known_cuid();
		   }

		 if(this.n_job.get_transferMode().equals("dcmsnd"))
		   {
			 DcmSnd snd = get_dcmsnd(cuid, this.get_executingJob());
			 if(snd == null)
			   {
				if(this.n_job.get_transferMode().equals("dcmsnd"))
				  {
				   if(this.n_job.get_status().equals("in progress"))
					 {
					  log_writer.doLogging_QRmgr(logRoot, logName,
					  "conductJobExecution<"+this.n_job.get_jobId()+">.check_restackQueue_2() msg: Failed to get appropriate DcmSnd for failed queue file: cuid:"+cuid+", filename: "+storedFile);
					  this.n_job.set_status("failed <code:004>");
					 }
				   else
					 {
					  this.n_job.storeInPendingQueue(Data);
					 }
				  }
				else
				  {
				   this.findAndStoreInHttpsController(this.n_job, Data);
				  }
			   }
			 else
			   {
				snd.putInStore(Data);
			   }
		   }
		 else
		   {
			this.findAndStoreInHttpsController(this.n_job, Data);
		   }
		}
} catch (Exception e) {
         e.printStackTrace();
         log_writer.doLogging_QRmgr(logRoot, logName, "<conductJobExecution>.check_restackQueue() error: "+e);
}
return continueOp;
}



private void findAndStoreInHttpsController(BasicJob n_job, Object[] dataToStore){

try {
	https_clientManager httpsContrlr = nHawkStarter.get_httpsCtrller(n_job.get_httpsMainStats());
	if(httpsContrlr == null)
	  {
	   log_writer.doLogging_QRmgr(logRoot, logName, "conductJobExecution <night hawk: "+n_job.get_jobId()+">.findAndStoreInHttpsController() msg: could not get https job controller");
	   //n_job.set_status("failed_code:003");

	   String[] reAssignData = new String[2];
	   reAssignData[0] = "wasHttps";
	   reAssignData[1] = (String) dataToStore[0];
	   n_job.storeInFailedQueue(reAssignData);
	  }
	else
	  {
	   httpsContrlr.storeInQueue(dataToStore);
	  }
} catch (Exception e) {
         e.printStackTrace();
         log_writer.doLogging_QRmgr(logRoot, logName, "<conductJobExecution>.findAndStoreInHttpsController() error: "+e);
         n_job.set_status("failed <code:005>");
}
}



//TODO: Sort this method out later....Proves problematic if sub-folders exist.
//App has been manually set to ensure it doesnt create sub-folders but rather
//stick them all into one folder.
private String moveFiles(String source,
                         String destination,
                         String newName) {

File dir = new File(source);
try {
	 File[] files = dir.listFiles();
	 if(files != null)
	   {
		for(File f : files)
		   {
			if(f.isDirectory())
			  {
			   newName = this.moveFiles(source,destination,newName);  //TODO: sort this out later.
			  }
			else
			  {
               newName = destination+"/"+DcmRcv.get_uniqueFileName()+".DCM";
               newName = newName.replaceAll("\\\\","/");
               newName = newName.replaceAll("//","/");
               boolean done = f.renameTo(new File(newName));
               if(!done)
                 {
				  //log_writer.doLogging_QRmgr(logRoot, logName, "conductJobExecution <night hawk: "+this.nHawkJob.get_jobId()+">.moveFiles(): Failed to move file "+f.getName()+", "+newName);
				  newName = "-100";
				 }
			   else
			     {
				  break;
				 }
			  }
		   }
	   }
} catch (Exception e) {
	     log_writer.doLogging_QRmgr(logRoot, logName, "conductJobExecution <night hawk: "+this.n_job.get_jobId()+">.moveFiles(): error: "+e);
		 e.printStackTrace();
		 newName = "-200";
}
return newName;
}
}


}