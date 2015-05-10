package com.duncanwestland.chk;

import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;


//import android.util.Log;

/** contains some general tools for data manipulation
 * which don't exist as standard in Java
 * @author duncan
 *
 */
public class Crypto {
	  private static final String DES3_ENCRYPTION_KEY_TYPE = "DESede";
	  private static final String DES_ENCRYPTION_KEY_TYPE = "DES";
	  private static final String DES3_CBC = "DESede/CBC/NoPadding";
	  private static final String DES_CBC = "DES/CBC/NoPadding";
	  private static final int DES_KEY_LENGTH = 24;
	  //private static String DEBUG = ReaderStartUpActivity.DEBUG;
	  /**
	  static{
		  Security.addProvider(new org.spongycastle.jce.provider.BouncyCastleProvider());
		  Log.i(DEBUG,"security provider SC set");
	  }
	  */
	  
	  /** concatenates two byte arrays.  Amazingly this isn't a standard Java function
	 * 
	 * @param A
	 * @param B
	 * @return concatenation of A and B
	 */
	public static byte[] concat(byte[] A, byte[] B) {
		if (A==null & B==null) return null;
		else if (A==null) return B;
		else if (B==null) return A;
		else{
			byte[] C= new byte[A.length+B.length];
			System.arraycopy(A, 0, C, 0, A.length);
			System.arraycopy(B, 0, C, A.length, B.length);
			return C;
		}
	}		

	
	/**
	 * makes ICAO BAC keys from a  key seed
	 * @return
	 * @throws NoSuchAlgorithmException 
	 */
	public static byte[] makeKeys(byte[] Kseed,byte[] c) throws NoSuchAlgorithmException {
		byte [] D = concat(Kseed,c);
		byte [] H = hash(D);
		byte[] Ka = Arrays.copyOf(H,8);
		byte[] Kb = Arrays.copyOfRange(H, 8, 16);
		Ka = desParity(Ka);
		Kb = desParity(Kb);
		byte[] Knew = concat(Ka,Kb);
		return Knew;
	}
	/** 
	 * Adjusts the parity of a byte array to form a DES key
	 * @param k
	 * @return
	 */
	private static byte[] desParity(byte[] k) {
		byte[] Knew=null;
		for (byte b: k) {
		    byte mask = 0x01;
		    boolean parity = true;
		    for (byte bit=0; bit<8; bit ++) {
		    	if ((byte)(mask & b) != 0) parity = ! parity;
		    	mask = (byte)(mask << 1); //the 0xFF just make sure the shift works correctly as the int is representing an unsigned byte
		    }		
		    if (parity) b = (byte)(b ^ 0x01);
		    Knew = Crypto.concat(Knew, new byte[]{b}); 
		}
		return Knew;
	}
	/**
	 * Computes the sha 1 hash of the a byte array; provides a simple interface
	 * @param data
	 * @return
	 * @throws NoSuchAlgorithmException 
	 */
	public static byte[] hash(byte[] data) throws NoSuchAlgorithmException{
		byte[] H = null;
		MessageDigest hash = MessageDigest.getInstance("SHA");
		hash.update(data);
		H = hash.digest();
		return H;
	}
	/**
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 * @throws InvalidAlgorithmParameterException 
	 * @throws InvalidKeyException 
	 * @throws NoSuchPaddingException 
	 * @throws NoSuchAlgorithmException 
	 * Perform DES3 encryption or decryption
	 * @param original
	 * @param mode (cipher.ENCRYPT_MODE), cipher.DECRYPT_MODE
	 * @return
	 * @throws GeneralSecurityException 
	 * @throws  
	 */
	public static byte[] DES3(byte[] original, int mode, byte[] key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException{
		//ICAO assumes 2key 3DES  (key1 =key3) but Java needs 3 key 3DES
		//very explicit error catching for ease of debugging
		byte[] threeKey = new byte[DES_KEY_LENGTH];
		int j,k;
		for (j=0;j<16;j++) threeKey[j]= key[j];
		for (j=0, k=16; j<8;){
			threeKey[k++] = threeKey[j++];
		}
	    SecretKeySpec keySpec = new SecretKeySpec(threeKey, DES3_ENCRYPTION_KEY_TYPE);
		Cipher cipher = null;
		cipher = Cipher.getInstance(DES3_CBC);
		IvParameterSpec iv = new IvParameterSpec(new byte[] { 0, 0, 0, 0, 0, 0, 0, 0 });
		cipher.init(mode, keySpec, iv);
		return cipher.doFinal(original);
	}
	/**
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 * @throws InvalidAlgorithmParameterException 
	 * @throws InvalidKeyException 
	 * @throws NoSuchPaddingException 
	 * @throws NoSuchAlgorithmException 
	 * Perform DES encryption or decryption
	 * @param original
	 * @param mode (cipher.ENCRYPT_MODE), cipher.DECRYPT_MODE
	 * @return
	 * @throws GeneralSecurityException 
	 * @throws  
	 */
	public static byte[] DES(byte[] original, int mode,String method, byte[] key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
	    SecretKeySpec keySpec = new SecretKeySpec(key, DES_ENCRYPTION_KEY_TYPE);
		Cipher cipher = null;
		cipher = Cipher.getInstance(method);
		IvParameterSpec iv = new IvParameterSpec(new byte[] { 0, 0, 0, 0, 0, 0, 0, 0 });
		cipher.init(mode, keySpec, iv);
		return cipher.doFinal(original);
	}
	/**
	 * calculates an ISO retail MAC
	 * @param k
	 * @param message
	 * @return
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 * @throws InvalidAlgorithmParameterException 
	 * @throws NoSuchPaddingException 
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeyException 
	 */
	public static byte[] MAC(byte[] k,byte[] message) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException{
		
		message = pad(message); 
		byte[] ka = Arrays.copyOfRange(k, 0, 8);
		byte[] kb = Arrays.copyOfRange(k, 8, k.length);
		byte[] cipher = null;
		cipher = DES(message, Cipher.ENCRYPT_MODE,Crypto.DES_CBC,ka);
		cipher = Arrays.copyOfRange(cipher, cipher.length-8, cipher.length);
		//Log.v(DEBUG,"cipher "+ hex(cipher));
		byte[] code = DES(cipher,Cipher.DECRYPT_MODE,Crypto.DES_CBC,kb);
		//Log.v(DEBUG,"code "+ hex(code));
		cipher = DES(code,Cipher.ENCRYPT_MODE,Crypto.DES_CBC,ka);
		//Log.v(DEBUG,"cipher "+ hex(cipher));
		return cipher;
	}
	/**
	 * pads message according to ISO/IEC9797-1 method 2
	 * @return
	 */
	public static byte[] pad(byte[] message){
		byte[] pad = {-0x80,0x00,0x00,0x00,0x00,0x00,0x00,0x00};
		//if (message.length %8 == 0) return message; //no padding needed!
		byte[] paddedMessage = Crypto.concat(message,pad);
		//TODO this is an inefficient way to cut down the message
		while (paddedMessage.length  %8 != 0) paddedMessage = Arrays.copyOfRange(paddedMessage, 0, paddedMessage.length-1);
		return paddedMessage;
	}
	/**returns a hexadecimal string representation of a byte array.
	 * 
	 * @param b
	 * @return
	 */
	public static String hex(byte[] b){
		String result = "";
		for (int i=0; i < b.length; i++) {
			result += Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
		}
		return result;
	} 
	
	/**
	 * Helper function that logs appropriately to LogCat
	 * @param var
	 * @param ret
	 * @param icao
	 */
	public static void logger(String var, byte[]ret) {
		//Log.d(DEBUG,var +" = " + Crypto.hex(ret));
	}
}
