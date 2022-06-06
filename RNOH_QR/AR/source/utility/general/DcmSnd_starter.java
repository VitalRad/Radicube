/*********************************************
 *
 *   Author: Mike Bassey
 *   Year: 2012
 *   Purpose: Starts and runs a DcmSnd object.
 *
 *********************************************/

 package utility.general;

 import org.dcm4che2.tool.dcmsnd.DcmSnd;


 public class DcmSnd_starter implements Runnable {

 private DcmSnd dSend = null;
 private String[] args = null;
 private String rootName = null;
 private String logName = null;


 public DcmSnd_starter(DcmSnd dSend,
                       String[] args){
 this.dSend = dSend;
 this.args = args;
 }

 public DcmSnd_starter(DcmSnd dSend,
                       String[] args,
                       String rootName,
                       String logName){
 this.dSend = dSend;
 this.args = args;
 this.rootName = rootName;
 this.logName = logName;
 }


 public void run(){

 try {
      dSend.startOp(this.args, this.dSend, this.rootName, this.logName);

 } catch (Exception e){
          e.printStackTrace();
 }
 }

 }
