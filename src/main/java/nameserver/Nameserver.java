package nameserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import nameserver.exceptions.AlreadyRegisteredException;
import nameserver.exceptions.InvalidDomainException;
import cli.Command;
import cli.Shell;
import util.Config;

/**
 * Please note that this class is not needed for Lab 1, but will later be used
 * in Lab 2. Hence, you do not have to implement it for the first submission.
 */
public class Nameserver implements INameserverCli, Runnable{

	private String componentName;
	private Config config;
	private InputStream userRequestStream;
	private PrintStream userResponseStream;
	
	private Shell shelli;
	private Registry registry;
	private NameserverRemote myRemote; //NOTE: my own remote object (this nameserver)
	
	private INameserver exported;

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
	public Nameserver(String componentName, Config config,
			InputStream userRequestStream, PrintStream userResponseStream) {
		this.componentName = componentName;
		this.config = config;
		this.userRequestStream = userRequestStream;
		this.userResponseStream = userResponseStream;

		shelli = new Shell(componentName,userRequestStream,userResponseStream);
		shelli.register(this);
		if(componentName.equals("ns-root")){
			myRemote = new NameserverRemote("",shelli);
		}else{
			myRemote = new NameserverRemote(config.getString("domain"),shelli);
		}
		exported = null;
	}
	
	
	private void register(){
		if(componentName.equals("ns-root")){
			//NOTE: root-server starts registry and binds himself
			try{
				//NOTE: create registry
				registry = LocateRegistry.createRegistry(config.getInt("registry.port"));
				
				INameserver server = (INameserver) UnicastRemoteObject.exportObject(myRemote, config.getInt("registry.port"));
				exported = server;
				
				registry.bind(config.getString("root_id"), server);
				
				try {
					shelli.writeLine("Starting registry and binding root");
				} catch (IOException e) {
					System.err.println(e);
				}
			}catch(RemoteException e){
				//TODO
				System.err.println(e);
			}catch(AlreadyBoundException e){
				//TODO
				System.err.println(e);
			}
			
		}else{
			//NOTE: everyone else looks for root in the registry -> afterwards recursive 
			Registry registry;
			try {
				registry = LocateRegistry.getRegistry(config.getString("registry.host"),config.getInt("registry.port"));
				
				INameserver server = (INameserver) registry.lookup(config.getString("root_id"));
				INameserver myself = (INameserver) UnicastRemoteObject.exportObject(myRemote, 0);
				//INameserverForChatserver t = (INameserverForChatserver) UnicastRemoteObject.exportObject(myRemote, 0);
				//server.registerNameserver(config.getString("domain"), myRemote, myRemote);
				server.registerNameserver(config.getString("domain"), myself, null);
				
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NotBoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (AlreadyRegisteredException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvalidDomainException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			//TODO
		}
	}
	
	@Override
	public void run() {
		Thread t = new Thread(shelli);
		t.start();
		//NOTE: i am running -> register myself
		register();
	}

	@Override
	@Command
	public String nameservers() throws IOException {
		String ret = "";
		int c = 1;
		for(String s : myRemote.getKnownNameServer()){
			ret += c + ". " + s + "\n"; 
			c++;
		}
		return ret;
	}

	@Override
	@Command
	public String addresses() throws IOException {
		String ret = "";
		int c = 1;
		for(String s : myRemote.getKnownUsersWithAddress()){
			ret += c + ". " + s + "\n";
			c++;
		}
		return ret;
	}

	@Override
	@Command
	public String exit() throws IOException {
		UnicastRemoteObject.unexportObject(myRemote, true);
		if(exported != null){
			//NOTE: exported will only be set if an object has been bound -> root-nameserver
			//UnicastRemoteObject.unexportObject(myRemote, true);
			try {
				registry.unbind(config.getString("root_id"));
			} catch (NotBoundException e) {
				//NOTE: this should never happen
				System.err.println(e);
			}
		}
		shelli.close();
		return "Shutting down nameserver '" + componentName + "' ...";
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link Nameserver}
	 *            component
	 */
	public static void main(String[] args) {
		Nameserver nameserver = new Nameserver(args[0], new Config(args[0]),
				System.in, System.out);
		nameserver.run();
	}

}
