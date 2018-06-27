package eu.dkvz;

public class IpUtils {

	public static String extractFirstBytes(String ip) {
		String [] bytes = ip.split("\\.");
		if (bytes.length >= 3) {
			return bytes[0] + "." + bytes[1] + "." + bytes[2]; 
		} else return ip;
	}
	
}
