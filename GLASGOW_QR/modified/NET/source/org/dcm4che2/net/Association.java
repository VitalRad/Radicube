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
 * Damien Evans <damien.daddy@gmail.com>
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

package org.dcm4che2.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.UID;
import org.dcm4che2.data.UIDDictionary;
import org.dcm4che2.data.VR;
import org.dcm4che2.net.pdu.AAbort;
import org.dcm4che2.net.pdu.AAssociateAC;
import org.dcm4che2.net.pdu.AAssociateRJ;
import org.dcm4che2.net.pdu.AAssociateRQ;
import org.dcm4che2.net.pdu.ExtendedNegotiation;
import org.dcm4che2.net.pdu.PresentationContext;
import org.dcm4che2.net.pdu.RoleSelection;
import org.dcm4che2.util.CloseUtils;
import org.dcm4che2.util.IntHashtable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//-------------------------------------------
import org.dcm4che2.tool.dcmrcv.DcmRcv;
import org.dcm4che2.net.log_writer;
import night_hawk.nightHawkJob;
import NH.NH_job;
//-------------------------------------------

/**
 * @author gunter zeilinger(gunterze@gmail.com)
 * @version $Revision: 7256 $ $Date: 2008-09-30 00:36:35 +0200 (Tue, 30 Sep 2008) $
 * @since Nov 25, 2005
 *
 *  On July 01, 2012, Michael Bassey & Edgar Luberenga made additions to sections of this code.
 *  The additions made were:
 * (1) Addition of new import statements.
 * (2) Creation/addition of new variables.
 * (3) Definition/addition of new methods
 *
 */
public class Association implements Runnable {
    static Logger log = LoggerFactory.getLogger(Association.class);

    static int nextSerialNo = 0;

    private final int serialNo = ++nextSerialNo;

    private final NetworkConnection connector;

    private final AssociationReaper reaper;

    private NetworkApplicationEntity ae;

    private UserIdentity userIdentity;

    private Socket socket;

    private boolean requestor;

    private InputStream in;

    private OutputStream out;

    private PDUEncoder encoder;

    private PDUDecoder decoder;

    private State state;

    private String name = "Association(" + serialNo + ")";

    private AAssociateRQ associateRQ;

    private AAssociateAC associateAC;

    private IOException exception;

    private int maxOpsInvoked;

    private int maxPDULength;

    private int msgID;

    private int performing;

    private boolean closed;

    private IntHashtable<DimseRSPHandler> rspHandlerForMsgId = new IntHashtable<DimseRSPHandler>();

    private IntHashtable<DimseRSP> cancelHandlerForMsgId = new IntHashtable<DimseRSP>();

    private HashMap<String, Map<String, PresentationContext>> acceptedPCs = new HashMap<String, Map<String, PresentationContext>>();

    private HashMap<String, TransferCapability> scuTCs = new HashMap<String, TransferCapability>();

    private HashMap<String, TransferCapability> scpTCs = new HashMap<String, TransferCapability>();

    private long idleTimeout = Long.MAX_VALUE;


    //---------------------------------------------------------------
    private String AR_uid = null;
    private String job_id = null;
    private nightHawkJob nJob = null;
    public boolean sockClosed = false;
    private String NHfileLoc = null;
    private String NHjobId = null;

    private DcmRcv dcmrcvObject = null;
    private String jobIdToCheckFor = null;
    private NH_job masterJob = null;
    //---------------------------------------------------------------

    protected Association(Socket socket, NetworkConnection connector,
            boolean requestor) throws IOException {

        if (socket == null)
            throw new NullPointerException("socket");
        if (connector == null)
            throw new NullPointerException("connector");
        this.connector = connector;
        this.reaper = connector.getDevice().getAssociationReaper();
        this.socket = socket;
        this.requestor = requestor;
        this.in = socket.getInputStream();
        this.out = socket.getOutputStream();
        this.encoder = new PDUEncoder(this, out);
        this.state = State.STA1;
        log
                .info(requestor ? "{} initiated {}" : "{} accepted {}", name,
                        socket);
    }

    @Override
    public String toString() {
        return name;
    }


    //-----------------------------------------------------------------
    // AR added...
    public String get_ARuid(){return this.AR_uid;}

    public void set_ARuid(){this.AR_uid = DcmRcv.get_newObjectID();}

	public void set_jobId(String value){

	this.job_id = value;
	}

	public String get_jobId(){return this.job_id;}


	public void set_nHawkObject(nightHawkJob nJob){

	this.nJob = nJob;
	}


	public nightHawkJob get_nHawkObject(){

	return this.nJob;
    }

    public void set_NHfileLoc(String fileLoc)
    {
	 this.NHfileLoc = fileLoc;
	}


    public void set_NHjobId(String NHjobId)
    {
	 this.NHjobId = NHjobId;
	}


	public void set_NHjob(NH_job job)
	{
	 this.masterJob = job;
	}

	public NH_job get_NHjob(){return this.masterJob;}
    public String get_NHfileLoc(){return this.NHfileLoc;}
    public String get_NHjobId(){return this.NHjobId;}

    public void setArgsForDcmRcv(DcmRcv dcmrcvObject, String jobIdToCheckFor)
    {
	 this.dcmrcvObject = dcmrcvObject;
	 this.jobIdToCheckFor = jobIdToCheckFor;
	}
    //-----------------------------------------------------------------


    public static Association request(Socket socket,
                                      NetworkConnection connector,
                                      NetworkApplicationEntity ae,
                                      UserIdentity userIdentity) throws IOException {
        Association a = new Association(socket, connector, true);
        a.setApplicationEntity(ae);
        a.setUserIdentity(userIdentity);
        a.setState(State.STA4);
        return a;
    }

    public static Association accept(Socket socket,
                                     NetworkConnection connector)throws IOException {
        Association a = new Association(socket, connector, false);
        a.setState(State.STA2);
        a.startARTIM(connector.getRequestTimeout());
        return a;
    }

    public final Socket getSocket() {
        return socket;
    }


    final void setApplicationEntity(NetworkApplicationEntity ae) {
        this.ae = ae;
    }

    final void setUserIdentity(UserIdentity userIdentity) {
        this.userIdentity = userIdentity;
    }


    final AAssociateAC getAssociateAC() {
        return associateAC;
    }

    final AAssociateRQ getAssociateRQ() {
        return associateRQ;
    }

    final IOException getException() {
        return exception;
    }

    void checkException() throws IOException {
        if (exception != null)
            throw exception;
    }

    public final boolean isRequestor() {
        return requestor;
    }

    public final boolean isReadyForDataTransfer() {
        return state.isReadyForDataTransfer();
    }

    private boolean isReadyForDataReceive() {
        return state.isReadyForDataReceive();
    }

    void setState(State state) {
        if (this.state == state)
            return;

        synchronized (this) {
            log.debug("{} enter state: {}", this, state);
            this.state = state;
            notifyAll();
        }
    }

    private void processAC() {
        Collection<PresentationContext> c = associateAC.getPresentationContexts();
        for (PresentationContext pc : c) {
            if (!pc.isAccepted())
                continue;
            PresentationContext pcrq = associateRQ.getPresentationContext(
                    pc.getPCID());
            if (pcrq == null) {
                log.warn("{}: Missing Presentation Context(id={}) in received AA-AC",
                                name, new Integer(pc.getPCID()));
                continue;
            }
            String as = pcrq.getAbstractSyntax();
            Map<String, PresentationContext> ts2pc = acceptedPCs.get(as);
            if (ts2pc == null) {
                ts2pc = new HashMap<String, PresentationContext>();
                acceptedPCs.put(as, ts2pc);
            }
            ts2pc.put(pc.getTransferSyntax(), pc);
        }
        for (Map.Entry<String, Map<String, PresentationContext>> entry : acceptedPCs
                .entrySet()) {
            String asuid = entry.getKey();
            Map<String, PresentationContext> ts2pc = entry.getValue();
            String[] tsuids = ts2pc.keySet().toArray(new String[ts2pc.size()]);
            String cuid = asuid; // TODO support of Meta SOP Classes
            ExtendedNegotiation extneg = associateAC
                    .getExtendedNegotiationFor(cuid);
            byte[] extinfo = extneg != null ? extneg.getInformation() : null;
            if (isSCUFor(cuid)) {
                TransferCapability tc = new TransferCapability(cuid, tsuids,
                        TransferCapability.SCU);
                tc.setExtInfo(extinfo);
                scuTCs.put(cuid, tc);
            }
            if (isSCPFor(cuid)) {
                TransferCapability tc = new TransferCapability(cuid, tsuids,
                        TransferCapability.SCP);
                tc.setExtInfo(extinfo);
                scpTCs.put(cuid, tc);
            }
        }
    }

    private boolean isSCPFor(String cuid) {
        RoleSelection rolsel = associateAC.getRoleSelectionFor(cuid);
        if (rolsel == null)
            return !requestor;
        return requestor ? rolsel.isSCP() : rolsel.isSCU();
    }

    private boolean isSCUFor(String cuid) {
        RoleSelection rolsel = associateAC.getRoleSelectionFor(cuid);
        if (rolsel == null)
            return requestor;
        return requestor ? rolsel.isSCU() : rolsel.isSCP();
    }

    public String getCallingAET() {
        return associateRQ != null ? associateRQ.getCallingAET() : null;
    }

    public String getCalledAET() {
        return associateRQ != null ? associateRQ.getCalledAET() : null;
    }

    public String getRemoteAET() {
        return requestor ? getCalledAET() : getCallingAET();
    }

    public String getLocalAET() {
        return requestor ? getCallingAET() : getCalledAET();
    }

    public final UserIdentity getUserIdentity() {
        return userIdentity;
    }

    public TransferCapability getTransferCapabilityAsSCP(String cuid) {
        return scpTCs.get(cuid);
    }

    public TransferCapability getTransferCapabilityAsSCU(String cuid) {

        return scuTCs.get(cuid);
    }



    public AAssociateAC negotiate(AAssociateRQ rq) throws IOException,
            InterruptedException {
        sendAssociateRQ(rq);
        synchronized (this) {
            while (state == State.STA5)
                wait();
        }
        checkException();
        if (state != State.STA6) {
            throw new RuntimeException("unexpected state: " + state);
        }
        return associateAC;
    }

    public void release(boolean waitForRSP) throws InterruptedException {
        if (ae != null)
            ae.removeFromPool(this);
        if (waitForRSP)
            waitForDimseRSP();

        sendReleaseRQ();
        synchronized (this) {
            while (state != State.STA1)
                wait();
        }
    }

    public void waitForDimseRSP() throws InterruptedException {
        synchronized (rspHandlerForMsgId) {
            while (!rspHandlerForMsgId.isEmpty() && isReadyForDataReceive())
                rspHandlerForMsgId.wait();
        }
    }

    public void abort() {
        abort(new AAbort());
    }

    private PresentationContext pcFor(String cuid, String tsuid)
            throws NoPresentationContextException {
        Map<String,PresentationContext> ts2pc = acceptedPCs.get(cuid);
        if (ts2pc == null)
            throw new NoPresentationContextException("Abstract Syntax "
                    + UIDDictionary.getDictionary().prompt(cuid)
                    + " not supported");
        if (tsuid == null)
            return ts2pc.values().iterator().next();
        PresentationContext pc = ts2pc.get(tsuid);
        if (pc == null)
            throw new NoPresentationContextException("Abstract Syntax "
                    + UIDDictionary.getDictionary().prompt(cuid)
                    + " with Transfer Syntax "
                    + UIDDictionary.getDictionary().prompt(tsuid)
                    + " not supported");
        return pc;
    }





    //----------------------------------------------------------------------------
    //============================================================================
    //----------------------------------------------------------------------------


    //assoc.cstore(info.cuid, info.iuid, priority, new DataWriter(info), tsuid, rspHandler);


    public void cstore(String cuid, String iuid, int priority, DataWriter data,
            String tsuid, DimseRSPHandler rspHandler) throws IOException,
            InterruptedException {
        cstore(cuid, cuid, iuid, priority, data, tsuid, rspHandler);
    }

    public void cstore(String asuid, String cuid, String iuid, int priority,
            DataWriter data, String tsuid, DimseRSPHandler rspHandler)
            throws IOException, InterruptedException {
        PresentationContext pc = pcFor(asuid, tsuid);
        DicomObject cstorerq = CommandUtils.mkCStoreRQ(++msgID, cuid, iuid,
                priority);
        invoke(pc.getPCID(), cstorerq, data, rspHandler,
                ae.getDimseRspTimeout());
    }

    public DimseRSP cstore(String cuid, String iuid, int priority,
            DataWriter data, String tsuid) throws IOException,
            InterruptedException {
        return cstore(cuid, cuid, iuid, priority, data, tsuid);
    }

    public DimseRSP cstore(String asuid, String cuid, String iuid,
            int priority, DataWriter data, String tsuid) throws IOException,
            InterruptedException {
        FutureDimseRSP rsp = new FutureDimseRSP();
        cstore(asuid, cuid, iuid, priority, data, tsuid, rsp);
        return rsp;
    }
    //----------------------------------------------------------------------------
    //============================================================================
    //----------------------------------------------------------------------------






    public void cfind(String cuid, int priority, DicomObject data,
            String tsuid, DimseRSPHandler rspHandler) throws IOException,
            InterruptedException {
        cfind(cuid, cuid, priority, data, tsuid, rspHandler);
    }

    public void cfind(String asuid, String cuid, int priority,
            DicomObject data, String tsuid, DimseRSPHandler rspHandler)
            throws IOException, InterruptedException {
        PresentationContext pc = pcFor(asuid, tsuid);
        DicomObject cfindrq = CommandUtils.mkCFindRQ(++msgID, cuid, priority);
        invoke(pc.getPCID(), cfindrq, new DataWriterAdapter(data), rspHandler,
                ae.getDimseRspTimeout());
    }

    public DimseRSP cfind(String cuid, int priority, DicomObject data,
            String tsuid, int autoCancel) throws IOException,
            InterruptedException {
        return cfind(cuid, cuid, priority, data, tsuid, autoCancel);
    }

    public DimseRSP cfind(String asuid, String cuid, int priority,
            DicomObject data, String tsuid, int autoCancel) throws IOException,
            InterruptedException {
        FutureDimseRSP rsp = new FutureDimseRSP();
        rsp.setAutoCancel(autoCancel);
        cfind(asuid, cuid, priority, data, tsuid, rsp);
        return rsp;
    }

    public void cget(String cuid, int priority, DicomObject data, String tsuid,
            DimseRSPHandler rspHandler) throws IOException,
            InterruptedException {
        cget(cuid, cuid, priority, data, tsuid, rspHandler);
    }

    public void cget(String asuid, String cuid, int priority, DicomObject data,
            String tsuid, DimseRSPHandler rspHandler) throws IOException,
            InterruptedException {
        PresentationContext pc = pcFor(asuid, tsuid);
        DicomObject cfindrq = CommandUtils.mkCGetRQ(++msgID, cuid, priority);
        invoke(pc.getPCID(), cfindrq, new DataWriterAdapter(data), rspHandler,
                ae.getRetrieveRspTimeout());
    }

    public DimseRSP cget(String cuid, int priority, DicomObject data,
            String tsuid) throws IOException, InterruptedException {
        return cget(cuid, cuid, priority, data, tsuid);
    }

    public DimseRSP cget(String asuid, String cuid, int priority,
            DicomObject data, String tsuid) throws IOException,
            InterruptedException {
        FutureDimseRSP rsp = new FutureDimseRSP();
        cget(asuid, cuid, priority, data, tsuid, rsp);
        return rsp;
    }

    public void cmove(String cuid, int priority, DicomObject data,
            String tsuid, String destination, DimseRSPHandler rspHandler)
            throws IOException, InterruptedException {
        cmove(cuid, cuid, priority, data, tsuid, destination, rspHandler);
    }

    public void cmove(String asuid, String cuid, int priority,
            DicomObject data, String tsuid, String destination,
            DimseRSPHandler rspHandler) throws IOException,
            InterruptedException {
        PresentationContext pc = pcFor(asuid, tsuid);
        DicomObject cfindrq = CommandUtils.mkCMoveRQ(++msgID, cuid, priority,
                destination);
        invoke(pc.getPCID(), cfindrq, new DataWriterAdapter(data), rspHandler,
                ae.getRetrieveRspTimeout());
    }

    public DimseRSP cmove(String cuid, int priority, DicomObject data,
            String tsuid, String destination) throws IOException,
            InterruptedException {
        return cmove(cuid, cuid, priority, data, tsuid, destination);
    }

    public DimseRSP cmove(String asuid, String cuid, int priority,
            DicomObject data, String tsuid, String destination)
            throws IOException, InterruptedException {
        FutureDimseRSP rsp = new FutureDimseRSP();
        cmove(asuid, cuid, priority, data, tsuid, destination, rsp);
        return rsp;
    }

    public DimseRSP cecho() throws IOException, InterruptedException {
        return cecho(UID.VerificationSOPClass);
    }

    public DimseRSP cecho(String cuid) throws IOException, InterruptedException {
        FutureDimseRSP rsp = new FutureDimseRSP();
        PresentationContext pc = pcFor(cuid, null);
        DicomObject cechorq = CommandUtils.mkCEchoRQ(++msgID, cuid);
        invoke(pc.getPCID(), cechorq, null, rsp, ae.getDimseRspTimeout());
        return rsp;
    }

    public void nevent(String cuid, String iuid, int eventTypeId,
            DicomObject attrs, String tsuid, DimseRSPHandler rspHandler)
            throws IOException, InterruptedException {
        nevent(cuid, cuid, iuid, eventTypeId, attrs, tsuid, rspHandler);
    }

    public void nevent(String asuid, String cuid, String iuid, int eventTypeId,
            DicomObject attrs, String tsuid, DimseRSPHandler rspHandler)
            throws IOException, InterruptedException {
        PresentationContext pc = pcFor(asuid, tsuid);
        DicomObject neventrq = CommandUtils.mkNEventReportRQ(++msgID, cuid,
                iuid, eventTypeId, attrs);
        invoke(pc.getPCID(), neventrq,
                attrs != null ? new DataWriterAdapter(attrs) : null,
                rspHandler, ae.getDimseRspTimeout());
    }

    public DimseRSP nevent(String cuid, String iuid, int eventTypeId,
            DicomObject attrs, String tsuid) throws IOException,
            InterruptedException {
        return nevent(cuid, cuid, iuid, eventTypeId, attrs, tsuid);
    }

    public DimseRSP nevent(String asuid, String cuid, String iuid,
            int eventTypeId, DicomObject attrs, String tsuid)
            throws IOException, InterruptedException {
        FutureDimseRSP rsp = new FutureDimseRSP();
        nevent(asuid, cuid, iuid, eventTypeId, attrs, tsuid, rsp);
        return rsp;
    }

    public void nget(String cuid, String iuid, int[] tags,
            DimseRSPHandler rspHandler)
            throws IOException, InterruptedException {
        nget(cuid, cuid, iuid, tags, rspHandler);
    }

    public void nget(String asuid, String cuid, String iuid, int[] tags,
            DimseRSPHandler rspHandler)
            throws IOException, InterruptedException {
        PresentationContext pc = pcFor(asuid, null);
        DicomObject ngetrq = CommandUtils.mkNGetRQ(++msgID, cuid, iuid, tags);
        invoke(pc.getPCID(), ngetrq, null, rspHandler, ae.getDimseRspTimeout());
    }

    public DimseRSP nget(String cuid, String iuid, int[] tags)
            throws IOException, InterruptedException {
        return nget(cuid, cuid, iuid, tags);
    }

    public DimseRSP nget(String asuid, String cuid, String iuid, int[] tags)
            throws IOException, InterruptedException {
        FutureDimseRSP rsp = new FutureDimseRSP();
        nget(asuid, cuid, iuid, tags, rsp);
        return rsp;
    }

    public void nset(String cuid, String iuid, DicomObject attrs, String tsuid,
            DimseRSPHandler rspHandler) throws IOException,
            InterruptedException {
        nset(cuid, cuid, iuid, attrs, tsuid, rspHandler);
    }

    public void nset(String asuid, String cuid, String iuid, DicomObject attrs,
            String tsuid, DimseRSPHandler rspHandler) throws IOException,
            InterruptedException {
        PresentationContext pc = pcFor(asuid, tsuid);
        DicomObject nsetrq = CommandUtils.mkNSetRQ(++msgID, cuid, iuid);
        invoke(pc.getPCID(), nsetrq, new DataWriterAdapter(attrs), rspHandler,
                ae.getDimseRspTimeout());
    }

    public DimseRSP nset(String cuid, String iuid, DicomObject attrs,
            String tsuid) throws IOException, InterruptedException {
        return nset(cuid, cuid, iuid, attrs, tsuid);
    }

    public DimseRSP nset(String asuid, String cuid, String iuid,
            DicomObject attrs, String tsuid) throws IOException,
            InterruptedException {
        FutureDimseRSP rsp = new FutureDimseRSP();
        nset(asuid, cuid, iuid, attrs, tsuid, rsp);
        return rsp;
    }

    public void naction(String cuid, String iuid, int actionTypeId,
            DicomObject attrs, String tsuid, DimseRSPHandler rspHandler)
            throws IOException, InterruptedException {
        naction(cuid, cuid, iuid, actionTypeId, attrs, tsuid, rspHandler);
    }

    public void naction(String asuid, String cuid, String iuid,
            int actionTypeId, DicomObject attrs, String tsuid,
            DimseRSPHandler rspHandler) throws IOException,
            InterruptedException {
        PresentationContext pc = pcFor(asuid, tsuid);
        DicomObject nactionrq = CommandUtils.mkNActionRQ(++msgID, cuid, iuid,
                actionTypeId, attrs);
        invoke(pc.getPCID(), nactionrq,
                attrs != null ? new DataWriterAdapter(attrs) : null,
                rspHandler, ae.getDimseRspTimeout());
    }

    public DimseRSP naction(String cuid, String iuid, int actionTypeId,
            DicomObject attrs, String tsuid) throws IOException,
            InterruptedException {
        return naction(cuid, cuid, iuid, actionTypeId, attrs, tsuid);
    }

    public DimseRSP naction(String asuid, String cuid, String iuid,
            int actionTypeId, DicomObject attrs, String tsuid)
            throws IOException, InterruptedException {
        FutureDimseRSP rsp = new FutureDimseRSP();
        naction(asuid, cuid, iuid, actionTypeId, attrs, tsuid, rsp);
        return rsp;
    }

    public void ncreate(String cuid, String iuid, DicomObject attrs,
            String tsuid, DimseRSPHandler rspHandler) throws IOException,
            InterruptedException {
        ncreate(cuid, cuid, iuid, attrs, tsuid, rspHandler);
    }

    public void ncreate(String asuid, String cuid, String iuid,
            DicomObject attrs, String tsuid, DimseRSPHandler rspHandler)
            throws IOException, InterruptedException {
        PresentationContext pc = pcFor(asuid, tsuid);
        DicomObject ncreaterq = CommandUtils.mkNCreateRQ(++msgID, cuid, iuid);
        invoke(pc.getPCID(), ncreaterq,
                attrs != null ? new DataWriterAdapter(attrs) : null,
                rspHandler, ae.getDimseRspTimeout());
    }

    public DimseRSP ncreate(String cuid, String iuid, DicomObject attrs,
            String tsuid) throws IOException, InterruptedException {
        return ncreate(cuid, cuid, iuid, attrs, tsuid);
    }

    public DimseRSP ncreate(String asuid, String cuid, String iuid,
            DicomObject attrs, String tsuid) throws IOException,
            InterruptedException {
        FutureDimseRSP rsp = new FutureDimseRSP();
        ncreate(asuid, cuid, iuid, attrs, tsuid, rsp);
        return rsp;
    }

    public void ndelete(String cuid, String iuid, DimseRSPHandler rspHandler)
            throws IOException, InterruptedException {
        ndelete(cuid, cuid, iuid, rspHandler);
    }

    public void ndelete(String asuid, String cuid, String iuid,
            DimseRSPHandler rspHandler) throws IOException,
            InterruptedException {
        PresentationContext pc = pcFor(asuid, null);
        DicomObject nsetrq = CommandUtils.mkNDeleteRQ(++msgID, cuid, iuid);
        invoke(pc.getPCID(), nsetrq, null, rspHandler, ae.getDimseRspTimeout());
    }

    public DimseRSP ndelete(String asuid, String cuid, String iuid)
            throws IOException, InterruptedException {
        FutureDimseRSP rsp = new FutureDimseRSP();
        ndelete(asuid, cuid, iuid, rsp);
        return rsp;
    }

    public DimseRSP ndelete(String cuid, String iuid) throws IOException,
            InterruptedException {
        return ndelete(cuid, cuid, iuid);
    }

    void invoke(int pcid, DicomObject cmd, DataWriter data,
            DimseRSPHandler rspHandler, int rspTimeout) throws IOException,
            InterruptedException {
        if (CommandUtils.isResponse(cmd))
            throw new IllegalArgumentException("cmd:\n" + cmd);
        checkException();
        if (!isReadyForDataTransfer())
            throw new IllegalStateException(state.toString());
        PresentationContext pc = associateAC.getPresentationContext(pcid);
        if (pc == null)
            throw new IllegalStateException("No Presentation State with id - "
                    + pcid);
        if (!pc.isAccepted())
            throw new IllegalStateException(
                    "Presentation State not accepted - " + pc);
        rspHandler.setPcid(pcid);
        rspHandler.setMsgId(cmd.getInt(Tag.MessageID));
        rspHandler.setTimeout(System.currentTimeMillis() + rspTimeout);
        addDimseRSPHandler(cmd.getInt(Tag.MessageID), rspHandler);
        encoder.writeDIMSE(pcid, cmd, data, pc.getTransferSyntax());
    }

    void cancel(int pcid, int msgid) throws IOException {
        DicomObject cmd = CommandUtils.mkCCancelRQ(msgid);
        encoder.writeDIMSE(pcid, cmd, null, null);
    }

    public void writeDimseRSP(int pcid, DicomObject cmd) throws IOException {
        writeDimseRSP(pcid, cmd, null);
    }

    public void writeDimseRSP(int pcid, DicomObject cmd, DicomObject data)
            throws IOException {
        if (!CommandUtils.isResponse(cmd))
            throw new IllegalArgumentException("cmd:\n" + cmd);
        PresentationContext pc = associateAC.getPresentationContext(pcid);
        if (pc == null)
            throw new IllegalStateException("No Presentation State with id - "
                    + pcid);
        if (!pc.isAccepted())
            throw new IllegalStateException(
                    "Presentation State not accepted - " + pc);

        DataWriter writer = null;
        int datasetType = CommandUtils.NO_DATASET;
        if (data != null) {
            writer = new DataWriterAdapter(data);
            datasetType = CommandUtils.getWithDatasetType();
        }
        cmd.putInt(Tag.DataSetType, VR.US, datasetType);
        encoder.writeDIMSE(pcid, cmd, writer, pc.getTransferSyntax());
        if (!CommandUtils.isPending(cmd)) {
            updateIdleTimeout();
            decPerforming();
        }
    }


    void onCancelRQ(DicomObject cmd) throws IOException {
        int msgId = cmd.getInt(Tag.MessageIDBeingRespondedTo);
        DimseRSP handler = removeCancelRQHandler(msgId);
        if (handler != null) {
            handler.cancel(this);
        }
    }

    public void registerCancelRQHandler(DicomObject cmd, DimseRSP handler) {
        synchronized (cancelHandlerForMsgId) {
            cancelHandlerForMsgId.put(cmd.getInt(Tag.MessageID), handler);
        }
    }

    private DimseRSP removeCancelRQHandler(int msgId) {
        synchronized (cancelHandlerForMsgId) {
            return (DimseRSP) cancelHandlerForMsgId.remove(msgId);
        }
    }

    void onDimseRSP(DicomObject cmd, DicomObject data) throws IOException {
        int msgId = cmd.getInt(Tag.MessageIDBeingRespondedTo);
        DimseRSPHandler rspHandler = getDimseRSPHandler(msgId);
        if (rspHandler == null) {
            log.warn("unexpected message ID in DIMSE RSP:\n{}", cmd);
            throw new AAbort();
        }
        try {
            rspHandler.onDimseRSP(this, cmd, data);
        } finally {
            if (!CommandUtils.isPending(cmd)) {
                updateIdleTimeout();
                removeDimseRSPHandler(msgId);
            } else {
                rspHandler.setTimeout(System.currentTimeMillis()
                        + (isRetrieveRsp(cmd) ? ae.getRetrieveRspTimeout()
                                              : ae.getDimseRspTimeout()));
            }
        }
    }

    private static boolean isRetrieveRsp(DicomObject cmd) {
        int cmdField = cmd.getInt(Tag.CommandField);
        return cmdField == CommandUtils.C_MOVE_RSP
                || cmdField == CommandUtils.C_GET_RSP;
    }

    private void addDimseRSPHandler(int msgId, DimseRSPHandler rspHandler)
            throws InterruptedException {
        synchronized (rspHandlerForMsgId) {
            while (maxOpsInvoked > 0
                    && rspHandlerForMsgId.size() >= maxOpsInvoked)
                rspHandlerForMsgId.wait();
            if (isReadyForDataReceive())
                rspHandlerForMsgId.put(msgId, rspHandler);
        }
    }

    private DimseRSPHandler removeDimseRSPHandler(int msgId) {
        synchronized (rspHandlerForMsgId) {
            DimseRSPHandler tmp = (DimseRSPHandler) rspHandlerForMsgId
                    .remove(msgId);
            rspHandlerForMsgId.notifyAll();
            return tmp;
        }
    }

    private DimseRSPHandler getDimseRSPHandler(int msgId) {
        synchronized (rspHandlerForMsgId) {
            return rspHandlerForMsgId.get(msgId);
        }
    }

    public void run() {
        try {
            connector.incListenerConnectionCount();
            this.decoder = new PDUDecoder(this, in);
            while (!(state == State.STA1 || state == State.STA13))
                decoder.nextPDU();
        } catch (AAbort aa) {
            abort(aa);
        } catch (SocketTimeoutException e) {
            exception = e;
            log.warn("ARTIM timer expired in State: " + state);
        } catch (IOException e) {
            exception = e;
            log.warn("i/o exception in State " + state, e);
        } finally {
            connector.decListenerConnectionCount();
            closeSocket();
        }
    }


    public void closeSocket() {


    //private void closeSocket() {
        if (state == State.STA13) {
            try {
                Thread.sleep(connector.getSocketCloseDelay());
            } catch (InterruptedException e) {
                log.warn("Interrupted Socket Close Delay", e);
            }
        }
        setState(State.STA1);
        CloseUtils.safeClose(out);
        CloseUtils.safeClose(in);
        if (!closed) {
            log.info("{}: close {}", name, socket);
            CloseUtils.safeClose(socket);
            closed = true;
            onClosed();
        }
    this.sockClosed = true;

    //=============================================
    // MY HACK:
    // If this is DcmRcv, check to see if
    // other associations for the same job are done.

     if(this.masterJob != null)
	   {
	    this.masterJob.checkJobStatus();
	   }

    //==============================================
    }

    private void onClosed() {
        if (ae != null)
            ae.removeFromPool(this);
        reaper.unregister(this);
        synchronized (rspHandlerForMsgId) {
            rspHandlerForMsgId.accept(new IntHashtable.Visitor() {

                public boolean visit(int key, Object value) {
                    ((DimseRSPHandler) value).onClosed(Association.this);
                    return true;
                }
            });
            rspHandlerForMsgId.clear();
            rspHandlerForMsgId.notifyAll();
        }
        if (ae != null) {
            ae.associationClosed(this);
        }

    }

    int getMaxPDULengthSend() {
        return maxPDULength;
    }

    boolean isPackPDV() {
        return ae.isPackPDV();
    }

    private void startARTIM(int timeout) throws IOException {
        if (log.isDebugEnabled())
            log.debug(name + ": start ARTIM " + timeout + "ms");
        socket.setSoTimeout(timeout);
    }

    private void stopARTIM() throws IOException {
        socket.setSoTimeout(0);
        log.debug("{}: stop ARTIM", name);
    }

    void receivedAssociateRQ(AAssociateRQ rq) throws IOException {
        log.info("{} >> {}", name, rq);
        state.receivedAssociateRQ(this, rq);
    }

    void receivedAssociateAC(AAssociateAC ac) throws IOException {
        log.info("{} >> {}", name, ac);
        state.receivedAssociateAC(this, ac);
    }

    void receivedAssociateRJ(AAssociateRJ rj) throws IOException {
        log.info("{} >> {}", name, rj);
        state.receivedAssociateRJ(this, rj);
    }

    void receivedPDataTF() throws IOException {
        state.receivedPDataTF(this);
    }

    void onPDataTF() throws IOException {
        decoder.decodeDIMSE();
    }

    void receivedReleaseRQ() throws IOException {
        log.info("{} >> A-RELEASE-RQ", name);
        state.receivedReleaseRQ(this);
    }

    void receivedReleaseRP() throws IOException {
        log.info("{} >> A-RELEASE-RP", name);
        state.receivedReleaseRP(this);
    }

    void receivedAbort(AAbort aa) {
        log.info("{}: >> {}", name, aa);
        exception = aa;
        setState(State.STA1);
        ae.removeFromPool(this);
    }

    void onDimseRQ(int pcid, DicomObject cmd, PDVInputStream data, String tsuid)
            throws IOException {
        incPerforming();
        ae.perform(this, pcid, cmd, data, tsuid);
    }

    private synchronized void incPerforming() {
        ++performing;
    }

    private synchronized void decPerforming() {
        --performing;
        notifyAll();
    }

    void sendPDataTF() throws IOException {
        try {
            state.sendPDataTF(this);
        } catch (IOException e) {
            closeSocket();
            throw e;
        }
    }

    void writePDataTF() throws IOException {
        encoder.writePDataTF();
    }

    void sendAssociateRQ(AAssociateRQ rq) throws IOException {
        try {
            // start ARTIM BEFORE sending A-ASSOCIATE-RQ PDU,
            // otherwise A-ASSOCIATE-AC PDU may be received and processed,
            // and startARTIM() invoked AFTER stopARTIM(), which may cause
            // a socket close during established Association with
            // WARN   - ARTIM timer expired in State: Sta6
            startARTIM(connector.getAcceptTimeout());
            state.sendAssociateRQ(this, rq);
        } catch (IOException e) {
            closeSocket();
            throw e;
        }
    }

    void sendReleaseRQ() {
        try {
            state.sendReleaseRQ(this);
        } catch (IOException e) {
            closeSocket();
        }
    }

    void abort(AAbort aa) {
        if (ae != null)
            ae.removeFromPool(this);
        state.abort(this, aa);
    }

    void writeAbort(AAbort aa) {
        exception = aa;
        setState(State.STA13);
        try {
            encoder.write(aa);
        } catch (IOException e) {
            log.debug("Failed to write " + aa, e);
        }
        closeSocket();
    }

    void unexpectedPDU(String name) throws AAbort {
        log.warn("received unexpected " + name + " in state: " + state);
        throw new AAbort(AAbort.UL_SERIVE_PROVIDER, AAbort.UNEXPECTED_PDU);
    }

    void illegalStateForSending(String name) throws IOException {
        log.warn("unable to send " + name + " in state: " + state);
        checkException();
        throw new AAbort();
    }

    void writeAssociationRQ(AAssociateRQ rq) throws IOException {
        associateRQ = rq;
        name = rq.getCalledAET() + '(' + serialNo + ")";
        setState(State.STA5);
        encoder.write(rq);
    }

    void onAAssociateRQ(AAssociateRQ rq) throws IOException {
        associateRQ = rq;
        name = rq.getCallingAET() + '(' + serialNo + ")";
        stopARTIM();
        setState(State.STA3);
        try {
            if ((rq.getProtocolVersion() & 1) == 0)
                throw new AAssociateRJ(AAssociateRJ.RESULT_REJECTED_PERMANENT,
                        AAssociateRJ.SOURCE_SERVICE_PROVIDER_ACSE,
                        AAssociateRJ.REASON_PROTOCOL_VERSION_NOT_SUPPORTED);
            if (!rq.getApplicationContext().equals(
                    UID.DICOMApplicationContextName))
                throw new AAssociateRJ(AAssociateRJ.RESULT_REJECTED_PERMANENT,
                        AAssociateRJ.SOURCE_SERVICE_USER,
                        AAssociateRJ.REASON_APP_CTX_NAME_NOT_SUPPORTED);
            NetworkApplicationEntity ae = connector.getDevice()
                    .getNetworkApplicationEntity(rq.getCalledAET());
            if (ae == null)
                throw new AAssociateRJ(AAssociateRJ.RESULT_REJECTED_PERMANENT,
                        AAssociateRJ.SOURCE_SERVICE_USER,
                        AAssociateRJ.REASON_CALLED_AET_NOT_RECOGNIZED);
            if (!connector.checkConnectionCountWithinLimit())
                throw new AAssociateRJ(AAssociateRJ.RESULT_REJECTED_TRANSIENT,
                        AAssociateRJ.SOURCE_SERVICE_PROVIDER_ACSE,
                        AAssociateRJ.REASON_TEMPORARY_CONGESTION);
            setApplicationEntity(ae);
            ae.negotiate(this, rq);
            associateAC = ae.negotiate(this, rq);
            processAC();
            maxOpsInvoked = associateAC.getMaxOpsPerformed();
            maxPDULength = minZeroAsMax(rq.getMaxPDULength(), ae
                    .getMaxPDULengthSend());
            setState(State.STA6);
            encoder.write(associateAC);
            updateIdleTimeout();
            reaper.register(this);
            ae.addToPool(this);
            ae.associationAccepted(this);
        } catch (AAssociateRJ e) {
            setState(State.STA13);
            encoder.write(e);
        }
    }

    void onAssociateAC(AAssociateAC ac) throws IOException {
        associateAC = ac;
        stopARTIM();
        processAC();
        maxOpsInvoked = associateAC.getMaxOpsInvoked();
        maxPDULength = minZeroAsMax(associateAC.getMaxPDULength(), ae
                .getMaxPDULengthSend());
        setState(State.STA6);
        updateIdleTimeout();
        reaper.register(this);
    }

    private int minZeroAsMax(int i1, int i2) {
        return i1 == 0 ? i2 : i2 == 0 ? i1 : Math.min(i1, i2);
    }

    private void updateIdleTimeout() {
        idleTimeout = System.currentTimeMillis() + ae.getIdleTimeout();
    }

    void onAssociateRJ(AAssociateRJ rj) throws IOException {
        stopARTIM();
        exception = rj;
        setState(State.STA1);
    }

    void writeReleaseRQ() throws IOException {
        setState(State.STA7);
        encoder.writeAReleaseRQ();
    }

    void onReleaseRP() throws IOException {
        stopARTIM();
        setState(State.STA1);
    }

    void onCollisionReleaseRP() throws IOException {
        stopARTIM();
        // setState(State.STA12);
        log.info("{} << A-RELEASE-RP", name);
        setState(State.STA13);
        encoder.writeAReleaseRP();
    }

    void onReleaseRQ() throws IOException {
        setState(State.STA8);
        if (ae != null)
            ae.removeFromPool(this);
        waitForPerformingOps();
        setState(State.STA13);
        encoder.writeAReleaseRP();
    }

    private synchronized void waitForPerformingOps() {
        while (performing > 0 && isReadyForDataReceive()) {
            try {
                wait();
            } catch (InterruptedException e) {
                // explicitly interrupted up by another thread; continue
            }
        }
    }

    void onCollisionReleaseRQ() throws IOException {
        if (requestor) {
            // setState(State.STA9);
            setState(State.STA11);
            encoder.writeAReleaseRP();
        } else {
            setState(State.STA10);
        }
    }

    void checkIdle(final long now) {
        if (performing > 0)
            return;
        if (rspHandlerForMsgId.isEmpty()) {
            if (now > idleTimeout)
                try {
                    release(false);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
        } else {
            rspHandlerForMsgId.accept(new IntHashtable.Visitor() {

                public boolean visit(int key, Object value) {
                    DimseRSPHandler rspHandler = (DimseRSPHandler) value;
                    if (now < rspHandler.getTimeout())
                        return true;
                    Association.this.abort();
                    return false;
                }
            });
        }
    }

    /**
     * Get the <code>NetworkConnection</code> object that is performing this
     * association.
     *
     * @return NetworkConnection Returns the connector.
     */
    public NetworkConnection getConnector()
    {
        return connector;
    }

}
