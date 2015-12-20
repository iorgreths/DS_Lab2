package client.tcp.ctc;

import java.io.IOException;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.bouncycastle.util.encoders.Base64;

import util.HMacHandler;
import cli.Shell;

public class ClientListener implements Runnable{

	private Socket socket;
	private Shell shell;
	private HMacHandler hmac;
	
	public ClientListener(Socket socket, Shell shell, HMacHandler hmac){
		this.socket = socket;
		this.shell = shell;
		this.hmac = hmac;
	}
	
	/**
	 * Close the socket
	 * @throws IOException 
	 */
	public void close() throws IOException{
		socket.close();
	}
	
	@Override
	public void run() {
		
		//BufferedReader reader;
		//PrintWriter writer;
		boolean running = true;
		while(running){
			
			try {
				byte[] b = new byte[1024];
				int len = socket.getInputStream().read(b);
				if(len > 0){
					byte[] msg = new byte[len];
					for(int i=0; i<len; i++){
						msg[i] = b[i];
					}
					try {
						String message = getMessageForShell(msg);
						String answer;
						if(message.startsWith("!tampered")){
							shell.writeLine(message.substring(message.indexOf("!tampered ")+10));
							answer = message;
						}else{
							shell.writeLine(message);
							answer = "!ok";
						}
						//System.out.println(message);
						byte[] ans = prepareMessageForSending(answer);
						socket.getOutputStream().write(ans);
						
					} catch (InvalidKeyException e) {
						shell.writeLine("Invalid key for hashing!");
						//e.printStackTrace();
					} catch (NoSuchAlgorithmException e) {
						shell.writeLine("Invalid algorithm for hashing!");
					}
					
				}
				
				if(socket.isClosed()){
					running = false;
				}
				
				/*
				reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				writer = new PrintWriter(socket.getOutputStream());
				String line = reader.readLine();
				if(line != null){
					shell.writeLine(line);
					writer.println("!ack");
				}else{
					writer.println("!nack");
				}
				writer.flush();
				*/
			} catch (IOException e) {
				if(socket.isClosed()){
					running = false;
				}
			}
			
		}
		
	}
	
	public String getMessageForShell(byte[] msg) throws InvalidKeyException, NoSuchAlgorithmException{
		String message = new String(Base64.decode(msg));
		//System.out.println("INC: " + message);
		//System.out.println("INDEX OF " + message.indexOf(" !msg"));
		String hmacS = message.substring(0, message.indexOf(" !msg"));
		String msgmsg = message.substring(message.indexOf("!msg"));
		//System.out.println("FROM SIDE: " + msgmsg);
		byte[] hash = hmac.computeHash(msgmsg);
		String hashString = new String(Base64.encode(hash));
		if(hashString.equals(hmacS)){
			return msgmsg;
		}else{
			writeErrorLog("sending !tampered " + msgmsg);
			return "!tampered " + msgmsg;
		}
	}
	
	private void writeErrorLog(String errorMessage){
		DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
		Calendar cal = Calendar.getInstance();
		String date  = dateFormat.format(cal.getTime()); //2014/08/06 16:00:22
		System.err.println("[" + date + "] " + errorMessage );
	}
	
	private byte[] prepareMessageForSending(String msg) throws InvalidKeyException, NoSuchAlgorithmException{
		byte[] hash = hmac.computeHash(msg);
		hash = Base64.encode(hash);
		msg = new String(hash) + " " + msg;
		//Message.testHashMsg(msg);
		return Base64.encode(msg.getBytes());
	}

}
