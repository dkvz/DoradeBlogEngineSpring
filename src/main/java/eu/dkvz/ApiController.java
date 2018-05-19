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
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.bind.annotation.RequestParam;

import eu.dkvz.BlogAuthoring.model.*;
import eu.dkvz.api.*;

@Controller
public class ApiController {
	
	public static final int MAX_COMMENT_LENGTH = 2000;
	public static final int MAX_AUTHOR_LENGTH = 70;
	
	@Autowired
    public BlogDataAccessSpring blogDataAccess;
	
	@RequestMapping("/")
    @ResponseBody
    public String index() {
    	return "Nothing here";
    }
	
	@RequestMapping("/test/{kakor}")
	@ResponseBody
	public String testing(@PathVariable String kakor, @RequestParam(value="cake", defaultValue="World") String cake) {
		return kakor + " ; " + cake;
	}
	
	@RequestMapping("/gimmemap")
	@ResponseBody
	public Map<String, Object> giveMap() {
		Article art = blogDataAccess.getArticleById(52);
		if (art == null) {
			throw new NotFoundException();
		} else {
			return art.toMap();
		}
	}
	
	@RequestMapping("/article/{articleUrl}")
	@ResponseBody
	public Map<String, Object> getArticle(@PathVariable String articleUrl) {
		Article art = blogDataAccess.getArticleByUrl(articleUrl);
		if (art != null) return art.toMap();
		else throw new NotFoundException();
	}
	
	@RequestMapping("/articles-starting-from/{articleId}")
	@ResponseBody
	public List<Map<String, Object>> articlesStartingFrom(@PathVariable long articleId,
			@RequestParam(value="max", defaultValue="30") int max,
			@RequestParam(value="tags", defaultValue="") String tags,
			@RequestParam(value="order", defaultValue="desc") String order) {
    	return this.getArticlesOrShortsStartingFrom(articleId, max, tags, order, false);
	}
	
	@RequestMapping("/shorts-starting-from/{articleId}")
	@ResponseBody
	public List<Map<String, Object>> shortsStartingFrom(@PathVariable long articleId,
			@RequestParam(value="max", defaultValue="30") int max,
			@RequestParam(value="tags", defaultValue="") String tags,
			@RequestParam(value="order", defaultValue="desc") String order) {
    	return this.getArticlesOrShortsStartingFrom(articleId, max, tags, order, true);
	}
	
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
	@RequestMapping(value="/comments", method=RequestMethod.POST, consumes=MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	@ResponseBody
	public Map<String, Object> saveComment(String comment, String author, String article_id, String articleurl, 
			HttpServletRequest request) {
		// TODO Escape everything HTML
		// TODO Strip comment that is too long
		// TODO Check that the article exists first
		// TODO Parse numeric articleId or use articleurl (yes, written like that)
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
				if (artId > 0) {
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
			com.setClientIP(request.getRemoteAddr());
			com.setDate(new java.util.Date());
			blogDataAccess.insertComment(com);
			return com.toReducedMap();
		}
		throw new BadRequestException("Missing arguments");
	}
	
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
