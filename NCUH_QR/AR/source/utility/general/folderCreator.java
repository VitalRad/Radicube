/***********************************************************
*
*     Author: Mike bass.
*     Year: 2012.
*     App name: folderCreator .
*
*     Description: Handles all file related manipulations.
*
************************************************************/

package utility.general;


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





public class folderCreator{


public folderCreator(){}




public static String createNameAndFolder_imageStore(String image_location){

String createdFileName = null;
try {

String arc_name = folderCreator.generate_imageStoreName();
image_location = image_location.replaceAll("\\\\", "/");
image_location = image_location+"/";
image_location = image_location.replaceAll("//", "/");
arc_name = image_location+arc_name;
image_location = arc_name;
createdFileName = folderCreator.inspectAndCreateImageStore(image_location);

} catch (Exception e){
	     createdFileName = null;
         e.printStackTrace();
         //log_writer.doLogging_QRmgr(3, "<folderCreator - createNameAndFolder_imageStore> "+e);
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
		             //log_writer.doLogging_QRmgr(3, "<folderCreator> "+e1);
		     }
		    }
		}
     }
     createdFile = folderCreator.createDirectory(count, image_location);
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
		   System.out.println("====== <folderCreator> createDirectory().couldn't create directory for image store.");
		   //log_writer.doLogging_QRmgr(3, "====== <folderCreator> createDirectory().couldn't create directory for image store.");
		  }

   } catch(Exception e1){
		   newFile = null;
		   System.out.println("====== createDirectory() error: exception in creating image store directory: "+e1);
		   e1.printStackTrace();
		   //log_writer.doLogging_QRmgr(3, "====== <folderCreator> createDirectory().couldn't create directory for image store."+e1);
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
         //log_writer.doLogging_QRmgr(3, "====== <folderCreator> generate_imageStoreName"+e);
}
return name;
}

}//end of class.

