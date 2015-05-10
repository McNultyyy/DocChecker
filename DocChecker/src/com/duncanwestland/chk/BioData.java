package com.duncanwestland.chk;

import java.util.HashMap;


import android.graphics.Bitmap;
import android.text.format.DateFormat;

public class BioData {
	public static String name="";
	public static String placeOfIssue="";
	//dates are in milliseconds since epoch
	public static long dateOfExpiry;
	public static long dateOfBirth;
	public static String passportNumber="";
	public static Bitmap portrait;
	public static String sex;
	public static byte[] bacInfo; //bytes from MRZ used to make the BAC keys
	static final int MRZ_INFORMATION_LENGTH = 24;
	static final int DATE_LENGTH = 7;
	static final int DOC_LENGTH = 10;
	static final String DEBUG = "crypto";
	public static byte[] dg1Hash;
	public static byte[] dg2Hash;
	public static byte[] efSOD;
	
	/**
	* clears the personal data in the 
	* Biodata class
	* test2
	*/
	public static void clear() {
		dateOfExpiry = 0;
		dateOfBirth = 0;
		passportNumber = "";
		portrait = null;
		sex = "";
		name = "";
		placeOfIssue = "";
		bacInfo = null;
	}
	/**
	 * check MRZ makes sure the MRZ data is available for opening the passport
	 * @return
	 */
	public static boolean checkMRZ() {
		if (!passportNumber.equals("") && dateOfBirth!=0 
				&& dateOfExpiry!=0 && passportNumber.length()==9)return true;
		else return false;
	}

	/**
	 * This works out the ICAO check digit
	 * @param digits
	 * @return the check digit (byte)
	 */
	private static byte chk(byte[] digits) {
		HashMap<Character,Integer> l = new HashMap<Character,Integer>();
		Integer checkDigit = 0;
		String checkStr;
		byte check;
		l.put('0',0);
		l.put('1',1);
		l.put('2',2);
		l.put('3',3);
		l.put('3',3);
		l.put('4',4);
		l.put('5',5);
		l.put('6',6);
		l.put('7',7);
		l.put('8',8);
		l.put('9',9);
		l.put('A',10);
		l.put('B',11);
		l.put('C',12);
		l.put('D',13);
		l.put('E',14);
		l.put('F',15);
		l.put('G',16);
		l.put('H',17);
		l.put('I',18);
		l.put('J',19);
		l.put('K',20);
		l.put('L',21);
		l.put('M',22);
		l.put('N',23);
		l.put('O',24);
		l.put('P',25);
		l.put('Q',26);
		l.put('R',27);
		l.put('S',28);
		l.put('T',29);
		l.put('U',30);
		l.put('V',31);
		l.put('W',32);
		l.put('X',33);
		l.put('Y',34);
		l.put('Z',35);
		l.put('<',0);
		int i = 0;
		final int[] m = {7,3,1};
		int n;
		for (byte b: digits) {
			n = l.get((char)b);
			n*=m[i];
			i++;
			if (i==3) i=0;
			checkDigit +=n;
		}
		checkDigit = checkDigit %10;
		checkStr = checkDigit.toString();
		check = checkStr.getBytes()[0];
		return check;
	}
	/**
	 * format a Date and put it in a byte array
	 * @param date
	 * @return
	 */
	private static byte[] dateToBytes(long date) {
		byte[] dateBytes = new byte[DATE_LENGTH];
	
		String dateString = DateFormat.format("yyMMdd", date).toString();
		dateBytes = dateString.getBytes();
		return dateBytes;
	}
	/** calculates a byte array which contains the MRZ information for generating the BAC keys
	 * @return MrzInformation
	 */
	public static byte[] getMrzInfo() {
		byte[] MrzInformation = new byte[MRZ_INFORMATION_LENGTH];
		byte[] doe = new byte[DATE_LENGTH];
		byte[] dob = new byte[DATE_LENGTH];
		byte[] doc = new byte[DOC_LENGTH];
	
		dob = dateToBytes(dateOfBirth);
		dob = Crypto.concat(dob,new byte[] {chk(dob)});
		doe = dateToBytes(dateOfExpiry);
		doe = Crypto.concat(doe,new byte[] {chk(doe)});
		doc = passportNumber.getBytes();
		doc = Crypto.concat(doc,new byte[] {chk(doc)});
		MrzInformation = Crypto.concat(doc,dob);
		MrzInformation = Crypto.concat(MrzInformation,doe);
		bacInfo = MrzInformation; //store the MRZ info as it's used to look up LSR data
		return MrzInformation;
	}
}
