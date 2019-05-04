package eu.dkvz.BlogAuthoring.model;

public class BlogDataAccess {

	protected static String generateOrderBy(String field, String order) {
		return "ORDER BY " + field + 
				(order.toLowerCase().contains("asc") ? " ASC" : " DESC");
	}
	
}
