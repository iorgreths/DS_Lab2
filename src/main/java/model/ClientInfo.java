package model;

import java.io.IOException;

import client.tcp.ServerCommunication;
import client.tcp.ctc.ClientCommunication;
import client.tcp.ctc.ClientListener;
import client.tcp.ctc.ClientSender;
import client.udp.ServerCommunicationUDP;

/**
 * Saved information on the client-side
 * @author Marcel Gredler
 *
 */
public interface ClientInfo {
	
	public boolean isOnline();
	
	public void setOnline(boolean online);
	
	public void addClientCommunication(ClientCommunication comm);
	
	public ClientSender getClientSender();
	
	public ClientCommunication getClientCommunication();
	
	/**
	 * Shut down all sockets on the client-side
	 * @throws IOEXception
	 */
	public void shutdown() throws IOException;
	
	/**
	 * Returns the udp-connections towards the server
	 * 
	 * @return
	 */
	public ServerCommunicationUDP getServerCommunicationUDP();
	
	/**
	 * Returns the tcp-connection towards the server
	 * 
	 * @return
	 */
	public ServerCommunication getServerCommunication();
	
	/**
	 * Start a new client listening thread
	 * 
	 * @param listener the information for the thread
	 * @return if the thread could be started
	 */
	public boolean startClientListenerThread(ClientListener listener);

}
