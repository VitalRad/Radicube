/***********************************************************************
*
*     Author:   Mike bass.
*     Year:     2012.
*     App name: genericJobs .
*     Purpose:  Used to conduct general (mostly file-related) tasks.
*
************************************************************************/

package utility.general;


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



public class genericJobs {


public genericJobs(){}



public static String prepareFilePath(String fPath, String logRoot, String logName){

try {
     fPath = fPath.replaceAll("\\\\","/");
     fPath = fPath.replaceAll("//","/");
} catch (Exception e){
	     fPath = null;
         e.printStackTrace();
         log_writer.doLogging_QRmgr(logRoot, logName,
         "<genericJobs.prepareFilePath()> error: "+e);
}
return fPath;
}



public static String createNameAndFolder_imageStore(String image_location){

String createdFileName = null;
try {

String arc_name = genericJobs.generate_imageStoreName();
image_location = image_location.replaceAll("\\\\", "/");
image_location = image_location+"/";
image_location = image_location.replaceAll("//", "/");
arc_name = image_location+arc_name;
image_location = arc_name;
createdFileName = genericJobs.inspectAndCreateImageStore(image_location);

} catch (Exception e){
	     createdFileName = null;
         e.printStackTrace();
         //log_writer.doLogging_QRmgr(3, "<genericJobs - createNameAndFolder_imageStore> "+e);
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
		             //log_writer.doLogging_QRmgr(3, "<genericJobs> "+e1);
		     }
		    }
		}
     }
     createdFile = genericJobs.createDirectory(count, image_location);
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
		   System.out.println("====== <genericJobs> createDirectory().couldn't create directory for image store.");
		   //log_writer.doLogging_QRmgr(3, "====== <genericJobs> createDirectory().couldn't create directory for image store.");
		  }

   } catch(Exception e1){
		   newFile = null;
		   System.out.println("====== createDirectory() error: exception in creating image store directory: "+e1);
		   e1.printStackTrace();
		   //log_writer.doLogging_QRmgr(3, "====== <genericJobs> createDirectory().couldn't create directory for image store."+e1);
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
         //log_writer.doLogging_QRmgr(3, "====== <genericJobs> generate_imageStoreName"+e);
}
return name;
}




public static boolean copyfile(String srFile,
                               String dtFile,
                               String logRoot,
                               String logName){
boolean successful = true;
InputStream in = null;
OutputStream out = null;
try {
     File f1 = new File(srFile);
     File f2 = new File(dtFile);
     in = new FileInputStream(f1);
     out = new FileOutputStream(f2);
     byte[] buf = new byte[2048];
     int len = 0;
     while((len = in.read(buf)) > 0)
      {
       out.write(buf, 0, len);
      }
} catch (Exception e){
	     successful = false;
         e.printStackTrace();
         log_writer.doLogging_QRmgr(logRoot, logName,
         "<genericJobs.copyfile()> error: "+e);
}finally {
          try {
			  if((in != null)&&(out != null))
			    {
			  	 in.close();
			     out.close();
			    }
		 } catch (Exception cls){
		          cls.printStackTrace();
		          log_writer.doLogging_QRmgr(logRoot, logName,
                  "<genericJobs.copyfile().stream closing> error: "+cls);
		 }
}
return successful;
}


public static void sleepForDesiredTime(int duration){

try {
	 Thread.sleep(duration);
} catch(InterruptedException ef) {
		ef.printStackTrace();
}
}


public static boolean deleteDir(File dir){

if (dir.isDirectory()) {
    String[] children = dir.list();
    for (int i=0; i<children.length; i++) {
         boolean success = deleteDir(new File(dir, children[i]));
         if (!success) {
             return false;
         }
    }
}
// The directory is now empty so delete it
return dir.delete();
}

public static boolean deleteFilesInDir(File dir, boolean success){

if (dir.isDirectory()) {
    String[] children = dir.list();
    for (int i=0; i<children.length; i++) {
         success = deleteFilesInDir(new File(dir, children[i]), success);
         if (!success) {
             return false;
         }
    }
}
return success;
}



public static void deleteFilesInDir(File dir){

  File[] files = dir.listFiles();
  if(files != null)
    {
     for(int a = 0; a < files.length; a++)
        {
         if(files[a].isDirectory())
           {
		    genericJobs.deleteFilesInDir(files[a]);
		   }
		 else
		   {
            files[a].delete();
		   }
		}
	}
}



//Traverses and copies everything from one folder to another.
public static void copyAll(File src,
                           File dest,
                           String logRoot,
                           String logName)
{
 try
 {
  File[] files = src.listFiles();
  if(files != null)
    {
     for(int a = 0; a < files.length; a++)
        {
         if(files[a].isDirectory())
           {
		    genericJobs.copyAll(files[a], dest, logRoot, logName);
		   }
		 else
		   {
            String destFile = dest.getAbsolutePath()+"/"+files[a].getName();
            destFile = genericJobs.prepareFilePath(destFile, null, null);
            boolean moved = files[a].renameTo(new File(destFile));
            if(!moved)
              {
			   //System.out.println("Failed to move file: "+files[a].getAbsolutePath()+", to "+destFile);
			   log_writer.doLogging_QRmgr(logRoot, logName,
               "<genericJobs> Failed to move file: "+files[a].getAbsolutePath()+", to "+destFile);
			  }
		   }
		}
	}

 } catch (Exception e)
 {
   e.printStackTrace();

   log_writer.doLogging_QRmgr(logRoot, logName,
   "<genericJobs> copyAll() exception "+e);
 }
}


}

