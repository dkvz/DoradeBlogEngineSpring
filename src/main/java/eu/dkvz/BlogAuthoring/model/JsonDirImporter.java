package eu.dkvz.BlogAuthoring.model;

import org.springframework.boot.json.*;
import java.io.*;
import java.util.*;
import java.nio.file.*;

public final class JsonDirImporter {

private final File dir;
	
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
	
	private boolean mandatoryFieldsPresent(Map<String, Object> parsed) {
		return parsed.get("articleURL") != null &&
				parsed.get("summary") != null &&
				parsed.get("content") != null &&
				parsed.get("title") != null;
	}
	
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
			if (parsed.get("articleId") instanceof Number) {
				Number num = (Number)parsed.get("articleId");
				art.getArticleSummary().setId(num.longValue());
			} else if (parsed.get("articleId") instanceof String) {
				try {
					art.getArticleSummary().setId(Long.parseLong((String)parsed.get("articleId")));
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
				art.getArticleSummary().setUser(new User());
				art.getArticleSummary().getUser().setName(parsed.get("userId").toString());
			}
			if (parsed.get("content") != null) {
				art.setContent(parsed.get("content").toString());
			}
			if (parsed.get("summary") != null) {
				art.setContent(parsed.get("summary").toString());
			}
			// We might have to parse a date. I know...
			
		}
		return ret;
	}
	
}
