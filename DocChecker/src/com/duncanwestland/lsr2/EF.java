package com.duncanwestland.lsr2;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import com.duncanwestland.chk.Crypto;

import android.nfc.tech.IsoDep;
//import android.util.Log;

public abstract class EF {
	protected byte[] efData;
	private IsoDep tcvr;
	protected static String DEBUG = "crypto";
	private String SMFail="Secure Messaging MAC authentication failed";
	protected byte[] select = {0x00,0x00}; //this must be re-set by the subclasses constructor
	private SecureMessageAPDU SMapdu = new SecureMessageAPDU();
	
	protected void setTranceiver(IsoDep tcvr) {
		this.tcvr = tcvr;
	}
	
	
	protected EF(byte[] select, IsoDep tcvr) throws IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		this.select = select;
		this.tcvr = tcvr;
		load();
	}
	
	
	/**
	 * selects a datagroup.  The select class variable must be overwritten by the subclass's constructor
	 * @throws IOException
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 * @throws InvalidAlgorithmParameterException 
	 * @throws NoSuchPaddingException 
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeyException 
	 */
	private void select() throws IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		EFResponse resp = new EFResponse();
		//calculate secure message APDU to select EF
		byte[] protectedAPDU = SMapdu.assemble(APDU.select(select),APDU.Mode.SELECT);
		Crypto.logger("select DG", protectedAPDU);
		//transmit and check response
		resp.setResponse(tcvr.transceive(protectedAPDU));
		if (!SMapdu.check(resp.data)) throw new IOException(SMFail);
		//else Log.i(DEBUG,"EF select check OK");
	}
	/**
	 * selects and reads a data group
	 * @param data
	 * @throws IOException
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 * @throws InvalidAlgorithmParameterException 
	 * @throws NoSuchPaddingException 
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeyException 
	 */
	public void load() throws IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		select(); //select the ef
		//read first bytes of EF
		EFResponse resp = new EFResponse();
		byte[] protectedAPDU = SMapdu.assemble(APDU.read((byte)0x00,(byte)0x00,(byte)0x00),APDU.Mode.READ);
		resp.setResponse(tcvr.transceive(protectedAPDU));
		if (!SMapdu.check(resp.data)) throw new IOException(SMFail);
		byte[] data = SMapdu.decryptDO87();
		Crypto.logger("EF Response data", data);	
		//tackle data that's longer than 128 bytes
		int numBit = data[1] & 0x80;
		int len;
		if (numBit==0x80) len = ((((int)data[2]&0xFF)<<8) | ((int)data[3]&0xFF)) + 4;
		else len = ((int)data[1]&0xFF) + 2;
		//Log.d(DEBUG, "EF is " + len + " bytes long");
		//Log.d(DEBUG, "chunk size = " + data.length);
		
		//read rest of EF
		int chunkSize = data.length;
		int chunks = len/chunkSize - 1; //need to take off the one we've already read
		if (chunks==0) {efData = data; return;} // no need to go further if all the data was read in one go
		int finalChunk = len - (chunks+1)*chunkSize;
		byte bytes;
		byte[] msbLsb;
		if (chunkSize != 256) bytes = (byte)chunkSize;
		else bytes = 0; //need to cope with a 256 byte chunk, which is requested by LE=0
		//Log.i(DEBUG, "chunks" + chunks + "final" +finalChunk);
		int i;
		for (i=1;i<chunks+1;i++){
			msbLsb = offset(i,chunkSize); //calculate the offset to apply
			protectedAPDU = SMapdu.assemble(APDU.read(msbLsb[0],msbLsb[1],bytes),APDU.Mode.READ);
			resp.setResponse(tcvr.transceive(protectedAPDU));
			if (!SMapdu.check(resp.data)) throw new IOException(SMFail);
			data = Crypto.concat(data, SMapdu.decryptDO87());
			//Log.d(DEBUG, "chunk " + i);
		}
		//Log.d(DEBUG, "chunks read; reading final");
		msbLsb = offset(i,chunkSize);
		protectedAPDU = SMapdu.assemble(APDU.read(msbLsb[0],msbLsb[1],(byte) finalChunk),APDU.Mode.READ);
		resp.setResponse(tcvr.transceive(protectedAPDU));
		if (!SMapdu.check(resp.data))  throw new IOException(SMFail);
		data = Crypto.concat(data, SMapdu.decryptDO87());
		//Log.d(DEBUG, "final length "+ i + data.length);
		efData = data;
	}
	private class EFResponse {
		public byte[] data;
		public byte sw1;
		public byte sw2;
		private void setResponse(byte[] resp) throws IOException {
			data = Arrays.copyOfRange(resp, 0, resp.length-2);
			sw2 = resp[resp.length-1];
			sw1 = resp[resp.length-2];
			if (sw1!=-0x70 | sw2!=0x00) {
				//Log.e(DEBUG,"response bytes weren't 0x90 0x00: " 
				//	+ Crypto.hex(new byte[]{sw1})+ Crypto.hex(new byte[]{sw2}));
				throw new IOException("response bytes weren't 0x90,0x00");
			}
		}
	}
	private byte[] offset(int j, int size) {
		long off = j*size;
		byte msb = (byte)((off >>> 8) & 0xFF);
		byte lsb = (byte)(off & 0xFF);
		return new byte[] {msb,lsb};
	}
	/**
	 * @throws NoSuchAlgorithmException 
	 * Generates a sha256 hash of the datagroup bytes
	 * @return
	 * @throws  
	 */
	public byte[] getHash() throws NoSuchAlgorithmException {
		MessageDigest hash = null;
		hash = MessageDigest.getInstance("SHA256");
		hash.update(efData);
		byte[] H = hash.digest();
		return H;
	}
	public byte[] getEncoded() {
		return efData;
	}
}
