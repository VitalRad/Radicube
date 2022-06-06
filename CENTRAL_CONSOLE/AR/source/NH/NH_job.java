/************************************************
 Author:  Mike Bass
 Year:    Feb 2015
 Purpose: Receives night hawk images for
          a particular job and save them in PACS.
 ************************************************/

 package NH;


 import java.io.FileInputStream;
 import java.util.Properties;
 import java.util.ArrayList;
 import java.util.concurrent.atomic.AtomicInteger;
 import java.util.concurrent.atomic.AtomicBoolean;

 import org.dcm4che2.net.log_writer;
 import org.dcm4che2.net.Association;



 public class NH_job {

 private String jobId = null;
 private String logRoot = null;
 private String logName = null;
 private String fileDest = null;

 private ArrayList<Association> jobAssoc = new ArrayList<Association>(10);
 private AtomicInteger numOfFiles = new AtomicInteger(0);
 private AtomicBoolean jobCompleted = new AtomicBoolean(false);




 /*

 in args:
 -------

(0) job ID
(1) patient ID
(2) patient name
(3) accession number
(4) modality
(5) study description
(6) study date


 in DB:
 -----
(0) 0010,0020 = patient ID
(1) 0010,0010 = patient name
(2) 0010,0030 = DOB
(3) 0010,0040 = Gender
(4) 0008,1030 = study name
(5) 0008,0050 = Acc no
(6) 0008,0060 = Modality
(7) 0008,0020 = Study Date
(8) 0008,0030 = Study Time

 */

 private String[] jobDemographics = new String[9];


 public NH_job(String jobId,
               String[] jobDemo,
               String logRoot,
               String logName)
 {
  this.jobId = jobId;
  this.logRoot = logRoot;
  this.logName = logName;

  //init...
  for(int a = 0; a < jobDemographics.length; a++)
     {
	  this.jobDemographics[a] = "null";
	 }

  for(int a = 0; a < jobDemo.length; a++)
     {
      if(a == 1) //patient ID...
        {
		 this.jobDemographics[0] = jobDemo[a];
		}
      else if(a == 2) //patient name...
        {
		 this.jobDemographics[1] = jobDemo[a];
		}
      else if(a == 3) //Acc No...
        {
		 this.jobDemographics[5] = jobDemo[a];
		}
      else if(a == 4) //Modality...
        {
		 this.jobDemographics[6] = jobDemo[a];
		}
      else if(a == 5) //Study Desc...
        {
		 this.jobDemographics[4] = jobDemo[a];
		}
      else if(a == 6) //Study Date...
        {
		 this.jobDemographics[7] = jobDemo[a];
		}
	 }
 }


 public void set_fileDes(String fileDest)
 {
  this.fileDest = fileDest;
 }

 public void set_jobCompleted(boolean value)
 {
  this.jobCompleted.set(value);
 }


 public void add_association(Association assc)
 {
  this.jobAssoc.add(assc);
 }


 public String get_jobId(){return this.jobId;}

 public String get_fileDest(){return this.fileDest;}

 public String[] get_demographics(){return this.jobDemographics;}


 public void incrementFileCounter()
 {
  this.numOfFiles.incrementAndGet();
 }

 public int getFileCount()
 {
  return this.numOfFiles.get();
 }

 public boolean taskCompleted()
 {
  return this.jobCompleted.get();
 }


 public void checkJobStatus()
 {
  try
  {
   synchronized(this.jobAssoc)
	{
	 boolean atleastOneAlive = false;
	 int pointer = 0;
	 for(int a = 0; a < this.jobAssoc.size(); a++)
		{
		 Association assoc = (Association) this.jobAssoc.get(pointer);
		 if(assoc == null)
		   {
			this.jobAssoc.remove(pointer);
		   }
		 else if((assoc.getSocket()) == null)
		   {
		    this.jobAssoc.remove(pointer);
		   }
         else if(((assoc.getSocket()).isClosed()) || (assoc.sockClosed))
		   {
		    this.jobAssoc.remove(pointer);
		   }
         else if(!((assoc.getSocket()).isConnected()))
		   {
		    this.jobAssoc.remove(pointer);
		   }
		 else
		   {
		    atleastOneAlive = true;
		    break;
		   }
		}
     if(!atleastOneAlive)
       {
	    this.set_jobCompleted(true);

	    //Just for testing..
	    //-------------------------------------------------------------
		  log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<NH job "+(this.get_jobId())+" Done!");
        //-------------------------------------------------------------
	   }

     }
  } catch (Exception e){
		 e.printStackTrace();
  //-------------------------------------------------------------
   log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<NH_job.checkJobStatus error: "+e);
  //-------------------------------------------------------------
 }
}


}