package client.tcp.ctc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

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

}
