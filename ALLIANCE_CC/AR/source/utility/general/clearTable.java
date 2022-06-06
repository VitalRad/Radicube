
/*************************************************
*
*   Author@ Mike Bass...2012.
*
*   Purpose: Updates night hawk table.
*
**************************************************/

import java.sql.*;

import org.dcm4che2.net.log_writer;


public class clearTable {

private Connection con = null;
private String logPathName = null;
private String logFileName = null;



public clearTable(String logPathName, String logFileName){

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
		    System.out.println("<clearTable.doUpdate> Trying to update table with null data");
		    log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "<clearTable.doUpdate> Trying to update table with null data");
		    break;
		   }
        }

     if(writeData)
       {
		Class.forName("com.mysql.jdbc.Driver").newInstance();
		this.con = DriverManager.getConnection("jdbc:mysql://localhost:3306/autorouter_https", "root", "");
		if(con.isClosed())
		  {
		   log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "<clearTable.doUpdate> error: con is closed!");
		  }
		else
		  {
		   //String sqlstr = "update autorouter_nighthawk set islive = ? where jobkey = ?";


           //====================================================
           //log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "<clearTable.doUpdate> data[0]: "+data[0]);
           //log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "<clearTable.doUpdate> data[1]: "+data[1]);
           //====================================================


		   PreparedStatement preparedStmt = con.prepareStatement(sqlstr);
		   preparedStmt.setString(1, data[0]);
		   //preparedStmt.setString(2, data[1]);

		   //===============================================================
		   //log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "<clearTable.doUpdate> update string: "+sqlstr);
		   //===============================================================

		   preparedStmt.executeUpdate();

		   this.con.close();

		   System.out.println("<clearTable>: Job status updated.");
		  }
       }

} catch (Exception e){
         e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "<clearTable.doUpdate> error: "+e);
} finally {
           try {
                if(this.con != null)
                  {
                   this.con.close();
			      }
           } catch(Exception e2){
			       e2.printStackTrace();
			       log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "<clearTable.doUpdate> con close error: "+e2);
		   }
}
}



public static void main (String[] args){


String cmmd = "update autorouter_nighthawk set islive = ? ";

String[] data = new String[1];
//data[0] = "2";
data[0] = "0";


new clearTable(null, null).doUpdate(cmmd, data);
}





}