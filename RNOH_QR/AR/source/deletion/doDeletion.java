
/*****************************************************

 Author: Mike Bassey
 Date: Jul, 2012
 Purpose: Conducts deletion based on desired argument.

 *****************************************************/

 package deletion;

 import java.io.File;
 import java.util.Vector;

 import org.dcm4che2.net.log_writer;

 public class doDeletion {

 private String[] filesToDelete = null;
 private int deletionType = -1;
 private String locationToDelete = null;

 private boolean FILES_ONLY_DELETE = false;
 private String filesOnly_folderName = null;


 public doDeletion(){}


 public doDeletion(int deletionType, String[] filesToDelete, String locationToDelete){

 this.deletionType = deletionType;
 this.filesToDelete = filesToDelete;
 this.locationToDelete = locationToDelete;
 }


 public doDeletion(String fName, boolean value){

 this.filesOnly_folderName = fName;
 this.FILES_ONLY_DELETE = value;
 }


 public void run(String logRoot,String logName){

 try {
      if(this.FILES_ONLY_DELETE)
        {
	     this.deleteFilesOnly(filesOnly_folderName, logRoot, logName);
	    }
      else if(this.deletionType == 4)
        {
	     boolean deleted = this.deleteDir(new File(this.filesToDelete[0]));//delete
		 if(!deleted)
		   {
		 	System.out.println("file "+new File(this.filesToDelete[0]).getAbsolutePath()+" could not be deleted");
		 	log_writer.doLogging_QRmgr(logRoot, logName, "file "+new File(this.filesToDelete[0]).getAbsolutePath()+" could not be deleted");
		   }
	    }
	  else
	    {
         this.scanDirectory(new File(locationToDelete), this.deletionType, this.filesToDelete, logRoot, logName);
	    }

 } catch (Exception e){
          e.printStackTrace();
          log_writer.doLogging_QRmgr(logRoot, logName, "<doDeletion> error "+e);
 }
 }




public void scanDirectory(File dir, int delType, String[] range, String logRoot,String logName){

try {

String[] startDate = null;
String[] endDate   = null;

int startYear  = -1;
int startMonth = -1;
int startDay   = -1;
int endYear    = -1;
int endMonth   = -1;
int endDay     = -1;

if(delType == 1)
  {
   startDate = range[0].split("-");
   endDate   = range[1].split("-");
   startYear  = Integer.parseInt(startDate[0]);
   startMonth = Integer.parseInt(startDate[1]);
   startDay   = Integer.parseInt(startDate[2]);
   endYear    = Integer.parseInt(endDate[0]);
   endMonth   = Integer.parseInt(endDate[1]);
   endDay     = Integer.parseInt(endDate[2]);
  }
else if((delType == 2) || (delType == 3))
  {
   startDate = range[0].split("-");
   startYear  = Integer.parseInt(startDate[0]);
   startMonth = Integer.parseInt(startDate[1]);
   startDay   = Integer.parseInt(startDate[2]);
  }


File[] files = dir.listFiles();
if(files != null)
  {
   for(File f : files)
      {
	   if(f.isDirectory())
		 {
          try {

			  if(delType == 1) //range deletion
				{
				 String[] fileToDelete = f.getName().split("-");
				 int yearVal  = Integer.parseInt(fileToDelete[0]);
				 int monthVal = Integer.parseInt(fileToDelete[1]);
				 int dayVal   = Integer.parseInt(fileToDelete[2]);

				 if((yearVal >= startYear) &&
				   ( yearVal <= endYear) &&
				   ( dayVal >= 1) &&
				   ( dayVal <= 31) &&
				   ( monthVal <= 12))
				   {
                   if((yearVal == startYear)&&(yearVal == endYear))
					  {
					   if((monthVal >= startMonth) && (monthVal <= endMonth))
						 {
                          if((monthVal == startMonth) && (monthVal == endMonth))
							{
							 if((dayVal >= startDay) && (dayVal <= endDay))
							   {
								if(f.exists()){
								boolean deleted = this.deleteDir(f);//delete
								if(!deleted)
								  {
								   System.out.println("file "+f.getAbsolutePath()+" could not be deleted");
								   log_writer.doLogging_QRmgr(logRoot, logName, "file "+f.getAbsolutePath()+" could not be deleted");
								  }
							    }
							   }
							}
						  else if(monthVal == startMonth)
							{
							 if(dayVal >= startDay)
							   {
								if(f.exists()){
								boolean deleted = this.deleteDir(f);//delete
								if(!deleted)
								  {
								   System.out.println("file "+f.getAbsolutePath()+" could not be deleted");
								   log_writer.doLogging_QRmgr(logRoot, logName, "file "+f.getAbsolutePath()+" could not be deleted");
								  }
							    }
							   }
							}
                          else if(monthVal == endMonth)
							{
							 if(dayVal <= endDay)
							   {
								if(f.exists()){
								boolean deleted = this.deleteDir(f);//delete
								if(!deleted)
								  {
								   System.out.println("file "+f.getAbsolutePath()+" could not be deleted");
								   log_writer.doLogging_QRmgr(logRoot, logName, "file "+f.getAbsolutePath()+" could not be deleted");
								  }
							    }
							   }
							}
						  else
							{
							 if(f.exists()){
							 boolean deleted = this.deleteDir(f);//delete
							 if(!deleted)
							   {
								System.out.println("file "+f.getAbsolutePath()+" could not be deleted");
								log_writer.doLogging_QRmgr(logRoot, logName, "file "+f.getAbsolutePath()+" could not be deleted");
							   }
						     }
							}
						 }
					  }

                   else if(yearVal == startYear)
					  {
					   if(monthVal >= startMonth)
						 {
						  if(monthVal == startMonth)
							{
							 if(dayVal >= startDay)
							   {
								if(f.exists()){
								boolean deleted = this.deleteDir(f);//delete
								if(!deleted)
								  {
								   System.out.println("file "+f.getAbsolutePath()+" could not be deleted");
								   log_writer.doLogging_QRmgr(logRoot, logName, "file "+f.getAbsolutePath()+" could not be deleted");
								  }
							    }
							   }
							}
						  else
							{
							 if(f.exists()){
							 boolean deleted = this.deleteDir(f);//delete
							 if(!deleted)
							   {
								System.out.println("file "+f.getAbsolutePath()+" could not be deleted");
								log_writer.doLogging_QRmgr(logRoot, logName, "file "+f.getAbsolutePath()+" could not be deleted");
							   }
						     }
							}
						 }
					  }
                    else if(yearVal == endYear)
					  {
					   if(monthVal <= endMonth)
						 {
						  if(monthVal == endMonth)
							{
							 if(dayVal <= endDay)
							   {
								if(f.exists()){
								boolean deleted = this.deleteDir(f);//delete
								if(!deleted)
								  {
								   System.out.println("file "+f.getAbsolutePath()+" could not be deleted");
								   log_writer.doLogging_QRmgr(logRoot, logName, "file "+f.getAbsolutePath()+" could not be deleted");
								  }
							    }
							   }
							}
						  else
							{
							 if(f.exists()){
							 boolean deleted = this.deleteDir(f);//delete
							 if(!deleted)
							   {
								System.out.println("file "+f.getAbsolutePath()+" could not be deleted");
								log_writer.doLogging_QRmgr(logRoot, logName, "file "+f.getAbsolutePath()+" could not be deleted");
							   }
						     }
							}
						 }
					  }
					else
					  {
					   if(f.exists()){
					   boolean deleted = this.deleteDir(f);//delete
					   if(!deleted)
					     {
						  System.out.println("file "+f.getAbsolutePath()+" could not be deleted");
						  log_writer.doLogging_QRmgr(logRoot, logName, "file "+f.getAbsolutePath()+" could not be deleted");
					     }
				       }
					  }

				   }
				}
		     else if((delType == 2) || (delType == 3)) //'older than' or 'younger than'deletion
		        {
                 String[] fileToDelete = f.getName().split("-");
				 int yearVal  = Integer.parseInt(fileToDelete[0]);
				 int monthVal = Integer.parseInt(fileToDelete[1]);
				 int dayVal   = Integer.parseInt(fileToDelete[2]);
				 if((dayVal >= 1) && (dayVal <= 31))
				   {

                    if(delType == 2) //older than..
                      {
					   if(yearVal <= startYear)
					     {
                          if(yearVal == startYear)
                            {
						     if(monthVal == startMonth)
							   {
								if(dayVal < startDay)
								  {
								   if(f.exists()){
								   boolean deleted = this.deleteDir(f);//delete
								   if(!deleted)
									 {
									  System.out.println("file "+f.getAbsolutePath()+" could not be deleted");
									  log_writer.doLogging_QRmgr(logRoot, logName, "file "+f.getAbsolutePath()+" could not be deleted");
									 }
							       }
								  }
							   }
							  else if(monthVal < startMonth)
							   {
							    if(f.exists()){
							    boolean deleted = this.deleteDir(f);//delete
							    if(!deleted)
								  {
								   System.out.println("file "+f.getAbsolutePath()+" could not be deleted");
								   log_writer.doLogging_QRmgr(logRoot, logName, "file "+f.getAbsolutePath()+" could not be deleted");
								  }
							    }
							   }
						    }
						  else
						    {
						     if(f.exists()){
						     boolean deleted = this.deleteDir(f);//delete
							 if(!deleted)
							   {
							 	System.out.println("file "+f.getAbsolutePath()+" could not be deleted");
							 	log_writer.doLogging_QRmgr(logRoot, logName, "file "+f.getAbsolutePath()+" could not be deleted");
							   }
						     }
						    }
						 }
					  }
					else if(delType == 3) //younger than..
					  {
					   if(yearVal >= startYear)
					     {
						  if(yearVal == startYear)
						    {
						     if(monthVal == startMonth)
						       {
							    if(dayVal > startDay)
								  {
								   if(f.exists()){
								   boolean deleted = this.deleteDir(f);//delete
								   if(!deleted)
									 {
									  System.out.println("file "+f.getAbsolutePath()+" could not be deleted");
									  log_writer.doLogging_QRmgr(logRoot, logName, "file "+f.getAbsolutePath()+" could not be deleted");
									 }
							       }
								  }
							   }
                             else if(monthVal > startMonth)
                               {
							    if(f.exists()){
							    boolean deleted = this.deleteDir(f);//delete
							    if(!deleted)
								  {
								   System.out.println("file "+f.getAbsolutePath()+" could not be deleted");
								   log_writer.doLogging_QRmgr(logRoot, logName, "file "+f.getAbsolutePath()+" could not be deleted");
								  }
							    }
							   }
						    }
						  else
						    {
						     if(f.exists()){
						     boolean deleted = this.deleteDir(f);//delete
							 if(!deleted)
							   {
							 	System.out.println("file "+f.getAbsolutePath()+" could not be deleted");
							 	log_writer.doLogging_QRmgr(logRoot, logName, "file "+f.getAbsolutePath()+" could not be deleted");
							   }
						     }
						    }
						 }
					  }
				   }
			    }

	      } catch (Exception e1){
		           e1.printStackTrace();
		           log_writer.doLogging_QRmgr(logRoot, logName, "<doDeletion.scanDirectory()> error: "+e1);
		  }

         }
	  }
  }

} catch (Exception e){
         e.printStackTrace();
}
}





// Deletes all files and subdirectories under dir.
// Returns true if all deletions were successful.
// If a deletion fails, the method stops attempting to delete and returns false.
public boolean deleteDir(File dir){

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




//Delete files only.
public void deleteFilesOnly(String source,
                            String logRoot,
                            String logName){
File dir = new File(source);
try {
	 File[] files = dir.listFiles();
	 if(files != null)
	   {
		for(File f : files)
		   {
			if(f.isDirectory())
			  {
			   deleteFilesOnly(f.getAbsolutePath(), logRoot, logName);
			  }
			else
			  {
               boolean done = f.delete();
               if(!done)
                 {
                  log_writer.doLogging_QRmgr(logRoot,logName, "<deleteFilesOnly()> msg: Failed to delete file "+f.getName());
                 }
			  }
		   }
	   }
} catch (Exception e) {
		 e.printStackTrace();
		 log_writer.doLogging_QRmgr(logRoot,logName, "<deleteFilesOnly()> error: "+e);
}
}




public void deleteThisDirOnly(String source,
                              String targetName,
                              String logRoot,
                              String logName){
File dir = new File(source);
try {
	 File[] files = dir.listFiles();
	 if(files != null)
	   {
		for(File f : files)
		   {
			if((f.isDirectory()) &&
			  ( f.getName().equals(targetName)))
			  {
			   boolean done = deleteDir(f);
			   if(!done)
			     {
                  log_writer.doLogging_QRmgr(logRoot, logName, "<doDeletion.deleteThisDirOnly()> msg: Failed to delete file "+f.getAbsolutePath());
                 }
               break;
			  }
			else
			  {
			   deleteThisDirOnly(f.getAbsolutePath(),
			                     targetName,
			                     logRoot,
			                     logName);
			  }
		   }
	   }
} catch (Exception e) {
		 e.printStackTrace();
		 log_writer.doLogging_QRmgr(logRoot, logName, "<doDeletion.deleteThisDirOnly()> error: "+e);
}
}




@SuppressWarnings("unchecked")
public Vector find_oldestOrYoungest(Vector folderNames,
	                                String source,
                                    int comparisonType,
                                    String logRoot,
                                    String logName){
try {
     File dir = new File(source);
     File[] files = dir.listFiles();
	 if(files != null)
	   {
		for(File f : files)
		   {
			if(f.isDirectory())
			  {
			   folderNames.add(f.getAbsolutePath());
			   folderNames = find_oldestOrYoungest(folderNames,
				                                   f.getAbsolutePath(),
			                                       comparisonType,
			                                       logRoot,
			                                       logName);
			  }
		   }
	   }

} catch (Exception e) {
		 e.printStackTrace();
		 log_writer.doLogging_QRmgr(logRoot, logName, "<doDeletion.find_oldestOrYoungest()> error: "+e);
}
return folderNames;
}



//Note: files to compare must come in format "yyyy-mm-dd" or "yyyy/mm/dd".
public String compareAndReturn(Vector filesToCompare,
                                int comparisonType,
	                            String logRoot,
                                String logName){
String oldestFound = null;
try {
     int[] Numbers  = new int[filesToCompare.size()];

     for(int a = 0; a < filesToCompare.size(); a++)
        {
		 String fName = (String) filesToCompare.get(a);
		 if(fName != null)
		   {
            File file = new File(fName);
            String lastName = file.getName();
            if(lastName == null)
              {
			   Numbers[a] = 0;
			   log_writer.doLogging_QRmgr(logRoot, logName, "<doDeletion.compareAndReturn()> msg: No file found. Assigned 0");
			  }
			else
			  {
			   lastName = lastName.replaceAll("\\\\","");
			   lastName = lastName.replaceAll("/","");
			   lastName = lastName.replaceAll("-","");
			   Numbers[a] = Integer.parseInt(lastName);
			  }
		   }
		 else
		   {
		    Numbers[a] = 0;
		    log_writer.doLogging_QRmgr(logRoot, logName, "<doDeletion.compareAndReturn()> msg <2>: No file found. Assigned 0");
		   }
		}

     if(Numbers.length > 0)
       {
        Numbers = doDeletion.bubbleSort(Numbers,
		                                comparisonType,
		                                logRoot,
                                        logName);
        if(Numbers[0] != 0)
          {
		   String fileFound = Integer.toString(Numbers[0]);
		   String yr = fileFound.substring(0,4);
		   String mnth = fileFound.substring(4,6);
		   String dy = fileFound.substring(6,8);

		   oldestFound = yr+"-"+mnth+"-"+dy;
		  }
	   }
} catch (Exception e) {
		 oldestFound = null;
		 e.printStackTrace();
		 log_writer.doLogging_QRmgr(logRoot, logName, "<doDeletion.compareAndReturn()> error: "+e);
}
return oldestFound;
}




private static int[] bubbleSort(int[] intArray,
                                int opType,
                                String logRoot,
                                String logName){
try {
	int n = intArray.length;
	int temp = 0;
	for(int i=0; i < n; i++){
		for(int j=1; j < (n-i); j++)
		   {
			if(opType == 1) //oldest first..
			  {
			   if(intArray[j-1] > intArray[j])
			     {
			      //swap the elements!
			      temp = intArray[j-1];
			      intArray[j-1] = intArray[j];
			      intArray[j] = temp;
			     }
		      }
            else if(opType == 2) //youngest first..
			  {
			   if(intArray[j-1] < intArray[j])
			     {
			      //swap the elements!
			      temp = intArray[j-1];
			      intArray[j-1] = intArray[j];
			      intArray[j] = temp;
			     }
		      }
		   }
	}
} catch (Exception e) {
		 e.printStackTrace();
		 log_writer.doLogging_QRmgr(logRoot, logName, "<doDeletion.bubbleSort()> error: "+e);
}
return intArray;
}



}//end of class.