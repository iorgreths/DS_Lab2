package client.tcp.ctc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import cli.Shell;

public class ClientListener implements Runnable{

	private Socket socket;
	private Shell shell;
	
	public ClientListener(Socket socket, Shell shell){
		this.socket = socket;
		this.shell = shell;
	}
	
	/**
	 * Close the socket
	 * @throws IOException 
	 */
	public void close() throws IOException{
		socket.close();
	}
	
	@Override
	public void run() {
		
		BufferedReader reader;
		PrintWriter writer;
		boolean running = true;
		while(running){
			
			try {
				reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				writer = new PrintWriter(socket.getOutputStream());
				String line = reader.readLine();
				if(line != null){
					shell.writeLine(line);
					writer.println("!ack");
				}else{
					writer.println("!nack");
				}
				writer.flush();
				
			} catch (IOException e) {
				if(socket.isClosed()){
					running = false;
				}
			}
			
		}
		
	}

}
