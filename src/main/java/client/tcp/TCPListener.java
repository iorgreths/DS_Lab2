package client.tcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import cli.Shell;

/**
 * @author Marcel Gredler
 * @verson 1.0
 *
 * Implementation of the TCP requirements (client to client)
 */
public class TCPListener implements Runnable {

	private Socket tcpSocket;
	private Shell shell;
	
	public TCPListener(Socket socket, Shell shell){
		tcpSocket = socket;
		this.shell = shell;
	}
	
	@Override
	public void run() {
		
		boolean running = true;
		BufferedReader reader;
		while(running){
			try {
				reader = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
				String line = reader.readLine();
				
				shell.writeLine(line);
				
				//reader.close();
			} catch (IOException e) {
				if(tcpSocket.isClosed()){
					running = false;
				}
			}
		}

	}
	
	/**
	 * Send a message to a client through the tcp-socket
	 * @param msg - the message for the client
	 * @throws IOException 
	 */
	public void sendMessageClient(String msg) throws IOException{
		PrintWriter pw = new PrintWriter(tcpSocket.getOutputStream());
		pw.println(msg);
		//pw.close();
	}
	
	public void close() throws IOException{
		tcpSocket.close();
	}

}
