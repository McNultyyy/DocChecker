package com.duncanwestland.lsr2;

import com.duncanwestland.chk.Crypto;

/**
 * This class contains useful APDU codes for talking to a passport
 * @author duncan
 *
 */
//TODO store these data in resource arrays
public class APDU {
	
	public static enum Mode {SELECT,READ};
	//file identifiers
	public static byte[] EFCOM = {0x01,0x1E};
	public static byte[] DG1 = {0x01,0x01};
	public static byte[] DG2 = {0x01,0x02};
	public static byte[] EFSOD = {0x01,0x1D};
	
	public static byte[] reqRNDICC() {
		//{0x00,0x84,0x00,0x00,0x08}
		byte[] ret = {0x00,-0x7C,0x00,0x00,0x08};
		return ret;
	}
	
	public static byte[] mutualAuthenticate(byte[] cmdData) {
		byte[] b1 = {0x00,-0x7E,0x00,0x00,0x28};
		byte[] b2 = {0x28};
		byte[] ret = new byte[b1.length+cmdData.length+b2.length];
		ret = Crypto.concat(b1, cmdData);
		ret = Crypto.concat(ret, b2);
		return ret;
	}
	
	public static byte[] selectIssuerApplication() {
		byte[] ret={0x00,-0x5C,0x04,0X0C,0x07,-0x60,0x00,0x00,0x02,0x47,0x10,0x01};
		return ret;
	}
	
	public static byte[] select(byte[] fileID) {
		byte[] b1 = {0x00,-0x5C,0x02,0x0C,0x02};
		byte[] ret = Crypto.concat(b1,fileID);
		return ret;
	}
	public static byte[] read(byte offsetMSB, byte offsetLSB, byte bytes) {
		byte[] b1 = {0x00,-0x50};
		byte[] ret = Crypto.concat(b1, new byte[] {offsetMSB});
		ret = Crypto.concat(ret, new byte[] {offsetLSB});
		ret = Crypto.concat(ret, new byte[] {bytes});
		return ret;
	}
}
