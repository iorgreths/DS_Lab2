package client.tcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.Queue;

import org.bouncycastle.util.encoders.Base64;

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
	
	//NOTE: for lab2
	private byte[] bmessage;
	private Queue<byte[]> bpubmsg;
	
	public ServerListener(Socket tcpSocket){
		this.tcpSocket = tcpSocket;
		pubmsg = new ArrayDeque<String>();
		bpubmsg = new ArrayDeque<byte[]>();
	}
	
	@Override
	public void run() {
		
		tcpThread = Thread.currentThread();
		
		boolean running = true;
		while(running){
			
			try {
				if(tcpSocket.isClosed()){
					running = false;
				}else{
					
					byte[] msg = new byte[1024];
					int len = tcpSocket.getInputStream().read(msg);
					
					byte[] message = new byte[len];
					for(int i=0; i<len; i++){
						message[i] = msg[i];
					}
					//NOTE: it's in base64
					//System.out.println(message + ":" + message.length);
					message = Base64.decode(message);
					
					String line = new String(message);
					if(line != null && line.startsWith("[SPMP]")){
						line = line.substring(6);
						//pubmsg.add(line);
						bpubmsg.add(line.getBytes());
					}else{
						//this.message = line;
						this.bmessage = message;
					}
					
				}
				
			} catch (IOException e) {
				if(tcpSocket.isClosed()){
					running = false;
				}
			}
			
			/*
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
				
			}*/
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
	
	public void send(byte[] bytes) throws IOException{
		//NOTE: only send base64 encoded
		tcpSocket.getOutputStream().write(bytes);
		tcpSocket.getOutputStream().flush();
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
	
	public synchronized boolean isMessageAvailableBYTE(){
		return bmessage != null;
	}
	
	public synchronized boolean isQueueEmptyBYTE(){
		return bpubmsg.isEmpty();
	}
	
	public synchronized byte[] getOldestMessageBYTE(){
		return bpubmsg.poll();
	}
	
	public synchronized byte[] readMessageBYTE(){
		byte[] ret = bmessage;
		bmessage = null;
		return ret;
	}

}
