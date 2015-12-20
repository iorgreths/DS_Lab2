package util;

public class Thirds<k,v,c> {
	
	private k key;
	private v value;
	private c challenge;
	
	public Thirds(k key, v value, c challenge){
		this.key = key;
		this.value = value;
		this.challenge = challenge;
	}
	
	public k getKey(){
		return key;
	}
	
	public v getValue(){
		return value;
	}
	
	public c getChallenge(){
		return challenge;
	}

}
