/*******************************************************
*
*     Author: Mike bass.
*     Year: 2012.
*     App name: log_writer .
*
*     Description: writes desired log ..
*
*******************************************************/

package org.dcm4che2.net;

//standard java packages..
import java.io.*;
import java.text.*;
import java.util.*;

//QR manager packages..
import org.dcm4che2.tool.dcmrcv.dcm_folderCreator;



public class log_writer {

public static boolean doLogWriting = true;

private static int writeCount = 0;


public log_writer(){}




public static void doLogging_QRmgr(String loc, String logName, String data){

if(log_writer.doLogWriting)
  {
	try {

	if(loc != null)
	  {
	   String cDate = dcm_folderCreator.generate_imageStoreName();
	   String compsdName = loc+"/"+cDate;
	   compsdName = compsdName.replaceAll("//", "/");
	   File newFile = new File(compsdName);
	   boolean created = true;
	   if(!newFile.exists())
		 {
		  created = newFile.mkdirs();
		 }

	   if(created)
		 {
		  if(logName != null)
			{
			 String fileName = (newFile.getAbsolutePath()+"/"+logName);
			 fileName = fileName.replaceAll("\\\\", "/");
			 fileName = fileName.replaceAll("//", "/");
			 new fileWriter_1(fileName).write(data);
			}
		 }
		else
		 {
		  System.out.println("<doLogging_QRmgr - int>: Could not create log file");
		 }
	  }
	} catch (Exception e){
			 e.printStackTrace();
	}
}
}


//===================================
//
// Author@ Mike Bass...2012
//
// writes to desired file.
//===================================

static class fileWriter_1 {

 private String fileName = null;


 public fileWriter_1(String fileName){

 this.fileName = fileName;
 }

 public void write(String s){

 if(this.fileName != null)
   {
    write(this.fileName, s);
   }
 }


 public void write(String[] s){

 if(this.fileName != null)
   {
    for(int a = 0; a < s.length; a++)
       {
        write(this.fileName, s[a]);
       }
   }
 }

 public void write(String f, String s){

 try {

	 TimeZone tz = TimeZone.getTimeZone("GMT"); // or PST, MID, etc ...
	 Date now = new Date();
	 DateFormat df = new SimpleDateFormat ("EEE: dd/MM/yyyy hh:mm:ss:SSSSSSSS ");
	 df.setTimeZone(tz);
	 String currentTime = df.format(now);

	 FileWriter aWriter = new FileWriter(f, true);
	 aWriter.write(currentTime + " " + s + "\r\n");

	 //aWriter.write("No: "+(writeCount++)+", "+ s + "\r\n");


	 aWriter.flush();
	 aWriter.close();

 } catch (Exception e){
	      System.out.println("=== fileWriter_1 error:");
          e.printStackTrace();
 }

 }

 }


}//end class