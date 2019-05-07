package eu.dkvz.BlogAuthoring.model;

import org.jsoup.Jsoup;

public class BlogDataAccess {

	protected static String generateOrderBy(String field, String order) {
		return "ORDER BY " + field + 
			(order.toLowerCase().contains("asc") ? " ASC" : " DESC");
	}

	/**
	 * Removes all the HTML from text data
	 * @param text The text to purify
	 * @return The purified text
	 */
	protected static String purifyText(String text) {
		return Jsoup.parse(text).text();
	}
	
}
