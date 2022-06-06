/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is part of dcm4che, an implementation of DICOM(TM) in
 * Java(TM), hosted at http://sourceforge.net/projects/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * Gunter Zeilinger, Huetteldorferstr. 24/10, 1150 Vienna/Austria/Europe.
 * Portions created by the Initial Developer are Copyright (C) 2002-2005
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * Gunter Zeilinger <gunterze@gmail.com>
 * Michael Bassey <mabaze178@yahoo.com>
 * Edgar Luberenga <edgar@pacs.pro>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */
package org.dcm4che2.tool.dcmsnd;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.UID;
import org.dcm4che2.data.UIDDictionary;
import org.dcm4che2.data.VR;
import org.dcm4che2.io.DicomInputStream;
import org.dcm4che2.io.DicomOutputStream;
import org.dcm4che2.io.StopTagInputHandler;
import org.dcm4che2.io.TranscoderInputHandler;
import org.dcm4che2.net.Association;
import org.dcm4che2.net.CommandUtils;
import org.dcm4che2.net.ConfigurationException;
import org.dcm4che2.net.Device;
import org.dcm4che2.net.DimseRSP;
import org.dcm4che2.net.DimseRSPHandler;
import org.dcm4che2.net.NetworkApplicationEntity;
import org.dcm4che2.net.NetworkConnection;
import org.dcm4che2.net.NewThreadExecutor;
import org.dcm4che2.net.NoPresentationContextException;
import org.dcm4che2.net.PDVOutputStream;
import org.dcm4che2.net.TransferCapability;
import org.dcm4che2.net.UserIdentity;
import org.dcm4che2.net.service.StorageCommitmentService;
import org.dcm4che2.util.CloseUtils;
import org.dcm4che2.util.StringUtils;
import org.dcm4che2.util.UIDUtils;

//------------------------------------------------
import java.util.concurrent.ArrayBlockingQueue;
import org.dcm4che2.net.log_writer;
import common.manager.BasicManager;
import common.job.BasicJob;
import retriever.pushToDestination;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import utility.general.dicomFileParser;
import org.dcm4che2.tool.dcmrcv.dcm_folderCreator;
import common.manager.clearanceManager;
import java.net.Socket;
//-----------------------------------------------

/**
 * @author gunter zeilinger(gunterze@gmail.com)
 * @version $Revision: 7154 $ $Date: 2008-09-24 18:03:54 +0200 (Wed, 24 Sep 2008) $
 * @since Oct 13, 2005


 * On July 20, 2012, Michael Bassey & Edgar Luberenga made modifications to some sections of this code.
 * Modifications made were:
 * (1) Addition of new import statements.
 * (2) Declaration/addition of new variables.
 * (3) Addition of new statements.
 * (4) Definition/addition of new methods.
 * (5) Creation of a new method that borrows largely from an existent method.
 * (6) Creation/addition of new classes.
 * (7) Disabling an existent method of the original code.
 * (8) Creation/addition of a new method that uses the name of a disabled original method.
 * (9) Disabling an original statement.
 * (10) Calling a new method that uses the name of a disabled original method.
 * (11) Addition to an existent method.
 * (12) Modification of an original method.
 * (13) Replacing original statement with a modified one.
 */
public class DcmSnd extends StorageCommitmentService {

    private static final int KB = 1024;

    private static final int MB = KB * KB;

    private static final int PEEK_LEN = 1024;

    private static final String USAGE =
        "dcmsnd [Options] <aet>[@<host>[:<port>]] <file>|<directory>...";

    private static final String DESCRIPTION =
        "\nLoad composite DICOM Object(s) from specified DICOM file(s) and send it "
      + "to the specified remote Application Entity. If a directory is specified,"
      + "DICOM Object in files under that directory and further sub-directories "
      + "are sent. If <port> is not specified, DICOM default port 104 is assumed. "
      + "If also no <host> is specified, localhost is assumed. Optionally, a "
      + "Storage Commitment Request for successfully tranferred objects is sent "
      + "to the remote Application Entity after the storage. The Storage Commitment "
      + "result is accepted on the same association or - if a local port is "
      + "specified by option -L - in a separate association initiated by the "
      + "remote Application Entity\n"
      + "OPTIONS:";

    private static final String EXAMPLE =
        "\nExample: dcmsnd -stgcmt -L DCMSND:11113 STORESCP@localhost:11112 image.dcm \n"
      + "=> Start listening on local port 11113 for receiving Storage Commitment "
      + "results, send DICOM object image.dcm to Application Entity STORESCP, "
      + "listening on local port 11112, and request Storage Commitment in same association.";

    private static char[] SECRET = { 's', 'e', 'c', 'r', 'e', 't' };

    private static final String[] ONLY_IVLE_TS = {
        UID.ImplicitVRLittleEndian
    };

    private static final String[] IVLE_TS = {
        UID.ImplicitVRLittleEndian,
        UID.ExplicitVRLittleEndian,
        UID.ExplicitVRBigEndian,
    };

    private static final String[] EVLE_TS = {
        UID.ExplicitVRLittleEndian,
        UID.ImplicitVRLittleEndian,
        UID.ExplicitVRBigEndian,
    };

    private static final String[] EVBE_TS = {
        UID.ExplicitVRBigEndian,
        UID.ExplicitVRLittleEndian,
        UID.ImplicitVRLittleEndian,
    };

    private static final int STG_CMT_ACTION_TYPE = 1;

    /** TransferSyntax: DCM4CHE URI Referenced */
    private static final String DCM4CHEE_URI_REFERENCED_TS_UID =
            "1.2.40.0.13.1.1.2.4.94";

    private Executor executor = new NewThreadExecutor("DCMSND");

    private NetworkApplicationEntity remoteAE = new NetworkApplicationEntity();

    private NetworkApplicationEntity remoteStgcmtAE;

    private NetworkConnection remoteConn = new NetworkConnection();

    private NetworkConnection remoteStgcmtConn = new NetworkConnection();

    private Device device = new Device("DCMSND");

    private NetworkApplicationEntity ae = new NetworkApplicationEntity();

    private NetworkConnection conn = new NetworkConnection();

    private Map<String, Set<String>> as2ts = new HashMap<String, Set<String>>();

    private ArrayList<FileInfo> files = new ArrayList<FileInfo>();

    private Association assoc;

    private int priority = 0;

    private int transcoderBufferSize = 1024;

    private int filesSent = 0;

    private long totalSize = 0L;

    private boolean fileref = false;

    private boolean stgcmt = false;

    private long shutdownDelay = 1000L;

    private DicomObject stgCmtResult;

    private String keyStoreURL = "resource:tls/test_sys_1.p12";

    private char[] keyStorePassword = SECRET;

    private char[] keyPassword;

    private String trustStoreURL = "resource:tls/mesa_certs.jks";

    private char[] trustStorePassword = SECRET;



    //-------------------------------------------
    private boolean assignAllCapabilities = true;
    private Object[][] sopClass_transferSyntax = null;
    private String alivenessFileLocation = null;
    private int alivenessFrequency = -1;
    private BasicManager nHawkStarter = null;
    private String[] original_args = null;
    private String logRoot = null;
    private String logName = null;
    private String objectStatus = null;
    private boolean teleradHack = false;
    private ArrayBlockingQueue<Object[]> fileLocations = null;
	private String supportedSopClass = null;
	private boolean alivenessFileAdded = false;
	private String ObjectState = "not_yet_set";
	private String[] mainStats = null;
	private boolean itsPusher = false;
	private pushToDestination pushObject = null;
	protected AtomicInteger alivenessValue = new AtomicInteger(0);
	AtomicBoolean keepDoingAliveness = new AtomicBoolean(true);
	AtomicBoolean keepRetrieving = new AtomicBoolean(true);
	protected ArrayBlockingQueue<Object[]> awaitingCStoreRSP = null;
	protected ArrayBlockingQueue<Object[]> CStoreRSPids = null;
	private int filePushedCounter = 0;
	private String NODE_ALIAS = null;
	private String[] jobDetails = null;
	private String TEL_MSG = null;

    //-------------------------------------------

    public DcmSnd() {
        remoteAE.setInstalled(true);
        remoteAE.setAssociationAcceptor(true);
        remoteAE.setNetworkConnection(new NetworkConnection[] { remoteConn });

        device.setNetworkApplicationEntity(ae);
        device.setNetworkConnection(conn);
        ae.setNetworkConnection(conn);
        ae.setAssociationInitiator(true);
        ae.setAssociationAcceptor(true);
        ae.register(this);
        ae.setAETitle("DCMSND");

        //========================
        // my add:
        ae.setDCMSND_status(true);
        ae.setDcmsndObject(this);
        //========================
    }




    //------------------------------------
    // AR added...

    public void set_TEL_MSG(String TEL_MSG)
    {
	 this.TEL_MSG = TEL_MSG;
	}

	public String get_TEL_MSG()
	{
	 return this.TEL_MSG;
	}

    public void set_jobDetails(String[] jobDetails)
    {
	 this.jobDetails = jobDetails;
	}

	public String[] get_jobDetails()
	{
	 return this.jobDetails;
	}

    public boolean socketIsClosed()
    {
		boolean valToReturn = false;
		try{
			if(assoc == null)
			  {
			   valToReturn = true;
			  }
			else
			  {
			   valToReturn = assoc.sockClosed;
			  }
		} catch (Exception e){
				 valToReturn = true;
				 log_writer.doLogging_QRmgr(this.logRoot, this.logName, "DcmSnd.socketIsClosed() exception: "+e);
		}
	return valToReturn;
	}

    public void set_mainStats(String[] value){

	this.mainStats = value;
	}


	public void set_nodeAlias(String value){

	this.NODE_ALIAS = value;
	}


	public String get_nodeAlias(){

	return this.NODE_ALIAS;
	}

	public String[] get_mainStats(){

	return this.mainStats;
	}

    public String getLogRoot(){

	return this.logRoot;
	}

    public String getLogName(){

    return this.logName;
    }

    public DcmSnd get_thisObject(){

	return this;
	}

    public String get_ObjectState(){

	return this.ObjectState;
	}

	public void set_ObjectState(String value){

	this.ObjectState = value;
	}

    public boolean still_retrieving(){

	return this.keepRetrieving.get();
	}

    public String[] get_supportedSopClasses(){

    String[] supported_sclasses = null;
    try {
         if(this.sopClass_transferSyntax != null)
           {
            supported_sclasses = new String[this.sopClass_transferSyntax.length];
            for(int a = 0; a < this.sopClass_transferSyntax.length; a++)
               {
                if(this.sopClass_transferSyntax[a][0] != null)
                  {
                   supported_sclasses[a] = (String) this.sopClass_transferSyntax[a][0];
			      }
	           }
	        }

    } catch (Exception e){
	         supported_sclasses = null;
	         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "DcmSnd.get_supportedSopClasses() exception: "+e);
	}
	return supported_sclasses;
	}



    public void set_objectStatus(String val){

	this.objectStatus = val;
	}

	public String get_objectStatus(){

	return this.objectStatus;
	}


	public int get_noOfScansInBuffer(){

	int value = 0;
	try {
	     value = fileLocations.size();
	} catch (Exception e){
		     value = -1;
	         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<get_noOfScansInBuffer> exception: "+e);
	}
	return value;
	}

    public void setSCandTS(Object [][] sopClass_transferSyntax, BasicManager nHawkStarter){

	this.sopClass_transferSyntax = sopClass_transferSyntax;
	this.nHawkStarter = nHawkStarter;

	try {
		 fileLocations = new ArrayBlockingQueue<Object[]>(this.nHawkStarter.get_dcmsnd_filelocationSize());

		 awaitingCStoreRSP = new ArrayBlockingQueue<Object[]>((this.nHawkStarter.get_dcmsnd_filelocationSize()) * 2);
		 CStoreRSPids = new ArrayBlockingQueue<Object[]>((this.nHawkStarter.get_dcmsnd_filelocationSize()));
		 new Thread (new processCStoreRSP()).start();


	} catch (Exception e){
	//----------------------------------
    log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<setSCandTS> exception: "+e);
    //-----------------------------------
	}
	}


    public void setSCandTS(Object [][] sopClass_transferSyntax, pushToDestination pushObject){

	this.sopClass_transferSyntax = sopClass_transferSyntax;
    this.itsPusher = true;
    this.pushObject = pushObject;

	try {
		 fileLocations = new ArrayBlockingQueue<Object[]>(this.pushObject.get_dcmsndQueueSize());

	} catch (Exception e){
	//----------------------------------
    log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<setSCandTS - 2> exception: "+e);
    //-----------------------------------
	}
	}



	public void set_alivenessStats(String[] stats){

	try {
	     this.alivenessFileLocation = stats[0];
	     this.alivenessFrequency = Integer.parseInt(stats[1]);
	} catch (Exception e){
	         e.printStackTrace();
	}
	}


	public void startOp(String[] args,
	                    DcmSnd dcmsnd,
	                    String logRoot,
	                    String logName){
	try {
         this.logRoot = logRoot;
         this.logName = logName;
    } catch (Exception e){
	} finally {
         this.startOp(args, dcmsnd);
	}
	}


    @SuppressWarnings("unchecked")
    public void startOp(String[] args, DcmSnd dcmsnd){

        //----------------------------------------------

        boolean continueOp = true;
        boolean setHack = false;
		System.out.println("DcmSnd args length: "+args.length);
		for(int a = 0; a < args.length; a++)
		   {
		    System.out.println("DcmSnd args["+a+"]: "+args[a]);
		   }

		if((args[args.length - 1]).equals("teleradhack"))
		  {
		   setHack = true;
		   String[] rebuild = new String[(args.length) - 1];
		   for(int a = 0; a < rebuild.length; a++)
			  {
			   rebuild[a] = args[a];
			  }
		   args = rebuild;
		  }
		//------------------------------------------------

        this.original_args = args;

        CommandLine cl = this.parse(args);

        //DcmSnd dcmsnd = new DcmSnd();

        //-----------------------------------
        //AR added...
        if(setHack)
          {
		   dcmsnd.set_ARhack(true);
		  }
        //-----------------------------------

        //if(!(this.get_ObjectState().equals("failed"))){

        if(this.get_ObjectState().equals("failed"))
          {}
        else
          {
			final List<String> argList = cl.getArgList();
			String remoteAE = argList.get(0);
			String[] calledAETAddress = split(remoteAE, '@');
			dcmsnd.setCalledAET(calledAETAddress[0]);
			if (calledAETAddress[1] == null) {
				dcmsnd.setRemoteHost("127.0.0.1");
				dcmsnd.setRemotePort(104);
			} else {
				String[] hostPort = split(calledAETAddress[1], ':');
				dcmsnd.setRemoteHost(hostPort[0]);
				dcmsnd.setRemotePort(toPort(hostPort[1]));
			}


			if(!(this.get_ObjectState().equals("failed"))){
			if (cl.hasOption("L")) {
				String localAE = cl.getOptionValue("L");
				String[] localPort = split(localAE, ':');
				if (localPort[1] != null) {
					dcmsnd.setLocalPort(toPort(localPort[1]));
				}
				String[] callingAETHost = split(localPort[0], '@');
				dcmsnd.setCalling(callingAETHost[0]);
				if (callingAETHost[1] != null) {
					dcmsnd.setLocalHost(callingAETHost[1]);
				}
			}
			dcmsnd.setOfferDefaultTransferSyntaxInSeparatePresentationContext(
					cl.hasOption("ts1"));
			dcmsnd.setSendFileRef(cl.hasOption("fileref"));
		    }//



            if(!(this.get_ObjectState().equals("failed"))){
			if (cl.hasOption("username")) {
				String username = cl.getOptionValue("username");
				UserIdentity userId;
				if (cl.hasOption("passcode")) {
					String passcode = cl.getOptionValue("passcode");
					userId = new UserIdentity.UsernamePasscode(username,
							passcode.toCharArray());
				} else {
					userId = new UserIdentity.Username(username);
				}
				userId.setPositiveResponseRequested(cl.hasOption("uidnegrsp"));
				dcmsnd.setUserIdentity(userId);
			}
			dcmsnd.setStorageCommitment(cl.hasOption("stgcmt"));
		    }//


			String remoteStgCmtAE = null;

			if(!(this.get_ObjectState().equals("failed"))){
			if (cl.hasOption("stgcmtae")) {
				try {
					remoteStgCmtAE = cl.getOptionValue("stgcmtae");
					String[] aet_hostport = split(remoteStgCmtAE, '@');
					String[] host_port = split(aet_hostport[1], ':');
					dcmsnd.setStgcmtCalledAET(aet_hostport[0]);
					dcmsnd.setRemoteStgcmtHost(host_port[0]);
					dcmsnd.setRemoteStgcmtPort(toPort(host_port[1]));
				} catch (Exception e) {
					//exit("illegal argument of option -stgcmtae");

					exit("illegal argument of option -stgcmtae", this.get_thisObject());
				}
			}
		    }//



            if(!(this.get_ObjectState().equals("failed"))){
			if (cl.hasOption("connectTO"))
				dcmsnd.setConnectTimeout(parseInt(cl.getOptionValue("connectTO"),
						"illegal argument of option -connectTO", 1,
						Integer.MAX_VALUE));
		    }


		    if(!(this.get_ObjectState().equals("failed"))){
			if (cl.hasOption("reaper"))
				dcmsnd.setAssociationReaperPeriod(
						parseInt(cl.getOptionValue("reaper"),
						"illegal argument of option -reaper",
						1, Integer.MAX_VALUE));
		    }


		    if(!(this.get_ObjectState().equals("failed"))){
			if (cl.hasOption("rspTO"))
				dcmsnd.setDimseRspTimeout(parseInt(cl.getOptionValue("rspTO"),
						"illegal argument of option -rspTO",
						1, Integer.MAX_VALUE));
		    }


		    if(!(this.get_ObjectState().equals("failed"))){
			if (cl.hasOption("acceptTO"))
				dcmsnd.setAcceptTimeout(parseInt(cl.getOptionValue("acceptTO"),
						"illegal argument of option -acceptTO",
						1, Integer.MAX_VALUE));
		    }



		    if(!(this.get_ObjectState().equals("failed"))){
			if (cl.hasOption("releaseTO"))
				dcmsnd.setReleaseTimeout(parseInt(cl.getOptionValue("releaseTO"),
						"illegal argument of option -releaseTO",
						1, Integer.MAX_VALUE));
		    }


            if(!(this.get_ObjectState().equals("failed"))){
			if (cl.hasOption("soclosedelay"))
				dcmsnd.setSocketCloseDelay(
						parseInt(cl.getOptionValue("soclosedelay"),
						"illegal argument of option -soclosedelay", 1, 10000));
		    }




		    if(!(this.get_ObjectState().equals("failed"))){
			if (cl.hasOption("shutdowndelay"))
				dcmsnd.setShutdownDelay(
						parseInt(cl.getOptionValue("shutdowndelay"),
						"illegal argument of option -shutdowndelay", 1, 10000));
		    }


		    if(!(this.get_ObjectState().equals("failed"))){
			if (cl.hasOption("rcvpdulen"))
				dcmsnd.setMaxPDULengthReceive(
						parseInt(cl.getOptionValue("rcvpdulen"),
						"illegal argument of option -rcvpdulen", 1, 10000) * KB);
		    }


		    if(!(this.get_ObjectState().equals("failed"))){
			if (cl.hasOption("sndpdulen"))
				dcmsnd.setMaxPDULengthSend(parseInt(cl.getOptionValue("sndpdulen"),
						"illegal argument of option -sndpdulen", 1, 10000) * KB);
		    }



		    if(!(this.get_ObjectState().equals("failed"))){
			if (cl.hasOption("sosndbuf"))
				dcmsnd.setSendBufferSize(parseInt(cl.getOptionValue("sosndbuf"),
						"illegal argument of option -sosndbuf", 1, 10000) * KB);
		    }



			if(!(this.get_ObjectState().equals("failed"))){
			if (cl.hasOption("sorcvbuf"))
				dcmsnd.setReceiveBufferSize(parseInt(cl.getOptionValue("sorcvbuf"),
						"illegal argument of option -sorcvbuf", 1, 10000) * KB);
		    }



		    if(!(this.get_ObjectState().equals("failed"))){
			if (cl.hasOption("bufsize"))
				dcmsnd.setTranscoderBufferSize(
						parseInt(cl.getOptionValue("bufsize"),
						"illegal argument of option -bufsize", 1, 10000) * KB);
			dcmsnd.setPackPDV(!cl.hasOption("pdv1"));
			dcmsnd.setTcpNoDelay(!cl.hasOption("tcpdelay"));
		    }


		    if(!(this.get_ObjectState().equals("failed"))){
			if (cl.hasOption("async"))
				dcmsnd.setMaxOpsInvoked(parseInt(cl.getOptionValue("async"),
						"illegal argument of option -async", 0, 0xffff));
		    }


		    if(!(this.get_ObjectState().equals("failed"))){
			if (cl.hasOption("lowprior"))
				dcmsnd.setPriority(CommandUtils.LOW);
		    }


		    if(!(this.get_ObjectState().equals("failed"))){
			if (cl.hasOption("highprior"))
				dcmsnd.setPriority(CommandUtils.HIGH);
		    }


			System.out.println("Scanning files to send");
			long t1 = System.currentTimeMillis();
			for (int i = 1, n = argList.size(); i < n; ++i)
				dcmsnd.addFile(new File(argList.get(i)));
			long t2 = System.currentTimeMillis();

			if (dcmsnd.getNumberOfFilesToSend() == 0) {

				continueOp = false;

				//System.exit(2);

				exit("No of files to send is 0", this.get_thisObject());
			}


            if(!(this.get_ObjectState().equals("failed"))){
			System.out.println("\nScanned " + dcmsnd.getNumberOfFilesToSend()
					+ " files in " + ((t2 - t1) / 1000F) + "s (="
					+ ((t2 - t1) / dcmsnd.getNumberOfFilesToSend()) + "ms/file)");

			dcmsnd.configureTransferCapability();
		    }//

            if(!(this.get_ObjectState().equals("failed"))){
			if (cl.hasOption("tls")) {
				String cipher = cl.getOptionValue("tls");
				if ("NULL".equalsIgnoreCase(cipher)) {
					dcmsnd.setTlsWithoutEncyrption();
				} else if ("3DES".equalsIgnoreCase(cipher)) {
					dcmsnd.setTls3DES_EDE_CBC();
				} else if ("AES".equalsIgnoreCase(cipher)) {
					dcmsnd.setTlsAES_128_CBC();
				} else {
					//exit("Invalid parameter for option -tls: " + cipher);

					exit("Invalid parameter for option -tls: " + cipher, this.get_thisObject());
				}

				if(!(this.get_ObjectState().equals("failed"))){
				if (cl.hasOption("nossl2")) {
					dcmsnd.disableSSLv2Hello();
				}
				dcmsnd.setTlsNeedClientAuth(!cl.hasOption("noclientauth"));
			    }//


                if(!(this.get_ObjectState().equals("failed"))){
				if (cl.hasOption("keystore")) {
					dcmsnd.setKeyStoreURL(cl.getOptionValue("keystore"));
				}
			    }


			    if(!(this.get_ObjectState().equals("failed"))){
				if (cl.hasOption("keystorepw")) {
					dcmsnd.setKeyStorePassword(
							cl.getOptionValue("keystorepw"));
				}
			    }


			    if(!(this.get_ObjectState().equals("failed"))){
				if (cl.hasOption("keypw")) {
					dcmsnd.setKeyPassword(cl.getOptionValue("keypw"));
				}
			    }


			    if(!(this.get_ObjectState().equals("failed"))){
				if (cl.hasOption("truststore")) {
					dcmsnd.setTrustStoreURL(
							cl.getOptionValue("truststore"));
				}
			    }


			    if(!(this.get_ObjectState().equals("failed"))){
				if (cl.hasOption("truststorepw")) {
					dcmsnd.setTrustStorePassword(
							cl.getOptionValue("truststorepw"));
				}
			    }


			    if(!(this.get_ObjectState().equals("failed"))){
				try {
					dcmsnd.initTLS();
				} catch (Exception e) {
					System.err.println("ERROR: Failed to initialize TLS context:"
							+ e.getMessage());
					//System.exit(2);

					exit("ERROR: Failed to initialize TLS context:", this.get_thisObject());
				}
			    }//
			}
		    }//


            if(!(this.get_ObjectState().equals("failed"))){
			//-----------------------------------------------------------------
			if(this.teleradHack)
			  {
			   if(this.nHawkStarter.get_managerType() != 2)
			     {
			      // start keep-alive object..
			      new Thread(new keepAliveClass(this.alivenessFileLocation, this.alivenessFrequency)).start();
			      //============================================================================
			      log_writer.doLogging_QRmgr(logRoot, logName, "ALIVENESS FILE STARTED>>>> "+this.nHawkStarter.get_managerType());
			      //============================================================================
			     }

			  }
			//-----------------------------------------------------------------
		    }//


		    if(!(this.get_ObjectState().equals("failed"))){
			try {
				dcmsnd.start();

				  //============================================================================
					log_writer.doLogging_QRmgr(logRoot, logName, "DCMSND POINT : 1 ");
			      //============================================================================
			} catch (Exception e) {
				System.err.println("ERROR: Failed to start server for receiving " +
						"Storage Commitment results:" + e.getMessage());
				//System.exit(2);

				exit( "Storage Commitment results:" + e.getMessage(), this.get_thisObject());
			}
		    }

		    if(!(this.get_ObjectState().equals("failed"))){
			try {
				t1 = System.currentTimeMillis();
				try {
					dcmsnd.open();

					//------------------------------
					//this.ObjectState = "successful";

				  //============================================================================
					log_writer.doLogging_QRmgr(logRoot, logName, "DCMSND POINT : 2 ");
			      //============================================================================

					this.set_ObjectState("successful");
					//------------------------------


				} catch (Exception e) {
					System.err.println("ERROR: Failed to establish association:"
							+ e.getMessage());
					//System.exit(2);

					exit("ERROR: Failed to establish association:"+e.getMessage(), this.get_thisObject());
				}

				if(!(this.get_ObjectState().equals("failed"))){
				t2 = System.currentTimeMillis();
				System.out.println("Connected to " + remoteAE + " in "
						+ ((t2 - t1) / 1000F) + "s");

				t1 = System.currentTimeMillis();
				dcmsnd.send();
			    }


			    if(!(this.get_ObjectState().equals("failed"))){
				t2 = System.currentTimeMillis();
				prompt(dcmsnd, (t2 - t1) / 1000F);
				if (dcmsnd.isStorageCommitment()) {
					t1 = System.currentTimeMillis();
					if (dcmsnd.commit()) {
						t2 = System.currentTimeMillis();
						System.out.println("Request Storage Commitment from "
								+ remoteAE + " in " + ((t2 - t1) / 1000F) + "s");
						System.out.println("Waiting for Storage Commitment Result..");
						try {
							DicomObject cmtrslt = dcmsnd.waitForStgCmtResult();
							t1 = System.currentTimeMillis();
							promptStgCmt(cmtrslt, ((t1 - t2) / 1000F));
						} catch (InterruptedException e) {
							System.err.println("ERROR:" + e.getMessage());
						}
					}
				 }
				dcmsnd.close();
			    }


                if(!(this.get_ObjectState().equals("failed"))){
				System.out.println("Released connection to " + remoteAE);
				if (remoteStgCmtAE != null) {
					t1 = System.currentTimeMillis();
					try {
						dcmsnd.openToStgcmtAE();
					} catch (Exception e) {
						System.err.println("ERROR: Failed to establish association:"
								+ e.getMessage());
						//System.exit(2);

						exit("ERROR: Failed to establish association:"+e.getMessage(), this.get_thisObject());
					}

					if(!(this.get_ObjectState().equals("failed"))){
					t2 = System.currentTimeMillis();
					System.out.println("Connected to " + remoteStgCmtAE + " in "
							+ ((t2 - t1) / 1000F) + "s");
					t1 = System.currentTimeMillis();
					if (dcmsnd.commit()) {
						t2 = System.currentTimeMillis();
						System.out.println("Request Storage Commitment from "
								+ remoteStgCmtAE + " in " + ((t2 - t1) / 1000F) + "s");
						System.out.println("Waiting for Storage Commitment Result..");
						try {
							DicomObject cmtrslt = dcmsnd.waitForStgCmtResult();
							t1 = System.currentTimeMillis();
							promptStgCmt(cmtrslt, ((t1 - t2) / 1000F));
						} catch (InterruptedException e) {
							System.err.println("ERROR:" + e.getMessage());
						}
					}
					dcmsnd.close();
					System.out.println("Released connection to " + remoteStgCmtAE);
				    }//
				}
			    }//


			} finally {
				dcmsnd.stop();
			}
		    }//

          }//end else..
	}



    public void putInStore(Object[] fileAndObject){
    try {
	     if(this.fileLocations != null){
		 synchronized(this.get_storeObject()){
	     if(!this.fileLocations.contains(fileAndObject))
	       {
		    if(keepRetrieving.get())
			  {
			   this.fileLocations.put(fileAndObject);
			  }
		    else
		      {
			   new Thread(new stashAwayInFailedQueue(fileAndObject)).start();
			  }
	       }
	       }
	       }
    } catch (Exception e){
	         e.printStackTrace();
	         //----------------------------------
			 log_writer.doLogging_QRmgr(logRoot, logName, "DcmSnd.putInStore() error: "+e);
			 new Thread(new stashAwayInFailedQueue(fileAndObject)).start();
             //-----------------------------------
	}
	}


	public Object[] takeFromStore(){
    Object[] loc = null;
    try {
	     loc = this.fileLocations.take();

	     System.out.println("<DcmSnd> took from store..");

	} catch (Exception e){
		     e.printStackTrace();
		     loc = null;
	}
	return loc;
	}




	public void setContainerToNull(){
    synchronized(this.get_storeObject()){

	this.fileLocations = null;
	}
	}



	protected ArrayBlockingQueue get_storeObject(){

	return this.fileLocations;
	}



    public void set_ARhack(boolean val){

	this.teleradHack = val;
	}


	public boolean get_ARhackStatus(){return this.teleradHack;}
	//---------------------------------------------------------------


    public final void setLocalHost(String hostname) {
        conn.setHostname(hostname);
    }

    public final void setLocalPort(int port) {
        conn.setPort(port);
    }

    public final void setRemoteHost(String hostname) {
        remoteConn.setHostname(hostname);
    }

    public final void setRemotePort(int port) {
        remoteConn.setPort(port);
    }

    public final void setRemoteStgcmtHost(String hostname) {
        remoteStgcmtConn.setHostname(hostname);
    }

    public final void setRemoteStgcmtPort(int port) {
        remoteStgcmtConn.setPort(port);
    }

    public final void setTlsWithoutEncyrption() {
        conn.setTlsWithoutEncyrption();
        remoteConn.setTlsWithoutEncyrption();
        remoteStgcmtConn.setTlsWithoutEncyrption();
    }

    public final void setTls3DES_EDE_CBC() {
        conn.setTls3DES_EDE_CBC();
        remoteConn.setTls3DES_EDE_CBC();
        remoteStgcmtConn.setTls3DES_EDE_CBC();
    }

    public final void setTlsAES_128_CBC() {
        conn.setTlsAES_128_CBC();
        remoteConn.setTlsAES_128_CBC();
        remoteStgcmtConn.setTlsAES_128_CBC();
    }

    public final void disableSSLv2Hello() {
        conn.disableSSLv2Hello();
    }

    public final void setTlsNeedClientAuth(boolean needClientAuth) {
        conn.setTlsNeedClientAuth(needClientAuth);
    }

    public final void setKeyStoreURL(String url) {
        keyStoreURL = url;
    }

    public final void setKeyStorePassword(String pw) {
        keyStorePassword = pw.toCharArray();
    }

    public final void setKeyPassword(String pw) {
        keyPassword = pw.toCharArray();
    }

    public final void setTrustStorePassword(String pw) {
        trustStorePassword = pw.toCharArray();
    }

    public final void setTrustStoreURL(String url) {
        trustStoreURL = url;
    }

    public final void setCalledAET(String called) {
        remoteAE.setAETitle(called);
    }

    public final void setCalling(String calling) {
        ae.setAETitle(calling);
    }

    public final void setUserIdentity(UserIdentity userIdentity) {
        ae.setUserIdentity(userIdentity);
    }

    public final void setOfferDefaultTransferSyntaxInSeparatePresentationContext(
            boolean enable) {
        ae.setOfferDefaultTransferSyntaxInSeparatePresentationContext(enable);
    }

    public final void setSendFileRef(boolean fileref) {
        this.fileref = fileref;
    }

    public final void setStorageCommitment(boolean stgcmt) {
        this.stgcmt = stgcmt;
    }

    public final boolean isStorageCommitment() {
        return stgcmt;
    }

    public final void setStgcmtCalledAET(String called) {
        remoteStgcmtAE = new NetworkApplicationEntity();
        remoteStgcmtAE.setInstalled(true);
        remoteStgcmtAE.setAssociationAcceptor(true);
        remoteStgcmtAE.setNetworkConnection(
                new NetworkConnection[] { remoteStgcmtConn });
        remoteStgcmtAE.setAETitle(called);
    }

    public final void setShutdownDelay(int shutdownDelay) {
        this.shutdownDelay = shutdownDelay;
    }


    public final void setConnectTimeout(int connectTimeout) {
        conn.setConnectTimeout(connectTimeout);
    }

    public final void setMaxPDULengthReceive(int maxPDULength) {
        ae.setMaxPDULengthReceive(maxPDULength);
    }

    public final void setMaxOpsInvoked(int maxOpsInvoked) {
        ae.setMaxOpsInvoked(maxOpsInvoked);
    }

    public final void setPackPDV(boolean packPDV) {
        ae.setPackPDV(packPDV);
    }

    public final void setAssociationReaperPeriod(int period) {
        device.setAssociationReaperPeriod(period);
    }

    public final void setDimseRspTimeout(int timeout) {
        ae.setDimseRspTimeout(timeout);
    }

    public final void setPriority(int priority) {
        this.priority = priority;
    }

    public final void setTcpNoDelay(boolean tcpNoDelay) {
        conn.setTcpNoDelay(tcpNoDelay);
    }

    public final void setAcceptTimeout(int timeout) {
        conn.setAcceptTimeout(timeout);
    }

    public final void setReleaseTimeout(int timeout) {
        conn.setReleaseTimeout(timeout);
    }

    public final void setSocketCloseDelay(int timeout) {
        conn.setSocketCloseDelay(timeout);
    }

    public final void setMaxPDULengthSend(int maxPDULength) {
        ae.setMaxPDULengthSend(maxPDULength);
    }

    public final void setReceiveBufferSize(int bufferSize) {
        conn.setReceiveBufferSize(bufferSize);
    }

    public final void setSendBufferSize(int bufferSize) {
        conn.setSendBufferSize(bufferSize);
    }

    public final void setTranscoderBufferSize(int transcoderBufferSize) {
        this.transcoderBufferSize = transcoderBufferSize;
    }

    public final int getNumberOfFilesToSend() {
        return files.size();
    }

    public final int getNumberOfFilesSent() {
        return filesSent;
    }

    public final long getTotalSizeSent() {
        return totalSize;
    }

    public List<FileInfo> getFileInfos() {
        return files;
    }



    private CommandLine parse(String[] args) {
        Options opts = new Options();
        OptionBuilder.withArgName("aet[@host][:port]");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription(
                "set AET, local address and listening port of local Application Entity");
        opts.addOption(OptionBuilder.create("L"));

        //----------------------------------------------------------------------------------
        opts.addOption("ts1", true, "offer Default Transfer Syntax in " +
                "separate Presentation Context. By default offered with\n" +
                "Explicit VR Little Endian TS in one PC.");
        //---------------------------------------------------------------------------------

        opts.addOption("fileref", false,
                "send objects without pixel data, but with a reference to " +
                "the DICOM file using DCM4CHE URI\nReferenced Transfer Syntax " +
                "to import DICOM objects\n                            on a given file system to a DCM4CHEE " +
                "archive.");

        OptionBuilder.withArgName("username");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription(
                "enable User Identity Negotiation with specified username and "
                + " optional passcode");
        opts.addOption(OptionBuilder.create("username"));

        OptionBuilder.withArgName("passcode");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription(
                "optional passcode for User Identity Negotiation, "
                + "only effective with option -username");
        opts.addOption(OptionBuilder.create("passcode"));

        opts.addOption("uidnegrsp", false,
                "request positive User Identity Negotation response, "
                + "only effective with option -username");

        OptionBuilder.withArgName("NULL|3DES|AES");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription(
                "enable TLS connection without, 3DES or AES encryption");
        opts.addOption(OptionBuilder.create("tls"));

        opts.addOption("nossl2", false, "disable acceptance of SSLv2Hello TLS handshake");
        opts.addOption("noclientauth", false, "disable client authentification for TLS");

        OptionBuilder.withArgName("file|url");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription(
                "file path or URL of P12 or JKS keystore, resource:tls/test_sys_2.p12 by default");
        opts.addOption(OptionBuilder.create("keystore"));

        OptionBuilder.withArgName("password");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription(
                "password for keystore file, 'secret' by default");
        opts.addOption(OptionBuilder.create("keystorepw"));

        OptionBuilder.withArgName("password");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription(
                "password for accessing the key in the keystore, keystore password by default");
        opts.addOption(OptionBuilder.create("keypw"));

        OptionBuilder.withArgName("file|url");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription(
                "file path or URL of JKS truststore, resource:tls/mesa_certs.jks by default");
        opts.addOption(OptionBuilder.create("truststore"));

        OptionBuilder.withArgName("password");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription(
                "password for truststore file, 'secret' by default");
        opts.addOption(OptionBuilder.create("truststorepw"));

        OptionBuilder.withArgName("aet@host:port");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription(
                "request storage commitment of (successfully) sent objects " +
                "afterwards in new association\nto specified remote " +
                "Application Entity");
        opts.addOption(OptionBuilder.create("stgcmtae"));

        opts.addOption("stgcmt", false,
                "request storage commitment of (successfully) sent objects " +
                "afterwards in same association");

        OptionBuilder.withArgName("maxops");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription(
                "maximum number of outstanding operations it may invoke " +
                "asynchronously, unlimited by default.");
        opts.addOption(OptionBuilder.create("async"));

        opts.addOption("pdv1", false,
                "send only one PDV in one P-Data-TF PDU, " +
                "pack command and data PDV in one P-DATA-TF PDU by default.");
        opts.addOption("tcpdelay", false,
                "set TCP_NODELAY socket option to false, true by default");

        OptionBuilder.withArgName("ms");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription(
                "timeout in ms for TCP connect, no timeout by default");
        opts.addOption(OptionBuilder.create("connectTO"));

        OptionBuilder.withArgName("ms");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription(
                "delay in ms for Socket close after sending A-ABORT, " +
                "50ms by default");
        opts.addOption(OptionBuilder.create("soclosedelay"));

        OptionBuilder.withArgName("ms");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription(
                "delay in ms for closing the listening socket, " +
                "1000ms by default");
        opts.addOption(OptionBuilder.create("shutdowndelay"));

        OptionBuilder.withArgName("ms");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription(
                "period in ms to check for outstanding DIMSE-RSP, " +
                "10s by default");
        opts.addOption(OptionBuilder.create("reaper"));

        OptionBuilder.withArgName("ms");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription(
                "timeout in ms for receiving DIMSE-RSP, 60s by default");
        opts.addOption(OptionBuilder.create("rspTO"));

        OptionBuilder.withArgName("ms");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription(
                "timeout in ms for receiving A-ASSOCIATE-AC, 5s by default");
        opts.addOption(OptionBuilder.create("acceptTO"));

        OptionBuilder.withArgName("ms");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription(
                "timeout in ms for receiving A-RELEASE-RP, 5s by default");
        opts.addOption(OptionBuilder.create("releaseTO"));

        OptionBuilder.withArgName("KB");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription(
                "maximal length in KB of received P-DATA-TF PDUs, 16KB by default");
        opts.addOption(OptionBuilder.create("rcvpdulen"));

        OptionBuilder.withArgName("KB");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription(
                "maximal length in KB of sent P-DATA-TF PDUs, 16KB by default");
        opts.addOption(OptionBuilder.create("sndpdulen"));

        OptionBuilder.withArgName("KB");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription(
                "set SO_RCVBUF socket option to specified value in KB");
        opts.addOption(OptionBuilder.create("sorcvbuf"));

        OptionBuilder.withArgName("KB");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription(
                "set SO_SNDBUF socket option to specified value in KB");
        opts.addOption(OptionBuilder.create("sosndbuf"));

        OptionBuilder.withArgName("KB");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription(
                "transcoder buffer size in KB, 1KB by default");
        opts.addOption(OptionBuilder.create("bufsize"));

        opts.addOption("lowprior", false,
                "LOW priority of the C-STORE operation, MEDIUM by default");
        opts.addOption("highprior", false,
                "HIGH priority of the C-STORE operation, MEDIUM by default");
        opts.addOption("h", "help", false, "print this message");
        opts.addOption("V", "version", false,
                "print the version information and exit");
        CommandLine cl = null;
        try {
            cl = new GnuParser().parse(opts, args);

        } catch (ParseException e) {
            //exit("dcmsnd: " + e.getMessage());

            exit( "dcmsnd:" + e.getMessage(), this.get_thisObject());

            throw new RuntimeException("unreachable");
        }
        if (cl.hasOption('V')) {
            Package p = DcmSnd.class.getPackage();
            System.out.println("dcmsnd v" + p.getImplementationVersion());

            exit("<parse commandline error>", this.get_thisObject());

            //System.exit(0);
        }
        if (cl.hasOption('h') || cl.getArgList().size() < 2) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(USAGE, DESCRIPTION, opts, EXAMPLE);
            //System.exit(0);

            exit("<parse commandline error>", this.get_thisObject());
        }
        return cl;
    }


    private static void promptStgCmt(DicomObject cmtrslt, float seconds) {
        System.out.println("Received Storage Commitment Result after "
                + seconds + "s:");
        DicomElement refSOPSq = cmtrslt.get(Tag.ReferencedSOPSequence);
        System.out.print(refSOPSq.countItems());
        System.out.println(" successful");
        DicomElement failedSOPSq = cmtrslt.get(Tag.FailedSOPSequence);
        if (failedSOPSq != null) {
            System.out.print(failedSOPSq.countItems());
            System.out.println(" FAILED!");
        }
    }

    private synchronized DicomObject waitForStgCmtResult() throws InterruptedException {
        while (stgCmtResult == null) wait();
        return stgCmtResult;
    }

    private static void prompt(DcmSnd dcmsnd, float seconds) {
        System.out.print("\nSent ");
        System.out.print(dcmsnd.getNumberOfFilesSent());
        System.out.print(" objects (=");
        promptBytes(dcmsnd.getTotalSizeSent());
        System.out.print(") in ");
        System.out.print(seconds);
        System.out.print("s (=");
        promptBytes(dcmsnd.getTotalSizeSent() / seconds);
        System.out.println("/s)");
    }

    private static void promptBytes(float totalSizeSent) {
        if (totalSizeSent > MB) {
            System.out.print(totalSizeSent / MB);
            System.out.print("MB");
        } else {
            System.out.print(totalSizeSent / KB);
            System.out.print("KB");
        }
    }

    private int toPort(String port) {
        return port != null ? parseInt(port, "illegal port number", 1, 0xffff)
                : 104;
    }

    private static String[] split(String s, char delim) {
        String[] s2 = { s, null };
        int pos = s.indexOf(delim);
        if (pos != -1) {
            s2[0] = s.substring(0, pos);
            s2[1] = s.substring(pos + 1);
        }
        return s2;
    }

    /*
    private static void exit(String msg) {
        System.err.println(msg);
        System.err.println("Try 'dcmsnd -h' for more information.");
        System.exit(1);
    }
    */



    private static void exit(String msg, DcmSnd dcmsnd_obj) {

    if(dcmsnd_obj != null)
      {
       //----------------------------------
	   log_writer.doLogging_QRmgr(dcmsnd_obj.getLogRoot(), dcmsnd_obj.getLogName(), "<exit> msg: "+msg);
       //-----------------------------------

       dcmsnd_obj.set_ObjectState("failed");
      }
    else
      {
	   System.err.println(msg);
	   System.err.println("Try 'dcmsnd -h' for more information.");
       System.exit(1);
	  }
    }

    private int parseInt(String s, String errPrompt, int min, int max) {
        try {
            int i = Integer.parseInt(s);
            if (i >= min && i <= max)
                return i;
        } catch (NumberFormatException e) {
            // parameter is not a valid integer; fall through to exit
        }
        //exit(errPrompt);

        exit("<parseInt error>", this.get_thisObject());
        throw new RuntimeException();
    }

    public void addFile(File f) {
        if (f.isDirectory()) {
            File[] fs = f.listFiles();
            for (int i = 0; i < fs.length; i++)
                addFile(fs[i]);
            return;
        }
        FileInfo info = new FileInfo(f);
        DicomObject dcmObj = new BasicDicomObject();
        DicomInputStream in = null;
        try {
            in = new DicomInputStream(f);
            in.setHandler(new StopTagInputHandler(Tag.StudyDate));
            in.readDicomObject(dcmObj, PEEK_LEN);
            info.tsuid = in.getTransferSyntax().uid();
            info.fmiEndPos = in.getEndOfFileMetaInfoPosition();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("WARNING: Failed to parse " + f + " - skipped.");
            System.out.print('F');
            return;
        } finally {
            CloseUtils.safeClose(in);
        }
        info.cuid = dcmObj.getString(Tag.SOPClassUID);
        if (info.cuid == null) {
            System.err.println("WARNING: Missing SOP Class UID in " + f
                    + " - skipped.");
            System.out.print('F');
            return;
        }
        info.iuid = dcmObj.getString(Tag.SOPInstanceUID);
        if (info.iuid == null) {
            System.err.println("WARNING: Missing SOP Instance UID in " + f
                    + " - skipped.");
            System.out.print('F');
            return;
        }

        ////////////////////////////////////////////////
        addTransferCapability(info.cuid, info.tsuid);
        files.add(info);
        ////////////////////////////////////////////////
        System.out.print('.');


       //----------------------------------
        if(this.teleradHack)
          {
           if(this.assignAllCapabilities)
             {
			  this.assignAllCapabilities = false;
			   assign_transferSyntaxes();
			 }
	      }
	    //------------------------------------
    }



    private void assign_transferSyntaxes(){
	try {
         for(int a = 0; a < this.sopClass_transferSyntax.length; a++)
            {
             String sopClassUid = (String) this.sopClass_transferSyntax[a][0];
             String[] transferSyntaxes = (String[]) this.sopClass_transferSyntax[a][1];
             for(int x = 0; x < transferSyntaxes.length; x++)
                {
			     addTransferCapability_hack(sopClassUid, transferSyntaxes[x]);
			    }
		    }
	} catch (Exception e){
	         e.printStackTrace();
	         //--------------------------------------------------
			 log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<assign_transferSyntaxes> Exception: "+e);
		     //--------------------------------------------------
	}
	}



    public Object[] setFile(File f) {

        Object[] data = new Object[4];

        FileInfo info = new FileInfo(f);
        DicomObject dcmObj = new BasicDicomObject();
        DicomInputStream in = null;
        try {
            in = new DicomInputStream(f);
            in.setHandler(new StopTagInputHandler(Tag.StudyDate));
            in.readDicomObject(dcmObj, PEEK_LEN);
            info.tsuid = in.getTransferSyntax().uid();
            info.fmiEndPos = in.getEndOfFileMetaInfoPosition();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("WARNING: Failed to parse " + f + " - skipped.");


        //----------------------------------
		log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<setFile> WARNING: Failed to parse " + f + " - skipped.");
		//-----------------------------------

            System.out.print('F');
            return data;
        } finally {
            CloseUtils.safeClose(in);
        }
        info.cuid = dcmObj.getString(Tag.SOPClassUID);
        if (info.cuid == null) {
            System.err.println("WARNING: Missing SOP Class UID in " + f
                    + " - skipped.");

       //----------------------------------
	  log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<setFile> WARNING: Missing SOP Class UID in " + f
                   + " - skipped.");
		//-----------------------------------

            System.out.print('F');
            return data;
        }
        info.iuid = dcmObj.getString(Tag.SOPInstanceUID);
        if (info.iuid == null) {
            System.err.println("WARNING: Missing SOP Instance UID in " + f
                    + " - skipped.");

        //----------------------------------
		log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<setFile> WARNING: Missing SOP Instance UID in " + f
                    + " - skipped.");
		//-----------------------------------

           System.out.print('F');

           return data;
        }
        System.out.print('.');

        data[0] = (Object) info;
        data[1] = (Object) info.cuid;
        data[2] = (Object) info.tsuid;
        data[3] = (Object) f.getName();

        return data;
    }


    public void addTransferCapability(String cuid, String tsuid) {
        Set<String> ts = as2ts.get(cuid);
        if (fileref) {
            if (ts == null) {
                as2ts.put(cuid,
                        Collections.singleton(DCM4CHEE_URI_REFERENCED_TS_UID));
            }
        } else {
            if (ts == null) {
                ts = new HashSet<String>();
                ts.add(UID.ImplicitVRLittleEndian);
                as2ts.put(cuid, ts);
            }
            ts.add(tsuid);
        }
    }



    public void addTransferCapability_hack(String cuid, String tsuid) {

    Set<String> ts = as2ts.get(cuid);
        if (fileref) {
            if (ts == null) {
                as2ts.put(cuid,
                        Collections.singleton(DCM4CHEE_URI_REFERENCED_TS_UID));
            }
        } else {
            if (ts == null) {
                ts = new HashSet<String>();
                ts.add(UID.ImplicitVRLittleEndian);
                as2ts.put(cuid, ts);
            }
            ts.add(tsuid);
        }
    }




    public void configureTransferCapability() {
        int off = stgcmt || remoteStgcmtAE != null ? 1 : 0;
        TransferCapability[] tc = new TransferCapability[off + as2ts.size()];
        if (off > 0) {
            tc[0] = new TransferCapability(
                    UID.StorageCommitmentPushModelSOPClass,
                    ONLY_IVLE_TS,
                    TransferCapability.SCU);
        }
        Iterator<Map.Entry<String, Set<String>>> iter = as2ts.entrySet().iterator();
        for (int i = off; i < tc.length; i++) {
            Map.Entry<String, Set<String>> e = iter.next();
            String cuid = e.getKey();
            Set<String> ts = e.getValue();
            tc[i] = new TransferCapability(cuid,
                    ts.toArray(new String[ts.size()]),
                    TransferCapability.SCU);
        }
        ae.setTransferCapability(tc);
    }

    public void start() throws IOException {
        if (conn.isListening()) {
            conn.bind(executor );
            System.out.println("Start Server listening on port " + conn.getPort());
        }
    }

    public void stop() {
        if (conn.isListening()) {
            try {
                Thread.sleep(shutdownDelay);
            } catch (InterruptedException e) {
                // Should not happen
                e.printStackTrace();
            }
            conn.unbind();
        }
    }

    public void open() throws IOException, ConfigurationException,
            InterruptedException {
        assoc = ae.connect(remoteAE, executor);
    }

    public void openToStgcmtAE() throws IOException, ConfigurationException,
            InterruptedException {
        assoc = ae.connect(remoteStgcmtAE, executor);
    }



    //------------------------------------------------------
	private boolean matches(BasicJob njob, DcmSnd snd){

	boolean found = false;

	try {
		 String[] data = njob.get_dcmsndMainStats();
		 String[] data_2 = snd.get_mainStats();

		 if((data[0].equals(data_2[0]))&&
		   ( data[1].equals(data_2[1]))&&
		   ( data[2].equals(data_2[2])))
		   {
			found = true;
		   }

	} catch (Exception e) {
			 found = false;
			 e.printStackTrace();
			 log_writer.doLogging_QRmgr(logRoot, logName, "<Dcmsnd>.doComparison() error: "+e);
	}
	return found;
	}
	//-----------------------------------------------------


    public void send() {

	  if(!this.get_ARhackStatus())
	    {
         for (int i = 0, n = files.size(); i < n; ++i) {
              FileInfo info = files.get(i);

            TransferCapability tc = assoc.getTransferCapabilityAsSCU(info.cuid);
            if (tc == null) {
                System.out.println();
                System.out.println(UIDDictionary.getDictionary().prompt(
                        info.cuid)
                        + " not supported by " + remoteAE.getAETitle());
                System.out.println("skip file " + info.f);
                continue;
            }
            String tsuid = selectTransferSyntax(tc.getTransferSyntax(),
                    fileref ? DCM4CHEE_URI_REFERENCED_TS_UID : info.tsuid);
            if (tsuid == null) {
                System.out.println();
                System.out.println(UIDDictionary.getDictionary().prompt(
                        info.cuid)
                        + " with "
                        + UIDDictionary.getDictionary().prompt(
                                fileref ? DCM4CHEE_URI_REFERENCED_TS_UID
                                        : info.tsuid)
                        + " not supported by " + remoteAE.getAETitle());
                System.out.println("skip file " + info.f);
                continue;
            }
            try {
                DimseRSPHandler rspHandler = new DimseRSPHandler() {
                    @Override
                    public void onDimseRSP(Association as, DicomObject cmd,
                            DicomObject data) {
                        DcmSnd.this.onDimseRSP(cmd);
                    }
                };

                assoc.cstore(info.cuid, info.iuid, priority,
                        new DataWriter(info), tsuid, rspHandler);
            } catch (NoPresentationContextException e) {
                System.err.println("WARNING: " + e.getMessage()
                        + " - cannot send " + info.f);
                System.out.print('F');
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("ERROR: Failed to send - " + info.f + ": "
                        + e.getMessage());
                System.out.print('F');
            } catch (InterruptedException e) {
                // should not happen
                e.printStackTrace();
            }

	       }//end for..

	    }
      else
	    {
         while(keepRetrieving.get())
          {
		    //--------------------------------------------------
		    System.out.println("reading from file...");

		    Object[] fileAndObject = this.takeFromStore();
		    String fname = null;
		    BasicJob nHawkJobObject = null;
		    clearanceManager clrMgr = null;

		    if(fileAndObject != null)
		      {
			   if(fileAndObject.length >= 5)
			     {
				  fname = (String) fileAndObject[3];
				  clrMgr = (clearanceManager) fileAndObject[4];
				 }
               else
                 {
			      if(fileAndObject[0] != null)
			        {
			         fname = (String) fileAndObject[0];
			        }
                  if(fileAndObject[1] != null)
			        {
			         nHawkJobObject = (BasicJob) fileAndObject[1];
			        }
			     }
			  }

		    if(fname.equals("END_IT"))
		      {
			   this.reQueueAll(); //just in case..
			   break;
			  }
		    else //continue..
		      {
			    boolean continueOp = true;

			    if(assoc.sockClosed)
			      {
                   continueOp = false;

                   if(clrMgr != null)
                     {
					  clrMgr.storeInQueue(fileAndObject);
					 }
                   else if(nHawkJobObject != null)
                     {
                      int currentValue = nHawkJobObject.currentRun.get();
				      Integer intObj = (Integer) fileAndObject[2];
				      int storedValue = intObj.intValue();

				      if(currentValue != storedValue)
				        {
					    }
				      else if((nHawkJobObject.get_status().equals("awaiting re-start"))||
				        (      nHawkJobObject.get_status().equals("awaiting download"))||
				        (      nHawkJobObject.get_status().equals("re-started"))||
                        (      nHawkJobObject.get_status().equals("cancelled"))||
                        (     (nHawkJobObject.get_status().indexOf("fail") >= 0)))
                        {
					    }
				      else if(nHawkJobObject.isRelegated())
				        {
						}
					  else
					    {
                         nHawkJobObject.storeInPendingQueue(fileAndObject);
					    }
				     }
				   this.reQueuedDueToError();
                   log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<Dcmsnd.sendFile() msg> Requeued all!");
                   break;
				  }

			    if((nHawkJobObject != null)&&(continueOp))
			      {
				   int currentValue = nHawkJobObject.currentRun.get();
				   Integer intObj = (Integer) fileAndObject[2];
				   int storedValue = intObj.intValue();

				   if(currentValue != storedValue)
				     {
					  continueOp = false;
					 }
				   else if((nHawkJobObject.get_status().equals("awaiting re-start"))||
				     (      nHawkJobObject.get_status().equals("awaiting download"))||
				     (      nHawkJobObject.get_status().equals("re-started"))||
                     (      nHawkJobObject.get_status().equals("cancelled"))||
                     (     (nHawkJobObject.get_status().indexOf("fail") >= 0)))
                     {
                      continueOp = false;
					 }
				   else if(nHawkJobObject.isRelegated())
				     {continueOp = false;}
				   else if((nHawkJobObject.get_status().equals("pending"))||
				     (      nHawkJobObject.get_status().equals("paused"))||
				     (      nHawkJobObject.get_status().equals("stopped"))||
				     (    !(nHawkJobObject.get_transferMode().equals("dcmsnd")))||
				     (    !(this.matches(nHawkJobObject, this))))
				     {
					  if((nHawkJobObject.get_pendingQueueObject().size()) >= nHawkJobObject.get_pendingQueueMaxSize())
					    {
						 //no space left...so let's send this.
						 log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<sendFile - hack> No space left, must run job: "+nHawkJobObject.get_jobId());
						}
					  else
					    {
					     nHawkJobObject.storeInPendingQueue(fileAndObject);
					     continueOp = false;
					    }
					 }
				  }

			    if(!(new File(fname).exists()))
			      {
                   log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<sendFile - hack...AR caught> file not found in system: "+fname);
                   continueOp = false;
                   //do some clean ups?
				  }

			    if(continueOp)
			      {
					File fileToAdd = new File(fname);

					if(fileToAdd == null){break;}

					Object[] data = this.setFile(fileToAdd);
					if(data == null)
					  {
					   log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<sendFile - hack> data returned is null.");
					  }
					else if((data[0] == null)||(data[1] == null)||(data[2] == null)||(data[3] == null))
					  {
					   log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<sendFile - hack> data objects returned are null.");
					  }
					else
					  {
						FileInfo info = (FileInfo) data[0];

						//--------------------------------------------------

						TransferCapability tc = assoc.getTransferCapabilityAsSCU(info.cuid);
						if (tc == null) {
							System.out.println();
							System.out.println(UIDDictionary.getDictionary().prompt(
							info.cuid)
							+ " not supported by " + remoteAE.getAETitle());
							System.out.println("skip file " + info.f);

					        //----------------------------------
					        log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<sendFile> File not supported, skip file " + info.f+", cuid: "+info.cuid);
					        new Thread(new stashAwayInFailedQueue(fileAndObject)).start();

					        //-----------------------------------

							continue;
						}
						String tsuid = selectTransferSyntax(tc.getTransferSyntax(),
								fileref ? DCM4CHEE_URI_REFERENCED_TS_UID : info.tsuid);
						if (tsuid == null) {
							System.out.println();
							System.out.println(UIDDictionary.getDictionary().prompt(
							info.cuid)
						    + " with "
							+ UIDDictionary.getDictionary().prompt(
							fileref ? DCM4CHEE_URI_REFERENCED_TS_UID
							: info.tsuid)
							+ " not supported by " + remoteAE.getAETitle());
							System.out.println("skip file " + info.f);

						   //----------------------------------
							 log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<sendFile> File(with) not supported, skip file " + info.f);

                             new Thread(new stashAwayInFailedQueue(fileAndObject)).start();
						   //-----------------------------------
							continue;
						}


						try {
							 DimseRSPHandler rspHandler = new DimseRSPHandler() {
								@Override
								public void onDimseRSP(Association as, DicomObject cmd,
										DicomObject data) {
									DcmSnd.this.onDimseRSP(cmd);
								}
							};

							assoc.cstore(info.cuid, info.iuid, priority, new DataWriter(info), tsuid, rspHandler);

							this.filePushedCounter++;


							//----------------------------------------
							//----------------------------------------
							//----------------------------------------
							//----------------------------------------

							alivenessValue.set(1);

							if(clrMgr != null)
							  {
							  //************************
							  // Update DB accordingly.
							  //************************
							  }
							else if(nHawkJobObject != null)
							  {
                               int cValue = nHawkJobObject.currentRun.get();
							   Integer intObj = (Integer) fileAndObject[2];
							   int sValue = intObj.intValue();

                               if((nHawkJobObject.get_status().equals("awaiting re-start"))||
                                 ( nHawkJobObject.get_status().equals("awaiting download"))||
                                 ( nHawkJobObject.get_status().equals("re-started"))||
                                 ( nHawkJobObject.get_status().equals("cancelled"))||
                                 ((nHawkJobObject.get_status().indexOf("fail") >= 0)))
                                 {}
                               else if(nHawkJobObject.isRelegated()){}
                               else if(cValue != sValue)
							   	 {}
                               else
                                 {
							      nHawkJobObject.increment_imagesUploaded(1);
							      nHawkJobObject.decrement_stored_moved(1);
							      nHawkJobObject.incrementBytesUploaded((new File(fileToAdd.getAbsolutePath())).length());
							      //-------------------------------------------------------
							      //log_writer.doLogging_QRmgr(logRoot, "transferredFileNames.txt", "<dcmsnd> msg: transferred file: " +fileToAdd.getAbsolutePath());
		                          //-------------------------------------------------------


		                          nHawkJobObject.incrementStoredCountForAlias(this.NODE_ALIAS, 1);

                                  Integer filecount = new Integer(this.filePushedCounter);
                                  //String currentTime = new Long(System.nanoTime()).toString();
                                  Object[] fileStats = new Object[3];
                                  fileStats[0] = filecount;
                                  //fileStats[1] = currentTime;
                                  fileStats[1] = dcm_folderCreator.get_currentDateAndTime();
                                  fileStats[2] = fileAndObject;
							      boolean stored = awaitingCStoreRSP.offer(fileStats);
							      if(!stored)
							        {
								     //--------------------------------------------------------------------------------------------------------------------------
									 log_writer.doLogging_QRmgr(logRoot, logName,
									 "<dcmsnd - send> msg: failed to saved file stats for " +fileToAdd.getAbsolutePath()+", job id: "+nHawkJobObject.get_jobId());
		                             //--------------------------------------------------------------------------------------------------------------------------
								    }
							     }
							  }

							//----------------------------------------
							//----------------------------------------
							//----------------------------------------
							//----------------------------------------



						} catch (NoPresentationContextException e) {
							     System.err.println("WARNING: " + e.getMessage()
								 + " - cannot send " + info.f);
					             //----------------------------------
						         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<sendFile> No presentation context: "+e);

                                 new Thread(new stashAwayInFailedQueue(fileAndObject)).start();
					             //-----------------------------------
							     System.out.print('F');
						} catch (IOException e) {
							     e.printStackTrace();
							     System.err.println("ERROR: Failed to send - " + info.f + ": "
							     + e.getMessage());

						         //----------------------------------
						         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<sendFile> IO exception: "+e);

                                 new Thread(new stashAwayInFailedQueue(fileAndObject)).start();
					             //-----------------------------------
							     System.out.print('F');
						} catch (InterruptedException e) {
							     // should not happen
							     e.printStackTrace();

					             //----------------------------------
						         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<sendFile> InterruptedException: "+e);

                                 new Thread(new stashAwayInFailedQueue(fileAndObject)).start();
					             //-----------------------------------

					    } catch (IllegalStateException illExcept){
					             //----------------------------------
								 log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<sendFile.illegalState> telerad caught: "+illExcept);

                                 new Thread(new stashAwayInFailedQueue(fileAndObject)).start();
								 //-----------------------------------
						         illExcept.printStackTrace();

						         this.reQueuedDueToError();

						} catch (Exception sndExcept){
						         //----------------------------------
								 log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<sendFile.sndExcept> telerad caught: "+sndExcept);

                                 new Thread(new stashAwayInFailedQueue(fileAndObject)).start();
					             //-----------------------------------
						         sndExcept.printStackTrace();

						         this.reQueuedDueToError();
						}

				  }//end else..

		    }//end else..if continueOp

	     } //end..continue..

        } //end while..

	    } //end else..

        try {
            assoc.waitForDimseRSP();
        } catch (InterruptedException e) {
            // should not happen
            e.printStackTrace();

            //----------------------------------
		    log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<waitForDimseRSP()> InterruptedException: "+e);
			//-----------------------------------
        }
    System.out.println(" Dcmsnd terminated!");
    //----------------------------------
	log_writer.doLogging_QRmgr(this.logRoot, this.logName, " Dcmsnd terminated!");
    //-----------------------------------
    }


    //----------------------------------------------------------------------------
    public void reQueueAll(){
	try {
		 synchronized(this.get_storeObject()){
		 keepDoingAliveness.set(false);
		 keepRetrieving.set(false);

		 while(this.fileLocations.size() > 0)
		   {
			Object[] fileAndObject = this.fileLocations.poll();
			if(fileAndObject != null)
			  {
			    if((fileAndObject[0] != null) && (fileAndObject[1] != null))
			      {
			       new Thread(new stashAwayInFailedQueue(fileAndObject)).start();
			      }
			  }
	       }
	      }
    } catch (Exception e){
	         e.printStackTrace();
	         log_writer.doLogging_QRmgr(this.logRoot, this.logName,
	         " Dcmsnd.reQueueAll() error: "+e);
	}finally {
	          keepDoingAliveness.set(false);
	          keepRetrieving.set(false);
	}
	}

	//--------------------------------------------------------------------------------
    public void reQueuedDueToError(){
	try {
		 synchronized(this.get_storeObject()){
		 keepDoingAliveness.set(false);
		 keepRetrieving.set(false);

		 while(this.fileLocations.size() > 0)
		   {
			Object[] fileAndObject = this.fileLocations.poll();
			if(fileAndObject != null)
			  {
			   if(fileAndObject.length >= 5)
			     {
				  new Thread(new stashAwayInFailedQueue(fileAndObject)).start();
				 }
			   else if((fileAndObject[0] != null) && (fileAndObject[1] != null))
			     {
   			      BasicJob nHawkJobObject = (BasicJob) fileAndObject[1];
				  int cValue = nHawkJobObject.currentRun.get();
				  Integer intObj = (Integer) fileAndObject[2];
				  int sValue = intObj.intValue();

				  if((nHawkJobObject.get_status().equals("awaiting re-start"))||
				    ( nHawkJobObject.get_status().equals("awaiting download"))||
				    ( nHawkJobObject.get_status().equals("re-started"))||
				    ( nHawkJobObject.get_status().equals("cancelled"))||
				    ((nHawkJobObject.get_status().indexOf("fail") >= 0)))
				    {}
				  else if(nHawkJobObject.isRelegated()){}
				  else if(cValue != sValue)
				    {}
				  else
				    {
                     new Thread(new stashAwayInFailedQueue(fileAndObject)).start();
                     String filename = (String) fileAndObject[0];
			         long fileLength = ((new File(filename)).getAbsolutePath()).length();
                     nHawkJobObject.noOfFilesRestacked.incrementAndGet();
                     nHawkJobObject.totalBytesRestacked += fileLength;

                     //===============================================================================================
                     //===============================================================================================
                       log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<reQueuedDueToError> restack error!: ");
                     //===============================================================================================
                     //===============================================================================================
				    }
			     }
			  }
	       }
	       }
    } catch (Exception e){
	         e.printStackTrace();
	         log_writer.doLogging_QRmgr(this.logRoot, this.logName,
	         " Dcmsnd.reQueuedDueToError() error: "+e);
	} finally {
	           keepDoingAliveness.set(false);
	           keepRetrieving.set(false);
	}
	}
	//--------------------------------------------------------------------------------


    public boolean commit() {
        DicomObject actionInfo = new BasicDicomObject();
        actionInfo.putString(Tag.TransactionUID, VR.UI, UIDUtils.createUID());
        DicomElement refSOPSq = actionInfo.putSequence(Tag.ReferencedSOPSequence);
        for (int i = 0, n = files.size(); i < n; ++i) {
            FileInfo info = files.get(i);
            if (info.transferred) {
                BasicDicomObject refSOP = new BasicDicomObject();
                refSOP.putString(Tag.ReferencedSOPClassUID, VR.UI, info.cuid);
                refSOP.putString(Tag.ReferencedSOPInstanceUID, VR.UI, info.iuid);
                refSOPSq.addDicomObject(refSOP);
            }
        }
        try {
            stgCmtResult = null;
            DimseRSP rsp = assoc.naction(UID.StorageCommitmentPushModelSOPClass,
                UID.StorageCommitmentPushModelSOPInstance, STG_CMT_ACTION_TYPE,
                actionInfo, UID.ImplicitVRLittleEndian);
            rsp.next();
            DicomObject cmd = rsp.getCommand();
            int status = cmd.getInt(Tag.Status);
            if (status == 0) {
                return true;
            }
            System.err.println(
                    "WARNING: Storage Commitment request failed with status: "
                    + StringUtils.shortToHex(status) + "H");
            System.err.println(cmd.toString());
        } catch (NoPresentationContextException e) {
            System.err.println("WARNING: " + e.getMessage()
                    + " - cannot request Storage Commitment");
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println(
                    "ERROR: Failed to send Storage Commitment request: "
                    + e.getMessage());
        } catch (InterruptedException e) {
            // should not happen
            e.printStackTrace();
        }
        return false;
    }

    private String selectTransferSyntax(String[] available, String tsuid) {
        if (tsuid.equals(UID.ImplicitVRLittleEndian))
            return selectTransferSyntax(available, IVLE_TS);
        if (tsuid.equals(UID.ExplicitVRLittleEndian))
            return selectTransferSyntax(available, EVLE_TS);
        if (tsuid.equals(UID.ExplicitVRBigEndian))
            return selectTransferSyntax(available, EVBE_TS);
        for (int j = 0; j < available.length; j++)
            if (available[j].equals(tsuid))
                return tsuid;
        return null;
    }

    private String selectTransferSyntax(String[] available, String[] tsuids) {
        for (int i = 0; i < tsuids.length; i++)
            for (int j = 0; j < available.length; j++)
                if (available[j].equals(tsuids[i]))
                    return available[j];
        return null;
    }

    public void close() {
        try {
            assoc.release(false);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static final class FileInfo {

        File f;

        String cuid;

        String iuid;

        String tsuid;

        long fmiEndPos;

        long length;

        boolean transferred;

        int status;

        public FileInfo(File f) {
            this.f = f;
            this.length = f.length();
        }

    }

    private class DataWriter implements org.dcm4che2.net.DataWriter {

        private FileInfo info;

        public DataWriter(FileInfo info) {
            this.info = info;
        }

        public void writeTo(PDVOutputStream out, String tsuid)
                throws IOException {
            if (tsuid.equals(info.tsuid)) {
                FileInputStream fis = new FileInputStream(info.f);
                try {
                    long skip = info.fmiEndPos;
                    while (skip > 0)
                        skip -= fis.skip(skip);
                    out.copyFrom(fis);
                } finally {
                    fis.close();
                }
            } else if (tsuid.equals(DCM4CHEE_URI_REFERENCED_TS_UID)) {
                DicomObject attrs;
                DicomInputStream dis = new DicomInputStream(info.f);
                try {
                    dis.setHandler(new StopTagInputHandler(Tag.PixelData));
                    attrs = dis.readDicomObject();
                } finally {
                    dis.close();
                }
                DicomOutputStream dos = new DicomOutputStream(out);
                attrs.putString(Tag.RetrieveURI, VR.UT, info.f.toURI().toString());
                dos.writeDataset(attrs, tsuid);
             } else {
                DicomInputStream dis = new DicomInputStream(info.f);
                try {
                    DicomOutputStream dos = new DicomOutputStream(out);
                    dos.setTransferSyntax(tsuid);
                    TranscoderInputHandler h = new TranscoderInputHandler(dos,
                            transcoderBufferSize);
                    dis.setHandler(h);
                    dis.readDicomObject();
                } finally {
                    dis.close();
                }
            }
        }

    }

    private void promptErrRSP(String prefix, int status, FileInfo info,
            DicomObject cmd) {
        System.err.println(prefix + StringUtils.shortToHex(status) + "H for "
                + info.f + ", cuid=" + info.cuid + ", tsuid=" + info.tsuid);
        System.err.println(cmd.toString());
    }


   private Object[] get_statsObject(Integer msgID){

   Object[] statsObj = null;
   try {
        if(awaitingCStoreRSP != null)
          {
		   Object[] allElements = awaitingCStoreRSP.toArray();
		   if(allElements != null)
		     {
			  for(int a = 0; a < allElements.length; a++)
			   {
			    Object[] data = (Object[]) allElements[a];
			    if(data != null)
			      {
                   Integer fileNo = (Integer) data[0];
                   if(fileNo.intValue() == msgID.intValue())
                     {
					  statsObj = data;
					  awaitingCStoreRSP.remove(data);
					  break;
					 }
				  }
			   }
			 }
		  }

   } catch (Exception e) {
	//----------------------------------
    log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<DcmSnd - get_statsObject> exception: "+e);
    //-----------------------------------
   }
   return statsObj;
   }




   private void onDimseRSP(DicomObject cmd) {
        int status = cmd.getInt(Tag.Status);
        int msgId = cmd.getInt(Tag.MessageIDBeingRespondedTo);

        //====================================
        //FileInfo info = files.get(msgId - 1); //real thing..

         FileInfo info = files.get(0); //modified..
        //====================================

	    Integer f_id = null; // new Integer(msgId);
		Object[] sObject = null; // new Object[2];
		boolean stored = false;

        info.status = status;
        switch (status) {
        case 0:
            info.transferred = true;
            totalSize += info.length;
            ++filesSent;
            System.out.print('.');

            if(this.get_ARhackStatus())
              {
               f_id = new Integer(msgId);
               sObject = new Object[2];
               sObject[0] = f_id;
               sObject[1] = dcm_folderCreator.get_currentDateAndTime();
               stored = CStoreRSPids.offer(sObject);
               if(!stored)
                 {
                  new Thread(new ridUnprocessedCStore(f_id)).start();
			     }
		      }

            break;
        case 0xB000:
        case 0xB006:
        case 0xB007:
            info.transferred = true;
            totalSize += info.length;
            ++filesSent;
            promptErrRSP("WARNING: Received RSP with Status ", status, info,
                    cmd);
            System.out.print('W');

            if(this.get_ARhackStatus())
              {
               f_id = new Integer(msgId);
               sObject = new Object[2];
               sObject[0] = f_id;
               sObject[1] = dcm_folderCreator.get_currentDateAndTime();
               stored = CStoreRSPids.offer(sObject);
               if(!stored)
                 {
                  new Thread(new ridUnprocessedCStore(f_id)).start();
			     }
		      }

            break;
        default:
            promptErrRSP("ERROR: Received RSP with Status ", status, info, cmd);
            System.out.print('F');
        }
    }



    @Override
    protected synchronized void onNEventReportRSP(Association as, int pcid,
            DicomObject rq, DicomObject info, DicomObject rsp) {
        stgCmtResult = info;
        notifyAll();
    }

    public void initTLS() throws GeneralSecurityException, IOException {
        KeyStore keyStore = loadKeyStore(keyStoreURL, keyStorePassword);
        KeyStore trustStore = loadKeyStore(trustStoreURL, trustStorePassword);
        device.initTLS(keyStore,
                keyPassword != null ? keyPassword : keyStorePassword,
                trustStore);
    }

    private static KeyStore loadKeyStore(String url, char[] password)
            throws GeneralSecurityException, IOException {
        KeyStore key = KeyStore.getInstance(toKeyStoreType(url));
        InputStream in = openFileOrURL(url);
        try {
            key.load(in, password);
        } finally {
            in.close();
        }
        return key;
    }

    private static InputStream openFileOrURL(String url) throws IOException {
        if (url.startsWith("resource:")) {
            return DcmSnd.class.getClassLoader().getResourceAsStream(
                    url.substring(9));
        }
        try {
            return new URL(url).openStream();
        } catch (MalformedURLException e) {
            return new FileInputStream(url);
        }
    }

    private static String toKeyStoreType(String fname) {
        return fname.endsWith(".p12") || fname.endsWith(".P12")
                 ? "PKCS12" : "JKS";
    }



	//---------------------------------------------------------


	 //Inner class...
	 class keepAliveClass implements Runnable {

	 private String keepAliveData = null;
	 private int sleepTime = -1;

	 public keepAliveClass(String keepAliveData, int sleepTime){

	 this.keepAliveData = keepAliveData;
	 this.sleepTime = sleepTime;
	 }

	 public void run(){

	 while(keepDoingAliveness.get())
	  {
	   try {
			if(alivenessValue.get() == 0)
			  {
			   Object[] fileAndObject = new Object[2];
			   fileAndObject[0] = (Object) this.keepAliveData;
			   fileAndObject[1] = null;
			   putInStore(fileAndObject);
		      }
		      alivenessValue.set(0);
		      this.sleepForDesiredTime(this.sleepTime);

	   } catch (Exception e){
				e.printStackTrace();
	   }
	  }
	 }

	 public void sleepForDesiredTime(int duration){

	 try {
		  Thread.sleep(duration);
	 } catch(InterruptedException ef) {
		  ef.printStackTrace();
	 }
	 }

	 } //end class..





   ///////////////////////////////////////////////////
   //////////////////////////////////////////////////
   class stashAwayInFailedQueue implements Runnable {

   private BasicJob nHawkJobObject = null;
   private Object[] fileAndObject = null;
   private String restackedFile = null;


   public stashAwayInFailedQueue(Object[] fileAndObject){

   this.fileAndObject = fileAndObject;
   }


   public void run(){

   try {

       if(fileAndObject.length >= 5)
         {
		  clearanceManager clrMgr = (clearanceManager) fileAndObject[4];
		  clrMgr.storeInQueue(fileAndObject);
		 }
       else
         {
		   String cuid = null;
		   this.restackedFile = (String) this.fileAndObject[0];
		   this.nHawkJobObject = (BasicJob) this.fileAndObject[1];

		   if(new File(this.restackedFile).exists())
			 {
			  cuid = new dicomFileParser().get_cuid(new File(this.restackedFile), logRoot, logName);
			 }

		   if(cuid != null){
		   if(itsPusher)
			 {
			  DcmSnd snd = pushObject.get_dcmsnd(cuid);
			  if(snd == null)
				{
				 log_writer.doLogging_QRmgr(logRoot, logName,
				 "<stashAwayInFailedQueue<<push>>.run() msg: Failed to find dcmsnd for "+restackedFile+", cuid: "+cuid+". file skipped!");
				}
			  else
				{
				 snd.putInStore(this.fileAndObject);
				}
			 }
		   else
			 {
			  if(this.nHawkJobObject != null)
				{
				  if((fileAndObject[0] != null) && (fileAndObject[1] != null))
					{
					 int cValue = nHawkJobObject.currentRun.get();
					 Integer intObj = (Integer) fileAndObject[2];
					 int sValue = intObj.intValue();

					 if((nHawkJobObject.get_status().equals("awaiting re-start"))||
					   ( nHawkJobObject.get_status().equals("awaiting download"))||
					   ( nHawkJobObject.get_status().equals("re-started"))||
					   ( nHawkJobObject.get_status().equals("cancelled"))||
					   ((nHawkJobObject.get_status().indexOf("fail") >= 0)))
					   {}
					 else if(nHawkJobObject.isRelegated()){}
					 else if(cValue != sValue)
					   {}
					 else
					   {
						this.nHawkJobObject.storeInPendingQueue(this.fileAndObject);
					   }
					}
				}
			  else
				{
				 log_writer.doLogging_QRmgr(logRoot, logName,
				 "<stashAwayInFailedQueue> msg: Night hawk object for skipped file " +this.restackedFile);
				}
			 }
			 }
	     }
   } catch (Exception e){
   	        e.printStackTrace();
   	        log_writer.doLogging_QRmgr(logRoot, logName,
   	        "<stashAwayInFailedQueue> msg: error in stashing away" +this.restackedFile+": "+e);
   }
   }
   }




   ///////////////////////////////////////////////
   // Processes received CSTORE RSP
   //////////////////////////////////////////////
   class processCStoreRSP implements Runnable {

   public processCStoreRSP(){}


   public void run(){

   while(true)
    {
	 try {
          Object[] stats = CStoreRSPids.take();
          if(stats != null)
            {
             Integer id = (Integer) stats[0];
             if(id != null)
               {
				Object[] statsObj = get_statsObject(id);
				if(statsObj != null)
				  {
				   String startTime = (String) statsObj[1];
				   String endTime = (String) stats[1];
				   Object[] fileObj = (Object[]) statsObj[2];
				   if(fileObj != null)
					 {
					  if(fileObj[1] != null)
						{
						 BasicJob jObject = (BasicJob) fileObj[1];
						 if(jObject != null)
						   {
							jObject.write_fileAnalysis(startTime, endTime, fileObj);
						   }
						}
					 }
				  }
		      }
		    }
     } catch (Exception e){
   	          e.printStackTrace();
   	          log_writer.doLogging_QRmgr(logRoot, logName,
   	          "<processCStoreRSP> run() error: "+e);
     }
	}
   }

   }




   ///////////////////////////////////////////////
   // Rid CSTORE RSP that could not be processed.
   //////////////////////////////////////////////
   class ridUnprocessedCStore implements Runnable {

   Integer id = null;

   public ridUnprocessedCStore(Integer id){

   this.id = id;
   }


   public void run(){

   try {
        Object[] data = get_statsObject(id);
        if(data == null)
          {
		   log_writer.doLogging_QRmgr(logRoot, logName,
   	          "<ridUnprocessedCStore> run() msg: ID "+id.intValue()+" not found!");
		  }

   } catch (Exception e){
   	          e.printStackTrace();
   	          log_writer.doLogging_QRmgr(logRoot, logName,
   	          "<ridUnprocessedCStore> run() error: "+e);
   }
   }
   }

    //------------------------------------------------------------------
}
