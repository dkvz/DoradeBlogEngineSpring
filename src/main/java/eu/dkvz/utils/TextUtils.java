package eu.dkvz.utils;

import java.util.regex.*;

public class TextUtils {
	
	public static String processRelativeUrls(String content, String baseUrl) {
		// Looking for text that matches both src="" and href="".
		// We could look for single quotes too but I won't.
		// TODO Detect possible whitespaces before and after the "=".
		Pattern urls = Pattern.compile("(src=\"|href=\")(?!https?://)/?(.*?)\"", Pattern.MULTILINE);
		Matcher matcher = urls.matcher(content);
//		if (matcher.find()) {
//			return matcher.group(2);
//		} else {
//			return "No matches";
//		}
		return matcher.replaceAll("$1" + baseUrl + "/$2\"");
	}
	
}
