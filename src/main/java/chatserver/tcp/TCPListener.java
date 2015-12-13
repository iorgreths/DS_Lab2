/**
 * 
 */
package chatserver.tcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

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
	
	/**
	 * 
	 * @param tcpSocket - a connected (and open!) {@code Socket}
	 */
	public TCPListener(Socket tcpSocket, ServerInfo info, String regLoc, int regPort, String rootID){
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
				reader = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
				String line = reader.readLine();
				if(line == null){
					if(tcpSocket.isClosed()){
						running = false;
						user.setOffline();
					}
				}else{
					String answer = handleUserCommand(line);
					if(answer != null && answer.length() > 0){
						sendAnswer(answer);
					}
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
							System.out.println("\n -------------- \nSending command to nameserver: " + user.getUsername() + " " + command_split[1]);
							server.registerUser(user.getUsername(), command_split[1]);
							user.register(command_split[1]);
						} catch (RemoteException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (NotBoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (AlreadyRegisteredException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (InvalidDomainException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
						/*
						 * Code for Lab1
						 * user.register(command_split[1]);
						 * retstring = "Successfully registered address for " + user.getUsername();
						 */
						retstring = "Successfully registered address for " + user.getUsername();
					}
				}
				break;
			case "!lookup":
				retstring = "Unknown command";
				if(command_split.length == 2){
					User u = info.getUser(command_split[1]);
					if(u != null && u.isRegistered()){
						retstring = u.getRegisteredAddress();
					}else{
						retstring = "User has not been registered.";
					}
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
