
/****************************************************
*
*   Author@ Mike Bass...2012.
*
*   Purpose: Does batch insert into night hawk table.
*
*****************************************************/

package utility.db_manipulation;

import java.sql.*;

import org.dcm4che2.net.log_writer;
import common.manager.BasicManager;

public class insertBatch_nightHawkTable {


private String logPathName = null;
private String logFileName = null;
private Connection connection = null;
private Object[] dataToInsert = null;
private boolean errorOcurred = false;


public insertBatch_nightHawkTable(String logPathName,
                                  String logFileName,
                                  Object[] dataToInsert){

this.logPathName = logPathName;
this.logFileName = logFileName;
this.dataToInsert = dataToInsert;
}

public boolean doInsert(){
try {
   Class.forName("com.mysql.jdbc.Driver").newInstance();
   //this.connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/autorouter_https", "root", "");

   this.connection = DriverManager.getConnection(BasicManager.DB_ROOT, "root", "");

   if(this.connection.isClosed())
   	 {
   	  log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "<insertBatch_nightHawkTable.doInsert()> error: con is closed!");
	 }
   else
     {
	  // Disable auto-commit
	  connection.setAutoCommit(false);

	  // Create a prepared statement
	  //String sql = "INSERT INTO autorouter_nighthawk (jobkey,destination,patientid,patientname,dob,sex,studyname,accessionnumber,modality,studydate,studytime,studystatus,downloadstatus,studynumber,islive, isqr) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";


      String sql = "INSERT INTO autorouter_nighthawk (jobkey,siteid,destination,patientid,patientname,dob,sex,studyname,accessionnumber,modality,studydate,studytime,studystatus,downloadstatus,studynumber,islive,isqr) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE jobkey=?,siteid=?,destination=?,patientid=?,patientname=?,dob=?,sex=?,studyname=?,accessionnumber=?,modality=?,studydate=?,studytime=?,studystatus=?,downloadstatus=?,studynumber=?,islive=?,isqr=?";

	  PreparedStatement pstmt = connection.prepareStatement(sql);

	  // Insert rows of data
	  for(int i = 0; i < this.dataToInsert.length; i++)
	     {
		  String[] data = (String[]) this.dataToInsert[i];
		  int pointer = -1;
		  int counter = 0;
		  for(int a = 0; a < (data.length + 1); a++)
			 {
			  pointer++;
			  if(a == 1) //quick hack: sneak in siteid..
			    {
			     pstmt.setObject((pointer + 1), BasicManager.SITE_ID);

			     //=============================
			     //log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "<insertBatch_nightHawkTable.doInsert()> Pos: "+pointer+" = "+BasicManager.SITE_ID);
			     //==============================
			    }
			  else
			    {
			     pstmt.setObject((pointer + 1), data[counter]);

			     //=============================
				 //log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "<insertBatch_nightHawkTable.doInsert()> Pos: "+pointer+" = "+data[counter]);
			     //==============================
			     counter++;
			    }
			 }

          counter = 0;
          for(int a = 0; a < (data.length + 1); a++)
			 {
			  pointer++;
			  if(a == 1)
			    {
			     pstmt.setObject((pointer + 1), BasicManager.SITE_ID);
			    //=============================
			    //log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "<insertBatch_nightHawkTable.doInsert()> Pos: "+pointer+" = "+BasicManager.SITE_ID);
			    //==============================
			    }
			  else
			    {
			     pstmt.setObject((pointer + 1), data[counter]);

			     //=============================
				 //log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "<insertBatch_nightHawkTable.doInsert()> Pos: "+pointer+" = "+data[counter]);
			     //==============================
			     counter++;
			    }
			 }
		  pstmt.addBatch();
	     }

	   // Execute the batch
	   int [] updateCounts = pstmt.executeBatch();

	   // All statements were successfully executed.
	   // updateCounts contains one element for each batched statement.
	   // updateCounts[i] contains the number of rows affected by that statement.
	   processUpdateCounts(updateCounts);

	   // Since there were no errors, commit
	   connection.commit();
     }
} catch (BatchUpdateException e) {

    e.printStackTrace();

    log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "<insertBatch_nightHawkTable.doInsert()> batchUpdate error: "+e);

    // Not all of the statements were successfully executed
    int[] updateCounts = e.getUpdateCounts();

    // Some databases will continue to execute after one fails.
    // If so, updateCounts.length will equal the number of batched statements.
    // If not, updateCounts.length will equal the number of successfully executed statements
    processUpdateCounts(updateCounts);

    // Either commit the successfully executed statements or rollback the entire batch
    try {
         this.errorOcurred = true;
         //System.out.println("we got here!");
         log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "<insertBatch_nightHawkTable.doInsert()> msg: Insertion rolled back!");
         connection.rollback();
    } catch (Exception cmt){
	         cmt.printStackTrace();
	         log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "<insertBatch_nightHawkTable.doInsert()> rollback error: "+cmt);
	}

} catch (SQLException e) {
         log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "<insertBatch_nightHawkTable.doInsert()> error: "+e);
} catch (Exception gen_err){
         gen_err.printStackTrace();
         log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "<insertBatch_nightHawkTable.doInsert()> error: "+gen_err);
} finally {
           if(this.connection != null)
             {
			  try {
			       this.connection.close();
			       this.connection = null;
		     } catch (Exception cls_err){
			          log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "<updateAsBatch.doUpdate()> closing error: "+cls_err);
			 }
			 }
}
return this.errorOcurred;
}


public static void processUpdateCounts(int[] updateCounts) {
    for (int i=0; i<updateCounts.length; i++) {
    	if (updateCounts[i] >= 0) {
    		// Successfully executed; the number represents number of affected rows
    	} else if (updateCounts[i] == Statement.SUCCESS_NO_INFO) {
    		// Successfully executed; number of affected rows not available
    	} else if (updateCounts[i] == Statement.EXECUTE_FAILED) {
    		// Failed to execute
    	}
    }
}


/*
public static void main (String[] args){

//String sqlstr = "INSERT INTO autorouter_nighthawk (jobkey,destination,patientid,patientname,dob,sex,studyname,accessionnumber,modality,studydate,studytime,studystatus,downloadstatus,studynumber,islive, isqr)"


Object[] dToInsert = new Object[2000];

for(int a = 0; a < 2000; a++)
   {
	String[] data_1 = new String[16];
	data_1[0] = "tkey_"+a;
	data_1[1] = "t_4whc";
	data_1[2] = "45011"+a;
	data_1[3] = "mike bass_"+a;
	data_1[4] = "10-May-1990";
	data_1[5] = "M";
	data_1[6] = "CT";
	data_1[7] = "test_accNo";
	data_1[8] = "CT pelvis";
	data_1[9] = "20-Dec-2008";
	data_1[10] = "08:00:01";
	data_1[11] = "just testing";
	data_1[12] = "just testing";
	data_1[13] = "0";
	data_1[14] = "1";
	data_1[15] = "1";

	dToInsert[a] = data_1;
   }

new insertBatch_nightHawkTable(dToInsert).doInsert();
}
*/

}