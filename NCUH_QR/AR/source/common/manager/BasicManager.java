/**********************************************

 Author:  Mike Bass
 Year:    2012
 Purpose: Holds common functionalities needed
          by manager modules.

 **********************************************/

 package common.manager;


 import java.io.FileInputStream;
 import java.io.File;
 import java.util.Properties;
 import java.util.Vector;
 import java.util.concurrent.atomic.AtomicInteger;


 import org.dcm4che2.tool.dcmsnd.DcmSnd;
 import utility.general.DcmSnd_starter;
 import java.util.concurrent.ArrayBlockingQueue;
 import org.dcm4che2.net.log_writer;
 import utility.db_manipulation.updateNightHawk;
 import utility.db_manipulation.doBulkUpdate;
 import https.https_clientManager;
 import common.job.BasicJob;
 import common.job.JobExecutor;
 import common.job.JobExecutionMgr;
 import common.job.dbUpdateMgr;




 public abstract class BasicManager {


 protected String logRoot = null;
 protected String logName = null;
 private int logLevel= -1;
 private String keepAliveFile = null;
 private String scanFile = null;
 private int alivenessFrequency = -1;
 private String sourceServerIp = null;
 protected int sourceServerPort = -1;
 protected String callingAET = null;
 private int dcmsnd_max_destination = -1;
 private String[][] dcmsnd_dest_serverDetails = null;
 private int https_max_destination = -1;
 private String[][] https_dest_serverDetails = null;
 private int dcmsnd_destination_default = -1;
 private int https_destination_default = -1;
 private int No_of_connections = -1;
 private AR_dcmsnd[] sendObjects = null;
 protected int dataHandler_snoozing = -1;
 protected int dataHandler_cannotMove = -1;
 private int dcmsnd_fileLocations_queueSize = -1;
 private static int jobIDcount = 0;
 private static int common_No_of_ts = -1;
 private static String[] common_ts = null;
 private static int fileCounter = 0;
 protected ArrayBlockingQueue<BasicJob> arrivingJobs = null;
 protected ArrayBlockingQueue<BasicJob> currentlyRunningJobs = null;
 private int arrivingJobQueueSize = -1;
 private int currentlyRunningJobQueueSize = -1;
 protected int failedMoveQueueSize = -1;
 private Vector<DcmSnd> temp_sendObjects = new Vector<DcmSnd>(5);
 public static int completionCheckSleepTime = -1;
 protected JobExecutor nHawkJobController = null;
 private int maxNoOfExecutingJobs = -1;
 private int pauseTimeBetweenChecks = -1;
 private JobExecutionMgr nHawkExecMgr = null;
 private String alivenessFileSopClass = null;
 public static int jobExecCleanUpTime = -1;
 private Object QUEUE_LOCK = new Object();
 protected String webServerIp = null;
 protected int webServerPort = -1;
 protected int webServerBackLog = -1;
 protected int pendingQueueSize = -1;
 public int connRetryPauseTime = -1;
 protected int tempConnCheckFrequency = -1;
 private String terminate_tempConnections = null;
 private String currentTransferMode = null;
 private int transferType = -1;
 private https_clientManager[] httpsJobControllers = null;
 private int noOfClients_https = -1;
 private int queueSize_https = -1;
 private int allBusyPauseTime_https = -1;
 private String machineID_https = null;
 private int imgType_https = -1;
 private int chunkBufferSize_https = -1;
 protected String[] rStrings = null;
 protected String[] rStrings_2 = null;
 protected Vector<String[]> All_destinations = new Vector<String[]>(5);
 public String[] notWantedTagValues = null;
 public int demograhphicRetrieval_maxAttempts = -1;
 public int demograhphicRetrieval_pauseInbetweenAttempts = 1;
 public String xmlFileHeader = null;
 public String xmlFile_braceName = null;
 public int max_allowableIndividualDcmSnd = -1;
 public AtomicInteger individualDcmSndCreated = new AtomicInteger(0);
 private Object[] baseConnections = null;
 public static int uploadCountUpdateFrequency = -1;
 public int maxConnRetryAttempts = -1;
 public AtomicInteger jobsInSystem = new AtomicInteger(0);
 public Object STORE_LOCK = new Object();
 public boolean doAmissionHere = false;
 private int relegatedJobQueueSize = -1;
 public ArrayBlockingQueue<BasicJob> relegatedJobs = null;
 protected String file_extracts = null;
 protected String extract_fileName = null;
 protected int nightHawk_no_of_tags = -1;
 protected String[] nightHawkTags = null;
 public static String[][] forPACSquery = new String[12][2];
 public static int updateJobHistFrequency = -1;
 public static int maxDemographicsRetrievalAttempts_CFIND = 7;
 public Vector<String> accNoInSystem = new Vector<String>(5);
 protected Object joiningLOCK = new Object();
 protected int dbUpdates_corePoolSize = -1;
 protected int dbUpdates_maxPoolSize = -1;
 protected int dbUpdates_idleTime = -1;
 protected int dbUpdates_queueSize = -1;
 protected int dbUpdates_cmmdQueue = -1;
 protected dbUpdateMgr dbUpdateMGR = null;
 public String focusedJobId = null;
 protected int focusedIDupdateFreq = -1;
 protected clearanceManager CLR_MGR = null;
 protected lateArrivalScanner L_Scanner = null;
 protected int clearanceQueue = -1;
 protected int maxCheckTime_clearance = -1;
 protected String clearanceStore = null;
 public boolean diskMapped = false;
 protected String diskMapCmd = null;
 public int diskMap_No_of_desiredMsg = -1;
 public String[] diskMapMsgs = null;


 public static String DB_ROOT = null;
 public static String aliveFileIuid = null;
 private int managerType = 0;
 protected String outstandingFiles = null;


 protected String out_aet = null;
 protected String out_ip = null;
 protected String outPacsAet = null;
 protected String outPacsIp = null;
 protected int outPacsPort = -1;

 protected String crashHandlerCmd = null;
 protected static String netStatPidCmd = null;
 protected static String netStatFileLocation = null;
 protected static String pidDBupdateCmd = null;
 protected static String instid = null;
 protected static String restoreCmd = null;

 public static int QUERY_TYPE = 0;
 public static int ACCEPT_TO = 0;
 public static String SITE_ID = null;




 public BasicManager(){}



 public void startOp(String propertiesFile, String sc_ts_properties){

 try {
		BasicManager.forPACSquery[0][0] = "patient id";           BasicManager.forPACSquery[0][1] = "00100020";
		BasicManager.forPACSquery[1][0] = "patient name";         BasicManager.forPACSquery[1][1] = "00100010";
		BasicManager.forPACSquery[2][0] = "DOB";                  BasicManager.forPACSquery[2][1] = "00100030";
		BasicManager.forPACSquery[3][0] = "sex";                  BasicManager.forPACSquery[3][1] = "00100040";
		BasicManager.forPACSquery[4][0] = "Accession No";         BasicManager.forPACSquery[4][1] = "00080050";
		BasicManager.forPACSquery[5][0] = "Study Desc";           BasicManager.forPACSquery[5][1] = "00081030";
		BasicManager.forPACSquery[6][0] = "Modality";             BasicManager.forPACSquery[6][1] = "00080061";
		BasicManager.forPACSquery[7][0] = "Study date";           BasicManager.forPACSquery[7][1] = "00080020";
		BasicManager.forPACSquery[8][0] = "Study time";           BasicManager.forPACSquery[8][1] = "00080030";
		BasicManager.forPACSquery[9][0] = "No of images";         BasicManager.forPACSquery[9][1] = "00201208";
		BasicManager.forPACSquery[10][0] = "Referring clinician"; BasicManager.forPACSquery[10][1] = "00080090";
		BasicManager.forPACSquery[11][0] = "Study Instance UID";  BasicManager.forPACSquery[11][1] = "0020000D";

		Properties prop = new Properties();
		FileInputStream fis = new FileInputStream(propertiesFile);
		prop.load(fis);

		this.logRoot = prop.getProperty("logRoot");
		this.logName = prop.getProperty("logName");
		this.logLevel = Integer.parseInt(prop.getProperty("logLevel"));
		this.keepAliveFile = prop.getProperty("keepAliveFile");
		this.scanFile = prop.getProperty("scanFile");
		this.alivenessFrequency = Integer.parseInt(prop.getProperty("alivenessFrequency"));
		this.dataHandler_snoozing = Integer.parseInt(prop.getProperty("dataHandler_snoozing"));
		this.dataHandler_cannotMove = Integer.parseInt(prop.getProperty("dataHandler_cannotMove"));
		this.sourceServerIp = prop.getProperty("sourceServerIp");
		this.sourceServerPort = Integer.parseInt(prop.getProperty("sourceServerPort"));
		this.callingAET = prop.getProperty("callingAET");
		this.dcmsnd_fileLocations_queueSize = Integer.parseInt(prop.getProperty("dcmsnd_fileLocations_queueSize"));
        this.arrivingJobQueueSize = Integer.parseInt(prop.getProperty("arrivingJobQueueSize"));
        this.currentlyRunningJobQueueSize = Integer.parseInt(prop.getProperty("currentlyRunningJobQueueSize"));
        this.failedMoveQueueSize = Integer.parseInt(prop.getProperty("failedMoveQueueSize"));
        BasicManager.completionCheckSleepTime = Integer.parseInt(prop.getProperty("completionCheckSleepTime"));
        this.maxNoOfExecutingJobs = Integer.parseInt(prop.getProperty("maxNoOfExecutingJobs"));
        this.pauseTimeBetweenChecks = Integer.parseInt(prop.getProperty("pauseTimeBetweenChecks"));
        BasicManager.jobExecCleanUpTime = Integer.parseInt(prop.getProperty("jobExecCleanUpTime"));
        this.webServerIp = prop.getProperty("webServerIp");
		this.webServerPort = Integer.parseInt(prop.getProperty("webServerPort"));
		this.webServerBackLog = Integer.parseInt(prop.getProperty("webServerBackLog"));
        this.pendingQueueSize = Integer.parseInt(prop.getProperty("pendingQueueSize"));
        this.connRetryPauseTime = Integer.parseInt(prop.getProperty("connRetryPauseTime"));
        this.tempConnCheckFrequency = Integer.parseInt(prop.getProperty("tempConnCheckFrequency"));
        this.terminate_tempConnections = prop.getProperty("terminate_tempConnections");
        this.currentTransferMode = prop.getProperty("currentTransferMode");
        this.transferType = Integer.parseInt(prop.getProperty("transferType"));
        this.noOfClients_https = Integer.parseInt(prop.getProperty("noOfClients_https"));
        this.queueSize_https = Integer.parseInt(prop.getProperty("queueSize_https"));
        this.allBusyPauseTime_https = Integer.parseInt(prop.getProperty("allBusyPauseTime_https"));
        this.machineID_https = prop.getProperty("machineID_https");
		this.imgType_https = Integer.parseInt(prop.getProperty("imgType_https"));
		this.chunkBufferSize_https = Integer.parseInt(prop.getProperty("chunkBufferSize_https"));
        this.dcmsnd_max_destination = Integer.parseInt(prop.getProperty("dcmsnd_max_destination"));
		this.https_max_destination = Integer.parseInt(prop.getProperty("https_max_destination"));
		this.dcmsnd_destination_default = Integer.parseInt(prop.getProperty("dcmsnd_destination_default"));
		this.https_destination_default = Integer.parseInt(prop.getProperty("https_destination_default"));
	    int noOfNotWantedTagValue = Integer.parseInt(prop.getProperty("noOfNotWantedTagValue"));
	    this.demograhphicRetrieval_maxAttempts = Integer.parseInt(prop.getProperty("demograhphicRetrieval_maxAttempts"));
        this.demograhphicRetrieval_pauseInbetweenAttempts = Integer.parseInt(prop.getProperty("demograhphicRetrieval_pauseInbetweenAttempts"));
        this.max_allowableIndividualDcmSnd = Integer.parseInt(prop.getProperty("max_allowableIndividualDcmSnd"));
        this.uploadCountUpdateFrequency = Integer.parseInt(prop.getProperty("uploadCountUpdateFrequency"));
        this.maxConnRetryAttempts = Integer.parseInt(prop.getProperty("maxConnRetryAttempts"));
        this.relegatedJobQueueSize = Integer.parseInt(prop.getProperty("relegatedJobQueueSize"));
        this.file_extracts = prop.getProperty("file_extracts");
		this.extract_fileName = prop.getProperty("extract_fileName");
        this.nightHawk_no_of_tags = Integer.parseInt(prop.getProperty("nightHawk_no_of_tags"));
        BasicManager.updateJobHistFrequency = Integer.parseInt(prop.getProperty("updateJobHistFrequency"));
        //BasicManager.maxDemographicsRetrievalAttempts_CFIND = Integer.parseInt(prop.getProperty("maxDemographicsRetrievalAttempts_CFIND"));
		this.dbUpdates_corePoolSize = Integer.parseInt(prop.getProperty("dbUpdates_corePoolSize"));
		this.dbUpdates_maxPoolSize = Integer.parseInt(prop.getProperty("dbUpdates_maxPoolSize"));
		this.dbUpdates_idleTime = Integer.parseInt(prop.getProperty("dbUpdates_idleTime"));
		this.dbUpdates_queueSize = Integer.parseInt(prop.getProperty("dbUpdates_queueSize"));
        this.dbUpdates_cmmdQueue = Integer.parseInt(prop.getProperty("dbUpdates_cmmdQueue"));
        this.focusedIDupdateFreq = Integer.parseInt(prop.getProperty("focusedIDupdateFreq"));
        this.clearanceQueue = Integer.parseInt(prop.getProperty("clearanceQueue"));
        this.maxCheckTime_clearance = Integer.parseInt(prop.getProperty("maxCheckTime_clearance"));
        this.clearanceStore = prop.getProperty("clearanceStore");

        BasicManager.DB_ROOT = prop.getProperty("DB_ROOT");
        BasicManager.aliveFileIuid = prop.getProperty("aliveFileIuid");

        BasicManager.netStatPidCmd = prop.getProperty("netStatPidCmd");
        BasicManager.netStatFileLocation = prop.getProperty("netStatFileLocation");
        BasicManager.pidDBupdateCmd = prop.getProperty("pidDBupdateCmd");
        BasicManager.instid = prop.getProperty("instid");
        BasicManager.SITE_ID = prop.getProperty("SITE_ID");

		if( (this.logRoot == null)||
			(this.logName == null)||
			(this.logLevel < 0)||
			(this.keepAliveFile == null)||
			(this.scanFile == null)||
			(this.alivenessFrequency < 0)||
			(this.dataHandler_snoozing < 0)||
			(this.dataHandler_cannotMove < 0)||
			(this.sourceServerIp == null)||
			(this.sourceServerPort <= -1)||
			(this.callingAET == null)||
			(this.dcmsnd_fileLocations_queueSize <= 0)||
			(this.arrivingJobQueueSize <= 0)||
			(this.currentlyRunningJobQueueSize <= 0)||
			(this.failedMoveQueueSize <= 0)||
			(BasicManager.completionCheckSleepTime < 0)||
			(this.maxNoOfExecutingJobs < 1)||
			(this.pauseTimeBetweenChecks < 0)||
			(BasicManager.jobExecCleanUpTime <= 0)||
			(this.webServerIp == null)||
            (this.webServerPort < 0)||
            (this.webServerBackLog < 0)||
            (this.pendingQueueSize <= 0)||
            (this.connRetryPauseTime < 0)||
            (this.tempConnCheckFrequency < 0)||
            (this.terminate_tempConnections == null)||
            (this.currentTransferMode == null)||
            (this.transferType < 0)||
            (this.noOfClients_https <= 0)||
            (this.queueSize_https <= 0)||
            (this.allBusyPauseTime_https < 0)||
            (this.imgType_https < 1)||
            (this.dcmsnd_max_destination < 0)||
            (this.https_max_destination < 0)||
            (this.dcmsnd_destination_default < 0)||
            (this.https_destination_default < 0)||
            (noOfNotWantedTagValue < 0)||
            (this.demograhphicRetrieval_maxAttempts < 0)||
            (this.demograhphicRetrieval_pauseInbetweenAttempts < 0)||
            (this.max_allowableIndividualDcmSnd < 0)||
            (BasicManager.uploadCountUpdateFrequency < 0)||
            (this.maxConnRetryAttempts < 0)||
            (this.relegatedJobQueueSize <= 0)||
            (this.file_extracts == null)||
            (this.extract_fileName == null)||
            (this.nightHawk_no_of_tags < 0)||
            (BasicManager.updateJobHistFrequency < 0)||
            (BasicManager.maxDemographicsRetrievalAttempts_CFIND < 0)||
            (this.dbUpdates_corePoolSize < 0)||
		    (this.dbUpdates_maxPoolSize < 0)||
		    (this.dbUpdates_idleTime < 0)||
		    (this.dbUpdates_queueSize < 0)||
            (this.dbUpdates_cmmdQueue < 0)||
            (this.focusedIDupdateFreq < 0)||
            (this.clearanceQueue < 0)||
            (this.maxCheckTime_clearance < 0)||
            (BasicManager.DB_ROOT == null)||
            (BasicManager.aliveFileIuid == null)||
            (this.clearanceStore == null)||
            (BasicManager.netStatFileLocation == null)||
            (BasicManager.netStatPidCmd == null)||
            (BasicManager.pidDBupdateCmd == null)||
            (BasicManager.instid == null)||
            (BasicManager.SITE_ID == null))
		    {
		     System.out.println("<BasicManager> startOp() error: Invalid values got from properties file 1!");
		     log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<BasicManager> startOp() error: Invalid values got from properties file 1!");
		     System.exit(0);
		    }



       this.dbUpdateMGR = new dbUpdateMgr(this.logRoot,
                                          this.logName,
                                          this.dbUpdates_corePoolSize,
                                          this.dbUpdates_maxPoolSize,
                                          this.dbUpdates_idleTime,
                                          this.dbUpdates_queueSize,
                                          this.dbUpdates_cmmdQueue);
       new Thread(this.dbUpdateMGR).start();


       this.nightHawkTags = new String[this.nightHawk_no_of_tags];
	   for(int a = 0; a < this.nightHawkTags.length; a++)
		  {
		   this.nightHawkTags[a] = prop.getProperty("nightHawk_tag_"+a);
		  }


	    try {
	         this.notWantedTagValues = new String[noOfNotWantedTagValue];
	         for(int m = 0; m < this.notWantedTagValues.length; m++)
	            {
				 this.notWantedTagValues[m] = prop.getProperty("notWantedValues_"+m);
				}

	         this.dcmsnd_dest_serverDetails = new String[this.dcmsnd_max_destination][3];
             this.https_dest_serverDetails  = new String[this.https_max_destination][2];

             this.httpsJobControllers = new https_clientManager[this.https_max_destination];

             for(int a = 0; a < this.dcmsnd_dest_serverDetails.length; a++)
                {
				 this.dcmsnd_dest_serverDetails[a][0] = prop.getProperty("dcmsnd_dest_"+a+"_calledAET");
				 this.dcmsnd_dest_serverDetails[a][1] = prop.getProperty("dcmsnd_dest_"+a+"_serverIp");
				 this.dcmsnd_dest_serverDetails[a][2] = prop.getProperty("dcmsnd_dest_"+a+"_serverPort");

                 //quick patch..
                 String[] destData = new String[5];
                 destData[0] = "dcmsnd";
                 destData[1] = prop.getProperty("dcmsnd_dest_"+a+"_alias");
                 destData[2] = this.dcmsnd_dest_serverDetails[a][0];
                 destData[3] = this.dcmsnd_dest_serverDetails[a][1];
                 destData[4] = this.dcmsnd_dest_serverDetails[a][2];
				 this.All_destinations.add(destData);
				}

             for(int a = 0; a < this.https_dest_serverDetails.length; a++)
                {
				 this.https_dest_serverDetails[a][0] = prop.getProperty("https_dest_"+a+"_serverIp");
				 this.https_dest_serverDetails[a][1] = prop.getProperty("https_dest_"+a+"_serverPort");

				 //quick patch..
				 String[] destData = new String[4];
				 destData[0] = "https";
				 destData[1] = prop.getProperty("https_dest_"+a+"_alias");
				 destData[2] = this.https_dest_serverDetails[a][0];
				 destData[3] = this.https_dest_serverDetails[a][1];
				 this.All_destinations.add(destData);
				}

	    } catch (Exception err){
		         err.printStackTrace();
		         log_writer.doLogging_QRmgr(logRoot, logName, "BasicManager error in setting initial stats 1: "+err);
		         System.exit(0);
		}

        arrivingJobs = new ArrayBlockingQueue<BasicJob>(arrivingJobQueueSize);
        currentlyRunningJobs = new ArrayBlockingQueue<BasicJob>(currentlyRunningJobQueueSize);
        relegatedJobs = new ArrayBlockingQueue<BasicJob>(relegatedJobQueueSize);


		Properties prop_2 = new Properties();
		FileInputStream fis_2 = new FileInputStream(sc_ts_properties);
		prop_2.load(fis_2);

        BasicManager.common_No_of_ts = Integer.parseInt(prop_2.getProperty("common_No_of_ts"));
		this.No_of_connections = Integer.parseInt(prop_2.getProperty("No_of_connections"));
		if((this.No_of_connections < 0) || (BasicManager.common_No_of_ts < 0))
		  {
		   System.err.println("<BasicManager.startOp() properties file 2> error: Invalid no of connections value: "+this.No_of_connections);
		   System.exit(0);
		  }

		this.alivenessFileSopClass = prop_2.getProperty("alivenessFileSopClass");

		BasicManager.common_ts = new String[BasicManager.common_No_of_ts];
		for(int x = 0; x < BasicManager.common_ts.length; x++)
		   {
            BasicManager.common_ts[x] = prop_2.getProperty("common_ts_"+x);
		   }


        //-----------------------------------------------------------------------------------------------
        this.baseConnections = new Object[Integer.parseInt(prop_2.getProperty("no_of_baseConnections"))];
		for(int k = 0; k < this.baseConnections.length; k++)
		   {
		    int num_sopclasses = Integer.parseInt(prop_2.getProperty("baseConnection_"+k+"_No_of_sopClasses"));
		    Object[][] s_classes = new Object[num_sopclasses + 1][2];
		    s_classes[0][0] = (Object) this.get_aliveSopClass();
			s_classes[0][1] = (Object) this.get_commonTS();
		    for(int m = 0; m < num_sopclasses; m++)
		       {
			    s_classes[m + 1][0] = (Object) prop_2.getProperty("baseConnection_"+k+"_sopClass_"+m);
			    s_classes[m + 1][1] = (Object) this.get_commonTS();
			   }

		   this.baseConnections[k] = s_classes;
		   }
		 //-----------------------------------------------------------------------------------------------


        if((this.transferType == 0)||(this.transferType == 2))
          {
			this.sendObjects = new AR_dcmsnd[this.No_of_connections];

			String[] args = new String[6];
			args[0] = "-ts1";
			args[1] = "true";
			args[2] = "-L "+this.callingAET+"@"+sourceServerIp;
			String[] serverDetails = this.get_desiredDcmsnd(this.get_defaultDcmsnd());
			args[3] = serverDetails[0]+"@"+serverDetails[1]+":"+serverDetails[2];
			args[4] = this.keepAliveFile; //for scanning & keepAlive
			args[5] = "teleradhack";


			for(int a = 0; a < this.No_of_connections; a++)
			   {
				int numOfSopClass =  Integer.parseInt(prop_2.getProperty("connection_"+a+"_No_of_sopClasses"));
				Object[][] sopclassUid = new Object[numOfSopClass][2];
				for(int b = 0; b < numOfSopClass; b++)
				   {
					sopclassUid[b][0] = (Object) prop_2.getProperty("connection_"+a+"_sopClass_"+b);
					int numOfTs = Integer.parseInt(prop_2.getProperty("connection_"+a+"_sopClass_"+b+"_No_of_ts"));
					String[] ts = new String[numOfTs];
					for(int c = 0; c < numOfTs; c++)
					   {
						ts[c] = prop_2.getProperty("connection_"+a+"_sopClass_"+b+"_ts_"+c);
					   }
					sopclassUid[b][1] = (Object) ts;
				   }

			   this.sendObjects[a] = new AR_dcmsnd(a, args, this.logRoot, this.logName, sopclassUid, this.keepAliveFile, this.alivenessFrequency, this);
			   new Thread(this.sendObjects[a]).start();

			   while((this.sendObjects[a].get_dcmsnd()) == null)
				 {
				  BasicManager.sleepForDesiredTime(1000);
				 }
			   }
	      }//end, transferType


		 this.rStrings = new String[14];

		 rStrings[0] = "-acceptTO";
		 rStrings[1] = "1800000";
		 rStrings[2] = "-connectTO";
		 rStrings[3] = "1800000";
		 rStrings[4] = "-rspTO";
		 rStrings[5] = "1800000";
		 rStrings[6] = "-releaseTO";
		 rStrings[7] = "1800000";
         rStrings[8] = "-ts1";
		 rStrings[9] = "true";
		 rStrings[10] = "-L "+this.callingAET+"@"+sourceServerIp;
		 String[] serverDetails = this.get_desiredDcmsnd(this.get_defaultDcmsnd());
	     rStrings[11] = serverDetails[0]+"@"+serverDetails[1]+":"+serverDetails[2];
		 rStrings[12] = this.keepAliveFile; //for scanning..
		 rStrings[13] = "teleradhack";

		 this.rStrings_2 = new String[2];
		 rStrings_2[0] = this.keepAliveFile;
		 rStrings_2[1] = Integer.toString(this.alivenessFrequency);

         fis.close();
         fis= null;

 } catch (Exception e){
          e.printStackTrace();
          log_writer.doLogging_QRmgr(this.logRoot, this.logName, "BasicManager.startOp() error: "+e);
          System.exit(0);
 }
 }


public static String get_netstatCmd()
{
 return BasicManager.netStatPidCmd;
}

public static String get_netstatFile()
{
 return BasicManager.netStatFileLocation;
}


public static void doNetstatPidFind(String logRoot, String logName)
{
 try
 {
  String netFile = BasicManager.get_netstatFile();
  netFile = utility.general.genericJobs.prepareFilePath(netFile, logRoot, logName);
  if((new File(netFile)).exists())
    {
     boolean oldFileDeleted = (new File(netFile)).delete();
     if(!oldFileDeleted)
       {
	    System.out.println("Failed to delete existent netstat file!");
	    log_writer.doLogging_QRmgr(logRoot, logName, "Failed to delete existent netstat file!");
        System.exit(0);
	   }
    }


  utility.general.cmdlineRunner.runCmd(logRoot, logName, BasicManager.get_netstatCmd(), true);
  netFile = BasicManager.get_netstatFile();
  int PID = BasicManager.do_netStatFileRead(new File(netFile));
  if(PID == -1)
    {
     log_writer.doLogging_QRmgr(logRoot, logName, "BasicManager.doNetstatPidFind() error: Failed to retrieve PID");
     System.exit(0);
    }
  else
    {
     //BasicManager.pidDBupdateCmd
     //pid =  PID
	 //islive = 1
     //instid = BasicManager.instid

     String[] data = new String[3];
     data[0] = Integer.toString(PID);
     data[1] = Integer.toString(1);
     data[2] = BasicManager.instid;
     new utility.db_manipulation.updatePidTable(logRoot, logName).doUpdate(BasicManager.pidDBupdateCmd, data);
    }

 } catch (Exception e){
          e.printStackTrace();
          log_writer.doLogging_QRmgr(logRoot, logName, "BasicManager.doNetstatPidFind() error: "+e);
          System.exit(0);
 }
}


public static int do_netStatFileRead(java.io.File f)
{
 int PID = -1;
 try
 {
  FileInputStream fs = new FileInputStream(f);
  java.io.InputStreamReader in = new java.io.InputStreamReader(fs);
  java.io.BufferedReader br = new java.io.BufferedReader(in);

  //We expect this to be a single line file..
  String textinLine = br.readLine();
  String[] dataSplit = textinLine.split(" ");
  PID = Integer.parseInt(dataSplit[(dataSplit.length)-1]);

 } catch (Exception e)
 {
   e.printStackTrace();
 }
return PID;
}



public dbUpdateMgr get_dbUpdateMgr(){

return this.dbUpdateMGR;
}


public String get_TEL_MSG()
{
 return null;
}




public lateArrivalScanner get_lateScanner(){

return this.L_Scanner;
}



//------------------------------------------------------------------------------------------------
//------------------------------------------------------------------------------------------------

public abstract void start_webServer();


public abstract String[] get_referralCardTags();



public void start_jobController(){

try {
	this.nHawkJobController = new JobExecutor(this,
											  this.maxNoOfExecutingJobs,
											  this.logRoot,
											  this.logName,
											  this.dataHandler_snoozing,
											  this.dataHandler_cannotMove,
											  this.rStrings,
											  this.rStrings_2);
} catch (Exception e){
         e.printStackTrace();
         log_writer.doLogging_QRmgr(logRoot, logName, "manager.start_jobController() error: "+e);
         System.exit(0);
}
}



public void start_httpsManager(){

try {
	if((this.transferType == 1)||(this.transferType == 2))
	  {
	   for(int x = 0; x < this.httpsJobControllers.length; x++)
		  {
		   this.httpsJobControllers[x] = new https_clientManager(this.noOfClients_https,
																 this.queueSize_https,
																 this.allBusyPauseTime_https,
																 this.logRoot,
																 this.logName,
																 this.https_dest_serverDetails[x][0],
																 Integer.parseInt(this.https_dest_serverDetails[x][1]),
																 this.machineID_https,
																 this.imgType_https,
																 this.chunkBufferSize_https);
			new Thread(this.httpsJobControllers[x]).start();
		  }
	  }
} catch (Exception e){
         e.printStackTrace();
         log_writer.doLogging_QRmgr(logRoot, logName, "manager.start_jobController() error: "+e);
         System.exit(0);
}
}



public void start_execMgr(){

try {
	this.nHawkExecMgr = new JobExecutionMgr(this.logRoot,
										    this.logName,
										    this.pauseTimeBetweenChecks,
											this,
											this.nHawkJobController);
	new Thread(this.nHawkExecMgr).start();

} catch (Exception e){
         e.printStackTrace();
         log_writer.doLogging_QRmgr(logRoot, logName, "manager.start_execMgr() error: "+e);
         System.exit(0);
}
}


public void start_tempConnectionChecker(){

try {
     if(this.terminate_tempConnections.equalsIgnoreCase("true"))
       {
	    new Thread(new monitor_tempConnections()).start();
       }
} catch (Exception e){
         e.printStackTrace();
         log_writer.doLogging_QRmgr(logRoot, logName, "manager.start_tempConnectionChecker() error: "+e);
         System.exit(0);
}
}



public void get_jobExecutor(JobExecutor jobExec){

this.nHawkJobController = jobExec;
}


public JobExecutor get_jobExecutor(){

return this.nHawkJobController;
}


public JobExecutionMgr get_jobExecMgr(){

return this.nHawkExecMgr;
}



public void start_jobAnalysisMgr(){

new Thread(new update_focusedJobHistory()).start();
}


public void start_clearanceMgrs(){

this.CLR_MGR = new clearanceManager(this,
	                                logRoot,
									logName,
									this.clearanceQueue,
									this.rStrings,
									this.rStrings_2);

new Thread(this.CLR_MGR).start();

this.L_Scanner = new lateArrivalScanner(logRoot,
                                        logName,
                                        this.CLR_MGR,
                                        ((this.maxCheckTime_clearance * 1000) * 60),
                                        this.clearanceStore);
new Thread(this.L_Scanner).start();
}

//------------------------------------------------------------------------------------------------
//------------------------------------------------------------------------------------------------



public synchronized void set_currentTransferMode(String value){

this.currentTransferMode = value;
}


public synchronized String get_currentTransferMode(){

return this.currentTransferMode;
}


public int get_transferType(){

return this.transferType;
}


public int get_dcmsnd_filelocationSize(){return this.dcmsnd_fileLocations_queueSize;}



public DcmSnd get_dcmsnd(String cuid, String[] stats){

DcmSnd snd = null;
snd = this.findInTempDcmSnds(cuid, stats);
return snd;
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
         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<BascicManager.findSopClass()> Exception: "+e);
}
return found;
}




public DcmSnd findInTempDcmSnds(String cuid, String[] stats){

DcmSnd obj = null;

//synchronized(this.get_queueLock()){
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
         log_writer.doLogging_QRmgr(logRoot, logName, "BasicManager <findInTempDcmSnds> error: "+e);
}
//}
return obj;
}




public void addToTempDcmSnd(DcmSnd snd){

try {
     this.temp_sendObjects.add(snd);
} catch (Exception e){
	     e.printStackTrace();
	     log_writer.doLogging_QRmgr(logRoot, logName, "BasicManager <addToTempDcmSnd> error: "+e);
}
}


public static void sleepForDesiredTime(int duration){

try {
	 Thread.sleep(duration);
} catch(InterruptedException ef) {
		ef.printStackTrace();
}
}


public static String get_newJobID(){

String value = null;
try {
     value = ""+System.currentTimeMillis()+"_"+BasicManager.jobIDcount;
     BasicManager.jobIDcount++;

} catch (Exception e){
	     value = null;
         e.printStackTrace();
}
return value;
}



public static String get_uniqueFileName(){

BasicManager.fileCounter++;

return ""+System.currentTimeMillis()+"_"+BasicManager.fileCounter;
}


public String get_aliveSopClass(){

return this.alivenessFileSopClass;
}


public static String[] get_commonTS(){

return BasicManager.common_ts;
}


public Object get_queueLock(){

return this.QUEUE_LOCK;
}



public void storeInQueue_arrivingJob(BasicJob job){

try {
     if(!this.arrivingJobs.contains(job))
       {
		 if(this.doAmissionHere)
		   {
		    while(this.jobsInSystem.get() >= this.arrivingJobQueueSize)
		     {
			  log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<BasicManager.storeInQueue_arrivingJob()> msg: System maxed out!");
			  this.sleepForDesiredTime(2000); //TODO: Take value in from properties file. Same for other 'equivalents'.
		     }
	       }

		 while(true)
		   {
			synchronized(this.get_queueLock())
			 {
			  if(this.arrivingJobs.size() >= this.arrivingJobQueueSize)
				{
				 System.out.println("<storeInQueue_arrivingJob> Job queue full. Must process an existing job before this new job can be stored in queue");
				 log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<storeInQueue_arrivingJob> Job queue full. Must process an existing job before this new job can be stored queue");
				}
			  else
				{
				 if(!this.arrivingJobs.contains(job))
				   {
					this.arrivingJobs.put(job);
					if(this.doAmissionHere)
					  {
                       if(!job.hasJoined())
                         {
					      job.set_joinStatus(true);
					      this.jobsInSystem.incrementAndGet();
					     }
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
         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<BasicManager.storeInQueue_arrivingJob()> exception: "+e);
}
}



public void storeInQueue_currentlyRunningJob(BasicJob job){

try {
	 if(!this.currentlyRunningJobs.contains(job))
	   {
		 while(true)
		   {
			synchronized(this.get_queueLock())
			 {
			  if(this.currentlyRunningJobs.size() == this.currentlyRunningJobQueueSize)
				{
				 System.out.println("currently running job queue full. Must process an existing job from this queue before this job can be stored in queue");
				 log_writer.doLogging_QRmgr(this.logRoot, this.logName, "currently running job queue full. Must process an existing job from this queue before this job can be stored in queue");
				}
			  else
				{
				 if(!this.currentlyRunningJobs.contains(job))
				   {
				    this.currentlyRunningJobs.put(job);
			       }
				 break;
				}
			 }
		   this.sleepForDesiredTime(2500);
		   }
       }
} catch (Exception e){
         e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<BasicManager.storeInQueue_currentlyRunningJob()> exception: "+e);
}
}



public void storeInQueue_relegatedJob(BasicJob job){

try {
	 if(!this.relegatedJobs.contains(job))
	   {
		 while(true)
		   {
			synchronized(this.get_queueLock())
			 {
			  if(this.relegatedJobs.size() == this.relegatedJobQueueSize)
				{
				 System.out.println("<storeInQueue_relegatedJob> Queue full. Must process an existing job from this queue before this job can be stored in queue");
				 log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<storeInQueue_relegatedJob> Queue full. Must process an existing job from this queue before this job can be stored in queue");
				}
			  else
				{
				 if(!this.relegatedJobs.contains(job))
				   {
				    this.relegatedJobs.put(job);
			       }
				 break;
				}
			 }
		   this.sleepForDesiredTime(2500);
		   }
       }
} catch (Exception e){
         e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<BasicManager.storeInQueue_relegatedJob()> exception: "+e);
}
}



public BasicJob get_newNightHawkJob(){

BasicJob nJob = null;

synchronized(this.get_queueLock()){
try {
     nJob = this.arrivingJobs.poll();

} catch (Exception e){
         nJob = null;
         e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<BasicManager.get_newNightHawkJob()> exception: "+e);
}
}
return nJob;
}



public BasicJob get_currentlyRunningNhawkJob(){

BasicJob nJob = null;

synchronized(this.get_queueLock()){
try {
     nJob = this.currentlyRunningJobs.poll();

} catch (Exception e){
         nJob = null;
         e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<BasicManager.get_currentlyRunningNhawkJob()> exception: "+e);
}
}
return nJob;
}



public ArrayBlockingQueue getArrivingQueueObject(){

return this.arrivingJobs;
}


public ArrayBlockingQueue getRunningQueueObject(){

return this.currentlyRunningJobs;
}


public JobExecutor get_jobController(){

return this.nHawkJobController;
}


public https_clientManager get_httpsCtrller(String[] data){

https_clientManager httpsJobContrler = null;
try {
     for(int x = 0; x < this.httpsJobControllers.length; x++)
        {
		 if(this.httpsJobControllers[x] != null)
		   {
		    String[] stats = this.httpsJobControllers[x].get_mainStats();
		    if((stats[0].equals(data[0]))&&
			   (stats[1].equals(data[1])))
			   {
				if(this.httpsJobControllers[x].atLeastOneAlive())
				  {
				   httpsJobContrler = this.httpsJobControllers[x];
			      }
				break;
			   }
		   }
		}
} catch (Exception e){
         httpsJobContrler = null;
         e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<BasicManager.get_httpsCtrller()> exception: "+e);
}
return httpsJobContrler;
}



public int get_defaultDcmsnd(){

int value = -1;
synchronized(this.get_queueLock())
 {
  value = this.dcmsnd_destination_default;
 }
return value;
}



public int get_defaultHttps(){

int value = -1;
synchronized(this.get_queueLock())
 {
  value = this.https_destination_default;
 }
return value;
}




public void set_dcmsndDefault(int value){

synchronized(this.get_queueLock())
 {
  if((value < 0)||(value >= this.dcmsnd_max_destination))
    {
	 log_writer.doLogging_QRmgr(logRoot, logName, "BasicManager.set_dcmsndDefault() msg: Out of range value: "+value);
	}
  else
    {
	 this.dcmsnd_destination_default = value;
	}
 }
}



public void set_httpsDefault(int value){

synchronized(this.get_queueLock())
 {
  if((value < 0)||(value >= this.https_max_destination))
    {
	 log_writer.doLogging_QRmgr(logRoot, logName, "BasicManager.set_httpsDefault() msg: Out of range value: "+value);
	}
  else
    {
	 this.https_destination_default = value;
	}
 }
}


public String[] get_desiredDcmsnd(int value){

String[] data = null;
synchronized(this.get_queueLock())
 {
  if((value < 0)||(value >= this.dcmsnd_max_destination))
    {
  	 log_writer.doLogging_QRmgr(logRoot, logName, "BasicManager.get_desiredDcmsnd() msg: Out of range value: "+value);
  	}
  else
    {
  	 data = new String[3];
  	 data[0] = this.dcmsnd_dest_serverDetails[value][0];
  	 data[1] = this.dcmsnd_dest_serverDetails[value][1];
  	 data[2] = this.dcmsnd_dest_serverDetails[value][2];
	}
 }
return data;
}



public String[] get_desiredHttps(int value){

String[] data = null;
synchronized(this.get_queueLock())
 {
  if((value < 0)||(value >= this.https_max_destination))
    {
  	 log_writer.doLogging_QRmgr(logRoot, logName, "BasicManager.get_desiredHttps() msg: Out of range value: "+value);
  	}
  else
    {
  	 data = new String[2];
  	 data[0] = this.https_dest_serverDetails[value][0];
  	 data[1] = this.https_dest_serverDetails[value][1];
	}
 }
return data;
}






public String[] get_desiredDcmsnd(String[] value){

String[] data = null;
synchronized(this.get_queueLock())
 {
  if(value != null){
  if((value.length <= 0)||(value.length > 3))
    {
  	 log_writer.doLogging_QRmgr(logRoot, logName, "BasicManager.get_desiredDcmsnd() msg: Invalid length value: "+value.length);
  	}
  else
    {
  	 for(int a = 0; a < this.dcmsnd_dest_serverDetails.length; a++)
  	    {
		 if((this.dcmsnd_dest_serverDetails[a][0].equals(value[0]))&&
		   ( this.dcmsnd_dest_serverDetails[a][1].equals(value[1]))&&
		   ( this.dcmsnd_dest_serverDetails[a][2].equals(value[2])))
		   {
		    data = new String[3];
			data[0] = this.dcmsnd_dest_serverDetails[a][0];
			data[1] = this.dcmsnd_dest_serverDetails[a][1];
  	        data[2] = this.dcmsnd_dest_serverDetails[a][2];
		   }
		}
	}
  }
 }
return data;
}



public String[] get_desiredHttps(String[] value){

String[] data = null;
synchronized(this.get_queueLock())
 {
  if(value != null){
  if((value.length <= 0)||(value.length > 2))
    {
  	 log_writer.doLogging_QRmgr(logRoot, logName, "BasicManager.get_desiredHttps() msg: Invalid length value: "+value.length);
  	}
  else
    {
     for(int a = 0; a < this.https_dest_serverDetails.length; a++)
  	    {
		 if((this.https_dest_serverDetails[a][0].equals(value[0]))&&
		   ( this.https_dest_serverDetails[a][1].equals(value[1])))
		   {
		    data = new String[2];
			data[0] = this.https_dest_serverDetails[a][0];
			data[1] = this.https_dest_serverDetails[a][1];
		   }
		}
	}
  }
 }
return data;
}




public Vector getDestinations(){

return this.All_destinations;
}



public String[] getDestination(String alias){

String[] details = null;
try {
     for(int a = 0; a < this.All_destinations.size(); a++)
        {
		 String[] data = (String[]) this.All_destinations.get(a);
		 if(data != null)
		   {
		    if(data[1].equals(alias))
		      {
			   details = data;
			   break;
			  }
		   }
		}

} catch (Exception e){
         details = null;
         e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<BasicManager.getDestination()> exception: "+e);
}
return details;
}





public String getDestination(String ip, String port){

String details = null;
try {
     for(int a = 0; a < this.All_destinations.size(); a++)
        {
		 String[] data = (String[]) this.All_destinations.get(a);
		 if(data != null)
		   {
			 if(data[0].equalsIgnoreCase("dcmsnd"))
			   {
			    if((data[3].equals(ip)) && (data[4].equals(port)))
			      {
				   details = data[1];
				   break;
				  }
			   }
			 else if(data[0].equalsIgnoreCase("https"))
			   {
			    if((data[2].equals(ip)) && (data[3].equals(port)))
			      {
				   details = data[1];
				   break;
				  }
			   }
			 else
			   {
			    log_writer.doLogging_QRmgr(this.logRoot, this.logName,
			    "<BasicManager.getDestination(2)> invalid alias: "+data[0]);
			   }
		   }
		}

} catch (Exception e){
         details = null;
         e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<BasicManager.getDestination(2)> exception: "+e);
}
return details;
}



//this.do_dbUpdate(qJob,2, null, intendedDest[1], logRoot, logName);

//nHawkStarter.do_dbUpdate(this.n_job,2, null, this.n_job.get_status(), logRoot, logName);
//nHawkStarter.do_dbUpdate(this.n_job,1, "0", null, logRoot, logName);

public void do_dbUpdate(BasicJob nHawk_job,
	                    int updateType,
                        String value_1,
                        String value_2,
                        String logRoot,
                        String logName){
 try {
	 if(updateType == 1)
	   {
        String sqlstr = "update autorouter_nighthawk set islive = ? where jobkey = ?";
		String[] data = new String[2];
		data[0] = value_1;
		data[1] = nHawk_job.get_jobId();

		new updateNightHawk(this,logRoot,logName).doUpdate(sqlstr, data);

		/*
		Object[] args = new Object[4];
		Integer opType = new Integer(7);

		args[0] = opType;
		args[1] = this;
		args[2] = data;
		args[3] = sqlstr;
		this.dbUpdateMGR.storeData(args);
		*/
	   }
	 else if(updateType == 2)
	   {
        String statusValue = value_2;
        try {
		     if(nHawk_job.get_status().equals("relegated"))
		       {
			    statusValue = "<html><i><b>relegated</b></i></html>";
			   }
		} catch (Exception er){
		         er.printStackTrace();
		}

        String sqlstr = "update autorouter_nighthawk set studystatus = ? where jobkey = ?";
		String[] data = new String[2];
		data[0] = statusValue;
		data[1] = nHawk_job.get_jobId();

		new updateNightHawk(this,logRoot,logName).doUpdate(sqlstr, data);

        /*
        Object[] args = new Object[4];
		Integer opType = new Integer(7);

		args[0] = opType;
		args[1] = this;
		args[2] = data;
		args[3] = sqlstr;
		this.dbUpdateMGR.storeData(args);
		*/
	   }
     else if(updateType == 3)
	   {
        String sqlstr = "update autorouter_nighthawk set studynumber = ? where jobkey = ?";
		String[] data = new String[2];
		data[0] = value_2;
		data[1] = nHawk_job.get_jobId();

		new updateNightHawk(this,logRoot,logName).doUpdate(sqlstr, data);

        /*
        Object[] args = new Object[4];
		Integer opType = new Integer(7);

		args[0] = opType;
		args[1] = this;
		args[2] = data;
		args[3] = sqlstr;
		this.dbUpdateMGR.storeData(args);
		*/
	   }
     else if(updateType == 4)
	   {
        String sqlstr = "update autorouter_nighthawk set destination = ? where  jobkey = ?";
		String[] data = new String[2];
		data[0] = value_2;
		data[1] = nHawk_job.get_jobId();

		new updateNightHawk(this, logRoot,logName).doUpdate(sqlstr, data);

        /*
        Object[] args = new Object[4];
		Integer opType = new Integer(7);

		args[0] = opType;
		args[1] = this;
		args[2] = data;
		args[3] = sqlstr;
		this.dbUpdateMGR.storeData(args);
		*/
	   }
	 else if(updateType == 5)
	   {
	    String sqlstr = "update autorouter_nighthawk set downloadstatus = ? where  jobkey = ?";
	    String[] data = new String[2];
	    data[0] = value_2;
	 	data[1] = nHawk_job.get_jobId();

	 	new updateNightHawk(this, logRoot,logName).doUpdate(sqlstr, data);

        /*
        Object[] args = new Object[4];
		Integer opType = new Integer(7);

		args[0] = opType;
		args[1] = this;
		args[2] = data;
		args[3] = sqlstr;
		this.dbUpdateMGR.storeData(args);
		*/
	   }
	 else
	   {
	    log_writer.doLogging_QRmgr(logRoot, logName, "BasicManager <night hawk: "+nHawk_job.get_jobId()+">.do_dbUpdate() msg: Invalid update type "+updateType);
	   }
 } catch (Exception db){
	     log_writer.doLogging_QRmgr(logRoot, logName, "BasicManager <night hawk: "+nHawk_job.get_jobId()+">.do_dbUpdate error: "+db);
 }
}



protected void do_jobTask(BasicJob nJob, String opType){

synchronized(this.get_queueLock()){
try {
	if((opType.equals("START_ALL")) || (opType.equals("START")))
	  {
	   nJob.set_status("pending");
	   this.storeInQueue_currentlyRunningJob(nJob);

	   if(opType.equals("START"))
	     {
	      this.do_dbUpdate(nJob,2, null, "pending", logRoot, logName);
	     }
	  }
	else if((opType.equals("STOP_ALL")) || (opType.equals("STOP")))
	  {
	   nJob.set_status("stopped");
	   this.storeInQueue_currentlyRunningJob(nJob);

	   if(opType.equals("STOP"))
	     {
	      this.do_dbUpdate(nJob,2, null, "stopped", logRoot, logName);
	     }
	  }
	else if(opType.equals(("PAUSE_ALL")) || (opType.equals("PAUSE")))
	  {
	   nJob.set_status("paused");
	   this.storeInQueue_currentlyRunningJob(nJob);

	   if(opType.equals("PAUSE"))
	     {
	      this.do_dbUpdate(nJob,2, null, "paused", logRoot, logName);
	     }
	  }
	else if((opType.equals("CANCEL_ALL")) || (opType.equals("CANCEL")))
	  {
	   nJob.set_status("cancelled");

	   if(opType.equals("CANCEL"))
	     {
	      this.do_dbUpdate(nJob,2, null, "cancelled", logRoot, logName);
	      this.do_dbUpdate(nJob,1, "0", null, logRoot, logName);
	     }
	  }

} catch (Exception e){
	     log_writer.doLogging_QRmgr(logRoot, logName, "BasicManager.do_jobTask() error: "+e);
}
}
}


public void doALL(String opType){

try {
     synchronized(this.get_queueLock())
      {
       int size = this.currentlyRunningJobs.size();
       for(int a = 0; a < size; a++)
          {
	       BasicJob nJob = this.currentlyRunningJobs.poll();
	       if(nJob != null)
	         {
		      this.do_jobTask(nJob, opType);
		      nJob= null;
		     }
	      }

       size = this.arrivingJobs.size();
       for(int a = 0; a < size; a++)
          {
           BasicJob nJob = this.arrivingJobs.poll();
	       if(nJob != null)
	         {
              this.do_jobTask(nJob, opType);
              nJob= null;
		     }
	      }

       size = this.relegatedJobs.size();
       for(int a = 0; a < size; a++)
          {
           BasicJob nJob = this.relegatedJobs.poll();
	       if(nJob != null)
	         {
              this.do_jobTask(nJob, opType);
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
	     log_writer.doLogging_QRmgr(logRoot, logName, "BasicManager.doALL error: "+e);
}
}



protected String[] getDest(String destination){

String[] intendedDest = null;
try {
     intendedDest = this.getDestination(destination);
     if(intendedDest == null)
	   {
	    log_writer.doLogging_QRmgr(logRoot, logName,
	    "<BasicManager.getDest()> msg: No matching destination found for "+destination);
	   }

} catch (Exception e){
	     intendedDest = null;
	     log_writer.doLogging_QRmgr(logRoot, logName, "BasicManager.getDest error: "+e);
}
return intendedDest;
}




public void changeAllDest(String destination){

try {
     String[] retrievedDest = this.getDest(destination);
     if(retrievedDest != null)
       {
        synchronized(this.get_queueLock())
         {
		  int size = this.currentlyRunningJobs.size();
		  for(int a = 0; a < size; a++)
			 {
			  BasicJob nJob = this.currentlyRunningJobs.poll();
			  if(nJob != null)
				{
				 this.switchDest(retrievedDest, nJob);
				 this.storeInQueue_currentlyRunningJob(nJob);
				}
			 }

		   size = this.arrivingJobs.size();
		   for(int a = 0; a < size; a++)
			  {
			   BasicJob nJob = this.arrivingJobs.poll();
			   if(nJob != null)
				 {
				  this.switchDest(retrievedDest, nJob);
				  this.storeInQueue_arrivingJob(nJob);
				 }
			  }

           size = this.relegatedJobs.size();
           for(int a = 0; a < size; a++)
              {
               BasicJob nJob = this.relegatedJobs.poll();
	           if(nJob != null)
	             {
                  this.switchDest(retrievedDest, nJob);
                  this.relegatedJobs.put(nJob);
		         }
	          }
	     }
	    this.nHawkJobController.changeDestJobObjects(retrievedDest);


        String sqlstr = "update autorouter_nighthawk set destination = ? where  islive = ?";
		String[] data = new String[2];
		data[0] = retrievedDest[1];
		data[1] = "1";
		new updateNightHawk(this, logRoot,logName).doUpdate(sqlstr, data);

	   }

} catch (Exception e){
	     log_writer.doLogging_QRmgr(logRoot, logName, "BasicManager.changeAllDest error: "+e);
}
}




public void switchDest(String[] intendedDest, BasicJob qJob){

try {
	if(intendedDest[0].equals("dcmsnd"))
	  {
	   qJob.set_transferMode("dcmsnd");

	   String[] dest = new String[3];
	   dest[0] = intendedDest[2];
	   dest[1] = intendedDest[3];
	   dest[2] = intendedDest[4];

	   qJob.set_dcmsndMainStats(dest);
	   qJob.set_httpsMainStats(this.get_desiredHttps(this.get_defaultHttps()));

	   //this.do_dbUpdate(qJob,4, null, intendedDest[1], logRoot, logName);
	  }
	else if(intendedDest[0].equals("https"))
	  {
	   qJob.set_transferMode("https");

	   String[] dest = new String[2];
	   dest[0] = intendedDest[2];
	   dest[1] = intendedDest[3];

	   qJob.set_httpsMainStats(dest);
	   qJob.set_dcmsndMainStats(this.get_desiredDcmsnd(this.get_defaultDcmsnd()));

	   //this.do_dbUpdate(qJob,4, null, intendedDest[1], logRoot, logName);
	  }
	else
	  {
	   log_writer.doLogging_QRmgr(logRoot, logName,
	   "<BasicManager.switchDest()> error: failed to find transfer mode for new job: "+qJob.get_jobId());
	  }

} catch (Exception e){
	     log_writer.doLogging_QRmgr(logRoot, logName, "BasicManager.switchDest error: "+e);
}
}




public boolean findAndDoOp_nightHawk(String jobId, String opType, String others){

BasicJob nJob = null;

boolean taskDone = false;

synchronized(this.get_queueLock()){
try {
     int size = this.relegatedJobs.size();
	 for(int a = 0; a < size; a++)
		{
		 nJob = this.relegatedJobs.poll();
		 if(nJob != null)
		   {
			if(nJob.get_jobId().equals(jobId))
			  {
			   this.handleRelegatedTask(nJob, jobId, opType, others);
			   taskDone = true;
			   break;
			  }
			else
			  {
			   this.storeInQueue_relegatedJob(nJob);
			   nJob = null;
			  }
		   }
	   }

     if(!taskDone)
       {
		 size = this.arrivingJobs.size();
		 for(int a = 0; a < size; a++)
			{
			 nJob = this.arrivingJobs.poll();
			 if(nJob != null)
			   {
				if(nJob.get_jobId().equals(jobId))
				  {
				   if(opType.equals("preemptRequest"))
					 {
					  boolean preempted = this.nHawkJobController.attemptPreempt(nJob);
					  if(!preempted)
						{
						 this.storeInQueue_arrivingJob(nJob);
						}
					 }
				   else if(opType.equals("changeDestination"))
					 {
					  this.changeDestination(nJob, others);
					  this.storeInQueue_arrivingJob(nJob);
					 }
				   else if(opType.equals("START_IMMEDIATELY"))
					 {
					  this.nHawkJobController.startThisJob(nJob);
					 }
				   else
					 {
					  this.do_jobTask(nJob, opType);
					 }
				   taskDone = true;
				   break;
				  }
				else
				  {
				   this.storeInQueue_arrivingJob(nJob);
				   nJob = null;
				  }
			   }
			}
       }
	 if(!taskDone)
	   {
		 size = this.currentlyRunningJobs.size();
		 for(int a = 0; a < size; a++)
			{
			 nJob = this.currentlyRunningJobs.poll();
			 if(nJob != null)
			   {
				if(nJob.get_jobId().equals(jobId))
				  {
                   if(opType.equals("preemptRequest"))
                     {
                      boolean preempted = this.nHawkJobController.attemptPreempt(nJob);
                      if(!preempted)
                        {
					     this.storeInQueue_currentlyRunningJob(nJob);
					    }
			         }
				   else if(opType.equals("changeDestination"))
				     {
				   	  this.changeDestination(nJob, others);
				   	  this.storeInQueue_currentlyRunningJob(nJob);
				     }
				   else if(opType.equals("START_IMMEDIATELY"))
					 {
					  this.nHawkJobController.startThisJob(nJob);
					 }
			       else
			   	     {
				      this.do_jobTask(nJob, opType);
			         }
				   taskDone = true;
				   break;
				  }
				else
				  {
				   this.storeInQueue_currentlyRunningJob(nJob);
				   nJob = null;
				  }
			   }
		    }
	    }

	 if((!taskDone) && (!(opType.equals("preemptRequest"))))
	   {
        if(opType.equals("CANCEL"))
          {
           taskDone = this.doCancelling(jobId, opType);
	      }
        else
          {
			nJob = this.nHawkJobController.checkAllForNightHawkObject(jobId);
			if(nJob != null)
			  {
			   taskDone = true;
			   if(opType.equals("changeDestination"))
				 {
				  this.changeDestination(nJob, others);
				 }
			   else if(opType.equals("START")) // TODO: ensure this don't happen. Disable button during this time.
				 {
				 }
			   else if(opType.equals("START_IMMEDIATELY")) // TODO: ensure this don't happen. Disable button during this time.
				 {
				 }
			   else if(opType.equals("STOP"))
				 {
				  nJob.set_status("stopped");
				  this.do_dbUpdate(nJob,2, null, "stopped", logRoot, logName);
				 }
			   else if(opType.equals("PAUSE"))
				 {
				  nJob.set_status("paused");
				  this.do_dbUpdate(nJob,2, null, "paused", logRoot, logName);
				 }
			   else if(opType.equals("CANCEL"))
				 {
				  nJob.set_status("cancelled");
				  this.do_dbUpdate(nJob,2, null, "cancelled", logRoot, logName);
				  this.do_dbUpdate(nJob,1, "0", null, logRoot, logName);
				 }
			  }
		    }
	   }

} catch (Exception e){
         e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<BasicManager.findAndDoOp_nightHawk()> exception: "+e);
}
}
return taskDone;
}





public BasicJob findAndgetJob(String jobId){

boolean taskDone = false;
BasicJob foundJob = null;
BasicJob nJob = null;

synchronized(this.get_queueLock()){
try {
     int size = this.relegatedJobs.size();
	 for(int a = 0; a < size; a++)
		{
		 nJob = this.relegatedJobs.poll();
		 if(nJob != null)
		   {
			if(nJob.get_jobId().equals(jobId))
			  {
			   foundJob = nJob;
			   taskDone = true;
			   this.storeInQueue_relegatedJob(nJob);
			   break;
			  }
			else
			  {
			   this.storeInQueue_relegatedJob(nJob);
			   nJob = null;
			  }
		   }
	   }

     if(!taskDone)
       {
		 size = this.arrivingJobs.size();
		 for(int a = 0; a < size; a++)
			{
			 nJob = this.arrivingJobs.poll();
			 if(nJob != null)
			   {
				if(nJob.get_jobId().equals(jobId))
				  {
				   foundJob = nJob;
				   taskDone = true;
				   this.storeInQueue_arrivingJob(nJob);
				   break;
				  }
				else
				  {
				   this.storeInQueue_arrivingJob(nJob);
				   nJob = null;
				  }
			   }
			}
       }
	 if(!taskDone)
	   {
		 size = this.currentlyRunningJobs.size();
		 for(int a = 0; a < size; a++)
			{
			 nJob = this.currentlyRunningJobs.poll();
			 if(nJob != null)
			   {
				if(nJob.get_jobId().equals(jobId))
				  {
                   foundJob = nJob;
				   taskDone = true;
				   this.storeInQueue_currentlyRunningJob(nJob);
				   break;
				  }
				else
				  {
				   this.storeInQueue_currentlyRunningJob(nJob);
				   nJob = null;
				  }
			   }
		    }
	    }

	 if(!taskDone)
	   {
		nJob = this.nHawkJobController.checkAllForNightHawkObject(jobId);
	    if(nJob != null)
		  {
		   foundJob = nJob;
		   taskDone = true;
		  }
	   }
} catch (Exception e){
         e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<BasicManager.findAndgetJob()> exception: "+e);
}
}
return foundJob;
}




public boolean doCancelling(String jobId, String opType){
boolean done = false;
try {
     done = this.nHawkJobController.checkAndDo(jobId, opType);
} catch (Exception e){
         e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<BasicManager.doCancelling()> exception: "+e);
}
return done;
}



public void changeDestination(BasicJob qJob, String destination){

try {
     String[] intendedDest = this.getDestination(destination);
     if(intendedDest == null)
	   {
	    log_writer.doLogging_QRmgr(logRoot, logName, "<BasicManager.changeDestination()> msg: No matching destination found for "+destination);
	   }
	 else
	   {
		if(intendedDest[0].equals("dcmsnd"))
		  {
		   qJob.set_transferMode("dcmsnd");

		   String[] dest = new String[3];
		   dest[0] = intendedDest[2];
		   dest[1] = intendedDest[3];
		   dest[2] = intendedDest[4];

		   qJob.set_dcmsndMainStats(dest);
		   qJob.set_httpsMainStats(this.get_desiredHttps(this.get_defaultHttps()));
		   this.do_dbUpdate(qJob,4, null, intendedDest[1], logRoot, logName);
		  }
		else if(intendedDest[0].equals("https"))
		  {
		   qJob.set_transferMode("https");

		   String[] dest = new String[2];
		   dest[0] = intendedDest[2];
		   dest[1] = intendedDest[3];

		   qJob.set_httpsMainStats(dest);
		   qJob.set_dcmsndMainStats(this.get_desiredDcmsnd(this.get_defaultDcmsnd()));
		   this.do_dbUpdate(qJob,4, null, intendedDest[1], logRoot, logName);
		  }
		else
		  {
		   log_writer.doLogging_QRmgr(logRoot, logName, "<BasicManager.changeDestination()> error: failed to find transfer mode for new job: "+qJob.get_jobId());
		  }
	   }

} catch (Exception e){
         e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<BasicManager.changeDestination()> exception: "+e);
}
}


 public void findAndStop(BasicJob job){

 this.storeInQueue_relegatedJob(job);
 }





 public boolean check_forDemographcs(BasicJob Job,
                                     String logRoot,
                                     String logName){
 boolean isTrue = false;
 try {
      if((Job.get_status().equals("cancelled"))||
        ( Job.get_status().equals("completed"))||
        ( Job.get_status().equals("Not found"))||
        ( Job.get_status().indexOf("fail") >= 0))
        {
         isTrue = true;
	    }
 } catch (Exception e){
 		  e.printStackTrace();
 		  log_writer.doLogging_QRmgr(logRoot, logName, "<QRjob_downloadMgr.check_forDemographcs()> exception: "+e);
 }
 return isTrue;
 }




 public boolean check_1(BasicJob Job,
                        String logRoot,
                        String logName){
 boolean isTrue = false;
 try {
      if((Job.get_status().equals("cancelled"))||
        ( Job.get_status().equals("completed"))||
        ( Job.get_status().equals("Not found"))||
        ( Job.get_status().indexOf("fail") >= 0))
        {
         isTrue = true;

         if(Job.get_status().equals("Not found"))
           {
            this.do_dbUpdate(Job,5, null, "Not found", logRoot, logName);
	       }

         if((Job.get_status().equals("completed"))||
           ( Job.get_status().equals("Not found"))||
           ( Job.get_status().indexOf("fail") >= 0))
           {
            this.do_dbUpdate(Job,2, null, Job.get_status(), logRoot, logName);
		    this.do_dbUpdate(Job,1, "0", null, logRoot, logName);
           }
	    }
 } catch (Exception e){
 		  e.printStackTrace();
 		  log_writer.doLogging_QRmgr(logRoot, logName, "<QRjob_downloadMgr.check_1()> exception: "+e);
 }
 return isTrue;
 }




 public static boolean check_2(BasicJob Job,
                               String logRoot,
                               String logName){
 boolean isTrue = false;
 try {
      if((Job.get_status().equals("paused"))||
        ( Job.get_status().equals("stopped")))
        {
         isTrue = true;
        }
 } catch (Exception e){
	      e.printStackTrace();
 		  log_writer.doLogging_QRmgr(logRoot, logName, "<QRjob_downloadMgr.check_2()> exception: "+e);
 }
 return isTrue;
 }




 public static boolean check_3(BasicJob Job,
                               String logRoot,
                               String logName){
 boolean isTrue = false;
 try {
      if((Job.downloadIsComplete())||
	    ( Job.downloading()))
        {
         isTrue = true;
        }
 } catch (Exception e){
 		  e.printStackTrace();
 		  log_writer.doLogging_QRmgr(logRoot, logName, "<QRjob_downloadMgr.check_3()> exception: "+e);
 }
 return isTrue;
 }



public void set_focusID(String value){

focusedJobId = value;

System.out.println("<set_focusID> id set: "+focusedJobId);
}



public abstract void storeInDemographcsQueue(String[] data);


public abstract void handleRelegatedTask(BasicJob job, String jobId, String opType, String others);


public abstract int get_downloaderQueueSize();


public abstract int get_downloaderPendingQueueSize();


public abstract void storeInDownloaderQueue_pendingJob(BasicJob job);


public abstract boolean cancelOp(BasicJob nJob, String opType);


public abstract void storeForDemographicsRetrieval(Object[] queryDetails);



public void set_managerType(int managerType)
{
 this.managerType = managerType;
}

public int get_managerType()
{
 return this.managerType;
}



//-------------------------------------------------

//      Monitors and terminates temporary
//      connections as necessary.

//-------------------------------------------------
class monitor_tempConnections implements Runnable {

protected monitor_tempConnections(){}


public void run(){

while(true)
 {
  try {
       synchronized(get_queueLock())
        {
		   if(getArrivingQueueObject().size() > 0){}
		   else if(getRunningQueueObject().size() > 0){}
		   else if(nHawkJobController.checkForAnExecutingThread().equals("executing")){}
		   else if(get_downloaderQueueSize() > 0){}
		   else if(get_downloaderPendingQueueSize() > 0){}

		   //else if(CLR_MGR.get_noOfElementLeft() > 0){}
		   //else if(L_Scanner.get_noOfElementLeft() > 0){}

		   else
			{
             boolean doClosing = false;
             if((CLR_MGR == null) && (L_Scanner == null))
               {
			    doClosing = true;
			   }
			 else
			   {
			    if((CLR_MGR.get_noOfElementLeft() <= 0)||
			       (L_Scanner.get_noOfElementLeft() <= 0))
			       {
			        doClosing = true;
			       }
			   }

             if(doClosing)
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
			    }

			}
	    }

       BasicManager.sleepForDesiredTime(tempConnCheckFrequency);

  } catch (Exception e){
           e.printStackTrace();
           log_writer.doLogging_QRmgr(logRoot, logName, "<BasicManager.monitor_tempConnections()> exception: "+e);
           BasicManager.sleepForDesiredTime(tempConnCheckFrequency);
  }
 }
}
}




//------------------------------------------------

// Updates the job history of the focused job id.

//------------------------------------------------
class update_focusedJobHistory implements Runnable {

private BasicJob b_job = null;
private String current_id = null;

public update_focusedJobHistory(){}


public void run(){

while(true)
 {
  BasicManager.sleepForDesiredTime(focusedIDupdateFreq);
  try {
       if(focusedJobId != null)
         {
		  if(focusedJobId == current_id)
		    {
		     if(b_job == null)
		       {
			    b_job = findAndgetJob(focusedJobId);
			    if(b_job != null)
			      {
				   b_job.doUpdateJobHistoryTable();
				  }
			   }
		     else
		       {
			    if(b_job.get_jobId().equals(focusedJobId))
			      {
				   b_job.doUpdateJobHistoryTable();
				  }
				else
				  {
				   b_job = findAndgetJob(focusedJobId);
				   if(b_job != null)
				     {
				   	  b_job.doUpdateJobHistoryTable();
				     }
				  }
			   }
		    }
		  else
		    {
			 current_id = focusedJobId;
			 b_job = findAndgetJob(focusedJobId);
			 if(b_job != null)
			   {
			    b_job.doUpdateJobHistoryTable();
			   }
			}
		 }
  } catch (Exception e){
           e.printStackTrace();
           log_writer.doLogging_QRmgr(logRoot, logName, "<BasicManager.update_focusedJobHistory()> exception: "+e);
           BasicManager.sleepForDesiredTime(focusedIDupdateFreq);
  }
 }
}
}


}