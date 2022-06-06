/*********************************************************************
 *
 *   Author:  Mike Bassey
 *   Year:    2012
 *   Purpose: Retrieves demographics for a job from a scan file.
 *
 *********************************************************************/

  package QR;


 import java.io.File;

 import org.dcm4che2.net.log_writer;
 import common.manager.BasicManager;
 import utility.general.dicomObjectTagExtractor;
 import common.job.BasicJob;



 public class jobDemoRetriever implements Runnable {


 private BasicJob nJob  = null;
 private String logRoot = null;
 private String logName = null;
 private String[] retrievedDemographics = null;
 private BasicManager bMgr = null;
 private dicomObjectTagExtractor dcmTagExtrctr = null;


 public jobDemoRetriever(BasicJob nJob,
                         String dcmFileLocation,
                         String xmlFileLocation,
                         String logRoot,
                         String logName,
                         int maxAttempts,
                         String[] desiredDemographics,
                         int breakInbetweenSearch,
                         BasicManager bMgr){

 this.nJob = nJob;
 this.logRoot = logRoot;
 this.logName = logName;
 this.bMgr = bMgr;



 this.dcmTagExtrctr = new dicomObjectTagExtractor(dcmFileLocation,
                         						  xmlFileLocation,
                         						  logRoot,
                         						  logName,
                         						  maxAttempts,
                        						  desiredDemographics,
                        						  breakInbetweenSearch,
                         						  this.bMgr.notWantedTagValues,
                         						  1);
 }


 public void run(){

 this.dcmTagExtrctr.commence_tagRetrieval_1();
 this.retrievedDemographics = this.dcmTagExtrctr.getRetrievedTags();
 if(this.retrievedDemographics == null)
   {
    log_writer.doLogging_QRmgr(this.logRoot, this.logName,
    "<jobDemoRetriever>.run() msg: failed to retrieve demographics for night hawk job: "+this.nJob.get_jobId());
   }
 else
   {
    this.nJob.set_jobDemographics(this.retrievedDemographics);
   }
 }
}