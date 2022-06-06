/*******************************************
 *
 *   Author:  Mike Bassey
 *   Year:    2012
 *   Purpose: Controls deletion activities.
 *
 *******************************************/

package deletion;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;
import java.util.Calendar;
import java.util.Vector;
import java.util.Timer;
import java.util.TimerTask;


import org.dcm4che2.net.log_writer;
import utility.general.genericJobs;



public class manager {

private String propFile = null;
private String logRoot = null;
private String logName = null;
private String[] ARpaths = null;
private String[] duration = new String[2];
private String[] dateRange = new String[2];
private long sizeLimit = 0L;
private int defaultSetting = -1;
private long diskSpaceThreshold = 0L;
private String targetPath = null;
private String[] duration_logs = new String[2];
private String logPath = null;
private int defaultSetting_log = -1;
private int sizeDeletionType = -1;



public manager(){}


public void startOp(String propertiesFile){

try {
     Properties prop = new Properties();
	 FileInputStream fis = new FileInputStream(propertiesFile);
     prop.load(fis);

     this.propFile = propertiesFile;

     this.logRoot = prop.getProperty("logRoot");
     this.logName = prop.getProperty("logName");
     int noOfPaths = Integer.parseInt(prop.getProperty("noOfPaths"));
     this.defaultSetting = Integer.parseInt(prop.getProperty("defaultSetting"));
     this.sizeLimit = Long.parseLong(prop.getProperty("sizeLimit"));
     this.duration[0] = prop.getProperty("durationValue");
     this.duration[1] = prop.getProperty("durationType");
     this.dateRange[0] = prop.getProperty("startDate");
     this.dateRange[1] = prop.getProperty("endDate");
     String serverIp = prop.getProperty("serverIp");
     int serverPort = Integer.parseInt(prop.getProperty("serverPort"));
     int serverBackLog = Integer.parseInt(prop.getProperty("serverBackLog"));
     this.diskSpaceThreshold = Long.parseLong(prop.getProperty("diskSpaceThreshold"));
     this.targetPath = prop.getProperty("targetPath");
     this.sizeDeletionType = Integer.parseInt(prop.getProperty("sizeDeletionType"));
     int checkfrequency = Integer.parseInt(prop.getProperty("checkfrequency"));

     this.defaultSetting_log = Integer.parseInt(prop.getProperty("defaultSetting_log"));
     this.duration_logs[0] = prop.getProperty("durationValue_logs");
     this.duration_logs[1] = prop.getProperty("durationType_logs");
     this.logPath = prop.getProperty("logPath");


     if((noOfPaths <= 0)||
       ( this.defaultSetting <= 0)||
       ( this.sizeLimit <= 0)||
       ( this.duration[0] == null)||
       ( this.duration[1] == null)||
       ( this.dateRange[0] == null)||
       ( this.dateRange[1] == null)||
       ( serverIp == null)||
       ( serverPort < 0)||
       ( serverBackLog <= 0)||
       ( this.diskSpaceThreshold <= 0)||
       ( this.targetPath == null)||
       ( this.sizeDeletionType < 0)||
       ( this.defaultSetting_log < 0)||
       ( this.duration_logs[0] == null)||
       ( this.duration_logs[1] == null)||
       ( this.logPath == null))
       {
	    System.out.println("<Server.startOp()> Invalid value from properties file ");
	    log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<Server.startOp()> Invalid value from properties file ");
	    System.exit(0);
	   }

     this.ARpaths = new String[noOfPaths];
     for(int a = 0; a < this.ARpaths.length; a++)
        {
		 this.ARpaths[a] = prop.getProperty("path_"+a);
		}


     new Thread(new Server(serverIp,
	                       serverPort,
	                       this.logRoot,
	                       this.logName,
	                       serverBackLog,
                           this)).start();

     new checkForDeletion().doTask(0, checkfrequency);

     fis.close();
     fis= null;

} catch (Exception e){
         e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<Server.startOp()> error: "+e);
         System.exit(0);
}
}



private void updatePropertiesFile(String[][] fieldAndData){

try {
	 Properties prop = new Properties();
	 FileInputStream fis = new FileInputStream(this.propFile);
	 prop.load(fis);
	 FileOutputStream fout = new FileOutputStream(this.propFile);

	 for(int a = 0; a < fieldAndData.length; a++)
	    {
	     prop.setProperty(fieldAndData[a][0], fieldAndData[a][1]);
	    }

     prop.store(fout, null);
	 fis.close();
	 fout.close();
	 fis = null;
	 fout = null;

} catch (Exception e){
         e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<Server.updatePropertiesFile()> error: "+e);
}
}



private String deriveSizeInBytes(String value, String type){

String derivedValue = "0";

try {
     long longValue = Long.parseLong(value);

     if(type.equalsIgnoreCase("KB"))
       {
	    long inBytes = (longValue * 1000);
	    derivedValue = Long.toString(inBytes);
	   }
     else if(type.equalsIgnoreCase("MB"))
       {
	    long inBytes = (longValue * 1000000);
	    derivedValue = Long.toString(inBytes);
	   }
     else if(type.equalsIgnoreCase("GB"))
       {
        long inBytes = (longValue * 1000000000);
	    derivedValue = Long.toString(inBytes);
	   }

} catch (Exception e){
         derivedValue = "0";
         e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<Server.deriveSizeInBytes()> error: "+e);
}
return derivedValue;
}



public synchronized void set_stats(String del_type,
                                   String value,
                                   String settings,
                                   int type){
try {
     if((del_type != "")&&
       ( value != "")&&
       ( settings != ""))
       {
        int settingsVal = Integer.parseInt(settings);
        this.defaultSetting = settingsVal;
        String[][] fieldAndData = null;

        if(type == 1)
          {
           fieldAndData = new String[3][2];
           this.duration[0] = value;
           this.duration[1] = del_type;

           fieldAndData[0][0] = "durationValue";  fieldAndData[0][1] = value;
           fieldAndData[1][0] = "durationType";   fieldAndData[1][1] = del_type;
           fieldAndData[2][0] = "defaultSetting"; fieldAndData[2][1] = Integer.toString(this.defaultSetting);
		  }
		else if(type == 2)
		  {
           fieldAndData = new String[3][2];
           this.duration_logs[0] = value;
           this.duration_logs[1] = del_type;

           fieldAndData[0][0] = "durationValue_logs"; fieldAndData[0][1] = value;
           fieldAndData[1][0] = "durationType_logs";  fieldAndData[1][1] = del_type;
           fieldAndData[2][0] = "defaultSetting_log"; fieldAndData[2][1] = Integer.toString(this.defaultSetting_log);
	      }
		else if(type == 3)
		  {
           fieldAndData = new String[2][2];
           long derivedVal = Long.parseLong(this.deriveSizeInBytes(value, del_type));
           if(derivedVal != 0)
             {
              this.sizeLimit = derivedVal;
		     }
		   else
		     {
			  log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<Server.set_timeoutStats()> msg: Derived timeout value ignored cos its 0!");
			 }

           fieldAndData[0][0] = "sizeLimit"; fieldAndData[0][1] = Long.toString(this.sizeLimit);
           fieldAndData[1][0] = "defaultSetting_log"; fieldAndData[1][1] = Integer.toString(this.defaultSetting_log);
	      }
        this.updatePropertiesFile(fieldAndData);
	   }

} catch (Exception e){
         e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<Server.set_timeoutStats()> error: "+e);
}
}




public synchronized void set_rangeStats(String startDate,
                                        String endDate,
                                        String settings){
try {
     if((startDate != "")&&
       ( endDate != "")&&
       ( settings != ""))
       {
        int settingsVal = Integer.parseInt(settings);
        String[][] fieldAndData = new String[3][2];

        this.defaultSetting = settingsVal;
        this.dateRange[0] = startDate;
        this.dateRange[1] = endDate;

        fieldAndData[0][0] = "startDate";      fieldAndData[0][1] = startDate;
        fieldAndData[1][0] = "endDate";        fieldAndData[1][1] = endDate;
        fieldAndData[2][0] = "defaultSetting"; fieldAndData[2][1] = Integer.toString(this.defaultSetting);

        this.updatePropertiesFile(fieldAndData);
	   }

} catch (Exception e){
         e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<Server.set_rangeStats()> error: "+e);
}
}



public void doManualDelete(String fileName, boolean filesOnly){

try {
     boolean deleted = false;

     if(filesOnly)
       {
	    new doDeletion().deleteFilesOnly(fileName,logRoot,logName);

	    //new doDeletion(fileName, true).run(logRoot, logName);
	   }
     else
       {
        deleted = new doDeletion().deleteDir(new File(fileName));
       }
     if(!deleted)
       {
	    log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<Server.doManualDelete()> msg:  failed to delete file "+fileName);
	   }
} catch (Exception e){
         e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<Server.doManualDelete()> error: "+e);
}
}






public synchronized void doOp(){

try {
	 if(defaultSetting == 1)
	   {
	    this.option_1("older", false);
	   }
	 else if(defaultSetting == 2)
	   {
	    this.option_1("younger", false);
	   }
	 else if(defaultSetting == 3)
	   {
	    this.option_1("range", false);
	   }
	 else if(defaultSetting == 4) //older & range
	   {
	    this.option_1("older", false);
	    this.option_1("range", false);
	   }
	 else if(defaultSetting == 5) //younger & range
	   {
	    this.option_1("younger", false);
	    this.option_1("range", false);
	   }
	 else
	   {
	    log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<Server.doOp()> msg: deletion setting not set >> "+defaultSetting);
	   }

	this.doOp_logs(); //do logs..
	this.option_2(); //Disk space limit (mandatory).

} catch (Exception e){
	     e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<Server.doOp()> error: "+e);
}
}




public synchronized void doOp_logs(){

try {
	 if(defaultSetting_log == 1)
	   {
	    this.option_1("older", true);
	   }
	 else if(defaultSetting_log == 2)
	   {
	    this.option_1("younger", true);
	   }
	 else
	   {
	 	log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<Server.doOp_logs()> msg: deletion setting not set >> "+defaultSetting_log);
	   }
} catch (Exception e){
	     e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<Server.doOp_logs()> error: "+e);
}
}



//older or younger than, or range....
private void option_1(String measureType, boolean isLog){

try {
	 int dType = -1;
	 String[] fileToDel = null;

	 if(measureType.equals("range"))
	   {
	    dType = 1;
		fileToDel = this.dateRange;
		this.startDeletion(dType, fileToDel);
	   }
	 else
	   {
		if(measureType.equals("older"))
		  {
		   dType = 2;
		  }
		else if(measureType.equals("younger"))
		  {
		   dType = 3;
		  }
		fileToDel = new String[1];
		String dDate = null;

		if(isLog)
		  {
		   dDate = this.deriveDate(this.duration_logs[0], this.duration_logs[1], measureType);
		  }
		else
		  {
		   dDate = this.deriveDate(this.duration[0], this.duration[1], measureType);
	      }
		if(dDate != null)
		  {
		   fileToDel[0] = dDate;
		   if(isLog)
		     {
		      this.startDeletion(dType, fileToDel, this.logPath);
		     }
		   else
		     {
		      this.startDeletion(dType, fileToDel);
		     }
		  }
       }

} catch (Exception e){
	     e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<Server.option_1()> error: "+e);
}
}




//disk space limit...
private void option_2(){

try {
     File tar_file = new File(this.targetPath);
     if(tar_file.getUsableSpace() < this.sizeLimit)
       {
        String currentDate = genericJobs.generate_imageStoreName();
        if(currentDate == null)
          {
		   log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<Server.option_2()> msg: deletion <disk limit> msg: Cannot generate current date");
		  }
		else
		  {
		   while(tar_file.getUsableSpace() < this.diskSpaceThreshold)
		    {
             Vector filesFound = new Vector(5);
             filesFound = new doDeletion().find_oldestOrYoungest(filesFound,
	                                                             this.targetPath,
                                                                 this.sizeDeletionType,
                                                                 logRoot,
                                                                 logName);

             String oldestOrYoungestPathName = new doDeletion().compareAndReturn(filesFound,
                                                                                 this.sizeDeletionType,
 	                                                                             logRoot,
                                                                                 logName);
		    if(oldestOrYoungestPathName == null)
		      {break;}
		    if(oldestOrYoungestPathName.equals(currentDate))
		      {break;}

            new doDeletion().deleteThisDirOnly(this.targetPath,
			                                   oldestOrYoungestPathName,
			                                   logRoot,
                                               logName);
		    }
	      }
	   }

} catch (Exception e){
	     e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<Server.option_2()> error: "+e);
}
}




private void startDeletion(int dType, String[] fileToDel){

try {
	 for(int a = 0; a < this.ARpaths.length; a++)
	    {
		 if(this.ARpaths[a] != null)
		   {
		    new doDeletion(dType, fileToDel, this.ARpaths[a]).run(logRoot, logName);
		   }
	    }
} catch (Exception e){
	     e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<Server.startDeletion()> error: "+e);
}
}


private void startDeletion(int dType, String[] fileToDel, String filePath){

try {
     if(filePath != null)
	   {
		new doDeletion(dType, fileToDel, filePath).run(logRoot, logName);
	   }
} catch (Exception e){
	     e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<Server.startDeletion(variant 1)> error: "+e);
}
}




private String deriveDate(String value, String measureType, String opType){

String derivedDate = null;
try {
     boolean continueOp = true;
     int valueNo = Integer.parseInt(value);
     String currentDate = genericJobs.generate_imageStoreName();
     if(currentDate ==  null)
       {
	    continueOp = false;
	    log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<Server.deriveDate()> msg: failed too generate current date ");
	   }

	 if(continueOp)
	   {
		 String[] currentDate_split = currentDate.split("-");

		 if((measureType.equals("day"))||
		   ( measureType.equals("days"))||
		   ( measureType.equals("week"))||
		   ( measureType.equals("weeks")))
		   {
            if((measureType.equals("week"))||
              ( measureType.equals("weeks")))
              {
			   valueNo = (7 * valueNo);
			  }

            if(opType.equals("older"))
              {
               valueNo = -valueNo;
               derivedDate = this.getActualDate(currentDate_split,valueNo, "day");
			  }
            else if(opType.equals("younger"))
              {
               derivedDate = this.getActualDate(currentDate_split,valueNo, "day");
			  }
            else
              {
			   log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<Server.deriveDate() -1> msg: Invalid  opType "+opType);
			  }
		   }
		 else if((measureType.equals("month"))||
		        ( measureType.equals("months")))
		   {
            if(opType.equals("older"))
              {
               valueNo = -valueNo;
               derivedDate = this.getActualDate(currentDate_split,valueNo, "month");
			  }
            else if(opType.equals("younger"))
              {
               derivedDate = this.getActualDate(currentDate_split,valueNo, "month");
			  }
            else
              {
			   log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<Server.deriveDate() -2> msg: Invalid  opType "+opType);
			  }
		   }
         else if((measureType.equals("year"))||
		        ( measureType.equals("years")))
		   {
            if(opType.equals("older"))
              {
               valueNo = -valueNo;
               derivedDate = this.getActualDate(currentDate_split,valueNo, "year");
			  }
            else if(opType.equals("younger"))
              {
               derivedDate = this.getActualDate(currentDate_split,valueNo, "year");
			  }
            else
              {
			   log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<Server.deriveDate() -3> msg: Invalid  opType "+opType);
			  }
		   }
		 else
		   {
			log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<Server.deriveDate()> msg: Invalid measure type "+measureType);
		   }
       }

} catch (Exception e){
	     derivedDate = null;
	     e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<Server.deriveDate()> error: "+e);
}
return derivedDate;
}




private String getActualDate(String[] current_date,
                             int opValue,
                             String measureType){
String actualDate = null;
try{
    //quick hack for Calendar class compatibility:
    String monthVal = current_date[1];
	int monthValNo = Integer.parseInt(monthVal);
    monthValNo = monthValNo - 1;


    Calendar ca1 = Calendar.getInstance();
    ca1.set(Integer.parseInt(current_date[0]),
            monthValNo,
            Integer.parseInt(current_date[2]));

    if(measureType.equals("day"))
      {
       ca1.add(Calendar.DATE, opValue);
	  }
    else if(measureType.equals("month"))
      {
       ca1.add(Calendar.MONTH, opValue);
	  }
    else if(measureType.equals("year"))
      {
       ca1.add(Calendar.YEAR, opValue);
	  }

    int year = ca1.get(Calendar.YEAR);
    int month = ca1.get(Calendar.MONTH);
    int day = ca1.get(Calendar.DATE);

    String yr = Integer.toString(year);
    String dy = Integer.toString(day);
    String mnth = arrangeMonth(month);
    if(mnth == null)
      {
	   actualDate = null; //just in case..
	  }
	else
	  {
	   if(yr.length() <= 1)
		 {
		  yr = "0"+yr;
		 }
	   if(mnth.length() <= 1)
		 {
		  mnth = "0"+mnth;
		 }
	   if(dy.length() <= 1)
		 {
		  dy = "0"+dy;
		 }

		actualDate = yr+"-"+mnth+"-"+dy;
      }

} catch (Exception e){
	     actualDate = null;
	     e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<Server.getActualDate()> error: "+e);
}
return actualDate;
}


//arranges month retrieved from Calendar class
//in the format we want.
private String arrangeMonth(int mnthVal){

String arrangedMnth = null;
try {
     if(mnthVal == 0)
       {
	    arrangedMnth = "01";
	   }
	 else if(mnthVal == 1)
	   {
	 	arrangedMnth = "02";
	   }
	 else if(mnthVal == 2)
	   {
	    arrangedMnth = "03";
	   }
	 else if(mnthVal == 3)
	   {
	 	arrangedMnth = "04";
	   }
	 else if(mnthVal == 4)
	   {
	 	arrangedMnth = "05";
	   }
	 else if(mnthVal == 5)
	   {
	 	arrangedMnth = "06";
	   }
	 else if(mnthVal == 6)
	   {
	 	arrangedMnth = "07";
	   }
	 else if(mnthVal == 7)
	   {
	 	arrangedMnth = "08";
	   }
	 else if(mnthVal == 8)
	   {
	 	arrangedMnth = "09";
	   }
	 else if(mnthVal == 9)
	   {
	 	arrangedMnth = "10";
	   }
	 else if(mnthVal == 10)
	   {
	 	arrangedMnth = "11";
	   }
	 else if(mnthVal == 11)
	   {
	 	arrangedMnth = "12";
	   }
	 else
	   {
	    log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<Server.arrangeMonth()> msg: Invalid month value "+mnthVal);
	   }

} catch (Exception e){
	     arrangedMnth = null;
	     e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<Server.arrangeMonth()> error: "+e);
}
return arrangedMnth;
}




/////////////////////////////////////////
//
//   Periodically checks deletion stats
//   and acts accordingly.
//
////////////////////////////////////////

class checkForDeletion  {

private Timer timer;

public checkForDeletion(){

timer = new Timer ();

}


public void doTask(int start_time, int delay_in_secs){

timer.scheduleAtFixedRate(new checkForDeletion_worker(),
                          (start_time * 1000),
                          (delay_in_secs * 1000));
}


class checkForDeletion_worker extends TimerTask{

public void run(){

doOp();

}
}
}



public static void main(String[] args){


new manager().startOp(args[0]);


//new manager().startOp("C:\\mike_work\\config\\deletion_properties.PROPERTIES");
}

}