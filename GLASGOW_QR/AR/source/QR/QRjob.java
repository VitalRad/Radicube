/********************************************
 *
 *   Author:  Mike Bassey
 *   Year:    2012
 *   Purpose: Encapsulates a QR job.
 *
 ********************************************/

 package QR;


 import java.util.concurrent.ArrayBlockingQueue;
 import java.util.concurrent.atomic.AtomicBoolean;
 import java.util.Vector;
 import java.io.File;
 import java.util.concurrent.atomic.AtomicInteger;


 import org.dcm4che2.net.Association;
 import org.dcm4che2.tool.xml2dcm.Xml2Dcm;
 import org.dcm4che2.tool.jpg2dcm.Jpg2Dcm;
 import org.dcm4che2.net.log_writer;
 import org.dcm4che2.tool.dcmrcv.dcm_folderCreator;
 import common.manager.BasicManager;
 import common.job.BasicJob;
 import utility.general.genericJobs;
 import utility.RIS_manipulation.risFormWriter;
 import utility.general.dicomObjectTagExtractor;




 public class QRjob extends BasicJob {


 private boolean storedInQueue = false;
 private String[] currentPACS = null;
 private String[] currentModalities = null;
 private String cmoverspTO = null;
 private boolean referralWanted = false;
 private AtomicBoolean referralRetrieved = new AtomicBoolean(false);
 private String[] RISdetails = null;
 private float imageQuality = -10F;
 private String[] cfgFileTags = null;
 public boolean download_reset = false;
 private QRjob_downloader QRdownloader = null;
 private String[] argsForInsertion = null;




 public QRjob(BasicManager bMgr,
	          String job_id,
              String fileLocation,
              String first_known_cuid,
              Association assoc,
              String logRoot,
              String logName,
              String fileStore,
              int failedMoveQueueSize,
              int pendingQueueSize){
 super(bMgr,
	   job_id,
       fileLocation,
       first_known_cuid,
       assoc,
       logRoot,
       logName,
       fileStore,
       failedMoveQueueSize,
       pendingQueueSize);
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
           "QRjob.updateTable_uploadCount() error: "+e);
 }
 }



 @Override
 public boolean referralCardRetrieved(){

 return this.referralRetrieved.get();
 }


 @Override
 public boolean isRelegated(){

 boolean state = false;

 if(this.status.equals("relegated"))
   {
    state = true;
   }
 else
   {
    state = false;
   }
 return state;
 }


 public String[] get_insertionData(){

 return this.argsForInsertion;
 }


 public void set_imagesUploaded(String value){

 this.bMgr.do_dbUpdate(this,
                       3,
                       null,
                       value,
                       this.logRoot,
                       this.logName);

 }


 public synchronized void set_Association(Association assoc){

 this.assoc = assoc;
 this.storedInQueue = true;
 }

 public synchronized void set_currentPACS(String[] currentPACS){

 this.currentPACS = currentPACS;
 }


 public synchronized void set_currentModalities(String[] currentModalities){

 this.currentModalities = currentModalities;
 }


 public synchronized void set_downloadStatus(boolean value){

 this.downloadCompleted.set(value);
 }


 public synchronized void set_downloadResult(String value){

 this.downloadResult = value;

 if(value.indexOf("fail") >= 0)
   {
    this.set_status("failed");
   }
 }


 public synchronized boolean isInQueue(){

 return this.storedInQueue;
 }


 public synchronized boolean downloadIsComplete(){

 return this.downloadCompleted.get();
 }


 public synchronized String get_downloadResult(){

 return this.downloadResult;
 }


 public synchronized String[] get_currentPACS(){

 return this.currentPACS;
 }


 public synchronized String[] get_currentModalities(){

 return this.currentModalities;
 }


 public void set_first_known_cuid(String uid){

 this.first_known_cuid = uid;
 }



 public void set_fileLocation(String location){ //still necessary??

 this.fileLocation = location;
 }



 public void set_essentials(boolean value){

 super.set_essentials(value);
 }



 public void set_cmoverspTO(String value){

 this.cmoverspTO = value;
 }



 public String get_cmoverspTO(){

 return this.cmoverspTO;
 }


 public void set_downloader(QRjob_downloader downloader){

 this.QRdownloader = downloader;
 }


 public QRjob_downloader get_downloader(){

 return this.QRdownloader;
 }


 public void writeDemographicsToDB(){
 try {
	 String[] retrievedDemographics = this.get_jobDemgraphics();
	 String jobKey = this.get_jobId();
	 String[] db_args = new String[(retrievedDemographics.length) + 6];
	 db_args[0] = jobKey;

	 String destValue = null;
	 String dest = this.get_transferMode();
	 if(dest.equalsIgnoreCase("dcmsnd"))
	   {
	    String[] data =  this.get_dcmsndMainStats();
	    destValue = this.bMgr.getDestination(data[1], data[2]);
	   }
	 else if(dest.equalsIgnoreCase("https"))
	   {
        String[] data =  this.get_httpsMainStats();
	    destValue = this.bMgr.getDestination(data[0], data[1]);
	   }

	 this.set_status("queued");

	 db_args[db_args.length -5] = this.get_status();
	 db_args[db_args.length -4] = "not started";
	 db_args[db_args.length -3] = Integer.toString(this.get_imagesUploaded());
	 db_args[db_args.length -2] = "1";
	 db_args[db_args.length -1] = "1";

	 for(int a = 0; a < retrievedDemographics.length; a++)
	    {
		 db_args[a + 1] = retrievedDemographics[a];
	    }

     //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++
     this.argsForInsertion = new String[db_args.length + 1];
     this.argsForInsertion[1] = destValue;
     for(int c = 0; c < db_args.length; c++)
        {
	     if(c == 0)
	       {
		    this.argsForInsertion[c] = db_args[c];
		   }
		 else
		   {
		    this.argsForInsertion[c + 1] = db_args[c];
		   }
	    }
	 //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++

 } catch (Exception e){
          e.printStackTrace();
          log_writer.doLogging_QRmgr(this.logRoot, this.logName,
          "QRjob.writeDemographicsToDB(), job id<"+this.get_jobId()+">  error: "+e);
 }
 }



 public void set_referralWanted(boolean value){

 this.referralWanted = value;
 }


 public void set_referralRetrieved(boolean value){

 this.referralRetrieved.set(value);
 }


 public void set_RISdetails(String[] RISdetails){

 this.RISdetails = RISdetails;
 }


 public void set_imageQuality(float imageQuality){

 this.imageQuality = imageQuality;
 }


 public void set_cfgFileData(String[] cfgFileTags){

 this.cfgFileTags = cfgFileTags;
 }



 private String getDemographicsValue(int pos){

 String value = null;
 try {
	  String[] dmgrphics = this.get_jobDemgraphics();
	  if(dmgrphics == null)
	    {
	 	 log_writer.doLogging_QRmgr(this.logRoot, this.logName,
	     "QRjob.getDemographicsValue() msg: retrieved domgraphics is null");
		}
      else
        {
	     value = dmgrphics[pos];
	    }

 } catch (Exception e){
          value = null;
          e.printStackTrace();
          log_writer.doLogging_QRmgr(this.logRoot, this.logName,
          "QRjob.getDemographicsValue() error: "+e);
 }
 return value;
 }



 public void commenceReferralCardProcessing(){

 if(this.referralWanted)
   {
    new Thread(new referralCardProcessor()).start();
   }
 }


 public void process_referralCards(){

 if(this.referralWanted)
   {
    try {
         boolean continueOp = true;
         String[] dmgrphics = this.get_jobDemgraphics();
         if(dmgrphics == null)
           {
		    continueOp = false;
		    log_writer.doLogging_QRmgr(this.logRoot, this.logName,
		    "QRjob.process_referralCards(), job id<"+this.get_jobId()+">  retrieved domgraphics is null");
		   }

		 String attNo = null;
		 if(continueOp)
		   {
		    attNo = stripAccessionNo(dmgrphics[5]);
		    if(attNo == null)
		      {
			   continueOp = false;
		       log_writer.doLogging_QRmgr(this.logRoot, this.logName,
		       "QRjob.process_referralCards(), job id<"+this.get_jobId()+">  retrieved attNo is null");
			  }
		   }

         String sql_arg = this.RISdetails[7]+"'"+attNo+"'";
         Object rCardLocations = null;
		 if(continueOp)
		   {
            utility.RIS_manipulation.queryRIS risQuery = new utility.RIS_manipulation.queryRIS(this.logRoot,this.logName);
            risQuery.connectToDatabase(this.RISdetails[0],
			                           Integer.parseInt(this.RISdetails[1]),
			                           this.RISdetails[2],
			                           this.RISdetails[3],
			                           this.RISdetails[4],
			                           this.RISdetails[5],
                                       Integer.parseInt(this.RISdetails[6]));
            Vector rVec =  risQuery.doExtraction_1(sql_arg);
            if(rVec == null)
              {
			   log_writer.doLogging_QRmgr(this.logRoot, this.logName,
			   "QRjob.process_referralCards(), job id<"+this.get_jobId()+">  retrieved location(s) is(are) null");
			  }
			else
			  {
               rCardLocations = get_rCardLocations(rVec);
               if(rCardLocations == null)
                 {
				  continueOp = false;
				  log_writer.doLogging_QRmgr(this.logRoot, this.logName,
				  "QRjob.process_referralCards(), job id<"+this.get_jobId()+">  failed to develop scanned doc path");
				 }
			  }
		   }

		if(continueOp)
		  {
           this.retrieveAndDicomise_referralCards(rCardLocations);
		  }

    } catch (Exception e){
             e.printStackTrace();
             log_writer.doLogging_QRmgr(this.logRoot, this.logName,
             "QRjob.process_referralCards(), job id<"+this.get_jobId()+">  error: "+e);
    }
   }
 this.set_referralRetrieved(true);
 }




private String stripAccessionNo(String ascNo){

String val = null;
try {
     val = ascNo.substring(5, (ascNo.length() - 2));

     while(true)
      {
	   String firstLetter = val.substring(0, 1);
	   if(firstLetter.equals("0"))
		 {
		  val = val.substring(1, val.length());
		 }
	   else
	     {
		  break;
		 }
	  }
} catch (Exception e) {
		 val = null;
		 e.printStackTrace();
		 log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<QRjob.stripAccessionNo()> error: "+e);
}
return val;
}




private Object get_rCardLocations(Vector rVec){

Object rCardLocations = null;
try {
     int vSize = rVec.size();
     String[] all_locations = new String[vSize];
     for(int c = 0; c < vSize; c++)
	    {
	     Object[] scanDetails = (Object[]) rVec.get(c);
	     if(scanDetails != null)
		   {
		    String sd_key = scanDetails[0].toString();
		    String actualDate = (String) scanDetails[1];
		    String[] trimDate = actualDate.split(" ");
		    String sd_date = trimDate[0];
		    sd_date = sd_date.replaceAll("-", "/");
		    sd_date = sd_date.replaceAll("\\\\", "/");
		    this.RISdetails[8] = this.RISdetails[8].replaceAll("\\\\", "/");
		    String[] result = sd_date.split("/");
		    result[0] = result[0].substring(0, 4);

		    String firstLetter = result[1].substring(0,1);
		    if(firstLetter.equals("0"))
			  {
			   result[1] = result[1].substring(1, result[1].length());
			  }

		    firstLetter = result[2].substring(0,1);
		    if(firstLetter.equals("0"))
			  {
			   result[2] = result[2].substring(1, result[2].length());
			  }

		    String sDoc_loc = this.RISdetails[8]+"/"+result[0]+"/"+result[1]+"/"+result[2]+"/"+sd_key+".TIF";
		    sDoc_loc = sDoc_loc.replaceAll(" ", "");
		    all_locations[c] = sDoc_loc;
		    log_writer.doLogging_QRmgr(this.logRoot, this.logName,
		    "<QRjob.get_rCardLocations()> msg: constructed scan doc addy for doc key "+sd_key+" is: "+sDoc_loc);
		   }
	    }
  rCardLocations = (Object) all_locations;
} catch (Exception e) {
		 rCardLocations = null;
		 e.printStackTrace();
		 log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<QRjob.get_rCardLocations()> error: "+e);
}
return rCardLocations;
}



private String createNewLocation(String cLocation){

String risProcessingLocation = null;
try {
     String[] storage = cLocation.split("/");
     for(int a = 0; a < storage.length; a++)
        {
		 if(risProcessingLocation == null)
		   {
		    risProcessingLocation = storage[a]+"/";
		   }
		 else if(a == (storage.length -1))
		   {
		    risProcessingLocation = risProcessingLocation+"/ris_processing/";
		    break;
		   }
		 else
		   {
		    risProcessingLocation = risProcessingLocation+"/"+storage[a]+"/";
		   }
		}
} catch (Exception e) {
		 risProcessingLocation = null;
		 e.printStackTrace();
		 log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<QRjob.createNewLocation()> error: "+e);
}
return risProcessingLocation;
}



private boolean convertToDcm(String cfgFile, String jpgFile, String newDcmFile){

boolean converted = true;
try {
     String[] args = new String[5];
     args[0] = "--no-appn";
     args[1] = "-c";
     args[2] = cfgFile;
     args[3] = jpgFile;
     args[4] = newDcmFile;

     Jpg2Dcm jpg2dcm = new Jpg2Dcm();
     jpg2dcm.startOp(args, jpg2dcm);
     while(jpg2dcm.get_ObjectState().equals("not_yet_set"))
      {
	   this.bMgr.sleepForDesiredTime(1000);
	  }

	if(jpg2dcm != null)
	  {
	   if(jpg2dcm.get_ObjectState().equals("failed"))
	     {
		  converted = false;
		  log_writer.doLogging_QRmgr(this.logRoot, this.logName,
		  "<QRjob.convertToDcm()> msg: failed to convert jpg file to dcm "+jpgFile);
		 }
	  }

} catch (Exception e) {
		 converted = false;
		 e.printStackTrace();
		 log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<QRjob.convertToDcm()> error: "+e);
}
return converted;
}




private String createXmlFile(String xmlFileLocation){

String newXmlFile = null;
try {
     int attempts = 0;
     int fileSwitch = 0;
     String dcmFileLocation = null;
     String[] extractedTags = null;
     while(attempts < this.bMgr.demograhphicRetrieval_maxAttempts)
      {
	   attempts++;
	   if(extractedTags != null){break;}

	   if(fileSwitch == 0)
	     {
		  fileSwitch = 1;
		  dcmFileLocation = this.fileLocation;
		 }
	   else
	     {
	      fileSwitch = 0;
	      dcmFileLocation = this.fileStore;
	     }

	   dicomObjectTagExtractor dcmTagExtrctr = new dicomObjectTagExtractor(dcmFileLocation,
	                            						  				   xmlFileLocation,
	                            						  				   this.logRoot,
	                            						  				   this.logName,
	                           						  					   this.bMgr.get_referralCardTags(),
	                            						                   this.bMgr.notWantedTagValues,
	                            						                   2);
	  dcmTagExtrctr.commence_tagRetrieval_2();
	  extractedTags = dcmTagExtrctr.getRetrievedTags();
	  }

	if(extractedTags == null)
	  {
	   log_writer.doLogging_QRmgr(this.logRoot, this.logName,
	   "<QRjob.createXmlFile()> msg: failed to create new xml file for job "+this.get_jobId());
	  }
    else
      {
       String[] xmlFileContents = new String[(extractedTags.length) + 3];
       xmlFileContents[0] = this.bMgr.xmlFileHeader;
       xmlFileContents[1] = "<"+this.bMgr.xmlFile_braceName+">";
       for(int x = 0; x < extractedTags.length; x++)
          {
		   xmlFileContents[x + 2] = extractedTags[x];
		  }
	   xmlFileContents[xmlFileContents.length - 1] = "</"+this.bMgr.xmlFile_braceName+">";

	   newXmlFile = xmlFileLocation+"/risfix.xml";
	   newXmlFile = genericJobs.prepareFilePath(newXmlFile, logRoot, logName);
	   boolean written = new utility.general.writeBytesToFile(logRoot,logName).start_writing(newXmlFile, xmlFileContents);
	   if(!written)
	     {
		  log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<QRjob.createXmlFile()> msg: Failed to write xml file "+newXmlFile);
		  newXmlFile = null;
		 }
	  }

} catch (Exception e) {
		 newXmlFile = null;
		 e.printStackTrace();
		 log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<QRjob.createXmlFile()> error: "+e);
}
return newXmlFile;
}





private void retrieveAndDicomise_referralCards(Object cardLocations){

try {
     boolean continueOp = true;
     String risProcessing = this.createNewLocation(this.fileStore);
     risProcessing = genericJobs.prepareFilePath(risProcessing, logRoot, logName);
     risProcessing = dcm_folderCreator.createNameAndFolder_imageStore(risProcessing);
     if(risProcessing == null)
	   {
        log_writer.doLogging_QRmgr(this.logRoot, this.logName,
        "<QRjob.retrieveAndDicomise_referralCards()> msg: Failed to create ris processing location "+risProcessing);
        continueOp = false;
       }

     if(continueOp)
       {
        String[] rCardLocations = (String[]) cardLocations;
        for(int a = 0; a < rCardLocations.length; a++)
           {
            //retrieve referral card...
            String[] splitForLast = rCardLocations[a].split("/");
            String fToWrite = risProcessing+"/"+splitForLast[(splitForLast.length-1)];
            fToWrite = genericJobs.prepareFilePath(fToWrite, logRoot, logName);
            boolean copied = genericJobs.copyfile(rCardLocations[a],fToWrite,logRoot,logName);
            if(!copied)
              {
		       log_writer.doLogging_QRmgr(this.logRoot, this.logName,
               "<QRjob.retrieveAndDicomise_referralCards()> msg: Failed to copy referral card "+rCardLocations[a]+", "+fToWrite);
               continueOp = false;
               break;
		      }

            //convert referral card from tiff to jpg..
		   	String[] splitForLast_2 = fToWrite.split("/");
		   	String[] splitForLast_3 = (splitForLast_2[(splitForLast_2.length-1)]).split("\\.");
		    String fName = (splitForLast_3[0])+".jpg";
		    String forRisWriter = risProcessing+"/"+fName;
		    forRisWriter = genericJobs.prepareFilePath(forRisWriter, logRoot, logName);
		    risFormWriter rsForm = new risFormWriter(fToWrite, forRisWriter, this.imageQuality, logRoot, logName);

		    try {
		         int res = 0;
			     while(res == 0)
			      {
			       this.bMgr.sleepForDesiredTime(500);
			       res = rsForm.getResults();
			      }
		    } catch (Exception rExcpt){}

		    if(rsForm != null)
		      {
		       rsForm.closeStreams();
		       rsForm = null;
		      }


			//develop a cfg file with the necessary tags.
			String[] cfgdata = new String[(cfgFileTags.length)-1];
			for(int x = 0; x < cfgdata.length; x++)
			   {
                if((x + 1) == ((cfgFileTags.length) - 1))
                  {
				   cfgdata[x] = cfgFileTags[x + 1] +(Integer.parseInt(cfgFileTags[0]) + (a + 1));
				  }
				else
				  {
				   cfgdata[x] = cfgFileTags[x + 1];
				  }
			   }

		   String accNo = this.getDemographicsValue(5);
		   if(accNo == null)
		     {
			  log_writer.doLogging_QRmgr(this.logRoot, this.logName,
			  "<QRjob.retrieveAndDicomise_referralCards()> msg: retrieved accNo is null");
			  continueOp = false;
              break;
			 }

           String cfgFilePath = risProcessing+"/"+accNo+".cfg";
		   if(continueOp)
		     {
			  cfgFilePath = genericJobs.prepareFilePath(cfgFilePath, logRoot, logName);
			  continueOp = new utility.general.writeBytesToFile(this.logRoot, this.logName).start_writing(cfgFilePath, cfgdata);
			  if(!continueOp)
			    {
			     log_writer.doLogging_QRmgr(this.logRoot, this.logName,
				 "<QRjob.retrieveAndDicomise_referralCards()> msg: failed to write cfg file: "+cfgFilePath);
				 break;
			    }
			 }

           String dcmLoc = null;
		   if(continueOp)
		     {
              //convert referral card (now in jpg format) to Dcm
              //(burning the cfg file contents into the new dcm file)
			  String[] splitForLast_4 = forRisWriter.split("/");
			  String[] splitForLast_5 = (splitForLast_4[(splitForLast_4.length-1)]).split("\\.");
			  String fName_2 = (splitForLast_5[0])+".DCM";
			  dcmLoc = risProcessing+"/"+fName_2;
			  dcmLoc = genericJobs.prepareFilePath(dcmLoc, logRoot, logName);
			  continueOp = this.convertToDcm(cfgFilePath, forRisWriter, dcmLoc);
		     }

           String newXml = null;
           if(continueOp)
             {
              //extract necessary tags from any one of the scans,
              //and create a new xml file with those tags.
              newXml = this.createXmlFile(risProcessing);
              if(newXml != null)
                {
                 //create a new dcm file for transmission (this file is the product of the
                 //referral card <now held in dcm format> with the new xml file tags burned into it)
                 String newDcmFile = this.fileLocation+"/"+accNo+"_fixed.DCM";
                 newDcmFile = genericJobs.prepareFilePath(newDcmFile, logRoot, logName);

                 String[] args = new String[6];
                 args[0] = "-i";
                 args[1] = dcmLoc;
                 args[2] = "-x";
                 args[3] = newXml;
                 args[4] = "-o";
                 args[5] = newDcmFile;

                 Xml2Dcm xml2dcm = new Xml2Dcm();
                 xml2dcm.startOp(args);
				}
			 }
	   	   }
       }

} catch (Exception e) {
		 e.printStackTrace();
		 log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<QRjob.retrieveAndDicomise_referralCards()> error: "+e);
}
}



 //////////////////////////////////////////////////////////
 // conducts referral card processing in a separate thread
 // so other processes are not held up.
 /////////////////////////////////////////////////////////
 class referralCardProcessor implements Runnable {

 referralCardProcessor(){}

 public void run(){

 process_referralCards();
 }
 }


}