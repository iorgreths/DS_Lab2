package chatserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Map.Entry;

import chatserver.tcp.TCPListener;
import chatserver.udp.UDPListener;
import cli.Command;
import cli.Shell;
import model.ServerInfo;
import model.ServerInfoImpl;
import util.Config;

public class Chatserver implements IChatserverCli, Runnable {

	@SuppressWarnings("unused")
	private String componentName;
	private Config config;
	@SuppressWarnings("unused")
	private InputStream userRequestStream;
	@SuppressWarnings("unused")
	private PrintStream userResponseStream;
	
	private ServerInfo model;
	
	private Thread tcpthread;
	private Thread udpthread;
	//private Thread shellthread;
	
	private ServerSocket tcp;
	private DatagramSocket udp;
	
	private Shell shelli;

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
	public Chatserver(String componentName, Config config,
			InputStream userRequestStream, PrintStream userResponseStream) {
		this.componentName = componentName;
		this.config = config;
		this.userRequestStream = userRequestStream;
		this.userResponseStream = userResponseStream;

		model = new ServerInfoImpl(userResponseStream);
		
		shelli = new Shell(componentName, userRequestStream, userResponseStream);
		shelli.register(this);
	}
	
	public void prepareChatServer() throws IOException{
		tcp = new ServerSocket(config.getInt("tcp.port"));
		udp = new DatagramSocket(config.getInt("udp.port"));
		model.setTCPSocket(tcp);
		model.setUDPSocket(udp);
	}

	@Override
	public void run() {
		
		try {
			prepareChatServer();
			UDPListener udpL = new UDPListener(udp,model);
			udpthread = new Thread(udpL);
			tcpthread = Thread.currentThread();
			Thread shell = new Thread(shelli);
			
			udpthread.start();
			shell.start();
			
			boolean running = true;
			while(running){
				try {
					Socket sock = tcp.accept();
					model.startTCPThread(new TCPListener(sock,model,config.getString("registry.host"),config.getInt("registry.port"),config.getString("root_id")));
				} catch (IOException e) {
					if(tcp.isClosed()){
						running = false;
					}
				}
			}
			
			shelli.close();
			
		} catch (UnknownHostException e) {
			System.out.println("Could not start chatserver!\n" + e.getLocalizedMessage());
		} catch (IOException e) {
			System.out.println("Could not start chatserver!\n" + e.getLocalizedMessage());
		}
		
	}

	@Override
	@Command
	public String users() throws IOException {
		String ret = "";
		for(Entry<String, String> e : model.getUsers().entrySet()){
			ret = ret + e.getKey() + " " + e.getValue() + "\n";
		}
		ret = ret.substring(0,ret.lastIndexOf("\n"));
		return ret;
	}

	@Override
	@Command
	public String exit() throws IOException {
		model.shutdown();
		shelli.close();
		udpthread.interrupt();
		tcpthread.interrupt();
		return "Shutting down server...";
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link Chatserver}
	 *            component
	 */
	public static void main(String[] args) {
		Chatserver chatserver = new Chatserver(args[0],
				new Config("chatserver"), System.in, System.out);
		chatserver.run();
	}

}
