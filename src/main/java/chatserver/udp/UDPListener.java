/**
 * 
 */
package chatserver.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Map.Entry;

import model.ServerInfo;

/**
 * @author Marcel Gredler
 * @version 1.0
 * 
 * Implementation of the UDP requirements
 *
 */
public class UDPListener implements Runnable {

	private DatagramSocket udpSocket;
	private ServerInfo info;
	
	/**
	 * 
	 * @param udpSocket - a connected (and open!) {@code DatagramSocket}
	 */
	public UDPListener(DatagramSocket udpSocket, ServerInfo info){
		if( (udpSocket == null) || (udpSocket.isClosed()) ){
			throw new IllegalArgumentException("UDP-Port has to be connected and open!");
		}else{
			this.udpSocket = udpSocket;
		}
		this.info = info;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		byte[] buffer;
		DatagramPacket packet;
		
		/*
		 * Run will be set false as soon as the socket throws a closed exception
		 * -> see exception-handling
		 */
		boolean run = true;
		while( run ){
			
			buffer = new byte[1024];
			packet = new DatagramPacket(buffer, buffer.length); //NOTE: create a new packet, which will be filled
			
			try {
				//NOTE: wait for packets -> may throw exception (e.g. socket is closed
				udpSocket.receive(packet);
				
				ResponseSender rs = new ResponseSender(packet, udpSocket);
				Thread t = new Thread(rs);
				t.start();
				
				//NOTE: has been switched to it's on thread
				//NOTE: map the bytes towards a string
				//String message = new String(packet.getData());
				
				//NOTE: is the message valid
				
				/*
				 * valid messages:
				 * -> !list
				 */
				/*String response;
				String msg = message.substring(0,5);
				boolean ok = true;
				for(int i=5; i<message.length(); i++){
					if( ((int)message.charAt(i)) != 0){
						ok = false;
					}
				}
				if( (message != null) && (msg.equals("!list")) && (ok) ){
					//NOTE: send appropiate response
					response = "Online users:\n";
					for(Entry<String, String> e : info.getUsers().entrySet()){
						if(e.getValue().equals("online")){
							response = response + e.getKey() + "\n";
						}
					}
					response = response.substring(0, response.lastIndexOf("\n"));
					
				}else{
					//NOTE: send error message
					response = "!error Doesn't know provided command! Known commands: ";
					response += " <!list>";
					
				}
				
				//NOTE: write response to user
				InetAddress address = packet.getAddress();
				int port = packet.getPort();
				buffer = response.getBytes(); //NOTE: String -> byte[]
				packet = new DatagramPacket(buffer, buffer.length, address, port);
				udpSocket.send(packet); //NOTE: send packet through udp-port
				*/
				
			} catch (IOException e) {
				
				if( udpSocket.isClosed() ){
					//NOTE: exception should have been thrown because the socket is closed
				    //      -> set run = false
					run = false;
				}else{
					//NOTE: a different error occured -> notify server-admin
					System.err.println("An error ocurred while handling UDP connections! \n " + e.getLocalizedMessage());
				}
			}
			
		}

	}
	
	/**
	 * Responsible for processing the response to the client.
	 * 
	 * @author Marcel Gredler
	 *
	 */
	private class ResponseSender implements Runnable{

		private DatagramPacket packet;
		private DatagramSocket udpSocket;
		
		public ResponseSender(DatagramPacket packet, DatagramSocket udpSocket){
			this.packet = packet;
			this.udpSocket = udpSocket;
		}
		
		@Override
		public void run() {
			
			try {
				
				//NOTE: map the bytes towards a string
				String message = new String(packet.getData());
				
				//NOTE: is the message valid
				
				/*
				 * valid messages:
				 * -> !list
				 */
				String response;
				String msg = message.substring(0,5);
				boolean ok = true;
				for(int i=5; i<message.length(); i++){
					if( ((int)message.charAt(i)) != 0){
						ok = false;
					}
				}
				if( (message != null) && (msg.equals("!list")) && (ok) ){
					//NOTE: send appropiate response
					response = "Online users:\n";
					for(Entry<String, String> e : info.getUsers().entrySet()){
						if(e.getValue().equals("online")){
							response = response + e.getKey() + "\n";
						}
					}
					response = response.substring(0, response.lastIndexOf("\n"));
					
				}else{
					//NOTE: send error message
					response = "!error Doesn't know provided command! Known commands: ";
					response += " <!list>";
					
				}
				
				//NOTE: write response to user
				InetAddress address = packet.getAddress();
				int port = packet.getPort();
				byte[] buffer;
				buffer = response.getBytes(); //NOTE: String -> byte[]
				packet = new DatagramPacket(buffer, buffer.length, address, port);
				udpSocket.send(packet); //NOTE: send packet through udp-port
				
			} catch (IOException e) {
				
				if( udpSocket.isClosed() ){
					//NOTE: exception should have been thrown because the socket is closed
				    
				}else{
					//NOTE: a different error occured -> notify server-admin
					System.err.println("An error ocurred while handling UDP connections! \n " + e.getLocalizedMessage());
				}
			}
			
		}
		
	}

}
