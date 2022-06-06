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


public class doBulkUpdate_2 {

private Connection con = null;
private String logPathName = null;
private String logFileName = null;



public doBulkUpdate_2(String logPathName, String logFileName){

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
		    //writeData = false;
		    System.out.println("<doBulkUpdate_2.doUpdate> Trying to update table with null data");
		    log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "<doBulkUpdate_2.doUpdate> Trying to update table with null data");
		    //break;

		    data[a] = "no value";
		   }
        }

     if(writeData)
       {
		if(this.con == null)
		  {
		   Class.forName("com.mysql.jdbc.Driver").newInstance();
		   this.con = DriverManager.getConnection("jdbc:mysql://localhost:3306/autorouter_https", "root", "");
		   if(con.isClosed())
		     {
		      writeData = false;
		      log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "<doBulkUpdate_2.doUpdate> error: con is closed!");
		     }
	      }

		if(writeData)
		  {
		   PreparedStatement preparedStmt = con.prepareStatement(sqlstr);
		   for(int a = 0; a < data.length; a++)
		      {
		   	   preparedStmt.setString((a + 1), data[a]);
		   	  }

		   preparedStmt.executeUpdate();

		   this.con.close();
		   this.con = null;

		   System.out.println("<doBulkUpdate_2>: Job status updated.");
		  }
       }

} catch (Exception e){
         e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "<doBulkUpdate_2.doUpdate> error: "+e);
} finally {
           try {
                if(this.con != null)
                  {
                   this.con.close();
                   this.con = null;
			      }
           } catch(Exception e2){
			       e2.printStackTrace();
			       log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "<doBulkUpdate_2.doUpdate> con close error: "+e2);
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


new doBulkUpdate_2(null, null).doUpdate(cmmd, data);
}
*/

}