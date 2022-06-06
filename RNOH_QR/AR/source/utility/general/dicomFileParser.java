/******************************************

Author : Mike Bassey, 2012.
Purpose: Parses a dicom object, retrieving
         desired info.

*******************************************/

package utility.general;

import java.io.File;
import java.io.IOException;


import org.dcm4che2.io.DicomInputStream;
import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.UID;
import org.dcm4che2.data.UIDDictionary;
import org.dcm4che2.io.StopTagInputHandler;
import org.dcm4che2.util.CloseUtils;
import org.dcm4che2.net.log_writer;

import org.dcm4che2.tool.dcmrcv.DcmRcv;



public class dicomFileParser {

private static final int PEEK_LEN = 1024;


public Object[] get_dicomStreams(File f,
                                 String logRoot,
                                 String logName){

Object[] d_objects = null;

DicomObject dcmObj = new BasicDicomObject();
DicomInputStream in = null;
try {
	 in = new DicomInputStream(f);
	 in.setHandler(new StopTagInputHandler(Tag.StudyDate));
	 in.readDicomObject(dcmObj, PEEK_LEN);
	 //info.tsuid = in.getTransferSyntax().uid();
	 //info.fmiEndPos = in.getEndOfFileMetaInfoPosition();

	 d_objects = new Object[1];
	 d_objects[0] = dcmObj;

} catch (Exception e) {
		 d_objects = null;
		 e.printStackTrace();
		 System.err.println("dicom_File_Parser.get_dicomStreams() msg: Failed to parse " + f);
	     log_writer.doLogging_QRmgr(logRoot, logName, "dicom_File_Parser.get_dicomStreams() msg: Failed to parse " +f);
		 return d_objects;
} finally {
	       CloseUtils.safeClose(in);
}
return d_objects;
}




public String get_cuid(File f,
                       String logRoot,
                       String logName){
String cuid = null;
if(f != null)
  {
   Object[] streams = get_dicomStreams(f,
                                       logRoot,
                                       logName);
   if(streams != null)
     {
      DicomObject dcmObj = (DicomObject) streams[0];
      cuid = dcmObj.getString(Tag.SOPClassUID);
	 }
  }
return cuid;
}



public String get_iuid(File f,
                       String logRoot,
                       String logName){
String iuid = null;
if(f != null)
  {
   Object[] streams = get_dicomStreams(f,
                                       logRoot,
                                       logName);
   if(streams != null)
     {
      DicomObject dcmObj = (DicomObject) streams[0];
      iuid = dcmObj.getString(Tag.SOPInstanceUID);
	 }
  }
return iuid;
}





    /*
	public static void sleepForDesiredTime(int duration){

	try {
		 Thread.sleep(duration);
	} catch(InterruptedException ef) {
		    ef.printStackTrace();
	}
	}
	*/



/*
public static void main(String[] args){

//new dicom_File_Parser().QuickTest("C:\\mike_folder\\codes\\ar_try\\data_containers\\images\\night_hawk\\for_transfer\\2015-02-20\\1424434191075_0\\1424434193692_5.DCM");


String cuid = new dicom_File_Parser().get_cuid(new File("C:\\mike_work\\0130422120120927_16\\test1.dcm"), "C:/mike_work/logs/", "dicom_File_Parser.txt");
String iuid = new dicom_File_Parser().get_iuid(new File("C:\\mike_work\\0130422120120927_16\\test1.dcm"), "C:/mike_work/logs/", "dicom_File_Parser.txt");


log_writer.doLogging_QRmgr("C:/mike_work/logs/", "dicom_File_Parser.txt", "dicom_File_Parser msg: cuid = " +cuid+", iuid = "+iuid);

}
*/


}