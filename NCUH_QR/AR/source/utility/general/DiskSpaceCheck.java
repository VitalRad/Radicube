/****************************************

 Author: Mike Bassey
 Date: 23/07/2012
 Purpose: Checks for remaining
 writable disk space on specified Drive.

 ****************************************/
import java.io.File;

public class DiskSpaceCheck {

private String checkLocation = null;
private long threshold = 0L;
private long spaceLeft = 0L;


public DiskSpaceCheck(String checkLocation, long threshold){

this.checkLocation = checkLocation;
this.threshold = threshold;
}


public void doCheck(){

try {
     System.out.println("this.checkLocation: "+this.checkLocation);

     File file = new File(this.checkLocation);
     //this.spaceLeft = file.getUsableSpace();

     //System.out.println("space left: "+this.spaceLeft);
     System.out.println("threshold : "+this.threshold);
     System.out.println("Total  (in bytes):   " + file.getTotalSpace());
	 System.out.println("Free   (in bytes):   " + file.getFreeSpace());
     System.out.println("Usable (in bytes):   " + file.getUsableSpace());

     if(this.spaceLeft <= this.threshold)
       {
	    System.out.println("running low on disk space!");
	   }

} catch (Exception e){
         e.printStackTrace();
}
}

/*
File file = new File("C:");
System.out.println("C:");
System.out.println("Total  (in bytes):   " + file.getTotalSpace());
System.out.println("Free   (in bytes):   " + file.getFreeSpace());
System.out.println("Usable (in bytes):   " + file.getUsableSpace());


System.out.println("Total   (in MB):  " + (file.getTotalSpace()/1000000));
System.out.println("Free    (in MB):  " + (file.getFreeSpace()/1000000));
System.out.println("Usable  (in MB):  " + (file.getUsableSpace()/1000000));


System.out.println("Total  (in GB):   " + (file.getTotalSpace() /1000000000));
System.out.println("Free   (in GB):   " + (file.getFreeSpace()/1000000000));
System.out.println("Usable (in GB):   " + (file.getUsableSpace()/1000000000));
*/





public static void main(String[] args) {

new DiskSpaceCheck("C:/mike_work/data_containers/deletionTest_all/", 100000000L).doCheck();

}
}