package model;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import client.tcp.ServerCommunication;
import client.tcp.ctc.ClientCommunication;
import client.tcp.ctc.ClientListener;
import client.tcp.ctc.ClientSender;
import client.udp.ServerCommunicationUDP;

public class ClientInfoImpl implements ClientInfo {

	private static int threads = 5;
	
	//NOTE: communication with server
	private ServerCommunicationUDP serverCommUDP;
	private ServerCommunication serverComm;
	
	//NOTE: being a server
	private ClientCommunication clientComm;
	private ClientSender sender;
	
	//NOTE: listening to clients
	private ExecutorService clientListener;
	private List<ClientListener> listeners;
	
	private boolean online;
	
	public ClientInfoImpl(ServerCommunicationUDP serverCommUDP, ServerCommunication serverComm){
		this.serverCommUDP = serverCommUDP;
		this.serverComm = serverComm;
		
		clientListener = Executors.newFixedThreadPool(threads);
		listeners = new LinkedList<ClientListener>();
		sender = new ClientSender();
		
		online = false;
	}

	@Override
	public void shutdown() throws IOException {
		serverCommUDP.closeSocket();
		serverComm.close();
		for(ClientListener cl : listeners){
			cl.close();
		}
		
		List<Runnable> runs = clientListener.shutdownNow();
		for(Runnable r : runs){
			ClientListener cl = (ClientListener) r;
			cl.close();
		}
		
		if(clientComm != null){
			clientComm.close();
		}
	}

	@Override
	public ServerCommunicationUDP getServerCommunicationUDP() {
		return serverCommUDP;
	}
	
	@Override
	public ServerCommunication getServerCommunication(){
		return serverComm;
	}

	@Override
	public boolean isOnline() {
		return online;
		
	}

	@Override
	public void setOnline(boolean online) {
		this.online = online;
		
	}

	@Override
	public boolean startClientListenerThread(ClientListener listener) {
		listeners.add(listener);
		clientListener.execute(listener);
		
		return true;
	}

	@Override
	public void addClientCommunication(ClientCommunication comm) {
		clientComm = comm;
	}
	
	@Override
	public ClientCommunication getClientCommunication(){
		return clientComm;
	}

	@Override
	public ClientSender getClientSender() {
		return sender;
	}
	
}
