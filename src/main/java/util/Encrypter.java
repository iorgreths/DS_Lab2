package util;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public class Encrypter {
	
	private String instance;
	private PublicKey pubKey;
	private SecretKey secKey;
	private IvParameterSpec iv;
	
	public Encrypter(String instance,PublicKey pk){
		pubKey = pk;
		secKey = null;
		this.instance = instance;
	}
	
	public Encrypter(String instance,SecretKey sk, byte[] iv){
		secKey = sk;
		pubKey = null;
		this.iv = new IvParameterSpec(iv);
		this.instance = instance;
	}
	
	public byte[] encrypt(String msg) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException{
		assert !(secKey != null && pubKey != null);
		
		byte[] cipher = msg.getBytes();
		Cipher c = Cipher.getInstance(instance);
		if(secKey != null){
			c.init(Cipher.ENCRYPT_MODE, secKey, iv);
		}else{
			c.init(Cipher.ENCRYPT_MODE, pubKey);
		}
		return c.doFinal(cipher);
		
	}

}
