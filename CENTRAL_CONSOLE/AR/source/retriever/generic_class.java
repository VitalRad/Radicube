/********************************************
 *
 *     Author: Mike bass,
 *     Year: 2010.
 *     App name: generic_class .
 *
 *     Description: This class holds common
 *     functionalities needed by all.
 *
 ********************************************/


 package retriever;



 public class generic_class {



 public generic_class(){}





  //---------------------------
  //   Buffer encoder methods:
  //---------------------------


  //Method to write long...
  public static byte[] bufWriteLong(long value, byte[]buf) {

	 buf[0] =(byte)(0xff & (value >> 56));
	 buf[1] =(byte)(0xff & (value >> 48));
	 buf[2] =(byte)(0xff & (value >> 40));
	 buf[3] =(byte)(0xff & (value >> 32));
	 buf[4] =(byte)(0xff & (value >> 24));
	 buf[5] =(byte)(0xff & (value >> 16));
	 buf[6] =(byte)(0xff & (value >>  8));
	 buf[7] =(byte)(0xff & value);

	 return buf;
  }





  //Method to write int...
   public static  byte[] bufWriteInt(int value, byte[] buf) {

     buf[0] = (byte)((value >> 24) & 0xFF);
     buf[1] = (byte)((value >> 16) & 0xFF);
     buf[2] = (byte)((value >>  8) & 0xFF);
     buf[3] = (byte)((value >>  0) & 0xFF);

     return buf;
  }



  //Method to write short...
  public static  byte[] bufWriteShort(int value, byte[] buf) {

    buf[0] = (byte)((value >> 8) & 0xFF);
    buf[1] = (byte)((value >> 0) & 0xFF);

    return buf;
  }






  //Method to write byte..
  public static  byte[] bufWriteByte(int value, byte[] buf) {

     buf[0] = (byte) value;

     return buf;
  }






  //--------------------------
  //  Buffer decoder methods:
  //--------------------------


   //Method to read long value....
   public static long Read_longVALUE(byte[] buff){

    long req_longValue = (((long)(buff[0] & 0xff) << 56) |
                          ((long)(buff[1] & 0xff) << 48) |
                          ((long)(buff[2] & 0xff) << 40) |
                          ((long)(buff[3] & 0xff) << 32) |
                          ((long)(buff[4] & 0xff) << 24) |
                          ((long)(buff[5] & 0xff) << 16) |
                          ((long)(buff[6] & 0xff) <<  8) |
                          ((long)(buff[7] & 0xff)));

    return req_longValue;
   }



 //Method to read int value....
 public static int read_intVALUE(byte[] buff){

	int req_intValue =  (int)((buff[0] & 0xff) << 24) |
							 ((buff[1] & 0xff) << 16) |
							 ((buff[2] & 0xff) <<  8) |
							 ((buff[3] & 0xff) <<  0);
	return req_intValue;
 }



//Method to read short value....
public static short read_shortVALUE(byte[] buff){

   short req_shortValue = (short)((buff[0] << 8) |
								  (buff[1] & 0xff));
   return req_shortValue;
}




//Method to read byte value....
public static byte read_byteVALUE(byte[] buff) {

  byte req_byteValue = (byte) buff[0];

  return req_byteValue;
}






//Encodes int value into an int primitive, then returns the primitive.
public static int encodeIntValue(int primitiveField, int valueToEncode, int numOfShifts){

primitiveField = (int) ((primitiveField & 0xFFFFFFFF) | valueToEncode << numOfShifts);

return primitiveField;

}


//Encodes short value into a short primitive, then returns the primitive.
public static short encodeShortValue(short primitiveField, int valueToEncode, int numOfShifts){

primitiveField = (short) ((primitiveField & 0xFFFF) | valueToEncode << numOfShifts);

return primitiveField;

}



//Encodes byte value into a byte primitive, then returns the primitive.
public static byte encodeByteValue(byte primitiveField, int valueToEncode, int numOfShifts){

primitiveField = (byte) ((primitiveField & 0xFF) | valueToEncode << numOfShifts);

return primitiveField;

}




//Extracts the wanted number of bits from a particular int primitive,
//decodes and return the value of the decoded bits.
public static int decodeIntValue(int primitiveField, int numOfBitsToDecode, int numOfShifts){

int r_val = -1;

r_val = (int) (((primitiveField >>> numOfShifts)) & generate_BitMask(numOfBitsToDecode));

return r_val;

}



//Extracts the wanted number of bits from a particular short primitive,
//decodes and return the value of the decoded bits.
public static short decodeShortValue(short primitiveField, int numOfBitsToDecode, int numOfShifts){

short r_val = -1;

r_val = (short) (((primitiveField >>> numOfShifts)) & generate_BitMask(numOfBitsToDecode));

return r_val;

}



//Extracts the wanted number of bits from a particular byte primitive,
//decodes and return the value of the decoded bits.
public static byte decodeByteValue(byte primitiveField, int numOfBitsToDecode, int numOfShifts){

byte r_val = -1;

r_val = (byte) (((primitiveField >>> numOfShifts)) & generate_BitMask(numOfBitsToDecode));

return r_val;

}




//Method to generate a bitMask for any arbitrary number of bits
public static int generate_BitMask(int no_ofBits){

 int bitMaskMultiplier  = 0;
 int bitMaskIncrementer = 0;
 int bitMaskValue       = 0;
 int forCounter         = 0;

 for(forCounter = 0; forCounter < no_ofBits; forCounter++)
	{
	if(forCounter == 0)
	  {
	   bitMaskMultiplier  = 1;
	   bitMaskIncrementer = (1 * bitMaskMultiplier);
	   bitMaskValue      += (bitMaskIncrementer);
	   bitMaskMultiplier  = (2 * bitMaskMultiplier);
	  }
	else
	  {
	   bitMaskIncrementer = (1 * bitMaskMultiplier);
	   bitMaskValue      += (bitMaskIncrementer);
	   bitMaskMultiplier  = (2 * bitMaskMultiplier);
	  }
	}
  return bitMaskValue;
}





public static void sleepForDesiredTime(int duration){

	try {
		 Thread.sleep(duration);

	} catch (InterruptedException ef) {
			 System.out.println("==== generic_class <sleepForDesiredTime> error: "+ef);
	}
}




}//end of class.