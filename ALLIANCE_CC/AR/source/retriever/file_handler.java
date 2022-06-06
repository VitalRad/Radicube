/***********************************************************
*
*     Author: Mike bass.
*     Year: 2012.
*     App name: file_handler .
*
*     Description: Handles all file related manipulations.
*
************************************************************/

package retriever;


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



public class file_handler{


public file_handler(){}



public static boolean copyfile(String srFile, String dtFile){

boolean successful = true;
InputStream in = null;
OutputStream out = null;

try {
     File f1 = new File(srFile);
     File f2 = new File(dtFile);
     in = new FileInputStream(f1);
     out = new FileOutputStream(f2);
     byte[] buf = new byte[1024];
     int len = 0;
     while((len = in.read(buf)) > 0)
      {
       out.write(buf, 0, len);
      }
     in.close();
     out.close();
     //System.out.println("File copied.");

} catch (Exception e){
	     successful = false;
         e.printStackTrace();
         //logWriter.doLogging_QRmgr(checkAndRetrieve.logPathName, checkAndRetrieve.logFileName, "<file_handler> "+e);
         try {
			  if((in != null)&&(out != null))
			    {
			  	 in.close();
			     out.close();
			    }
		 } catch (Exception ee){
		          ee.printStackTrace();
		          //logWriter.doLogging_QRmgr(checkAndRetrieve.logPathName, checkAndRetrieve.logFileName, "<file_handler> "+ee);
		 }
}
return successful;
}







public static boolean copyfile(String srFile, String dtFile, boolean append){

boolean successful = true;
InputStream in = null;
OutputStream out = null;

try {
     File f1 = new File(srFile);
     File f2 = new File(dtFile);
     in = new FileInputStream(f1);
     out = new FileOutputStream(f2, append);
     byte[] buf = new byte[1024];
     int len = 0;
     while((len = in.read(buf)) > 0)
      {
       out.write(buf, 0, len);
      }
     in.close();
     out.close();
     //System.out.println("File copied.");

} catch (Exception e){
	     successful = false;
         e.printStackTrace();
         //logWriter.doLogging_QRmgr(checkAndRetrieve.logPathName, checkAndRetrieve.logFileName, "<file_handler> "+e);
         try {
			  if((in != null)&&(out != null))
			    {
			  	 in.close();
			     out.close();
			    }
		 } catch (Exception ee){
		          ee.printStackTrace();
		          //logWriter.doLogging_QRmgr(checkAndRetrieve.logPathName, checkAndRetrieve.logFileName, "<file_handler> "+ee);
		 }
}
return successful;
}




public static void moveFilesToDest(File source,
                                   String Destination,
                                   String[] exempt_startsWith,
                                   String[] exempt_endsWith) {
 try {
	  File[] files = source.listFiles();
	  if(files != null)
		{
		 for(File f : files)
			{
			 if(f.isDirectory())
			   {
				file_handler.moveFilesToDest(f, Destination, exempt_startsWith, exempt_endsWith);
			   }
			 if(file_handler.checkString(f.getName(), exempt_startsWith, 1)){}
			 else if(file_handler.checkString(f.getName(), exempt_endsWith, 2)){}
			 else
			   {
				boolean success = f.renameTo(new File(Destination, f.getName()));
				if(!success)
				  {
				   System.out.println("<file_handler.moveFilesToDest()> could not move file: "+f.getName());
				   //logWriter.doLogging_QRmgr(checkAndRetrieve.logPathName, checkAndRetrieve.logFileName, "<file_handler.moveFilesToDest()> could not move file: "+f.getName());
				  }

			   }

			 }
		  }

   } catch (Exception e) {
	        System.out.println("<file_handler.moveFilesToDest()> error: "+e);
            //logWriter.doLogging_QRmgr(checkAndRetrieve.logPathName, checkAndRetrieve.logFileName, "<file_handler.moveFilesToDest()> error: "+e);
   }
}



public static boolean checkString(String subject, String[] cases, int queryType){

boolean val = false;

try {
     if(cases != null)
       {
        for(int x = 0; x < cases.length; x++)
           {
	        if(cases[x] == null)
	          {}
	        else if(queryType == 1) //startsWith..
	          {
               if(subject.startsWith(cases[x]))
                 {
			      val = true;
			      break;
			     }
		      }
		    else if(queryType == 2)//endsWith
		      {
               if(subject.endsWith(cases[x]))
                 {
			      val = true;
			      break;
			     }
		      }
		    else
		      {
		       val = false;
		       System.out.println("<file_handler.checkString()> invalid query type: "+queryType);
               //logWriter.doLogging_QRmgr(checkAndRetrieve.logPathName, checkAndRetrieve.logFileName, "<file_handler.checkString()> invalid query type: "+queryType);
               break;
		      }
	       }
       }
} catch (Exception e) {
	     val = false;
	     System.out.println("<file_handler.checkString()> error: "+e);
         //logWriter.doLogging_QRmgr(checkAndRetrieve.logPathName, checkAndRetrieve.logFileName, "<file_handler.checkString()> error: "+e);
}
return val;
}








// Deletes all files and subdirectories under dir.
// Returns true if all deletions were successful.
// If a deletion fails, the method stops attempting to delete and returns false.
public static boolean deleteDir(File dir) {
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



//Moves a file or folder into another...
public static boolean MOVE_DIR(String src, String dest){

boolean success = false;

try {
     File srcfile = new File(src);
     File destFile = new File(dest);
     success = srcfile.renameTo(new File(destFile, srcfile.getName()));

} catch (Exception e){
         e.printStackTrace();
         success = false; //just in case..
         //logWriter.doLogging_QRmgr(checkAndRetrieve.logPathName, checkAndRetrieve.logFileName, "<file_handler> "+e);
}
return success;
}




public static String createNameAndFolder_imageStore(String image_location){

String createdFileName = null;
try {

String arc_name = file_handler.generate_imageStoreName();
image_location = image_location.replaceAll("\\\\", "/");
image_location = image_location+"/";
image_location = image_location.replaceAll("//", "/");
arc_name = image_location+arc_name;
image_location = arc_name;
createdFileName = file_handler.inspectAndCreateImageStore(image_location);

} catch (Exception e){
	     createdFileName = null;
         e.printStackTrace();
         //logWriter.doLogging_QRmgr(checkAndRetrieve.logPathName, checkAndRetrieve.logFileName, "<file_handler - createNameAndFolder_imageStore> "+e);
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
		             //logWriter.doLogging_QRmgr(checkAndRetrieve.logPathName, checkAndRetrieve.logFileName, "<file_handler> "+e1);
		     }
		    }
		}
     }
     createdFile = file_handler.createDirectory(count, image_location);
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
		   System.out.println("====== <file_handler> createDirectory().couldn't create directory for image store.");
		   //logWriter.doLogging_QRmgr(checkAndRetrieve.logPathName, checkAndRetrieve.logFileName, "====== <file_handler> createDirectory().couldn't create directory for image store.");
		  }

   } catch(Exception e1){
		   newFile = null;
		   System.out.println("====== createDirectory() error: exception in creating image store directory: "+e1);
		   e1.printStackTrace();
		   //logWriter.doLogging_QRmgr(checkAndRetrieve.logPathName, checkAndRetrieve.logFileName, "====== <file_handler> createDirectory().couldn't create directory for image store."+e1);
   }
  }
return newFile;
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
         //logWriter.doLogging_QRmgr(checkAndRetrieve.logPathName, checkAndRetrieve.logFileName, "====== <file_handler> generate_imageStoreName"+e);
}
return name;
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




 public static String get_newFileNumber(String logRoot, String logName, int fileRenameCounter){

 String value = null;
 try {
      value = ""+System.currentTimeMillis()+"_"+fileRenameCounter;

 } catch (Exception e){
	      value = null;
          e.printStackTrace();
          log_writer.doLogging_QRmgr(logRoot, logName, "fileHandler.get_newFileNumber() exception: "+e);
 }
 return value;
 }




 //TODO: Sort this method out later....Proves problematic if sub-folders exist.
 //App has been manually set to ensure it doesn't create sub-folders but rather
 //stick them all into one folder.
 public static String getFile(String source,
                        String destination,
                        String newName,
                        String logRoot,
                        String logName,
                        int counter,
                        int fileCounter) {

 File dir = new File(source);
 try {
	 File[] files = dir.listFiles();
	 if(files != null)
	   {
		for(File f : files)
		   {
			if(f.isDirectory())
			  {
			   //newName = this.getFile(f.getAbsolutePath(),destination,newName);
			  }
			else
			  {
               newName = destination+"/"+file_handler.get_newFileNumber(logRoot, logName, fileCounter)+"_"+counter+".DCM";
               newName = newName.replaceAll("\\\\","/");
               newName = newName.replaceAll("//","/");
               boolean done = f.renameTo(new File(newName));
               if(!done)
                 {
				  //log_writer.doLogging_QRmgr(logRoot, logName, "dataRetrieverHandler .getFile(): Failed to move file "+f.getName()+", "+newName);

				  //System.out.println("dataRetrieverHandler .getFile(): Failed to move file "+f.getName()+", "+newName);

				  newName = "0";
				 }
			   else
			     {
				  break;
				 }
			  }
		   }
	   }
 } catch (Exception e) {
 	      log_writer.doLogging_QRmgr(logRoot, logName, "fileHandler.getFile(): error: "+e);
 		  e.printStackTrace();
		  newName = "0";
 }
 return newName;
 }



}//end of class.

