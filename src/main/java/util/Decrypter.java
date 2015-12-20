package util;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public class Decrypter {

	private String instance;
	private PrivateKey privKey;
	private SecretKey secKey;
	private IvParameterSpec iv;
	
	public Decrypter(String instance, PrivateKey pk){
		this.instance = instance;
		privKey = pk;
		secKey = null;
	}
	
	public Decrypter(String instance, SecretKey sk, byte[] iv){
		this.instance = instance;
		secKey= sk;
		privKey = null;
		this.iv = new IvParameterSpec(iv);
	}
	
	public byte[] decrypt(String msg) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException{
		assert !(secKey != null && privKey != null);
		
		return internalDecrypt(msg.getBytes());
	}
	
	public byte[] decrypt(byte[] msg) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException{
		assert !(secKey != null && privKey != null);
		
		return internalDecrypt(msg);
		
	}
	
	private byte[] internalDecrypt(byte[] cipher) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException{
		Cipher c = Cipher.getInstance(instance);
		if(secKey != null){
			c.init(Cipher.DECRYPT_MODE, secKey, iv);
		}else{
			c.init(Cipher.DECRYPT_MODE, privKey);
		}
		return c.doFinal(cipher);
	}
	
}
