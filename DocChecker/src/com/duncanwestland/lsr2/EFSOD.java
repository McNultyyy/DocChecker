package com.duncanwestland.lsr2;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import android.nfc.tech.IsoDep;

public class EFSOD extends EF {
	
	public EFSOD(IsoDep tcvr) throws IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		super(APDU.EFSOD,tcvr);
	}
	/**
	 * returns the bytes representing EF.COM with the
	 * initial tag and length bytes stripped off
	 */
	@Override
	public byte[] getEncoded() {
		if (this.efData[0]!=0x77) return null;
		int l = efData.length;
		byte[] encoded;
		switch (efData[1]) {
			case (byte) 0x82:
				encoded = Arrays.copyOfRange(efData, 4, l);
				break;
			case (byte) 0x81:
				encoded = Arrays.copyOfRange(efData, 3, l);
				break;				
			default:
				encoded = Arrays.copyOfRange(efData, 2, l);
			}
		return encoded;
	}
}
