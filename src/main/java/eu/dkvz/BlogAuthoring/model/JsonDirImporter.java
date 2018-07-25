package eu.dkvz.BlogAuthoring.model;

import org.springframework.boot.json.*;
import java.io.*;
import java.util.*;
import java.nio.file.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public final class JsonDirImporter {

	private final File dir;
	private final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	
	public JsonDirImporter(String path) {
		this.dir = new File(path);
	}
	
	/**
	 * List .json files that we can write (we need to remove them)
	 * Earliest files first
	 * @return
	 */
	public File[] lsEarliestFirst() {
		File[] ret = this.dir.listFiles(
            (file) -> file.getName().toLowerCase().endsWith(".json") &&
            	file.canWrite()
	    );
		Arrays.sort(ret, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));
		return ret;
	}
	
	public boolean isWritable() {
		return this.dir.canWrite();
	}
	
	// I don't use this method.
	private boolean mandatoryFieldsPresent(Map<String, Object> parsed) {
		return parsed.get("articleURL") != null &&
				parsed.get("summary") != null &&
				parsed.get("content") != null &&
				parsed.get("title") != null;
	}
	
	// This method is awful.
	// Now I know I should always use an object wrapper instead of this horror.
	public List<Article> importArticles() {
		// Try to parse the JSON.
		// Delete the files for which it doesn't work.
		List<Article> ret = new ArrayList<>();
		JsonParser parser = JsonParserFactory.getJsonParser();
		File[] files = this.lsEarliestFirst();
		for (File file : files) {
			// Read the file.
			String content;
			try {
				content = new String(Files.readAllBytes(file.toPath()));
			} catch (IOException ex) {
				// Can't read this file for some reason. Just forget about it.
				continue;
			}
			Map<String, Object> parsed;
			try {
				parsed = parser.parseMap(content);
				if (parsed == null) throw new JsonParseException();
			} catch (JsonParseException ex) {
				// Invalid file, delete it.
				// This might throw an IO exception and crash the app if
				// deleting doesn't work for some reason.
				file.delete();
				continue;
			}
			// Try to create the Article objects with no errors and strange stuff
			// in it.
			// Actually we'll check for this later.
			// If you provide an invalid article ID that is not a number, we just
			// add a new article.
			// We do need to parse the things that are numbers.
			Article art = new Article();
			if (parsed.get("id") instanceof Number) {
				Number num = (Number)parsed.get("id");
				art.getArticleSummary().setId(num.longValue());
			} else if (parsed.get("id") instanceof String) {
				try {
					art.getArticleSummary().setId(Long.parseLong((String)parsed.get("id")));
				} catch (NumberFormatException ex) {
					// Leave it at -1. It should already be at -1.
				}
			}
			// This is so ugly.
			// It could be a whole bunch of ternary operators but that would be just as ugly.
			// Yes I know there is a way to map to an object directly.
			// Burps.
			if (parsed.get("articleUrl") != null) {
				parsed.put("articleURL", parsed.get("articleUrl"));
			}
			if (parsed.get("articleURL") != null) art.getArticleSummary().setArticleURL(parsed.get("articleURL").toString());
			if (parsed.get("userId") != null) {
				User usr = new User();
				if (parsed.get("userId") instanceof Number) {
					Number uid = (Number)parsed.get("userId");
					usr.setId(uid.longValue());
					art.getArticleSummary().setUser(usr);
				} else {
					try {
						usr.setId(Long.parseLong(parsed.get("userId").toString()));
						art.getArticleSummary().setUser(usr);
					} catch(NumberFormatException ex) {
						// Nope.
					}
				}
			}
			if (parsed.get("content") != null) {
				art.setContent(parsed.get("content").toString());
			}
			if (parsed.get("summary") != null) {
				art.getArticleSummary().setSummary((parsed.get("summary").toString()));
			}
			// We might have to parse a date. Sad face.
			if (parsed.get("date") != null) {
				java.util.Date date = null;
				try {
					date = df.parse(parsed.get("date").toString());
				} catch (ParseException ex) {
					date = null;
				}
				if (date != null) {
					art.getArticleSummary().setDate(date);
				}
			}
			if (parsed.get("thumbImage") != null) {
				art.getArticleSummary().setThumbImage(parsed.get("thumbImage").toString());
			}
			if (parsed.get("title") != null) {
				art.getArticleSummary().setTitle(parsed.get("title").toString());
			}
			// We need to check if the tags are an array.
			if (parsed.get("tags") instanceof List) {
				// Normally "tags" in ArticleSummary is instantiated by the constructor.
				// Each item is actually a LinkedHashMap, but we can just use Map.
				// We just need the ids.
				for (Object o: (List<Object>)parsed.get("tags")) {
					if (o instanceof Map) {
						Map<String, Object> m = (Map<String, Object>)o;
						if (m.get("id") != null) {
							if (m.get("id") instanceof Number) {
								ArticleTag t = new ArticleTag();
								Number n = (Number)m.get("id");
								t.setId(n.longValue());
								art.getArticleSummary().getTags().add(t);
							} else if (m.get("id") instanceof String) {
								try {
									ArticleTag t = new ArticleTag();
									t.setId(Long.parseLong((String)m.get("id")));
									art.getArticleSummary().getTags().add(t);
								} catch (NumberFormatException ex) {
									// Don't add the tag.
								}
							}
						}
					}
				}
			}
			// Also check for published:
			if (parsed.get("published") != null) {
				if (parsed.get("published") instanceof Boolean) {
					art.getArticleSummary().setPublished((Boolean)parsed.get("published") == Boolean.TRUE ? 
							true : false);
				} else if (parsed.get("published") instanceof String) {
					String str = (String)parsed.get("published");
					if (str.equals("false")) art.getArticleSummary().setPublished(false);
					else art.getArticleSummary().setPublished(true);
				} else if (parsed.get("published") instanceof Number) {
					Number n = (Number)parsed.get("published");
					art.getArticleSummary().setPublished(
						n.intValue() != 0
					);
				} else {
					art.getArticleSummary().setPublished(true);
				}
			} else {
				art.getArticleSummary().setPublished(false);
			}
			// Parsed the article, we can now delete it:
			file.delete();
			ret.add(art);
		}
		return ret;
	}
	
}
