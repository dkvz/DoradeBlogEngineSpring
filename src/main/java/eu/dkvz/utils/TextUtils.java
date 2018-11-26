package eu.dkvz.utils;

import java.util.regex.*;

public class TextUtils {
	
	public static final Pattern relativeLinksPattern = Pattern.compile("(src=\"|href=\")(?!https?://)/?(.*?)\"", Pattern.MULTILINE);
	
	public static String processRelativeUrls(String content, String baseUrl) {
		// Looking for text that matches both src="" and href="".
		// We could look for single quotes too but I won't.
		// TODO Detect possible whitespaces before and after the "=".
		Matcher matcher = TextUtils.relativeLinksPattern.matcher(content);
//		if (matcher.find()) {
//			return matcher.group(2);
//		} else {
//			return "No matches";
//		}
		return matcher.replaceAll("$1" + baseUrl + "/$2\"");
	}
	
}
