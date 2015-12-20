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
import java.nio.ByteBuffer;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.util.encoders.Base64;

import util.Decrypter;
import util.Encrypter;
import util.Keys;
import util.Message;
import util.Pair;
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
	
	//NOTE: key-locations
	private String privKeyLoc;
	private String pubKeyLoc;
	private String pubKeyLocUser;
	
	//NOTE: secret-key for connection
	private Pair<Encrypter,Decrypter> aes;

	
	//NOTE: secret key attemp
	private SecretKey secretKey;
	private boolean doNotUse;
	private byte[] iv;
	private String serverChallenge;

	//NOTE: shittybit
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
		shittybit = String.valueOf(ch);// + String.valueOf(ch);
	}
	
	private void writeErrorLog(String errorMessage){
		DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
		Calendar cal = Calendar.getInstance();
		String date  = dateFormat.format(cal.getTime()); //2014/08/06 16:00:22
		System.err.println("[" + date + "] " + errorMessage );
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		boolean running = true;
		//BufferedReader reader;
		while(running){
			try {
				boolean skip = false;
				
				if(tcpSocket.isClosed()){
					running = false;
				}else{
					
					byte[] b = new byte[1024];
					int len = tcpSocket.getInputStream().read(b);
					if(len > 0){
						byte[] msg = new byte[len];
						for(int i=0; i<len; i++){
							msg[i] = b[i];
						}
						if(tcpSocket.isClosed()){
							running = false;
							user.setOffline();
						}else{
							//NOTE: decrypt client message
							Decrypter temp;
							if(aes != null){
								temp = aes.getValue();
							}else{
								File f = new File(privKeyLoc);
								if(!f.exists()){
									throw new IOException("No private key for server!");
								}
								PrivateKey pk = Keys.readPrivatePEM(f);
								temp = new Decrypter("RSA/NONE/OAEPWithSHA256AndMGF1Padding",pk);
							}
							
							String message = null;
							
							try {
								message = decodeUserInput(msg,temp);
								
							} catch (InvalidKeyException e1) {
								//NOTE: -> session must have worn of
								// -> try private key
								File f = new File(privKeyLoc);
								if(!f.exists()){
									throw new IOException("No private key for server!");
								}
								PrivateKey pk = Keys.readPrivatePEM(f);
								temp = new Decrypter("RSA/NONE/OAEPWithSHA256AndMGF1Padding",pk);

								try {
									message = decodeUserInput(msg,temp);
								} catch (InvalidKeyException | NoSuchAlgorithmException
										| NoSuchPaddingException
										| IllegalBlockSizeException
										| BadPaddingException | InvalidAlgorithmParameterException e) {
									//NOTE: should only happen if user is malicious
									writeErrorLog("Possible maliocious input from IP-address " + tcpSocket.getInetAddress());
									skip = true;
								}
								
							} catch (NoSuchAlgorithmException e1) {
								writeErrorLog("server-host does not support necessary encryption-algorithms!");
								skip = true;
							} catch (NoSuchPaddingException e1) {
								writeErrorLog("server-host has a problem with the padding:\n" + e1.getLocalizedMessage());
								skip = true;
							} catch (IllegalBlockSizeException e1) {
								writeErrorLog("server-host has a problem with the block-size:\n" + e1.getLocalizedMessage());
								skip = true;
							} catch (BadPaddingException e1) {
								e1.printStackTrace();
								writeErrorLog("server-host has a problem with the padding of the input:\n" + e1.getLocalizedMessage());
								skip = true;
							} catch (InvalidAlgorithmParameterException e1) {
								writeErrorLog("server-host has a problem with the iv:\n" + e1.getLocalizedMessage());
								skip = true;
							}
							
							if( (message == null) && (skip == false) ){
								skip = true;
							}
							
							if(skip){
								sendAnswer( Base64.encode(("!nok").getBytes()) );
							}
							
							if(!skip){
								
								try {
									
									//NOTE: handle client message
									String answer = handleUserCommand(message);
									
									//NOTE: look if the answer is a response to server-challenge
									if(answer.startsWith("[CHALLENGE]")){
										String t = answer.substring(11);
										if(!t.equals(serverChallenge)){
											aes = null;
											tcpSocket.close();
										}
										//NOTE: if challenge matches -> do nothing
										this.user.setOnline();
										info.setOnline(this.user.getUsername(), true);
										answer = "!ok";
									}
									if(answer.equals("")){
										skip = true;
									}
									
									//NOTE: if challenge has failed -> socket has been closed
									if( (!tcpSocket.isClosed()) && (!skip) ){
										//NOTE: prepare message for sending (-> encrypt and base64 encode)
										Encrypter enc = null;
										if( (aes != null) && (doNotUse == false) ){
											enc = aes.getKey();
										}else{
											doNotUse = false;
											File fi = new File(pubKeyLocUser);
											if(!fi.exists()){
												answer = "!nok " + new String(Base64.encode(("Who are you, brah?(i have no recollection of you!)").getBytes()));
											}else{
												PublicKey pub = Keys.readPublicPEM(fi);
												enc = new Encrypter("RSA/NONE/OAEPWithSHA256AndMGF1Padding", pub);
											}
										}
										
										if(answer.equals("User already in use!")){
											answer = "!nok " + new String(Base64.encode(("User already in use!").getBytes()));
										}
										
										//System.err.println(answer);
										byte[] ans = encodeServerAnswer(answer,enc);
										
										//NOTE: send message
										sendAnswer(ans);
									}
									
								} catch (InvalidKeyException | NoSuchAlgorithmException
										| NoSuchPaddingException
										| IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException e) {
									writeErrorLog("Problem with encoding response for client:\n" + e.getLocalizedMessage());
									sendAnswer( Base64.encode(("!nok").getBytes()) );
								}
								
							}
							
						}
					}
				}
				
				//reader.close();
			} catch (IOException e) {
				if(e.getMessage().equals("No private key for server!")){
					writeErrorLog("Server has no private key, shutdown server and look for the key in '" + privKeyLoc + "'!");
				}
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
	/*private String decodeUserInput(byte[] msg) throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException{
		Cipher c;
		System.out.println(msg.length);
		//System.err.println(msg);
		if(secretKey == null){
			//NOTE: has to be a new user-connection -> read server-private key
			File f = new File(privKeyLoc);
			//File f = new File("keys/client/bill.de.pem");
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
	}*/
	
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
	 * @throws InvalidAlgorithmParameterException 
	 */
	private String decodeUserInput(byte[] msg, Decrypter dec) throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException{
		//byte[] msgByte = msg.getBytes();
		//NOTE: decode from base64
		byte[] temp = Base64.decode(msg);

		//NOTE: decode cipher-string
		byte[] plaintext = dec.decrypt(temp);
		
		//NOTE: return value
		return new String(plaintext);
	}
	
	private byte[] encodeServerAnswer(String msg, Encrypter enc) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException{
		//Message.testMsg2(msg);
		
		byte[] ret = null;
		if(enc != null){
			ret = enc.encrypt(msg);
		}else{
			ret = msg.getBytes();
		}
		ret = Base64.encode(ret);
		
		return ret;
	}
	
	/**
	 * Encondes each part with Base64 before combining them
	 * @param parts
	 * @return
	 */
	/*
	private byte[] createFullMessage(byte[][] parts){
		int len = 0;
		List<byte[]> partsEncoded = new LinkedList<byte[]>();
		for(byte[] b : parts){
			byte[] temp = Base64.encode(b);
			len += temp.length;
			partsEncoded.add(temp);
		}
		byte[] retval = new byte[len];
		ByteBuffer bb = ByteBuffer.wrap(retval);
		for(byte[] b : partsEncoded){
			bb.put(b);
		}
		return retval;
	}*/
	
	/**
	 * Handle the command of a user and return information
	 * 
	 * @param command - the user input
	 * @return an error-message if the command is invalid, otherwise the answer
	 * @throws NoSuchAlgorithmException 
	 * @throws NoSuchPaddingException 
	 * @throws IOException 
	 * @throws InvalidKeyException 
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 */
	private String handleUserCommand(String command) throws NoSuchAlgorithmException, NoSuchPaddingException, IOException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException{
		//TODO after authenticate works -> update all commands (especially !send -> [SPMP] outside of encryption
		String retstring = "Unknown command";
		if(command.startsWith("!")){
			String[] command_split = command.split(" ");
			
			switch(command_split[0]){
			case "!login":
				retstring = "No longer supported! Use !authenticate instead.";
				/*
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
				}*/
				break;
			case "!authenticate":
				//NOTE: only lab2
				retstring = "Unknown user.";
				if(command_split.length != 3){
					retstring = "Wrong usage of command authenticate!";
				}else{
					//NOTE: prepare path to user-key
					String user = new String(Base64.decode(command_split[1]));
					this.user = new User(user,null);
					pubKeyLocUser = pubKeyLoc + "/" + user + ".pub.pem";
					if(info.isAuthenticated(user)){
						return "User already in use!";
					}
					
					//NOTE: prep message
					retstring = "!ok ";
					retstring += command_split[2];
					retstring += " ";
					
					//NOTE: create server-challenge
					SecureRandom rand = new SecureRandom();
					byte[] srChall = new byte[32];
					rand.nextBytes(srChall);
					
					//NOTE: create aes-secret-key
					KeyGenerator generator = KeyGenerator.getInstance("AES");
					generator.init(256); // KEYSIZE is in bits
					SecretKey key = generator.generateKey();
					byte[] keyToSend = key.getEncoded();
					
					//NOTE: create iv
					iv= new byte[16];
					rand = new SecureRandom();
					rand.nextBytes(iv);
					
					//add secret-key
					String instance = "AES/CTR/NoPadding";
					//byte[] encodedKey = keyToSend;
					//SecretKey secret = new SecretKeySpec(encodedKey, 0, encodedKey.length, "AES");
					Encrypter e = new Encrypter(instance,key,iv);
					Decrypter d = new Decrypter(instance,key,iv);
					aes = new Pair<>(e,d);
					doNotUse = true;
					
					//NOTE: base64 encoding
					srChall = Base64.encode(srChall);
					serverChallenge = new String(srChall);
					keyToSend = Base64.encode(keyToSend);
					iv = Base64.encode(iv);
					
					retstring += new String(srChall) + " ";
					retstring += new String(keyToSend) + " ";
					retstring += new String(iv);
				}
				break;
			case "!logout":
				retstring = "Unknwown command";
				if(command_split.length == 1){
					retstring = "Must be logged in to do this.";
					if(user != null && user.isOnline()){
						user.setOffline();
						info.setOnline(this.user.getUsername(), false);
						retstring = "Successfully logged out.";
					}
				}
				break;
			case "!send":
				retstring = "";
				//System.out.println(command.substring(command.indexOf(" ")+1) + "::" + user.getUsername());
				if(command_split.length > 1){
					info.sendToAll(command.substring(command.indexOf(" ")+1), user);
				}
				retstring = "!ok";
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
		}else{
			retstring = "[CHALLENGE]" + command;
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
		boolean test = (aes != null) && (user != null) && (user.isOnline());
		System.out.println("TRUE? " + test);
		if(aes != null){
			if(user != null && user.isOnline()){
				if(!sender.getUsername().equals(user.getUsername())){
					try {
						String m = sender.getUsername() + ":" + msg;
						String temp = "";
						for(String s : m.split(" ")){
							temp += new String(Base64.encode(s.getBytes())) + " ";
						}
						byte[] cipher = aes.getKey().encrypt(temp);
						String ans = "[SPMP]" + new String(cipher);
						System.out.println("TO " + user.getUsername() + " MESSAGE: " + ans);
						cipher = Base64.encode(ans.getBytes());
						tcpSocket.getOutputStream().write(cipher);
						tcpSocket.getOutputStream().flush();
						
					} catch (InvalidKeyException | NoSuchAlgorithmException
							| NoSuchPaddingException
							| IllegalBlockSizeException | BadPaddingException
							| InvalidAlgorithmParameterException e) {
						writeErrorLog("Problem with encoding response for client:\n" + e.getLocalizedMessage());
						//sendAnswer( Base64.encode(("!nok").getBytes()) );
					}
					
					/*
					PrintWriter pw = new PrintWriter(tcpSocket.getOutputStream());
					pw.println("[SPMP]" + sender.getUsername() + ": " + msg);
					pw.flush();*/
					//pw.close();
				}
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
	
	public void sendAnswer(byte[] answer) throws IOException{
		tcpSocket.getOutputStream().write(answer);
		tcpSocket.getOutputStream().flush();
	}
	
	/**
	 * Closes the socket for this connection
	 * @throws IOException
	 */
	public void closeSocket() throws IOException{
		tcpSocket.close();
	}
	
	
	/*
	 * just backup
	 */
	/*
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
				if(len > 0){
					try {
					
						if(tcpSocket.isClosed()){
							running = false;
							user.setOffline();
						}else{
							
							//NOTE: decode the message
							String message = decodeUserInput(msg);
							//System.out.println("INFO: " + message);
							
							//NOTE: handle message

							//TODO
							System.out.println(message);
							String[] command_split = Message.splitMessage(message);
							command_split[0] = Message.removeShittyBit(command_split[0]);
							command_split[1] = Message.removeShittyBit(command_split[1]);
							/*int count = 0;
							System.out.println(message);
							String[] temp = message.split(shittybit);
							for(String s : temp){
								if(s.length() > 1){
									count++;
								}
							}
							String[] command_split = new String[count];
							int ind = 0;
							for(String s : temp){
								if(s.length() > 1){
									command_split[ind] = s;
									ind++;
								}
							}
							
							byte[] comByte = ("!ok").getBytes();
							byte[] clChall = command_split[2].getBytes();
							SecureRandom rand = new SecureRandom();
							byte[] srChall = new byte[32];
							rand.nextBytes(srChall);
							
							KeyGenerator generator = KeyGenerator.getInstance("AES");
							// KEYSIZE is in bits
							generator.init(256);
							SecretKey key = generator.generateKey();
							byte[] keyToSend = key.getEncoded();
							
							iv= new byte[16];
							rand.nextBytes(iv);
							
							//Create full message
							byte[][] blist = {comByte};
							byte[] retbyte = createFullMessage(blist);
							
							//NOTE: do encryption
							//TODO wrong key?
							File f = new File(pubKeyLoc + "/" + command_split[1] + ".pub.pem");
							System.out.println(f.getAbsolutePath());
							byte[] answer;
							if(!f.exists()){
								answer = ("No key for this user!").getBytes();
							}else{
								PublicKey pk = Keys.readPublicPEM(f);
								Cipher c = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
								c.init(Cipher.ENCRYPT_MODE, pk);
								retbyte = c.doFinal(retbyte);
								
								answer = retbyte;
							}
							sendAnswer(answer);
							//TODO
							
						}
						
					} catch (InvalidKeyException | NoSuchAlgorithmException
							| NoSuchPaddingException | IllegalBlockSizeException
							| BadPaddingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
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
					
				}
				
				if(tcpSocket.isClosed()){
					running = false;
					user.setOffline();
				}
				//reader.close();
			} catch (IOException e) {
				if(tcpSocket.isClosed()){
					running = false;
				}
			}
		}
	}*/
	
	public String getUser(){
		return this.user.getUsername();
	}

}
