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
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.LinkedList;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.util.encoders.Base64;

import model.ClientInfo;
import model.ClientInfoImpl;
import cli.Command;
import cli.Shell;
import client.tcp.ServerCommunication;
import client.tcp.ctc.ClientCommunication;
import client.udp.ServerCommunicationUDP;
import util.Config;
import util.Decrypter;
import util.Encrypter;
import util.Keys;
import util.Message;
import util.Pair;
import util.Thirds;

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
	private Pair<Encrypter,Decrypter> aes;

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
		aes = null;
		
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
		//NOTE: send message 1
		String retval = "authenticating ...";
		try {
			
			Thirds<List<String>,util.Encrypter,String> msg1 = prepareAuthenticateMSG1(username);
			
			//NOTE: send message to server
			File pkey = new File(config.getString("keys.dir") + "/" + username + ".pem");
			if(!pkey.exists()){
				return "No key for this user!";
			}
			PrivateKey priv = Keys.readPrivatePEM(pkey);
			Decrypter dec = new Decrypter("RSA/NONE/OAEPWithSHA256AndMGF1Padding",priv);
			String answer = model.getServerCommunication().sendAndListen("!authenticate", msg1.getKey(), msg1.getValue(), dec);
			//System.out.println("MSG2: " + answer);
						
			//NOTE: answer is MSG2 -> handle it
			String[] msg_split = answer.split(" ");
			if(msg_split.length != 5){
				return "Invalid response from server, message not conform!";
			}
			if(!msg_split[0].equals("!ok")){
				return "Server terminated handshake!";
			}
			if(!msg_split[1].equals(msg1.getChallenge())){
				return "Server sent invalid response (challenge failed!), terminating handshake!";
			}
			//NOTE: challenge ok -> create aes-pair
			byte[] encodedKey = Base64.decode(msg_split[3]);
			SecretKey secret = new SecretKeySpec(encodedKey, 0, encodedKey.length, "AES");
			byte[] iv = Base64.decode(msg_split[4]);
			String encoding = "AES/CTR/NoPadding";
			Encrypter e = new Encrypter(encoding,secret,iv);
			Decrypter d = new Decrypter(encoding,secret,iv);
			aes = new Pair<>(e,d);
			
			//NOTE send MSG3 to server
			List<String> param = new LinkedList<>();
			
			answer = model.getServerCommunication().sendAndListen(msg_split[2], param, e, d);
			
			/*if( (new String(answer)).equals("No key for this user!") ){
				retval = "No key for this user!";
			}else{
				prepareAuthenticateMSG3(username,msg1[0],msg1[1]);
			}*/
			retval = "Successfully authenticated!";
			
		} catch (NoSuchAlgorithmException e) {
			return "The requested rsa-cipher is not supported!";
		} catch (NoSuchPaddingException e) {
			return "The requested padding for the rsa-cipher is not supported!";
		} catch (IllegalBlockSizeException e) {
			return "The block-size for the rsa-cipher is illegal!";
		} catch (BadPaddingException e) {
			return "Bad padding for encryption!";
		} catch (InvalidKeyException e) {
			e.printStackTrace();
			return "The used key is invalid!";
		} catch (InvalidAlgorithmParameterException e1) {
			e1.printStackTrace();
			return "Got an invalid iv from server!";
		}
		
		return retval;
	}
	
	/**
	 * Encondes each part with Base64 before combining them
	 * @param parts
	 * @return
	 */
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
	}
	
	/**
	 * Returns the {cipher,challenge} for this message.
	 * If this cannot be done -> no key for this user the result will be {msg}
	 * @param username
	 * @return
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws InvalidKeyException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 */
	private Thirds<List<String>,util.Encrypter,String> prepareAuthenticateMSG1(String username) throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException{
		
		//NOTE: generate challenge and create encrypter
		File f = new File(config.getString("chatserver.key"));
		PublicKey pk = Keys.readPublicPEM(f);
		SecureRandom sr = new SecureRandom();
		byte[] challenge = new byte[32];
		sr.nextBytes(challenge);
		
		byte[] userByte = username.getBytes();
		
		//NOTE: base64
		challenge = Base64.encode(challenge);
		userByte  = Base64.encode(userByte);
		
		//NOTE: create encryptor
		util.Encrypter enc = new util.Encrypter("RSA/NONE/OAEPWithSHA256AndMGF1Padding", pk);
		
		List<String> param = new LinkedList<String>();
		param.add(new String(userByte));
		param.add(new String(challenge));
		
		return new Thirds<List<String>,util.Encrypter,String>(param,enc,new String(challenge));
	}
	
	/**
	 * Returns the {cipher,challenge} for this message.
	 * If this cannot be done -> no key for this user the result will be {msg}
	 * @param username
	 * @return
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws InvalidKeyException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 */
	/* EXPERIMENTAL
	 * private byte[][] prepareAuthenticateMSG1(String username) throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException{
		//File pkey = new File("keys/chatserver/bill.de.pub.pem");
		File pkey = new File(config.getString("keys.dir") + "/" + username + ".pem");
		if(!pkey.exists()){
			byte[][] retlist = {("No private key for this user!").getBytes()};
			return retlist;
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
		//byte[] bOfSpace = ("\n").getBytes();
		
		//NOTE: concat message-string
		byte[][] blist = {msgByte,Message.getSeparator().getBytes(),userByte,Message.getSeparator().getBytes(),challenge};
		byte[] message = createFullMessage(blist);
		
		//message = Base64.encode(message);
		//NOTE: encrypt message
		Cipher rsaCipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
		rsaCipher.init(Cipher.ENCRYPT_MODE, pk);
		byte[] cipher = rsaCipher.doFinal(message);
		
		byte[][] retlist = {cipher,challenge};
		return retlist;
	}*/
	
	private void prepareAuthenticateMSG3(String username, byte[] msg, byte[] challenge) throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException{
		//NOTE: character for separation of messages
		char ch = 0;
		String shittybit = String.valueOf(ch);// + String.valueOf(ch);
		
		File f = new File(config.getString("keys.dir") + "/" + username + ".pem");
		System.err.println("\n\n"+f.getAbsolutePath());
		PrivateKey pk = Keys.readPrivatePEM(f);
		
		//NOTE: deocde message
		Cipher rsaCipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
		rsaCipher.init(Cipher.DECRYPT_MODE, pk);
		
		byte[] plaintext = rsaCipher.doFinal(msg);
		String message = new String(Base64.decode(plaintext));
		
		//NOTE: split message
		int count = 0;
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
				System.out.println(s);
			}
		}
	}

}
