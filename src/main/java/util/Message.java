package util;

public class Message {

	private static String separator = "SEPARATOR";
	private static final String B64 = "a-zA -Z0 -9/+ ";
	
	public static String[] splitMessage(String command){		
		String[] ret = command.split(separator);
		
		return ret;
	}
	
	public static String getSeparator(){
		return separator;
	}
	
	public static String removeShittyBit(String msg){
		String ret = msg;
		if(msg.indexOf(0) > 0){
			ret = msg.substring(0,msg.indexOf(0));
		}
		return ret;
	}
	
	public static void testMsg1(String msg1){
		
		String firstMessage = msg1;
		System.err.println("Testing Message1: " + firstMessage);
		assert firstMessage . matches ("! authenticate   [\\ w \\.]+  ["+ B64 +" ]{43}= ") : "1st  message ";
		
	}
	
	public static void testMsg2(String msg2){
		
		String secondMessage = msg2;
		System.err.println("Testing Message2: " + secondMessage);
		assert secondMessage . matches ("!ok ["+ B64 +" ]{43}=  ["+ B64 +" ]{43}=  ["+ B64+" ]{43}=  ["+B64 +"]{22}== ") : "2nd  message ";
		
	}
	
	public static void testMsg3(String msg3){
		
		String thirdMessage = msg3;
		System.err.println("Testing Message3: " + thirdMessage);
		assert thirdMessage . matches ("["+ B64 +" ]{43}= ") : "3rd  message ";
		
	}
	
	public static void testHashMsg(String msg){
		// --- stage III ---
		// Private messages being exchanged between clients before the final Base64 encoding
		String hashedMessage = msg;
		assert hashedMessage . matches ("["+ B64 +" ]{43}=   [\\ s [^\\ s]]+ ");
		System.err.println(hashedMessage.matches("["+ B64 +"]{43}= [\\s [^\\s]]+"));
	}
}
