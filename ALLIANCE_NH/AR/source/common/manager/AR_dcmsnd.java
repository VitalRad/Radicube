/************************************************

 Author:  Mike Bass
 Year:    2012
 Purpose: Encapsulates a dicom send object,
          adding Telerad-specific attributes
          to it.

 ************************************************/

package common.manager;

import org.dcm4che2.tool.dcmsnd.DcmSnd;
import org.dcm4che2.net.log_writer;
import common.manager.BasicManager;


public class AR_dcmsnd implements Runnable {

private int id = -1;
private String[] args = null;
private DcmSnd sendObject = null;
private String logRoot = null;
private String logName = null;
private Object[][] sopClass_transferSyntax = null;
private String alivenessFile = null;
private int alivenessFrequency = -1;
private BasicManager nHawkStarter = null;
private Object dcmsndCreationLock = new Object();


public AR_dcmsnd(int id,
                 String[] args,
	             String logRoot,
                 String logName,
                 Object[][] sopClass_transferSyntax,
                 String alivenessFile,
                 int alivenessFrequency,
                 BasicManager nHawkStarter){
this.id = id;
this.args = args;
this.logRoot = logRoot;
this.logName = logName;
this.sopClass_transferSyntax = sopClass_transferSyntax;
this.alivenessFile = alivenessFile;
this.alivenessFrequency = alivenessFrequency;
this.nHawkStarter = nHawkStarter;
}



public DcmSnd checkForSopClass(String cuid){

DcmSnd found = null;
try {
     for(int a = 0; a < sopClass_transferSyntax.length; a++)
        {
	     String uid = (String) this.sopClass_transferSyntax[a][0];
	     if((uid.equals(cuid)) && (!(this.sendObject.socketIsClosed())))
	       {
		    found = this.sendObject;
		    break;
		   }
	    }

} catch (Exception e){
         found = null;
         e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<AR_dcmsnd<"+this.id+">.checkForSopClass()> Exception: "+e);
}
return found;
}


public DcmSnd get_dcmsnd(){

DcmSnd snd = null;

synchronized(this.dcmsndCreationLock)
 {
  snd = this.sendObject;
 }
return snd;
}


public void run(){

try {
     synchronized(this.dcmsndCreationLock)
      {
       while(true)
        {
         String[] stats = new String[2];
         stats[0] = this.alivenessFile;
         stats[1] = Integer.toString(this.alivenessFrequency);
         this.sendObject = new DcmSnd();
         this.sendObject.set_alivenessStats(stats);
         this.sendObject.setSCandTS(this.sopClass_transferSyntax, this.nHawkStarter);
         this.sendObject.startOp(this.args, this.sendObject, this.logRoot, this.logName);
         while(this.sendObject.get_ObjectState().equals("not_yet_set"))
          {
           BasicManager.sleepForDesiredTime(1000); //TODO: take this value in from properties file.
		  }

         if(this.sendObject.get_ObjectState().equals("failed")) //try again..
           {
		    BasicManager.sleepForDesiredTime(this.nHawkStarter.connRetryPauseTime);
		   }
         else
          {
		   break;
		  }
	    }
      }

} catch (Exception e){
         e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<AR_dcmsnd<"+this.id+">.run()> Exception: "+e);
}
}


}