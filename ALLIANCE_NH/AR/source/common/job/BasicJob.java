/********************************************
 *
 *   Author:  Mike Bassey
 *   Year:    2012
 *   Purpose: Holds common functionalities
 *            needed by all job objects.
 *
 ********************************************/

 package common.job;


 import java.util.concurrent.ArrayBlockingQueue;
 import java.util.concurrent.atomic.AtomicInteger;
 import java.util.concurrent.atomic.AtomicLong;
 import java.util.concurrent.atomic.AtomicBoolean;
 import java.util.Timer;
 import java.util.TimerTask;
 import java.io.File;
 import java.util.Vector;


 import org.dcm4che2.net.Association;
 import org.dcm4che2.net.log_writer;
 import org.dcm4che2.tool.dcmrcv.dcm_folderCreator;
 import common.manager.BasicManager;
 import utility.db_manipulation.insertIntoHistory;
 import utility.db_manipulation.updateJobHistoryTable;
 import utility.db_manipulation.insertIntoAtable;

 //new NH..
 import NH.NH_job;
 import org.dcm4che2.tool.dcmsnd.DcmSnd;



 public abstract class BasicJob {

 protected BasicManager bMgr = null;
 protected String job_id = null;
 protected String fileLocation = null;
 protected String first_known_cuid = null;
 protected String[] job_demographics = null;
 protected int noOfImagesDownloadedThusFar = 0;
 protected int noOfImagesUploadedThusFar = 0;
 protected Association assoc = null;
 protected String logRoot = null;
 protected String logName = null;
 protected String status = "";
 protected String fileStore = null;
 protected int failedMoveQueueSize = -1;
 protected AtomicInteger stored_moved_count = new AtomicInteger(0);
 protected int pendingQueueSize = -1;
 protected long currentTime = 0L;
 protected String currentStatus = null;
 protected long[] operationTimeStats = null;
 protected String jobDate = null;
 protected long connectionDuration = 0L;
 protected String transferMode = null;
 protected String[] dcmsndMainStats = null;
 protected String[] httpsMainStats = null;
 protected ArrayBlockingQueue<Object[]> pendingQueue = null;
 protected ArrayBlockingQueue<Object[]> failedMoveQueue = null;
 protected long totalBytesDownloaded = 0L;
 protected long totalBytesUploaded = 0L;
 protected boolean essentials_set = false;

 private AtomicLong totalQueuedTime = new AtomicLong(0);
 private AtomicLong totalPauseTime = new AtomicLong(0);
 private AtomicLong totalStopTime = new AtomicLong(0);
 private AtomicLong totalConnectionTime = new AtomicLong(0);
 private AtomicLong totalPendingTime = new AtomicLong(0);
 private AtomicLong totalAwaitingTime = new AtomicLong(0);

 private volatile boolean queuedTimeStarted = false;
 private volatile boolean pauseTimeStarted = false;
 private volatile boolean stopTimeStarted = false;
 private volatile boolean pendingTimeStarted = false;
 private volatile boolean awaitingTimeStarted = false;

 private volatile long currentQueuedTime = 0L;
 private volatile long currentPauseTime = 0L;
 private volatile long currentStopTime = 0L;
 private volatile long currentPendingTime = 0L;
 private volatile long currentAwaitingTime = 0L;


 public AtomicInteger noOfTimesRan = new AtomicInteger(0);
 private regulateUpdate regulator = null;
 public AtomicInteger currentRun = new AtomicInteger(0);
 protected int lastSentValue = 0;
 public AtomicInteger noOfTimesConnAttemptsMaxed = new AtomicInteger(0);
 public AtomicInteger noOfFilesRestacked = new AtomicInteger(0);
 public long totalBytesRestacked = 0L;

 private volatile boolean downloadInProgress = false;
 protected AtomicBoolean downloadCompleted = new AtomicBoolean(false);
 protected volatile String downloadResult = "not yet set";

 private AtomicLong humanRestack_runTimeThusFar = new AtomicLong(0);
 private AtomicLong humanRestack_queueTimeThusFar = new AtomicLong(0);
 private AtomicInteger humanRestack_noOfFilesDownloaded = new AtomicInteger(0);
 private AtomicInteger humanRestack_noOfFilesUploaded = new AtomicInteger(0);
 private AtomicLong humanRestack_totalBytesDownloaded = new AtomicLong(0);
 private AtomicLong humanRestack_totalBytesUploaded = new AtomicLong(0);

 private Object queueLOCK = new Object();
 private String starttime = "not yet set";
 private String endtime = "not yet set";

 private regulateJobHistoryUpdate jobHistTimer = null;
 private Object[] demographicsRetrievingInfo = null;
 public AtomicInteger CFIND_attempts_made = new AtomicInteger(0);
 private AtomicBoolean isDead = new AtomicBoolean(false);
 private Vector<String[]> dest_list = new Vector<String[]>(5);
 private AtomicBoolean markedAsJoined = new AtomicBoolean(false);
 private String AccessionNo = "";

 private NH_job nhJob = null;
 private String[] jobDetails = null;
 private Vector<DcmSnd> temp_sendObjects = new Vector<DcmSnd>(5);
 protected AtomicInteger jobType = new AtomicInteger(0);
 private String actualFileStore = null;

 private AtomicInteger dataWaitTimeThusFar = new AtomicInteger(0);


 public BasicJob(BasicManager bMgr,
	             String job_id,
                 String fileLocation,
                 String first_known_cuid,
                 Association assoc,
                 String logRoot,
                 String logName,
                 String fileStore,
                 int failedMoveQueueSize,
                 int pendingQueueSize){

 this.bMgr = bMgr;
 this.job_id = job_id;
 this.fileLocation = fileLocation;
 this.first_known_cuid = first_known_cuid;
 this.assoc = assoc;
 this.logRoot = logRoot;
 this.logName = logName;
 this.fileStore = fileStore;
 this.failedMoveQueueSize = failedMoveQueueSize;
 this.pendingQueueSize = pendingQueueSize;


 this.operationTimeStats = new long[3];
 for(int a = 0; a < this.operationTimeStats.length; a++)
    {
     this.operationTimeStats[a] = 0L;
    }

 try {
	  this.regulator = new regulateUpdate();
      this.regulator.doTask(0, BasicManager.uploadCountUpdateFrequency); //start timer..
	  this.jobDate = dcm_folderCreator.generate_imageStoreName();
	  this.failedMoveQueue = new ArrayBlockingQueue<Object[]>(this.failedMoveQueueSize);
	  this.pendingQueue = new ArrayBlockingQueue<Object[]>(this.pendingQueueSize);
 } catch (Exception e){
          e.printStackTrace();
          log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<BasicJob> constructor exception: "+e);
 }
 }



 public void set_actualFileStore(String actualFileStore)
 {
  this.actualFileStore = actualFileStore;
 }

 public String get_actualFileStore()
 {
  return this.actualFileStore;
 }

 public void setJobType(int val)
 {
  this.jobType.set(val);
 }

 public int getJobType()
 {
  return this.jobType.get();
 }

 public void set_detailsForDcmSnd(String[] jobDetails)
 {
  this.jobDetails = jobDetails;
 }

 public String[] get_jobDetailsForDcmSnd()
 {
  return this.jobDetails;
 }

 public void set_nhJob(NH_job nhJob)
 {
  this.nhJob = nhJob;
 }


 public NH_job get_nhJob()
 {
  return this.nhJob;
 }


 public void set_AccNo(String value){

 this.AccessionNo = value;
 }


 public String get_AccNo(){

 return this.AccessionNo;
 }


 public void set_joinStatus(boolean value){

 this.markedAsJoined.set(value);
 }


 public boolean hasJoined(){

 return this.markedAsJoined.get();
 }


 public DcmSnd get_dcmsnd(String cuid, String[] stats){

 DcmSnd snd = null;
 snd = this.findInTempDcmSnds(cuid, stats);
 return snd;
 }




 public DcmSnd findInTempDcmSnds(String cuid, String[] stats){

 DcmSnd obj = null;

 try{
    int pointer = 0;
    for(int a = 0; a < this.temp_sendObjects.size(); a++)
       {
	    DcmSnd snd = (DcmSnd) this.temp_sendObjects.get(pointer);
	    if(snd == null)
	      {
		   this.temp_sendObjects.remove(pointer);
		  }
	    else
	      {
		   if(snd.still_retrieving())
		     {
			  pointer++;
			 }

		   if(!(snd.still_retrieving()))
		     {
			  this.temp_sendObjects.remove(pointer);
			 }
		   else if((this.findSopClass(snd.get_supportedSopClasses(), cuid)) &&
		     (!(snd.socketIsClosed())) &&
		     (  snd.still_retrieving()))
		     {
			  String[] data = snd.get_mainStats();
			  if(data != null)
			    {
			     if((data[0].equals(stats[0]))&&
			       ( data[1].equals(stats[1]))&&
			       ( data[2].equals(stats[2])))
			       {
				    obj = snd;
			        break;
				   }
			    }
			 }
		  }
	   }
 } catch (Exception e){
          obj = null;
          e.printStackTrace();
          log_writer.doLogging_QRmgr(logRoot, logName, "BasicJob <findInTempDcmSnds> error: "+e);
 }
 return obj;
 }


 public boolean findSopClass(String[] sopclassList,String cuid){

 boolean found = false;
 try {
     for(int a = 0; a < sopclassList.length; a++)
        {
	     if(sopclassList[a].equals(cuid))
	       {
		    found = true;
		    break;
		   }
	    }
 } catch (Exception e){
         found = false;
         e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<BascicJob.findSopClass()> Exception: "+e);
 }
 return found;
 }


 public void addToTempDcmSnd(DcmSnd snd){

 try {
     this.temp_sendObjects.add(snd);
 } catch (Exception e){
	     e.printStackTrace();
	     log_writer.doLogging_QRmgr(logRoot, logName, "BascicJob <addToTempDcmSnd> error: "+e);
 }
 }


 public void terminate_allDcmsnd()
 {
  try
  {
   int size = temp_sendObjects.size();
   for(int a = 0; a < size; a++)
	  {
	   DcmSnd snd = temp_sendObjects.remove(0);
	   if(snd != null)
	     {
		  Object[] data = new Object[2];
		  data[0] = "END_IT";
		  data[1] = null;
		  snd.putInStore(data);
	     }
	  }
 } catch (Exception e){
	     e.printStackTrace();
	     log_writer.doLogging_QRmgr(logRoot, logName, "BascicJob <terminate_allDcmsnd> error: "+e);
 }
 }

 public void set_startOrEndtime( int type){

 try {
      if(type == 1)
        {
         this.starttime = dcm_folderCreator.get_currentDateAndTime();
	    }
	  else if(type == 2)
	    {
	     this.endtime = dcm_folderCreator.get_currentDateAndTime();
	    }
	  else
	    {
		 log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<BasicJob> set_starttime() msg: Invalid type "+type);
		}

 } catch (Exception e){
          e.printStackTrace();
          log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<BasicJob> set_starttime() exception: "+e);
 }
 }





 public void addNewAlias(String value, int count){

 synchronized(this.dest_list){
 try {
      if(value != null)
        {
         boolean addNewDest = true;
         String[] info = new String[2];
         info[0] = value;
         info[1] = Integer.toString(count);

         int size = this.dest_list.size();
         for(int a = 0; a < size; a++)
            {
		     String[] r_alias = this.dest_list.get(a);
		     if(r_alias != null)
		       {
			    log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<BasicJob> addNewAlias(2) msg: job id="+this.get_jobId()+", alias="+r_alias[0]);

			    if(r_alias[0].equals(value))
			      {
                   if(r_alias[1] != null)
				     {
				      int currentValue = Integer.parseInt(r_alias[1]);
				      int newValue = (currentValue + count);
				      r_alias[1] = Integer.toString(newValue);
				     }
				   else
				     {
					  r_alias[1] = Integer.toString(count);
					 }
				   addNewDest = false;
				   break;
				  }
			   }
		    }

		 if(addNewDest)
		   {
		    this.dest_list.add(info);

		    log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<BasicJob> addNewAlias(2) msg: New alias added: job id="+this.get_jobId()+", alias="+value);
		   }
        }
 } catch (Exception e){
          e.printStackTrace();
          log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<BasicJob> addNewAlias(2) exception: "+e);
 }
 }
 }



 public void incrementStoredCountForAlias(String alias, int value){

 try {
      if(alias != null)
        {
         boolean aliasFound = false;

         int size = this.dest_list.size();
         for(int a = 0; a < size; a++)
            {
		     String[] r_alias = this.dest_list.get(a);
		     if(r_alias != null)
		       {
			    if(r_alias[0].equals(alias))
			      {
				   aliasFound = true;
				   if(r_alias[1] != null)
				     {
				      int currentValue = Integer.parseInt(r_alias[1]);
				      int newValue = (currentValue + value);
				      r_alias[1] = Integer.toString(newValue);
				     }
				   else
				     {
					  r_alias[1] = Integer.toString(value);
					 }
				   break;
				  }
			   }
		    }

		 if(!aliasFound)
		   {
		    this.addNewAlias(alias, value);
		   }
        }
 } catch (Exception e){
          e.printStackTrace();
          log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<BasicJob> incrementStoredCountForAlias() exception: "+e);
 }
 }



 public Association get_Association(){

 return this.assoc;
 }

 public void set_jobDemographics(String[] job_demographics){

 this.job_demographics = job_demographics;
 }



 public synchronized void set_transferMode(String value){

 this.transferMode = value;
 }



 public void addTo_connectionTime(long value){
 try {
	  this.totalConnectionTime.addAndGet(value);
 } catch (Exception e){
          e.printStackTrace();
          log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<BasicJob> addTo_connectionTime() exception: "+e);
 }
 }



 public void set_demographicsRetrivingInfo(Object[] demographicsRetrievingInfo){

 this.demographicsRetrievingInfo = demographicsRetrievingInfo;
 }


 public Object[] get_demographicsRetrivingInfo(){

 return this.demographicsRetrievingInfo;
 }


 public String getValue(int type){

 String value = null;
 try {
     if(type == 1){
     if(this.queuedTimeStarted)
	   {
		long duration = (System.currentTimeMillis() - this.currentQueuedTime);
		this.totalQueuedTime.addAndGet(duration);
		this.currentQueuedTime = System.currentTimeMillis();
	   }
	 value = dcm_folderCreator.convertToDesiredTimeFormat(this.totalQueuedTime.get(), logRoot, logName);
     }

     if(type == 2){
	 if(this.pauseTimeStarted)
	   {
		long duration = (System.currentTimeMillis() - this.currentPauseTime);
		this.totalPauseTime.addAndGet(duration);
		this.currentPauseTime = System.currentTimeMillis();
	   }
	 value = dcm_folderCreator.convertToDesiredTimeFormat(this.totalPauseTime.get(), logRoot, logName);
     }


     if(type == 3){
	 if(this.stopTimeStarted)
	   {
		long duration = (System.currentTimeMillis() - this.currentStopTime);
		this.totalStopTime.addAndGet(duration);
		this.currentStopTime = System.currentTimeMillis();
	   }
	 value = dcm_folderCreator.convertToDesiredTimeFormat(this.totalStopTime.get(), logRoot, logName);
     }


     if(type == 4){
	 if(this.pendingTimeStarted)
	   {
		long duration = (System.currentTimeMillis() - this.currentPendingTime);
		this.totalPendingTime.addAndGet(duration);
		this.currentPendingTime = System.currentTimeMillis();
	   }
	 value = dcm_folderCreator.convertToDesiredTimeFormat(this.totalPendingTime.get(), logRoot, logName);
     }

     if(type == 5){
     if(this.awaitingTimeStarted)
	   {
		long duration = (System.currentTimeMillis() - this.currentAwaitingTime);
		this.totalAwaitingTime.addAndGet(duration);
		this.currentAwaitingTime = System.currentTimeMillis();
	   }
	 value = dcm_folderCreator.convertToDesiredTimeFormat(this.totalAwaitingTime.get(), logRoot, logName);
     }


 } catch (Exception e){
          e.printStackTrace();
          log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<BasicJob> getValue() exception: "+e);
 }
 return value;
 }



 public synchronized void set_status(String value){

 try {

	 if(!(this.isDead.get())) {

	 if(this.queuedTimeStarted)
	   {
		long duration = (System.currentTimeMillis() - this.currentQueuedTime);
		this.totalQueuedTime.addAndGet(duration);
	   }
	 if(this.pauseTimeStarted)
	   {
		long duration = (System.currentTimeMillis() - this.currentPauseTime);
		this.totalPauseTime.addAndGet(duration);
	   }

	 if(this.stopTimeStarted)
	   {
		long duration = (System.currentTimeMillis() - this.currentStopTime);
		this.totalStopTime.addAndGet(duration);
	   }

	 if(this.pendingTimeStarted)
	   {
		long duration = (System.currentTimeMillis() - this.currentPendingTime);
		this.totalPendingTime.addAndGet(duration);
	   }

     if(this.awaitingTimeStarted)
	   {
		long duration = (System.currentTimeMillis() - this.currentAwaitingTime);
		this.totalAwaitingTime.addAndGet(duration);
	   }


         this.status = value;

		 if(value.equals("queued"))
		   {
			this.currentQueuedTime = System.currentTimeMillis();

			this.queuedTimeStarted   = true;
			this.pauseTimeStarted    = false;
			this.stopTimeStarted     = false;
			this.pendingTimeStarted  = false;
			this.awaitingTimeStarted = false;
		   }
		 else if(value.equals("paused"))
		   {
			this.currentPauseTime = System.currentTimeMillis();

			this.queuedTimeStarted   = false;
			this.pauseTimeStarted    = true;
			this.stopTimeStarted     = false;
			this.pendingTimeStarted  = false;
			this.awaitingTimeStarted = false;
		   }
		 else if(value.equals("stopped"))
		   {
			this.currentStopTime = System.currentTimeMillis();

			this.queuedTimeStarted   = false;
			this.pauseTimeStarted    = false;
			this.stopTimeStarted     = true;
			this.pendingTimeStarted  = false;
			this.awaitingTimeStarted = false;
		   }
		 else if(value.equals("pending"))
		   {
			this.currentPendingTime = System.currentTimeMillis();

			this.queuedTimeStarted   = false;
			this.pauseTimeStarted    = false;
			this.stopTimeStarted     = false;
			this.pendingTimeStarted  = true;
			this.awaitingTimeStarted = false;
		   }
		 else if(value.equals("awaiting download"))
		   {
			this.currentAwaitingTime = System.currentTimeMillis();

			this.queuedTimeStarted   = false;
			this.pauseTimeStarted    = false;
			this.stopTimeStarted     = false;
			this.pendingTimeStarted  = false;
			this.awaitingTimeStarted = true;
		   }
		 else
		   {
			this.queuedTimeStarted   = false;
			this.pauseTimeStarted    = false;
			this.stopTimeStarted     = false;
			this.pendingTimeStarted  = false;
			this.awaitingTimeStarted = false;
		   }


     if(this.bMgr.check_1(this, logRoot, logName))
	   {
        this.queuedTimeStarted   = false;
	    this.pauseTimeStarted    = false;
		this.stopTimeStarted     = false;
	    this.pendingTimeStarted  = false;
	    this.awaitingTimeStarted = false;

		this.bMgr.jobsInSystem.decrementAndGet();
		this.bMgr.accNoInSystem.remove(this.get_AccNo());

		//------------------------------------------------------------------------------------
		log_writer.doLogging_QRmgr(this.logRoot, this.logName, "No of jobs in system: "+this.bMgr.accNoInSystem.size());
		//------------------------------------------------------------------------------------

		String DURATION  = dcm_folderCreator.addOrSubtractTime(this.starttime,
                                                               dcm_folderCreator.get_currentDateAndTime(),
                                                               2,
                                                               "yyyy-MM-dd hh:mm:ss a",
	                                                           this.logRoot,
                                                               this.logName); //duration..

		this.updateTable_uploadCount();
		this.regulator.stop();

		log_writer.doLogging_QRmgr(this.logRoot, this.logName, "");
		log_writer.doLogging_QRmgr(this.logRoot, this.logName, "job id: "+(this.get_jobId())+", job date: "+this.jobDate);
		log_writer.doLogging_QRmgr(this.logRoot, this.logName, "START TIME: "+this.starttime);

		this.set_startOrEndtime(2);
		log_writer.doLogging_QRmgr(this.logRoot, this.logName, "END TIME: "+this.endtime);

		log_writer.doLogging_QRmgr(this.logRoot, this.logName, "Total queued time (in secs): "+(this.totalQueuedTime.get()/1000));
		log_writer.doLogging_QRmgr(this.logRoot, this.logName, "Total time ran: "+(DURATION));
		log_writer.doLogging_QRmgr(this.logRoot, this.logName, "Total time paused (in secs): "+(this.totalPauseTime.get()/1000));
		log_writer.doLogging_QRmgr(this.logRoot, this.logName, "Total time stopped (in secs): "+(this.totalStopTime.get()/1000));
		log_writer.doLogging_QRmgr(this.logRoot, this.logName, "Total pending time (in secs): "+(this.totalPendingTime.get()/1000));
		log_writer.doLogging_QRmgr(this.logRoot, this.logName, "Total time spent waiting for download (in secs): "+(this.totalAwaitingTime.get()/1000));
		log_writer.doLogging_QRmgr(this.logRoot, this.logName, "Total connection time (in secs): "+(this.totalConnectionTime.get()/1000));
		log_writer.doLogging_QRmgr(this.logRoot, this.logName, "Number of times connection threshold breached: "+(this.noOfTimesConnAttemptsMaxed.get()));
		log_writer.doLogging_QRmgr(this.logRoot, this.logName, "Total number of files re-stacked: "+(this.noOfFilesRestacked.get()));
		log_writer.doLogging_QRmgr(this.logRoot, this.logName, "Total bytes re-stacked: "+(this.totalBytesRestacked/1000000)+"MB");

		int numRestarted = (this.noOfTimesRan.get() -1);
		if(numRestarted < 0){numRestarted = 0;}
		log_writer.doLogging_QRmgr(this.logRoot, this.logName, "Total no of times re-started: "+numRestarted);
		log_writer.doLogging_QRmgr(this.logRoot, this.logName, "Total images downloaded: "+this.noOfImagesDownloadedThusFar);
		log_writer.doLogging_QRmgr(this.logRoot, this.logName, "Total images uploaded: "+this.noOfImagesUploadedThusFar);
		log_writer.doLogging_QRmgr(this.logRoot, this.logName, "Total bytes downloaded: "+(this.getBytesDownloaded()/1000000)+"MB");
		log_writer.doLogging_QRmgr(this.logRoot, this.logName, "Total bytes uploaded: "+(this.getBytesUploaded()/1000000)+"MB");


		log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<Human reStack> Total run time (in secs): "+(this.humanRestack_runTimeThusFar.get()/1000));
		log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<Human reStack> Total queued time (in secs): "+(this.humanRestack_queueTimeThusFar.get()/1000));
		log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<Human reStack> Total number of files downloaded: "+(this.humanRestack_noOfFilesDownloaded.get()));
		log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<Human reStack> Total number of files uploaded: "+(this.humanRestack_noOfFilesUploaded.get()));
		log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<Human reStack> Total bytes downloaded: "+(this.humanRestack_totalBytesDownloaded.get()/1000000)+"MB");
		log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<Human reStack> Total bytes uploaded: "+(this.humanRestack_totalBytesUploaded.get()/1000000)+"MB");

        String sqlStmt = "INSERT INTO autorouter_jobhistory_destination (jobkey ,alias ,filenumber)VALUES(?,?,?)";

		int size = this.dest_list.size();
		for(int a = 0; a < size; a++)
		   {
		    String[] r_alias = this.dest_list.get(a);
		    if(r_alias != null)
		      {
			   log_writer.doLogging_QRmgr(this.logRoot, this.logName, "Destination =  "+r_alias[0]+", total no of files = "+r_alias[1]);

			   //----------------------------------------------
			   	 String[] argsForTable = new String[3];
			   	 argsForTable[0] = this.get_jobId();
			   	 argsForTable[1] = r_alias[0];
			   	 argsForTable[2] = r_alias[1];

			   	 //new insertIntoAtable (this.bMgr, this.logRoot,this.logName).doUpdate(sqlStmt, argsForTable);

				 Object[] args = new Object[4];
				 Integer opType = new Integer(8);

				 args[0] = opType;
				 args[1] = this.bMgr;
				 args[2] = argsForTable;
				 args[3] = sqlStmt;
				 this.bMgr.get_dbUpdateMgr().storeData(args);
	           //----------------------------------------------
			  }
	       }


		log_writer.doLogging_QRmgr(this.logRoot, this.logName, "");

		this.doUpdateJobHistoryTable();

        //-------------------
        this.terminate_allDcmsnd();
        //-------------------
		this.isDead.set(true);

		//----------------------------------
		//BasicManager.foldersToDelete.put(get_actualFileStore());
		new Thread(new storeForDeletion(get_actualFileStore())).start();
		//----------------------------------
	   }
     }

	 } catch (Exception e){
			  e.printStackTrace();
			  log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<BasicJob> set_status() exception: "+e);
	 }
 }






    //=========================================
	//  just so we do not block this object..
	//=========================================
	class storeForDeletion implements Runnable
	{
	 String fLocatn = null;

	 public storeForDeletion(String fLocatn)
	 {
	  this.fLocatn = fLocatn;
	 }

	 public void run()
	 {
	  try
	  {
	   BasicManager.foldersToDelete.put(this.fLocatn);
	  } catch (Exception e)
	  {
	    e.printStackTrace();
	    log_writer.doLogging_QRmgr(logRoot, logName, "DcmSnd.storeForDeletion() error: "+e);
	  }
	 }
    }






public void insertDataIntoJobHistoryTable(){

try {
     this.set_startOrEndtime(1);

     String[] data = new String[25];
     data[0] = this.get_jobId();
     data[1] = this.jobDate;
     data[2] = this.starttime;
     data[3] = this.endtime;
     data[4] = this.getValue(1); //queue time..
     data[5] = dcm_folderCreator.addOrSubtractTime(this.starttime,
                                                   dcm_folderCreator.get_currentDateAndTime(),
                                                   2,
                                                   "yyyy-MM-dd hh:mm:ss a",
	                                               this.logRoot,
                                                   this.logName); //duration..
     data[6] = this.getValue(2); //pause time..
     data[7] = this.getValue(3); //stop time..
     data[8] = this.getValue(4); //pending time..
     data[9] = this.getValue(5); //download await..
     data[10] = dcm_folderCreator.convertToDesiredTimeFormat(this.totalConnectionTime.get(), logRoot, logName); //connection time..
     data[11] =""+(this.noOfTimesConnAttemptsMaxed.get()); //max breach
     data[12] =""+(this.noOfFilesRestacked.get()); //no of files re-stacked
     data[13] =""+this.noOfImagesDownloadedThusFar; //files downloaded
     data[14] =""+this.noOfImagesUploadedThusFar; //files uploaded

     int numRestarted = (this.noOfTimesRan.get() -1);
     if(numRestarted < 0){numRestarted = 0;}

     data[15] =""+numRestarted; // no of times restarted..
     data[16] =""+(this.totalBytesRestacked/1000000)+" MB"; //total bytes restacked..
     data[17] =""+(this.getBytesDownloaded()/1000000)+" MB"; //total bytes downloaded..
     data[18] =""+(this.getBytesUploaded()/1000000)+" MB"; //total bytes uploaded..
     data[19] = dcm_folderCreator.convertToDesiredTimeFormat(this.humanRestack_runTimeThusFar.get(), logRoot, logName); //restack duration..
     data[20] = dcm_folderCreator.convertToDesiredTimeFormat(this.humanRestack_queueTimeThusFar.get(), logRoot, logName); //restack queue time..
     data[21] =""+(this.humanRestack_noOfFilesDownloaded.get()); //restack downloaded..
     data[22] =""+(this.humanRestack_noOfFilesUploaded.get()); //restack uploaded..
     data[23] =""+(this.humanRestack_totalBytesDownloaded.get()/1000000)+" MB";
     data[24] =""+(this.humanRestack_totalBytesUploaded.get()/1000000)+" MB";

     //new insertIntoHistory(logRoot, logName).startOp(data);

     Object[] argsForExe = new Object[3];
     Integer thisInt = new Integer(2);
     argsForExe[0] = thisInt;
     argsForExe[1] = this.bMgr;
     argsForExe[2] = data;

     this.bMgr.get_dbUpdateMgr().storeData(argsForExe);

} catch (Exception e){
         e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<BasicJob.insertDataIntoJobHistoryTable()> exception: "+e);
}
}



public void doUpdateJobHistoryTable(){

if(!(this.isDead.get())) {
try {
     String sqlstr = "update autorouter_nighthawk_jobhistory set endtime = ?, queuetime = ?, duration = ?, pausetime = ?, stoptime = ?, pendtime = ?, downloadwait = ?, connectiontime = ?, breachnumber = ?, filesrestacked = ?, filesdownloaded = ?, filesuploaded = ?, restarted = ?, bytesrestacked = ?, bytesdownloaded = ?, bytesuploaded = ?, restackduration = ?, restackqueuetime = ?, restackfilesdownloaded = ?, restackfilesuploaded = ?, restackbytesdownloaded = ?, restackbytesuploaded = ?  where jobkey = ?";

     String[] jobData = new String[23];

     String endTime = null;
     if(!(this.endtime.equals("not yet set")))
       {
	    endTime = this.endtime;
	   }
	 else
	   {
	    endTime = dcm_folderCreator.get_currentDateAndTime();
	   }

     jobData[0] = this.endtime;
     jobData[1] = this.getValue(1); //queue time..
     jobData[2] = dcm_folderCreator.addOrSubtractTime(this.starttime,
                                                      endTime,
                                                      2,
                                                      "yyyy-MM-dd hh:mm:ss a",
	                                                  this.logRoot,
                                                      this.logName); //duration..
     jobData[3] = this.getValue(2); //pause time..
     jobData[4] = this.getValue(3); //stop time..
     jobData[5] = this.getValue(4); //pending time..
     jobData[6] = this.getValue(5); //awaiting download..
     jobData[7] = dcm_folderCreator.convertToDesiredTimeFormat(this.totalConnectionTime.get(), logRoot, logName); //connection time..
     jobData[8] =""+(this.noOfTimesConnAttemptsMaxed.get()); //max breach
     jobData[9] =""+(this.noOfFilesRestacked.get()); //no of files re-stacked
     jobData[10] =""+this.noOfImagesDownloadedThusFar; //files downloaded
     jobData[11] =""+this.noOfImagesUploadedThusFar; //files uploaded

     int numRestarted = (this.noOfTimesRan.get() -1);
     if(numRestarted < 0){numRestarted = 0;}

     jobData[12] =""+numRestarted; // no of times restarted..
     jobData[13] =""+(this.totalBytesRestacked/1000000)+" MB"; //total bytes restacked..
     jobData[14] =""+(this.getBytesDownloaded()/1000000)+" MB"; //total bytes downloaded..
     jobData[15] =""+(this.getBytesUploaded()/1000000)+" MB"; //total bytes uploaded..
     jobData[16] = dcm_folderCreator.convertToDesiredTimeFormat(this.humanRestack_runTimeThusFar.get(), logRoot, logName); //restack duration..
     jobData[17] = dcm_folderCreator.convertToDesiredTimeFormat(this.humanRestack_queueTimeThusFar.get(), logRoot, logName); //restack queue time..
     jobData[18] =""+(this.humanRestack_noOfFilesDownloaded.get()); //restack downloaded..
     jobData[19] =""+(this.humanRestack_noOfFilesUploaded.get()); //restack uploaded..
     jobData[20] =""+(this.humanRestack_totalBytesDownloaded.get()/1000000)+" MB";
     jobData[21] =""+(this.humanRestack_totalBytesUploaded.get()/1000000)+" MB";
     jobData[22] = this.get_jobId();

	 //new updateJobHistoryTable(logRoot,logName).doUpdate(sqlstr, jobData);

	 Object[] argsForExe = new Object[4];
	 Integer thisInt = new Integer(5);
	 argsForExe[0] = thisInt;
	 argsForExe[1] = this.bMgr;
     argsForExe[2] = jobData;
     argsForExe[3] = sqlstr;

     this.bMgr.get_dbUpdateMgr().storeData(argsForExe);

} catch (Exception e){
         e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<BasicJob.doUpdateJobHistoryTable()> exception: "+e);
}
}
}


 public void set_imagesUploaded(int val){

 this.noOfImagesUploadedThusFar = val;
 }


 public void increment_imagesUploaded(int val){

 this.noOfImagesUploadedThusFar += val;
 }


 public long[] get_operationStats(){

 return this.operationTimeStats;
 }


 public synchronized void set_dcmsndMainStats(String[] value){

 this.dcmsndMainStats = value;
 }

 public synchronized String[] get_dcmsndMainStats(){

 return this.dcmsndMainStats;
 }


 public synchronized void set_httpsMainStats(String[] value){

 this.httpsMainStats = value;
 }

 public synchronized String[] get_httpsMainStats(){

 return this.httpsMainStats;
 }

 public synchronized String get_status(){

 return this.status;
 }

 public synchronized String get_transferMode(){

 return this.transferMode;
 }


 public String get_jobId(){

 return this.job_id;
 }

 public String[] get_jobDemgraphics(){

 return this.job_demographics;
 }

 public String get_fileLocation(){

 return this.fileLocation;
 }

 public String get_first_known_cuid(){

 return this.first_known_cuid;
 }


 public String get_storeLocation(){

 return this.fileStore;
 }

 public void increment_imagesDownloaded(int val){

 this.noOfImagesDownloadedThusFar += val;
 }


 public void incrementBytesDownloaded(long value){

 this.totalBytesDownloaded += value;
 }


 public void incrementBytesUploaded(long value){

 this.totalBytesUploaded += value;
 }

 public int get_imagesDownloaded(){

 return this.noOfImagesDownloadedThusFar;
 }


 public int get_imagesUploaded(){

 return this.noOfImagesUploadedThusFar;
 }


 public long getBytesDownloaded(){

 return this.totalBytesDownloaded;
 }


 public long getBytesUploaded(){

 return this.totalBytesUploaded;
 }


 public void increment_stored_moved(int val){

 this.stored_moved_count.incrementAndGet();
 }


 public void decrement_stored_moved(int val){

 this.stored_moved_count.decrementAndGet();
 }


 public int get_stored_moved(){

 return this.stored_moved_count.get();
 }

 public void storeInFailedQueue(Object[] data){

 try {
       this.failedMoveQueue.put(data);
 } catch (Exception e){
          e.printStackTrace();
          log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<BasicJob.storeInFailedQueue()> exception: "+e);
 }
 }

 public Object[] takeFromFailedQueue(){

 Object[] data = null;
 try {
      data = this.failedMoveQueue.poll();
 } catch (Exception e){
          data = null;
          e.printStackTrace();
          log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<BasicJob.takeFromFailedQueue()> exception: "+e);
 }
 return data;
 }



 public void storeInPendingQueue(Object[] data){

 try {
      synchronized(this.queueLOCK)
       {
        if(!this.pendingQueue.contains(data))
          {
           int currentValue = this.currentRun.get();
		   Integer intObj = (Integer) data[2];
		   int storedValue = intObj.intValue();
		   if(currentValue == storedValue)
		   	 {
              this.pendingQueue.put(data);
		     }
	      }
       }
 } catch (Exception e){
          e.printStackTrace();
          log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<BasicJob.storeInFailedQueue()> exception: "+e);
 }
 }


 public Object[] takeFromPendingQueue(){

 Object[] data = null;
 try {
      synchronized(this.queueLOCK)
       {
        data = this.pendingQueue.poll();
       }
 } catch (Exception e){
          data = null;
          e.printStackTrace();
          log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<BasicJob.takeFromFailedQueue()> exception: "+e);
 }
 return data;
 }


 public ArrayBlockingQueue get_pendingQueueObject(){

 return this.pendingQueue;
 }


 public int get_pendingQueueMaxSize(){

 return this.pendingQueueSize;
 }



 public void set_essentials(boolean value){

 this.essentials_set = value;
 }


 public boolean essentialsSet(){

 return this.essentials_set;
 }



 public synchronized void set_fileLocation(String loc){

 this.fileLocation = loc;
 }

 //============================================================
 //     Data Wait time...
 public void addTo_dataWaitTime(int value)
 {
  dataWaitTimeThusFar.addAndGet(value);
 }

 public void set_dataWaitTime(int value)
 {
  dataWaitTimeThusFar.set(value);
 }

 public int getFrom_dataWaitTime()
 {
  return dataWaitTimeThusFar.get();
 }

 //============================================================


 public void resetAll(){

 synchronized(this.queueLOCK)
  {
   this.humanRestack_runTimeThusFar.set(dcm_folderCreator.addOrSubtractTimeAsLong(this.starttime,
                                                               					  dcm_folderCreator.get_currentDateAndTime(),
                                                               					  2,
                                                               				      "yyyy-MM-dd hh:mm:ss a",
	                                                           					  this.logRoot,
                                                               					  this.logName));
   this.humanRestack_queueTimeThusFar.set(this.totalQueuedTime.get());
   this.humanRestack_noOfFilesDownloaded.addAndGet(this.noOfImagesDownloadedThusFar);
   this.humanRestack_noOfFilesUploaded.addAndGet(this.noOfImagesUploadedThusFar);
   this.humanRestack_totalBytesDownloaded.addAndGet(this.totalBytesDownloaded);
   this.humanRestack_totalBytesUploaded.addAndGet(this.totalBytesUploaded);

   stored_moved_count = new AtomicInteger(0);
   this.noOfImagesDownloadedThusFar = 0;
   this.noOfImagesUploadedThusFar = 0;
   this.assoc = null;
   this.currentTime = 0L;

   this.queuedTimeStarted = false;
   this.pauseTimeStarted = false;
   this.stopTimeStarted = false;
   this.pendingTimeStarted = false;
   this.awaitingTimeStarted = false;
   this.currentQueuedTime = 0L;
   this.currentPauseTime = 0L;
   this.currentStopTime = 0L;
   this.currentPendingTime = 0L;
   this.currentAwaitingTime = 0L;
   this.pendingQueue.clear();
   this.failedMoveQueue.clear();
   this.set_essentials(false);
   this.currentRun.incrementAndGet();
   this.lastSentValue = 0;
  }
 }



public void start_jobHistoryUpdates(){

/*
try {
     this.jobHistTimer = new regulateJobHistoryUpdate();
     this.jobHistTimer.doTask(0, BasicManager.updateJobHistFrequency); //start timer..

} catch (Exception e){
         e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<BasicJob.start_jobHistoryUpdates()> exception: "+e);
}
*/
}



public BasicManager getManager(){

return this.bMgr;
}



public void write_fileAnalysis(String sTime, String eTime, Object[] data){

try {
     String fileName = (String) data[0];
     long FileLength =  (new File(fileName)).length();

     String duration = dcm_folderCreator.addOrSubtractTime(sTime,
	                                     				   eTime,
	                                     				   2,
	                                    				   "yyyy-MM-dd hh:mm:ss a",
	 	                                				   this.logRoot,
                                        				   this.logName);

    long timeTaken = dcm_folderCreator.addOrSubtractTimeAsLong(sTime,
	                                     				       eTime,
	                                     				       2,
	                                    				       "yyyy-MM-dd hh:mm:ss a",
	 	                                				       this.logRoot,
                                        				       this.logName);

    String transferSpeed = calc_transferSpeed(FileLength, timeTaken);

    //INSERT INTO autorouter_jobhistory_files (jobkey ,filename ,filelocation ,fileduration ,filesize ,filespeed ,filestart ,filefinish)
    //VALUES('6532', 'a.dcm', 'c:telerad', '1 secs', '2MB', '16 mbps', '16:00:01', '16:00:02');

    //----------------------------------------------

    if(transferSpeed != null)
      {
		String sqlStmt = "INSERT INTO autorouter_jobhistory_files (jobkey, filename, fileduration, filesize, filespeed, filestart, filefinish)VALUES(?,?,?,?,?,?,?)";
		String[] argsForTable = new String[7];
		argsForTable[0] = this.get_jobId();
		argsForTable[1] = fileName;
		argsForTable[2] = duration;
		argsForTable[3] = (FileLength/1024)+"KB";
		argsForTable[4] = transferSpeed+ "kbits/sec";
		argsForTable[5] = sTime;
		argsForTable[6] = eTime;


		Object[] args = new Object[4];
		Integer opType = new Integer(8);

		args[0] = opType;
		args[1] = this.bMgr;
		args[2] = argsForTable;
		args[3] = sqlStmt;
		this.bMgr.get_dbUpdateMgr().storeData(args);
      }
	//----------------------------------------------

} catch (Exception e){
         e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<BasicJob.write_fileAnalysis()> exception: "+e);
}
}




private String calc_transferSpeed(long b_read, long t_taken){

String bwSpeed = null;
try {
     boolean continueOp = true;
     long d_trans = 0;
     long t_time = 0;
     long d_rate = 0;
     long c_drate = 0;

     d_trans = (b_read * 8); //in bits
     if(d_trans <= 0){continueOp = false;}

     if(continueOp)
       {
        t_time  = ((t_taken/1000)); //in secs
        if(t_time <= 0){continueOp = false;}
       }

     if(continueOp)
       {
        d_rate  = (d_trans/t_time); //in bits/sec
        if(d_rate <= 0){continueOp = false;}
       }

     if(continueOp)
       {
        d_rate = (d_rate / 1000); //Kbits/sec
        c_drate =  d_rate;
        bwSpeed = new Long(c_drate).toString();
       }

} catch (Exception e){
         bwSpeed = null;
         e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logRoot, this.logName,
         "<BasicJob.calc_transferSpeed()> exception: "+e);
}
return bwSpeed;
}





///////////////////////////////////////
//
//  Regulates table update frequency.
//
//////////////////////////////////////

class regulateUpdate  {

private Timer timer;

public regulateUpdate(){

this.timer = new Timer ();

}


public void doTask(int start_time, int delay_in_secs){

this.timer.scheduleAtFixedRate(new regulateUpdate_worker(),
                          (start_time * 1000),
                          (delay_in_secs * 1000));
}


public void stop(){

this.timer.cancel();
}


//worker class..
class regulateUpdate_worker extends TimerTask{

public void run(){

updateTable_uploadCount();
}
}
}



///////////////////////////////////////
//
//  Regulates table update frequency.
//
//////////////////////////////////////

class regulateJobHistoryUpdate  {

private Timer timer;

public regulateJobHistoryUpdate(){

this.timer = new Timer ();

}


public void doTask(int start_time, int delay_in_secs){

this.timer.scheduleAtFixedRate(new regulateJobHistUpdate_worker(),
                          (start_time * 1000),
                          (delay_in_secs * 1000));
}


public void stop(){

this.timer.cancel();
}


//worker class..
class regulateJobHistUpdate_worker extends TimerTask{

public void run(){

doUpdateJobHistoryTable();
}
}
}



public void markDownloadStart(boolean value){

this.downloadInProgress = value;
}


public boolean downloading(){

return this.downloadInProgress;
}



public abstract boolean referralCardRetrieved();


public abstract void updateTable_uploadCount();


public abstract boolean isRelegated();


public abstract void set_downloadStatus(boolean value);


public abstract void set_downloadResult(String value);


public abstract boolean downloadIsComplete();


public abstract String get_downloadResult();


}