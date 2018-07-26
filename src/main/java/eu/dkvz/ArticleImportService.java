package eu.dkvz;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import eu.dkvz.BlogAuthoring.model.*;
import java.util.*;

@Service
public class ArticleImportService {

	@Value("${import-path}")
	private String importPath;
	
	private JsonDirImporter jsDirImporter;
	
	@Autowired
    private BlogDataAccessSpring blogDataAccess;
	
	@PostConstruct
	public void initialize() throws Exception {
		this.jsDirImporter = new JsonDirImporter(this.importPath);
		if (!this.jsDirImporter.isWritable()) {
			throw new Exception("FATAL: The import path " + this.importPath + " is not writable.");
		}
	}
	
	public List<ImportedArticle> importJsonFiles() {
		List<ImportedArticle> imported = this.jsDirImporter.importArticles();
		// Check if we need to create or update.
		// Check if mandatory fields are present.
		// We'll also need to check if an articleUrl already exists ;
		// or if the article ID resolves to an article in DB (if updating).
		// We also need a list of all tags to check if they exist:
		List<ArticleTag> allTags = blogDataAccess.getAllTags();
		for (ImportedArticle art: imported) {
			if (!art.isError()) {
				User usr = blogDataAccess.getUser(art.getArticleSummary().getUser().getId());
				if (usr != null) {
					// Check if the tags exist:
					if (art.getArticleSummary().getTags() != null && art.getArticleSummary().getTags().size() > 0) {
						for (ArticleTag tag : art.getArticleSummary().getTags()) {
							if (!allTags.contains(tag)) {
								// Tag doesn't exist.
								art.setError(true);
								art.setMessage("One of the provided tags doesn't exist");
								continue;
							}
						}
					}
					if (art.getArticleSummary().getId() > 0) {
						// Updating:
						Article existingArt = blogDataAccess.getArticleById(art.getArticleSummary().getId());
						if (existingArt != null) {
							// In fact nothing is mandatory here.
							// Let's have the DB method do most of the work.
							
							
							
						} else {
							art.setError(true);
							art.setMessage("Provided article ID does not exist");
						}
					} else {
						// Add a new article to the database.
						// The fields title and articleURL are mandatory. Must be not null and 
						// longer than 0 and the articleURL must not already exist.
						// Actually, if it's a short we don't care about articleURL.
						try {
							if (art.getArticleSummary().getTitle() != null && 
									!art.getArticleSummary().getTitle().isEmpty()) {
								if (!art.isShortArticle() && 
										art.getArticleSummary().getArticleURL() != null &&
										!art.getArticleSummary().getArticleURL().isEmpty()) {
									// We're adding a full article.
									if (art.getArticleSummary().getSummary() == null) art.getArticleSummary().setSummary("");
									if (art.getContent() == null) art.setContent("");
									if (!blogDataAccess.insertArticle(art)) {
										art.setError(true);
										art.setMessage("Possible SQL error inserting new article");
									} else {
										art.setMessage("Created new article");
									}
								} else {
									// We're adding a short. And that is even if shortArticle is
									// actually false.
									if (!blogDataAccess.insertShort(art)) {
										art.setError(true);
										art.setMessage("Possible SQL error inserting new short");
									} else {
										art.setMessage("Created new short");
									}
								}
							} else {
								// No title.
								art.setError(true);
								art.setMessage("Articles must have a title");
							}
						} catch (DataAccessException ex) {
							art.setError(true);
							art.setMessage("SQL error, see stacktrace");
							ex.printStackTrace();
						}
					}
				} else {
					art.setError(true);
					art.setMessage("Provided user ID does not exist");
				}
			}
		}
		return imported;
	}
	
	
}
