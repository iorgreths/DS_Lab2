package client.tcp;

import java.io.IOException;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.bouncycastle.util.encoders.Base64;

import util.Decrypter;
import util.Encrypter;
import cli.Shell;

/**
 * @author Marcel Gredler
 * @verson 1.0
 *
 * Implementation of the TCP requirements (client to server)
 */
public class ServerCommunication implements Runnable{

	private Thread tcpThread;
	private Shell shell;
	
	private ServerListener listener;
	private AtomicBoolean lock;
	
	private String lastMessage;
	private Decrypter pubDec;
	
	public ServerCommunication(Socket socket, Shell shell){
		listener = new ServerListener(socket);
		this.shell = shell;
		lock = new AtomicBoolean();
		lock.set(false);
		
		pubDec = null;
		lastMessage = "";
	}
	
	public void close() throws IOException{
		listener.close();
		tcpThread.interrupt();
	}
	
	/**
	 * Send a command to the server and listen to the servers reply.
	 * Note that sending the command !send will not listen for a reply
	 * @param command the command for the server (e.g. !send, !login)
	 * @param additionalParams further information for the command (e.g. for !login <username> <password>)
	 * @return the answer of the server
	 * @throws IOException
	 */
	public String sendAndListen(String command, List<String> additionalParams) throws IOException{
		String ret = "";
		
		//NOTE: scraped locking => using a different protocol now
		//NOTE: lock reading
		//lock.set(true);
		
		//NOTE: prepare message to send
		String msg = command;
		for(String s : additionalParams){
			//NOTE: add information; e.g. username and password
			msg = msg + " " + s;
		}
		
		//NOTE: send message
		listener.send(msg);
		
		//NOTE: get answer (if not a !send)
		while(!listener.isMessageAvailable()){
			//wait for answer
		}
		ret = listener.readMessage();
		/*if( !(command.equals("!send") || !command.startsWith("!")) ){
			while(!listener.isMessageAvailable()){
				//wait for answer
			}
			ret = listener.readMessage();
			
		}else{
			ret = "message sent";
		}*/
		
		//NOTE: release lock
		//lock.set(false);
		//tcpThread.interrupt();
		
		return ret;
	}
	
	/**
	 * Send a command to the server and listen to the servers reply.
	 * Note that sending the command !send will not listen for a reply
	 * @param command the command for the server (e.g. !send, !login)
	 * @param additionalParams further information for the command (e.g. for !login <username> <password>); These parameters are in base64
	 * @param enc encryptor to encrypt the message
	 * @return the answer of the server
	 * @throws IOException
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 * @throws NoSuchPaddingException 
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeyException 
	 * @throws InvalidAlgorithmParameterException 
	 */
	public String sendAndListen(String command, List<String> additionalParams, Encrypter enc, Decrypter dec) throws IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException{
		String ret = "";
		
		//NOTE: scraped locking => using a different protocol now
		//NOTE: lock reading
		//lock.set(true);
		
		//NOTE: prepare message to send
		String msg = command;
		for(String s : additionalParams){
			//NOTE: add information; e.g. username and password
			msg = msg + " " + s;
		}
		
		//NOTE: encrypt and send message
		//Message.testMsg1(msg);
		//System.out.println("\n --> MSG: " + msg);
		byte[] message = Base64.encode(enc.encrypt(msg));
		listener.send(message);
		
		
		
		//NOTE: get answer (if not a !send)
		while(!listener.isMessageAvailableBYTE()){
			//wait for answer
		}
		ret = new String(dec.decrypt(listener.readMessageBYTE()));
		/*if(!command.equals("!send")){
			
			
			
		}else{
			ret = "!ok";
		}*/
		
		//NOTE: release lock
		//lock.set(false);
		//tcpThread.interrupt();
		
		return ret;
	}
	
	/**
	 * Send a command to the server and listen to the servers reply.
	 * @param wait - is a response from the server expected?
	 * @param command - the command for the server (encrypted!)
	 * @return the answer of the server
	 * @throws IOException
	 */
	public byte[] sendAndListen(boolean wait, byte[] command) throws IOException{
		byte[] ret;
		
		//NOTE: scraped locking => using a different protocol now
		//NOTE: lock reading
		//lock.set(true);
		
		//NOTE: prepare message to send
		//String msg = command;
		byte[] msg = command;
		
		//NOTE: send message
		listener.send(msg);
		
		//NOTE: get answer (if wait)
		if(wait){
			while(!listener.isMessageAvailableBYTE()){
				//wait for answer
			}
			ret = listener.readMessageBYTE();
			
		}else{
			ret = ("!send").getBytes();
		}
		
		//NOTE: release lock
		//lock.set(false);
		//tcpThread.interrupt();
		
		return ret;
	}

	@Override
	public void run() {
		
		tcpThread = Thread.currentThread();
		
		//NOTE: start an extra reading thread
		Thread t = new Thread(listener);
		t.start();
		
		//NOTE: reading from listener
		boolean running = true;
		while(running){
			
			if(listener.isClosed()){
				running = false;
			}else{
				
				try{
					//NOTE: using a queue for pubmsgs now
					/*
					if(listener.isMessageAvailable()){
						lastMessage = listener.readMessage();
						shell.writeLine(lastMessage);
					}*/
					//boolean test  =(!listener.isQueueEmptyBYTE()) && (pubDec != null);
					
					if( (!listener.isQueueEmptyBYTE()) ){
						if( pubDec != null ){
							//System.out.println("READING PUB MSG? " + test);
							byte[] m = listener.getOldestMessageBYTE();
							try {
								m = pubDec.decrypt(m);
								String temp = new String(m);
								String msg = "";
								//System.out.println(temp);
								//String[] te = temp.split(" ");
								//System.out.println(te.length);
								//byte[] b = Base64.decode(temp);
								//System.out.println(new String(b));
								//msg = new String(b);
								//System.out.println(temp);
								String[] split = temp.split(" ");
								//System.out.println("INFO: " + split.length);
								for(int i=0; i<split.length; i++){
									//System.out.println("INFO-curr: " + i );
									String s = split[i].replace(" ", "");
									byte[] by;
									if(s.length() > 0){
										
										by = Base64.decode(s);
										//System.err.println("IN -" + s + "- -" + new String(by) + "-");
										String innerTemp = new String(by);
										//System.out.println("\nT:" + s);
										msg += innerTemp + " ";
									}
								}
								lastMessage = new String(msg);
							} catch (NoSuchAlgorithmException e) {
								lastMessage = "[PM] The requested rsa-cipher is not supported!";
							} catch (NoSuchPaddingException e) {
								lastMessage = "[PM] The requested padding for the rsa-cipher is not supported!";
							} catch (IllegalBlockSizeException e) {
								lastMessage = "[PM] The block-size for the rsa-cipher is illegal!";
							} catch (BadPaddingException e) {
								lastMessage = "[PM] Bad padding for encryption!";
							} catch (InvalidKeyException e) {
								e.printStackTrace();
								lastMessage = "[PM] The used key is invalid!";
							} catch (InvalidAlgorithmParameterException e1) {
								e1.printStackTrace();
								lastMessage = "[PM] Got an invalid iv from server!";
							}
							shell.writeLine(lastMessage);
						}
					}
				}catch (IOException e) {
					//no prob
				}
				
			}
			
		}
	}
	
	/**
	 * Return the last received public message
	 * @return
	 */
	public String lastMsg(){
		return lastMessage;
	}
	
	public synchronized void setPublicMessageDecrypter(Decrypter d){
		pubDec = d;
	}
	
}
