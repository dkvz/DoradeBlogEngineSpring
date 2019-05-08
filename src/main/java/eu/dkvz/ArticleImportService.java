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
		// I feel dirty for using labels.
		mainLoop: for (ImportedArticle art: imported) {
			if (!art.isError()) {
				if (art.getArticleSummary().getUser() != null && 
						art.getArticleSummary().getUser().getId() > 0 &&
						blogDataAccess.getUser(art.getArticleSummary().getUser().getId()) == null) {
							// User does not exist.
							art.setError(true);
							art.setMessage("Provided user does not exist");
							continue mainLoop;
				}
				// Check if the tags exist:
				if (art.getArticleSummary().getTags() != null && art.getArticleSummary().getTags().size() > 0) {
					for (ArticleTag tag : art.getArticleSummary().getTags()) {
						if (!allTags.contains(tag)) {
							// Tag doesn't exist.
							art.setError(true);
							art.setMessage("One of the provided tags doesn't exist");
							continue mainLoop;
						}
					}
				}
				if (art.getArticleSummary().getId() > 0) {
					// Check the action, we need to delete the article if that's what
					// was requested:
					Article existingArt = blogDataAccess.getArticleById(art.getArticleSummary().getId());
					// Updating or deleting:
					if (existingArt != null) {
						if (art.getAction() == ImportedArticle.ACTION_DELETE) {
							try {
								blogDataAccess.deleteArticleById(art.getArticleSummary().getId());
								art.setMessage("Delete article or short");
							} catch (DataAccessException ex) {
								art.setError(true);
								art.setMessage("SQL Error deleting article - " + ex.getMessage());
								ex.printStackTrace();
							}
						} else {
							// Updating the article
							// In fact nothing is mandatory here.
							// Let's have the DB method do most of the work.
							try {
								// Check if we're updating the URL and if it already exists:
								if (art.getArticleSummary().getArticleURL() != null && 
										!art.getArticleSummary().getArticleURL().equals(existingArt.getArticleSummary().getArticleURL()) &&
											blogDataAccess.getArticleByUrl(art.getArticleSummary().getArticleURL()) != null) {
										// This URL is already in use.
										art.setError(true);
										art.setMessage("Provided URL already exists");
								} else {
									blogDataAccess.updateArticle(art);
									art.setMessage("Updated article or short");
								}
							} catch (DataAccessException ex) {
								art.setError(true);
								art.setMessage("SQL Error updating article - " + ex.getMessage());
								ex.printStackTrace();
							}
						}
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
						// This is a mess.
						if (art.getArticleSummary().getUser() == null || 
								art.getArticleSummary().getUser().getId() <= 0) {
							// Can't add a new article with no attached user.
							art.setError(true);
							art.setMessage("Cannot add article with no user provided");
							continue mainLoop;
						}
						if (art.getArticleSummary().getTitle() != null && 
								!art.getArticleSummary().getTitle().isEmpty()) {
							if (!art.isShortArticle() && 
									art.getArticleSummary().getArticleURL() != null &&
									!art.getArticleSummary().getArticleURL().isEmpty()) {
								// We're adding a full article.
								// If we got an URL we have to check that it doesn't already exist:
								if (blogDataAccess.getArticleByUrl(art.getArticleSummary().getArticleURL()) == null) {
									if (art.getArticleSummary().getSummary() == null) art.getArticleSummary().setSummary("");
									if (art.getContent() == null) art.setContent("");
									if (!blogDataAccess.insertArticle(art)) {
										art.setError(true);
										art.setMessage("Possible SQL error inserting new article");
									} else {
										art.setMessage("Created new article");
									}
								} else {
									// URL already exists.
									art.setError(true);
									art.setMessage("Provided URL already exists");
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
		return imported;
	}
	
	
}
