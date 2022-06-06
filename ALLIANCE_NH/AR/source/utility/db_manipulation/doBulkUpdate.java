/***************************************************
*
*  Author@ Mike Bass...2012.
*
*  Purpose: Conducts desired update on all records.
*
****************************************************/

package utility.db_manipulation;

import java.sql.*;

import org.dcm4che2.net.log_writer;
import common.manager.BasicManager;



public class doBulkUpdate {

private Connection con = null;
private String logPathName = null;
private String logFileName = null;



public doBulkUpdate(String logPathName, String logFileName){

this.logPathName = logPathName;
this.logFileName = logFileName;
}


public void doUpdate(String sqlstr, String[] data){

try {
	 boolean writeData = true;

	 for(int a = 0; a < data.length; a++)
		{
		 if(data[a] == null)
		   {
		    writeData = false;
		    System.out.println("<doBulkUpdate.doUpdate> Trying to update table with null data");
		    log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "<doBulkUpdate.doUpdate> Trying to update table with null data");
		    break;
		   }
        }

     if(writeData)
       {
		Class.forName("com.mysql.jdbc.Driver").newInstance();
		//this.con = DriverManager.getConnection("jdbc:mysql://localhost:3306/autorouter_https", "root", "");

		this.con = DriverManager.getConnection(BasicManager.DB_ROOT, "root", "");
		if(con.isClosed())
		  {
		   log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "<doBulkUpdate.doUpdate> error: con is closed!");
		  }
		else
		  {
		   PreparedStatement preparedStmt = con.prepareStatement(sqlstr);
		   for(int a = 0; a < data.length; a++)
		      {
		   	   preparedStmt.setString((a + 1), data[a]);
		   	  }

		   preparedStmt.executeUpdate();

		   this.con.close();

		   System.out.println("<doBulkUpdate>: Job status updated.");
		  }
       }

} catch (Exception e){
         e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "<doBulkUpdate.doUpdate> error: "+e);
} finally {
           try {
                if(this.con != null)
                  {
                   this.con.close();
			      }
           } catch(Exception e2){
			       e2.printStackTrace();
			       log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "<doBulkUpdate.doUpdate> con close error: "+e2);
		   }
}
}


/*
public static void main (String[] args){


//String cmmd = "update autorouter_nighthawk set islive = ? ";

String cmmd = "update autorouter_nighthawk set studystatus = ? where islive = ?";

String[] data = new String[2];
data[0] = "paused";
data[1] = "1";


new doBulkUpdate(null, null).doUpdate(cmmd, data);
}
*/

}