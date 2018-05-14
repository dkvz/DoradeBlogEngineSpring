package eu.dkvz;

import java.io.IOException;
import java.util.*;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestParam;

import eu.dkvz.BlogAuthoring.model.*;
import eu.dkvz.api.NotFoundException;

@Controller
public class ApiController {
	
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
		
		
		return null;
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
