package eu.dkvz.BlogAuthoring.model;

public class ImportedArticle extends Article {

	private String filename;
	private boolean error = false;
	private String message;
	private boolean shortArticle = false;
	
	public ImportedArticle() {
		super();
	}
	
	public ImportedArticle(ArticleSummary sum) {
		super(sum);
	}
	
	public String getFilename() {
		return filename;
	}
	public void setFilename(String filename) {
		this.filename = filename;
	}
	public boolean isError() {
		return error;
	}
	public void setError(boolean error) {
		this.error = error;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}

	public boolean isShortArticle() {
		return shortArticle;
	}

	public void setShortArticle(boolean shortArticle) {
		this.shortArticle = shortArticle;
	}
	
}
