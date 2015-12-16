/**
 * 
 */
package chatserver.tcp;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import org.bouncycastle.util.encoders.Base64;

import util.Keys;
import nameserver.INameserver;
import nameserver.exceptions.AlreadyRegisteredException;
import nameserver.exceptions.InvalidDomainException;
import model.ServerInfo;
import model.User;

/**
 * @author Marcel Gredler
 * @verson 1.0
 *
 * Implementation of the TCP requirements (for one connection -> one client)
 */
public class TCPListener implements Runnable {

	private Socket tcpSocket;
	private ServerInfo info;
	private User user;
	
	private String regLoc;
	private int    regPort;
	private String rootID;
	
	//NOTE: secret-key for connection
	private SecretKey secretKey;
	private String privKeyLoc;
	private String pubKeyLoc;
	private String shittybit;
	
	/**
	 * 
	 * @param tcpSocket - a connected (and open!) {@code Socket}
	 */
	public TCPListener(Socket tcpSocket, ServerInfo info, String regLoc, int regPort, String rootID, String privKeyLoc, String pubKeyLoc){
		if( (tcpSocket == null) || (tcpSocket.isClosed()) ){
			throw new IllegalArgumentException("TCP-Port has to be connected and open!");
		}else{
			this.tcpSocket = tcpSocket;
		}
		
		this.info = info;
		user = null;
		
		this.regLoc = regLoc;
		this.regPort = regPort;
		this.rootID = rootID;
		
		secretKey = null;
		this.privKeyLoc = privKeyLoc;
		this.pubKeyLoc = pubKeyLoc;
		
		//NOTE: character for separation of messages
		char ch = 0;
		shittybit = String.valueOf(ch) + String.valueOf(ch);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		boolean running = true;
		BufferedReader reader;
		while(running){
			try {
				
				byte[] b = new byte[1024];
				int len = tcpSocket.getInputStream().read(b);
				byte[] msg = new byte[len];
				for(int i=0; i<len; i++){
					msg[i] = b[i];
				}
				//System.out.println(len);
				try {
					String message = decodeUserInput(msg);
					System.out.println(message);
				} catch (InvalidKeyException | NoSuchAlgorithmException
						| NoSuchPaddingException | IllegalBlockSizeException
						| BadPaddingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				/*
				reader = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
				String line = reader.readLine();
				if(line == null){
					if(tcpSocket.isClosed()){
						running = false;
						user.setOffline();
					}
				}else{
					//NOTE: decrypt client message
					
					
					//NOTE: compute answer for client
					String answer = handleUserCommand(line);
					if(answer != null && answer.length() > 0){
						sendAnswer(answer);
					}
					
				}*/
				
				if(tcpSocket.isClosed()){
					running = false;
				}
				//reader.close();
			} catch (IOException e) {
				if(tcpSocket.isClosed()){
					running = false;
				}
			}
		}
	}
	
	/**
	 * 
	 * @param msg
	 * @return
	 * @throws IOException 
	 * @throws NoSuchPaddingException 
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeyException 
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 */
	private String decodeUserInput(byte[] msg) throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException{
		Cipher c;
		//System.err.println(msg);
		if(secretKey == null){
			//NOTE: has to be a new user-connection -> read server-private key
			File f = new File(privKeyLoc);
			PrivateKey pk = Keys.readPrivatePEM(f);
			
			c = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
			c.init(Cipher.DECRYPT_MODE, pk);
			
		}else{
			//NOTE: secretkey has been set -> use for communication
			c = Cipher.getInstance("AES/CTR/NoPadding");
			c.init(Cipher.DECRYPT_MODE, secretKey);
		
		}
		//byte[] msgByte = msg.getBytes();
		//NOTE: decode from base64
		msg = Base64.decode(msg);

		//NOTE: decode cipher-string
		byte[] plaintext = c.doFinal(msg);
		
		//NOTE: decode base64 again
		plaintext = Base64.decode(plaintext);
		
		//NOTE: return value
		return new String(plaintext);
	}
	
	/**
	 * Handle the command of a user and return information
	 * 
	 * @param command - the user input
	 * @return an error-message if the command is invalid, otherwise the answer
	 */
	private String handleUserCommand(String command){
		String retstring = "Unknown command";
		if(command.startsWith("!")){
			String[] command_split = command.split(" ");
			switch(command_split[0]){
			case "!login":
				retstring = "Wrong username or password.";
				if(command_split.length == 3){
					
					User test = info.getUser(command_split[1]);
					if(test != null && test.isOnline()){
						retstring = "User already logged in.";
					}else{
						User user = info.loginUser(command_split[1], command_split[2]);
						if(user != null && user.isOnline()){
							this.user = user;
							retstring = "Successfully logged in.";
						}
					}
				}
				break;
			case "!logout":
				retstring = "Unknwown command";
				if(command_split.length == 1){
					retstring = "Must be logged in to do this.";
					if(user != null && user.isOnline()){
						user.setOffline();
						retstring = "Successfully logged out.";
					}
				}
				break;
			case "!send":
				retstring = "";
				if(command_split.length > 1){
					info.sendToAll(command.substring(command.indexOf(" ")+1), user);
				}
				break;
			case "!register":
				retstring = "Unknown command";
				if(command_split.length > 1){
					retstring = "Wrong register format -> IP:port";
					String[] reginfo = command_split[1].split(":");
					if(reginfo.length == 2){
						retstring = "contacting server ...";
						try {
							Registry registry = LocateRegistry.getRegistry(regLoc,regPort);
							INameserver server = (INameserver) registry.lookup(rootID);
							//System.out.println("\n -------------- \nSending command to nameserver: " + user.getUsername() + " " + command_split[1]);
							server.registerUser(user.getUsername(), command_split[1]);
							user.register(command_split[1]);
							retstring = "Successfully registered address for " + user.getUsername();
						} catch (RemoteException e) {
							retstring = "[Nameserver Error] Failed to register user! Shit happens...";
						} catch (NotBoundException e) {
							retstring = "Necessary domains not bound!";
						} catch (AlreadyRegisteredException e) {
							retstring = "User is already registered!";
						} catch (InvalidDomainException e) {
							retstring = "User does not exist in this domain!";
						}
						
						/*
						 * Code for Lab1
						 * user.register(command_split[1]);
						 * retstring = "Successfully registered address for " + user.getUsername();
						 */
					}
				}
				break;
			case "!lookup":
				retstring = "Unknown command";
				if(command_split.length == 2){
					//NOTE: implementation for lab2
					Registry registry;
					try {
						registry = LocateRegistry.getRegistry(regLoc,regPort);
						INameserver server = (INameserver) registry.lookup(rootID);
						
						String user = command_split[1];
						while(user.contains(".")){
							//NOTE: look for server
							String zone = user.substring(user.lastIndexOf(".")+1);
							user = user.substring(0,user.lastIndexOf("."));
							//System.out.println("\n\n\n=======\nZONE="+zone+"\n\n");
							server = (INameserver) server.getNameserver(zone);
						}
						
						//NOTE: look for user
						retstring = server.lookup(user);
						
					} catch (RemoteException e) {
						retstring = "[Nameserver Error] Failed to register user! Shit happens...";
					} catch (NotBoundException e) {
						retstring = "User not bound!";
					}
					
					
					
					/* Implementation for Lab1
					 * 
					 * User u = info.getUser(command_split[1]);
					if(u != null && u.isRegistered()){
						retstring = u.getRegisteredAddress();
					}else{
						retstring = "User has not been registered.";
					}*/
				}
				break;
			default:
				//return error information
				retstring = "Unknown command";
				break;
			}
		}
		return retstring;
	}
	
	/**
	 * Send a message to the user behind this listener
	 * [SPMP]username:message
	 * @param msg - the message the user should receive
	 * @param sender the user which sends the message
	 * @throws IOException 
	 */
	public void sendMessage(String msg, User sender) throws IOException{
		if(user != null && user.isOnline()){
			if(!sender.getUsername().equals(user.getUsername())){
				PrintWriter pw = new PrintWriter(tcpSocket.getOutputStream());
				pw.println("[SPMP]" + sender.getUsername() + ": " + msg);
				pw.flush();
				//pw.close();
			}
		}
	}
	
	/**
	 * Send an answer to the client
	 * @param answer - the message for the client
	 * @throws IOException
	 */
	public void sendAnswer(String answer) throws IOException{
		PrintWriter pw = new PrintWriter(tcpSocket.getOutputStream());
		pw.println(answer);
		pw.flush();
		//pw.close();
	}
	
	/**
	 * Closes the socket for this connection
	 * @throws IOException
	 */
	public void closeSocket() throws IOException{
		tcpSocket.close();
	}

}
