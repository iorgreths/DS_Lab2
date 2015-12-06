package client.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class ServerCommunicationUDP {

	private DatagramSocket udpSocket;
	
	private String servername;
	private int port;

	public ServerCommunicationUDP(DatagramSocket udp, String servername, int port){
		udpSocket = udp;
		this.servername = servername;
		this.port = port;
	}
	
	/**
	 * Close the udp-socket
	 */
	public void closeSocket(){
		udpSocket.close();
	}
	
	/**
	 * Send the !list request towards the server
	 * @throws IOException 
	 */
	public String sendListRequestToServer() throws IOException{
		//NOTE: send request
		String request = "!list";
		byte[] buffer = request.getBytes();
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(servername), port);
		udpSocket.send(packet);
		
		//NOTE: wait for reply
		buffer = new byte[1024];
		packet = new DatagramPacket(buffer, buffer.length);
		udpSocket.receive(packet);
		
		//NOTE: map the bytes towards a string
		String message = new String(packet.getData());
		if(message.indexOf(0) >=0){
			message = message.substring(0,message.indexOf(0));
		}
		return message;
	}
	
}
