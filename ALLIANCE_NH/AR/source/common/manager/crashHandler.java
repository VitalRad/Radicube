/*****************************************************

 Author:  Mike Bass
 Year:    March, 2015
 Purpose: Called when Radicube starts to clean things up.
          Particularly useful if a crash had occurred.

 *****************************************************/

 package common.manager;


 import java.net.ServerSocket;
 import java.net.InetAddress;
 import java.io.FileInputStream;
 import java.util.Properties;
 import java.util.Timer;
 import java.util.TimerTask;

 import org.dcm4che2.net.log_writer;
 import utility.db_manipulation.crashUpdate;



 public class crashHandler
 {
  protected String logRoot = null;
  protected String logName = null;
  protected String serverIp = null;
  protected int serverPort = -1;
  protected int checkInterval = -1;
  private crashChecker cChecker = null;
  public String cleanupArg = null;
  public static String DB_CNAME = null;
  public static String DB_ROOT = null;



 public void startOp(String propertiesFile)
 {
  try
  {
   Properties prop = new Properties();
   FileInputStream fis = new FileInputStream(propertiesFile);
   prop.load(fis);

   this.logRoot = prop.getProperty("logRoot");
   this.logName = prop.getProperty("logName");
   this.serverIp = prop.getProperty("serverIp");
   this.serverPort = Integer.parseInt(prop.getProperty("serverPort"));
   this.checkInterval = Integer.parseInt(prop.getProperty("checkInterval"));
   this.cleanupArg = prop.getProperty("cleanupArg");
   crashHandler.DB_CNAME = prop.getProperty("DB_CNAME");
   crashHandler.DB_ROOT = prop.getProperty("DB_ROOT");

   if((this.logRoot == null)||
	  (this.logName == null)||
	  (this.serverIp == null)||
	  (this.checkInterval < 0)||
	  (this.cleanupArg == null)||
	  (crashHandler.DB_CNAME == null)||
	  (crashHandler.DB_ROOT == null))
	  {
	   System.out.println("<crashHandler> startOp() error: Invalid values got from properties file.");
	   log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<crashHandler> startOp() error: Invalid values got from properties file.");
	   System.exit(0);
	  }

	  this.cChecker = new crashChecker();
      this.cChecker.doTask_2(0, 0);

  } catch (Exception e)
  {
    log_writer.doLogging_QRmgr(this.logRoot, this.logName,
                               "<crashHandler> startOp() error: "+e);
    e.printStackTrace();
  }
 }



	///////////////////////////////////////
	//
	//  Checks for Radicube crash.
	//
	//////////////////////////////////////

	class crashChecker  {

	private Timer timer;

	public crashChecker(){

	this.timer = new Timer ();

	}


	public void doTask(int start_time, int delay_in_secs){

	this.timer.scheduleAtFixedRate(new crashChecker_worker(),
							      (start_time * 1000),
							      (delay_in_secs * 1000));
	}



	public void doTask_2(int start_time, int delay_in_secs){

	this.timer.schedule(new crashChecker_worker(),(delay_in_secs * 1000));
	}



	public void stop(){

	this.timer.cancel();
	}


	//worker class..
	class crashChecker_worker extends TimerTask{

	public void run(){

    /*
	boolean succeeded = canBind();
	if(succeeded)
	  {
       System.out.println("Time for clean up!");
       log_writer.doLogging_QRmgr(logRoot, logName,
                                 "<crashHandler> dTime for clean up!");
       do_DBcleanup(); //do cleaning up then exit.

       System.exit(0);
	  }
	  */

      do_DBcleanup(); //do cleaning up then exit.
      System.exit(0);
	}
	}
	}


    //Conducts binding check.
    //Attaining success here would mean AR
    //is not currently running.
 	public boolean canBind()
	{
	 boolean didBind = true;
	 try
	 {
      ServerSocket serverSock = new ServerSocket(this.serverPort,
                                                 5,
                                                 InetAddress.getByName(this.serverIp));

     } catch (Exception e)
     {
      didBind = false;
      e.printStackTrace();
     }
    return didBind;
	}


	public void do_DBcleanup()
	{
	 try
	 {
	  new crashUpdate(this, this.logRoot, this.logName).doUpdate(this.cleanupArg);

     } catch (Exception e)
     {
      log_writer.doLogging_QRmgr(this.logRoot, this.logName,
                                 "<crashHandler> do_DBcleanup() error: "+e);
      e.printStackTrace();
     }
	}


	public static void main(String[] args)
	{
	 new crashHandler().startOp(args[0]);

	 //new crashHandler().startOp("C:\\Users\\mabaze179yahoo.com\\Desktop\\new_libs\\crashHandler_properties.PROPERTIES");
	}

 }