package client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.LinkedList;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.bouncycastle.util.encoders.Base64;

import model.ClientInfo;
import model.ClientInfoImpl;
import cli.Command;
import cli.Shell;
import client.tcp.ServerCommunication;
import client.tcp.ctc.ClientCommunication;
import client.udp.ServerCommunicationUDP;
import util.Config;
import util.Keys;

public class Client implements IClientCli, Runnable {

	@SuppressWarnings("unused")
	private String componentName;
	private Config config;
	@SuppressWarnings("unused")
	private InputStream userRequestStream;
	@SuppressWarnings("unused")
	private PrintStream userResponseStream;
	
	private Shell shelli;
	private ClientInfo model;

	/**
	 * @param componentName
	 *            the name of the component - represented in the prompt
	 * @param config
	 *            the configuration to use
	 * @param userRequestStream
	 *            the input stream to read user input from
	 * @param userResponseStream
	 *            the output stream to write the console output to
	 */
	public Client(String componentName, Config config,
			InputStream userRequestStream, PrintStream userResponseStream) {
		this.componentName = componentName;
		this.config = config;
		this.userRequestStream = userRequestStream;
		this.userResponseStream = userResponseStream;

		shelli = new Shell(componentName, userRequestStream, userResponseStream);
		shelli.register(this);
		
	}
	
	public void prepareClient() throws IOException{
		ServerCommunicationUDP serverCommUDP = new ServerCommunicationUDP(new DatagramSocket(), config.getString("chatserver.host"), config.getInt("chatserver.udp.port"));
		Socket socket = new Socket(InetAddress.getByName(config.getString("chatserver.host")),config.getInt("chatserver.tcp.port"));
		ServerCommunication serverComm = new ServerCommunication(socket,shelli);
		
		model = new ClientInfoImpl(serverCommUDP,serverComm);
	}

	@Override
	public void run() {
		
		try {
			prepareClient();
			Thread serverTCP = new Thread(model.getServerCommunication());
			serverTCP.start();
			shelli.run();
			
		} catch (IOException e) {
			System.err.println("Unable to prepare client-information:\n"+e.getLocalizedMessage());
		}
		
		
	}

	@Override
	@Command
	public String login(String username, String password) throws IOException {
		LinkedList<String> additionalParams = new LinkedList<String>();
		additionalParams.add(username);
		additionalParams.add(password);
		String answer = model.getServerCommunication().sendAndListen("!login", additionalParams);
		if(answer.toLowerCase().contains("success")){
			model.setOnline(true);
		}
		return answer;
	}

	@Override
	@Command
	public String logout() throws IOException {
		LinkedList<String> additionalParams = new LinkedList<String>();
		String answer = model.getServerCommunication().sendAndListen("!logout", additionalParams);
		if(answer.toLowerCase().contains("success")){
			model.setOnline(false);
		}
		return answer;
	}

	@Override
	@Command
	public String send(String message) throws IOException {
		if(model.isOnline()){
			LinkedList<String> additionalParams = new LinkedList<String>();
			additionalParams.add(message);
			return model.getServerCommunication().sendAndListen("!send", additionalParams);
		}else{
			return "Must be logged in to do this.";
		}
	}

	@Override
	@Command
	public String list() throws IOException {
		return model.getServerCommunicationUDP().sendListRequestToServer();
	}

	@Override
	@Command
	public String msg(String username, String message) throws IOException {
		if(model.isOnline()){
			String ret = "";
			String address = lookup(username);
			ret = address;
			if(ClientCommunication.validAddress(address)){
				ret = model.getClientSender().sendMessageToUser(username, address, message);
			}
			return ret;
		}else{
			return "Must be logged in to do this.";
		}
	}

	@Override
	@Command
	public String lookup(String username) throws IOException {
		if(model.isOnline()){
			LinkedList<String> additionalParams = new LinkedList<String>();
			additionalParams.add(username);
			return model.getServerCommunication().sendAndListen("!lookup", additionalParams);
		}else{
			return "Must be logged in to do this.";
		}
	}

	@Override
	@Command
	public String register(String privateAddress) throws IOException {
		if(model.isOnline()){
			if(!ClientCommunication.validAddress(privateAddress)){
				return "Invalid address.";
			}else{
				ServerSocket socket = new ServerSocket(Integer.valueOf(privateAddress.split(":")[1]));
				ClientCommunication cc = new ClientCommunication(model,socket,shelli);
				model.addClientCommunication(cc);
				Thread t = new Thread(cc);
				t.start();
				
				LinkedList<String> additionalParams = new LinkedList<String>();
				additionalParams.add(privateAddress);
				return model.getServerCommunication().sendAndListen("!register", additionalParams);
			}
		}else{
			return "Must be logged in to do this.";
		}
	}
	
	@Override
	@Command
	public String lastMsg() throws IOException {
		return model.getServerCommunication().lastMsg();
	}

	@Override
	@Command
	public String exit() throws IOException {
		String msg = this.logout();
		shelli.writeLine(msg);
		shelli.close();
		model.shutdown();
		return "Shutting down client...";
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link Client} component
	 */
	public static void main(String[] args) {
		Client client = new Client(args[0], new Config("client"), System.in,
				System.out);
		client.run();
	}

	// --- Commands needed for Lab 2. Please note that you do not have to
	// implement them for the first submission. ---

	@Override
	@Command
	public String authenticate(String username) throws IOException {
		//NOTE: look for key for user
		
		File pkey = new File(config.getString("keys.dir") + "/" + username + ".pem");
		if(!pkey.exists()){
			return "No private key for this user!";
		}
		//NOTE: generate challenge and encrypt message
		File f = new File(config.getString("chatserver.key"));
		PublicKey pk = Keys.readPublicPEM(f);
		SecureRandom sr = new SecureRandom();
		byte[] challenge = new byte[32];
		sr.nextBytes(challenge);
		String msg = "!authenticate";
		byte[] msgByte = msg.getBytes();
		byte[] userByte = username.getBytes();
		msgByte = Base64.encode(msgByte);
		userByte = Base64.encode(userByte);
		byte[] bOfSpace = (" ").getBytes();
		//bOfSpace = Base64.encode(bOfSpace);
		
		//NOTE: concat message-string
		byte[] message = new byte[challenge.length + msgByte.length + userByte.length + bOfSpace.length*2];
		ByteBuffer bb = ByteBuffer.wrap(message);
		bb.put(msgByte);
		bb.put(bOfSpace);
		bb.put(userByte);
		bb.put(bOfSpace);
		bb.put(challenge);
		
		//NOTE: encrypt message
		try {
			Cipher rsaCipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
			rsaCipher.init(Cipher.ENCRYPT_MODE, pk);
			byte[] cipher = rsaCipher.doFinal(message);
			//System.out.println(cipher.toString());
			cipher = Base64.encode(cipher);
			
			//NOTE: send message to server
			//System.err.println(cipher.toString());
			String answer = model.getServerCommunication().sendAndListen(true, cipher);
			
		} catch (NoSuchAlgorithmException e) {
			return "The requested rsa-cipher is not supported!";
		} catch (NoSuchPaddingException e) {
			return "The requested padding for the rsa-cipher is not supported!";
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return "ok";
	}

}
