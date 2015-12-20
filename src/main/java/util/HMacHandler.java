package util;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;

public class HMacHandler {

	private Key secretKey;
	private String algorithm;
	
	public HMacHandler(Key key){
		this.secretKey = key;
		algorithm = "HmacSHA256";
	}
	
	public byte[] computeHash(String msg) throws InvalidKeyException, NoSuchAlgorithmException{
		Mac hMac = Mac.getInstance(algorithm);
		hMac.init(secretKey);
		// MESSAGE is the message to sign in bytes
		hMac.update(msg.getBytes());
		byte[] hash = hMac.doFinal();
		return hash;
	}
	
}
