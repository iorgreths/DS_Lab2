package client.tcp.ctc;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import cli.Shell;
import model.ClientInfo;

public class ClientCommunication implements Runnable{

	private ClientInfo info;
	
	private ServerSocket socket;
	private Shell shell;
	
	public ClientCommunication(ClientInfo info, ServerSocket socket, Shell shell){
		this.info = info;
		this.socket = socket;
		this.shell = shell;
		
	}
	
	@Override
	public void run() {
		
		boolean running = true;
		while(running){
			try {
				Socket sock = socket.accept();
				info.startClientListenerThread(new ClientListener(sock,shell));
				
			} catch (IOException e) {
				if(socket.isClosed()){
					running = false;
				}
			}
		}
		
	}
	
	/**
	 * Closes the socket
	 * 
	 * @throws IOException 
	 */
	public void close() throws IOException{
		socket.close();
		
	}
	
	//NOTE: static methods for checking registration
	public static boolean validAddress(String address){
		boolean retval = false;
		
		if(address != null){
			String[] split = address.split(":");
			if( (split.length == 2) ){
				try{
					Integer.valueOf(split[1]);
					retval = true;
				}catch(NumberFormatException e){
					retval = false;
				}
			}
		}
		return retval;
	}
	
}
