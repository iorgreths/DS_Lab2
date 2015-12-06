package model;

public class User {
	
	private String username;
	private String password;
	
	private boolean registered;
	private String registerString;
	
	private boolean online;
	
	public User(String username, String password){
		this.username = username;
		this.password = password;
		online = false;
		registered = false;
	}
	
	public void setOnline(){
		online = true;
	}
	
	public void setOffline(){
		online = false;
	}
	
	public void register(String address){
		registerString = address;
		registered = true;
	}
	
	public boolean isRegistered(){
		return registered;
	}
	
	public boolean isOnline(){
		return online;
	}
	
	public boolean login(String password){
		boolean on = (this.password != null) && (password != null) && (this.password.equals(password));
		online = on;
		return on;
	}
	
	public String getRegisteredAddress(){
		if(isRegistered()){
			return registerString;
		}else{
			return "";
		}
	}
	
	public String getUsername(){
		return username;
	}
	
	@Override
	public String toString(){
		return username + ":" + password;
	}
}
