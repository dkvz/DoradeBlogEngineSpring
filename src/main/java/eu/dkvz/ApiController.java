package eu.dkvz;

import java.io.IOException;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.*;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.springframework.web.bind.annotation.RequestParam;

import eu.dkvz.BlogAuthoring.model.*;
import eu.dkvz.api.*;
import eu.dkvz.utils.*;

@Controller
public class ApiController {
	
	// IP addresses allowed to ask for costly endpoints:
	public static final String[] ALLOWED_IP_ADDRESSES 
		= new String[]{"::1", "127.0.0.1"};
	
	public static final int MAX_COMMENT_LENGTH = 2000;
	public static final int MAX_AUTHOR_LENGTH = 70;
	// Max length of article content in RSS descriptions:
	public static final int MAX_RSS_LENGTH = 2500;
	
	// This is used for the article rendering:
	public static final String SITE_TITLE = "Blog des gens compliqués";
	public static final String SITE_ROOT = "https://dkvz.eu";
	public static final String RSS_FULL_URL = "https://dkvz.eu/rss.xml";
	public static final String SITE_ARTICLES_ROOT = "articles";
	public static final String SITE_SHORTS_ROOT = "breves";
	public static final String SITE_DESCRIPTION = "Blog bizarre d'un humble consultant en progress bars.";

	public boolean lockImport = false;

	private RateLimiter rateLimiter = new RateLimiter();
	
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
		// Change relative URLs to absolute URLs in the article content:
		art.setContent(TextUtils.processRelativeUrls(art.getContent(), ApiController.SITE_ROOT));
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
	public String getSitemap(@RequestParam(value="articlesRoot", defaultValue="dkvz.eu/articles") String articlesRoot) {
		List<ArticleSummary> articles = blogDataAccess.getArticleSummariesDescFromTo(0, Integer.MAX_VALUE, "", "desc");
		List<Article> shorts = blogDataAccess.getShortsDescFromTo(0, Integer.MAX_VALUE, "", "desc");
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
		// This code is so ugly it hurts
		if (shorts != null && shorts.size() > 0) {
			for (Article aShort : shorts) {
				sitemap = sitemap.concat("\t<url>\n");
				sitemap = sitemap.concat("\t\t<loc>" + baseRoot + aShort.getArticleSummary().getId() + "</loc>\n");
				sitemap = sitemap.concat("\t</url>\n");
			}
		}
		sitemap = sitemap.concat("</urlset>");
		return sitemap;
	}
	
	@RequestMapping(value="/rss", produces="application/xml")
	@ResponseBody
	public String getRSS(HttpServletRequest request) throws Exception {
		// This endpoint can only be called from a set of
		// allowed IP addresses.
		if (Arrays.stream(ApiController.ALLOWED_IP_ADDRESSES).anyMatch(
				s -> s.equals(IpUtils.getRealIp(request))
				)) {
					return this.createRSSFeed(
						blogDataAccess.getAllPublishedArticles(0, "DESC")
					);
		}
		throw new ForbiddenException();
	}

	@RequestMapping("/rebuild-indexes")
	@ResponseBody
	public Map<String, Object> rebuildIndexes(HttpServletRequest request) throws Exception {
		if (Arrays.stream(ApiController.ALLOWED_IP_ADDRESSES).anyMatch(
			s -> s.equals(IpUtils.getRealIp(request))
			)) {
				Map<String, Object> ret = new HashMap<>();
				if (this.lockImport) {
					ret.put("status", "error");
					ret.put("message", "Rebuild already in progress");
				} else {
					try {
						this.lockImport = true;
						ret.put("count", this.blogDataAccess.rebuildFulltext());
						this.blogDataAccess.cleanUpDatabase();
					} finally {
						// Apparently the DB lock is not released immediately so we
						// wait for a bit:
						Thread.sleep(500);
						this.lockImport = false;
					}
				}
				return ret;
		} else {
			throw new ForbiddenException();
		}
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
	public List<Map<String, Object>> importArticles() {
		// This is a wonky locking mechanism but very few
		// people are supposed to know when to call this endpoint.
		List<Map<String, Object>> ret = new ArrayList<Map<String, Object>>();
		if (this.lockImport) {
			Map<String, Object> stat = new HashMap<String, Object>();
			stat.put("status", "error");
			stat.put("message", "Import already in progress");
			ret.add(stat);
		} else {
			this.lockImport = true;
			List<ImportedArticle> articles = this.articleImportService.importJsonFiles();
			for (ImportedArticle art : articles) {
				ret.add(art.toImportStatusMap());
			}
			this.lockImport = false;
		}
		return ret;
	}

	@CrossOrigin(origins = "*")
	@RequestMapping(value="/articles/search", method=RequestMethod.POST)
	@ResponseBody
	public List<Map<String, Object>> searchArticles(@RequestBody Search search) {
		// Using the rate limiter thingy (which should be a middleware):
		if (this.rateLimiter.isAllowed()) {
			// Check if the request body was parsed correctly:
			if (search != null && search.getInclude() != null) {
				List<Map<String, Object>> ret = new ArrayList<Map<String, Object>>();
				Map<String, Object> res = new HashMap<>();
				// Cleanup the search terms:
				search.cleanUpIncludes();
				// Check if we still got search terms:
				if (!search.getInclude().isEmpty()) {
					res.put("terms", search.getInclude().toString());
					ret.add(res);
				}
				return ret;
			} else {
				throw new BadRequestException();
			}
		} else {
			throw new ForbiddenException("Endpoint disabled due to high load");
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
	
	// XML document building in Java is the worst thing on earth.
	private String createRSSFeed(List<Article> articles) throws Exception {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();
        Element rootElement = doc.createElement("rss");
        rootElement.setAttribute("version", "2.0");
        rootElement.setAttribute("xmlns:media", "http://search.yahoo.com/mrss/");
        rootElement.setAttribute("xmlns:atom", "http://www.w3.org/2005/Atom");
				Element channel = doc.createElement("channel");
        XMLUtils.addTextElement(doc, channel, "title", ApiController.SITE_TITLE);
        XMLUtils.addTextElement(doc, channel, "link", ApiController.SITE_ROOT);
        XMLUtils.addTextElement(doc, channel, "description", ApiController.SITE_DESCRIPTION);
        XMLUtils.addTextElement(doc, channel, "generator", ApiController.SITE_ROOT);
        XMLUtils.addTextElement(doc, channel, "language", "fr-FR");
        DateFormat df = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss Z");
        XMLUtils.addTextElement(doc, channel, "lastBuildDate", df.format(new Date()));
        Element atomLink = doc.createElement("atom:link");
        atomLink.setAttribute("href", ApiController.RSS_FULL_URL);
        atomLink.setAttribute("rel", "self");
        atomLink.setAttribute("type", "application/rss+xml");
        channel.appendChild(atomLink);
        rootElement.appendChild(channel);
        for (Article art : articles) {
        	Element item = doc.createElement("item");
        	XMLUtils.addTextElement(doc, item, "title", art.getArticleSummary().getTitle());
        	String artUrl = ApiController.SITE_ROOT + 
    				"/" + 
    				ApiController.SITE_ARTICLES_ROOT +
    				"/" +
    				((art.getArticleSummary().getArticleURL() != null && art.getArticleSummary().getArticleURL().length() > 0) ?
    						art.getArticleSummary().getArticleURL() : Long.toString(art.getArticleSummary().getId()));
        	XMLUtils.addTextElement(
    			doc,
    			item,
    			"link",
    			artUrl
        	);
        	XMLUtils.addTextElement(doc, item, "pubDate", df.format(art.getArticleSummary().getDate()));
        	XMLUtils.addTextElement(doc, item, "guid", artUrl);
        	if (art.getArticleSummary().getThumbImage() != null && !art.getArticleSummary().getThumbImage().isEmpty()) {
        		Element thumb = doc.createElement("media:thumbnail");
        		thumb.setAttribute("url", 
        				((art.getArticleSummary().getThumbImage().indexOf('/') == 0) ? 
        						ApiController.SITE_ROOT + art.getArticleSummary().getThumbImage() : 
        							ApiController.SITE_ROOT + "/" + art.getArticleSummary().getThumbImage()));
        		item.appendChild(thumb);
        	}
        	// We need to:
        	// - Process relative links
        	// - Check size
        	//   - Cut down to max size and add a message to read the rest on the site.
        	if (art.getContent().length() > ApiController.MAX_RSS_LENGTH) {
        		XMLUtils.addTextElement(doc, item, "description", 
        				TextUtils.processRelativeUrls(
    						art.getContent().substring(0, ApiController.MAX_RSS_LENGTH) + 
    						"...<p><a href=\"" + 
							artUrl  + 
							"\" target=\"_blank\">Suite disponible sur le site</a></b></p>", 
							ApiController.SITE_ROOT)
        				);
        	} else {
        		XMLUtils.addTextElement(doc, item, "description", 
        				TextUtils.processRelativeUrls(art.getContent(), ApiController.SITE_ROOT));
        	}
        	channel.appendChild(item);
        }
        doc.appendChild(rootElement);
        
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        StringWriter writer = new StringWriter();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
		return writer.getBuffer().toString();
	}
	
}
