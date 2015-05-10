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

import android.graphics.BitmapFactory;
import android.nfc.tech.IsoDep;
//import android.util.Log;

public class DG2 extends EF{
	public DG2(IsoDep tcvr) throws IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		super(APDU.DG2, tcvr);
	}
	public void parseBioData() {
		//extract biometric data
		byte[] data = efData;
		int posn = 0, len = 0;
		for (int i=0; i<data.length-1;i++){
			if (data[i]==0x5F & data[i+1]==0x2E){
				len = (((int)data[i+3]) & 0xFF)<<8 | ((int)data[i+4]&0xFF);
				posn = i+5;
				break;
			}
		}
		//Log.d(DEBUG, "BDB starts at byte "+ posn + "length = "+len);
		byte[] BDB = Arrays.copyOfRange(data, posn, posn+len);
		//parse the biometric data block
		//byte[] facialRecordHeader = Arrays.copyOfRange(BDB,0,14);
		byte[] facialInformationBlock = Arrays.copyOfRange(BDB,14,34);
		int numberFeaturePointBlocks =((int)facialInformationBlock[4]&0xFF)<<8 | 
										((int)facialInformationBlock[5]&0xFF);
		int startImageInfo = numberFeaturePointBlocks*8 + 34;
		//byte[] imageInfo = Arrays.copyOfRange(BDB,startImageInfo,startImageInfo+12);
		//byte FaceImageType = imageInfo[0];
		//byte imageDataType = imageInfo[1];
		//int width = ((int)imageInfo[2]&0xFF)<<8 | ((int)imageInfo[3]&0xFF);
		//int height = ((int)imageInfo[4]&0xFF)<<8 | ((int)imageInfo[5]&0xFF);
		//byte quality = imageInfo[6];
		//Log.d(DEBUG, "FaceImageType = "+ FaceImageType + "imageDataType = " + imageDataType);
		//Log.d(DEBUG, "width = " + width + "height = " + height);
		int startImageData = startImageInfo + 12;
		byte[] imageData = Arrays.copyOfRange(BDB,startImageData, BDB.length);
		//imadeData now contains a jpeg which could be displayed
		BioData.portrait = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
	}
}
