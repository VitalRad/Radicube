/***********************************************************
*
*     Author:      Mike bass.
*     Year:        2012.
*     App name:    dcm_folderCreator .
*     Description: Handles all file related manipulations.
*
************************************************************/

package org.dcm4che2.tool.dcmrcv;


//standard java packages.
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.TimeZone;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.DateFormat;


import org.dcm4che2.net.log_writer;



public class dcm_folderCreator{


public dcm_folderCreator(){}




public static String createNameAndFolder_imageStore(String image_location){

String createdFileName = null;
try {

String arc_name = dcm_folderCreator.generate_imageStoreName();
image_location = image_location.replaceAll("\\\\", "/");
image_location = image_location+"/";
image_location = image_location.replaceAll("//", "/");
arc_name = image_location+arc_name;
image_location = arc_name;
createdFileName = dcm_folderCreator.inspectAndCreateImageStore(image_location);

} catch (Exception e){
	     createdFileName = null;
         e.printStackTrace();
         //log_writer.doLogging_QRmgr(3, "<dcm_folderCreator - createNameAndFolder_imageStore> "+e);
}
return createdFileName;
}


private static synchronized String inspectAndCreateImageStore(String image_location){

String newFilename = null;

if(image_location != null)
  {
   File createdFile = null;
   File file = new File(image_location);
   int count = 1;
   boolean continueOp = true;
   boolean fileExists = file.exists();
   if(fileExists)
     {
      File[] list = file.listFiles();
      if(list != null)
        {
		 for(int a = 0; a < list.length; a++)
		    {
		     try {
		          int fName = Integer.parseInt(list[a].getName());
		          if(fName >= count){count = (fName + 1);}
		     } catch(Exception e1){
		             e1.printStackTrace();
		             //log_writer.doLogging_QRmgr(3, "<dcm_folderCreator> "+e1);
		     }
		    }
		}
     }
     createdFile = dcm_folderCreator.createDirectory(count, image_location);
     if(createdFile == null)
       {
		continueOp = false;
		newFilename = null;
	   }
	 else
	   {
	    newFilename = createdFile.toString();
	   }
   }
return newFilename;
}



private static File createDirectory(int count, String image_location){

 image_location = image_location+"/"+count+"/";
 image_location = image_location.replaceAll("\\\\", "/");
 image_location = image_location.replaceAll("//", "/");

 File newFile = new File(image_location);
 if(!newFile.exists())
   {
   try {
		boolean results = newFile.mkdirs();
		if(!results)
		  {
		   newFile = null;
		   System.out.println("====== <dcm_folderCreator> createDirectory().couldn't create directory for image store.");
		   //log_writer.doLogging_QRmgr(3, "====== <dcm_folderCreator> createDirectory().couldn't create directory for image store.");
		  }

   } catch(Exception e1){
		   newFile = null;
		   System.out.println("====== createDirectory() error: exception in creating image store directory: "+e1);
		   e1.printStackTrace();
		   //log_writer.doLogging_QRmgr(3, "====== <dcm_folderCreator> createDirectory().couldn't create directory for image store."+e1);
   }
  }
return newFile;
}






public static boolean createExactlyThatLocation(String locationToCreate){

boolean created = true;
try {
     File newFile = new File(locationToCreate);
     if(!newFile.exists())
       {
		created = newFile.mkdirs();
	   }
} catch(Exception e){
		created = false;
		e.printStackTrace();
}
return created;
}



public static String generate_imageStoreName(){
String name = null;
try {
     TimeZone tz = TimeZone.getTimeZone("GMT"); // or PST, MID, etc ...
     Date now = new Date();
     //DateFormat df = new SimpleDateFormat ("EEE: dd/MM/yyyy hh:mm:ss ");
     DateFormat df = new SimpleDateFormat ("yyyy-MM-dd");
     df.setTimeZone(tz);
     name = df.format(now);

} catch (Exception e){
         name = null;
         e.printStackTrace();
         //log_writer.doLogging_QRmgr(3, "====== <dcm_folderCreator> generate_imageStoreName"+e);
}
return name;
}



public static String get_currentDateAndTime(){
String name = null;
try {
     TimeZone tz = TimeZone.getTimeZone("GMT"); // or PST, MID, etc ...
     Date now = new Date();
     //DateFormat df = new SimpleDateFormat ("EEE: dd/MM/yyyy hh:mm:ss ");
     DateFormat df = new SimpleDateFormat ("yyyy-MM-dd hh:mm:ss a");
     df.setTimeZone(tz);
     name = df.format(now);

} catch (Exception e){
         name = null;
         e.printStackTrace();
         //log_writer.doLogging_QRmgr(3, "====== <dcm_folderCreator> generate_imageStoreName"+e);
}
return name;
}




public static String addOrSubtractTime(String startDate,
                                       String endDate,
                                       int opType,
                                       String formatType,
	                                   String logRoot,
                                       String logName){
String convertedValue = null;
try {
     //java.text.DateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

     java.text.DateFormat df = new java.text.SimpleDateFormat(formatType);
     df.setTimeZone(TimeZone.getTimeZone("GMT"));
     java.util.Date date1 = df.parse(startDate);
     java.util.Date date2 = df.parse(endDate);

     long diff = 0L;
     if(opType == 1)
       {
        diff = date2.getTime() + date1.getTime();
       }
     else if(opType == 2)
	   {
	    diff = date2.getTime() - date1.getTime();
       }

     float f = new Long(diff).floatValue();
     f = (f/1000/60);
     f = dcm_folderCreator.Round(f, 2, logRoot, logName);

     if(f < 1)
	   {
	 	f = f * 60;
	 	f = dcm_folderCreator.Round(f, 2, logRoot, logName);
	 	convertedValue = Float.toString(f) + " sec(s)";
	   }
     else
       {
		 convertedValue = Float.toString(f) + " min(s)";
		 if(f >= 60) //mins to hours..
		   {
			f = dcm_folderCreator.convertToDesiredTime(f, 1, logRoot, logName);
			f = dcm_folderCreator.Round(f, 2, logRoot, logName);
			convertedValue = Float.toString(f) + " hour(s)";
			if(f >= 24) //hours to day..
			  {
			   f = dcm_folderCreator.convertToDesiredTime(f, 2, logRoot, logName);
			   f = dcm_folderCreator.Round(f, 2, logRoot, logName);
			   convertedValue = Float.toString(f) + " day(s)";
			  }
		   }
       }
} catch (Exception e){
         e.printStackTrace();
         log_writer.doLogging_QRmgr(logRoot, logName, "<dcm_folderCreator> addOrSubtractTime() exception: "+e);
}
return convertedValue;
}







public static String convertToDesiredTimeFormat(long diff, String logRoot, String logName){

String convertedValue = null;
try {
     float f = new Long(diff).floatValue();
     f = (f/1000/60);
     f = dcm_folderCreator.Round(f, 2, logRoot, logName);

     if(f < 1)
	   {
		f = f * 60;
		f = dcm_folderCreator.Round(f, 2, logRoot, logName);
		convertedValue = Float.toString(f) + " sec(s)";
	   }
	  else
		{
		 convertedValue = Float.toString(f) + " min(s)";
		 if(f >= 60) //mins to hours..
		   {
			f = dcm_folderCreator.convertToDesiredTime(f, 1, logRoot, logName);
			f = dcm_folderCreator.Round(f, 2, logRoot, logName);
			convertedValue = Float.toString(f) + " hour(s)";
			if(f >= 24) //hours to day..
			  {
			   f = dcm_folderCreator.convertToDesiredTime(f, 2, logRoot, logName);
			   f = dcm_folderCreator.Round(f, 2, logRoot, logName);
			   convertedValue = Float.toString(f) + " day(s)";
			  }
		   }
       }
} catch (Exception e){
         e.printStackTrace();
         log_writer.doLogging_QRmgr(logRoot, logName, "<dcm_folderCreator> convertToDesiredTimeFormat() exception: "+e);
}
return convertedValue;
}





public static float convertToDesiredTime(float value, int type, String logRoot, String logName){

try {
     if(type == 1) //secs to mins or mins to hours
       {
	    value = (value/60);
	   }
     if(type == 2) //hours to day
       {
        value = (value/24);
	   }
} catch (Exception e){
         e.printStackTrace();
         log_writer.doLogging_QRmgr(logRoot, logName, "<dcm_folderCreator> addOrSubtractTime() exception: "+e);
}
return value;
}



public static long addOrSubtractTimeAsLong(String startDate,
                                           String endDate,
                                           int opType,
                                           String formatType,
	                                       String logRoot,
                                           String logName){
long diff = 0L;
try {
     //java.text.DateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

     java.text.DateFormat df = new java.text.SimpleDateFormat(formatType);
     df.setTimeZone(TimeZone.getTimeZone("GMT"));
     java.util.Date date1 = df.parse(startDate);
     java.util.Date date2 = df.parse(endDate);
     if(opType == 1)
       {
        diff = date2.getTime() + date1.getTime();
       }
     else if(opType == 2)
	   {
	    diff = date2.getTime() - date1.getTime();
       }

} catch (Exception e){
         e.printStackTrace();
         log_writer.doLogging_QRmgr(logRoot, logName, "<dcm_folderCreator> addOrSubtractTimeAsLong() exception: "+e);
}
return diff;
}



public static float Round(float Rval, int Rpl, String logRoot, String logName) {

float val = -01f;
try {
     float p = (float)Math.pow(10,Rpl);
     Rval = Rval * p;
     float tmp = Math.round(Rval);
     val = (float)tmp/p;

} catch (Exception e){
         e.printStackTrace();
         log_writer.doLogging_QRmgr(logRoot, logName, "<dcm_folderCreator> Round() exception: "+e);
}
return val;
}


}//end of class.

