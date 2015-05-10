package com.duncanwestland.lsr2;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import com.duncanwestland.chk.BioData;

import android.nfc.tech.IsoDep;

public class DG1 extends EF {
	public DG1(IsoDep tcvr) throws IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		super(APDU.DG1,tcvr); //select and load data into DG1 object from chip
		//super(APDU.EFCOM,tcvr); //test
	}
	/**
	 * Decodes the data in a 2 line MRZ and populates BioData
	 * @param dg1Bytes
	 */
	public void parseBioData(){
		//TODO need to remove the  
		int i;
		byte[] dg1Bytes = Arrays.copyOfRange(efData, 5, efData.length);//strip the preamble from the MRZ data
		byte[] mrzBytes = Arrays.copyOfRange(dg1Bytes, 44, 53);
		char[] c = new char[mrzBytes.length];
		for (i=0;i<9;i++) c[i] = (char)mrzBytes[i];
		//BioData.passportNumber = clean(String.copyValueOf(c));
		//Log.i("crypto","BioData.passportNumber"+BioData.passportNumber);
		mrzBytes = Arrays.copyOfRange(dg1Bytes, 57, 63);
		//BioData.dateOfBirth = byteToDate(mrzBytes);
		//Log.i("crypto","BioData.dateOfBirth"+DateFormat.format("yyMMdd", BioData.dateOfBirth).toString());
		mrzBytes = Arrays.copyOfRange(dg1Bytes, 65,71);
		//BioData.dateOfExpiry = byteToDate(mrzBytes);
		//Log.i("crypto","BioData.dateOfExpiry"+DateFormat.format("yyMMdd", BioData.dateOfExpiry).toString());
		BioData.sex = String.valueOf((char)Arrays.copyOfRange(dg1Bytes, 64, 65)[0]); //TODO not displayed at present
		//Log.i("crypto","BioData.sex"+BioData.sex);
		mrzBytes = Arrays.copyOfRange(dg1Bytes, 5, 44);
		char[] n = new char[mrzBytes.length];
		for (i=0;i<38;i++) n[i] = (char)mrzBytes[i];
		BioData.name = clean(String.copyValueOf(n));
		//Log.i("crypto","BioData.name"+BioData.name);
	}
	private String clean(String bio) {
		String out;
		out = bio.replace('<', ' ');
		bio = out.trim();
		return bio;
	}
	/**
	private long byteToDate(byte[] dateBytes){
		Time t = new Time();
		char[] c = new char[dateBytes.length];
		for (int i=0;i<dateBytes.length;i++) c[i] = (char)dateBytes[i];
		String date = String.copyValueOf(c);
		int yr = Integer.parseInt(date.substring(0, 2));
		Calendar cal = Calendar.getInstance();
		int iYear = cal.get(Calendar.YEAR) - 1988;
		if (yr>iYear) date = "19"+ date; //assume if year>current year+12 then this is a 19xx year
		else date = "20" + date;
		t.parse(date);
		t.normalize(true);
		long ret = t.toMillis(true);
		return ret;
	}
	**/
}
