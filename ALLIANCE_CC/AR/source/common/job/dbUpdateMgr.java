/********************************************
 *
 *   Author:  Mike Bassey
 *   Year:    2012
 *   Purpose: Manages database updates.
 *
 *
 ********************************************/

 package common.job;


 import java.util.concurrent.ThreadPoolExecutor;
 import java.util.concurrent.ArrayBlockingQueue;
 import java.util.concurrent.TimeUnit;
 import java.util.concurrent.RejectedExecutionException;


 import common.manager.BasicManager;
 import org.dcm4che2.net.log_writer;
 import utility.db_manipulation.insertIntoARresults;
 import utility.db_manipulation.insertIntoHistory;
 import utility.db_manipulation.insertIntoWorklist;
 import utility.db_manipulation.insertNightHawk;
 import utility.db_manipulation.updateJobHistoryTable;
 import utility.db_manipulation.updateJobStatus;
 import utility.db_manipulation.updateNightHawk;
 import utility.db_manipulation.insertIntoAtable;



 public class dbUpdateMgr implements Runnable {

 private String logRoot = null;
 private String logName = null;
 private int corePoolSize = -1;
 private int maxPoolSize = -1;
 private int idleTime = -1;
 private int queueSize = -1;
 private int cmmdQueue = -1;
 private ArrayBlockingQueue<Object[]> dataToExecute = null;
 private ThreadPoolExecutor poolExe = null;



 public dbUpdateMgr(String logRoot,
                    String logName,
	                int corePoolSize,
                    int maxPoolSize,
                    int idleTime,
                    int queueSize,
                    int cmmdQueue){

 this.logRoot = logRoot;
 this.logName = logName;
 this.corePoolSize = corePoolSize;
 this.maxPoolSize = maxPoolSize;
 this.idleTime = idleTime;
 this.queueSize = queueSize;
 this.cmmdQueue = cmmdQueue;

 try {
      this.dataToExecute = new ArrayBlockingQueue<Object[]>(this.cmmdQueue);
      this.poolExe = new ThreadPoolExecutor(this.corePoolSize,
                                            this.maxPoolSize,
                                            this.idleTime,
      TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(this.queueSize),
                                                         new ThreadPoolExecutor.AbortPolicy());

 } catch (Exception e){
          e.printStackTrace();
          log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<dbUpdateMgr> constructor() exception: "+e);
          System.exit(0);
 }
 }


 public void storeData(Object[] data){

 try {
      this.dataToExecute.put(data);

 } catch (Exception e){
          e.printStackTrace();
          log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<dbUpdateMgr> storeData() exception: "+e);
 }
 }


 public void run(){

 while(true)
  {
   try {
        Object[] data = this.dataToExecute.take();
        boolean reStack = true;
        while(reStack)
        try {
             this.poolExe.execute(new executejob(data));
             reStack = false;

        } catch (RejectedExecutionException rjExcptn){
				 BasicManager.sleepForDesiredTime(1500);
	    } catch (Exception e2){
			     reStack = false;
	    }

	    //BasicManager.sleepForDesiredTime(1000);

   } catch (Exception e){
            e.printStackTrace();
            log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<dbUpdateMgr> run() exception: "+e);
   }
  }
 }


 //////////////////////////////////////////
 //Allows for execution in separate thread.
 //////////////////////////////////////////
 class executejob implements Runnable {

 private Object[] data = null;

 public executejob(Object[] data){

 this.data = data;
 }



 public void run(){

 try {
        if(data != null)
          {
           Integer thisInt = (Integer) data[0];
           int opType = thisInt.intValue();
           BasicManager mgr = (BasicManager) data[1];

           if(opType == 1)
             {
              String[] args = (String[]) data[2];
              String jobId = (String) data[3];
              String src = (String) data[4];
              new insertIntoARresults(mgr, logRoot, logName).startOp(args, jobId, src);
			 }
           else if(opType == 2)
             {
              String[] args = (String[]) data[2];
              new insertIntoHistory(mgr, logRoot, logName).startOp(args);
			 }
           else if(opType == 3)
             {
              String[] args = (String[]) data[2];
			  String jobId = (String) data[3];
              String src = (String) data[4];
              new insertIntoWorklist(mgr, logRoot,logName).startOp(args, jobId, src);
			 }
           else if(opType == 4)
             {
              String[] args = (String[]) data[2];
              String dest = (String) data[3];
              new insertNightHawk(mgr, logRoot, logName).startOp(args, dest);
			 }
           else if(opType == 5)
             {
              String[] args = (String[]) data[2];
              String sqlstr = (String) data[3];
              new updateJobHistoryTable(mgr, logRoot, logName).doUpdate(sqlstr, args);
			 }
           else if(opType == 6)
             {
              String[] args = (String[]) data[2];
              new updateJobStatus(mgr, logRoot, logName).doUpdate(args);
			 }
		   else if(opType == 7)
		     {
              String[] args = (String[]) data[2];
              String sqlstr = (String) data[3];
              new updateNightHawk(mgr, logRoot,logName).doUpdate(sqlstr, args);
			 }
           else if(opType == 8)
		     {
              String[] args = (String[]) data[2];
              String sqlstr = (String) data[3];
              new insertIntoAtable(mgr, logRoot,logName).doUpdate(sqlstr, args);
			 }
           else
             {
			  log_writer.doLogging_QRmgr(logRoot, logName, "<dbUpdateMgr.executejob> run() msg: faulty opType "+opType);
			 }
		  }

 } catch (Exception e){
          e.printStackTrace();
          log_writer.doLogging_QRmgr(logRoot, logName, "<dbUpdateMgr.executejob> run() exception: "+e);
 }
 }

 }

 }
