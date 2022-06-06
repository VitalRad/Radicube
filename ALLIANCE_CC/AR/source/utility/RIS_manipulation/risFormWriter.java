
/************************************

 Author:  Mike Bassey, 2011.
 Purpose: Converts media files from
          one format to another.

************************************/


package utility.RIS_manipulation;


import java.awt.image.renderable.*;
import java.io.*;
import javax.media.jai.*;
import javax.media.jai.widget.*;
import com.sun.media.jai.codec.*;
import java.awt.Container;
import com.sun.media.jai.codec.SeekableStream;


import org.dcm4che2.net.log_writer;


public class risFormWriter {


private ImageEncoder encoder = null;
private JPEGEncodeParam encodeParam = null;
private int res = 0;
private com.sun.media.jai.codec.SeekableStream sStream = null;
private DataInputStream ds = null;

// Create some Quantization tables.
 private static int[] qtable1 = {
     1,1,1,1,1,1,1,1,
     1,1,1,1,1,1,1,1,
     1,1,1,1,1,1,1,1,
     1,1,1,1,1,1,1,1,
     1,1,1,1,1,1,1,1,
     1,1,1,1,1,1,1,1,
     1,1,1,1,1,1,1,1,
     1,1,1,1,1,1,1,1
 };
 private static int[] qtable2 = {
     2,2,2,2,2,2,2,2,
     2,2,2,2,2,2,2,2,
     2,2,2,2,2,2,2,2,
     2,2,2,2,2,2,2,2,
     2,2,2,2,2,2,2,2,
     2,2,2,2,2,2,2,2,
     2,2,2,2,2,2,2,2,
     2,2,2,2,2,2,2,2
 };
 private static int[] qtable3 = {
     3,3,3,3,3,3,3,3,
     3,3,3,3,3,3,3,3,
     3,3,3,3,3,3,3,3,
     3,3,3,3,3,3,3,3,
     3,3,3,3,3,3,3,3,
     3,3,3,3,3,3,3,3,
     3,3,3,3,3,3,3,3,
     3,3,3,3,3,3,3,3
 };
 // Really rotten quality Q Table
 private static int[] qtable4 = {
     200,200,200,200,200,200,200,200,
     200,200,200,200,200,200,200,200,
     200,200,200,200,200,200,200,200,
     200,200,200,200,200,200,200,200,
     200,200,200,200,200,200,200,200,
     200,200,200,200,200,200,200,200,
     200,200,200,200,200,200,200,200,
     200,200,200,200,200,200,200,200
 };


private String logRoot = null;
private String logName = null;
private float imageQuality = -10F;


public risFormWriter(String inFile,
                     String outFile,
                     float imageQuality,
                     String logRoot,
                     String logName) {
this.logRoot = logRoot;
this.logName = logName;
this.imageQuality = imageQuality;

FileOutputStream out1 = createOutputStream(outFile);

// Create the source op image.
PlanarImage src = loadImage(inFile);
double[] constants = new double[3];
constants[0] = 0.0;
constants[1] = 0.0;
constants[2] = 0.0;

ParameterBlock pb = new ParameterBlock();
pb.addSource(src);
pb.add(constants);



// Set the encoding parameters if necessary.
encodeParam = new JPEGEncodeParam();
encodeParam.setQuality(this.imageQuality);
encodeParam.setHorizontalSubsampling(0, 1);
encodeParam.setHorizontalSubsampling(1, 2);
encodeParam.setHorizontalSubsampling(2, 2);
encodeParam.setVerticalSubsampling(0, 1);
encodeParam.setVerticalSubsampling(1, 1);
encodeParam.setVerticalSubsampling(2, 1);
encodeParam.setRestartInterval(64);
//encodeParam.setWriteImageOnly(false);
//encodeParam.setWriteTablesOnly(true);
//encodeParam.setWriteJFIFHeader(true);
// Create the encoder.
encodeImage(src, out1);

this.res = 1;
}



public int getResults(){

return this.res;
}



// Load the source image.
private PlanarImage loadImage(String imageName) {

try {

this.ds =  new DataInputStream(new BufferedInputStream(new FileInputStream(imageName)));
this.sStream = SeekableStream.wrapInputStream(this.ds, true);

} catch (Exception ee){
         ee.printStackTrace();
}

PlanarImage src = JAI.create("Stream", this.sStream);

if(src == null)
  {
   log_writer.doLogging_QRmgr(this.logRoot, this.logName, "risFormWriter: Error in loading image " + imageName);
  }
return src;
}



public void closeStreams(){

try {
     if(this.sStream != null)
       {
        this.sStream.close();
	    this.sStream = null;
       }
     if(this.ds != null)
       {
	    this.ds.close();
	    this.ds = null;
	   }

} catch (Exception e) {
		 e.printStackTrace();
		 log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<risFormWriter.closeStreams()> error :" +e);
}
}


// Create the image encoder.
private void encodeImage(PlanarImage img, FileOutputStream out) {

encoder = ImageCodec.createImageEncoder("JPEG", out, encodeParam);
try {
	 encoder.encode(img);
	 out.close();
	 out = null;


} catch (Exception e) {
		 log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<risFormWriter.encodeImage()> error :" +e);

         try{
             out.close();
             out = null;
	     } catch(Exception ef){}
}
}


private FileOutputStream createOutputStream(String outFile) {

FileOutputStream out = null;
try {
	 out = new FileOutputStream(outFile);
} catch(Exception e) {
		log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<risFormWriter.createOutputStream()> error :" +e);
        try {
		     out.close();
		     out = null;
	    } catch(Exception ef){}
}
return out;
}

}