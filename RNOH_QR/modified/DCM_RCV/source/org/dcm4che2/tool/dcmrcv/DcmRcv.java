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
 * Michael Bassey <mabaze178@yaoo.com>
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

package org.dcm4che2.tool.dcmrcv;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.List;
import java.util.concurrent.Executor;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.UID;
import org.dcm4che2.io.DicomOutputStream;
import org.dcm4che2.net.Association;
import org.dcm4che2.net.CommandUtils;
import org.dcm4che2.net.Device;
import org.dcm4che2.net.DicomServiceException;
import org.dcm4che2.net.NetworkApplicationEntity;
import org.dcm4che2.net.NetworkConnection;
import org.dcm4che2.net.NewThreadExecutor;
import org.dcm4che2.net.PDVInputStream;
import org.dcm4che2.net.Status;
import org.dcm4che2.net.TransferCapability;
import org.dcm4che2.net.service.StorageService;
import org.dcm4che2.net.service.VerificationService;


import utility.general.DcmSnd_starter;
import org.dcm4che2.tool.dcmsnd.DcmSnd;
import org.dcm4che2.net.log_writer;
import night_hawk.nightHawkJob;
import night_hawk.jobDemographicsRetriever;
import common.manager.BasicManager;

import NH.NH_job;
import java.util.ArrayList;

import org.dcm4che2.io.DicomInputStream;
import org.dcm4che2.io.StopTagInputHandler;

/**
 * @author gunter zeilinger(gunterze@gmail.com)
 * @version $Revision: 6952 $ $Date: 2008-09-06 14:52:22 +0200 (Sat, 06 Sep 2008) $
 * @since Oct 13, 2005

 * On August 22, 2012, Michael Bassey & Edgar Luberenga made modifications to some sections of these code.
 * Modifications made were:
 * (1)  Addition of new import statements.
 * (2)  Declaration/addition of new variables.
 * (3)  Definition/addition of new methods.
 * (4)  Creation of a new method that borrows largely from an existent method.
 * (5)  Adding to an existent method.
 * (7)  Creation/addition of a new class.
 */

public class DcmRcv extends StorageService {

    private static final int KB = 1024;

    private static final String USAGE = "dcmrcv [Options] [<aet>[@<ip>]:]<port>";

    private static final String DESCRIPTION = "DICOM Server listening on specified <port> for incoming association "
            + "requests. If no local IP address of the network interface is specified "
            + "connections on any/all local addresses are accepted. If <aet> is "
            + "specified, only requests with matching called AE title will be "
            + "accepted.\n" + "Options:";

    private static final String EXAMPLE = "\nExample: dcmrcv DCMRCV:11112 -dest /tmp \n"
            + "=> Starts server listening on port 11112, accepting association "
            + "requests with DCMRCV as called AE title. Received objects "
            + "are stored to /tmp.";

    private static char[] SECRET = { 's', 'e', 'c', 'r', 'e', 't' };

    private static final String[] ONLY_DEF_TS = { UID.ImplicitVRLittleEndian };

    private static final String[] NATIVE_TS = { UID.ExplicitVRLittleEndian,
            UID.ExplicitVRBigEndian, UID.ImplicitVRLittleEndian };

    private static final String[] NATIVE_LE_TS = { UID.ExplicitVRLittleEndian,
            UID.ImplicitVRLittleEndian };

    private static final String[] NON_RETIRED_TS = { UID.JPEGLSLossless,
            UID.JPEGLossless, UID.JPEGLosslessNonHierarchical14,
            UID.JPEG2000LosslessOnly, UID.DeflatedExplicitVRLittleEndian,
            UID.RLELossless, UID.ExplicitVRLittleEndian,
            UID.ExplicitVRBigEndian, UID.ImplicitVRLittleEndian,
            UID.JPEGBaseline1, UID.JPEGExtended24, UID.JPEGLSLossyNearLossless,
            UID.JPEG2000, UID.MPEG2, };

    private static final String[] NON_RETIRED_LE_TS = { UID.JPEGLSLossless,
            UID.JPEGLossless, UID.JPEGLosslessNonHierarchical14,
            UID.JPEG2000LosslessOnly, UID.DeflatedExplicitVRLittleEndian,
            UID.RLELossless, UID.ExplicitVRLittleEndian,
            UID.ImplicitVRLittleEndian, UID.JPEGBaseline1, UID.JPEGExtended24,
            UID.JPEGLSLossyNearLossless, UID.JPEG2000, UID.MPEG2, };

    private static final String[] CUIDS = {
            UID.BasicStudyContentNotificationSOPClassRetired,
            UID.StoredPrintStorageSOPClassRetired,
            UID.HardcopyGrayscaleImageStorageSOPClassRetired,
            UID.HardcopyColorImageStorageSOPClassRetired,
            UID.ComputedRadiographyImageStorage,
            UID.DigitalXRayImageStorageForPresentation,
            UID.DigitalXRayImageStorageForProcessing,
            UID.DigitalMammographyXRayImageStorageForPresentation,
            UID.DigitalMammographyXRayImageStorageForProcessing,
            UID.DigitalIntraoralXRayImageStorageForPresentation,
            UID.DigitalIntraoralXRayImageStorageForProcessing,
            UID.StandaloneModalityLUTStorageRetired,
            UID.EncapsulatedPDFStorage, UID.StandaloneVOILUTStorageRetired,
            UID.GrayscaleSoftcopyPresentationStateStorageSOPClass,
            UID.ColorSoftcopyPresentationStateStorageSOPClass,
            UID.PseudoColorSoftcopyPresentationStateStorageSOPClass,
            UID.BlendingSoftcopyPresentationStateStorageSOPClass,
            UID.XRayAngiographicImageStorage, UID.EnhancedXAImageStorage,
            UID.XRayRadiofluoroscopicImageStorage, UID.EnhancedXRFImageStorage,
            UID.XRayAngiographicBiPlaneImageStorageRetired,
            UID.PositronEmissionTomographyImageStorage,
            UID.StandalonePETCurveStorageRetired, UID.CTImageStorage,
            UID.EnhancedCTImageStorage, UID.NuclearMedicineImageStorage,
            UID.UltrasoundMultiframeImageStorageRetired,
            UID.UltrasoundMultiframeImageStorage, UID.MRImageStorage,
            UID.EnhancedMRImageStorage, UID.MRSpectroscopyStorage,
            UID.RTImageStorage, UID.RTDoseStorage, UID.RTStructureSetStorage,
            UID.RTBeamsTreatmentRecordStorage, UID.RTPlanStorage,
            UID.RTBrachyTreatmentRecordStorage,
            UID.RTTreatmentSummaryRecordStorage,
            UID.NuclearMedicineImageStorageRetired,
            UID.UltrasoundImageStorageRetired, UID.UltrasoundImageStorage,
            UID.RawDataStorage, UID.SpatialRegistrationStorage,
            UID.SpatialFiducialsStorage, UID.RealWorldValueMappingStorage,
            UID.SecondaryCaptureImageStorage,
            UID.MultiframeSingleBitSecondaryCaptureImageStorage,
            UID.MultiframeGrayscaleByteSecondaryCaptureImageStorage,
            UID.MultiframeGrayscaleWordSecondaryCaptureImageStorage,
            UID.MultiframeTrueColorSecondaryCaptureImageStorage,
            UID.VLImageStorageTrialRetired, UID.VLEndoscopicImageStorage,
            UID.VideoEndoscopicImageStorage, UID.VLMicroscopicImageStorage,
            UID.VideoMicroscopicImageStorage,
            UID.VLSlideCoordinatesMicroscopicImageStorage,
            UID.VLPhotographicImageStorage, UID.VideoPhotographicImageStorage,
            UID.OphthalmicPhotography8BitImageStorage,
            UID.OphthalmicPhotography16BitImageStorage,
            UID.StereometricRelationshipStorage,
            UID.VLMultiframeImageStorageTrialRetired,
            UID.StandaloneOverlayStorageRetired, UID.BasicTextSRStorage,
            UID.EnhancedSRStorage, UID.ComprehensiveSRStorage,
            UID.ProcedureLogStorage, UID.MammographyCADSRStorage,
            UID.KeyObjectSelectionDocumentStorage,
            UID.ChestCADSRStorage, UID.StandaloneCurveStorageRetired,
            UID._12leadECGWaveformStorage, UID.GeneralECGWaveformStorage,
            UID.AmbulatoryECGWaveformStorage, UID.HemodynamicWaveformStorage,
            UID.CardiacElectrophysiologyWaveformStorage,
            UID.BasicVoiceAudioWaveformStorage, UID.HangingProtocolStorage,
            UID.SiemensCSANonImageStorage };

    private Executor executor = new NewThreadExecutor("DCMRCV");

    private Device device = new Device("DCMRCV");

    private NetworkApplicationEntity ae = new NetworkApplicationEntity();

    private NetworkConnection nc = new NetworkConnection();

    private String[] tsuids = NON_RETIRED_LE_TS;

    private File destination;

    private boolean devnull;

    private int fileBufferSize = 256;

    private int rspdelay = 0;

    private String keyStoreURL = "resource:tls/test_sys_2.p12";

    private char[] keyStorePassword = SECRET;

    private char[] keyPassword;

    private String trustStoreURL = "resource:tls/mesa_certs.jks";

    private char[] trustStorePassword = SECRET;

    //------------------------------------------------
    // AR added...
    private static boolean teleradHack = false;
    private static int fileNameCount = 1;
    private int asCreatedCount = 0;
    private BasicManager nHawkStarter = null;
    private static String imageLocation = null;
    private String logRoot = null;
    private String logName = null;
    private String processedFilesLocation = null;
    private int dataHandler_snoozing = -1;
    private int dataHandler_cannotMove = -1;
    private int dcmrcv_fileMoveMode = -1;
    private int dataHandler_queueSize = -1;
    private String file_extracts = null;
	private String extract_fileName = null;
	private String[] nightHawkTags = null;
	private String[] reAssignArgs_1 = null;
	private String[] reAssignArgs_2 = null;
	private int demograhphicRetrieval_maxAttempts = -1;
	private int demograhphicRetrieval_pauseInbetweenAttempts = -1;
	private int failedMoveQueueSize = -1;
	private int pendingQueueSize = -1;

	private ArrayList<NH_job> currentNHjobs = new ArrayList<NH_job>(20);
	private String TEL_MSG = null;
	private int maxWaitDuration = -1;



    //------------------------------------------------



    public DcmRcv() {
        super(CUIDS);
        device.setNetworkApplicationEntity(ae);
        device.setNetworkConnection(nc);
        ae.setNetworkConnection(nc);
        ae.setAssociationAcceptor(true);
        ae.register(new VerificationService());
        ae.register(this);
    }


    //-----------------------------------------------------------------------
    //AR added..

    public void set_TEL_MSG(String TEL_MSG)
    {
	 this.TEL_MSG = TEL_MSG;
	}

	public void set_maxWaitDuration(int maxWaitDuration)
	{
     this.maxWaitDuration = maxWaitDuration;
    }

    public int get_maxWaitDuration()
    {
	 return this.maxWaitDuration;
	}

	public String get_TEL_MSG()
	{
	 return this.TEL_MSG;
	}

    public NetworkConnection get_thisNC()
    {
	 return this.nc;
	}


    public void add_NHjob(NH_job newNHjob)
    {
	 this.currentNHjobs.add(newNHjob);
	}


	public void checkAndCreate_NHjob(String[] jobDets,
	                                 Association newAssoc)
	{
     try
     {
      synchronized(this.currentNHjobs)
       {
         String jobID = jobDets[0];
         NH_job nj = null;
		 int pointer = 0;
		 for(int a = 0; a < this.currentNHjobs.size(); a++)
		    {
			 NH_job liveJob = (NH_job) this.currentNHjobs.get(pointer);
			 if(liveJob == null)
			   {
			    this.currentNHjobs.remove(pointer);
			    //---------------------------------------------------------------------------
			    log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<dcmrcv.checkAndCreate_NHjob() msg: Job removed: "+jobID);
			    //---------------------------------------------------------------------------
			   }
			 else if(liveJob.taskCompleted())
			   {
			    this.currentNHjobs.remove(pointer);
			    //---------------------------------------------------------------------------
				log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<dcmrcv.checkAndCreate_NHjob() msg: Job removed: "+jobID);
			    //---------------------------------------------------------------------------
			   }
             else if((liveJob.get_jobId()).equals(jobID))
			   {
                nj = liveJob;
                break;
			   }
			 else
			   {
			    pointer++;
			   }
			}

		 if(nj == null)
		   {
		    String fLoc = DcmRcv.create_newFileLoc(jobID, this.logRoot, this.logName);
			if(fLoc == null)
			  {
			   log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<dcmrcv.checkAndCreate_NHjob() error: Failed to create file loc for job: "+jobID);
			  }
			else
			  {
			   nj = new NH_job(jobID, jobDets, this.logRoot, this.logName);
               nj.set_fileDes(fLoc);
               this.currentNHjobs.add(nj);


               //=====================================================
			   newAssoc.set_jobId(nj.get_jobId());
			   nightHawkJob nJob = new nightHawkJob(this.nHawkStarter,
													newAssoc.get_jobId(),
													nj.get_fileDest(),
													null,
													newAssoc,
													this.logRoot,
													this.logName,
													this.processedFilesLocation,
													this.failedMoveQueueSize,
													this.pendingQueueSize);
			   nJob.set_transferMode(this.nHawkStarter.get_currentTransferMode());
			   nJob.set_dcmsndMainStats(this.nHawkStarter.get_desiredDcmsnd(this.nHawkStarter.get_defaultDcmsnd()));
			   nJob.set_httpsMainStats(this.nHawkStarter.get_desiredHttps(this.nHawkStarter.get_defaultHttps()));

			   //----------------------------
			   nJob.set_nhJob(nj);
			   //----------------------------

			   String destValue = null;
			   String dest = nJob.get_transferMode();
			   if(dest.equalsIgnoreCase("dcmsnd"))
				 {
				  String[] data =  nJob.get_dcmsndMainStats();
				  destValue = this.nHawkStarter.getDestination(data[1], data[2]);
				 }
			   else if(dest.equalsIgnoreCase("https"))
				 {
				  String[] data =  nJob.get_httpsMainStats();
				  destValue = this.nHawkStarter.getDestination(data[0], data[1]);
				 }

			   this.nHawkStarter.do_dbUpdate(nJob,2, null, destValue, logRoot, logName);
			   newAssoc.set_nHawkObject(nJob);
               nJob.set_jobDemographics(nj.get_demographics());

               this.writeToDB(nJob, this.nHawkStarter, nj);
	           this.nHawkStarter.storeInQueue_arrivingJob(nJob);
               //=====================================================
			  }
		   }

		 if(nj != null)
		   {
		    nj.add_association(newAssoc);
		    newAssoc.set_NHjob(nj);
		    newAssoc.set_NHjobId(nj.get_jobId());
		    newAssoc.set_NHfileLoc(nj.get_fileDest());
		   }


	   }

     } catch (Exception e){
		     e.printStackTrace();
      //-------------------------------------------------------------
	   log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<dcmrcv.checkAndCreate_NHjob() error: "+e);
	  //-------------------------------------------------------------
	 }
	}




public void writeToDB(nightHawkJob nJob, BasicManager nHawkStarter, NH_job nj){
try {
	 String jobKey = nJob.get_jobId();
	 //String[] db_args = new String[(nj.retrievedDemographics.length) + 6];

	 String[] db_args = new String[((nj.get_demographics()).length) + 6];

	 db_args[0] = jobKey;

	 String destValue = null;
	 String dest = nJob.get_transferMode();
	 if(dest.equalsIgnoreCase("dcmsnd"))
	   {
	 	String[] data =  nJob.get_dcmsndMainStats();
	 	destValue = nHawkStarter.getDestination(data[1], data[2]);
	   }
	 else if(dest.equalsIgnoreCase("https"))
	   {
	    String[] data =  nJob.get_httpsMainStats();
	    destValue = nHawkStarter.getDestination(data[0], data[1]);
	   }

     nJob.set_status("queued");

	 db_args[db_args.length -5] = nJob.get_status();
	 db_args[db_args.length -4] = "<html><i>downloaded</i></html>";
	 db_args[db_args.length -3] = Integer.toString(nJob.get_imagesUploaded());
	 db_args[db_args.length -2] = "1";
	 db_args[db_args.length -1] = "0";

	 String[] demoGraph = nj.get_demographics();

	 for(int a = 0; a < ((nj.get_demographics()).length); a++)
	    {
		 db_args[a + 1] = demoGraph[a];
	    }


     Object[] args = new Object[4];
     Integer opType = new Integer(4);

     //================================
     for(int r = 0; r < db_args.length; r++)
        {
	     if(db_args[r] == null)
	       {
		    db_args[r] = ".";
		   }
	     else if((db_args[r].equals(""))||
	             (db_args[r].equals(" "))||
	             (db_args[r].equals("null")))
	       {
		    db_args[r] = ".";
		   }
	    }
     //================================

     args[0] = opType;
	 args[1] = nHawkStarter;
	 args[2] = db_args;
	 args[3] = destValue;
	 nHawkStarter.get_dbUpdateMgr().storeData(args);


	nJob.insertDataIntoJobHistoryTable();
	nJob.start_jobHistoryUpdates();

 } catch (Exception e){
          e.printStackTrace();
          log_writer.doLogging_QRmgr(this.logRoot, this.logName,
          "DcmRcv.writeToDB(), night hawk job<"+nJob.get_jobId()+">  error: "+e);
 }
}



    public void set_stats(Object[] stats){

    try {

		this.nHawkStarter = (BasicManager) stats[0];
		DcmRcv.imageLocation = (String) stats[1];
		this.logRoot = (String) stats[2];
		this.logName = (String) stats[3];
		this.processedFilesLocation = (String) stats[4];
		this.dataHandler_snoozing = Integer.parseInt((String) stats[5]);
		this.dataHandler_cannotMove = Integer.parseInt((String) stats[6]);
		this.file_extracts = (String) stats[7];
        this.extract_fileName = (String) stats[8];
        this.nightHawkTags = (String[]) stats[9];

        Object[][] inc_args = (Object[][]) stats[10];

        this.reAssignArgs_1 = (String[]) inc_args[0][0];
        this.reAssignArgs_2 = (String[]) inc_args[0][1];

        this.demograhphicRetrieval_maxAttempts = Integer.parseInt((String) stats[11]);
        this.demograhphicRetrieval_pauseInbetweenAttempts = Integer.parseInt((String) stats[12]);
        this.failedMoveQueueSize = Integer.parseInt((String) stats[13]);
        this.pendingQueueSize = Integer.parseInt((String) stats[14]);

    } catch (Exception e){
		     e.printStackTrace();
    //-------------------------------------------------------------
	log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<dcmrcv.set_stats() error: "+e);
	//-------------------------------------------------------------
	}
	}


    public static synchronized String get_uniqueFileName(){

    String newname = null;
    try {
	     newname = ""+System.currentTimeMillis()+"_"+DcmRcv.fileNameCount;
	     DcmRcv.fileNameCount++;
	} catch (Exception e){
	         e.printStackTrace();
	         newname = null;
	}
	return newname;
    }


    public static boolean get_teleradHackStatus(){return DcmRcv.teleradHack;}

    public static String get_newObjectID(){


		String newID = null;
		try {
			 newID = dcm_folderCreator.createNameAndFolder_imageStore(DcmRcv.imageLocation);

		} catch (Exception e){
				 newID = null;
				 e.printStackTrace();
		}
	    return newID;
	}


     public static String create_newFileLoc(String nJobID, String logRoot, String logName){


		String newID = null;
		try {
			 newID = dcm_folderCreator.createNameAndFolder_imageStore(DcmRcv.imageLocation);
			 if(newID != null)
			   {
			    newID = newID+"/"+nJobID+"/";
			    newID = utility.general.genericJobs.prepareFilePath(newID, logRoot, logName);
			    if(!utility.general.genericJobs.createExactlyThatLocation(newID))
				  {
				   log_writer.doLogging_QRmgr(logRoot, logName, "<dcmrcv.create_newFileLoc() error: Failed to create location "+newID);
				   newID = null;
				  }
			   }

		} catch (Exception e){
				 newID = null;
				 e.printStackTrace();
		}
	    return newID;
	}


	public static void sleepForDesiredTime(int duration){

	try {
		 Thread.sleep(duration);
	} catch(InterruptedException ef) {
		    ef.printStackTrace();
	}
	}
    //-----------------------------------------------------------------------

    public final void setAEtitle(String aet) {
        ae.setAETitle(aet);
    }

    public final void setHostname(String hostname) {
        nc.setHostname(hostname);
    }

    public final void setPort(int port) {
        nc.setPort(port);
    }

    public final void setTlsWithoutEncyrption() {
        nc.setTlsWithoutEncyrption();
    }

    public final void setTls3DES_EDE_CBC() {
        nc.setTls3DES_EDE_CBC();
    }

    public final void setTlsAES_128_CBC() {
        nc.setTlsAES_128_CBC();
    }

    public final void disableSSLv2Hello() {
        nc.disableSSLv2Hello();
    }

    public final void setTlsNeedClientAuth(boolean needClientAuth) {
        nc.setTlsNeedClientAuth(needClientAuth);
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

    public final void setPackPDV(boolean packPDV) {
        ae.setPackPDV(packPDV);
    }

    public final void setAssociationReaperPeriod(int period) {
        device.setAssociationReaperPeriod(period);
    }

    public final void setTcpNoDelay(boolean tcpNoDelay) {
        nc.setTcpNoDelay(tcpNoDelay);
    }

    public final void setRequestTimeout(int timeout) {
        nc.setRequestTimeout(timeout);
    }

    public final void setReleaseTimeout(int timeout) {
        nc.setReleaseTimeout(timeout);
    }

    public final void setSocketCloseDelay(int delay) {
        nc.setSocketCloseDelay(delay);
    }

    public final void setIdleTimeout(int timeout) {
        ae.setIdleTimeout(timeout);
    }

    public final void setDimseRspTimeout(int timeout) {
        ae.setDimseRspTimeout(timeout);
    }

    public final void setMaxPDULengthSend(int maxLength) {
        ae.setMaxPDULengthSend(maxLength);
    }

    public void setMaxPDULengthReceive(int maxLength) {
        ae.setMaxPDULengthReceive(maxLength);
    }

    public final void setReceiveBufferSize(int bufferSize) {
        nc.setReceiveBufferSize(bufferSize);
    }

    public final void setSendBufferSize(int bufferSize) {
        nc.setSendBufferSize(bufferSize);
    }

    public void setDimseRspDelay(int delay) {
        rspdelay = delay;
    }

    private static CommandLine parse(String[] args) {

        Options opts = new Options();

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

        OptionBuilder.withArgName("dir");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription(
                "store received objects into files in specified directory <dir>."
                        + " Do not store received objects by default.");
        opts.addOption(OptionBuilder.create("dest"));

        opts.addOption("defts", false, "accept only default transfer syntax.");
        opts.addOption("bigendian", false,
                "accept also Explict VR Big Endian transfer syntax.");
        opts.addOption("native", false,
                "accept only transfer syntax with uncompressed pixel data.");

        OptionBuilder.withArgName("maxops");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription(
                "maximum number of outstanding operations performed "
                        + "asynchronously, unlimited by default.");
        opts.addOption(OptionBuilder.create("async"));

        opts.addOption("pdv1", false, "send only one PDV in one P-Data-TF PDU, "
                + "pack command and data PDV in one P-DATA-TF PDU by default.");
        opts.addOption("tcpdelay", false,
                "set TCP_NODELAY socket option to false, true by default");

        OptionBuilder.withArgName("ms");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription(
                "delay in ms for Socket close after sending A-ABORT, 50ms by default");
        opts.addOption(OptionBuilder.create("soclosedelay"));

        OptionBuilder.withArgName("ms");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription(
                "delay in ms for DIMSE-RSP; useful for testing asynchronous mode");
        opts.addOption(OptionBuilder.create("rspdelay"));

        OptionBuilder.withArgName("ms");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription(
                "timeout in ms for receiving -ASSOCIATE-RQ, 5s by default");
        opts.addOption(OptionBuilder.create("requestTO"));

        OptionBuilder.withArgName("ms");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription(
                "timeout in ms for receiving A-RELEASE-RP, 5s by default");
        opts.addOption(OptionBuilder.create("releaseTO"));

        OptionBuilder.withArgName("ms");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription(
                "period in ms to check for outstanding DIMSE-RSP, 10s by default");
        opts.addOption(OptionBuilder.create("reaper"));

        OptionBuilder.withArgName("ms");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription(
                "timeout in ms for receiving DIMSE-RQ, 60s by default");
        opts.addOption(OptionBuilder.create("idleTO"));

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
                "minimal buffer size to write received object to file, 1KB by default");
        opts.addOption(OptionBuilder.create("bufsize"));

        opts.addOption("h", "help", false, "print this message");
        opts.addOption("V", "version", false,
                "print the version information and exit");
        CommandLine cl = null;
        try {
            cl = new GnuParser().parse(opts, args);
        } catch (ParseException e) {
            exit("dcmrcv: " + e.getMessage());
            throw new RuntimeException("unreachable");
        }
        if (cl.hasOption("V")) {
            Package p = DcmRcv.class.getPackage();
            System.out.println("dcmrcv v" + p.getImplementationVersion());
            System.exit(0);
        }
        if (cl.hasOption("h") || cl.getArgList().size() == 0) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(USAGE, DESCRIPTION, opts, EXAMPLE);
            System.exit(0);
        }
        return cl;
    }

    public void startOp(String[] args, DcmRcv dcmrcv) {


        //----------------------------------------------
 		System.out.println("DcmRcv args length: "+args.length);
		for(int a = 0; a < args.length; a++)
		   {
		    System.out.println("DcmRcv args["+a+"]: "+args[a]);
		   }

		if((args[args.length - 1]).equals("teleradhack"))
		  {
		   DcmRcv.teleradHack = true;
		   String[] rebuild = new String[(args.length) - 1];
		   for(int a = 0; a < rebuild.length; a++)
			  {
			   rebuild[a] = args[a];
			  }
		   args = rebuild;

           //==============================================
		   NetworkConnection thisNC = dcmrcv.get_thisNC();
		   thisNC.setDCMRCV_status(true);
           thisNC.set_DCMRCVobject(dcmrcv);
           //=============================================
		  }
		//------------------------------------------------



        CommandLine cl = parse(args);

        //DcmRcv dcmrcv = new DcmRcv();

        final List argList = cl.getArgList();
        String port = (String) argList.get(0);
        String[] aetPort = split(port, ':', 1);
        dcmrcv.setPort(parseInt(aetPort[1], "illegal port number", 1, 0xffff));
        if (aetPort[0] != null) {
            String[] aetHost = split(aetPort[0], '@', 0);
            dcmrcv.setAEtitle(aetHost[0]);
            if (aetHost[1] != null) {
                dcmrcv.setHostname(aetHost[1]);
            }
        }

        if (cl.hasOption("dest"))
            dcmrcv.setDestination(cl.getOptionValue("dest"));
        if (cl.hasOption("defts"))
            dcmrcv.setTransferSyntax(ONLY_DEF_TS);
        else if (cl.hasOption("native"))
            dcmrcv.setTransferSyntax(cl.hasOption("bigendian") ? NATIVE_TS
                    : NATIVE_LE_TS);
        else if (cl.hasOption("bigendian"))
            dcmrcv.setTransferSyntax(NON_RETIRED_TS);
        if (cl.hasOption("reaper"))
            dcmrcv.setAssociationReaperPeriod(parseInt(cl
                            .getOptionValue("reaper"),
                            "illegal argument of option -reaper", 1,
                            Integer.MAX_VALUE));
        if (cl.hasOption("idleTO"))
            dcmrcv.setIdleTimeout(parseInt(cl.getOptionValue("idleTO"),
                            "illegal argument of option -idleTO", 1,
                            Integer.MAX_VALUE));
        if (cl.hasOption("requestTO"))
            dcmrcv.setRequestTimeout(parseInt(cl.getOptionValue("requestTO"),
                    "illegal argument of option -requestTO", 1,
                    Integer.MAX_VALUE));
        if (cl.hasOption("releaseTO"))
            dcmrcv.setReleaseTimeout(parseInt(cl.getOptionValue("releaseTO"),
                    "illegal argument of option -releaseTO", 1,
                    Integer.MAX_VALUE));
        if (cl.hasOption("soclosedelay"))
            dcmrcv.setSocketCloseDelay(parseInt(cl
                    .getOptionValue("soclosedelay"),
                    "illegal argument of option -soclosedelay", 1, 10000));
        if (cl.hasOption("rspdelay"))
            dcmrcv.setDimseRspDelay(parseInt(cl.getOptionValue("rspdelay"),
                    "illegal argument of option -rspdelay", 0, 10000));
        if (cl.hasOption("rcvpdulen"))
            dcmrcv.setMaxPDULengthReceive(parseInt(cl
                    .getOptionValue("rcvpdulen"),
                    "illegal argument of option -rcvpdulen", 1, 10000)
                    * KB);
        if (cl.hasOption("sndpdulen"))
            dcmrcv.setMaxPDULengthSend(parseInt(cl.getOptionValue("sndpdulen"),
                    "illegal argument of option -sndpdulen", 1, 10000)
                    * KB);
        if (cl.hasOption("sosndbuf"))
            dcmrcv.setSendBufferSize(parseInt(cl.getOptionValue("sosndbuf"),
                    "illegal argument of option -sosndbuf", 1, 10000)
                    * KB);
        if (cl.hasOption("sorcvbuf"))
            dcmrcv.setReceiveBufferSize(parseInt(cl.getOptionValue("sorcvbuf"),
                    "illegal argument of option -sorcvbuf", 1, 10000)
                    * KB);
        if (cl.hasOption("bufsize"))
            dcmrcv.setFileBufferSize(parseInt(cl.getOptionValue("bufsize"),
                    "illegal argument of option -bufsize", 1, 10000)
                    * KB);

        dcmrcv.setPackPDV(!cl.hasOption("pdv1"));
        dcmrcv.setTcpNoDelay(!cl.hasOption("tcpdelay"));
        if (cl.hasOption("async"))
            dcmrcv.setMaxOpsPerformed(parseInt(cl.getOptionValue("async"),
                    "illegal argument of option -async", 0, 0xffff));
        dcmrcv.initTransferCapability();
        if (cl.hasOption("tls")) {
            String cipher = cl.getOptionValue("tls");
            if ("NULL".equalsIgnoreCase(cipher)) {
                dcmrcv.setTlsWithoutEncyrption();
            } else if ("3DES".equalsIgnoreCase(cipher)) {
                dcmrcv.setTls3DES_EDE_CBC();
            } else if ("AES".equalsIgnoreCase(cipher)) {
                dcmrcv.setTlsAES_128_CBC();
            } else {
                exit("Invalid parameter for option -tls: " + cipher);
            }
            if (cl.hasOption("nossl2")) {
                dcmrcv.disableSSLv2Hello();
            }
            dcmrcv.setTlsNeedClientAuth(!cl.hasOption("noclientauth"));

            if (cl.hasOption("keystore")) {
                dcmrcv.setKeyStoreURL(cl.getOptionValue("keystore"));
            }
            if (cl.hasOption("keystorepw")) {
                dcmrcv.setKeyStorePassword(
                        cl.getOptionValue("keystorepw"));
            }
            if (cl.hasOption("keypw")) {
                dcmrcv.setKeyPassword(cl.getOptionValue("keypw"));
            }
            if (cl.hasOption("truststore")) {
                dcmrcv.setTrustStoreURL(
                        cl.getOptionValue("truststore"));
            }
            if (cl.hasOption("truststorepw")) {
                dcmrcv.setTrustStorePassword(
                        cl.getOptionValue("truststorepw"));
            }
            try {
                dcmrcv.initTLS();
            } catch (Exception e) {
                System.err.println("ERROR: Failed to initialize TLS context:"
                        + e.getMessage());
                System.exit(2);
            }
        }
        try {
            dcmrcv.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void setTransferSyntax(String[] tsuids) {
        this.tsuids = tsuids;
    }

    public void initTransferCapability() {
        TransferCapability[] tc = new TransferCapability[CUIDS.length + 1];
        tc[0] = new TransferCapability(UID.VerificationSOPClass, ONLY_DEF_TS,
                TransferCapability.SCP);
        for (int i = 0; i < CUIDS.length; i++)
            tc[i + 1] = new TransferCapability(CUIDS[i], tsuids,
                    TransferCapability.SCP);
        ae.setTransferCapability(tc);
    }

    public void setFileBufferSize(int size) {
        fileBufferSize = size;
    }

    public void setMaxOpsPerformed(int maxOps) {
        ae.setMaxOpsPerformed(maxOps);
    }

    public void setDestination(String filePath) {
        this.destination = new File(filePath);
        this.devnull = "/dev/null".equals(filePath);
        if (!devnull)
            destination.mkdir();
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
            return DcmRcv.class.getClassLoader().getResourceAsStream(
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

    public void start() throws IOException {
        device.startListening(executor);
        System.out.println("Start Server listening on port " + nc.getPort());
    }

    public void stop() {
        if (device != null)
            device.stopListening();

        if (nc != null)
            System.out.println("Stop Server listening on port " + nc.getPort());
        else
            System.out.println("Stop Server");
    }

    private static String[] split(String s, char delim, int defPos) {
        String[] s2 = new String[2];
        s2[defPos] = s;
        int pos = s.indexOf(delim);
        if (pos != -1) {
            s2[0] = s.substring(0, pos);
            s2[1] = s.substring(pos + 1);
        }
        return s2;
    }

    private static void exit(String msg) {
        System.err.println(msg);
        System.err.println("Try 'dcmrcv -h' for more information.");
        System.exit(1);
    }

    private static int parseInt(String s, String errPrompt, int min, int max) {
        try {
            int i = Integer.parseInt(s);
            if (i >= min && i <= max)
                return i;
        } catch (NumberFormatException e) {
            // parameter is not a valid integer; fall through to exit
        }
        exit(errPrompt);
        throw new RuntimeException();
    }

    /** Overwrite {@link StorageService#cstore} to send delayed C-STORE RSP
     * by separate Thread, so reading of following received C-STORE RQs from
     * the open association is not blocked.
     */
    @Override
    public void cstore(final Association as, final int pcid, DicomObject rq,
            PDVInputStream dataStream, String tsuid)
            throws DicomServiceException, IOException {
        final DicomObject rsp = CommandUtils.mkRSP(rq, CommandUtils.SUCCESS);
        onCStoreRQ(as, pcid, rq, dataStream, tsuid, rsp);

        if (rspdelay > 0) {
            executor.execute(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(rspdelay);
                        as.writeDimseRSP(pcid, rsp);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } else {
            as.writeDimseRSP(pcid, rsp);
        }
        onCStoreRSP(as, pcid, rq, dataStream, tsuid, rsp);
    }



    @Override
    protected void onCStoreRQ(Association as, int pcid, DicomObject rq,
            PDVInputStream dataStream, String tsuid, DicomObject rsp)
            throws IOException, DicomServiceException {
        if (destination == null) {
            super.onCStoreRQ(as, pcid, rq, dataStream, tsuid, rsp);
        } else {
            try {
                 String cuid = rq.getString(Tag.AffectedSOPClassUID);
				 String iuid = rq.getString(Tag.AffectedSOPInstanceUID);
				 BasicDicomObject fmi = null;
				 File file = null;

				 if(iuid.equals(BasicManager.aliveFileIuid))
				   {

				    //------------------------------------------------------------------------------------
				    //log_writer.doLogging_QRmgr("C:/", "NH_logs_2.txt", "aliveness file =>> "+BasicManager.aliveFileIuid+", iuid =>> "+iuid);
				    //------------------------------------------------------------------------------------
				   }
				 else
				   {
					if(DcmRcv.teleradHack)
					  {
					   NH_job nhJob = as.get_NHjob();
					   if(nhJob == null)
						 {
						  //-----------------------------------------------------------------------------------------------------------
						  log_writer.doLogging_QRmgr(this.logRoot, this.logName, "Null NH_job for association. Should never happen! ");
						  //-----------------------------------------------------------------------------------------------------------
						 }
						else
						 {
						  String fileLocation = nhJob.get_fileDest();
						  String newFileName = fileLocation+"/"+iuid;
						  newFileName = utility.general.genericJobs.prepareFilePath(newFileName, this.logRoot, this.logName);
						  file = new File(newFileName);
						  fmi = new BasicDicomObject();
						  fmi.initFileMetaInformation(cuid, iuid, tsuid);
						  nhJob.incrementFileCounter();
						 }
						}
					  else
						{
						 fmi = new BasicDicomObject();
						 fmi.initFileMetaInformation(cuid, iuid, tsuid);
						 file = devnull ? destination : new File(destination, iuid);
						}


						 FileOutputStream fos = new FileOutputStream(file);
						 BufferedOutputStream bos = new BufferedOutputStream(fos,fileBufferSize);
						 DicomOutputStream dos = new DicomOutputStream(bos);
						 dos.writeFileMetaInformation(fmi);
						 dataStream.copyTo(dos);
						 dos.close();


				    //------------------------------------------------------------------------------------
				    //log_writer.doLogging_QRmgr("C:/", "NH_logs_3.txt", "aliveness file =>> "+BasicManager.aliveFileIuid+", iuid =>> "+iuid);
				    //------------------------------------------------------------------------------------

				    }//end else..


            } catch (IOException e) {
                     //-------------------------------------------------------------
					 log_writer.doLogging_QRmgr(this.logRoot, this.logName, "=== DCMRCV SEND ERROR: "+e);
		             //-------------------------------------------------------------

                throw new DicomServiceException(rq, Status.ProcessingFailure, e.getMessage());
            }
        }
    }

}
