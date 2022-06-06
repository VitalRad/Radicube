
/*************************************************
*
*   Author@ Mike Bass...2012.
*
*   Purpose: Inserts record into AR results table.
*
**************************************************/

package utility.db_manipulation;

import java.sql.*;

import org.dcm4che2.net.log_writer;
import common.manager.BasicManager;



public class insertIntoARresults {

private Connection con = null;
private String logPathName = null;
private String logFileName = null;
private BasicManager mgr = null;



public insertIntoARresults(BasicManager mgr,
                           String logPathName,
                           String logFileName){

this.mgr = mgr;
this.logPathName = logPathName;
this.logFileName = logFileName;
}





public void startOp(String[] data, String jobid, String src){

try {
	 boolean writeData = true;

	 for(int a = 0; a < data.length; a++)
		{
		 if(data[a] == null)
		   {
		    //writeData = false;
		    System.out.println("<insertIntoARresults> Trying to write null data to dataBase at position: "+a);
		    log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "<insertIntoARresults> Trying to write null data to dataBase at position: "+a);

		    data[a] = "no value";
		   }
        }

     if(writeData)
       {
		 Class.forName("com.mysql.jdbc.Driver").newInstance();
		 //this.con = DriverManager.getConnection("jdbc:mysql://localhost:3306/autorouter_https", "root", "");

		 this.con = DriverManager.getConnection(BasicManager.DB_ROOT, "root", "");

		 if(this.con.isClosed())
		   {
		    log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "<insertIntoARresults> error: con is closed!");
		   }
		 else
		   {
			Statement Stmt = this.con.createStatement();

            //String sqlstr = "INSERT INTO autorouter_results (patientid,patientname,dob,sex,accessionnumber,studydesc,modality,studydate,studytime,studynumber,referringclinician,islive, jobid)"


            String sqlstr = "INSERT INTO autorouter_results (patientid,patientname,dob,sex,accessionnumber,studydesc,modality,studydate,studytime,studynumber,referringclinician,islive, jobkey, source)"
			+ "VALUES ('"+data[0]+"','"+data[1]+"','"+data[2]+"','"+data[3]+"','"+data[4]+"','"+data[5]+"','"+data[6]+"','"+data[7]+"','"+data[8]+"','"+data[9]+"','"+data[10]+"','"+"1"+"','"+jobid+"','"+src+"')";


			//=========================
			// Delete later..

			//log_writer.doLogging_QRmgr("C:/temp", "sqlCmmd.txt", sqlstr);

			log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "");
			//log_writer.doLogging_QRmgr("C:/temp", "sqlCmmd.txt", sqlstr);
			log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, sqlstr);
			log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "");
			//=========================


			Stmt.executeUpdate(sqlstr);
			this.con.close();
			this.con = null;
		   }
       }

} catch (Exception e){
         e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "<insertIntoARresults> error: "+e);
} finally {
           try {
                if(this.con != null)
                  {
                   this.con.close();
                   this.con = null;
			      }
           } catch(Exception e2){
			       e2.printStackTrace();
			       log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "<insertIntoARresults> con close error: "+e2);
		   }
}
}


/*
public static void main(String[] args){

 String[] data = new String[11];


  data[0] ="1278AB_890";
  data[1] ="Mike Bassey ";
  data[2] =" 10-03-1980";
  data[3] ="M";
  data[4] ="RF40000158467401";
  data[5] ="US-pelvic";
  data[6] ="US";
  data[7] ="20-may-2005";
  data[8] ="14:20:00";
  data[9] ="33";
  data[10] ="AUSTIN SMITH (MR)";


new insertIntoARresults(null,null).startOp(data);
}
*/

}