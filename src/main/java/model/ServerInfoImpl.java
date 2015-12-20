package model;

import java.io.IOException;
import java.io.PrintStream;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import chatserver.tcp.TCPListener;
import util.Config;

public class ServerInfoImpl implements ServerInfo {

	private static int threads = 5;
	
	private ExecutorService pool;
	private ServerSocket tcp;
	private DatagramSocket udp;
	
	private Map<String,User> users; //NOTE: username, user-object
	private List<TCPListener> listener;
	
	private PrintStream out;
	
	public ServerInfoImpl(PrintStream userResponseStream){
		out = userResponseStream;
		
		//pool = Executors.newScheduledThreadPool(10);
		pool = Executors.newFixedThreadPool(threads);
		
		HashMap<String,User> temp = new HashMap<String,User>();
		users = Collections.synchronizedMap(temp);
		listener = new LinkedList<TCPListener>();
		Config config = new Config("user");
		for(String s : config.listKeys()){
			String username = s.substring(0,s.lastIndexOf("."));
			User user = new User(username,config.getString(s));
			users.put(username, user);
		}
	}
	
	@Override
	public boolean startTCPThread(TCPListener run) {
		listener.add(run);
		pool.execute(run);
		
		return true;
	}

	@Override
	public void setTCPSocket(ServerSocket socket) {
		this.tcp = socket;

	}

	@Override
	public void setUDPSocket(DatagramSocket socket) {
		this.udp = socket;

	}

	@Override
	public void shutdown() throws IOException {
		udp.close();
		tcp.close();
		
		for(TCPListener l : listener){
			l.closeSocket();
		}
		//pool.shutdown();
		List<Runnable> wait = pool.shutdownNow();
		for(Runnable r : wait){
			TCPListener l = (TCPListener) r;
			l.closeSocket();
		}

	}

	@Override
	public User loginUser(String username, String password) {
		User user = users.get(username);
		if(user != null){
			if(!user.login(password)){
				user = null;
			}
		}
		return user;
	}
	
	@Override
	public void setOnline(String username, boolean online){
		User user = users.get(username);
		if(online){
			user.setOnline();
		}else{
			user.setOffline();
		}
	}

	@Override
	public Map<String, String> getUsers() {
		HashMap<String,String> usermap = new HashMap<String,String>();
		for(Entry<String,User> e : users.entrySet()){
			String on = "offline";
			if(e.getValue().isOnline()){
				on = "online";
			}
			usermap.put(e.getKey(), on);
		}
		return usermap;
	}

	@Override
	public User getUser(String username) {
		User user = null;
		user = users.get(username);
		return user;
	}

	@Override
	public void sendToAll(String msg, User sender) {
		//System.out.println("SEND TO ALL:" + sender.getUsername() + ":" + msg);
		for(TCPListener l : listener){
			//System.out.println("SENDING TO " + l.getUser());
			try {
				l.sendMessage(msg, sender);
			} catch (IOException e) {
				out.println("Could not send message to all users!\n"+e.getLocalizedMessage());
			}
		}
	}
	
	@Override
	public boolean isAuthenticated(String user){
		boolean notInUse = false;
		if(users.get(user).isOnline()){
			notInUse = true;
		}
		return notInUse;
	}

}
