/**********************************************************

 Author:  Mike Bass
 Year:    2013
 Purpose: Checks for scan(s) that came in late and handle
          things accordingly.

 **********************************************************/

 package common.manager;


 import java.io.File;
 import java.util.Vector;
 import java.util.Iterator;


 import org.dcm4che2.net.log_writer;
 import org.dcm4che2.tool.dcmrcv.DcmRcv;
 import org.dcm4che2.tool.dcmrcv.dcm_folderCreator;



 public class lateArrivalScanner implements Runnable {

 private String logRoot = null;
 private String logName = null;
 private clearanceManager clr_mgr = null;
 private Vector<Object[]> newArrivals = new Vector<Object[]>(5);
 private long maxStorageTime = -1;
 private String storageLocation = null;



 public lateArrivalScanner(String logRoot,
                           String logName,
                           clearanceManager clr_mgr,
                           long maxStorageTime,
                           String storageLocation){

 this.logRoot = logRoot;
 this.logName = logName;
 this.clr_mgr = clr_mgr;
 this.maxStorageTime = maxStorageTime;
 this.storageLocation = storageLocation;
 }



 public void storeInQueue(Object[] data){

 try {
      boolean stored = this.newArrivals.add(data);
      if(!stored)
        {
		 log_writer.doLogging_QRmgr(logRoot, logName, "lateArrivalScanner.storeInQueue() msg: Failed to store in queue");
		}
 } catch (Exception e){
 		  e.printStackTrace();
 		  log_writer.doLogging_QRmgr(logRoot, logName, "lateArrivalScanner.storeInQueue() error: "+e);
 }
 }


 public int get_noOfElementLeft(){

 int size = 0;

 if(this.newArrivals != null)
   {
    size = this.newArrivals.size();
   }
 return size;
 }


 public String get_storageDest(String jobid){

 String destination = null;
 try {
      destination = this.storageLocation+"/"+dcm_folderCreator.generate_imageStoreName()+"/"+jobid;
      destination = destination.replaceAll("\\\\","/");
      destination = destination.replaceAll("//","/");
      destination = dcm_folderCreator.createNameAndFolder_imageStore(destination);
 } catch (Exception e) {
	      log_writer.doLogging_QRmgr(logRoot, logName, "lateArrivalScanner.get_storageDest(): error: "+e);
		  e.printStackTrace();
 }
 return destination;
 }


 //TODO: Sort this method out later....Proves problematic if sub-folders exist.
 //App has been manually set to ensure it doesnt create sub-folders but rather
 //stick them all into one folder.
 private String moveFiles(String source,
                          String destination,
                          String newName) {

 File dir = new File(source);
 try {
	 File[] files = dir.listFiles();
	 if(files != null)
	   {
		for(File f : files)
		   {
			if(f.isDirectory())
			  {
			   newName = this.moveFiles(source,destination,newName);  //TODO: sort this out later.
			  }
			else
			  {
               newName = destination+"/"+DcmRcv.get_uniqueFileName()+".DCM";
               newName = newName.replaceAll("\\\\","/");
               newName = newName.replaceAll("//","/");
               boolean done = f.renameTo(new File(newName));
               if(!done)
                 {
				  //log_writer.doLogging_QRmgr(logRoot, logName,
				  //"lateArrivalScanner <night hawk: "+this.nHawkJob.get_jobId()+">.moveFiles(): Failed to move file "+f.getName()+", "+newName);
				  newName = "-100";
				 }
			   else
			     {
				  break;
				 }
			  }
		   }
	   }
 } catch (Exception e) {
	      log_writer.doLogging_QRmgr(logRoot, logName, "lateArrivalScanner.moveFiles(): error: "+e);
		  e.printStackTrace();
		  newName = "-200";
 }
 return newName;
 }



 public void run(){

 while(true)
  {
   boolean fileSaved = false;
   try {
        Object[] allElements = this.newArrivals.toArray();
        if(allElements != null)
          {
           for(int a = 0; a < allElements.length; a++)
            {
		     Object[] data = (Object[]) allElements[a];
			 if(data != null)
		       {
			    String jobid        = (String)   data[0];
			    String transMode    = (String)   data[1];
			    String[] sendStats  = (String[]) data[2];
			    /*
			    //============================================
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
			    //============================================
			    */


			    String fileLocation = (String)   data[3];
			    Long storageTime    = (Long)     data[4];
			    String destination  = (String)   data[5];

                String fileName = this.moveFiles(fileLocation, destination, "0");
                if((fileName.equals("-100"))||(fileName.equals("-200")))
				  {
				   lateArrivalScanner.sleepForDesiredTime(1500);
				  }
				else if(fileName.equals("0"))//no file
				  {
				   //time up for this guy?
				   long diff = System.currentTimeMillis() - storageTime.longValue();
				   if(diff >= this.maxStorageTime)
					 {
					  //------------------------------------------
					  log_writer.doLogging_QRmgr(logRoot, logName, "lateArrivalScanner.run() msg: diff="+diff+",maxTime="+maxStorageTime+",storage time="+storageTime.longValue());
					  //------------------------------------------

					  boolean objectedRemoved = this.newArrivals.remove(data);
					  if(!objectedRemoved)
					    {
					     //------------------------------------------
						 log_writer.doLogging_QRmgr(logRoot, logName, "lateArrivalScanner.run() msg: Failed to delete object!");
					     //------------------------------------------
					    }
					     //------------------------------------------
						 log_writer.doLogging_QRmgr(logRoot, logName, "lateArrivalScanner.run() msg: Queue size = "+newArrivals.size());
					     //------------------------------------------
				     }
				   }
				 else
				   {
					Object[] argForClrMgr = new Object[5];
					argForClrMgr[0] = jobid;
					argForClrMgr[1] = transMode;
					argForClrMgr[2] = sendStats;
					argForClrMgr[3] = fileName;
					argForClrMgr[4] = this.clr_mgr;
					this.clr_mgr.storeInQueue(argForClrMgr);
					fileSaved = true;
				   }
			   }
		    }
		  }

      if(!fileSaved)
        {
         lateArrivalScanner.sleepForDesiredTime(1500); //TODO: take this in from properties file.
	    }
	  else
	    {
		 lateArrivalScanner.sleepForDesiredTime(50); //TODO: take this in from properties file.
		}

   } catch (Exception e){
 		    e.printStackTrace();
 		    log_writer.doLogging_QRmgr(logRoot, logName, "lateArrivalScanner.run() error: "+e);
   }
  }
 }

	public static void sleepForDesiredTime(int duration){

	try {
		 Thread.sleep(duration);
	} catch(InterruptedException ef) {
		    ef.printStackTrace();
	}
	}

 }