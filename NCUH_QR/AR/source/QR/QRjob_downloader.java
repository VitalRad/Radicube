/************************************************
 *
 *   Author:  Mike Bassey
 *   Year:    2012
 *   Purpose: does a c-move on requested QR job,
 *            to requested PACS.
 *
 ************************************************/

 package QR;


 import org.dcm4che2.net.log_writer;
 import org.dcm4che2.tool.dcmqr.DcmQR;
 import common.job.BasicJob;
 import utility.general.genericJobs;


 public class QRjob_downloader {


 private QRjob currentJob = null;
 private String status = "free";
 private String logRoot = null;
 private String logName = null;
 private String nodeAET = null;
 private String nodeIP = null;
 private String nodePort = null;
 private DcmQR dcmqr = null;
 private QRmanager bMgr = null;



 public QRjob_downloader(String logRoot,
                         String logName,
                         String nodeAET,
                         String nodeIP,
                         String nodePort,
                         QRmanager bMgr){

 this.logRoot = logRoot;
 this.logName = logName;
 this.nodeAET = nodeAET;
 this.nodeIP = nodeIP;
 this.nodePort = nodePort;
 this.bMgr = bMgr;

 //----------------------------------------
 log_writer.doLogging_QRmgr(logRoot, logName, "QRjob_downloader started: nodeAET:"+nodeAET+", ip: "+nodeIP+", port: "+nodePort);
 //----------------------------------------
 }


 public synchronized void startOp(QRjob currentJob, boolean addToQueue){

 this.setStatus("busy"); //just in case..

 this.currentJob = currentJob;

 if(this.currentJob == null)
   {
    System.out.println("QRjob_downloader <startOp> msg: current job is null");
    log_writer.doLogging_QRmgr(logRoot, logName, "QRjob_downloader <startOp> msg: current job is null");
   }
 else
   {
    //---------------------------------------------------------------------
    log_writer.doLogging_QRmgr(logRoot, logName, "QRjob_downloader job started: nodeAET:"+nodeAET+", ip: "+nodeIP+", port: "+nodePort);
    //--------------------------------------------------------------------

    String[] currentPACS = this.currentJob.get_currentPACS();
    String[] currentModalities  = this.currentJob.get_currentModalities();

    if((currentPACS == null)||
      ( currentModalities == null))
	  {
	   System.out.println("QRjob_downloader <startOp> msg: current stats are null");
	   log_writer.doLogging_QRmgr(logRoot, logName, "QRjob_downloader <startOp> msg: current stats are null");
      }
    else
      {
		try {

		String[] demographics = this.currentJob.get_jobDemgraphics();
		if(demographics == null)
		  {
		   System.out.println("QRjob_downloader <startOp> msg: job demographics for job: "+this.currentJob.get_jobId()+" is null");
		   log_writer.doLogging_QRmgr(logRoot, logName, "QRjob_downloader <startOp> msg: job demographics for job: "+this.currentJob.get_jobId()+" is null");
		  }
		else
		  {
		   String fileLocation = this.currentJob.get_fileLocation();
		   if(fileLocation == null)
			 {
			  System.out.println("QRjob_downloader <startOp> msg: file location for job: "+this.currentJob.get_jobId()+" is null");
			  log_writer.doLogging_QRmgr(logRoot, logName, "QRjob_downloader <startOp> msg: file location for job: "+this.currentJob.get_jobId()+" is null");
			 }
		   else
			 {
			  int numTimes = this.currentJob.noOfTimesRan.addAndGet(1);
			  fileLocation = fileLocation+"/"+numTimes+"/";
			  fileLocation = genericJobs.prepareFilePath(fileLocation, logRoot, logName);
			  this.currentJob.set_fileLocation(fileLocation);


			  //------------------------------------------------------
			  if(currentJob.get_status().equals("awaiting re-start"))
			  	{
			     log_writer.doLogging_QRmgr(logRoot, logName,
			     "QRjob_downloader <startOp> msg: restarted job: "+this.currentJob.get_jobId()+", file location: "+this.currentJob.get_fileLocation());
			    }
			  //-------------------------------------------------------

			  String accessionNo = demographics[5];
			  if(accessionNo == null)
				{
				 System.out.println("QRjob_downloader <startOp> msg: Accession No for job: "+this.currentJob.get_jobId()+" is null");
				 log_writer.doLogging_QRmgr(logRoot, logName, "QRjob_downloader <startOp> msg: Accession No for job: "+this.currentJob.get_jobId()+" is null");
				}
			  else
				{
				 int argLength = -1;
				 String[] arg_1 = null;
				 if(bMgr.QUERY_TYPE == 0)
				   {
				    argLength = ((currentModalities.length * 2) + 14);
				    arg_1 = new String[12];

				    arg_1[0] = "-acceptTO";
		            arg_1[1] = Integer.toString(bMgr.ACCEPT_TO);
				    arg_1[2] ="-L";
				    arg_1[3] = this.nodeAET+"@"+this.nodeIP+":"+this.nodePort;
				    arg_1[4] = currentPACS[0]+"@"+currentPACS[1]+":"+currentPACS[2];
				    arg_1[5] = "-cmove";
				    arg_1[6] = this.nodeAET;
				    arg_1[7] ="-cmoverspTO";
                    arg_1[8] = currentJob.get_cmoverspTO();
				    arg_1[9] = "-q";
				    arg_1[10] = "AccessionNumber";
				    arg_1[11] = accessionNo;
				   }
				 else if(bMgr.QUERY_TYPE == 1)
				   {
				    argLength = ((currentModalities.length * 2) + 11);
				    arg_1 = new String[9];

				    arg_1[0] = "-I";
				    arg_1[1] ="-L";
				    arg_1[2] = this.nodeAET+"@"+this.nodeIP+":"+this.nodePort;
				    arg_1[3] = currentPACS[0]+"@"+currentPACS[1]+":"+currentPACS[2];
				    arg_1[4] = "-cmove";
				    arg_1[5] = this.nodeAET;
				    arg_1[6] = "-q";
				    arg_1[7] = "AccessionNumber";
				    arg_1[8] = accessionNo;
				   }

				 String[] arg_2 = new String[2];
				 arg_2[0] = "-cstoredest";
				 arg_2[1] = fileLocation;


				 int counter = 0;
				 String[] arg_3 = new String[argLength];
				 for(int a = 0; a < arg_1.length; a++)
					{
					 arg_3[counter] = arg_1[a];
					 counter++;
					}

				 for(int a = 0; a < currentModalities.length; a++)
					{
					 arg_3[counter] ="-cstore";
					 counter++;
					 arg_3[counter] = currentModalities[a];
					 counter++;
					}

				 for(int a = 0; a < arg_2.length; a++)
					{
					 arg_3[counter] = arg_2[a];
					 counter++;
					}

                 if(addToQueue)
                   {
				    String[] pDemogrhcs = this.currentJob.get_jobDemgraphics();
				    if(pDemogrhcs == null) //should never be the case!
				      {
					   log_writer.doLogging_QRmgr(logRoot, logName, "<QRjob_downloader <startOp>, job id: "+this.currentJob.get_jobId()+" Patient demographics is null!");
					  }
					else
					  {
					    String[] jobDetails = new String[8];
						jobDetails[0] = this.currentJob.get_jobId();
						jobDetails[1] = pDemogrhcs[0];
						jobDetails[2] = pDemogrhcs[1];
						jobDetails[3] = pDemogrhcs[5];
						jobDetails[4] = pDemogrhcs[6];
						jobDetails[5] = "null"; //study desc..
						jobDetails[6] = pDemogrhcs[7];
						jobDetails[7] = Integer.toString(this.currentJob.get_imagesUploaded());

						this.currentJob.set_detailsForDcmSnd(jobDetails);
					  }

				    bMgr.storeInQueue_arrivingJob(this.currentJob);
			       }

                 runNormalJob jobRunner = new runNormalJob(arg_3);
			     new Thread(jobRunner).start();

			     while(!jobRunner.isStarted())
			      {
				   bMgr.sleepForDesiredTime(1000);
				  }
				}
			 }
		  }

	   } catch (Exception e){
				e.printStackTrace();
				log_writer.doLogging_QRmgr(logRoot, logName, "<QRjob_downloader <startOp>, job id: "+this.currentJob.get_jobId()+" exception: "+e);
	   }
   }
   }
 }



 private void print_arg(String[] data){

 for(int a = 0; a < data.length; a++)
    {
	 log_writer.doLogging_QRmgr(logRoot, this.nodeAET+".txt", "<QRjob_downloader <print_arg>, data["+a+"]: "+data[a]);
	}
 }



 public synchronized DcmQR getDcmQR(){

 return this.dcmqr;
 }



 public void stopDcmQR(){

 if(this.dcmqr != null)
   {
    try {
         this.dcmqr.stop();
         this.dcmqr.close();
         this.dcmqr = null;
    } catch (Exception e){
			 e.printStackTrace();
			 log_writer.doLogging_QRmgr(logRoot, logName,
			 "<QRjob_downloader.stopDcmQR() exception: "+e);
	}
   }
 }



public synchronized void stopCurrentJob(String statusVal){

try {
	 if(this.currentJob != null)
	   {
		this.currentJob.download_reset = true;
		this.currentJob.set_status(statusVal);
	   }
	 if(this.getDcmQR() != null)
	   {
		while(this.dcmqr.argReceptionState == 2)
		 {
		  bMgr.sleepForDesiredTime(1000);
		  if(this.dcmqr == null)
			{
			 break;
			}
		 }

	   this.dcmqr.terminateImmediately();
	   this.stopDcmQR();
	   }

	 while(!(this.status.equals("free")))
	  {
	   this.bMgr.sleepForDesiredTime(1000); //TODO: Take value in from properties file.
	   if(this.currentJob == null)
		 {
		  break;
		 }
		//===============================================================================================================
		log_writer.doLogging_QRmgr(logRoot, logName, "<QRjob_downloader.stopCurrentJob() new job: we got here. point 0A");
		//===============================================================================================================
	  }

     //this.bMgr.storeInQueue_relegatedJob(this.currentJob);
	 this.currentJob = null; //just so we're sure..

//====================================================================================================
log_writer.doLogging_QRmgr(logRoot, logName, "<QRjob_downloader.start new job: Downloader freed (0B)");
//====================================================================================================

 } catch (Exception e){
 			 e.printStackTrace();
 			 log_writer.doLogging_QRmgr(logRoot, logName,
 			 "<QRjob_downloader.stopCurrentJob() exception: "+e);
 }
 }



 public synchronized void takeOver(QRjob newJob){

 if(this.currentJob != null)
   {
    this.currentJob.download_reset = true;
    this.currentJob.set_status("awaiting re-start");
    //====================================
    this.currentJob.terminate_allDcmsnd();
    //====================================
   }
 if(this.getDcmQR() != null)
   {
    while(this.dcmqr.argReceptionState == 2)
     {
	  bMgr.sleepForDesiredTime(1000);
	  if(this.dcmqr == null)
	    {
		 break;
		}
	 }

   this.dcmqr.terminateImmediately();
   this.stopDcmQR();
   }

 while(!(this.status.equals("free")))
  {
   bMgr.sleepForDesiredTime(1000); //TODO: Take value in from properties file.
   if(this.currentJob == null)
     {
	  break;
	 }
    //====================================================================================================
    log_writer.doLogging_QRmgr(logRoot, logName, "<QRjob_downloader.start new job: we got here. point 0");
 //====================================================================================================
  }

 if(this.currentJob != null)
   {
    if(!this.bMgr.relegatedJobs.contains(this.currentJob))
      {
       this.bMgr.storeInDownloaderQueue_pendingJob(currentJob);
      }
   }
 this.currentJob = null; //just so we're sure..
 this.setStatus("busy");

 //====================================================================================================
 log_writer.doLogging_QRmgr(logRoot, logName, "<QRjob_downloader.start new job: we got here. point 1");
 //====================================================================================================

 this.startOp(newJob, false);

 //====================================================================================================
 log_writer.doLogging_QRmgr(logRoot, logName, "<QRjob_downloader.start new job: we got here. point 2");
 //====================================================================================================
 }




 public synchronized void setStatus(String value){

 this.status = value;
 }


 public synchronized String getStatus(){

 return this.status;
 }



 public BasicJob get_currentJob(){

 return this.currentJob;
 }


 public QRjob_downloader get_thisDownloader(){

 return this;
 }


 /////////////////////////////////////////
 //
 //     Used to run a normal job.
 //
 /////////////////////////////////////////
 class runNormalJob implements Runnable {

 private String[] arg_3 = null;
 private boolean started = false;

 public runNormalJob(String[] arg_3){

 this.arg_3 = arg_3;
 }


 public boolean isStarted(){

 return this.started;
 }


 public void run(){

 try {
	  dcmqr = new DcmQR();
	  dcmqr.argReceptionState = 2; // to prevent immediate take-over..
	  currentJob.set_downloader(get_thisDownloader());
	  if(currentJob.get_status().equals("awaiting re-start"))
	    {
	     currentJob.set_status("re-started");
	    }
	  currentJob.markDownloadStart(true);

	  bMgr.do_dbUpdate(currentJob,5, null, "downloading...", logRoot, logName);
	  this.started = true;
	  dcmqr.startOp(dcmqr,
				    this.arg_3,
				    null,
				    "cmove",
				    logRoot,
				    logName,
				    currentJob);

	 boolean statusSet = false;
     if(currentJob != null)
       {
        if(currentJob.download_reset)
          {
		   //====================================================================================================
		   log_writer.doLogging_QRmgr(logRoot, logName, "<QRjob_downloader.start new job: we got here. point 3");
		   //====================================================================================================

		   currentJob.download_reset = false;

		   //======================================================================================================================================
		   log_writer.doLogging_QRmgr(logRoot, logName, "<QRjob_downloader.start new job: we got here. point 3a, job id: "+currentJob.get_jobId());
		   //======================================================================================================================================

           if(!(currentJob.get_status().equalsIgnoreCase("cancelled")))
             {
		      currentJob.set_imagesUploaded(".");
		      bMgr.do_dbUpdate(currentJob,5, null, "not started", logRoot, logName);
		     }


		   while((bMgr.get_jobExecutor().checkAllForNightHawkObject(currentJob.get_jobId())) != null)
		    {
			 bMgr.sleepForDesiredTime(1000);
			 //====================================================================================================================
			 log_writer.doLogging_QRmgr(logRoot, logName, "<QRjob_downloader> Job still running. job id: "+currentJob.get_jobId());
		     //====================================================================================================================
			}

		   currentJob.resetAll();

		   //====================================================================================================
		   	log_writer.doLogging_QRmgr(logRoot, logName, "<QRjob_downloader.start new job: we got here. point 3b");
		   //====================================================================================================

		   //====================================================================================================
		   log_writer.doLogging_QRmgr(logRoot, logName, "<QRjob_downloader.start new job: we got here. point 3c");
		   //====================================================================================================

		   statusSet = true;
	      }
       }

//====================================================================================================
log_writer.doLogging_QRmgr(logRoot, logName, "<QRjob_downloader.start new job: we got here. point 4");
//====================================================================================================


     if(!statusSet)
       {
	    currentJob.set_downloadStatus(true);
	    currentJob.set_downloadResult(dcmqr.get_ObjectState());
	    currentJob.set_downloader(null);
        currentJob.markDownloadStart(false);


	    if((dcmqr.NO_ELEMENTS_RETRIEVED.get() <= 0) || (dcmqr.NO_STUDIES_RETRIEVED.get() <= 0))
	      {
		   currentJob.set_status("Not found");

		   //============================================================================================================================
		   log_writer.doLogging_QRmgr(logRoot, logName, "<QRjob_downloader.start new> STATUS SET TO NOT FOUND: "+currentJob.get_jobId());
          //=============================================================================================================================
		  }
	    else
	      {
		   bMgr.do_dbUpdate(currentJob,5, null, "<html><i>downloaded</i></html>", logRoot, logName);
		  }
	    stopDcmQR();
	    currentJob = null;
       }
     else
       {
	    currentJob.set_downloader(null);
        currentJob.markDownloadStart(false);
	   }


 } catch (Exception e){
		  e.printStackTrace();
		  log_writer.doLogging_QRmgr(logRoot, logName,
		  "<QRjob_downloader.runNormalJob.run(), job id:  exception: "+e);
 } finally {
            this.started = true; //just in case..
            status = "free";

 //====================================================================================================
 log_writer.doLogging_QRmgr(logRoot, logName, "<QRjob_downloader.start new job: we got here. point 5");
 //====================================================================================================

 }
 }

 }

 }