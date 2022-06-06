/*****************************************************************
 *
 *   Author:  Mike Bassey
 *   Year:    2012
 *   Purpose: Retrieves demographics for a job from desired PACS.
 *
 *****************************************************************/

  package QR;

  import java.util.Vector;

  import org.dcm4che2.net.log_writer;
  import common.job.BasicJob;
  import utility.db_manipulation.updateNightHawk;
  import utility.db_manipulation.updateJobStatus;
  import common.manager.BasicManager;



  public class getDemographicsFromPACS {

  private BasicJob job = null;
  private Object[] dataForQuery = null;
  private String logRoot = null;
  private String logName = null;
  private QRmanager bMgr = null;



  public getDemographicsFromPACS(QRmanager bMgr,
	                             Object[] dataForQuery,
                                 String logRoot,
                                 String logName,
                                 BasicJob job){
  this.bMgr = bMgr;
  this.dataForQuery = dataForQuery;
  this.logRoot = logRoot;
  this.logName = logName;
  this.job = job;
  }


  public void run(){
  try {
       Vector results =  new QRjob_cFinder(this.bMgr,
		                                   this.logRoot,
	   				                       this.logName,
	   					                  (String[])   this.dataForQuery[0],
	   					                  (String[])   this.dataForQuery[1],
	   					                  (String[])   this.dataForQuery[2],
	   					                  (String[][]) this.dataForQuery[3],
					                      (String)     this.dataForQuery[4]).get_demographics();
	  if(results == null)
	    {
	     log_writer.doLogging_QRmgr(logRoot, logName, "<QRjob_cFinder.getDemographicsFromPACS()> msg: Retrieved demographics is null, job id: "+job.get_jobId());
         //this.job.set_status("Not found");
         if((job.CFIND_attempts_made.addAndGet(1)) < BasicManager.maxDemographicsRetrievalAttempts_CFIND)
           {
            if(!(this.bMgr.check_forDemographcs(job, this.logRoot, this.logName)))
              {
               job.getManager().storeForDemographicsRetrieval(job.get_demographicsRetrivingInfo());
		      }
		   }
	    }
	  else
	    {
		 String[] demographcs = (String[]) results.remove(0);

		 if(demographcs == null)
		   {
		    log_writer.doLogging_QRmgr(logRoot, logName, "<QRjob_cFinder.getDemographicsFromPACS()> msg: Retrieved demographics is null, job id: "+job.get_jobId());
            //this.job.set_status("failed");

            if((job.CFIND_attempts_made.addAndGet(1)) < BasicManager.maxDemographicsRetrievalAttempts_CFIND)
			  {
               if(!(this.bMgr.check_forDemographcs(job, this.logRoot, this.logName)))
			     {
			      job.getManager().storeForDemographicsRetrieval(job.get_demographicsRetrivingInfo());
		         }
		      }
		   }
		 else
		   {
		    //String sqlstr = "update autorouter_nighthawk set patientid = ?, patientname = ?, dob = ?,  sex = ?, accessionnumber = ?, studyname = ?, modality = ?, studydate = ?, studytime = ? where jobkey = ?";
		    String[] data = new String[10];
		    data[0] = demographcs[0];
		    data[1] = demographcs[1];
		    data[2] = demographcs[2];
		    data[3] = demographcs[3];
		    data[4] = demographcs[4];
		    data[5] = demographcs[5];
		    data[6] = demographcs[6];
		    data[7] = demographcs[7];
		    data[8] = demographcs[8];
		    data[9] = job.get_jobId();

		    job.getManager().storeInDemographcsQueue(data);
	       }
	    }

  } catch (Exception e){
 		   e.printStackTrace();
 		   log_writer.doLogging_QRmgr(logRoot, logName, "<QRjob_cFinder.getDemographicsFromPACS()> exception: "+e);
  }
  }

  }