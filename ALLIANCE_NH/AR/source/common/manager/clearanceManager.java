/*******************************************************

 Author:  Mike Bass
 Year:    2013
 Purpose: Ships to destination, scans that came in late.

 *******************************************************/

 package common.manager;


 import java.util.concurrent.ArrayBlockingQueue;
 import java.io.File;


 import org.dcm4che2.net.log_writer;
 import utility.general.dicomFileParser;
 import org.dcm4che2.tool.dcmsnd.DcmSnd;
 import utility.general.DcmSnd_starter;



 public class clearanceManager implements Runnable {


 private BasicManager bMgr = null;
 private String logRoot = null;
 private String logName = null;
 private int forTransferSize = -1;
 private ArrayBlockingQueue<Object[]> forTransfer = null;
 private String[] reAssignArgs_1 = null;
 private String[] reAssignArgs_2 = null;


 public clearanceManager(BasicManager bMgr,
	                     String logRoot,
                         String logName,
                         int forTransferSize,
                         String[] reAssignArgs_1,
                         String[] reAssignArgs_2){
 this.bMgr = bMgr;
 this.logRoot = logRoot;
 this.logName = logName;
 this.forTransferSize = forTransferSize;
 this.reAssignArgs_1 = reAssignArgs_1;
 this.reAssignArgs_2 = reAssignArgs_2;

 try {
      this.forTransfer = new ArrayBlockingQueue<Object[]>(this.forTransferSize);
 } catch (Exception e){
 		  e.printStackTrace();
 		  log_writer.doLogging_QRmgr(logRoot, logName, "clearanceManager() constr. error: "+e);
 		  System.exit(0);
 }
 }


 public void storeInQueue(Object[] data){

 try {
      this.forTransfer.put(data);
 } catch (Exception e){
 		  e.printStackTrace();
 		  log_writer.doLogging_QRmgr(logRoot, logName, "clearanceManager.storeInQueue() error: "+e);
 }
 }



 public int get_noOfElementLeft(){

 int size = 0;

 if(this.forTransfer != null)
   {
    size = this.forTransfer.size();
   }
 return size;
 }




private DcmSnd get_dcmsnd(String cuid, String[] dcmsndStats){

DcmSnd dcmsndObject = null;
try {
	 int count = 0;
	 boolean continueOp = true;
	 boolean doAdd = false;
     while(continueOp)
      {
		 dcmsndObject = this.bMgr.get_dcmsnd(cuid, dcmsndStats);
		 if(dcmsndObject == null)
		   {
			//-------------------------------------------------------------
			log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<clearanceManager.get_dcmsnd() error: Could not find dcmsnd for: "+cuid);
			//-------------------------------------------------------------

		   doAdd = true;

		   Object[][] cuid_tsuid = new Object[2][2];
		   cuid_tsuid[0][0] = (Object) cuid;
		   cuid_tsuid[1][0] = (Object) this.bMgr.get_aliveSopClass();

		   String[] tsyntaxes = this.bMgr.get_commonTS();
		   cuid_tsuid[0][1] = (Object) tsyntaxes;
		   cuid_tsuid[1][1] = (Object) tsyntaxes;

		   dcmsndObject = new DcmSnd();
		   dcmsndObject.set_alivenessStats(this.reAssignArgs_2);
		   dcmsndObject.setSCandTS(cuid_tsuid, this.bMgr);
		   dcmsndObject.set_objectStatus("temp");

		   /*
           //-----------------------------------
		   // NEW: must include with new DataCenter hack.
		   String[] jDetailsForDcmsnd =  nJob.get_jobDetailsForDcmSnd();
		   if(jDetailsForDcmsnd != null)
		     {
		      dcmsndObject.set_jobDetails(jDetailsForDcmsnd);
		     }
		   //-----------------------------------
		   */

		   //-----------------------------------------------------
		   dcmsndObject.set_mainStats(dcmsndStats);

		   String[] sndDetails = dcmsndStats;
		   if(sndDetails == null)
		     {
		      log_writer.doLogging_QRmgr(this.logRoot, this.logName,
		      "<clearanceManager.get_dcmsnd() msg: sndDetails is null. ");
		     }
		   else
		     {
		      String alias = this.bMgr.getDestination(sndDetails[1], sndDetails[2]);
		      dcmsndObject.set_nodeAlias(alias);
		     }
		   //-----------------------------------------------------

		   String[] serverDetails = this.bMgr.get_desiredDcmsnd(dcmsndStats);
		   String[] newDetails = new String[this.reAssignArgs_1.length];
		   for(int a = 0; a < newDetails.length; a++)
		      {
			   if(a == 11)
			     {
				  newDetails[a] = serverDetails[0]+"@"+serverDetails[1]+":"+serverDetails[2];
				 }
			   else
			     {
				  newDetails[a] = this.reAssignArgs_1[a];
				 }
			  }

		   new Thread(new DcmSnd_starter(dcmsndObject,newDetails,this.logRoot,this.logName)).start();

		   while(dcmsndObject.get_ObjectState().equals("not_yet_set"))
			{
			 BasicManager.sleepForDesiredTime(1000); //TODO: take this value in from properties file.
			}

		   if(dcmsndObject.get_ObjectState().equals("failed")) //try again..
			 {
			  log_writer.doLogging_QRmgr(this.logRoot, this.logName,
			  "<clearanceManager.get_dcmsnd() error: Couldn't establish connection for: "+cuid+", attempts: "+count);
			  count++;
			  BasicManager.sleepForDesiredTime(this.bMgr.connRetryPauseTime);
			  dcmsndObject = null;
			 }
		   else
			 {
			  continueOp = false;
			  break;
			 }
           }
          else
           {
		    continueOp = false;
			break;
		   }
	 }
/*
if((doAdd)&&(dcmsndObject != null))
  {
   this.bMgr.addToTempDcmSnd(dcmsndObject);
  }
  */

} catch (Exception e) {
         dcmsndObject = null;
         e.printStackTrace();
         log_writer.doLogging_QRmgr(logRoot, logName, "<clearanceManager>.get_dcmsnd() exception: "+e);
}
return dcmsndObject;
}



 public void run(){

 while(true)
  {
   try {
        Object[] data = this.forTransfer.take();
        if(data != null)
          {
           String transMode = (String) data[1];
           if(transMode.equals("dcmsnd"))
             {
              String[] dcmsndStats = (String[]) data[2];
              if(dcmsndStats != null)
                {
                 String cuid = new dicomFileParser().get_cuid(new File((String)data[3]), this.logRoot, this.logName);
                 if(cuid != null)
                   {
                    DcmSnd sndObject = this.get_dcmsnd(cuid, dcmsndStats);
                    if(sndObject != null)
                      {
					   sndObject.putInStore(data);
					  }
				   }
		          //log error:
				  else
				   {
				    log_writer.doLogging_QRmgr(logRoot, logName, "clearanceManager.run() error: Null cuid returned for file >>> "+((String)data[3]));
				   }

			    }
		     }
		  }

   } catch (Exception e){
 		    e.printStackTrace();
 		    log_writer.doLogging_QRmgr(logRoot, logName, "clearanceManager.run() error: "+e);
   }
  }
 }

 }