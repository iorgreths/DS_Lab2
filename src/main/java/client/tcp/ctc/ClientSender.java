package client.tcp.ctc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bouncycastle.util.encoders.Base64;

import util.HMacHandler;

public class ClientSender {
	
	private Map<String,Socket> userTargets; //NOTE: username, connection
	
	public ClientSender(){
		userTargets = new HashMap<String,Socket>();
	}
	
	public void close() throws IOException{
		for(Entry<String,Socket> e : userTargets.entrySet()){
			e.getValue().close();
		}
	}
	
	public String sendMessageToUser(String username,String address,String msg) throws IOException{
		Socket user = userTargets.get(username);
		//NOTE: if we don't have a channel for the user yet, we open a new one
		if(user == null){
			String[] split = address.split(":");
			int port = Integer.valueOf(split[1]);
			user = new Socket(InetAddress.getByName(split[0]),port);
			userTargets.put(username, user);
		}
		
		BufferedReader rd = new BufferedReader(new InputStreamReader(user.getInputStream()));
		PrintWriter pw = new PrintWriter(user.getOutputStream());
		pw.println(msg);
		pw.flush();
		String ret = rd.readLine();
		return ret;
	}
	
	public String sendMessageToUser(String username,String address,String msg, HMacHandler hmac) throws IOException, InvalidKeyException, NoSuchAlgorithmException{
		Socket user = userTargets.get(username);
		//NOTE: if we don't have a channel for the user yet, we open a new one
		if(user == null){
			String[] split = address.split(":");
			int port = Integer.valueOf(split[1]);
			user = new Socket(InetAddress.getByName(split[0]),port);
			userTargets.put(username, user);
		}
		byte[] hash = hmac.computeHash(msg);
		hash = Base64.encode(hash);
		msg = new String(hash) + " " + msg;
		//System.out.println("SENDING: " + msg);
		//Message.testHashMsg(msg);
		
		user.getOutputStream().write(Base64.encode(msg.getBytes()));
		
		//String ret = rd.readLine();
		//NOTE: read answer
		byte[] b = new byte[1024];
		int len = user.getInputStream().read(b);
		String ret = "";
		if(len > 0){
			byte[] ans = new byte[len];
			for(int i=0; i<len; i++){
				ans[i] = b[i];
			}
			
			//NOTE: handling ctc answer
			ans = Base64.decode(ans);
			String answer = new String(ans);
			String recHash;
			String recMsg;
			//System.out.println(answer);
			if(answer.indexOf("!tampered") >= 0){
				recHash = answer.substring(0, answer.indexOf("!tampered")-1);
				recMsg  = answer.substring(answer.indexOf("!tampered"));
			}else{
				recHash = answer.substring(0, answer.indexOf("!ok")-1);
				recMsg  = answer.substring(answer.indexOf("!ok"));
			}
			byte[] computedHash = hmac.computeHash(recMsg);
			computedHash = Base64.encode(computedHash);
			String compHash = new String(computedHash);
			
			if(compHash.equals(recHash)){
				ret = recMsg;
			}else{
				ret = "!tampered " + recMsg;
			}
			
		}else{
			return "!nok connection closed while listening for response!";
		}
		
		
		return ret;
	}

}
