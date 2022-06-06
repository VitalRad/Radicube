
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

import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.UIDDictionary;
import org.dcm4che2.data.VR;

/**
 * @author gunter zeilinger(gunterze@gmail.com)
 * @version $Reversion$ $Date: 2008-09-30 00:36:35 +0200 (Tue, 30 Sep 2008) $
 * @since Oct 6, 2005
 *
 */
public class CommandUtils
{
    public static final int SUCCESS = 0;
    public static final int PENDING = 0xFF00;

    public static final int NORMAL = 0;
    public static final int HIGH = 1;
    public static final int LOW = 2;

    public static final int C_STORE_RQ = 0x0001;
    public static final int C_STORE_RSP = 0x8001;
    public static final int C_GET_RQ = 0x0010;
    public static final int C_GET_RSP = 0x8010;
    public static final int C_FIND_RQ = 0x0020;
    public static final int C_FIND_RSP = 0x8020;
    public static final int C_MOVE_RQ = 0x0021;
    public static final int C_MOVE_RSP = 0x8021;
    public static final int C_ECHO_RQ = 0x0030;
    public static final int C_ECHO_RSP = 0x8030;
    public static final int N_EVENT_REPORT_RQ = 0x0100;
    public static final int N_EVENT_REPORT_RSP = 0x8100;
    public static final int N_GET_RQ = 0x0110;
    public static final int N_GET_RSP = 0x8110;
    public static final int N_SET_RQ = 0x0120;
    public static final int N_SET_RSP = 0x8120;
    public static final int N_ACTION_RQ = 0x0130;
    public static final int N_ACTION_RSP = 0x8130;
    public static final int N_CREATE_RQ = 0x0140;
    public static final int N_CREATE_RSP = 0x8140;
    public static final int N_DELETE_RQ = 0x0150;
    public static final int N_DELETE_RSP = 0x8150;
    public static final int C_CANCEL_RQ = 0x0FFF;
    private static final int RSP = 0x8000;

    public static final int NO_DATASET = 0x0101;
    private static final String NL = System.getProperty("line.separator");
    private static int withDatasetType = 0x0000;

    private static boolean includeUIDinRSP;


    public static boolean isResponse(DicomObject dcmobj)
    {
        return (dcmobj.getInt(Tag.CommandField) & RSP) != 0;
    }

    public static boolean isCancelRQ(DicomObject dcmobj)
    {
        return dcmobj.getInt(Tag.CommandField) == C_CANCEL_RQ;
    }

    public static DicomObject mkCStoreRQ(int msgId, String cuid, String iuid,
            int priority)
    {
       DicomObject rq = mkRQ(msgId, C_STORE_RQ, withDatasetType);
       rq.putString(Tag.AffectedSOPClassUID, VR.UI, cuid);
       rq.putString(Tag.AffectedSOPInstanceUID, VR.UI, iuid);
       rq.putInt(Tag.Priority, VR.US, priority);
       return rq;
    }

    public static DicomObject mkCStoreRQ(int msgId, String cuid, String iuid,
            int priority, String moveOriginatorAET, int moveOriginatorMsgId)
    {
       DicomObject rq = mkCStoreRQ(msgId, cuid, iuid, priority);
       rq.putString(Tag.MoveOriginatorApplicationEntityTitle, VR.AE,
               moveOriginatorAET);
       rq.putInt(Tag.MoveOriginatorMessageID, VR.US, moveOriginatorMsgId);
       return rq;
    }

    public static DicomObject mkCFindRQ(int msgId, String cuid, int priority)
    {
       DicomObject rq = mkRQ(msgId, C_FIND_RQ, withDatasetType);
       rq.putString(Tag.AffectedSOPClassUID, VR.UI, cuid);
       rq.putInt(Tag.Priority, VR.US, priority);
       return rq;
    }

    public static DicomObject mkCGetRQ(int msgId, String cuid, int priority)
    {
       DicomObject rq = mkRQ(msgId, C_GET_RQ, withDatasetType);
       rq.putString(Tag.AffectedSOPClassUID, VR.UI, cuid);
       rq.putInt(Tag.Priority, VR.US, priority);
       return rq;
    }

    public static DicomObject mkCMoveRQ(int msgId, String cuid, int priority,
            String destination)
    {
       DicomObject rq = mkRQ(msgId, C_MOVE_RQ, withDatasetType);
       rq.putString(Tag.AffectedSOPClassUID, VR.UI, cuid);
       rq.putInt(Tag.Priority, VR.US, priority);
       rq.putString(Tag.MoveDestination, VR.AE, destination);
       return rq;
    }

    public static DicomObject mkCCancelRQ(int msgId)
    {
        DicomObject rq = new BasicDicomObject();
        rq.putInt(Tag.CommandField, VR.US, C_CANCEL_RQ);
        rq.putInt(Tag.DataSetType, VR.US, NO_DATASET);
        rq.putInt(Tag.MessageIDBeingRespondedTo, VR.US, msgId);
        return rq;
    }

    public static DicomObject mkCEchoRQ(int msgId, String cuid)
    {
       DicomObject rq = mkRQ(msgId, C_ECHO_RQ, NO_DATASET);
       rq.putString(Tag.AffectedSOPClassUID, VR.UI, cuid);
       return rq;
    }

    public static DicomObject mkNEventReportRQ(int msgId, String cuid,
            String iuid, int eventTypeID, DicomObject data)
    {
       DicomObject rq = mkRQ(msgId, N_EVENT_REPORT_RQ,
               data == null ? NO_DATASET : withDatasetType);
       rq.putString(Tag.AffectedSOPClassUID, VR.UI, cuid);
       rq.putString(Tag.AffectedSOPInstanceUID, VR.UI, iuid);
       rq.putInt(Tag.EventTypeID, VR.US, eventTypeID);
       return rq;
    }

    public static DicomObject mkNGetRQ(int msgId, String cuid, String iuid,
            int[] tags)
    {
       DicomObject rq = mkRQ(msgId, N_GET_RQ, NO_DATASET);
       rq.putString(Tag.RequestedSOPClassUID, VR.UI, cuid);
       rq.putString(Tag.RequestedSOPInstanceUID, VR.UI, iuid);
       if (tags != null) {
           rq.putInts(Tag.AttributeIdentifierList, VR.AT, tags);
       }
       return rq;
    }

    public static DicomObject mkNSetRQ(int msgId, String cuid, String iuid)
    {
        DicomObject rq = mkRQ(msgId, N_SET_RQ, withDatasetType);
        rq.putString(Tag.RequestedSOPClassUID, VR.UI, cuid);
        rq.putString(Tag.RequestedSOPInstanceUID, VR.UI, iuid);
        return rq;
    }

    public static DicomObject mkNActionRQ(int msgId, String cuid,
            String iuid, int actionTypeID, DicomObject data)
    {
       DicomObject rq = mkRQ(msgId, N_ACTION_RQ,
               data == null ? NO_DATASET : withDatasetType);
       rq.putString(Tag.RequestedSOPClassUID, VR.UI, cuid);
       rq.putString(Tag.RequestedSOPInstanceUID, VR.UI, iuid);
       rq.putInt(Tag.ActionTypeID, VR.US, actionTypeID);
       return rq;
    }

    public static DicomObject mkNCreateRQ(int msgId, String cuid, String iuid)
    {
        DicomObject rq = mkRQ(msgId, N_CREATE_RQ, withDatasetType);
        if (cuid != null)
            rq.putString(Tag.AffectedSOPClassUID, VR.UI, cuid);
        rq.putString(Tag.AffectedSOPInstanceUID, VR.UI, iuid);
        return rq;
    }

    public static DicomObject mkNDeleteRQ(int msgId, String cuid, String iuid)
    {
        DicomObject rq = mkRQ(msgId, N_DELETE_RQ, NO_DATASET);
        rq.putString(Tag.RequestedSOPClassUID, VR.UI, cuid);
        rq.putString(Tag.RequestedSOPInstanceUID, VR.UI, iuid);
        return rq;
    }

    private static DicomObject mkRQ(int msgId, int cmdfield, int datasetType)
    {
        DicomObject rsp = new BasicDicomObject();
        rsp.putInt(Tag.MessageID, VR.US, msgId);
        rsp.putInt(Tag.CommandField, VR.US, cmdfield);
        rsp.putInt(Tag.DataSetType, VR.US, datasetType);
        return rsp;
    }

    public static DicomObject mkRSP(DicomObject rq, int status)
    {
        DicomObject rsp = new BasicDicomObject();
        rsp.putInt(Tag.CommandField, VR.US, rq.getInt(Tag.CommandField) | RSP);
        rsp.putInt(Tag.Status, VR.US, status);
        rsp.putInt(Tag.MessageIDBeingRespondedTo, VR.US, rq.getInt(Tag.MessageID));
        if (includeUIDinRSP) {
            String cuid = rq.getString(Tag.AffectedSOPClassUID);
            if (cuid == null)
                cuid = rq.getString(Tag.RequestedSOPClassUID);
            rsp.putString(Tag.AffectedSOPClassUID, VR.UI, cuid);
            String iuid = rq.getString(Tag.AffectedSOPInstanceUID);
            if (iuid == null)
                iuid = rq.getString(Tag.RequestedSOPInstanceUID);
            if (iuid != null) {
                rsp.putString(Tag.AffectedSOPInstanceUID, VR.UI, iuid);
            }
        }
        return rsp;
    }

    public static boolean isIncludeUIDinRSP()
    {
        return includeUIDinRSP;
    }

    public static void setIncludeUIDinRSP(boolean includeUIDinRSP)
    {
        CommandUtils.includeUIDinRSP = includeUIDinRSP;
    }

    public static int getWithDatasetType()
    {
        return withDatasetType;
    }

    public static void setWithDatasetType(int withDatasetType)
    {
        if (withDatasetType == NO_DATASET || (withDatasetType & 0xffff0000) != 0)
            throw new IllegalArgumentException("withDatasetType: "
                    + Integer.toHexString(withDatasetType) + "H");
        CommandUtils.withDatasetType = withDatasetType;
    }

    public static boolean hasDataset(DicomObject dcmobj)
    {
        return dcmobj.getInt(Tag.DataSetType) != NO_DATASET;
    }

    public static boolean isPending(DicomObject cmd)
    {
        return (cmd.getInt(Tag.Status) & PENDING) == PENDING;
    }

    public static String toString(DicomObject cmd, int pcid, String tsuid)
    {
        UIDDictionary dict = UIDDictionary.getDictionary();
        StringBuffer sb = new StringBuffer(64);
        switch (cmd.getInt(Tag.CommandField))
        {
            case C_STORE_RQ:
                sb.append(cmd.getInt(Tag.MessageID));
                sb.append(":C-STORE-RQ[pcid=").append(pcid);
                sb.append(", prior=").append(cmd.getInt(Tag.Priority));
                sb.append(NL).append("\tcuid=").append(dict.prompt(cmd.getString(Tag.AffectedSOPClassUID)));
                sb.append(NL).append("\tiuid=").append(cmd.getString(Tag.AffectedSOPInstanceUID));
                break;
            case C_GET_RQ:
                sb.append(cmd.getInt(Tag.MessageID));
                sb.append(":C-GET-RQ[pcid=").append(pcid);
                sb.append(", prior=").append(cmd.getInt(Tag.Priority));
                sb.append(NL).append("\tcuid=");
                sb.append(dict.prompt(cmd.getString(Tag.AffectedSOPClassUID)));
                break;
            case C_FIND_RQ:
                sb.append(cmd.getInt(Tag.MessageID));
                sb.append(":C-FIND-RQ[pcid=").append(pcid);
                sb.append(", prior=").append(cmd.getInt(Tag.Priority));
                sb.append(NL).append("\tcuid=");
                sb.append(dict.prompt(cmd.getString(Tag.AffectedSOPClassUID)));
                break;
            case C_MOVE_RQ:
                sb.append(cmd.getInt(Tag.MessageID));
                sb.append(":C-MOVE-RQ[pcid=").append(pcid);
                sb.append(", aet=").append(dict.prompt(cmd.getString(Tag.MoveDestination)));
                sb.append(", prior=").append(cmd.getInt(Tag.Priority));
                sb.append(NL).append("\tcuid=");
                sb.append(dict.prompt(cmd.getString(Tag.AffectedSOPClassUID)));
                break;
            case C_ECHO_RQ:
                sb.append(cmd.getInt(Tag.MessageID));
                sb.append(":C-ECHO-RQ[pcid=").append(pcid);
                sb.append(NL).append("\tcuid=");
                sb.append(dict.prompt(cmd.getString(Tag.AffectedSOPClassUID)));
                break;
            case N_EVENT_REPORT_RQ:
                sb.append(cmd.getInt(Tag.MessageID));
                sb.append(":N-EVENT-REPORT-RQ[pcid=").append(pcid);
                sb.append(", eventID=").append(cmd.getInt(Tag.EventTypeID));
                sb.append(NL).append("\tcuid=");
                sb.append(dict.prompt(cmd.getString(Tag.AffectedSOPClassUID)));
                sb.append(NL).append("\tiuid=");
                sb.append(cmd.getString(Tag.AffectedSOPInstanceUID));
                break;
             case N_GET_RQ:
                 sb.append(cmd.getInt(Tag.MessageID));
                 sb.append(":N-GET-RQ[pcid=").append(pcid);
                 sb.append(NL).append("\tcuid=");
                 sb.append(dict.prompt(cmd.getString(Tag.RequestedSOPClassUID)));
                 sb.append(NL).append("\tiuid=");
                 sb.append(cmd.getString(Tag.RequestedSOPInstanceUID));
                 break;
            case N_SET_RQ:
                sb.append(cmd.getInt(Tag.MessageID));
                sb.append(":N-SET-RQ[pcid=").append(pcid);
                sb.append(NL).append("\tcuid=");
                sb.append(dict.prompt(cmd.getString(Tag.RequestedSOPClassUID)));
                sb.append(NL).append("\tiuid=");
                sb.append(cmd.getString(Tag.RequestedSOPInstanceUID));
                break;
           case N_ACTION_RQ:
               sb.append(cmd.getInt(Tag.MessageID));
               sb.append(":N-ACTION-RQ[pcid=").append(pcid);
               sb.append(", actionID=").append(cmd.getInt(Tag.ActionTypeID));
               sb.append(NL).append("\tcuid=");
               sb.append(dict.prompt(cmd.getString(Tag.RequestedSOPClassUID)));
               sb.append(NL).append("\tiuid=");
               sb.append(cmd.getString(Tag.RequestedSOPInstanceUID));
               break;
            case N_CREATE_RQ:
                sb.append(cmd.getInt(Tag.MessageID));
                sb.append(":N-CREATE-RQ[pcid=").append(pcid);
                sb.append(NL).append("\tcuid=");
                sb.append(dict.prompt(cmd.getString(Tag.AffectedSOPClassUID)));
                sb.append(NL).append("\tiuid=");
                sb.append(cmd.getString(Tag.AffectedSOPInstanceUID));
                break;
            case N_DELETE_RQ:
                sb.append(cmd.getInt(Tag.MessageID));
                sb.append(":N-DELETE-RQ[pcid=").append(pcid);
                sb.append(NL).append("\tcuid=");
                sb.append(dict.prompt(cmd.getString(Tag.RequestedSOPClassUID)));
                sb.append(NL).append("\tiuid=");
                sb.append(cmd.getString(Tag.RequestedSOPInstanceUID));
                break;
            case C_CANCEL_RQ:
                sb.append(cmd.getInt(Tag.MessageIDBeingRespondedTo));
                sb.append(":C-CANCEL-RQ[pcid=").append(pcid);
                break;
            case C_STORE_RSP:
                sb.append(cmd.getInt(Tag.MessageIDBeingRespondedTo));
                sb.append(":C-STORE-RSP[pcid=").append(pcid);
                break;
            case C_GET_RSP:
                sb.append(cmd.getInt(Tag.MessageIDBeingRespondedTo));
                sb.append(":C-GET-RSP[pcid=").append(pcid);
                sb.append(", remaining=").append(cmd.getString(Tag.NumberOfRemainingSuboperations));
                sb.append(", completed=").append(cmd.getString(Tag.NumberOfCompletedSuboperations));
                sb.append(", failed=").append(cmd.getString(Tag.NumberOfFailedSuboperations));
                sb.append(", warning=").append(cmd.getString(Tag.NumberOfWarningSuboperations));
                break;
            case C_FIND_RSP:
                sb.append(cmd.getInt(Tag.MessageIDBeingRespondedTo));
                sb.append(":C-FIND-RSP[pcid=").append(pcid);
                break;
            case C_MOVE_RSP:
                sb.append(cmd.getInt(Tag.MessageIDBeingRespondedTo));
                sb.append(":C-MOVE-RSP[pcid=").append(pcid);
                sb.append(", remaining=").append(cmd.getString(Tag.NumberOfRemainingSuboperations));
                sb.append(", completed=").append(cmd.getString(Tag.NumberOfCompletedSuboperations));
                sb.append(", failed=").append(cmd.getString(Tag.NumberOfFailedSuboperations));
                sb.append(", warning=").append(cmd.getString(Tag.NumberOfWarningSuboperations));
                break;
            case C_ECHO_RSP:
                sb.append(cmd.getInt(Tag.MessageIDBeingRespondedTo));
                sb.append(":C-ECHO-RSP[pcid=").append(pcid);
                break;
            case N_EVENT_REPORT_RSP:
                sb.append(cmd.getInt(Tag.MessageIDBeingRespondedTo));
                sb.append(":N-EVENT-REPORT-RSP[pcid=").append(pcid);
                sb.append(", eventID=").append(cmd.getString(Tag.EventTypeID));
                break;
            case N_GET_RSP:
                sb.append(cmd.getInt(Tag.MessageIDBeingRespondedTo));
                sb.append(":N-GET-RSP[pcid=").append(pcid);
                break;
            case N_SET_RSP:
                sb.append(cmd.getInt(Tag.MessageIDBeingRespondedTo));
                sb.append(":N-SET-RSP[pcid=").append(pcid);
                break;
            case N_ACTION_RSP:
                sb.append(cmd.getInt(Tag.MessageIDBeingRespondedTo));
                sb.append(":N-ACTION-RSP[pcid=").append(pcid);
                sb.append(", actionID=").append(cmd.getString(Tag.ActionTypeID));
                break;
            case N_CREATE_RSP:
                sb.append(cmd.getInt(Tag.MessageIDBeingRespondedTo));
                sb.append(":N-CREATE-RSP[pcid=").append(pcid);
                break;
            case N_DELETE_RSP:
                sb.append(cmd.getInt(Tag.MessageIDBeingRespondedTo));
                sb.append(":N-DELETE-RSP[pcid=").append(pcid);
                break;
           default:
                throw new IllegalArgumentException("CommandField:"
                        + cmd.get(Tag.CommandField));
        }
        if (hasDataset(cmd))
            sb.append(NL).append("\tts=").append(dict.prompt(tsuid));
        if (isResponse(cmd))
        {
            sb.append(", status=").append(Integer.toHexString(cmd.getInt(Tag.Status))).append('H');
            if (cmd.contains(Tag.ErrorID))
                sb.append(", errorID=").append(cmd.getInt(Tag.ErrorID));
            if (cmd.contains(Tag.ErrorComment))
                sb.append(NL).append("\terror=").append(cmd.getString(Tag.ErrorComment));
        }
        sb.append(']');
        return sb.toString();
    }

}
