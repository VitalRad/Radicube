/***********************************************

 Author:  Mike Bass
 Year:    2012
 Purpose: Starts and manages Night hawk module.

 ***********************************************/

 package night_hawk;


 import java.io.FileInputStream;
 import java.util.Properties;
 import java.util.ArrayList;
 import java.io.File;


 import org.dcm4che2.tool.dcmrcv.DcmRcv;
 import org.dcm4che2.tool.dcmsnd.DcmSnd;
 import org.dcm4che2.net.log_writer;
 import common.manager.BasicManager;
 import common.job.BasicJob;

 import utility.general.genericJobs;


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
      this.outstandingFiles = prop.getProperty("outstandingFiles");

      this.out_aet = prop.getProperty("out_aet");
      this.out_ip = prop.getProperty("out_ip");
      this.outPacsAet = prop.getProperty("outPacsAet");
      this.outPacsIp = prop.getProperty("outPacsIp");
      this.outPacsPort = Integer.parseInt(prop.getProperty("outPacsPort"));

      this.crashHandlerCmd = prop.getProperty("crashHandlerCmd");


      if((this.receivedFiles == null)||
        ( this.processedFiles == null)||
        ( this.TEL_MSG == null)||
        ( this.maxWaitDuration < 0)||
        ( this.outstandingFiles == null))
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



		 //=====================================================================
		  utility.general.cmdlineRunner.runCmd(this.logRoot, this.logName, this.crashHandlerCmd, true);
		 //=====================================================================

		 /*
		 //================================
		 //Clear interface (just in case..)
         utility.general.cmdlineRunner.runCmd(this.logRoot, this.logName, this.crashHandlerCmd, true);

		 // Check for outstanding files..
         if(((new File(this.outstandingFiles).listFiles().length)) > 0)
		   {
			utility.general.genericJobs.deleteFilesInDir(new File(this.outstandingFiles));
		   }

		 utility.general.genericJobs.copyAll(new File(this.processedFiles),
		                                     new File(this.outstandingFiles),
		                                     this.logRoot,
                                             this.logName);

		 utility.general.genericJobs.copyAll(new File(this.receivedFiles),
										     new File(this.outstandingFiles),
										     this.logRoot,
		                                     this.logName);

		 if(((new File(this.outstandingFiles).listFiles().length)) > 0)
		   {
		    //start independent dcmsnd..
		    new Thread(new start_independentDcmsnd()).start();
		   }
		   */


         //==========================
         //==========================
         //this.set_managerType(2);
         //==========================
         //==========================

         //Clear interface (just in case..)

         //get all leftovers...
         ArrayList<String> leftOvers_1 =  genericJobs.retrieveDirWithData(new File(this.processedFiles),
		                            				                      new ArrayList<String>(5),
		                            				                      null,
		                            				                      this.logRoot,
                           				                                  this.logName);


         ArrayList<String> leftOvers_2 =  genericJobs.retrieveDirWithData(new File(this.receivedFiles),
		                            				                      new ArrayList<String>(5),
		                            				                      null,
		                            				                      this.logRoot,
                           				                                  this.logName);
         this.doAmissionHere = true;
		 super.start_jobController();
		 super.start_httpsManager();
		 this.start_webServer();

		 //=========================================================
		 //Retrieve/record Pid...
		 //BasicManager.doNetstatPidFind(this.logRoot, this.logName);
		 //=========================================================

		 this.start_dcmServer(server_arg,statsForServer);
		 super.start_execMgr();
         super.start_tempConnectionChecker();
         super.start_jobAnalysisMgr();

         fis.close();
         fis= null;

		 ArrayList<ArrayList> allLocations = new  ArrayList<ArrayList>(2);
		 allLocations.add(leftOvers_1);
		 allLocations.add(leftOvers_2);

         new Thread(new leftOverHandler_2(allLocations,
		                                  this.callingAET,
                                          this.sourceServerPort)).start();

 } catch (Exception e){
          e.printStackTrace();
          log_writer.doLogging_QRmgr(logRoot, logName, "nightHawkManager.commenceOp() error: "+e);
          System.exit(0);
 }
 }


 public DcmRcv get_dcmRcv()
 {
  return this.dcmrcv;
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


 public String get_TEL_MSG()
 {
  return this.TEL_MSG;
 }

 @Override
 public void storeInDownloaderQueue_pendingJob(BasicJob job){}


 @Override
 public boolean cancelOp(BasicJob nJob, String opType){return false;}


 @Override
 public void storeForDemographicsRetrieval(Object[] queryDetails){}

 @Override
 public  void storeInDemographcsQueue(String[] data){}



 // Pushes outstanding files to desired PACS..
 class start_independentDcmsnd implements Runnable
 {
  public void run()
  {
   try
   {
    String[] args = new String[3];
    //args[0] = "-L DCMRCV@localhost";
    //args[1] = "KPSERVER@localhost:140";

    args[0] = "-L "+out_aet+"@"+out_ip;
    args[1] = outPacsAet+"@"+outPacsIp+":"+outPacsPort;
    args[2] = outstandingFiles;

    DcmSnd dcmsnd = new DcmSnd();
    dcmsnd.startOp(args, dcmsnd);

   } catch (Exception e)
   {
     e.printStackTrace();
     log_writer.doLogging_QRmgr(logRoot, logName, "nightHawkManager.start_independentDcmsnd() error: "+e);
   }
  }

 }



 // Pushes outstanding files to NightHawk server..
 class leftOverHandler_2 implements Runnable
 {
  String cAet = null;
  int sPort = -1;

  ArrayList<ArrayList> dataForTransf = null;

  public leftOverHandler_2(ArrayList<ArrayList> dataForTransf,
                           String cAet,
                           int sPort)
  {
   this.dataForTransf = dataForTransf;
   this.cAet = cAet;
   this.sPort = sPort;
  }

  public void run()
  {
   try
   {
    Object[] dForTransfer = this.dataForTransf.toArray();
    for(int a = 0; a < dForTransfer.length; a++)
       {
	    ArrayList thisData = (ArrayList) dForTransfer[a];
        Object[] loc_1 = thisData.toArray();
        for(int l1 = 0; l1 < loc_1.length; l1++)
	       {
	        String fileLocation = (String)loc_1[l1];
            String[] args = new String[3];
            args[0] = "-L "+this.cAet+"@localhost";
            args[1] = this.cAet+"@localhost:"+this.sPort;
            args[2] = fileLocation;

            DcmSnd dcmsnd = new DcmSnd();
            dcmsnd.setFileToDelete(fileLocation);
            dcmsnd.startOp(args, dcmsnd);
	       }
	   }

   } catch (Exception e)
   {
     e.printStackTrace();
     log_writer.doLogging_QRmgr(logRoot, logName, "nightHawkManager.leftOverHandler_2() error: "+e);
   }
  }

 }



 public static void main(String[] args){


 new nightHawkManager().commenceOp(args[0], args[1]);

 /*
 new nightHawkManager().commenceOp("C:\\mike\\codes\\ar_mod\\config\\nightHawk_properties.PROPERTIES",
                                   "C:\\mike\\codes\\ar_mod\\config\\dicom_sopClass_transferSyntax.PROPERTIES");
                                   */

 }


 }