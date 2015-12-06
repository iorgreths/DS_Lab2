package model;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.Map;

import chatserver.tcp.TCPListener;

/**
 * Saved information about users, server, threads and sockets
 * @author Marcel Gredler
 *
 */
public interface ServerInfo {

	/**
	 * Add a new connection to a chat client
	 * @param run - the information used by the thread
	 * @return if the thread could be started
	 */
	public boolean startTCPThread(TCPListener run);
	
	
	/**
	 * Set the socket on which the server listens for new connections
	 * 
	 * @param socket the TCP-socket
	 */
	public void setTCPSocket(ServerSocket socket);
	
	/**
	 * Set the socket on which the server listens for udp-traffic
	 * 
	 * @param socket the UDP-socket
	 */
	public void setUDPSocket(DatagramSocket socket);
	
	/**
	 * Close all sockets and shutdown threads
	 * 
	 * @throws IOException - if the sockets could not be closed
	 */
	public void shutdown() throws IOException;
	
	/**
	 * Get information about all users
	 * @return a map containing usernames as key and their status as value
	 */
	public Map<String,String> getUsers();
	
	/**
	 * Returns the user for a certain username
	 * 
	 * @param username - the name of the user
	 * @return the user-object or null
	 */
	public User getUser(String username);
	
	/**
	 * Login a user
	 * 
	 * @return a new logged in user or null
	 */
	public User loginUser(String username, String password);
	
	/**
	 * Send to all online users a message
	 * 
	 * @param msg - the message to be send
	 * @param sender - the user which sends the message
	 */
	public void sendToAll(String msg, User sender);
}
