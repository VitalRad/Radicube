/**********************************************
 *
 *   Author:  Mike Bassey
 *   Year:    2012
 *   Purpose: Used by tag extractor
 *            Object to do its job.
 *
 **********************************************/

package utility.general;

import java.io.FileReader;
import java.io.BufferedReader;

import org.dcm4che2.net.log_writer;



public class tagRetriever {


private String fileToExtractFrom = null;
private String logRoot = null;
private String logName = null;
private String[] wantedtags = null;
private String[] retrievedTags = null;
private String[] not_wanted_values = null;


public tagRetriever(String fileToExtractFrom,
                    String logRoot,
                    String logName,
                    String[] wantedtags,
                    String[] not_wanted_values){

this.fileToExtractFrom = fileToExtractFrom;
this.logRoot = logRoot;
this.logName = logName;
this.wantedtags = wantedtags;
this.not_wanted_values = not_wanted_values;
}


public String[] doExtraction(int extractionType){

boolean keepGoing = true;
int tagFoundCount = 0;
try {
     this.retrievedTags = new String[this.wantedtags.length];
     BufferedReader in = new BufferedReader(new FileReader(this.fileToExtractFrom));

	 while(keepGoing)
	  {
       String str = in.readLine();
       if((str == null) || (tagFoundCount >= (this.wantedtags.length)))
         {
		  keepGoing = false;
		  break;
		 }

	   if(extractionType == 1)
		 {
          tagFoundCount = this.seekForTag(str, tagFoundCount);
	     }
	   else if(extractionType == 2)
	     {
	      tagFoundCount = this.findTag(str, tagFoundCount);
	     }
      }
    in.close();


//do check..
for(int a = 0; a < this.retrievedTags.length; a++)
   {
    if(this.retrievedTags[a] == null)
      {
	   this.retrievedTags = null;
	   break;
	  }

	if(extractionType == 1)
	  {
	   if(checkForUnwantedValues(this.retrievedTags[a]))
	     {
	      this.retrievedTags = null;
	      break;
	     }
      }
   }

} catch (Exception e) {
         this.retrievedTags = null;
         e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<tagRetriever>.doExtraction() error: "+e);
}
return this.retrievedTags;
}




public boolean checkForUnwantedValues(String value){

boolean found = false;
try {
     for(int a = 0; a < this.not_wanted_values.length; a++)
        {
	     if(value.equalsIgnoreCase(this.not_wanted_values[a]))
	       {
		    found = true;
		    break;
		   }
	    }

} catch (Exception e) {
         found = true;
         e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<tagRetriever>.checkForUnwantedValues() error: "+e);
}
return found;
}




public boolean UnwantedValuesFound(String value){

boolean found = false;
try {
     for(int a = 0; a < this.not_wanted_values.length; a++)
        {
	     if(value.indexOf(this.not_wanted_values[a]) >= 0)
	       {
		    //-----------------------------------
		    //log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<tagRetriever>.UnwantedValuesFound() value found: "+value);
		    //-----------------------------------
		    found = true;
		    break;
		   }
	    }

} catch (Exception e) {
         found = true;
         e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<tagRetriever>.UnwantedValuesFound() error: "+e);
}
return found;
}




public int seekForTag(String str, int tagFoundCount){

try {
     for(int a = 0; a < this.wantedtags.length; a++)
        {
	     this.wantedtags[a] = this.wantedtags[a].replaceAll(" ","");
	     if((str.indexOf(this.wantedtags[a])) >= 0)
	       {
            this.retrievedTags[a] = this.getTagValue(str);
            tagFoundCount++;


            //--------------------------------------------------------------
            if(this.wantedtags[a].equals("00080020"))
              {
               this.retrievedTags[a] = this.sortDate(this.retrievedTags[a]);
			  }
            //--------------------------------------------------------------

            break;
		   }
	    }
} catch (Exception e) {
         e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<tagRetriever>.seekForTag() error: "+e);
}
return tagFoundCount;
}





public int findTag(String str, int tagFoundCount){

try {
     for(int a = 0; a < this.wantedtags.length; a++)
        {

	     //------------------------------------------
		 //log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<tagRetriever>.findTag() data line: "+str);
		 //------------------------------------------

         this.wantedtags[a] = this.wantedtags[a].replaceAll(" ","");
	     if((str.indexOf(this.wantedtags[a])) >= 0)
	       {
	        if(!(UnwantedValuesFound(str)))
	          {
               this.retrievedTags[a] = str;

               //------------------------------------------
			   	//log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<tagRetriever>.findTag() found: "+str);
		       //------------------------------------------

               tagFoundCount++;
               break;
		      }
	       }
	    }
} catch (Exception e) {
         e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<tagRetriever>.findTag() error: "+e);
}
return tagFoundCount;
}




private String sortDate(String date){

try {
     String year  = date.substring(0,4);
     String month = date.substring(4,6);
     String day   = date.substring(6,8);

     String modified_month = this.convertNumToLetter(month);
     if(modified_month == null)
       {
	    modified_month = month;
	   }

     date = day+"-"+modified_month+"-"+year;

} catch (Exception e) {
         e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<tagRetriever>.sortDate() error: "+e);
}
return date;
}




private String convertNumToLetter(String value){

String rVal = null;
try {
     if(value.equalsIgnoreCase("01"))
       {
	    rVal = "Jan";
	   }
	 else if(value.equalsIgnoreCase("02"))
	   {
	    rVal = "Feb";
	   }
	 else if(value.equalsIgnoreCase("03"))
	   {
	 	rVal = "Mar";
	   }
     else if(value.equalsIgnoreCase("04"))
	   {
		rVal = "Apr";
	   }
     else if(value.equalsIgnoreCase("05"))
	   {
		rVal = "May";
	   }
     else if(value.equalsIgnoreCase("06"))
	   {
		rVal = "Jun";
	   }
     else if(value.equalsIgnoreCase("07"))
	   {
		rVal = "Jul";
	   }
	 else if(value.equalsIgnoreCase("08"))
	   {
	 	rVal = "Aug";
	   }
	 else if(value.equalsIgnoreCase("09"))
	   {
	 	rVal = "Sep";
	   }
	 else if(value.equalsIgnoreCase("10"))
	   {
	 	rVal = "Oct";
	   }
	 else if(value.equalsIgnoreCase("11"))
	   {
	 	rVal = "Nov";
	   }
	 else if(value.equalsIgnoreCase("12"))
	   {
	 	rVal = "Dec";
	   }

} catch (Exception e){
	     rVal = null;
	     log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<tagRetriever>.convertNumToLetter() error: "+e);
         e.printStackTrace();
}
return rVal;
}




public String getTagValue(String data){

String finalData = null;
try {
	  String[] step_1 = data.split("len="); //step 1...
	  String[] step_2 = step_1[step_1.length-1].split(">"); //step 2...
	  String[] step_3 = step_2[step_2.length-1].split("<"); //step 3...

	  String result = step_3[0];
	  result = result.replaceAll("\\^"," ");
	  String[] arrange = result.split(" ");
	  for(int a = 0; a < arrange.length; a++)
		 {
		  if((arrange[a].equalsIgnoreCase("mr"))||
			( arrange[a].equalsIgnoreCase("MRS"))||
			( arrange[a].equalsIgnoreCase("MISS"))||
			( arrange[a].equalsIgnoreCase("MS")))
			{
			 arrange[a] = "("+arrange[a]+")";
			}

		  if(finalData == null)
		    {
			 finalData = arrange[a];
		    }
		  else
		    {
			 finalData = finalData+" "+arrange[a];
		    }
		 }

} catch (Exception e) {
         finalData = null;
         e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<tagRetriever>.getTagValue() error: "+e);
}
return finalData;
}


}
