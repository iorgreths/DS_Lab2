package client.tcp;

import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import cli.Shell;

/**
 * @author Marcel Gredler
 * @verson 1.0
 *
 * Implementation of the TCP requirements (client to server)
 */
public class ServerCommunication implements Runnable{

	private Thread tcpThread;
	private Shell shell;
	
	private ServerListener listener;
	private AtomicBoolean lock;
	
	private String lastMessage;
	
	public ServerCommunication(Socket socket, Shell shell){
		listener = new ServerListener(socket);
		this.shell = shell;
		lock = new AtomicBoolean();
		lock.set(false);
		
		lastMessage = "";
	}
	
	public void close() throws IOException{
		listener.close();
		tcpThread.interrupt();
	}
	
	/**
	 * Send a command to the server and listen to the servers reply.
	 * Note that sending the command !send will not listen for a reply
	 * @param command the command for the server (e.g. !send, !login)
	 * @param additionalParams further information for the command (e.g. for !login <username> <password>)
	 * @return the answer of the server
	 * @throws IOException
	 */
	public String sendAndListen(String command, List<String> additionalParams) throws IOException{
		String ret = "";
		
		//NOTE: scraped locking => using a different protocol now
		//NOTE: lock reading
		//lock.set(true);
		
		//NOTE: prepare message to send
		String msg = command;
		for(String s : additionalParams){
			//NOTE: add information; e.g. username and password
			msg = msg + " " + s;
		}
		
		//NOTE: send message
		listener.send(msg);
		
		//NOTE: get answer (if not a !send)
		if(!command.equals("!send")){
			while(!listener.isMessageAvailable()){
				//wait for answer
			}
			ret = listener.readMessage();
			
		}else{
			ret = "";
		}
		
		//NOTE: release lock
		//lock.set(false);
		//tcpThread.interrupt();
		
		return ret;
	}

	@Override
	public void run() {
		tcpThread = Thread.currentThread();
		
		//NOTE: start an extra reading thread
		Thread t = new Thread(listener);
		t.start();
		
		//NOTE: reading from listener
		boolean running = true;
		while(running){
			
			if(listener.isClosed()){
				running = false;
			}
			
			if(!lock.get()){
				try{
					//NOTE: using a queue for pubmsgs now
					/*
					if(listener.isMessageAvailable()){
						lastMessage = listener.readMessage();
						shell.writeLine(lastMessage);
					}*/
					if(!listener.isQueueEmpty()){
						lastMessage = listener.getOldestMessage();
						shell.writeLine(lastMessage);
					}
				}catch (IOException e) {
					//no prob
				}
				
				/*try {
					Thread.sleep(300);
				} catch (InterruptedException e) {
					//no problem
				}*/
			}
		}
	}
	
	/**
	 * Return the last received public message
	 * @return
	 */
	public String lastMsg(){
		return lastMessage;
	}
	
}
