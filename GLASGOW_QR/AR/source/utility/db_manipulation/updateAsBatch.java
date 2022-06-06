
/****************************************************
*
*   Author@ Mike Bass...2012.
*
*   Purpose: Does batch table updates.
*
*****************************************************/

package utility.db_manipulation;

import java.sql.*;

import org.dcm4che2.net.log_writer;
import common.manager.BasicManager;


public class updateAsBatch {


private String logPathName = null;
private String logFileName = null;
private Connection connection = null;
private Object[] dataToInsert = null;
private boolean errorOcurred = false;
private String sqlStr = null;


public updateAsBatch(String logPathName,
                     String logFileName,
                     Object[] dataToInsert,
                     String sqlStr){

this.logPathName = logPathName;
this.logFileName = logFileName;
this.dataToInsert = dataToInsert;
this.sqlStr = sqlStr;
}

public boolean doUpdate(){
try {
   Class.forName("com.mysql.jdbc.Driver").newInstance();
   //this.connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/autorouter_https", "root", "");

   this.connection = DriverManager.getConnection(BasicManager.DB_ROOT, "root", "");

   if(this.connection.isClosed())
   	 {
   	  log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "<updateAsBatch.doUpdate()> error: con is closed!");
	 }
   else
     {
	  // Disable auto-commit
	  connection.setAutoCommit(false);

	  // Create a prepared statement
	  PreparedStatement pstmt = connection.prepareStatement(this.sqlStr);

	  // Insert rows of data
	  for(int i = 0; i < this.dataToInsert.length; i++)
	     {
		  String[] data = (String[]) this.dataToInsert[i];
		  for(int a = 0; a < data.length; a++)
			 {
			  pstmt.setObject((a + 1), data[a]);
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

    log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "<updateAsBatch.doUpdate()> batchUpdate error: "+e);

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
         log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "<updateAsBatch.doUpdate()> msg: doUpdate rolled back!");
         connection.rollback();
    } catch (Exception cmt){
	         cmt.printStackTrace();
	         log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "<updateAsBatch.doUpdate()> rollback error: "+cmt);
	}

} catch (SQLException e) {
         log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "<updateAsBatch.doUpdate()> error: "+e);
} catch (Exception gen_err){
         gen_err.printStackTrace();
         log_writer.doLogging_QRmgr(this.logPathName, this.logFileName, "<updateAsBatch.doUpdate()> error: "+gen_err);
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

}