
/*************************************************
*
*   Author@ Mike Bass
*   March, 2012.
*   Purpose: Cleans up table upon crash.
*
**************************************************/

package utility.db_manipulation;

import java.sql.*;

import org.dcm4che2.net.log_writer;
import common.manager.crashHandler;


public class crashUpdate {

private Connection con = null;
private String logPathName = null;
private String logFileName = null;
private crashHandler mgr = null;



public crashUpdate(crashHandler mgr,
	               String logPathName,
                   String logFileName){

this.mgr = mgr;
this.logPathName = logPathName;
this.logFileName = logFileName;
}


public void doUpdate(String sqlstr)
{
	try
	{
	 Class.forName(crashHandler.DB_CNAME).newInstance();
	 this.con = DriverManager.getConnection(crashHandler.DB_ROOT, "root", "");

	 //Class.forName("com.mysql.jdbc.Driver").newInstance();
	 //this.con = DriverManager.getConnection("jdbc:mysql://localhost:3306/autorouter_https", "root", "");

	 if(con.isClosed())
	   {
		log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "<crashUpdate.doUpdate> error: con is closed!");
	   }
	 else
	   {
		PreparedStatement preparedStmt = con.prepareStatement(sqlstr);
		preparedStmt.executeUpdate();
		this.con.close();
		this.con = null;
		System.out.println("<crashUpdate>: table updated.");
	   }

	 } catch (Exception e){
			 e.printStackTrace();
			 log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "<crashUpdate.doUpdate> error: "+e);
	} finally {
			   try {
					if(this.con != null)
					  {
					   this.con.close();
					   this.con = null;
					  }
			   } catch(Exception e2){
					   e2.printStackTrace();
					   log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "<crashUpdate.doUpdate> con close error: "+e2);
			   }
	//System.exit(0); //exit stage..
	}
}
}