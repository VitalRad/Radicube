/******************************************************************************
 *
 *   Author:  Mike Bassey
 *   Year:    2012
 *   Purpose: Does the actual pushing of retrieved data to desired destination.
 *   Note: The data could be pushed to another night hawk server,
 *   and then from that night hawk server to another https server, and then to
 *   another retriever, etc.....it can be chained thus.
 *
 ******************************************************************************/

 package retriever;

 import java.io.File;
 import java.util.Vector;
 import java.util.Timer;
 import java.util.TimerTask;
 import java.util.concurrent.atomic.AtomicInteger;


 import org.dcm4che2.net.log_writer;
 import org.dcm4che2.tool.dcmsnd.DcmSnd;
 import utility.general.DcmSnd_starter;



 public class pushToDestination implements Runnable {


 private String logRoot = null;
 private String logName = null;
 private String fileLocation = null;
 private String movedLocation = null;
 private static int fileRenameCounter = 0;
 private int counter = 0;
 private Vector<DcmSnd> temp_sendObjects = new Vector<DcmSnd>(5);
 private String alivenessFileSopClass = null;
 private static String[] common_ts = null;
 private String[] reAssignArgs_1 = null;
 private String[] reAssignArgs_2 = null;
 private int dcmsndFileQueueSize = -1;
 private int connRetryPauseTime = -1;

 private int storedCounter = 0;
 private AtomicInteger lastPushFlag = new AtomicInteger(0);


 public pushToDestination(String logRoot,
                          String logName,
                          String fileLocation,
                          String movedLocation,
                          String alivenessFileSopClass,
						  String[] common_ts,
						  String[] reAssignArgs_1,
                          String[] reAssignArgs_2,
                          int dcmsndFileQueueSize,
                          int connRetryPauseTime){

 this.logRoot = logRoot;
 this.logName = logName;
 this.fileLocation = fileLocation;
 this.movedLocation = movedLocation;
 this.alivenessFileSopClass = alivenessFileSopClass;
 pushToDestination.common_ts = common_ts;
 this.reAssignArgs_1 = reAssignArgs_1;
 this.reAssignArgs_2 = reAssignArgs_2;
 this.dcmsndFileQueueSize = dcmsndFileQueueSize;
 this.connRetryPauseTime = connRetryPauseTime;
 }



 public void run(){

 new manageTempConnections().doTask(25, 25); //start timer..

 while(true){

 try {
      String fileToPush = file_handler.getFile(this.fileLocation,
                                               movedLocation,
                                               "0",
                                               this.logRoot,
                                               this.logName,
                                               this.counter,
                                               this.fileRenameCounter);
	    if(fileToPush.equals("0"))
	      {
		   //System.out.println("file is 0");
		   generic_class.sleepForDesiredTime(800); //TODO: take in from properties file.
		  }
	    else
	      {
           this.counter++;
           this.fileRenameCounter++;

           synchronized(this.temp_sendObjects)
            {
			   String cuid = new utility.general.dicomFileParser().get_cuid(new File(fileToPush), logRoot, logName);
			   if(cuid == null)
				 {
				  log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<pushToDestination.run() msg: skipped file with no cuid: "+fileToPush);
				 }
			   else
				 {
				  DcmSnd snd = this.get_dcmsnd(cuid);
				  if(snd == null)
					{
					 log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<pushToDestination.run() msg: Failed to find dcmsnd for "+fileToPush+". file skipped!");
					}
				  else
					{
					 Object[] data = new Object[2];
					 data[0] = fileToPush;
					 data[1] = null;
					 snd.putInStore(data);
					 lastPushFlag.set(1);
					}
				 }
		    }

		  }


 } catch (Exception e) {
          e.printStackTrace();
          log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<pushToDestination.run() error: "+e);
 }
 }
 }


 public int get_dcmsndQueueSize(){

 return this.dcmsndFileQueueSize;
 }


 public String get_aliveSopClass(){

 return this.alivenessFileSopClass;
 }


 public static String[] get_commonTS(){

 return pushToDestination.common_ts;
 }


 public DcmSnd findInTempDcmSnds(String cuid){

 DcmSnd obj = null;

 synchronized(this.temp_sendObjects){
 try{
	 for(int a = 0; a < this.temp_sendObjects.size(); a++)
	    {
		 DcmSnd snd = (DcmSnd) this.temp_sendObjects.get(a);
		 if(snd != null)
		   {
		    if(snd.get_supportedSopClass().equals(cuid))
			  {
               obj = snd;
		       break;
			  }
		   }
	    }
 } catch (Exception e){
		  obj = null;
		  e.printStackTrace();
		  log_writer.doLogging_QRmgr(logRoot, logName, "pushToDestination <findInTempDcmSnds> error: "+e);
 }
 }
 return obj;
 }



 public void addToTempDcmSnd(DcmSnd snd){

 try {
      this.temp_sendObjects.add(snd);
 } catch (Exception e){
 	     e.printStackTrace();
 	     log_writer.doLogging_QRmgr(logRoot, logName, "pushToDestination <addToTempDcmSnd> error: "+e);
 }
 }




 public DcmSnd get_dcmsnd(String cuid){

 DcmSnd dcmsndObject = null;

 long startTime = System.currentTimeMillis();

 try {
	 int count = 0;
	 boolean continueOp = true;
	 boolean doAdd = false;
     while(continueOp)
      {
		 dcmsndObject = this.findInTempDcmSnds(cuid);
		 if(dcmsndObject == null)
		   {
			log_writer.doLogging_QRmgr(this.logRoot, this.logName,
			"<pushToDestination.get_dcmsnd() error: Could not find dcmsnd for: "+cuid);

		   doAdd = true;

		   Object[][] cuid_tsuid = new Object[2][2];
		   cuid_tsuid[0][0] = (Object) cuid;
		   cuid_tsuid[1][0] = (Object) this.get_aliveSopClass();

		   String[] tsyntaxes = this.get_commonTS();
		   cuid_tsuid[0][1] = (Object) tsyntaxes;
		   cuid_tsuid[1][1] = (Object) tsyntaxes;

		   dcmsndObject = new DcmSnd();
		   dcmsndObject.set_alivenessStats(this.reAssignArgs_2);
		   dcmsndObject.setSCandTS(cuid_tsuid, this);
		   dcmsndObject.set_objectStatus("temp");
		   dcmsndObject.set_supportedSopClass(cuid);


		   new Thread(new DcmSnd_starter(dcmsndObject,this.reAssignArgs_1,this.logRoot,this.logName)).start();

		   while(dcmsndObject.get_ObjectState().equals("not_yet_set"))
			{
			 generic_class.sleepForDesiredTime(1000); //TODO: take this value in from properties file.
			}

		   if(dcmsndObject.get_ObjectState().equals("failed")) //try again..
			 {
			  log_writer.doLogging_QRmgr(this.logRoot, this.logName,
			  "<pushToDestination.get_dcmsnd() error: Couldn't establish connection for: "+cuid+", attempts: "+count);
			  count++;
			  generic_class.sleepForDesiredTime(this.connRetryPauseTime);
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

 if(doAdd)
   {
    this.addToTempDcmSnd(dcmsndObject);
   }

 long endTime = System.currentTimeMillis()-startTime; //Log this info?


  } catch (Exception e) {
          dcmsndObject = null;
          e.printStackTrace();
          log_writer.doLogging_QRmgr(logRoot, logName, "<pushToDestination>.get_dcmsnd() exception: "+e);
  }
 return dcmsndObject;
 }



 public void checkAndTerminate_tempConn(){
 try {
      synchronized(temp_sendObjects)
        {
		 int value = lastPushFlag.get();
		 if(value == 1)
		   {
		    lastPushFlag.set(0);
		   }
         else
           {
			 for(int a = 0; a < temp_sendObjects.size(); a++)
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
} catch (Exception e){
           e.printStackTrace();
           log_writer.doLogging_QRmgr(logRoot, logName, "<pushToDestination.checkAndTerminate_tempConn()> exception: "+e);
}
}



//==================================
//
//   Monitors and terminates temp
//   connections as necessary.
//
//==================================

class manageTempConnections  {

private Timer timer;

public manageTempConnections(){

timer = new Timer ();

}


public void doTask(int start_time, int delay_in_secs){

timer.scheduleAtFixedRate(new manageTempConnections_worker(),
                          (start_time * 1000),
                          (delay_in_secs * 1000));
}


//worker class..
class manageTempConnections_worker extends TimerTask{

public void run(){

checkAndTerminate_tempConn();

}
}
}



 }