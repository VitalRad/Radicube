/**********************************************

 Author:  Mike Bass
 Year:    2012
 Purpose: Starts and manages QR module.

 **********************************************/

 package QR;


 import java.io.FileInputStream;
 import java.util.Properties;
 import java.util.concurrent.ArrayBlockingQueue;
 import java.util.concurrent.atomic.AtomicInteger;
 import java.util.Vector;
 import java.util.Calendar;
 import java.util.Timer;
 import java.util.TimerTask;


 import org.dcm4che2.net.log_writer;
 import common.manager.BasicManager;
 import common.job.BasicJob;
 import utility.general.folderCreator;
 import utility.db_manipulation.updateJobStatus;
 import org.dcm4che2.tool.dcmrcv.dcm_folderCreator;
 import utility.db_manipulation.doBulkUpdate;
 import utility.db_manipulation.insertBatch_nightHawkTable;
 import utility.db_manipulation.updateAsBatch;
 import utility.general.readProcessOutput;

 //--------------------------------------------
 //import utility.db_manipulation.doBulkUpdate_2;
 //--------------------------------------------





 public class QRmanager extends BasicManager {


 private String downloadedFiles = null;
 private String filesForTransfer = null;
 private QRjob_downloader[] downloaders = null;
 private String[][] downloaderStats = null;
 private String[] cModalities = null;
 private Object downloadQueueLock = new Object();
 private int arrivingJob_downloaderQueue = -1;
 private int pendingJob_downloaderQueue = -1;
 private int maxNoOfExecutingJobs = -1;
 private int maxNoOfDownloaders = -1;
 private QRjob_downloadMgr downloadMgr = null;
 private ArrayBlockingQueue<BasicJob> arrivingJobs_downloader = null;
 private ArrayBlockingQueue<BasicJob> pendingJobs_downloader = null;
 protected int downloaderCheckPauseTime = -1;
 private String cMoversTo = null;
 private QRwebServer QRwServer = null;
 private String endValue_cMoveString = null;
 private String[] cFindLocal = new String[3];
 private int no_of_pacs = -1;
 private String[][] PACS = null;
 protected String DB_serverName = null;
 protected int DB_port = 1;
 protected String DB_user = null;
 protected String DB_password = null;
 protected String DB_SID = null;
 protected String DB_staticURL = null;
 protected int DB_dbType = -1;
 protected String referralCardQuery = null;
 private String scanned_form_root = null;
 protected boolean referralCardsWanted = false;
 private String[] risDetails = null;
 private float imageQuality = -10F;
 private String[] cfgFileTags = null;
 private String[] referralCardTags = null;
 private demographicsAssigner dmAssigner = null;
 private ArrayBlockingQueue<String[]> demographicsRetrieverStats = null;
 private regulateDemographicsRetrieving d_regulator = null;

 private ArrayBlockingQueue<BasicJob> jobsAwaitingDemographics = null;
 private int awaitingDemographicsQueue = -1;

 private String TEL_MSG = null;






 public QRmanager(){

 super();
 }


 public void commenceOp(String propertiesFile, String sc_ts_properties){

 try {
      super.startOp(propertiesFile, sc_ts_properties);

      Properties prop = new Properties();
 	  FileInputStream fis = new FileInputStream(propertiesFile);
	  prop.load(fis);

      this.downloadedFiles = prop.getProperty("downloadedFiles");
      this.filesForTransfer = prop.getProperty("filesForTransfer");
      this.arrivingJob_downloaderQueue = Integer.parseInt(prop.getProperty("arrivingJob_downloaderQueue"));
      this.pendingJob_downloaderQueue = Integer.parseInt(prop.getProperty("pendingJob_downloaderQueue"));
	  this.maxNoOfExecutingJobs = Integer.parseInt(prop.getProperty("maxNoOfExecutingJobs"));
      this.maxNoOfDownloaders = Integer.parseInt(prop.getProperty("maxNoOfDownloaders"));
      this.downloaderCheckPauseTime = Integer.parseInt(prop.getProperty("downloaderCheckPauseTime"));
      this.cMoversTo = prop.getProperty("cMoversTo");
      this.endValue_cMoveString = prop.getProperty("endValue_cMoveString");
      this.cFindLocal[0] = prop.getProperty("cFindLocal_AET");
	  this.cFindLocal[1] = prop.getProperty("cFindLocal_ip");
      this.cFindLocal[2] = prop.getProperty("cFindLocal_port");
      int no_of_modalities = Integer.parseInt(prop.getProperty("no_of_modalities"));
      this.no_of_pacs = Integer.parseInt(prop.getProperty("no_of_pacs"));
	  this.DB_serverName = prop.getProperty("DB_serverName");
	  this.DB_port = Integer.parseInt(prop.getProperty("DB_port"));
	  this.DB_user = prop.getProperty("DB_user");
	  this.DB_password = prop.getProperty("DB_password");
	  this.DB_SID = prop.getProperty("DB_SID");
	  this.DB_staticURL = prop.getProperty("DB_staticURL");
	  this.DB_dbType = Integer.parseInt(prop.getProperty("DB_dbType"));
	  this.referralCardQuery = prop.getProperty("referralCardQuery");
	  this.scanned_form_root = prop.getProperty("scanned_form_root");
	  int rCardWanted = Integer.parseInt(prop.getProperty("referralCardsWanted"));
	  this.imageQuality = Float.valueOf(prop.getProperty("imageQuality"));
	  int noOfTags_cfg = Integer.parseInt(prop.getProperty("noOfTags_cfg"));
	  this.xmlFileHeader = prop.getProperty("xmlFileHeader");
      this.xmlFile_braceName = prop.getProperty("xmlFile_braceName");
      int demographcsSize = Integer.parseInt(prop.getProperty("demographicsRetrieverStats"));
      int demographicsUpdateFreq = Integer.parseInt(prop.getProperty("demographicsUpdateFreq"));
      this.diskMapCmd = prop.getProperty("diskMapCmd");
      this.diskMap_No_of_desiredMsg = Integer.parseInt(prop.getProperty("diskMap_No_of_desiredMsg"));

      this.awaitingDemographicsQueue = Integer.parseInt(prop.getProperty("awaitingDemographicsQueue"));

      this.TEL_MSG = prop.getProperty("TEL_MSG");


      if((this.downloadedFiles == null)||
        ( this.filesForTransfer == null)||
        ( this.arrivingJob_downloaderQueue <= 0)||
        ( this.pendingJob_downloaderQueue <= 0)||
        ( this.maxNoOfExecutingJobs <= 0)||
        ( this.maxNoOfDownloaders <= 0)||
        ( this.downloaderCheckPauseTime < 0)||
        ( this.cMoversTo == null)||
        ( this.endValue_cMoveString == null)||
        ( no_of_modalities <= 0)||
        ( this.no_of_pacs <= 0)||
        ( this.DB_serverName == null)||
        ( this.DB_port < 0)||
        ( this.DB_user == null)||
        ( this.DB_password == null)||
        ( this.DB_SID == null)||
        ( this.DB_staticURL == null)||
        ( this.DB_dbType < 0)||
        ( this.referralCardQuery == null)||
        ( this.scanned_form_root == null)||
        ( rCardWanted < 0)||
        ( noOfTags_cfg < 0)||
        ( this.xmlFileHeader == null)||
        ( this.xmlFile_braceName == null)||
        ( demographcsSize <= 0)||
        ( demographicsUpdateFreq < 0)||
        ( this.diskMap_No_of_desiredMsg < 0)||
        ( this.awaitingDemographicsQueue <= 0)||
        ( this.TEL_MSG == null))
        {
         System.out.println("QRmanager.commenceOp() msg: Invalid values from properties file");
         log_writer.doLogging_QRmgr(logRoot, logName, "QRmanager.commenceOp() msg: Invalid values from properties file");
         System.exit(0);
		}


	 //=========================================================================================
	 jobsAwaitingDemographics = new ArrayBlockingQueue<BasicJob>(this.awaitingDemographicsQueue);
	 //=========================================================================================


      //this.set_managerType(1);
	  this.demographicsRetrieverStats = new ArrayBlockingQueue<String[]>(demographcsSize);
	  this.diskMapMsgs = new String[this.diskMap_No_of_desiredMsg];

	  for(int k = 0; k < this.diskMapMsgs.length; k++)
	     {
		  this.diskMapMsgs[k] = prop.getProperty("diskMap_desiredMsg_"+k);
		 }

	  //-----------------------------------------------------
	  // this.doBulkObj2 = new doBulkUpdate_2(logRoot, logName);
	  //-----------------------------------------------------

	  this.d_regulator = new regulateDemographicsRetrieving();
      this.d_regulator.doTask(0, demographicsUpdateFreq); //start timer..

	  this.risDetails = new String[9];
	  this.risDetails[0] = this.DB_serverName;
	  this.risDetails[1] = Integer.toString(this.DB_port);
	  this.risDetails[2] = this.DB_user;
	  this.risDetails[3] = this.DB_password;
	  this.risDetails[4] = this.DB_SID;
	  this.risDetails[5] = this.DB_staticURL;
	  this.risDetails[6] = Integer.toString(this.DB_dbType);
	  this.risDetails[7] = this.referralCardQuery;
	  this.risDetails[8] = this.scanned_form_root;


	  this.cfgFileTags = new String[noOfTags_cfg + 1];
	  this.cfgFileTags[0] = prop.getProperty("cfg_startvalue");
	  for(int x = 1; x < this.cfgFileTags.length; x++)
	     {
		  this.cfgFileTags[x] = prop.getProperty("cfg_tag_"+(x-1));
		 }

      if(rCardWanted == 1)
        {
		 this.referralCardsWanted = true;
		 int noOfRcards = Integer.parseInt(prop.getProperty("referralCardTag_no_of_tags"));
		 this.referralCardTags = new String[noOfRcards];
		 for(int p = 0; p < this.referralCardTags.length; p++)
		    {
		     this.referralCardTags[p] = prop.getProperty("referralCardTag_"+p);
		    }
		 //------------------------------------------------------------------------------------------------
		 log_writer.doLogging_QRmgr(logRoot, logName, "QRmanager.commenceOp() msg: REFERRAL CARDS WANTED");
		 //------------------------------------------------------------------------------------------------
		}

	  this.cModalities = new String[no_of_modalities];
	  for(int a = 0; a < no_of_modalities; a++)
	     {
		  this.cModalities[a] = prop.getProperty("modality_"+a);
		 }


      this.PACS = new String[this.no_of_pacs][4];
      for(int a = 0; a < this.PACS.length; a++)
	  	 {
	  	  this.PACS[a][0] = prop.getProperty("pacs_"+a+"_alias");
	  	  this.PACS[a][1] = prop.getProperty("pacs_"+a+"_AET");
	  	  this.PACS[a][2] = prop.getProperty("pacs_"+a+"_ip");
	  	  this.PACS[a][3] = prop.getProperty("pacs_"+a+"_port");
		 }


      this.arrivingJobs_downloader = new ArrayBlockingQueue<BasicJob>(this.arrivingJob_downloaderQueue);
      this.pendingJobs_downloader = new ArrayBlockingQueue<BasicJob>(this.pendingJob_downloaderQueue);
      this.downloaders = new QRjob_downloader[this.maxNoOfDownloaders];
	  this.downloaderStats = new String[this.maxNoOfDownloaders][3];
	  for(int a = 0; a <  this.downloaderStats.length; a++)
	     {
		  this.downloaderStats[a][0] = prop.getProperty("downloader_"+a+"_AET");
		  this.downloaderStats[a][1] = prop.getProperty("downloader_"+a+"_ip");
		  this.downloaderStats[a][2] = prop.getProperty("downloader_"+a+"_port");
		 }


	  this.dmAssigner = new demographicsAssigner();
	  new Thread(this.dmAssigner).start();

      this.do_diskMapping();
	  this.initDownloader();
	  super.start_jobController();
	  super.start_httpsManager();
	  this.start_webServer();
	  super.start_execMgr();
	  this.startDownloadMgr();
	  super.start_clearanceMgrs();
      //------------------------------------
      //super.start_tempConnectionChecker();
      //------------------------------------
      super.start_jobAnalysisMgr();


      fis.close();
      fis = null;

 } catch (Exception e){
          e.printStackTrace();
          log_writer.doLogging_QRmgr(logRoot, logName, "QRmanager.commenceOp() error: "+e);
          System.exit(0);
 }
 }


public String get_TEL_MSG()
{
 return this.TEL_MSG;
}



public void do_diskMapping(){

try {
     if(!this.diskMapped)
       {
        Runtime rt = Runtime.getRuntime();
        Process p = rt.exec(this.diskMapCmd);
        readProcessOutput s1 = new readProcessOutput(this, this.diskMapMsgs, "input", p.getInputStream(), logRoot, logName);
        readProcessOutput s2 = new readProcessOutput(this, this.diskMapMsgs, "error", p.getErrorStream(), logRoot, logName);
        s1.start();
        s2.start();
        int termValue = p.waitFor();

        log_writer.doLogging_QRmgr(logRoot, logName, "QRmanager.do_diskMapping() msg: Disk map successful="+this.diskMapped+", termination value="+termValue);
       }
} catch (Exception e){
         e.printStackTrace();
         log_writer.doLogging_QRmgr(logRoot, logName, "QRmanager.do_diskMapping() error: "+e);
}
}




 @Override
 public void storeInDemographcsQueue(String[] data){

 synchronized(this.demographicsRetrieverStats){
 try {
      this.demographicsRetrieverStats.put(data);

        //-------------------------------------------------------
        //log_writer.doLogging_QRmgr(logRoot, logName, "QRmanager.storeInDemographcsQueue() msg: data saved "+this.demographicsRetrieverStats.size());
        //------------------------------------------------------

 } catch (Exception e){
           e.printStackTrace();
           log_writer.doLogging_QRmgr(logRoot, logName, "QRmanager.storeInDemographcsQueue() error: "+e);
 }
 }
 }



 private Object[] get_allDemographicsRequest(){

 Object[] data = null;
 try {
      synchronized(this.demographicsRetrieverStats)
       {
        if(this.demographicsRetrieverStats.size() > 0)
          {
		   data = this.demographicsRetrieverStats.toArray();
		   this.demographicsRetrieverStats.clear();
		  }
	   }

 } catch (Exception e){
          e.printStackTrace();
          log_writer.doLogging_QRmgr(logRoot, logName, "QRmanager.get_allDemographicsRequest() error: "+e);
 }
 return  data;
 }




 public void doBulkDemographicsUpdate(){

 //---------------------------------------------
 //synchronized(this.demographicsRetrieverStats){
 //---------------------------------------------

 try {
      //------------------------------------------------------------------------------------------------------
	  //log_writer.doLogging_QRmgr(logRoot, logName, "QRmanager.doBulkDemographicsUpdate() called! ");
      //------------------------------------------------------------------------------------------------------

      String sqlstr = "update autorouter_nighthawk set patientid = ?, patientname = ?, dob = ?,  sex = ?, accessionnumber = ?, studyname = ?, modality = ?, studydate = ?, studytime = ? where jobkey = ?";
      Object[] data = this.get_allDemographicsRequest();
      if(data != null)
        {
         Vector<String[]> newData = new Vector<String[]>(5);
         Vector<QRjob> jobObjects = new Vector<QRjob>(5);
         for(int a = 0; a < data.length; a++)
            {
			 String[] nData = (String[]) data[a];
             QRjob qj = this.getJobWithKey((String)nData[nData.length - 1]);
             if(qj == null)
               {
			    log_writer.doLogging_QRmgr(logRoot, logName, "QRmanager.doBulkDemographicsUpdate() null object >> "+((String)nData[nData.length - 1]));
			    System.out.println("QRmanager.doBulkDemographicsUpdate() null object >> "+((String)nData[nData.length - 1]));
			   }
			 else
			   {

				  String[] jobDemographics = new String[9];

				  jobDemographics[0] = nData[0];
				  jobDemographics[1] = nData[1];
				  jobDemographics[2] = nData[2];
				  jobDemographics[3] = nData[3];
				  jobDemographics[4] = nData[5];
				  jobDemographics[5] = nData[4];
				  jobDemographics[6] = nData[6];
				  jobDemographics[7] = nData[7];
				  jobDemographics[8] = nData[8];

				  qj.set_jobDemographics(jobDemographics);


			    jobObjects.add(qj);
			    newData.add(nData);
			   }
			}

       //Add those with demographics to system...
       Object[] dToInsert = new Object[jobObjects.size()];
       for(int c = 0; c < dToInsert.length; c++)
          {
           QRjob job = (QRjob) jobObjects.get(c);
           dToInsert[c] = job.get_insertionData();
	      }
       boolean errorOccurred = new insertBatch_nightHawkTable(this.logRoot, this.logName, dToInsert).doInsert();
       if(!errorOccurred)
         {
          for(int c = 0; c < jobObjects.size(); c++)
             {
              QRjob job = (QRjob) jobObjects.get(c);
              job.insertDataIntoJobHistoryTable();
              this.storeInDownloaderQueue_newJob(job);
	         }
	     }


	     new updateAsBatch(logRoot,
		                   logName,
		                   (newData.toArray()),
                           sqlstr).doUpdate();
	    }

 } catch (Exception e){
          e.printStackTrace();
          log_writer.doLogging_QRmgr(logRoot, logName, "QRmanager.doBulkDemographicsUpdate() error: "+e);
 }
 }



 public QRjob getJobWithKey(String jobKey)
 {
  QRjob qJob = null;
  try
  {
   Object[] aData = this.jobsAwaitingDemographics.toArray();
   for(int a = 0; a < aData.length; a++)
      {
       QRjob qj = (QRjob) aData[a];
       if(qj != null)
         {
		  String jid = qj.get_jobId();
		  if(jid.equals(jobKey))
		    {
			 qJob = qj;
			 boolean objRemoved = this.jobsAwaitingDemographics.remove(qj);
			 System.out.println("OBJECT REMOVED: "+objRemoved);
			 break;
			}
		 }
	  }
  } catch (Exception e){
           e.printStackTrace();
           log_writer.doLogging_QRmgr(logRoot, logName, "QRmanager.getJobWithKey() error: "+e);
  }
 return qJob;
 }



 public void start_webServer(){

 try {
	  this.QRwServer = new QRwebServer(this.webServerIp,
									   this.webServerPort,
									   this.logRoot,
									   this.logName,
									   this.webServerBackLog,
									   this.nHawkJobController,
									   this);
	  this.QRwServer.set_endValue_cMoveString(this.endValue_cMoveString);
	  new Thread(this.QRwServer).start();

 } catch (Exception e){
          e.printStackTrace();
          log_writer.doLogging_QRmgr(logRoot, logName, "QRmanager.start_webServer() error: "+e);
          System.exit(0);
 }
 }



public String[] get_referralCardTags(){

return this.referralCardTags;
}



 private void initDownloader(){

 try {
      for(int a = 0; a <  this.maxNoOfDownloaders; a++)
         {
		  this.downloaders[a] = new QRjob_downloader(this.logRoot,
                                                     this.logName,
                                                     this.downloaderStats[a][0],
                                                     this.downloaderStats[a][1],
                                                     this.downloaderStats[a][2],
                                                     this);
		 }
 } catch (Exception e){
          e.printStackTrace();
          log_writer.doLogging_QRmgr(logRoot, logName, "QRmanager.initDownloader() error: "+e);
          System.exit(0);
 }
 }



 private void startDownloadMgr(){

 try {
      this.downloadMgr = new QRjob_downloadMgr(this.logRoot,
                                               this.logName,
                                               this.downloaders,
                                               this);
      new Thread(this.downloadMgr).start();

 } catch (Exception e){
          e.printStackTrace();
          log_writer.doLogging_QRmgr(logRoot, logName, "QRmanager.startDownloadMgr() error: "+e);
          System.exit(0);
 }
 }


 public QRjob_downloadMgr get_downloadMgr(){

 return this.downloadMgr;
 }


 public QRmanager getThisObject(){

 return this;
 }





 public void storeInDownloaderQueue_newJob(BasicJob job){

 try {
      if(!this.arrivingJobs_downloader.contains(job))
        {
		  while(this.jobsInSystem.get() >= this.arrivingJob_downloaderQueue)
		   {
			log_writer.doLogging_QRmgr(this.logRoot, this.logName,
			"<QRmanager.storeInDownloaderQueue_newJob()> msg: System maxed out!...jobsInSystem="+this.jobsInSystem.get()+"queueSize:="+this.arrivingJob_downloaderQueue);
			this.sleepForDesiredTime(2000);
		   }

		  while(true)
		   {
			synchronized(this.get_queueLock())
			 {
			  if(this.arrivingJobs_downloader.size() >= this.arrivingJob_downloaderQueue)
				{
				 System.out.println("Downloader Job queue full. Must process an existing job before this new job can be stored in queue");
				 log_writer.doLogging_QRmgr(this.logRoot, this.logName, "Downloader Job queue full. Must process an existing job before this new job can be stored queue");
				}
			  else
				{
				 if(!this.arrivingJobs_downloader.contains(job))
				   {
					this.arrivingJobs_downloader.put(job);
                    if(!job.hasJoined())
                      {
					   job.set_joinStatus(true);
					   this.jobsInSystem.incrementAndGet();
					  }
				   }
				 break;
				}
			 }
		   this.sleepForDesiredTime(2500);
		   }
        }
 } catch (Exception e){
    	  e.printStackTrace();
		  log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<QRmanager.storeInDownloaderQueue_newJob()> exception: "+e);
 }
 }


 @Override
 public void storeInDownloaderQueue_pendingJob(BasicJob job){

 try {
	  if(!this.pendingJobs_downloader.contains(job))
	    {
		  while(true)
			{
			 synchronized(this.get_queueLock())
			  {
			  if(this.pendingJobs_downloader.size() == this.pendingJob_downloaderQueue)
				{
				 System.out.println("currently running downloader job queue full. Must process an existing job from this queue before this job can be stored in queue");
				 log_writer.doLogging_QRmgr(this.logRoot, this.logName, "currently running downloader job queue full. Must process an existing job from this queue before this job can be stored in queue");
				}
			  else
				{
				 if(!this.pendingJobs_downloader.contains(job))
				   {
				    this.pendingJobs_downloader.put(job);
			       }
				 break;
				}
			  }
			this.sleepForDesiredTime(2500);
			}
	    }
 } catch (Exception e){
          e.printStackTrace();
          log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<BasicManager.storeInDownloaderQueue_pendingJob()> exception: "+e);
 }
 }



 @Override
 public void storeForDemographicsRetrieval(Object[] queryDetails){
 try {
      new Thread(new storeInDemographicsRetrieverQueue(queryDetails)).start();

 } catch (Exception e){
           e.printStackTrace();
           log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<BasicManager.storeForDemographicsRetrieval()> exception: "+e);
 }
 }



 public BasicJob get_arrivedJob(){

 BasicJob nJob = null;

 synchronized(this.get_queueLock()){
 try {
	  nJob = this.arrivingJobs_downloader.poll();

 } catch (Exception e){
		  nJob = null;
		  e.printStackTrace();
		  log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<QRmanager.get_arrivedJob()> exception: "+e);
 }
 }
 return nJob;
 }



 public BasicJob get_pendingJob(){

 BasicJob nJob = null;

 synchronized(this.get_queueLock()){
 try {
	  nJob = this.pendingJobs_downloader.poll();

 } catch (Exception e){
		  nJob = null;
		  e.printStackTrace();
		  log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<QRmanager.get_pendingJob()> exception: "+e);
 }
 }
 return nJob;
 }



 public ArrayBlockingQueue getArrivedJobQueue(){

 return this.arrivingJobs_downloader;
 }



 public ArrayBlockingQueue getPendingJobQueue(){

 return this.pendingJobs_downloader;
 }




 public static boolean itsComplete(QRjob jobToCheck,
                                   String logRoot,
                                   String logName){
 boolean done = false;
 try {
      if(jobToCheck == null)
        {
		 log_writer.doLogging_QRmgr(logRoot, logName, "<QRmanager.itsComplete()> msg: Object to check is null");
		 done = true;
		}
	  else
	    {
		 if(jobToCheck.downloadIsComplete())
		   {
		    done = true;
		   }
		}

 } catch (Exception e){
	      done = true;
	      e.printStackTrace();
	      log_writer.doLogging_QRmgr(logRoot, logName, "<QRmanager.itsComplete()> exception: "+e);
 }
 return done;
 }



 public String get_newObjectID(){

 String newID = null;
 try {
	  newID = folderCreator.createNameAndFolder_imageStore(this.downloadedFiles);

 } catch (Exception e){
		  newID = null;
		  e.printStackTrace();
 }
 return newID;
 }



 public String[] getPACS(String pacsWanted){

 String[] rPACS = null;

 try {
      for(int a = 0; a < this.PACS.length; a++)
         {
	      if(this.PACS[a][0].equals(pacsWanted))
	        {
			 rPACS = new String[3];
			 rPACS[0] = this.PACS[a][1];
			 rPACS[1] = this.PACS[a][2];
			 rPACS[2] = this.PACS[a][3];

			 break;
			}
	     }
 } catch (Exception e){
	      rPACS = null;
	      e.printStackTrace();
	      log_writer.doLogging_QRmgr(logRoot, logName, "<QRmanager.getPACS()> exception: "+e);
 }
 return rPACS;
 }





 public void addNewJob(String dataRead){
 try {
      Vector<QRjob> newJobs = new Vector<QRjob>(5);

      String[] individualJobs = dataRead.split(this.endValue_cMoveString);
      if(individualJobs.length <= 0)
        {
	     log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<QRmanager.doExtraction_1()> msg: Empty string");
	    }
	  else
	    {
         for(int a = 0; a < individualJobs.length; a++)
            {
             String[] jobValues = individualJobs[a].split("&");

			 String job_id = null;
			 String opType = null;

			 String patientId = null;
			 String pName = null;
			 String dob = null;
			 String sex = null;
			 String studyDesc = null;
			 String AccNo = null;
			 String modality = null;
			 String studyDate = null;
			 String studyTime = null;
			 String noOfImages = null;
			 String referringClinician = null;
			 String source = null;
			 String destination = null;

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
				    else if(jobValues[x].indexOf("pid") >= 0)
					  {
					   patientId = this.getValue(jobValues[x]);
				      }
				    else if(jobValues[x].indexOf("pName") >= 0)
					  {
					   pName = this.getValue(jobValues[x]);
				      }
				    else if(jobValues[x].indexOf("DOB") >= 0)
					  {
					   dob = this.getValue(jobValues[x]);
				      }
				    else if(jobValues[x].indexOf("sex") >= 0)
					  {
					   sex = this.getValue(jobValues[x]);
				      }
				    else if(jobValues[x].indexOf("StudyDesc") >= 0)
					  {
					   studyDesc = this.getValue(jobValues[x]);
				      }
				    else if(jobValues[x].indexOf("AccNo") >= 0)
					  {
					   AccNo = this.getValue(jobValues[x]);
				      }
				    else if(jobValues[x].indexOf("modality") >= 0)
					  {
					   modality = this.getValue(jobValues[x]);
				      }
				    else if(jobValues[x].indexOf("studyDate") >= 0)
					  {
					   studyDate = this.getValue(jobValues[x]);
				      }
				    else if(jobValues[x].indexOf("studyTime") >= 0)
					  {
					   studyTime = this.getValue(jobValues[x]);
				      }
				    else if(jobValues[x].indexOf("NoOfImages") >= 0)
					  {
					   noOfImages = this.getValue(jobValues[x]);
				      }
				    else if(jobValues[x].indexOf("rClinician") >= 0)
					  {
					   referringClinician = this.getValue(jobValues[x]);
				      }
                    else if(jobValues[x].indexOf("source") >= 0)
					  {
					   source = this.getValue(jobValues[x]);
				      }
				    else if(jobValues[x].indexOf("destination") >= 0)
					  {
					   destination = this.getValue(jobValues[x]);
				      }
			       }
				}


			  if((job_id != null) && (AccNo != null))
			    {
				 QRjob n_job = null;
				 if((AccNo == "")||(AccNo == " "))
				   {}
				 else
				   {
					n_job = this.start_newJob(job_id ,
											  opType ,
											  patientId ,
											  pName ,
											  dob ,
											  sex ,
											  studyDesc ,
											  AccNo ,
											  modality ,
											  studyDate ,
											  studyTime ,
											  noOfImages ,
											  referringClinician,
											  source,
											  destination);

	//String sqlstr = "update autorouter_nighthawk set patientid = ?, patientname = ?, dob = ?,  sex = ?, accessionnumber = ?, studyname = ?, modality = ?, studydate = ?, studytime = ? where jobkey = ?";


                     if(n_job != null)
			           {
			            newJobs.add(n_job);
			           }
				    }
			    }
			  else
			    {
			 	 log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<QRmanager.doExtraction_1()> msg: Null values passed in arg");
			    }
		    }
	    }

  if(newJobs.size() > 0)
    {
     Vector<QRjob> jobsWithDemo = new Vector<QRjob>(5);
     Vector<QRjob> jobsWithNoDemo = new Vector<QRjob>(5);
     Vector<Object[]> insertionInfo = new Vector<Object[]>(5);

     for(int k = 0; k < newJobs.size(); k++)
        {
         QRjob job = (QRjob) newJobs.get(k);
         String[] jobDemoData = job.get_jobDemgraphics();
         if(jobDemoData[0] == null)
           {
		    jobsWithNoDemo.add(job);
		   }
         else if((jobDemoData[0] == "")||(jobDemoData[0] == " "))
           {
		    jobsWithNoDemo.add(job);
		   }
		 else
		   {
		    jobsWithDemo.add(job);
		   }
		}

     //Add those with demographics to system...
     if(jobsWithDemo.size() > 0)
       {
		 Object[] dToInsert = new Object[jobsWithDemo.size()];
		 for(int c = 0; c < dToInsert.length; c++)
			{
			 QRjob job = (QRjob) jobsWithDemo.get(c);
			 dToInsert[c] = job.get_insertionData();
			}
		 boolean errorOccurred = new insertBatch_nightHawkTable(this.logRoot, this.logName, dToInsert).doInsert();
		 if(!errorOccurred)
		   {
			for(int c = 0; c < newJobs.size(); c++)
			   {
				QRjob job = (QRjob) jobsWithDemo.get(c);
				job.insertDataIntoJobHistoryTable();
				this.storeInDownloaderQueue_newJob(job);
			   }
		   }
	   }

	  //Next, let's add those with no demographics to the "demographics retrieving" queue..
	  for(int d = 0; d < jobsWithNoDemo.size(); d++)
	     {
		  QRjob job = (QRjob) jobsWithNoDemo.get(d);
          Object[] demogrhpcs = job.get_demographicsRetrivingInfo();
          if(demogrhpcs != null)
            {
			 dmAssigner.storeData(demogrhpcs);
			 //=====================================
			 this.jobsAwaitingDemographics.add(job);
			 //=====================================
		    }
		 }


     /*
     Object[] dToInsert = new Object[newJobs.size()];
     for(int a = 0; a < dToInsert.length; a++)
        {
         QRjob job = (QRjob) newJobs.get(a);
         dToInsert[a] = job.get_insertionData();
	    }
     boolean errorOccurred = new insertBatch_nightHawkTable(this.logRoot, this.logName, dToInsert).doInsert();
     if(!errorOccurred)
       {
        for(int a = 0; a < newJobs.size(); a++)
           {
            QRjob job = (QRjob) newJobs.get(a);
            job.insertDataIntoJobHistoryTable();
            Object[] demogrhpcs = job.get_demographicsRetrivingInfo();
            if(demogrhpcs != null)
              {
			   dmAssigner.storeData(demogrhpcs);
		      }
            this.storeInDownloaderQueue_newJob(job);
	       }
	   }
	   */

    }

 } catch (Exception e){
	      e.printStackTrace();
          log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<QRmanager.doExtraction_1()> error: "+e);
 }
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
          log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<QRmanager.getValue()> error: "+e);
 }
 return extractedVal;
 }




 // starts new QR job.
 public QRjob start_newJob(String job_id ,
						   String opType ,
						   String patientId ,
						   String pName ,
						   String dob ,
						   String sex ,
						   String studyDesc ,
						   String AccNo ,
						   String modality ,
						   String studyDate ,
						   String studyTime ,
						   String noOfImages ,
						   String referringClinician,
						   String source,
						   String destination){


 QRjob qJob = null;

 synchronized(this.joiningLOCK){
 try {
      if(!this.accNoInSystem.contains(AccNo)){
      boolean continueOp = true;
      String[] pacsDetails = null;
      String[] intendedDest = null;
      if(continueOp)
	    {
		 pacsDetails = this.getPACS(source);
		 if(pacsDetails == null)
		   {
			continueOp = false;
			log_writer.doLogging_QRmgr(logRoot, logName, "<QRmanager.start_newJob()> msg: No matching PACS found for "+source);
		   }

		 intendedDest = this.getDestination(destination);
		 if(intendedDest == null)
		   {
			continueOp = false;
			log_writer.doLogging_QRmgr(logRoot, logName, "<QRmanager.start_newJob()> msg: No matching destination found for "+destination);
		   }
	    }

      if(continueOp)
        {
		  String[] jobDemographics = new String[9];

		  jobDemographics[0] = patientId;
		  jobDemographics[1] = pName;
		  jobDemographics[2] = dob;
		  jobDemographics[3] = sex;
		  jobDemographics[4] = studyDesc;
		  jobDemographics[5] = AccNo;
		  jobDemographics[6] = modality;
		  jobDemographics[7] = studyDate;
		  jobDemographics[8] = studyTime;

		  String f_location = this.get_newObjectID();

		  if(f_location == null)
			{
			 log_writer.doLogging_QRmgr(logRoot, logName, "<QRmanager.start_newJob()> error: failed to create folder for new job: "+job_id);
			}
		  else
			{
			 qJob = new QRjob (this,
				               job_id,
							   f_location,
							   null,
							   null,
							   this.logRoot,
							   this.logName,
							   this.filesForTransfer,
							   this.failedMoveQueueSize,
							   this.pendingQueueSize);

			//===================
			//New...
			qJob.setJobType(1);
			//===================

			qJob.set_jobDemographics(jobDemographics);
			if(this.referralCardsWanted)
			  {
			   qJob.set_referralWanted(true);
			   qJob.set_RISdetails(this.risDetails);
			   qJob.set_imageQuality(this.imageQuality);
			   qJob.set_cfgFileData(this.cfgFileTags);
			  }
			else
			  {
			   qJob.set_referralRetrieved(true);
			  }

			if(intendedDest[0].equals("dcmsnd"))
			  {
			   qJob.set_transferMode("dcmsnd");

			   String[] dest = new String[3];
			   dest[0] = intendedDest[2];
			   dest[1] = intendedDest[3];
			   dest[2] = intendedDest[4];

			   qJob.set_dcmsndMainStats(dest);
			   qJob.set_httpsMainStats(this.get_desiredHttps(this.get_defaultHttps()));

			   //==================================================================
			   //this.do_dbUpdate(qJob,4, null, intendedDest[1], logRoot, logName);
			   //==================================================================
			  }
			else if(intendedDest[0].equals("https"))
			  {
			   qJob.set_transferMode("https");

			   String[] dest = new String[2];
			   dest[0] = intendedDest[2];
			   dest[1] = intendedDest[3];

			   qJob.set_httpsMainStats(dest);
			   qJob.set_dcmsndMainStats(this.get_desiredDcmsnd(this.get_defaultDcmsnd()));

			   //==================================================================
			   //this.do_dbUpdate(qJob,4, null, intendedDest[1], logRoot, logName);
			   //==================================================================
			  }
			else
			  {
			   log_writer.doLogging_QRmgr(logRoot, logName, "<QRmanager.start_newJob()> error: failed to find transfer mode for new job: "+job_id);
			   continueOp = false;
			  }

			if(continueOp)
			  {
			   qJob.set_cmoverspTO(this.cMoversTo);
			   qJob.set_currentPACS(pacsDetails);
			   qJob.set_currentModalities(this.cModalities);

               if((jobDemographics[0] == null)||(jobDemographics[1] == null))
			     {
				  String[] searchDetails = new String[4];
				  searchDetails[0] = job_id;
				  searchDetails[1] = "Accession_Number";
				  searchDetails[2] = AccNo;
				  searchDetails[3] = "NEW_SEARCH";

				  Object[] queryDetails = new Object[6];
				  queryDetails[0] = qJob;
				  queryDetails[1] = this.cFindLocal;
				  queryDetails[2] = pacsDetails;
				  queryDetails[3] = searchDetails;
				  queryDetails[4] = BasicManager.forPACSquery;
				  queryDetails[5] = source;

				  qJob.set_demographicsRetrivingInfo(queryDetails);
			     }

			  qJob.set_AccNo(AccNo);
			  this.accNoInSystem.add(qJob.get_AccNo()); //add job to system..
			  qJob.writeDemographicsToDB();
		      }
			}
	    }
	   }

 } catch (Exception e){
	      e.printStackTrace();
	      log_writer.doLogging_QRmgr(logRoot, logName, "<QRmanager.start_newJob()> exception: "+e);
 }
 }
 return qJob;
 }



 ///////////////////////////////////////////
 // Just so another process is not delayed.
 //////////////////////////////////////////
 class storeInDemographicsRetrieverQueue implements Runnable {

 Object[] dataToStore = null;

 public storeInDemographicsRetrieverQueue(Object[] dataToStore){

 this.dataToStore = dataToStore;
 }

 public void run(){

 try {
      dmAssigner.storeData(this.dataToStore);
 } catch (Exception e){
	      e.printStackTrace();
	      log_writer.doLogging_QRmgr(logRoot, logName,
	      "<QRmanager.storeInDemographicsRetrieverQueue()> exception: "+e);
 }
 }
 }



 public void start_newSearch(String job_id ,
							 String opType ,
							 String source ,
							 String querytype ,
							 String searchid ,
							 String jobstatus){
 try {
      if((job_id == null)||
        ( opType == null)||
        ( source == null)||
        ( querytype == null)||
        ( searchid == null)||
        ( jobstatus == null))
        {
         log_writer.doLogging_QRmgr(logRoot, logName, "<QRmanager.start_newSearch()> msg: Null values received as arg");
		}
	  else
	    {
         String[] pacsDetails = this.getPACS(source);
         if(pacsDetails == null)
           {
		    log_writer.doLogging_QRmgr(logRoot, logName, "<QRmanager.start_newSearch()> msg: No matching PACS found for "+source);
		   }
		 else
		   {
			//TODO: Take this in from properties file..
			String[][] data = new String[12][2];

			data[0][0] = "patient id";           data[0][1] = "00100020";
			data[1][0] = "patient name";         data[1][1] = "00100010";
			data[2][0] = "DOB";                  data[2][1] = "00100030";
			data[3][0] = "sex";                  data[3][1] = "00100040";
			data[4][0] = "Accession No";         data[4][1] = "00080050";
			data[5][0] = "Study Desc";           data[5][1] = "00081030";
			data[6][0] = "Modality";             data[6][1] = "00080061";
			data[7][0] = "Study date";           data[7][1] = "00080020";
			data[8][0] = "Study time";           data[8][1] = "00080030";
			data[9][0] = "No of images";         data[9][1] = "00201208";
			data[10][0] = "Referring clinician"; data[10][1] = "00080090";
			data[11][0] = "Study Instance UID";  data[11][1] = "0020000D";

			String[] searchDetails = new String[4];
			searchDetails[0] = job_id;
			searchDetails[1] = querytype;
			searchDetails[2] = searchid;
			searchDetails[3] = jobstatus;

			new QRjob_cFinder(this,
				              logRoot,
							  logName,
							  this.cFindLocal,
							  pacsDetails,
							  searchDetails,
							  data,
							  source).startOp();
		   }
		}

 } catch (Exception e){
	      e.printStackTrace();
	      log_writer.doLogging_QRmgr(logRoot, logName, "<QRmanager.start_newSearch()> exception: "+e);
 }
 }




 public void do_newSearch_2(String job_id,
                            String source,
                            String modalitytype,
                            String duration){
 try {
         String[] pacsDetails = this.getPACS(source);
         if(pacsDetails == null)
           {
		    log_writer.doLogging_QRmgr(logRoot, logName, "<QRmanager.do_newSearch_2()> msg: No matching PACS found for "+source);
		   }
		 else
		   {
			//TODO: Take this in from properties file..
			String[][] data = new String[12][2];

			data[0][0] = "patient id";           data[0][1] = "00100020";
			data[1][0] = "patient name";         data[1][1] = "00100010";
			data[2][0] = "DOB";                  data[2][1] = "00100030";
			data[3][0] = "sex";                  data[3][1] = "00100040";
			data[4][0] = "Accession No";         data[4][1] = "00080050";
			data[5][0] = "Study Desc";           data[5][1] = "00081030";
			data[6][0] = "Modality";             data[6][1] = "00080060";
			data[7][0] = "Study date";           data[7][1] = "00080020";
			data[8][0] = "Study time";           data[8][1] = "00080030";
			data[9][0] = "No of images";         data[9][1] = "00201208";
			data[10][0] = "Referring clinician"; data[10][1] = "00080090";
			data[11][0] = "Study Instance UID";  data[11][1] = "0020000D";


			//TODO: Work out dates from duration.
			String currentDate = dcm_folderCreator.generate_imageStoreName(); // (yyyy-mm-dd)
            String[] dateSplit = currentDate.split("-");

            //quick hack for Calendar class compatibility:
			String monthVal = dateSplit[1];
		    int monthValNo = Integer.parseInt(monthVal);
			monthValNo = monthValNo - 1;

			int durationVal = Integer.parseInt(duration);
			durationVal = -durationVal;
			Calendar ca1 = Calendar.getInstance();
			ca1.set(Integer.parseInt(dateSplit[0]),
			        monthValNo,
			        Integer.parseInt(dateSplit[2]));
		    ca1.add(Calendar.DATE, durationVal);

		    int year = ca1.get(Calendar.YEAR);
			int month = ca1.get(Calendar.MONTH);
            int day = ca1.get(Calendar.DATE);

		    String actualDate = null;
            String yr = Integer.toString(year);
            String dy = Integer.toString(day);
            String mnth = arrangeMonth(month);
            if(mnth == null)
              {
	           actualDate = null; //just in case..
	          }
	        else
	          {
	           if(yr.length() <= 1)
		         {
		          yr = "0"+yr;
		         }
	           if(mnth.length() <= 1)
		         {
		          mnth = "0"+mnth;
				 }
			   if(dy.length() <= 1)
				 {
				  dy = "0"+dy;
				 }

				actualDate = yr+"-"+mnth+"-"+dy;
			  }


            //--------------------------------------------------------------------------------------------------------------
            log_writer.doLogging_QRmgr(logRoot, logName, "<QRmanager.do_newSearch_2()> msg: Worklist <start date>: "+currentDate+"  <end date>: "+actualDate);
            //--------------------------------------------------------------------------------------------------------------

            actualDate = actualDate.replaceAll("-","");
            actualDate = actualDate.replaceAll(" ","");
            currentDate = currentDate.replaceAll("-","");
            currentDate = currentDate.replaceAll(" ","");



			/*  For testing..
			String[] setArgs = new String[8];

			setArgs[0] = "-L";
			setArgs[1] = this.cFindLocal[0]+"@"+this.cFindLocal[1];
			setArgs[2] = pacsDetails[0]+"@"+pacsDetails[1]+":"+pacsDetails[2];
			setArgs[3] = "-q";
			setArgs[4] = "PatientName";
			setArgs[5] = "*";
			setArgs[6] = "-r";
			setArgs[7] = "00080050";
			*/


            //Real thing..

            String[] setArgs = null;

            if(modalitytype.equalsIgnoreCase("ALL"))
              {

			   log_writer.doLogging_QRmgr(logRoot, logName, "<QRmanager.do_newSearch_2()> Worklist: Modality type set to ALL");
			   /*
			   setArgs = new String[13];
			   setArgs[0] = "-L";
			   setArgs[1] = this.cFindLocal[0]+"@"+this.cFindLocal[1];
			   setArgs[2] = pacsDetails[0]+"@"+pacsDetails[1]+":"+pacsDetails[2];
			   setArgs[3] = "-q";
			   setArgs[4] = "0032000A";
			   setArgs[5] = "COMPLETED";
			   setArgs[6] = "-q";
			   setArgs[7] = "00080020";
			   setArgs[8] = actualDate+"-"+currentDate;
			   setArgs[9] = "-r";
			   setArgs[10] = "00080060";
			   setArgs[11] = "-r";
			   setArgs[12] = "0032000C";
			   */
			  }
			else
			  {
               setArgs = new String[18];
			   setArgs[0] = "-L";
			   setArgs[1] = this.cFindLocal[0]+"@"+this.cFindLocal[1];
			   setArgs[2] = pacsDetails[0]+"@"+pacsDetails[1]+":"+pacsDetails[2];
			   setArgs[3] = "-q";
			   setArgs[4] = "00080060";
			   setArgs[5] = modalitytype;
			   setArgs[6] = "-q";
			   setArgs[7] = "0032000A";
			   setArgs[8] = "COMPLETED";
			   setArgs[9] = "-q";
			   setArgs[10] = "00080020";
			   setArgs[11] = actualDate+"-"+currentDate;
			   setArgs[12] = "-r";
			   setArgs[13] = "00081030";
			   setArgs[14] = "-r";
			   setArgs[15] = "00100010";
			   setArgs[16] = "-r";
			   setArgs[17] = "00100020";

			   log_writer.doLogging_QRmgr(logRoot, logName, "<QRmanager.do_newSearch_2()> msg: actualDate = "+actualDate+", currentDate = "+currentDate);
			  }

		    Vector results = new QRjob_cFinder(this, logRoot, logName).runDirect_2(setArgs, data, job_id, source);
		    if(results == null)
		      {
		       log_writer.doLogging_QRmgr(logRoot, logName, "<QRmanager.do_newSearch_2()> msg: Worklist query returns nothing!");

               String[] msg = new String[3];
		       msg[0] = "SEARCH_FAILED";
		       msg[1] = "No record found";
		       msg[2] = job_id;
		       new updateJobStatus(this, logRoot, logName).doUpdate(msg);

		       /*
		       Object[] args = new Object[3];
		       Integer thisInt = new Integer(6);
		       args[0] = thisInt;
		       args[1] = this;
		       args[2] = msg;
               this.get_dbUpdateMgr().storeData(args);
               */
			  }
			else
			  {
			   String[] msg = new String[2];
			   msg[0] = "SEARCH_COMPLETED";
			   msg[1] = job_id;
			   new updateJobStatus(this, logRoot, logName).doUpdate(msg);
			  }
	       }

 } catch (Exception e){
	      e.printStackTrace();
	      log_writer.doLogging_QRmgr(logRoot, logName, "<QRmanager.do_newSearch_2()> exception: "+e);
 }
 }




//arranges month retrieved from Calendar class
//in the format we want.
private String arrangeMonth(int mnthVal){

String arrangedMnth = null;
try {
     if(mnthVal == 0)
       {
	    arrangedMnth = "01";
	   }
	 else if(mnthVal == 1)
	   {
	 	arrangedMnth = "02";
	   }
	 else if(mnthVal == 2)
	   {
	    arrangedMnth = "03";
	   }
	 else if(mnthVal == 3)
	   {
	 	arrangedMnth = "04";
	   }
	 else if(mnthVal == 4)
	   {
	 	arrangedMnth = "05";
	   }
	 else if(mnthVal == 5)
	   {
	 	arrangedMnth = "06";
	   }
	 else if(mnthVal == 6)
	   {
	 	arrangedMnth = "07";
	   }
	 else if(mnthVal == 7)
	   {
	 	arrangedMnth = "08";
	   }
	 else if(mnthVal == 8)
	   {
	 	arrangedMnth = "09";
	   }
	 else if(mnthVal == 9)
	   {
	 	arrangedMnth = "10";
	   }
	 else if(mnthVal == 10)
	   {
	 	arrangedMnth = "11";
	   }
	 else if(mnthVal == 11)
	   {
	 	arrangedMnth = "12";
	   }
	 else
	   {
	    log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<QRmanager.arrangeMonth()> msg: Invalid month value "+mnthVal);
	   }

} catch (Exception e){
	     arrangedMnth = null;
	     e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<QRmanager.arrangeMonth()> error: "+e);
}
return arrangedMnth;
}






 private String stripAndRetrieve_accNo(String[] res){

 String accNo = null;
 try {
	  for(int p = 0; p < res.length; p++)
		 {
		  if(res[p] != null)
			{
			 if((res[p].equals("")) || (res[p].equals(".")))
			   {
			   }
			 else
			   {
				if(res[p].indexOf("Accession No") >= 0)
				  {
				   String[] accNoSplit = res[p].split(":");
				   if(accNoSplit.length > 1)
				     {
				      if(accNoSplit[1] != null)
					    {
					     if((accNoSplit[1].equals("")) || (accNoSplit[1].equals(".")))
						   {
						   }
					     else
						   {
						    accNo = accNoSplit[1];
						    accNo = accNo.replaceAll(" ","");
						   }
					    }
				     }
				   break;
				  }
			   }
			}
		 }

 } catch (Exception e){
 	      accNo = null;
 	      e.printStackTrace();
 	      log_writer.doLogging_QRmgr(logRoot, logName, "<QRmanager.stripAndRetrieve_accNo()> exception: "+e);
 }
 return accNo;
 }



 public void doOpOnRelegated(BasicJob job, String opType){

 try {

 } catch (Exception e){
	      log_writer.doLogging_QRmgr(logRoot, logName, "QRmanager.doOpOnRelegated() error: "+e);
 }
 }



//Overrides superclass..
public void doALL(String opType){

try {
     synchronized(this.get_queueLock())
      {
       int size = this.arrivingJobs_downloader.size();
       for(int a = 0; a < size; a++)
          {
           BasicJob nJob = this.arrivingJobs_downloader.poll();
	       if(nJob != null)
	         {
              this.doJobTask(nJob, opType, 1);
              nJob= null;
		     }
	      }

       size = this.pendingJobs_downloader.size();
       for(int a = 0; a < size; a++)
          {
	       BasicJob nJob = this.pendingJobs_downloader.poll();
	       if(nJob != null)
	         {
	    	  this.doJobTask(nJob, opType, 2);
		      nJob= null;
		     }
	      }

       size = this.currentlyRunningJobs.size();
       for(int a = 0; a < size; a++)
          {
	       BasicJob nJob = this.currentlyRunningJobs.poll();
	       if(nJob != null)
	         {
		      this.doJobTask(nJob, opType, 3);
		      nJob= null;
		     }
	      }

       size = this.arrivingJobs.size();
       for(int a = 0; a < size; a++)
          {
           BasicJob nJob = this.arrivingJobs.poll();
	       if(nJob != null)
	         {
              this.doJobTask(nJob, opType, 4);
              nJob= null;
		     }
	      }

       size = this.relegatedJobs.size();
       for(int a = 0; a < size; a++)
          {
           BasicJob nJob = this.relegatedJobs.poll();
	       if(nJob != null)
	         {
              this.doJobTask(nJob, opType, 5);
              nJob= null;
		     }
	      }
	  }

this.nHawkJobController.sortJobObjects(opType);

String desiredOp = null;
if(opType.equals("START_ALL"))
  {
   desiredOp = "pending";
  }
else if(opType.equals("STOP_ALL"))
  {
   desiredOp = "stopped";
  }
else if(opType.equals("PAUSE_ALL"))
  {
   desiredOp = "paused";
  }
else if(opType.equals("CANCEL_ALL"))
  {
   desiredOp = "cancelled";
  }


String cmmd = "update autorouter_nighthawk set studystatus = ? where islive = ?";
String[] data = new String[2];
data[0] = desiredOp;
data[1] = "1";
new doBulkUpdate(logRoot, logName).doUpdate(cmmd, data);

if(opType.equals("CANCEL_ALL"))
  {
   String cmmd_2 = "update autorouter_nighthawk set islive = ? ";
   String[] data_2 = new String[1];
   data_2[0] = "0";
   new doBulkUpdate(logRoot, logName).doUpdate(cmmd_2, data_2);
  }

 if(opType.equals("START_ALL"))
   {
    Object[] activeJobs = this.nHawkJobController.getActiveJobs();
    if(activeJobs != null)
      {
	   for(int r = 0; r < activeJobs.length; r++)
	      {
		   BasicJob aJob = (BasicJob) activeJobs[r];
		   if(aJob != null)
		     {
			  if((aJob.get_status()).equals("in progress"))
			    {
				 this.do_dbUpdate(aJob,2, null, aJob.get_status(), logRoot, logName);
				}
			 }
		  }
	  }
   }

} catch (Exception e){
	     log_writer.doLogging_QRmgr(logRoot, logName, "QRmanager.doALL() error: "+e);
}
}


//Overrides..
public void changeAllDest(String destination){

try {
     String[] retrievedDest = this.getDest(destination);
     if(retrievedDest != null)
       {
        synchronized(this.get_queueLock())
         {
          super.changeAllDest(destination);

          int size = this.pendingJobs_downloader.size();
          for(int a = 0; a < size; a++)
             {
	          BasicJob nJob = this.pendingJobs_downloader.poll();
	          if(nJob != null)
	            {
		         this.switchDest(retrievedDest, nJob);
		         this.storeInDownloaderQueue_pendingJob(nJob);
		        }
	         }

          size = this.arrivingJobs_downloader.size();
          for(int a = 0; a < size; a++)
             {
              BasicJob nJob = this.arrivingJobs_downloader.poll();
	          if(nJob != null)
	            {
                 this.switchDest(retrievedDest, nJob);
                 this.storeInDownloaderQueue_newJob(nJob);
		        }
	         }
         }
       }

} catch (Exception e){
	     log_writer.doLogging_QRmgr(logRoot, logName, "QRmanager.changeAllDest error: "+e);
}
}




protected void doJobTask(BasicJob nJob, String opType, int queueType){

synchronized(this.get_queueLock()){
try {

	if(this.check_1(nJob, logRoot,logName))
	  {}
	else if((opType.equals("START_ALL")) || (opType.equals("START")))
	  {
	   if((queueType == 1) || (queueType == 2) || (queueType == 5))
	     {
	      nJob.set_status("pending");
	      this.storeInDownloaderQueue_pendingJob(nJob);
	      if(opType.equals("START"))
	        {
             this.do_dbUpdate(nJob,2, null, "pending", logRoot, logName);
		    }
	     }
	   else if((queueType == 3) || (queueType == 4))
	   	 {
          nJob.set_status("pending");
	   	  this.storeInQueue_currentlyRunningJob(nJob);

	   	  if(opType.equals("START"))
	   	    {
             this.do_dbUpdate(nJob,2, null, "pending", logRoot, logName);
		    }
	     }
	  }
	else if((opType.equals("STOP_ALL")) || (opType.equals("STOP")))
	  {
	   nJob.set_status("stopped");
	   if(queueType == 1)
	     {
		  this.storeInDownloaderQueue_newJob(nJob);

		  if(opType.equals("STOP"))
		    {
		     this.do_dbUpdate(nJob,2, null, "stopped", logRoot, logName);
		    }
		 }
	   else if(queueType == 2)
	     {
		  this.storeInDownloaderQueue_pendingJob(nJob);

		  if(opType.equals("STOP"))
		    {
		     this.do_dbUpdate(nJob,2, null, "stopped", logRoot, logName);
		    }
		 }
	   else if(queueType == 3)
	     {
		  this.storeInQueue_currentlyRunningJob(nJob);

		  if(opType.equals("STOP"))
		    {
		     this.do_dbUpdate(nJob,2, null, "stopped", logRoot, logName);
		    }
		 }
	   else if(queueType == 4)
	     {
		  this.storeInQueue_arrivingJob(nJob);

		  if(opType.equals("STOP"))
		    {
		     this.do_dbUpdate(nJob,2, null, "stopped", logRoot, logName);
		    }
		 }
	   else if(queueType == 5)
	     {
		  this.storeInQueue_relegatedJob(nJob);

		  if(opType.equals("STOP"))
		    {
		     this.do_dbUpdate(nJob,2, null, "stopped", logRoot, logName);
		    }
		 }
	   else
	     {
		  log_writer.doLogging_QRmgr(logRoot, logName, "QRmanager.doJobTask() msg: Invalid queue type for job "+nJob.get_jobId());
		 }
	  }
	else if(opType.equals(("PAUSE_ALL")) || (opType.equals("PAUSE")))
	  {
	   nJob.set_status("paused");
	   if(queueType == 1)
	  	 {
	  	  this.storeInDownloaderQueue_newJob(nJob);

	  	  if(opType.equals("PAUSE"))
	  	    {
	  	     this.do_dbUpdate(nJob,2, null, "paused", logRoot, logName);
		    }
	     }
	   else if(queueType == 2)
	  	 {
	  	  this.storeInDownloaderQueue_pendingJob(nJob);

	  	  if(opType.equals("PAUSE"))
	  	    {
	  	     this.do_dbUpdate(nJob,2, null, "paused", logRoot, logName);
		    }
	  	 }
       else if(queueType == 3)
	     {
		  this.storeInQueue_currentlyRunningJob(nJob);

		  if(opType.equals("PAUSE"))
		    {
		     this.do_dbUpdate(nJob,2, null, "paused", logRoot, logName);
		    }
		 }
	   else if(queueType == 4)
	     {
		  this.storeInQueue_arrivingJob(nJob);

		  if(opType.equals("PAUSE"))
		    {
		     this.do_dbUpdate(nJob,2, null, "paused", logRoot, logName);
		    }
		 }
	   else if(queueType == 5)
	     {
		  this.storeInQueue_relegatedJob(nJob);

		  if(opType.equals("PAUSE"))
		    {
		     this.do_dbUpdate(nJob,2, null, "paused", logRoot, logName);
		    }
		 }
	   else
	  	 {
	  	  log_writer.doLogging_QRmgr(logRoot, logName, "QRmanager.doJobTask() msg: Invalid queue type for job "+nJob.get_jobId());
		 }
	  }
	else if((opType.equals("CANCEL_ALL")) || (opType.equals("CANCEL")))
	  {
       this.cancelOp(nJob, opType);
	  }

} catch (Exception e){
	     log_writer.doLogging_QRmgr(logRoot, logName, "QRmanager.doJobTask() error: "+e);
}
}
}




public boolean cancelOp(BasicJob nJob, String opType){
try {
   boolean statusSet = false;
   if(this.downloaders != null)
	 {
	  for(int c = 0; c < this.downloaders.length; c++)
		 {
		  if(this.downloaders[c] != null)
			{
			 BasicJob execJob = this.downloaders[c].get_currentJob();
			 if(execJob != null)
			   {
				//log_writer.doLogging_QRmgr(logRoot, logName, "QRmanager.cancelOp() msg: job stopped! exec job id: "+execJob.get_jobId()+", current job id: "+nJob.get_jobId());

				if(execJob.get_jobId().equals(nJob.get_jobId()))
				  {
				   this.downloaders[c].stopCurrentJob("cancelled");
				   statusSet = true;
				   log_writer.doLogging_QRmgr(logRoot, logName, "QRmanager.cancelOp() msg: job stopped! "+nJob.get_jobId());
				   break;
				  }
			   }
			}
		 }
	 }

   if(!statusSet)
	 {
	  nJob.set_status("cancelled");
	 }

   if(opType.equals("CANCEL"))
     {
      this.do_dbUpdate(nJob,2, null, "cancelled", logRoot, logName);
      this.do_dbUpdate(nJob,1, "0", null, logRoot, logName);
     }

} catch (Exception e){
	     log_writer.doLogging_QRmgr(logRoot, logName, "QRmanager.cancelOp() error: "+e);
}
return true;
}





 //Overwrites superclass...
 public boolean findAndDoOp_nightHawk(String jobId, String opType, String others){

 boolean taskDone = false;
 synchronized(this.get_queueLock()){
 try {
      taskDone = super.findAndDoOp_nightHawk(jobId, opType, others);
      if(!taskDone)
        {
         for(int a = 0; a < this.arrivingJobs_downloader.size(); a++)
            {
		     BasicJob nJob = this.arrivingJobs_downloader.poll();
			 if(nJob != null)
			   {
			 	if(nJob.get_jobId().equals(jobId))
		          {
				   if(opType.equals("preemptRequest"))
				     {
					  boolean preempted = this.nHawkJobController.attemptPreempt(nJob);
					  if(!preempted)
					    {
						 this.storeInDownloaderQueue_newJob(nJob);
					    }
					 }
					else if(opType.equals("changeDestination"))
					 {
					  this.changeDestination(nJob, others);
					  this.storeInDownloaderQueue_newJob(nJob);
					 }
					else if(opType.equals("START_IMMEDIATELY"))
					 {
					  if(this.check_1(nJob, logRoot,logName))
	                    {}
					  else
					    {
						  boolean doTakeOver = true;
						  if(!nJob.downloadIsComplete())
							{
							 QRjob_downloader downloader = this.get_anyDownloader();
							 if(downloader == null)
							   {
								log_writer.doLogging_QRmgr(this.logRoot, this.logName,
								"<QRmanager.findAndDoOp_nightHawk()> msg: Failed to download for immediate start <job id>: "+nJob.get_jobId());
								doTakeOver = false;
							   }
							 else
							   {
								downloader.takeOver((QRjob) nJob);
							   }
							}
						  if(doTakeOver){this.nHawkJobController.startThisJob(nJob);}
					    }
					 }
					else
					 {
					  this.doJobTask(nJob, opType, 1);
					 }

					taskDone = true;
					break;
				  }
		        else
		          {
				   this.storeInDownloaderQueue_newJob(nJob);
			       nJob = null;
				  }
		       }
		    }
		}
      if(!taskDone)
        {
         for(int a = 0; a < this.pendingJobs_downloader.size(); a++)
            {
		     BasicJob nJob = this.pendingJobs_downloader.poll();
			 if(nJob != null)
			   {
			 	if(nJob.get_jobId().equals(jobId))
		          {
				   if(opType.equals("preemptRequest"))
				     {
					  boolean preempted = this.nHawkJobController.attemptPreempt(nJob);
					  if(!preempted)
					    {
						 this.storeInDownloaderQueue_pendingJob(nJob);
					    }
					 }
					else if(opType.equals("changeDestination"))
					 {
					  this.changeDestination(nJob, others);
					  this.storeInDownloaderQueue_pendingJob(nJob);
					 }
					else if(opType.equals("START_IMMEDIATELY"))
					 {
					  if(this.check_1(nJob, logRoot,logName))
	                    {}
	                  else
	                    {
						  boolean doTakeOver = true;
						  if(!nJob.downloadIsComplete())
							{
							 QRjob_downloader downloader = this.get_anyDownloader();
							 if(downloader == null)
							   {
								log_writer.doLogging_QRmgr(this.logRoot, this.logName,
								"<QRmanager.findAndDoOp_nightHawk()> msg: Failed to download for immediate start <job id>: "+nJob.get_jobId());
								doTakeOver = false;
							   }
							 else
							   {
								downloader.takeOver((QRjob) nJob);
							   }
							}
						  if(doTakeOver){this.nHawkJobController.startThisJob(nJob);}
					    }
					 }
					else
					 {
					  this.doJobTask(nJob, opType, 2);
					 }

					taskDone = true;
					break;
				  }
		        else
		          {
				   this.storeInDownloaderQueue_pendingJob(nJob);
			       nJob = null;
				  }
		       }
		    }
	    }

 } catch (Exception e){
         e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<QRmanager.findAndDoOp_nightHawk()> exception: "+e);
 }
 }
 return taskDone;
 }



 private QRjob_downloader get_anyDownloader(){

 QRjob_downloader dwnloader = null;
 try{
     for(int a = 0; a <this.downloaders.length; a++)
        {
	     if(this.downloaders[a] != null)
	       {
		    dwnloader = this.downloaders[a];
		    break;
		   }
	    }
 } catch (Exception e){
          dwnloader = null;
          e.printStackTrace();
          log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<QRmanager.get_anyDownloader()> exception: "+e);
 }
 return dwnloader;
 }




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
		  boolean doTakeOver = true;
		  if(!nJob.downloadIsComplete())
			{
			 QRjob_downloader downloader = this.get_anyDownloader();
			 if(downloader == null)
			   {
				log_writer.doLogging_QRmgr(this.logRoot, this.logName,
				"<QRmanager.findAndDoOp_nightHawk()> msg: Failed to download for immediate start <job id>: "+nJob.get_jobId());
				doTakeOver = false;
			   }
			 else
			   {
				downloader.takeOver((QRjob) nJob);
			   }
			}
		  if(doTakeOver){this.nHawkJobController.startThisJob(nJob);}
	     }
	   else
		 {
		  this.doJobTask(nJob, opType, 5);
		 }
 } catch (Exception e){
           e.printStackTrace();
           log_writer.doLogging_QRmgr(logRoot, logName, "QRmanager.handleRelegatedTask() error: "+e);
 }
 }



 public int get_downloaderQueueSize(){

 int size = 0;

 if(this.arrivingJobs_downloader != null)
   {
    size = this.arrivingJobs_downloader.size();
   }
 return size;
 }


 public int get_downloaderPendingQueueSize(){

 int size = 0;

 if(this.pendingJobs_downloader != null)
   {
    size = this.pendingJobs_downloader.size();
   }
 return size;
 }





public BasicJob findAndgetJob(String jobId){

boolean taskDone = false;
BasicJob foundJob = null;
BasicJob nJob = null;

synchronized(this.get_queueLock()){
try {
     foundJob = super.findAndgetJob(jobId);

     if(foundJob == null)
       {
		 int size = this.arrivingJobs_downloader.size();
		 for(int a = 0; a < size; a++)
			{
			 nJob = this.arrivingJobs_downloader.poll();
			 if(nJob != null)
			   {
				if(nJob.get_jobId().equals(jobId))
				  {
				   foundJob = nJob;
				   taskDone = true;
				   this.storeInDownloaderQueue_newJob(nJob);
				   break;
				  }
				else
				  {
				   this.storeInDownloaderQueue_newJob(nJob);
				   nJob = null;
				  }
			   }
		   }

		 if(!taskDone)
		   {
			 size = this.pendingJobs_downloader.size();
			 for(int a = 0; a < size; a++)
				{
				 nJob = this.pendingJobs_downloader.poll();
				 if(nJob != null)
				   {
					if(nJob.get_jobId().equals(jobId))
					  {
					   foundJob = nJob;
					   taskDone = true;
					   this.storeInDownloaderQueue_pendingJob(nJob);
					   break;
					  }
					else
					  {
					   this.storeInDownloaderQueue_pendingJob(nJob);
					   nJob = null;
					  }
				   }
				}
		   }
       }

} catch (Exception e){
         e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<QRmanager.findAndgetJob()> exception: "+e);
}
}
return foundJob;
}



 ////////////////////////////////////////////////
 // Tasked with assigning demographics for jobs.
 ///////////////////////////////////////////////
 class demographicsAssigner implements Runnable {

 ArrayBlockingQueue<Object[]> demoGraphList = new ArrayBlockingQueue<Object[]>(300); //TODO: take in from properties file..

 public demographicsAssigner(){}


 public void storeData(Object[] data){
 try {
      this.demoGraphList.put(data);
 } catch (Exception e){
           e.printStackTrace();
           log_writer.doLogging_QRmgr(logRoot, logName, "QRmanager.demographicsAssigner.storeData() error: "+e);
 }
 }


 public void run(){
 try {
      while(true)
       {
        Object[] demoQueryDetails = demoGraphList.take();
        if(demoQueryDetails != null)
          {
           BasicJob job = (BasicJob) demoQueryDetails[0];
           Object[] queryDetails = new Object[(demoQueryDetails.length - 1)];
           for(int k = 0; k < queryDetails.length; k++)
              {
			   queryDetails[k] = demoQueryDetails[ k + 1];
			  }


           new getDemographicsFromPACS(getThisObject(),
			                           queryDetails,
					                   logRoot,
					                   logName,
                                       job).run();

	       sleepForDesiredTime(100); //TODO: Take in from properties file..


	      //new Thread(new runDemographTest(queryDetails, job)).start();
	      }
	   }

 } catch (Exception e){
           e.printStackTrace();
           log_writer.doLogging_QRmgr(logRoot, logName, "QRmanager.demographicsAssigner() error: "+e);
 }
 }


 //-----------------------------------------------------------------
 class runDemographTest implements Runnable {

 private Object[] queryDetails = null;
 BasicJob job = null;

 public runDemographTest(Object[] queryDetails, BasicJob job){

 this.queryDetails = queryDetails;
 this.job = job;
 }

 public void run(){

 new getDemographicsFromPACS(getThisObject(),
			                 this.queryDetails,
					         logRoot,
					         logName,
                             this.job).run();

 }
 }
 //------------------------------------------------------------------


 }






///////////////////////////////////////
//
//  Regulates demographics retrieving.
//
//////////////////////////////////////

class regulateDemographicsRetrieving  {

private Timer timer;

public regulateDemographicsRetrieving(){

this.timer = new Timer ();

}


public void doTask(int start_time, int delay_in_secs){

this.timer.scheduleAtFixedRate(new updateWorkerThread(),
                          (start_time * 1000),
                          (delay_in_secs * 1000));
}


public void stop(){

this.timer.cancel();
}


//worker class..
class updateWorkerThread extends TimerTask{

public void run(){

doBulkDemographicsUpdate();
}
}
}



 public static void main(String[] args){

 new QRmanager().commenceOp(args[0], args[1]);



/*

 new QRmanager().commenceOp("C:\\mike_folder\\codes\\ar_try\\config\\QR_properties.PROPERTIES",
                            "C:\\mike_folder\\codes\\ar_try\\config\\dicom_sopClass_transferSyntax.PROPERTIES");
                            */





 }

 }