/***********************************************

 Author:  Mike Bass
 Year:    2012
 Purpose: Starts and manages Night hawk module.

 ***********************************************/

 package night_hawk;


 import java.io.FileInputStream;
 import java.util.Properties;


 import org.dcm4che2.tool.dcmrcv.DcmRcv;
 import org.dcm4che2.net.log_writer;
 import common.manager.BasicManager;
 import common.job.BasicJob;


 public class nightHawkManager extends BasicManager{


 private DcmRcv dcmrcv = null;
 private String receivedFiles = null;
 private String processedFiles = null;
 private nightHawkWebServer nHawkServer = null;
 private String TEL_MSG = null;
 private int maxWaitDuration = -1;



 public nightHawkManager(){

 super();
 }


 public void commenceOp(String propertiesFile, String sc_ts_properties){

 try {
      super.startOp(propertiesFile, sc_ts_properties);

      Properties prop = new Properties();
 	  FileInputStream fis = new FileInputStream(propertiesFile);
	  prop.load(fis);

      this.receivedFiles = prop.getProperty("receivedFiles");
      this.processedFiles = prop.getProperty("processedFiles");

      this.TEL_MSG = prop.getProperty("TEL_MSG");
      this.maxWaitDuration = Integer.parseInt(prop.getProperty("maxWaitDuration"));


      if((this.receivedFiles == null)||
        ( this.processedFiles == null)||
        ( this.TEL_MSG == null)||
        ( this.maxWaitDuration < 0))
        {
         System.out.println("nightHawkManager.commenceOp() msg: Invalid values from properties file");
         log_writer.doLogging_QRmgr(logRoot, logName, "nightHawkManager.commenceOp() msg: Invalid values from properties file");
         System.exit(0);
		}


		 Object[][] rArgs = new Object[1][2];
		 rArgs[0][0] = (Object) rStrings;
		 rArgs[0][1] = (Object) rStrings_2;


		 String[] server_arg = new String[4];
		 server_arg[0] = this.callingAET+":"+this.sourceServerPort;
		 server_arg[1] = "-dest";
		 server_arg[2] = this.receivedFiles;
		 server_arg[3] = "teleradhack";

		 Object[] statsForServer = new Object[15];
		 statsForServer[0] = (Object) this;
		 statsForServer[1] = (Object) this.receivedFiles;
		 statsForServer[2] = (Object) this.logRoot;
		 statsForServer[3] = (Object) this.logName;
		 statsForServer[4] = (Object) this.processedFiles;
		 statsForServer[5] = (Object) Integer.toString(this.dataHandler_snoozing);
		 statsForServer[6] = (Object) Integer.toString(this.dataHandler_cannotMove);
		 statsForServer[7] = (Object) this.file_extracts;
		 statsForServer[8] = (Object) this.extract_fileName;
		 statsForServer[9] = (Object) this.nightHawkTags;
		 statsForServer[10] = (Object) rArgs;
		 statsForServer[11] = (Object) Integer.toString(this.demograhphicRetrieval_maxAttempts);
		 statsForServer[12] = (Object) Integer.toString(this.demograhphicRetrieval_pauseInbetweenAttempts);
		 statsForServer[13] = (Object) Integer.toString(this.failedMoveQueueSize);
		 statsForServer[14] = (Object) Integer.toString(this.pendingQueueSize);


         this.set_managerType(2);
         this.doAmissionHere = true;
		 super.start_jobController();
		 super.start_httpsManager();
		 this.start_webServer();
		 this.start_dcmServer(server_arg,statsForServer);
		 super.start_execMgr();
         super.start_tempConnectionChecker();
         super.start_jobAnalysisMgr();

         fis.close();
         fis= null;

 } catch (Exception e){
          e.printStackTrace();
          log_writer.doLogging_QRmgr(logRoot, logName, "nightHawkManager.commenceOp() error: "+e);
          System.exit(0);
 }
 }



public void start_webServer(){

try {
	 this.nHawkServer = new nightHawkWebServer(this.webServerIp,
								               this.webServerPort,
								               this.logRoot,
								               this.logName,
								               this.webServerBackLog,
								               this.nHawkJobController,
								               this);
	 new Thread(this.nHawkServer).start();

} catch (Exception e){
         e.printStackTrace();
         log_writer.doLogging_QRmgr(logRoot, logName, "nightHawkManager.start_webServer() error: "+e);
         System.exit(0);
}
}



 public void start_dcmServer(String[] server_arg,
                             Object[] statsForServer){
 try {
	 this.dcmrcv = new DcmRcv();
	 this.dcmrcv.set_stats(statsForServer);
	 //====================================
	 this.dcmrcv.set_TEL_MSG(this.TEL_MSG);
	 this.dcmrcv.set_maxWaitDuration(this.maxWaitDuration);
	 //====================================
	 this.dcmrcv.startOp(server_arg, this.dcmrcv);

 } catch (Exception e){
          e.printStackTrace();
          log_writer.doLogging_QRmgr(logRoot, logName, "nightHawkManager.start_dcmServer() error: "+e);
          System.exit(0);
 }
 }



 public String[] get_referralCardTags(){return null;}





 public void handleRelegatedTask(BasicJob nJob, String jobId, String opType, String others){

 try {
	   if(opType.equals("preemptRequest"))
		 {
		  boolean preempted = this.nHawkJobController.attemptPreempt(nJob);
		  if(!preempted)
			{
			 this.storeInQueue_relegatedJob(nJob);
			}
		 }
	   else if(opType.equals("changeDestination"))
		 {
		  this.changeDestination(nJob, others);
		  this.storeInQueue_relegatedJob(nJob);
		 }
	   else if(opType.equals("START_IMMEDIATELY"))
		 {
		  this.nHawkJobController.startThisJob(nJob);
		 }
	   else
		 {
		  this.do_jobTask(nJob, opType);
		 }
 } catch (Exception e){
           e.printStackTrace();
           log_writer.doLogging_QRmgr(logRoot, logName, "nightHawkManager.handleRelegatedTask() error: "+e);
 }
 }




 public int get_downloaderQueueSize(){

 return 0;
 }


 public int get_downloaderPendingQueueSize(){

 return 0;
 }


 @Override
 public void storeInDownloaderQueue_pendingJob(BasicJob job){}


 @Override
 public boolean cancelOp(BasicJob nJob, String opType){return false;}


 @Override
 public void storeForDemographicsRetrieval(Object[] queryDetails){}

 @Override
 public  void storeInDemographcsQueue(String[] data){}



 public static void main(String[] args){


// new nightHawkManager().commenceOp(args[0], args[1]);

 
 new nightHawkManager().commenceOp("D:\\Sushant\\radicube\\radicube\\radicube config\\gwc_config\\config\\nightHawk_properties.PROPERTIES",
                                   "D:\\Sushant\\radicube\\radicube\\radicube config\\gwc_config\\config\\dicom_sopClass_transferSyntax.PROPERTIES");
                                   

 }


 }