
/*************************************************
*
*   Author:  Mike Bass.
*   Date:    March, 2015.
*   Purpose: Updates process pid table.
*
**************************************************/

package utility.db_manipulation;

import java.sql.*;

import org.dcm4che2.net.log_writer;


public class updatePidTable {

private Connection con = null;
private String logPathName = null;
private String logFileName = null;



public updatePidTable(String logPathName,
                      String logFileName){

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
		    System.out.println("<updatePidTable.doUpdate> Trying to update table with null data");
		    log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "<updateJobHistoryTable.doUpdate> Trying to update table with null data");
		    break;
		   }
        }

     if(writeData)
       {
		Class.forName("com.mysql.jdbc.Driver").newInstance();
		this.con = DriverManager.getConnection(common.manager.BasicManager.DB_ROOT, "root", "");
		if(con.isClosed())
		  {
		   log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "<updatePidTable.doUpdate> error: con is closed!");
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
		   this.con = null;

		   System.out.println("<updatePidTable>: table updated.");
		  }
       }

} catch (Exception e){
         e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "<updatePidTable.doUpdate> error: "+e);
} finally {
           try {
                if(this.con != null)
                  {
                   this.con.close();
                   this.con = null;
			      }
           } catch(Exception e2){
			       e2.printStackTrace();
			       log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "<updatePidTable.doUpdate> con close error: "+e2);
		   }
}
}

}