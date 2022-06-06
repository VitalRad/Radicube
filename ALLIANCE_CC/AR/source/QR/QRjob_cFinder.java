/************************************************
 *
 *   Author: Mike Bassey
 *   Year: 2012
 *   Purpose: does a c-find on requested QR job,
 *   to requested PACS.
 *
 ************************************************/

 package QR;

 import java.util.Vector;

 import org.dcm4che2.net.log_writer;
 import org.dcm4che2.tool.dcmqr.DcmQR;
 import utility.db_manipulation.insertIntoARresults;
 import utility.db_manipulation.updateJobStatus;
 import utility.db_manipulation.insertIntoWorklist;


 public class QRjob_cFinder {

 private String logRoot = null;
 private String logName = null;
 private String[] localAET = null;
 private String[] PACS = null;
 private String[] searchDetails = null;
 private String[][] wantedFields = null;
 private String dataSrc = null;
 private Vector<String[]> extractedData = new Vector<String[]>(5);
 private String[] firstRun_demographics = null;
 private QRmanager qMgr = null;


 public QRjob_cFinder(QRmanager qMgr,
	                  String logRoot,
                      String logName,
	                  String[] localAET,
	                  String[] PACS,
                      String[] searchDetails,
                      String[][] wantedFields,
                      String dataSrc){
 this.qMgr = qMgr;
 this.logRoot = logRoot;
 this.logName = logName;
 this.localAET = localAET;
 this.PACS = PACS;
 this.searchDetails = searchDetails;
 this.wantedFields = wantedFields;
 this.dataSrc = dataSrc;
 }





 public QRjob_cFinder(QRmanager qMgr,
	                  String logRoot,
                      String logName){
 this.qMgr = qMgr;
 this.logRoot = logRoot;
 this.logName = logName;
 }





 public void startOp(){

 try {
	 if(searchDetails[1].equals("Patient_ID"))
	   {
		 String[] arg = new String[9];
		 arg[0] = "-acceptTO";
		 arg[1] = "1800000";
		 arg[2] = "-L";
		 arg[3] = this.localAET[0]+"@"+this.localAET[1];
		 arg[4] = this.PACS[0]+"@"+this.PACS[1]+":"+this.PACS[2];
		 arg[5] = "-P";
		 arg[6] = "-q";
		 arg[7] = "PatientID";
		 arg[8] = searchDetails[2];


		 DcmQR dcmqr = new DcmQR();
		 dcmqr.startOp(dcmqr,
					   arg,
					   this.wantedFields,
					   "find",
					   this.logRoot,
					   this.logName,
					   this,
					   true);

	    //2nd run...
	    arg = new String[14];
	    arg[0] = "-acceptTO";
	    arg[1] = "1800000";
        arg[2] = "-L";
		arg[3] = this.localAET[0]+"@"+this.localAET[1];
		arg[4] = this.PACS[0]+"@"+this.PACS[1]+":"+this.PACS[2];
		arg[5] = "-q";
		arg[6] = "PatientID";
		arg[7] = searchDetails[2];
		arg[8] = "-r";
		arg[9] = "00080061"; //modality
		arg[10] = "-r";
		arg[11] = "00080090"; //referring clinician
		arg[12] = "-r";
		arg[13] = "00081030"; //study desc

        DcmQR dcmqr_2 = new DcmQR();
		dcmqr_2.startOp(dcmqr_2,
					    arg,
					    this.wantedFields,
					    "find",
					    this.logRoot,
					    this.logName,
					    this);

		if(this.extractedData.size() <= 0)
		  {
           log_writer.doLogging_QRmgr(logRoot, logName, "<QRjob_cFinder.startOp()> msg: Failed to find record for patient ID  "+searchDetails[2]);

           String[] msg = new String[3];
		   msg[0] = "SEARCH_FAILED";
		   msg[1] = "Failed to find record for patient ID "+searchDetails[2];
		   msg[2] = this.searchDetails[0];

		   new updateJobStatus(this.qMgr, logRoot, logName).doUpdate(msg);


		   /*
		   Object[] args = new Object[3];
		   Integer thisInt = new Integer(6);
		   args[0] = thisInt;
		   args[1] = this.qMgr;
		   args[2] = msg;
           this.qMgr.get_dbUpdateMgr().storeData(args);
           */
          }
        else
          {
           for(int x = 0; x < this.extractedData.size(); x++)
              {
               String[] data = this.extractedData.get(x);
               if(data != null)
                 {
                  //new insertIntoARresults(this.logRoot,this.logName).startOp(data, searchDetails[0], this.dataSrc);

                  Object[] argsToExe = new Object[5];
                  Integer thisInt = new Integer(1);
                  argsToExe[0] = thisInt;
                  argsToExe[1] = this.qMgr;
                  argsToExe[2] = data;
                  argsToExe[3] = searchDetails[0];
                  argsToExe[4] = this.dataSrc;
                  this.qMgr.get_dbUpdateMgr().storeData(argsToExe);
			     }
		      }


           String[] msg = new String[2];
		   msg[0] = "SEARCH_COMPLETED";
		   msg[1] = this.searchDetails[0];
		   new updateJobStatus(this.qMgr, logRoot, logName).doUpdate(msg);

		   /*
		   Object[] args = new Object[3];
		   Integer thisInt = new Integer(6);
		   args[0] = thisInt;
		   args[1] = this.qMgr;
		   args[2] = msg;
           this.qMgr.get_dbUpdateMgr().storeData(args);
           */
	      }
	   }
	 else if(searchDetails[1].equals("Accession_Number"))
	   {
         String[] arg = new String[8];

		 arg[0] = "-acceptTO";
		 arg[1] = "1800000";
		 arg[2] = "-L";
		 arg[3] = this.localAET[0]+"@"+this.localAET[1];
		 arg[4] = this.PACS[0]+"@"+this.PACS[1]+":"+this.PACS[2];
		 arg[5] = "-q";
		 arg[6] = "AccessionNumber";
		 arg[7] = searchDetails[2];

		 DcmQR dcmqr = new DcmQR();
		 dcmqr.startOp(dcmqr,
					   arg,
					   this.wantedFields,
					   "find",
					   this.logRoot,
					   this.logName,
					   this,
					   true);

	    String[] extractedDemographics = this.firstRun_demographics;
	    if(extractedDemographics == null)
	      {
		   log_writer.doLogging_QRmgr(logRoot, logName, "<QRjob_cFinder.startOp()> msg: Failed to find record for Accession Number  "+searchDetails[2]);

		   String[] msg = new String[3];
		   msg[0] = "SEARCH_FAILED";
		   msg[1] = "Failed to find record for Accession Number "+searchDetails[2];
		   msg[2] = this.searchDetails[0];
		   new updateJobStatus(this.qMgr, logRoot, logName).doUpdate(msg);

           /*
		   Object[] args = new Object[3];
		   Integer thisInt = new Integer(6);
		   args[0] = thisInt;
		   args[1] = this.qMgr;
		   args[2] = msg;
           this.qMgr.get_dbUpdateMgr().storeData(args);
           */
		  }
	    else if(extractedDemographics[11] == null)
	      {
		   log_writer.doLogging_QRmgr(logRoot, logName, "<QRjob_cFinder.startOp()> msg: Failed to retrieve demographics for Accession Number  "+searchDetails[2]);

           String[] msg = new String[3];
		   msg[0] = "SEARCH_FAILED";
		   msg[1] = "Failed to retrieve demographics for Accession Number "+searchDetails[2];
		   msg[2] = this.searchDetails[0];
		   new updateJobStatus(this.qMgr, logRoot, logName).doUpdate(msg);

           /*
		   Object[] args = new Object[3];
		   Integer thisInt = new Integer(6);
		   args[0] = thisInt;
		   args[1] = this.qMgr;
		   args[2] = msg;
           this.qMgr.get_dbUpdateMgr().storeData(args);
           */
		  }
	    else
	      {
			//2nd run..
			arg = new String[22];
		    arg[0] = "-acceptTO";
		    arg[1] = "1800000";
			arg[2] = "-L";
			arg[3] = this.localAET[0]+"@"+this.localAET[1];
			arg[4] = this.PACS[0]+"@"+this.PACS[1]+":"+this.PACS[2];
			arg[5] = "-q";
			arg[6] = "0020000D";
			arg[7] = extractedDemographics[11];
			arg[8] = "-r";
			arg[9] = "00100020";
			arg[10] = "-r";
			arg[11] = "00100010";
			arg[12] = "-r";
			arg[13] = "00100030";
			arg[14] = "-r";
			arg[15] = "00100040";
			arg[16] = "-r";
			arg[17] = "00081030";
			arg[18] = "-r";
			arg[19] = "00080061";
			arg[20] = "-r";
			arg[21] = "00080090";

			DcmQR dcmqr_2 = new DcmQR();
			dcmqr_2.startOp(dcmqr_2,
							arg,
							this.wantedFields,
							"find",
							this.logRoot,
							this.logName,
							this);

            if(this.extractedData.size() <= 0)
		      {
               log_writer.doLogging_QRmgr(logRoot, logName, "<QRjob_cFinder.startOp()> msg: Failed to find record for Accession Number "+searchDetails[2]);

              }
            else
              {
               for(int x = 0; x < this.extractedData.size(); x++)
                  {
                   String[] data = this.extractedData.get(x);
                   if(data != null)
                     {
                      //new insertIntoARresults(this.logRoot,this.logName).startOp(data, searchDetails[0], this.dataSrc);

                      Object[] argsToExe = new Object[5];
                      Integer thisInt = new Integer(1);
                      argsToExe[0] = thisInt;
                      argsToExe[1] = this.qMgr;
                      argsToExe[2] = data;
                      argsToExe[3] = searchDetails[0];
                      argsToExe[4] = this.dataSrc;
                      this.qMgr.get_dbUpdateMgr().storeData(argsToExe);
			         }
		          }

               String[] msg = new String[2];
			   msg[0] = "SEARCH_COMPLETED";
			   msg[1] = this.searchDetails[0];
		       new updateJobStatus(this.qMgr, logRoot, logName).doUpdate(msg);

               /*
		       Object[] args = new Object[3];
		       Integer thisInt = new Integer(6);
		       args[0] = thisInt;
		       args[1] = this.qMgr;
		       args[2] = msg;
               this.qMgr.get_dbUpdateMgr().storeData(args);
               */
	          }
	       }

	   }

 } catch (Exception e){
		  e.printStackTrace();
		  log_writer.doLogging_QRmgr(logRoot, logName, "<QRjob_cFinder.startOp()> exception: "+e);
 }
 }





 public void add_firstRunDemographics(String data[]){

 try {
      if(data != null)
        {
		 this.firstRun_demographics = data;
		}

 } catch (Exception e){
		  e.printStackTrace();
		  log_writer.doLogging_QRmgr(logRoot, logName, "<QRjob_cFinder.add_firstRunData()> exception: "+e);
 }
 }



 public void addData(String data[]){

 try {
      if(data != null)
        {
		 String[] dataToStore = null;
		 if(firstRun_demographics != null)
		   {
		    if(firstRun_demographics.length == data.length)
		      {
               dataToStore = new String[data.length];
               for(int a = 0; a < data.length; a++)
                  {
			       if(firstRun_demographics[a] == null)
			         {
					  dataToStore[a] = data[a];
					 }
				   else if(firstRun_demographics[a].equals("."))
				     {
					  dataToStore[a] = data[a];
					 }
				   else
				     {
					  dataToStore[a] = firstRun_demographics[a];
					 }
			      }
			  }
			else
			  {
			   dataToStore = data;
			  }
		   }
		 else
		   {
		    dataToStore = data;
		   }

		 this.extractedData.add(dataToStore);
		}

 } catch (Exception e){
		  e.printStackTrace();
		  log_writer.doLogging_QRmgr(logRoot, logName, "<QRjob_cFinder.addData()> exception: "+e);
 }
 }




 public void addDataDirectly(String data[]){
 try {
      if(data != null)
        {
		 this.extractedData.add(data);
		}

 } catch (Exception e){
		  e.printStackTrace();
		  log_writer.doLogging_QRmgr(logRoot, logName, "<QRjob_cFinder.addDataDirectly()> exception: "+e);
 }
 }



 public Vector runDirect(String[] arg,
                         String[][] wantedFields){
 try {
	  this.wantedFields = wantedFields;
	  DcmQR dcmqr = new DcmQR();
	  dcmqr.startOp(dcmqr,
					arg,
					this.wantedFields,
					"find",
					this.logRoot,
					this.logName,
					this,
					1);

 } catch (Exception e){
 		  this.extractedData = null;
 		  e.printStackTrace();
 		  log_writer.doLogging_QRmgr(logRoot, logName, "<QRjob_cFinder.runDirect()> exception: "+e);
 }
 return this.extractedData;
 }





 public Vector runDirect_2(String[] arg,
                           String[][] wantedFields,
                           String jobid,
                           String src){
 try {
	  this.wantedFields = wantedFields;
	  DcmQR dcmqr = new DcmQR();
	  dcmqr.startOp(dcmqr,
					arg,
					this.wantedFields,
					"find",
					this.logRoot,
					this.logName,
					this,
					0);


	 if(this.extractedData.size() <= 0)
	   {
	    log_writer.doLogging_QRmgr(logRoot, logName, "<QRjob_cFinder.runDirect_2()> msg: Failed to find record for worklist");
	    this.extractedData = null;
	   }
	 else
	   {
	    for(int x = 0; x < this.extractedData.size(); x++)
		   {
		    String[] data = this.extractedData.get(x);
		    if(data != null)
			  {
			   //new insertIntoWorklist(this.logRoot,this.logName).startOp(data, searchDetails[0], this.dataSrc);

			   Object[] argsToExe = new Object[5];
			   Integer thisInt = new Integer(3);
			   argsToExe[0] = thisInt;
			   argsToExe[1] = this.qMgr;
			   argsToExe[2] = data;
			   //argsToExe[3] = searchDetails[0];
			   argsToExe[3] = jobid;
			   //argsToExe[4] = this.dataSrc;
			   argsToExe[4] = src;
			   this.qMgr.get_dbUpdateMgr().storeData(argsToExe);
			  }
		   }
	  }

 } catch (Exception e){
 		  this.extractedData = null;
 		  e.printStackTrace();
 		  log_writer.doLogging_QRmgr(logRoot, logName, "<QRjob_cFinder.runDirect_2()> exception: "+e);
 }
 return this.extractedData;
 }




 public void startOp_2(){

 try {
     if(searchDetails[1].equals("Accession_Number"))
	   {
         String[] arg = new String[8];
		 arg[0] = "-acceptTO";
		 arg[1] = "1800000";
		 arg[2] = "-L";
		 arg[3] = this.localAET[0]+"@"+this.localAET[1];
		 arg[4] = this.PACS[0]+"@"+this.PACS[1]+":"+this.PACS[2];
		 arg[5] = "-q";
		 arg[6] = "AccessionNumber";
		 arg[7] = searchDetails[2];

		 DcmQR dcmqr = new DcmQR();
		 dcmqr.startOp(dcmqr,
					   arg,
					   this.wantedFields,
					   "find",
					   this.logRoot,
					   this.logName,
					   this,
					   true);

	    String[] extractedDemographics = this.firstRun_demographics;
	    if(extractedDemographics == null)
	      {
		   log_writer.doLogging_QRmgr(logRoot, logName, "<QRjob_cFinder.startOp_2()> msg: Failed to find record for Accession Number  "+searchDetails[2]);

           this.extractedData = null;
		   /*
		   String[] msg = new String[3];
		   msg[0] = "SEARCH_FAILED";
		   msg[1] = "Failed to find record for Accession Number "+searchDetails[2];
		   msg[2] = this.searchDetails[0];
		   new updateJobStatus(logRoot, logName).doUpdate(msg);
		   */
		  }
	    else if(extractedDemographics[11] == null)
	      {
		   log_writer.doLogging_QRmgr(logRoot, logName, "<QRjob_cFinder.startOp_2()> msg: Failed to retrieve demographics for Accession Number  "+searchDetails[2]);

           this.extractedData = null;
           /*
           String[] msg = new String[3];
		   msg[0] = "SEARCH_FAILED";
		   msg[1] = "Failed to retrieve demographics for Accession Number "+searchDetails[2];
		   msg[2] = this.searchDetails[0];
		   new updateJobStatus(logRoot, logName).doUpdate(msg);
		   */
		  }
	    else
	      {
			//2nd run..
			arg = new String[22];
		    arg[0] = "-acceptTO";
		    arg[1] = "1800000";
			arg[2] = "-L";
			arg[3] = this.localAET[0]+"@"+this.localAET[1];
			arg[4] = this.PACS[0]+"@"+this.PACS[1]+":"+this.PACS[2];
			arg[5] = "-q";
			arg[6] = "0020000D";
			arg[7] = extractedDemographics[11];
			arg[8] = "-r";
			arg[9] = "00100020";
			arg[10] = "-r";
			arg[11] = "00100010";
			arg[12] = "-r";
			arg[13] = "00100030";
			arg[14] = "-r";
			arg[15] = "00100040";
			arg[16] = "-r";
			arg[17] = "00081030";
			arg[18] = "-r";
			arg[19] = "00080061";
			arg[20] = "-r";
			arg[21] = "00080090";

			DcmQR dcmqr_2 = new DcmQR();
			dcmqr_2.startOp(dcmqr_2,
							arg,
							this.wantedFields,
							"find",
							this.logRoot,
							this.logName,
							this);

            if(this.extractedData.size() <= 0)
		      {
               log_writer.doLogging_QRmgr(logRoot, logName, "<QRjob_cFinder.startOp_2()> msg: Failed to find record for Accession Number "+searchDetails[2]);
              }
            else
              {

               for(int x = 0; x < this.extractedData.size(); x++)
                  {
                   String[] data = this.extractedData.get(x);
                   if(data != null)
                     {
                      //new insertIntoWorklist(this.logRoot,this.logName).startOp(data, searchDetails[0], this.dataSrc);


					  Object[] argsToExe = new Object[5];
					  Integer thisInt = new Integer(3);
					  argsToExe[0] = thisInt;
					  argsToExe[1] = this.qMgr;
					  argsToExe[2] = data;
					  argsToExe[3] = searchDetails[0];
					  argsToExe[4] = this.dataSrc;
					  this.qMgr.get_dbUpdateMgr().storeData(argsToExe);
			         }
		          }

               /*
               String[] msg = new String[2];
			   msg[0] = "SEARCH_COMPLETED";
			   msg[1] = this.searchDetails[0];
		       new updateJobStatus(logRoot, logName).doUpdate(msg);
		       */

	          }

	       }

	   }

 } catch (Exception e){
 		  this.extractedData = null;
 		  e.printStackTrace();
 		  log_writer.doLogging_QRmgr(logRoot, logName, "<QRjob_cFinder.startOp_2()> exception: "+e);
 }
 }




 public Vector get_demographics(){

 try {
     if(searchDetails[1].equals("Accession_Number"))
	   {
         String[] arg = new String[8];

		 arg[0] = "-acceptTO";
		 arg[1] = "1800000";
		 arg[2] = "-L";
		 arg[3] = this.localAET[0]+"@"+this.localAET[1];
		 arg[4] = this.PACS[0]+"@"+this.PACS[1]+":"+this.PACS[2];
		 arg[5] = "-q";
		 arg[6] = "AccessionNumber";
		 arg[7] = searchDetails[2];

		 DcmQR dcmqr = new DcmQR();
		 dcmqr.startOp(dcmqr,
					   arg,
					   this.wantedFields,
					   "find",
					   this.logRoot,
					   this.logName,
					   this,
					   true);

	    String[] extractedDemographics = this.firstRun_demographics;
	    if(extractedDemographics == null)
	      {
		   log_writer.doLogging_QRmgr(logRoot, logName, "<QRjob_cFinder.get_demographics()> msg: Failed to find record for Accession Number  "+searchDetails[2]);

		   this.extractedData = null;
		  }
	    else if(extractedDemographics[11] == null)
	      {
		   log_writer.doLogging_QRmgr(logRoot, logName, "<QRjob_cFinder.get_demographics()> msg: Failed to retrieve demographics for Accession Number  "+searchDetails[2]);

		   this.extractedData = null;
		  }
	    else
	      {
			//2nd run..
			arg = new String[22];
		    arg[0] = "-acceptTO";
		    arg[1] = "1800000";
			arg[2] = "-L";
			arg[3] = this.localAET[0]+"@"+this.localAET[1];
			arg[4] = this.PACS[0]+"@"+this.PACS[1]+":"+this.PACS[2];
			arg[5] = "-q";
			arg[6] = "0020000D";
			arg[7] = extractedDemographics[11];
			arg[8] = "-r";
			arg[9] = "00100020";
			arg[10] = "-r";
			arg[11] = "00100010";
			arg[12] = "-r";
			arg[13] = "00100030";
			arg[14] = "-r";
			arg[15] = "00100040";
			arg[16] = "-r";
			arg[17] = "00081030"; //study desc...
			arg[18] = "-r";
			arg[19] = "00080061";
			arg[20] = "-r";
			arg[21] = "00080090";

			DcmQR dcmqr_2 = new DcmQR();
			dcmqr_2.startOp(dcmqr_2,
							arg,
							this.wantedFields,
							"find",
							this.logRoot,
							this.logName,
							this);

            if(this.extractedData.size() <= 0)
		      {
               log_writer.doLogging_QRmgr(logRoot, logName, "<QRjob_cFinder.get_demographics()> msg: Failed to find record for Accession Number "+searchDetails[2]);
              }
	       }
	   }

 } catch (Exception e){
 		  this.extractedData = null;
 		  e.printStackTrace();
 		  log_writer.doLogging_QRmgr(logRoot, logName, "<QRjob_cFinder.get_demographics()> exception: "+e);
 }
 return this.extractedData;
 }

 }