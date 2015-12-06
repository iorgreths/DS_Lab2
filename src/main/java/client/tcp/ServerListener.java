package client.tcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.Queue;

/**
 * 
 * @author Marcel Gredler
 * @version 1.0
 *
 */
public class ServerListener implements Runnable {

	private Socket tcpSocket;
	private Thread tcpThread;
	private BufferedReader reader;
	
	private String message;
	private Queue<String> pubmsg;
	
	public ServerListener(Socket tcpSocket){
		this.tcpSocket = tcpSocket;
		pubmsg = new ArrayDeque<String>();
	}
	
	@Override
	public void run() {
		
		tcpThread = Thread.currentThread();
		
		boolean running = true;
		while(running){
			try{
				reader = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
				String line = reader.readLine();

				if(line != null && line.startsWith("[SPMP]")){
					line = line.substring(6);
					pubmsg.add(line);
				}else{
					message = line;
				}
			}catch (IOException e) {
				if(tcpSocket.isClosed()){
					running = false;
				}
				
			}
		}

	}
	
	/**
	 * Send a message to the server through the set socket
	 * 
	 * @param msg -the message for the server
	 * @throws IOException 
	 */
	public void send(String msg) throws IOException{
		PrintWriter pw;
		pw = new PrintWriter(tcpSocket.getOutputStream());
		pw.println(msg);
		pw.flush();
	}
	
	public void close() throws IOException{
		tcpSocket.close();
		tcpThread.interrupt();
	}
	
	public boolean isClosed(){
		return tcpSocket.isClosed();
	}
	
	public synchronized boolean isMessageAvailable(){
		return message != null;
	}
	
	public synchronized boolean isQueueEmpty(){
		return pubmsg.isEmpty();
	}
	
	public synchronized String getOldestMessage(){
		return pubmsg.poll();
	}
	
	public synchronized String readMessage(){
		String ret = message;
		message = null;
		return ret;
	}

}
