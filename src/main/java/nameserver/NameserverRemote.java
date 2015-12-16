package nameserver;

import java.rmi.RemoteException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import cli.Shell;
import nameserver.exceptions.AlreadyRegisteredException;
import nameserver.exceptions.InvalidDomainException;

public class NameserverRemote implements INameserver{

	/**
	 * 
	 */
	//private static final long serialVersionUID = -1144999507208352516L;
	//private List<NameserverRemote> myHandledRemotes;
	private Map<String,INameserver> myHandledRemotes;
	private String myDomain;
	//private Shell shelli;
	
	//NOTE: handling users
	private Map<String,String> knownUsers;
	
	public NameserverRemote(String myDomain, Shell shelli){
		HashMap<String,INameserver> tempMap = new HashMap<>();
		myHandledRemotes = Collections.synchronizedMap(tempMap);
		//LinkedList<NameserverRemote> temp = new LinkedList<>();
		//myHandledRemotes = Collections.synchronizedList(temp);
		this.myDomain = myDomain;
		
		//this.shelli = shelli;
		Map<String,String> tempMap2 = new HashMap<>();
		knownUsers = Collections.synchronizedMap(tempMap2);
	}
	
	@Override
	public void registerUser(String username, String address)
			throws RemoteException, AlreadyRegisteredException,
			InvalidDomainException {
		System.out.println("Incoming command with " + username + " " + address);
		if(username.contains(".")){
			String last = username.substring(username.lastIndexOf(".")+1);
			boolean found = false;
			for(Entry<String,INameserver> e : myHandledRemotes.entrySet()){
				if(e.getKey().equals(last)){
					username = username.substring(0,username.lastIndexOf("."));
					e.getValue().registerUser(username, address);
					found = true;
					break;
				}
				
				if(!found){
					throw new InvalidDomainException("There is no domain for this user: " + username);
				}
			}
		}else{
			//NOTE: add user to our domain, if we don`t have him already
			if(knownUsers.containsKey(username)){
				throw new AlreadyRegisteredException("This user has already been registered, but nice try bro!");
			}else{
				System.out.println("Saving users at domain " + myDomain + " with " + username);
				knownUsers.put(username, address);
				System.out.println("Saved user? " + knownUsers.get(username));
			}
			
		}
		
	}

	@Override
	public INameserverForChatserver getNameserver(String zone)
			throws RemoteException {
		INameserverForChatserver retval = null;
		for(Entry<String,INameserver> e : myHandledRemotes.entrySet()){
			if(e.getKey().equals(zone)){
				retval = e.getValue();
			}
		}
		if(retval == null){
			throw new RemoteException("No known nameserver for domain '" + zone + "'");
		}
		return retval;
	}

	@Override
	public String lookup(String username) throws RemoteException {
		String retval = null;
		for(Entry<String,String> e : knownUsers.entrySet()){
			if(e.getKey().equals(username)){
				retval = e.getValue();
			}
		}
		if(retval == null){
			throw new RemoteException("No known user '" + username + "'");
		}
		return retval;
	}

	@Override
	public void registerNameserver(String domain, INameserver nameserver,
			INameserverForChatserver nameserverForChatserver)
			throws RemoteException, AlreadyRegisteredException,
			InvalidDomainException {
		
		if( myDomain.equals("") && !domain.contains(".") ){
			//NOTE: add to root
			boolean contains = false;
			for(Entry<String,INameserver> e : myHandledRemotes.entrySet()){
				if(e.getKey().equals(domain)){
					contains = true;
					break;
				}
			}
			if(contains){
				throw new AlreadyRegisteredException("There is already a server for " + domain);
			}
			
			DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
			Calendar cal = Calendar.getInstance();
			String date  = dateFormat.format(cal.getTime()); //2014/08/06 16:00:22
			System.out.println(date + " Registering domain '" + domain + "'");
			/*try {
				shelli.writeLine("Registering domain '" + domain + "'");
			} catch (IOException e) {
				System.err.println(e);
			}*/
			myHandledRemotes.put(domain, nameserver);
			//NameserverRemote server = (NameserverRemote) nameserver;
			//myHandledRemotes.add(server);
		}else if( myDomain.equals("") && domain.contains(".") ){
			//NOTE: look for next zone-server
			String last = domain.substring(domain.lastIndexOf(".")+1);
			
			boolean found = false;
			for(Entry<String,INameserver> e : myHandledRemotes.entrySet()){
				//System.out.println("REMOTE_DOMAIN: " + nr.myDomain);
				if(e.getKey().equals(last)){
					found = true;
					e.getValue().registerNameserver(domain, nameserver, nameserverForChatserver);
					break;
				}
				
			}
			if(!found){
				//System.out.println(myDomain);
				throw new InvalidDomainException("Cannot register invalid domain: " + domain);
			}
		}else{
			
			//NOTE: i am not the root
			//NOTE: myDomain is clearly part of the domain
			String currDom = domain.substring(0,domain.indexOf(myDomain)-1);
			//System.out.println(currDom);
			if(currDom.contains(".")){
				//NOTE: recursive register
				String last = domain.substring(domain.lastIndexOf(".")+1);
				boolean found = false;
				for(Entry<String,INameserver> e : myHandledRemotes.entrySet()){
					if(e.getKey().equals(last)){
						found = true;
						e.getValue().registerNameserver(currDom, nameserver, nameserverForChatserver);
						break;
					}
				}
				if(!found){
					//System.out.println(myDomain);
					throw new InvalidDomainException("Cannot register invalid domain: " + domain);
				}
			}else{
				//NOTE: i have to add him
				boolean contains = false;
				for(Entry<String,INameserver> e : myHandledRemotes.entrySet()){
					if(e.getKey().equals(currDom)){
						contains = true;
						break;
					}
				}
				if(contains){
					throw new AlreadyRegisteredException("There is already a server for " + domain);
				}
				DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
				Calendar cal = Calendar.getInstance();
				String date  = dateFormat.format(cal.getTime()); //2014/08/06 16:00:22
				System.out.println("[" + date + "] Registering domain '" + domain + "'");
				/*try {
					shelli.writeLine("Registering domain '" + domain + "'");
				} catch (IOException e) {
					System.err.println(e);
				}*/
				myHandledRemotes.put(currDom, nameserver);
				//NameserverRemote server = (NameserverRemote) nameserver;
				//System.out.println(server.myDomain);
				//myHandledRemotes.add(server);
			}
		}
		
	}
	
	public List<String> getKnownNameServer(){
		LinkedList<String> server = new LinkedList<>();
		for(Entry<String,INameserver> e : myHandledRemotes.entrySet()){
			server.add(e.getKey());
		}
		Collections.sort(server);
		return server;
	}
	
	public List<String> getKnownUsersWithAddress(){
		LinkedList<String> users = new LinkedList<>();
		System.out.println(knownUsers.size());
		for(Entry<String, String> e : knownUsers.entrySet()){
			System.out.println(e.getKey());
			users.add(e.getKey() + " " + e.getValue());
		}
		Collections.sort(users);
		return users;
	}

}
