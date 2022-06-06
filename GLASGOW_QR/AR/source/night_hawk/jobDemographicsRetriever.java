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

 import org.dcm4che2.net.log_writer;
 import utility.db_manipulation.insertNightHawk;
 import common.manager.BasicManager;
 import utility.general.dicomObjectTagExtractor;



 public class jobDemographicsRetriever implements Runnable {


 private nightHawkJob nJob = null;
 private String logRoot = null;
 private String logName = null;
 private String[] retrievedDemographics = null;
 private BasicManager nHawkStarter = null;
 private dicomObjectTagExtractor dcmTagExtrctr = null;


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

 this.dcmTagExtrctr.commence_tagRetrieval_1();
 this.retrievedDemographics = this.dcmTagExtrctr.getRetrievedTags();
 if(this.retrievedDemographics == null)
   {
    log_writer.doLogging_QRmgr(this.logRoot, this.logName,
    "<jobDemographicsRetriever>.run() msg: failed to retrieve demographics for night hawk job: "+this.nJob.get_jobId());
   }
 else
   {
    this.writeToDB();
    this.nJob.set_jobDemographics(this.retrievedDemographics);
	this.nHawkStarter.storeInQueue_arrivingJob(this.nJob);
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

	//new insertNightHawk(this.logRoot, this.logName).startOp(db_args, destValue);


	 /*

	 Integer thisInt = (Integer) data[0];
	 BasicManager mgr = (BasicManager) data[1];
	 String[] args = (String[]) data[2];
	 String dest = (String) data[3];
     new insertNightHawk(mgr, logRoot, logName).startOp(args, dest);

     */


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