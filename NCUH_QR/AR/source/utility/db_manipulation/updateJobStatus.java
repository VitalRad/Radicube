
/******************************************************
*
*   Author@ Mike Bass...2012.
*
*   Purpose: Updates autoResults table.
*
*******************************************************/

package utility.db_manipulation;

import java.sql.*;

import org.dcm4che2.net.log_writer;
import common.manager.BasicManager;


public class updateJobStatus {

private Connection con = null;
private String logPathName = null;
private String logFileName = null;
private BasicManager mgr = null;


public updateJobStatus(BasicManager mgr,
	                   String logPathName,
                       String logFileName){

this.mgr = mgr;
this.logPathName = logPathName;
this.logFileName = logFileName;
}


public void doUpdate(String[] data){

try {
	 /*
	 //////////////////////
	 System.out.println("data length: "+data.length);

     for(int k = 0; k < data.length; k++)
        {
         System.out.println("data["+k+"]: "+data[k]);
	    }
	 /////////////////////
	 */


	 boolean writeData = true;

	 for(int a = 0; a < data.length; a++)
		{
		 if(data[a] == null)
		   {
		    writeData = false;
		    System.out.println("<updateJobStatus.doUpdate> Trying to update table with null data");

		    //log_writer.doLogging_QRmgr(3, "<updateJobStatus.doUpdate> Trying to update table with null data");

		    log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "<updateJobStatus.doUpdate> Trying to update table with null data");

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
		   //log_writer.doLogging_QRmgr(3, "<updateJobStatus.doUpdate> error: con is closed!");

		   log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "<updateJobStatus.doUpdate> error: con is closed!");
		  }
		else
		  {

		   //update autorouter_jobs set jobstatus='SEARCH_FAILED', errstr='Could not find patientid' where id='1'

           if(data.length == 2)
             {
		      String sqlstr = "update autorouter_jobs set jobstatus = ? where id = ?";
		      PreparedStatement preparedStmt = con.prepareStatement(sqlstr);
		      preparedStmt.setString(1, data[0]);
		      preparedStmt.setInt(2, Integer.parseInt(data[1]));
		      preparedStmt.executeUpdate();
		      this.con.close();
		      System.out.println("<updateJobStatus>: Job status updated.");
		     }
		   else if(data.length == 3)
		     {
		   	  String sqlstr = "update autorouter_jobs set jobstatus = ?, errstr = ? where id = ?";
		   	  PreparedStatement preparedStmt = con.prepareStatement(sqlstr);
		      preparedStmt.setString(1, data[0]);
		      preparedStmt.setString(2, data[1]);
		   	  preparedStmt.setInt(3, Integer.parseInt(data[2]));
		   	  preparedStmt.executeUpdate();
		   	  this.con.close();
		   	  this.con = null;
		   	  System.out.println("<updateJobStatus>: Job status updated.");
		     }
		  }
       }

} catch (Exception e){
         e.printStackTrace();
         //log_writer.doLogging_QRmgr(3, "<updateJobStatus.doUpdate> error: "+e);

         log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "<updateJobStatus.doUpdate> error: "+e);
} finally {
           try {
                if(this.con != null)
                  {
                   this.con.close();
                   this.con = null;
			      }
           } catch(Exception e2){
			       e2.printStackTrace();
			       //log_writer.doLogging_QRmgr(3, "<updateJobStatus.doUpdate> con close error: "+e2);

			       log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "<updateJobStatus.doUpdate> con close error: "+e2);
		   }
}
}

}