package eu.dkvz;

import java.io.IOException;
import java.util.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestParam;

import eu.dkvz.BlogAuthoring.model.*;
import eu.dkvz.api.*;
import eu.dkvz.utils.IpUtils;

@Controller
public class ApiController {
	
	public static final int MAX_COMMENT_LENGTH = 2000;
	public static final int MAX_AUTHOR_LENGTH = 70;
	// This is used for the article rendering:
	public static final String SITE_TITLE = "Blog des gens compliqués";
	public static final String SITE_ROOT = "https://dkvz.eu";
	public static final String SITE_ARTICLES_ROOT = "articles";
	public static final String SITE_SHORTS_ROOT = "breves";
	
	public boolean lockImport = false;
	
	@Autowired
    public BlogDataAccessSpring blogDataAccess;
	
	@Autowired
	private ApiStatsService apiStatsService;
	
	@Autowired
	private ArticleImportService articleImportService;
	
	@CrossOrigin(origins = "*")
	@RequestMapping("/")
    @ResponseBody
    public String index() {
    	return "Nothing here";
    }
	
	@CrossOrigin(origins = "*")
	@RequestMapping("/article/{articleUrl}")
	@ResponseBody
	public Map<String, Object> getArticle(@PathVariable String articleUrl, HttpServletRequest request) {
		Article art = null; 
		// Check if we got an article ID:
		try {
			long articleId = Long.parseLong(articleUrl);
			if (articleId > 0l) {
				art = blogDataAccess.getArticleById(articleId);
			} else  {
				art = blogDataAccess.getArticleByUrl(articleUrl);
			}
		} catch (NumberFormatException ex) {
			art = blogDataAccess.getArticleByUrl(articleUrl);
		}
		if (art != null) {
			// This is asynchronous by the magic of Spring magic.
			apiStatsService.insertStats(request.getHeader("User-Agent"), IpUtils.getRealIp(request), art.getArticleSummary().getId());
			return art.toMap();
		}
		else throw new NotFoundException();
	}
	
	@RequestMapping("/render-article/{articleUrl}")
	public String renderArticle(@PathVariable String articleUrl, Map<String, Object> model) {
		Article art = null;
		// Check if we got an article ID:
		// Never refactor this lel
		try {
			long articleId = Long.parseLong(articleUrl);
			if (articleId > 0l) {
				art = blogDataAccess.getArticleById(articleId);
				// We need to change the article URL if using numeric ID.
				model.put("siteArticlesRoot", ApiController.SITE_SHORTS_ROOT);
				art.getArticleSummary().setArticleURL(Long.toString(articleId));
			} else  {
				art = blogDataAccess.getArticleByUrl(articleUrl);
				model.put("siteArticlesRoot", ApiController.SITE_ARTICLES_ROOT);
			}
		} catch (NumberFormatException ex) {
			art = blogDataAccess.getArticleByUrl(articleUrl);
			model.put("siteArticlesRoot", ApiController.SITE_ARTICLES_ROOT);
		}
		if (art == null) throw new NotFoundException();
		model.put("article", art);
		model.put("siteTitle", ApiController.SITE_TITLE);
		model.put("siteRoot", ApiController.SITE_ROOT);
		// We need to check if the thumbImage requires adding the
		// website root to it:
		if (art.getArticleSummary().getThumbImage() != null 
				&& art.getArticleSummary().getThumbImage().length() > 0) {
			if (!art.getArticleSummary().getThumbImage().contains("://")) {
				art.getArticleSummary().setThumbImage(
						ApiController.SITE_ROOT.concat(art.getArticleSummary().getThumbImage())
				);
			}
		}
		return "article";
	}
	
	@CrossOrigin(origins = "*")
	@RequestMapping("/articles-starting-from/{articleId}")
	@ResponseBody
	public List<Map<String, Object>> articlesStartingFrom(@PathVariable long articleId,
			@RequestParam(value="max", defaultValue="30") int max,
			@RequestParam(value="tags", defaultValue="") String tags,
			@RequestParam(value="order", defaultValue="desc") String order) {
    	return this.getArticlesOrShortsStartingFrom(articleId, max, tags, order, false);
	}
	
	@CrossOrigin(origins = "*")
	@RequestMapping("/shorts-starting-from/{articleId}")
	@ResponseBody
	public List<Map<String, Object>> shortsStartingFrom(@PathVariable long articleId,
			@RequestParam(value="max", defaultValue="30") int max,
			@RequestParam(value="tags", defaultValue="") String tags,
			@RequestParam(value="order", defaultValue="desc") String order) {
    	return this.getArticlesOrShortsStartingFrom(articleId, max, tags, order, true);
	}
	
	@CrossOrigin(origins = "*")
	@RequestMapping("/comments-starting-from/{articleUrl}")
	@ResponseBody
	public List<Map<String, Object>> commentsStartingFrom(@PathVariable String articleUrl, 
			@RequestParam(value="start", defaultValue="0") int start,
			@RequestParam(value="max", defaultValue="30") int max) {
		if (max > 50) {
    		max = 50;
    	}
		long articleId;
		try {
    		// Check if we got an article ID as the URL
    		articleId = Long.parseLong(articleUrl);
    	} catch(NumberFormatException ex) {
    		articleId = blogDataAccess.getArticleIdFromUrl(articleUrl);
    	}
		long count = blogDataAccess.getCommentCount(articleId);
    	if (start >= count) {
    		throw new NotFoundException();
    	} else {
    		List<Comment> list = blogDataAccess.getCommentsFromTo(start, max, articleId);
			List<Map<String, Object>> listMap = new ArrayList<Map<String, Object>>();
			for (Comment art : list) {
				listMap.add(art.toReducedMap());
			}
			return listMap;
    	}
	}
	
	@RequestMapping(value="/gimme-sitemap", produces="application/xml")
	@ResponseBody
	public String getSitemap(@RequestParam(value="articlesRoot", defaultValue="") String articlesRoot) {
		List<ArticleSummary> articles = blogDataAccess.getArticleSummariesDescFromTo(0, Integer.MAX_VALUE, "", "desc");
		String sitemap = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
    	sitemap = sitemap.concat("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
    	String baseRoot = "https://" + articlesRoot + "/";
		if (articles != null && articles.size() > 0) {
	    	for (ArticleSummary sum : articles) {
	    		sitemap = sitemap.concat("\t<url>\n");
	    		sitemap = sitemap.concat("\t\t<loc>" + baseRoot.concat(sum.getArticleURL()) + "</loc>\n");
	    		sitemap = sitemap.concat("\t</url>\n");
	    	}
		}
		sitemap = sitemap.concat("</urlset>");
		return sitemap;
	}
	
	@CrossOrigin(origins = "*")
	@RequestMapping("/tags")
	@ResponseBody
	public List<Map<String, Object>> getTags() {
		List<ArticleTag> tags = blogDataAccess.getAllTags();
		if (tags != null && tags.size() > 0) {
			List<Map<String, Object>> listMap = new ArrayList<Map<String, Object>>();
			for (ArticleTag tag : tags) {
				listMap.add(tag.toMap());
			}
			return listMap;
		} else {
			throw new NotFoundException();
		}
	}

	// I love how I designed this thing (sarcasm).
	// Also the name of the articleId is not in CamelCase. Sorry.
	@CrossOrigin(origins = "*")
	@RequestMapping(value="/comments", method=RequestMethod.POST, consumes=MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	@ResponseBody
	public Map<String, Object> saveComment(String comment, String author, String article_id, String articleurl, 
			HttpServletRequest request) {
		// Any absent argument will be set to null.
		if (comment != null && (article_id != null || articleurl != null) && 
				(author != null && author.replace(" ", "").length() > 0)) {
			
			Comment com = new Comment();
			if (article_id != null) {
				try {
					com.setArticleId(Long.parseLong(article_id));
				} catch (NumberFormatException ex) {
					throw new BadRequestException("Malformed article ID");
				}
			} else {
				// Using articleurl:
				long artId = blogDataAccess.getArticleIdFromUrl(articleurl);
				if (artId > 0l) {
					com.setArticleId(artId);
				} else {
					throw new BadRequestException("Invalid article URL");
				}
			}
			if (author.length() > ApiController.MAX_AUTHOR_LENGTH) {
				author = author.substring(0, ApiController.MAX_AUTHOR_LENGTH);
			}
			com.setAuthor(author);
			// Reduce comment to some arbitrary max length.
			if (comment.length() > ApiController.MAX_COMMENT_LENGTH) {
				comment = comment.substring(0, ApiController.MAX_COMMENT_LENGTH);
			}
			com.setComment(comment);
			// This is crazy GDPR compliance:
			com.setClientIP(IpUtils.extractFirstBytes(IpUtils.getRealIp(request)));
			com.setDate(new java.util.Date());
			blogDataAccess.insertComment(com);
			return com.toReducedMap();
		}
		throw new BadRequestException("Missing arguments");
	}
	
	@CrossOrigin(origins = "*")
	@RequestMapping("/last-comment")
	@ResponseBody
	public Map<String, Object> getLastComment() {
		Comment lastCom = blogDataAccess.getLastComment();
		if (lastCom != null) {
			return lastCom.toReducedMap();
		} else {
			throw new NotFoundException();
		}
	}
	
	@CrossOrigin(origins = "*")
	@RequestMapping("/import-articles")
	@ResponseBody
	public Map<String, Object> importArticles() {
		// TODO We need to lock this method, can't run it twice at the same time;
		Map<String, Object> ret = new HashMap<>();
		if (this.lockImport) {
			ret.put("status", "error");
			ret.put("message", "Import already in progress");
		} else {
			this.lockImport = true;
			// DO STUFF
			this.lockImport = false;
		}
		return null;
	}
	
	public List<Map<String, Object>> getArticlesOrShortsStartingFrom(long articleId, int max, String tags, String order, boolean isShort) {
		if (max > 100) {
    		max = 30;
    	}
    	try {
    		long count = blogDataAccess.getArticleCount(true, false, tags);
    		if (articleId >= count) {
        		throw new NotFoundException();
			} else {
				List<Map<String, Object>> ret = new ArrayList<>();
				// I could make an interface of stuff that have "toMap" and use that.
				if (isShort) {
					List<Article> sums = blogDataAccess.getShortsDescFromTo(articleId, max, tags, order);
					sums.forEach(i -> ret.add(i.toMap()));
				} else {
					List<ArticleSummary> sums = blogDataAccess.getArticleSummariesDescFromTo(articleId, max, tags, order);
					sums.forEach(i -> ret.add(i.toMap()));
				}
				return ret;
			}
    	} catch (DataAccessException ex) {
    		System.err.println(ex);
    		throw ex;
    	}
	}
	
	@ExceptionHandler
	void handleDataAccessException(DataAccessException ex, HttpServletResponse response) throws IOException {
	    response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value());
	}
	
}
