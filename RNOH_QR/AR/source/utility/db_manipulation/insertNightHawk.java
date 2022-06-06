
/*************************************************
*
*   Author:  Mike Bass
*   Year:    2012.
*   Purpose: Inserts record into night hawk table.
*
**************************************************/

package utility.db_manipulation;

import java.sql.*;

import org.dcm4che2.net.log_writer;
import common.manager.BasicManager;



public class insertNightHawk {

private Connection con = null;
private String logPathName = null;
private String logFileName = null;
private BasicManager mgr = null;



public insertNightHawk(BasicManager mgr, String logPathName, String logFileName){

this.mgr = mgr;
this.logPathName = logPathName;
this.logFileName = logFileName;
}



public void startOp(String[] data, String destination){

try {
	 boolean writeData = true;

	 //----------------------------------------
	 //log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "<insertnighthawk>destination]:"+destination);
     //----------------------------------------

     //----------------------------------------
	 log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "");
     //----------------------------------------

	 for(int a = 0; a < data.length; a++)
		{

         //----------------------------------------
		 log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "<insertnighthawk> data["+a+"]:"+data[a]);
         //----------------------------------------

		 if(data[a] == null)
		   {
		    //writeData = false;
		    System.out.println("<insertnighthawk> Trying to write null data to dataBase at position: "+a);
		    log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "<insertnighthawk> Trying to write null data to dataBase at position: "+a);

		    data[a] = "no value";
		   }
        }
     //-----------------------------------------
	 log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "");
      //----------------------------------------

     if(writeData)
       {
		 Class.forName("com.mysql.jdbc.Driver").newInstance();
		 //this.con = DriverManager.getConnection("jdbc:mysql://localhost:3306/autorouter_https", "root", "");

		 this.con = DriverManager.getConnection(BasicManager.DB_ROOT, "root", "");

		 if(this.con.isClosed())
		   {
		    log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "<insertnighthawk> error: con is closed!");
		   }
		 else
		   {
			Statement Stmt = this.con.createStatement();

			//siteid,
            //BasicManager.SITE_ID

            String sqlstr = "INSERT INTO autorouter_nighthawk (jobkey,siteid,destination,patientid,patientname,dob,sex,studyname,accessionnumber,modality,studydate,studytime,studystatus,downloadstatus,studynumber,islive, isqr)"
			//+ "VALUES ('"+data[0]+"','"+destination+"','"+data[1]+"','"+data[2]+"','"+data[3]+"','"+data[4]+"','"+data[5]+"','"+data[6]+"','"+data[7]+"','"+data[8]+"','"+data[9]+"','"+data[10]+"','"+data[11]+"','"+data[12]+"','"+data[13]+"','"+data[14]+"')"

            + "VALUES ('"+data[0]+"','"+BasicManager.SITE_ID+"','"+destination+"','"+data[1]+"','"+data[2]+"','"+data[3]+"','"+data[4]+"','"+data[5]+"','"+data[6]+"','"+data[7]+"','"+data[8]+"','"+data[9]+"','"+data[10]+"','"+data[11]+"','"+data[12]+"','"+data[13]+"','"+data[14]+"')"


+ " ON DUPLICATE KEY UPDATE jobkey='"+data[0]+"', "
+ " siteid= '"+BasicManager.SITE_ID+"', "
+ " destination= '"+destination+"', "
+ " patientid= '"+data[1]+"', "
+ " patientname= '"+data[2]+"', "
+ " dob= '"+data[3]+"', "
+ " sex= '"+data[4]+"', "
+ " studyname= '"+data[5]+"', "
+ " accessionnumber= '"+data[6]+"', "
+ " modality= '"+data[7]+"', "
+ " studydate= '"+data[8]+"', "
+ " studytime= '"+data[9]+"', "
+ " studystatus= '"+data[10]+"', "
+ " downloadstatus= '"+data[11]+"', "
+ " studynumber= '"+data[12]+"', "
+ " islive= '"+data[13]+"', "
+ " isqr= '"+data[14]+"' ";


			Stmt.executeUpdate(sqlstr);
			this.con.close();
			this.con = null;
		   }
       }

} catch (Exception e){
         e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "<insertnighthawk> error: "+e);
} finally {
           try {
                if(this.con != null)
                  {
                   this.con.close();
                   this.con = null;
			      }
           } catch(Exception e2){
			       e2.printStackTrace();
			       log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "<insertnighthawk> con close error: "+e2);
		   }
}
}

/*
//String sqlstr = "INSERT INTO autorouter_nighthawk (jobkey,destination,patientid,patientname,dob,sex,studyname,accessionnumber,modality,studydate,studytime,studystatus,studynumber,islive, isqr) "

public static void main(String[] args){

 String[] data = new String[11];


  data[0] ="1346008725942_24";
  data[1] ="12355";
  data[2] ="Mikky ";
  data[3] ="M";
  data[4] ="RF40000158467401";
  data[5] ="CT";
  data[6] ="2012-10-09";
  data[7] ="16:00:00";
  data[8] ="queued";
  data[9] ="1";
  data[10] ="0";


new insertNightHawk(null,null).startOp(data, "MK_TEST");
*/

 /*
 data[0] ="1347529439399_0";
 data[1] ="4322288421";
 data[2] ="SALEEM SONYA";
 data[3] ="19870324";
 data[4] ="F";
 data[5] ="MRI Lumbar Spine";
 data[9] ="1";

 new insertNightHawk(null,null).startOp(data);
 */

//}

}