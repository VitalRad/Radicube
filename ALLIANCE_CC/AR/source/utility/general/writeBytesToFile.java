
/**********************************************

Author:  Mike Bassey
Year:    2012.
Purpose: write bytes to file.

***********************************************/

package utility.general;


import java.io.BufferedOutputStream;
import java.io.FileOutputStream;


import org.dcm4che2.net.log_writer;


public class writeBytesToFile {

BufferedOutputStream bytesWriter = null;
String fName = null;
private String CRLF = "\r\n";
private String logRoot = null;
private String logName = null;



public writeBytesToFile(String logRoot, String logName){

this.logRoot = logRoot;
this.logName = logName;
}


public boolean start_writing(String fName, String[] dataToWrite){

boolean done = false;
try {
	this.fName = fName;
	this.bytesWriter = new BufferedOutputStream(new FileOutputStream(this.fName));

	for(int a = 0; a < dataToWrite.length; a++)
	   {
		this.bytesWriter.write(dataToWrite[a].getBytes());
		this.bytesWriter.flush();
		this.bytesWriter.write(this.CRLF.getBytes());
		this.bytesWriter.flush();
	   }

    if(this.bytesWriter != null)
      {
	   this.bytesWriter.close();
	   this.bytesWriter = null;
      }

    done = true;

} catch (Exception e){
	     log_writer.doLogging_QRmgr(this.logRoot, this.logName, "writeBytesToFile exception: "+e);
	     e.printStackTrace();
	     done = false;
	     try{
	         if(this.bytesWriter != null)
	           {
	            this.bytesWriter.close();
	            this.bytesWriter = null;
		       }
	     }catch(Exception er){
	        log_writer.doLogging_QRmgr(this.logRoot, this.logName, "writeBytesToFile <closing>exception: "+er);
	     }
}
return done;
}

}
