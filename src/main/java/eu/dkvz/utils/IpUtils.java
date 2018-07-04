package eu.dkvz.utils;

import javax.servlet.http.HttpServletRequest;

public class IpUtils {

	public static String extractFirstBytes(String ip) {
		String [] bytes = ip.split("\\.");
		if (bytes.length >= 3) {
			return bytes[0] + "." + bytes[1] + "." + bytes[2]; 
		} else return ip;
	}
	
	public static String getRealIp(HttpServletRequest request) {
		if (request.getHeader("X-Forwarded-For") != null) {
			return request.getHeader("X-Forwarded-For");
		} else return request.getRemoteAddr();
	}
	
}
