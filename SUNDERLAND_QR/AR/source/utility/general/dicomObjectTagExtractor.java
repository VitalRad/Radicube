/**********************************************************
 *
 *   Author:  Mike Bassey
 *   Year:    2012
 *   Purpose: Extracts desired tag(s) from a dicom object.
 *
 **********************************************************/

 package utility.general;


 import java.io.File;

 import org.dcm4che2.net.log_writer;
 import org.dcm4che2.tool.dcm2xml.Dcm2Xml;
 import org.dcm4che2.tool.dcmrcv.dcm_folderCreator;



 public class dicomObjectTagExtractor {


 private String dcmFileLocation = null;
 private String xmlFileLocation = null;
 private String logRoot = null;
 private String logName = null;
 private int attempts = 0;
 private int maxAttempts = -1;
 private String[] desiredTags = null;
 private int breakInbetweenSearch = -1;
 private String[] retrievedTags = null;
 private String[] notWantedValues = null;
 private int extractionType = -1;



 public dicomObjectTagExtractor(String dcmFileLocation,
                                String xmlFileLocation,
                                String logRoot,
                                String logName,
                                int maxAttempts,
                                String[] desiredTags,
                                int breakInbetweenSearch,
                                String[] notWantedValues,
                                int extractionType){

 this.dcmFileLocation = dcmFileLocation;
 this.xmlFileLocation = xmlFileLocation;
 this.logRoot = logRoot;
 this.logName = logName;
 this.maxAttempts = maxAttempts;
 this.desiredTags = desiredTags;
 this.breakInbetweenSearch = breakInbetweenSearch;
 this.notWantedValues = notWantedValues;
 this.extractionType = extractionType;
 }



 public dicomObjectTagExtractor(String dcmFileLocation,
                                String xmlFileLocation,
                                String logRoot,
                                String logName,
                                String[] desiredTags,
                                String[] notWantedValues,
                                int extractionType){

 this.dcmFileLocation = dcmFileLocation;
 this.xmlFileLocation = xmlFileLocation;
 this.logRoot = logRoot;
 this.logName = logName;
 this.desiredTags = desiredTags;
 this.notWantedValues = notWantedValues;
 this.extractionType = extractionType;


/*
 //------------------------------------------------------------
 for(int a = 0; a < desiredTags.length; a++)
    {
     log_writer.doLogging_QRmgr(this.logRoot, this.logName,
	 "<dicomObjectTagExtractor>.constructor() msg: desiredTags["+a+"]: "+desiredTags[a]);
    }
 //------------------------------------------------------------


 //------------------------------------------------------------
 for(int a = 0; a < notWantedValues.length; a++)
    {
     log_writer.doLogging_QRmgr(this.logRoot, this.logName,
	 "<dicomObjectTagExtractor>.constructor() msg: notWantedValues["+a+"]: "+notWantedValues[a]);
    }
 //------------------------------------------------------------
 */

 }




public void commence_tagRetrieval_1(){

while(this.attempts < this.maxAttempts)
 {
  if(this.retrievedTags != null)
    {break;}
  this.doTagRetrieval();
  this.attempts++;
  if(this.retrievedTags == null)
    {genericJobs.sleepForDesiredTime(breakInbetweenSearch);}
  else
    {break;}
 }
}



public void commence_tagRetrieval_2(){

this.doTagRetrieval();
}



private void doTagRetrieval(){
try {
     String location = dcm_folderCreator.createNameAndFolder_imageStore(this.xmlFileLocation);
	 if(location == null)
	   {
		log_writer.doLogging_QRmgr(this.logRoot, this.logName,
		"<dicomObjectTagExtractor>.run() msg: could not create xml folder location");
	   }
	 else
	   {
		this.retrieveTags(this.dcmFileLocation, location);
	   }

   } catch (Exception e){
			e.printStackTrace();
			log_writer.doLogging_QRmgr(this.logRoot, this.logName,
			"dicomObjectTagExtractor.doTagRetrieval() error: "+e);
   }
}



public void retrieveTags(String source, String destination) {

File dir = new File(source);
try {
	 File[] files = dir.listFiles();
	 if(files != null)
	   {
		for(File f : files)
		   {
			if(f.isDirectory())
			  {
			   this.retrieveTags(f.getAbsolutePath(),destination);
			  }
			else
			  {
               String xmlFile = destination+"/dicom2xml.xml";
               xmlFile = genericJobs.prepareFilePath(xmlFile, logRoot, logName);
               String[] tags = doExtraction(f.getAbsolutePath(), xmlFile);
               if(tags != null)
                 {
			      this.retrievedTags = tags;
			      break;
			     }
			  }
		   }
	   }
} catch (Exception e) {
	     e.printStackTrace();
	     log_writer.doLogging_QRmgr(this.logRoot, this.logName,
	     "dicomObjectTagExtractor.retrieveTags() error: "+e);
}
}




public String[] doExtraction(String dcmFile, String xmlFile){

String[] tags = null;

try {
     String[] argsForDcm2xml = null;
	 argsForDcm2xml = new String[3];
     argsForDcm2xml[0] = dcmFile;
     argsForDcm2xml[1] = "-o";
	 argsForDcm2xml[2] = xmlFile;
	 Dcm2Xml dcm2xml = new Dcm2Xml();
	 dcm2xml.startOp(argsForDcm2xml,dcm2xml);

	 if(argsForDcm2xml != null)
	   {
		tags = new tagRetriever(argsForDcm2xml[2],
							    this.logRoot,
							    this.logName,
							    this.desiredTags,
							    this.notWantedValues).doExtraction(this.extractionType);
	   }

 } catch (Exception e){
          tags = null;
          e.printStackTrace();
          log_writer.doLogging_QRmgr(this.logRoot, this.logName,
          "dicomObjectTagExtractor.doExtraction() error: "+e);
 }
 return tags;
 }



 public String[] getRetrievedTags(){

 return this.retrievedTags;
 }

}