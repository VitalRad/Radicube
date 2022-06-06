/*********************************************************************
 *
 *   Author:  Mike Bassey
 *   Year:    2012
 *   Purpose: Tasked with retrieving demographics
 *            for a particular job. A job cannot be stored in the job
 *            queue until its demographics have been retrieved.
 *
 *********************************************************************/

 package night_hawk;


 import java.io.File;

 import org.dcm4che2.net.Association;
 import org.dcm4che2.net.log_writer;
 import org.dcm4che2.tool.dcmrcv.DcmRcv;
 import utility.db_manipulation.insertNightHawk;
 import common.manager.BasicManager;
 import utility.general.dicomObjectTagExtractor;


 public class jobDemographicsRetriever {



 private nightHawkJob nJob = null;
 private String logRoot = null;
 private String logName = null;
 private String[] retrievedDemographics = null;
 private BasicManager nHawkStarter = null;
 private dicomObjectTagExtractor dcmTagExtrctr = null;
 private Association assoc = null;
 private String dcmFileLocation = null;
 private String JOB_ID = null;


 public jobDemographicsRetriever(nightHawkJob nJob,
                                 String dcmFileLocation,
                                 String xmlFileLocation,
                                 String logRoot,
                                 String logName,
                                 int maxAttempts,
                                 String[] desiredDemographics,
                                 int breakInbetweenSearch,
                                 BasicManager nHawkStarter){

 this.nJob = nJob;
 this.logRoot = logRoot;
 this.logName = logName;
 this.nHawkStarter = nHawkStarter;
 this.dcmFileLocation = dcmFileLocation;

 this.dcmTagExtrctr = new dicomObjectTagExtractor(dcmFileLocation,
                         						  xmlFileLocation,
                         						  logRoot,
                         						  logName,
                         						  maxAttempts,
                        						  desiredDemographics,
                        						  breakInbetweenSearch,
                         						  this.nHawkStarter.notWantedTagValues,
                         						  1);
 }



 public jobDemographicsRetriever(Association assoc,
	                             String dcmFileLocation,
                                 String xmlFileLocation,
                                 String logRoot,
                                 String logName,
                                 int maxAttempts,
                                 String[] desiredDemographics,
                                 int breakInbetweenSearch,
                                 BasicManager nHawkStarter){

 this.assoc = assoc;
 this.logRoot = logRoot;
 this.logName = logName;
 this.nHawkStarter = nHawkStarter;
 this.dcmFileLocation = dcmFileLocation;

 this.dcmTagExtrctr = new dicomObjectTagExtractor(dcmFileLocation,
                         						  xmlFileLocation,
                         						  logRoot,
                         						  logName,
                         						  maxAttempts,
                        						  desiredDemographics,
                        						  breakInbetweenSearch,
                         						  this.nHawkStarter.notWantedTagValues,
                         						  1);
 }



 public jobDemographicsRetriever(Association assoc,
	                             String dcmFileLocation,
                                 String xmlFileLocation,
                                 String logRoot,
                                 String logName,
                                 int maxAttempts,
                                 String[] desiredDemographics,
                                 int breakInbetweenSearch,
                                 BasicManager nHawkStarter,
                                 String JOB_ID){

 this.assoc = assoc;
 this.logRoot = logRoot;
 this.logName = logName;
 this.nHawkStarter = nHawkStarter;
 this.dcmFileLocation = dcmFileLocation;
 this.JOB_ID = JOB_ID;

 this.dcmTagExtrctr = new dicomObjectTagExtractor(dcmFileLocation,
                         						  xmlFileLocation,
                         						  logRoot,
                         						  logName,
                         						  maxAttempts,
                        						  desiredDemographics,
                        						  breakInbetweenSearch,
                         						  this.nHawkStarter.notWantedTagValues,
                         						  1);
 }

 public void run(){

 try
 {

 this.dcmTagExtrctr.commence_tagRetrieval_1();
 this.retrievedDemographics = this.dcmTagExtrctr.getRetrievedTags();

 if(this.retrievedDemographics == null)
   {
    log_writer.doLogging_QRmgr(this.logRoot, this.logName,
    "<jobDemographicsRetriever>.run() msg: failed to retrieve demographics for files at: "+this.dcmFileLocation);

    this.runWithJobID();
   }
 else
   {
	//==================================================
    if(this.nJob != null)
      {
       this.writeToDB();
       this.nJob.set_jobDemographics(this.retrievedDemographics); //switch this line with next?
	   this.nHawkStarter.storeInQueue_arrivingJob(this.nJob);
      }
    else
      {
       String[] jobDetails = new String[8];
	   jobDetails[0] = this.JOB_ID;
	   jobDetails[1] = this.retrievedDemographics[0];
	   jobDetails[2] = this.retrievedDemographics[1];
	   jobDetails[3] = this.retrievedDemographics[5];
	   jobDetails[4] = this.retrievedDemographics[6];
	   jobDetails[5] = this.retrievedDemographics[4];
	   jobDetails[6] = this.retrievedDemographics[7];
	   jobDetails[7] = "to be assigned";

	   DcmRcv dcmObj = this.nHawkStarter.get_dcmRcv();
	   if(dcmObj == null)
	     {
	      log_writer.doLogging_QRmgr(this.logRoot, this.logName,
          "<jobDemographicsRetriever>.run() msg: Null Dcmrcv Object. SHOULD NEVER HAPPEN!");
	     }
	   else
	     {
		  dcmObj.checkAndCreate_NHjob(jobDetails, this.assoc, this.dcmFileLocation);
		 }
	  }
	  //==================================================

   }

  } catch (Exception ef) {
		   ef.printStackTrace();
		   log_writer.doLogging_QRmgr(logRoot, logName, "<jobDemographicsRetriever.run()> exception: "+ef);
  }

 }



 private void runWithJobID()
 {
  try
  {
	if(this.nJob != null)
	  {
	   this.writeToDB();
	   this.nJob.set_jobDemographics(this.retrievedDemographics); //switch this line with next?
	   this.nHawkStarter.storeInQueue_arrivingJob(this.nJob);
	  }
	else
	  {
	   String[] jobDetails = new String[8];
	   jobDetails[0] = this.JOB_ID;
	   jobDetails[1] = ".";
	   jobDetails[2] = ".";
	   jobDetails[3] = ".";
	   jobDetails[4] = ".";
	   jobDetails[5] = ".";
	   jobDetails[6] = ".";
	   jobDetails[7] = ".";

	   DcmRcv dcmObj = this.nHawkStarter.get_dcmRcv();
	   if(dcmObj == null)
		 {
		  log_writer.doLogging_QRmgr(this.logRoot, this.logName,
		  "<jobDemographicsRetriever>.run() msg: Null Dcmrcv Object. SHOULD NEVER HAPPEN!");
		 }
	   else
		 {
		  dcmObj.checkAndCreate_NHjob(jobDetails, this.assoc, this.dcmFileLocation);
		 }
	  }

	} catch (Exception ef) {
			 ef.printStackTrace();
			 log_writer.doLogging_QRmgr(logRoot, logName, "<jobDemographicsRetriever.runWithJobID()> exception: "+ef);
	}
 }


public void writeToDB(){
try {
	 String jobKey = this.nJob.get_jobId();
	 String[] db_args = new String[(this.retrievedDemographics.length) + 6];
	 db_args[0] = jobKey;

	 String destValue = null;
	 String dest = this.nJob.get_transferMode();
	 if(dest.equalsIgnoreCase("dcmsnd"))
	   {
	 	String[] data =  this.nJob.get_dcmsndMainStats();
	 	destValue = this.nHawkStarter.getDestination(data[1], data[2]);
	   }
	 else if(dest.equalsIgnoreCase("https"))
	   {
	    String[] data =  this.nJob.get_httpsMainStats();
	    destValue = this.nHawkStarter.getDestination(data[0], data[1]);
	   }

     this.nJob.set_status("queued");

	 db_args[db_args.length -5] = this.nJob.get_status();
	 db_args[db_args.length -4] = "<html><i>downloaded</i></html>";
	 db_args[db_args.length -3] = Integer.toString(this.nJob.get_imagesUploaded());
	 db_args[db_args.length -2] = "1";
	 db_args[db_args.length -1] = "0";

	 for(int a = 0; a < this.retrievedDemographics.length; a++)
	    {
		 db_args[a + 1] = this.retrievedDemographics[a];
	    }

     Object[] args = new Object[4];
     Integer opType = new Integer(4);

     args[0] = opType;
	 args[1] = this.nHawkStarter;
	 args[2] = db_args;
	 args[3] = destValue;
	 this.nHawkStarter.get_dbUpdateMgr().storeData(args);

	 this.nJob.insertDataIntoJobHistoryTable();
	 this.nJob.start_jobHistoryUpdates();

 } catch (Exception e){
          e.printStackTrace();
          log_writer.doLogging_QRmgr(this.logRoot, this.logName,
          "jobDemographicsRetriever.writeToDB(), night hawk job<"+this.nJob.get_jobId()+">  error: "+e);
 }
}

}