/******************************************************
 *
 *   Author@ Mike Bass...2012.
 *
 *   Purpose: connects to, queries and extracts desired
 *            info from RIS database.
 *
 ******************************************************/

package utility.RIS_manipulation;

import java.sql.*;
import java.util.Vector;


import org.dcm4che2.net.log_writer;



public class queryRIS {


private Connection conn = null;
private String serverName = null;
private int port = -1;
private String user = null;
private String password = null;
private String SID = null;
private String staticURLString = null;
private int databaseType = -1;

private String logRoot = null;
private String logName = null;


public queryRIS(String logRoot,
                String logName){

}


public void connectToDatabase(String serverName,
                              int port,
                              String user,
                              String password,
                              String SID,
                              String staticURLString,
                              int databaseType){

try {
     this.serverName = serverName;
     this.port = port;
     this.user = user;
     this.password = password;
     this.SID = SID;
     this.staticURLString = staticURLString;
     this.databaseType = databaseType;

     /*
     if(this.databaseType == 1) //implement at real site..
       {
        DriverManager.registerDriver(new oracle.jdbc.driver.OracleDriver());
       }
       */

     String URL = this.staticURLString + serverName + ":" + port + ":" + SID;
     this.conn = DriverManager.getConnection(URL, user, password);
     System.out.println("<queryRIS> CONNECTED TO DATABASE");

     log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<queryRIS> CONNECTED TO DATABASE");


} catch (Exception e){
         e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<queryRIS.connectToDatabase() error>: "+e);
}
}


public void close_connection(){

try {
     this.conn.close();

} catch (Exception e){
         e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<queryRIS.close_connection() error>: "+e);
}
}


public Vector doExtraction_1(String SQL){

Vector<Object[]> data = null;

try {
	 Statement stat = conn.createStatement();
	 ResultSet rs = stat.executeQuery(SQL);
	 while (rs.next())
	  {
	   if(data == null)
	     {
	      data = new Vector<Object[]>(5);
	     }
	   Object[] r_data = new Object[2];
	   r_data[0] = rs.getInt(1);
	   r_data[1] = rs.getString(2);
	   data.add(r_data);
	  }
     stat.close();
} catch (Exception e){
         data = null;
         e.printStackTrace();
         log_writer.doLogging_QRmgr(this.logRoot, this.logName, "<queryRIS.doExtraction_1() error>: "+e);
}
return data;
}

}