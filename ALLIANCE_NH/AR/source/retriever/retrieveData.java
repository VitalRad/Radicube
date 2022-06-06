
/****************************************************************************
 *
 *   Author:  Mike Bassey
 *   Year:    2012
 *   Purpose: Contacts https server to request and retrieve appropriate data,
 *   and then feed same to desired destination.
 *
 ****************************************************************************/

 package retriever;


 import java.io.File;
 import java.io.BufferedOutputStream;
 import java.net.Socket;
 import java.io.InputStream;
 import java.io.FileInputStream;
 import java.io.FileOutputStream;
 import java.util.Properties;
 import java.io.BufferedReader;
 import java.io.InputStreamReader;



 import org.dcm4che2.net.log_writer;



 public class retrieveData {


 private static String CLRF = "\r\n";
 private int SERIES_NO = 0;
 private String TARGET_HTTPS_SERVER = null;
 private int TARGET_HTTPS_PORT = -1;
 private String machineID = null;
 private int imgType = -1;
 private int chunkBufferSize = -1;
 private Socket socket = null;
 private BufferedOutputStream out = null;
 private String logRoot = null;
 private String logName = null;
 private String fileStore = null;
 private static int fileRenameCounter = 0;
 private InputStream input = null;

 private String movedLocation = null;
 private String alivenessFileSopClass = null;
 private String[] common_ts = null;
 private int alivenessFrequency = -1;
 private String keepAliveFile = null;
 private String sourceServerIp = null;
 private String callingAET = null;
 private String calledAET = null;
 private int dcmsnd_fileLocations_queueSize = -1;
 private int connRetryPauseTime = -1;
 private String[] rStrings = null;
 private String[] rStrings_2 = null;
 private pushToDestination pshToDest = null;
 private String sourceTargetServerIp = null;
 private int sourceTargetServerPort = -1;


public retrieveData(){}



 public void commenceOp(String propertiesFile, String sc_ts_properties){

 try {
	  Properties prop = new Properties();
	  FileInputStream fis = new FileInputStream(propertiesFile);
	  prop.load(fis);

	  this.logRoot = prop.getProperty("logRoot");
	  this.logName = prop.getProperty("logName");
      this.TARGET_HTTPS_SERVER = prop.getProperty("TARGET_HTTPS_SERVER");
      this.TARGET_HTTPS_PORT = Integer.parseInt(prop.getProperty("TARGET_HTTPS_PORT"));
      this.machineID = prop.getProperty("machineID");
      this.imgType = Integer.parseInt(prop.getProperty("imgType"));
      this.chunkBufferSize = Integer.parseInt(prop.getProperty("chunkBufferSize"));
      this.fileStore = prop.getProperty("fileStore");
      this.movedLocation = prop.getProperty("movedLocation");
      this.alivenessFrequency = Integer.parseInt(prop.getProperty("alivenessFrequency"));
      this.keepAliveFile = prop.getProperty("keepAliveFile");
      this.sourceServerIp = prop.getProperty("sourceServerIp");
      this.callingAET = prop.getProperty("callingAET");
      this.calledAET = prop.getProperty("calledAET");
      this.dcmsnd_fileLocations_queueSize = Integer.parseInt(prop.getProperty("dcmsnd_fileLocations_queueSize"));
      this.connRetryPauseTime = Integer.parseInt(prop.getProperty("connRetryPauseTime"));
      this.sourceTargetServerIp = prop.getProperty("sourceTargetServerIp");
      this.sourceTargetServerPort = Integer.parseInt(prop.getProperty("sourceTargetServerPort"));

	  if( (this.logRoot == null)||
		  (this.logName == null)||
		  (this.TARGET_HTTPS_SERVER == null)||
		  (this.TARGET_HTTPS_PORT < 0)||
		  (this.machineID == null)||
		  (this.imgType < 0)||
		  (this.chunkBufferSize <= 0)||
		  (this.fileStore == null)||
          (this.movedLocation == null)||
          (this.alivenessFrequency < 0)||
          (this.keepAliveFile == null)||
          (this.sourceServerIp == null)||
          (this.callingAET == null)||
          (this.calledAET == null)||
          (this.dcmsnd_fileLocations_queueSize <= 0)||
          (this.connRetryPauseTime <= 0)||
          (this.sourceTargetServerIp == null)||
          (this.sourceTargetServerPort < 0))
		  {
		   System.out.println("<retrieveData> commenceOp() error: Invalid values got from properties file 1!");
		   System.exit(0);
		  }

		Properties prop_2 = new Properties();
		FileInputStream fis_2 = new FileInputStream(sc_ts_properties);
		prop_2.load(fis_2);

		int common_No_of_ts = Integer.parseInt(prop_2.getProperty("common_No_of_ts"));
		if(common_No_of_ts < 0)
		  {
		   System.err.println("<retrieveData.startOp() properties file 2> error: Invalid common_No_of_ts value: "+common_No_of_ts);
		   System.exit(0);
		  }

		this.alivenessFileSopClass = prop_2.getProperty("alivenessFileSopClass");
        this.common_ts = new String[common_No_of_ts];
		for(int x = 0; x < common_ts.length; x++)
		   {
            this.common_ts[x] = prop_2.getProperty("common_ts_"+x);
		   }



		 this.rStrings = new String[6];
         rStrings[0] = "-ts1";
		 rStrings[1] = "true";
		 rStrings[2] = "-L "+this.callingAET+"@"+sourceServerIp;
	     rStrings[3] = this.calledAET+"@"+this.sourceTargetServerIp+":"+this.sourceTargetServerPort;
		 rStrings[4] = this.keepAliveFile; //for scanning..
		 rStrings[5] = "teleradhack";

		 this.rStrings_2 = new String[2];
		 rStrings_2[0] = this.keepAliveFile;
		 rStrings_2[1] = Integer.toString(this.alivenessFrequency);

         this.startOp();


 } catch (Exception e) {
          e.printStackTrace();
          System.exit(0);
 }
 }



 public void start_pushObject(String retrievalLocation){

 if(this.pshToDest == null)
   {
    this.pshToDest = new pushToDestination(this.logRoot,
                                           this.logName,
                                           retrievalLocation,
                                           this.movedLocation,
                                           this.alivenessFileSopClass,
						                   this.common_ts,
						                   this.rStrings,
                                           this.rStrings_2,
                                           this.dcmsnd_fileLocations_queueSize,
                                           this.connRetryPauseTime);
    new Thread(this.pshToDest).start();
   }
 }



 public void startOp(){

 try {
	  this.makeConnection();
	  boolean done = this.makeIntro();
	  if(!done)
	    {
	     log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<retrieveData.startOp() msg: Failed to make successful intro to server ");
	    }
      else
        {
		 this.decodeAndSave();
		}

 } catch (Exception e) {
            e.printStackTrace();
            log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<retrieveData.startOp() error: "+e);
            this.closeSocket();
 }
 }


 public void makeConnection(){

 try {
      this.socket = new Socket(this.TARGET_HTTPS_SERVER,
                               this.TARGET_HTTPS_PORT);
	  this.out = (new BufferedOutputStream(this.socket.getOutputStream()));
	  this.input = this.socket.getInputStream();


 } catch (Exception e) {
           e.printStackTrace();
           log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<retrieveData.makeConnection() error: "+e);
           this.closeSocket();
 }
 }


 public boolean makeIntro(){

 boolean successful = false;
 if(this.getSocket() != null){
 if(this.getSocket().isConnected()){
 if(!(this.getSocket().isClosed())){
 try {
		String msg_1 = "GET / HTTP/1.1"+retrieveData.CLRF;
		this.out.write(msg_1.getBytes());
		//this.out.flush();

		msg_1 = "Host: " + this.TARGET_HTTPS_SERVER + ":" +this.TARGET_HTTPS_PORT +retrieveData.CLRF;
		this.out.write(msg_1.getBytes());
		//this.out.flush();

		msg_1 = "Agent: SSL.Ready to start."+retrieveData.CLRF;
		this.out.write(msg_1.getBytes());
		//this.out.flush();

		msg_1 = "machine ID="+this.machineID+retrieveData.CLRF;
		this.out.write(msg_1.getBytes());

		msg_1 = "status=ar_retriever"+retrieveData.CLRF;
		this.out.write(msg_1.getBytes());

		String imTyp = "not_specified";
		if(this.imgType == 1)
		  {
		   imTyp = "QR";
		  }
		else if(this.imgType == 2)
		  {
		   imTyp = "night_hawk";
		  }

		msg_1 = "imageType="+imTyp+retrieveData.CLRF;
		this.out.write(msg_1.getBytes());
		//this.out.flush();

		this.out.write(retrieveData.CLRF.getBytes());
		this.out.flush();

		boolean serverReady = false;
		BufferedReader in = new BufferedReader(new InputStreamReader(this.input));
		String line = null;
		while((line = in.readLine()) != null)
		 {
		  System.out.println(line);

		  if((line.equals(""))||
		     (line.equals(retrieveData.CLRF)))
		     {break;}

		  if((line.indexOf("Server ready. Start streaming")) >= 0);
		    {
		     successful = true;
		    }
		 }

 } catch (Exception e) {
          e.printStackTrace();
          log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<retrieveData id: makeIntro() error: "+e);
          this.closeSocket();
 }
 }
 }
 }
 return successful;
 }






 private void decodeAndSave(){

 boolean keepGoing = true;
 FileOutputStream fOut = null;
 try {
	 String filePath = file_handler.createNameAndFolder_imageStore(this.fileStore);
	 if(filePath == null)
	   {
	 	log_writer.doLogging_QRmgr(logRoot, logName, "<retrieveData.decodeAndSave> error: Failed to create file store");
	 	keepGoing = false;
	   }

	 int currentChunkSeries = 0;

	 while(keepGoing)
	  {
	   String fileNameNo = get_newFileNumber();
	   if(fileNameNo == null)
		 {
		  fileNameNo = filePath+"/"+"_mod_"+retrieveData.fileRenameCounter+".DCM";
		  retrieveData.fileRenameCounter++;
		 }
	   else
		 {
		  fileNameNo = filePath+"/"+fileNameNo+".DCM";
		 }

	   fOut = null;
	   int currentChunkNo = 1;

	   while(true)
		{
		 byte[] buffer = new byte[4];

		 //read series No..
		 int length = this.input.read(buffer);
		 if(length <= 0) //EOF.
		   {
			keepGoing = false;
			retrieveData.closeStreams(fOut,logRoot,logName);

			break;
		   }

		 while(length < 4)
		   {
			length += this.input.read(buffer, length, (4 - length));
		   }

		 int seriesNo = generic_class.read_intVALUE(buffer);
		 if(seriesNo != currentChunkSeries)
		   {
			System.out.println("<decodeAndSave> error: Series No not what was expected: "+seriesNo+", "+currentChunkSeries);
			currentChunkSeries++;
			keepGoing = false;

			log_writer.doLogging_QRmgr(logRoot, logName, "<decodeAndSave> error: Series No not what was expected: "+seriesNo+", "+currentChunkSeries);

			retrieveData.closeStreams(fOut,logRoot,logName);

			break;
		   }

		 //read chunk No within series..
		 length = 0;
		 buffer = new byte[4];
		 length = this.input.read(buffer);
		 if(length <= 0) //EOF.
		   {
			keepGoing = false;
			retrieveData.closeStreams(fOut,logRoot,logName);

			break;
		   }
		 while(length < 4)
		   {
			length += this.input.read(buffer, length, (4 - length));
		   }

		 int chunkNo = generic_class.read_intVALUE(buffer);
		 if(chunkNo != currentChunkNo)
		   {
			System.out.println("<decodeAndSave> error: chunk No not what was expected: "+chunkNo+", "+currentChunkNo);
			currentChunkSeries++;
			keepGoing = false;

			log_writer.doLogging_QRmgr(logRoot, logName, "<decodeAndSave> error: chunk No not what was expected: "+chunkNo+", "+currentChunkNo);
			retrieveData.closeStreams(fOut,logRoot,logName);

			break;
		   }

		 currentChunkNo++;

		 //read chunk status..
		 length = 0;
		 buffer = new byte[1];
		 length = this.input.read(buffer);
		 if(length <= 0) //EOF.
		   {
			keepGoing = false;
			retrieveData.closeStreams(fOut,logRoot,logName);

			break;
		   }

         while(length < 1)
		   {
			length += this.input.read(buffer, length, (1 - length));
		   }

		 byte chunkStatus = generic_class.read_byteVALUE(buffer);
		 if(chunkStatus == 1)
		   {
			//read size..
			length = 0;
			buffer = new byte[4];
			length = this.input.read(buffer);
			if(length <= 0) //EOF.
			  {
			   keepGoing = false;
			   retrieveData.closeStreams(fOut,logRoot,logName);

			   break;
			  }
			while(length < 4)
			  {
			   length += this.input.read(buffer, length, (4 - length));
			  }

			System.out.println("<decodeAndSave> file complete. series No: "+seriesNo);
			currentChunkSeries++;

		    retrieveData.closeStreams(fOut,logRoot,logName);


		    //------------------------------
		    this.start_pushObject(filePath);
		    //------------------------------


			/*
			/////////////////////////////////////////////////////////////////////////////////////////////////////////
			boolean moved = new File(fileNameNo).renameTo(new File(forTransferPath, new File(fileNameNo).getName()));
			if(!moved)
			  {
			   log_writer.doLogging_QRmgr(logRoot, logName, "<httpTestServer> couldn't move file: "+fileNameNo);
		      }
			 /////////////////////////////////////////////////////////////////////////////////////////////////////////
			 */

			break;
		   }
		 else if(chunkStatus == 2)
		   {
			//read last size value..
			length = 0;
			buffer = new byte[4];
			length = this.input.read(buffer);

            if(length <= 0) //EOF.
			  {
			   keepGoing = false;
			   retrieveData.closeStreams(fOut,logRoot,logName);

			   break;
			  }
			 while(length < 4)
			  {
			   length += this.input.read(buffer, length, (4 - length));
			  }

			System.out.println("<decodeAndSave> transmission complete");
			keepGoing = false;

			retrieveData.closeStreams(fOut,logRoot,logName);

			break;
		   }
		 else if(chunkStatus == 0)
		   {
			//read chunk size..
			length = 0;
			buffer = new byte[4];
			length = this.input.read(buffer);
			if(length <= 0) //EOF.
			  {
			   keepGoing = false;
			   retrieveData.closeStreams(fOut,logRoot,logName);

			   break;
			  }
			while(length < 4)
			  {
			   length += this.input.read(buffer, length, (4 - length));
			  }

			int chunkSize = generic_class.read_intVALUE(buffer);

            length = 0;
			buffer = new byte[chunkSize];
			length = this.input.read(buffer);
			while(length < chunkSize)
			  {
			   length += this.input.read(buffer, length, (chunkSize - length));
			  }

			//write to disk..
			if(fOut == null)
			  {
			   fOut = new FileOutputStream(fileNameNo, true);
			  }
			fOut.write(buffer, 0, buffer.length);
		   }
		 else
		   {
			System.out.println("<decodeAndSave> error: Invalid chunk status: "+chunkStatus);
			keepGoing = false;
            retrieveData.closeStreams(fOut,logRoot,logName);
			log_writer.doLogging_QRmgr(logRoot, logName, "<decodeAndSave> error: Invalid chunk status: "+chunkStatus);

			break;
		   }
		}
	  }


  } catch (Exception e){
 		  retrieveData.closeStreams(fOut,logRoot,logName);

 		  log_writer.doLogging_QRmgr(logRoot, logName, "retrieveData.decodeAndSave() error: "+e);
          e.printStackTrace();
 		  this.closeSocket();
 } finally {
		    retrieveData.closeStreams(fOut,logRoot,logName);

		    this.closeSocket();
 }
 }




  public synchronized String get_newFileNumber(){

  String value = null;
  try {
       value = ""+System.currentTimeMillis()+"_"+retrieveData.fileRenameCounter;
       retrieveData.fileRenameCounter++;

  } catch (Exception e){
	       value = null;
           e.printStackTrace();
           log_writer.doLogging_QRmgr(this.logRoot, this.logName, "retrieveData.get_newFileNumber() exception: "+e);
  }
  return value;
  }





public static void closeStreams(FileOutputStream fOut,
							    String logRoot,
							    String logName){
try {
  if(fOut != null)
	{
	 fOut.close();
	 fOut = null;
	}
} catch (Exception e){
	   e.printStackTrace();
	   log_writer.doLogging_QRmgr(logRoot, logName, "<retrieveData> closeSocket(2) error: "+e);
}
}


 public Socket getSocket(){

 return this.socket;
 }




 public void closeSocket(){
 try {
	  if(this.out != null)
	    {
	     this.out.close();
	     this.out = null;
	    }
	  if(this.socket != null)
		{
		 this.socket.close();
		 this.socket = null;
		}
 } catch (Exception e){
		  e.printStackTrace();
		  log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<retrieveData closeSocket() error: "+e);
 }
 }



 public boolean socketConnected(){

 boolean connected = true;

 if(this.getSocket() == null){connected = false;}
 else if(!(this.getSocket().isConnected())){connected = false;}
 else if(this.getSocket().isClosed()){connected = false;}

 return connected;
 }



 public static void main(String[] arg){

 new retrieveData().commenceOp("C:/mike_work/config/retriever_properties.PROPERTIES",
                               "C:/mike_work/config/dicom_sopClass_transferSyntax.PROPERTIES");
 }


 }