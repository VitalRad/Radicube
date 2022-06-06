/********************************************
 *
 *   Author:  Mike Bassey
 *   Year:    2012
 *   Purpose: Encapsulates a night hawk job.
 *
 ********************************************/

 package night_hawk;


 import java.util.concurrent.ArrayBlockingQueue;


 import org.dcm4che2.net.Association;
 import org.dcm4che2.net.log_writer;
 import org.dcm4che2.tool.dcmrcv.dcm_folderCreator;
 import common.manager.BasicManager;
 import common.job.BasicJob;



 public class nightHawkJob extends BasicJob {



 public nightHawkJob(BasicManager bmgr,
	                 String job_id,
                     String fileLocation,
                     String first_known_cuid,
                     Association assoc,
                     String logRoot,
                     String logName,
                     String fileStore,
                     int failedMoveQueueSize,
                     int pendingQueueSize){
 super(bmgr,
	   job_id,
       fileLocation,
       first_known_cuid,
       assoc,
       logRoot,
       logName,
       fileStore,
       failedMoveQueueSize,
       pendingQueueSize);

 this.set_essentials(true);
 }



  @Override
 public void updateTable_uploadCount(){
 try {
      if(this.lastSentValue != this.noOfImagesUploadedThusFar)
        {
         this.lastSentValue = this.noOfImagesUploadedThusFar;
         this.bMgr.do_dbUpdate(this,
                               3,
                               null,
                               Integer.toString(this.noOfImagesUploadedThusFar),
                               this.logRoot,
                               this.logName);
         this.doUpdateJobHistoryTable();
	    }
  } catch (Exception e){
           e.printStackTrace();
           log_writer.doLogging_QRmgr(this.logRoot, this.logName,
           "nightHawkJob.updateTable_uploadCount() error: "+e);
 }
 }



 @Override
 public boolean isRelegated(){

 return false;
 }

 @Override
 public boolean referralCardRetrieved(){return true;}


 @Override
 public synchronized void set_downloadStatus(boolean value){}


 @Override
 public synchronized void set_downloadResult(String value){}



 @Override
 public synchronized boolean downloadIsComplete(){

 return true;
 }


 @Override
 public synchronized String get_downloadResult(){

 return "successful";
 }



 public void set_essentials(boolean value){

 super.set_essentials(value);
 }

}