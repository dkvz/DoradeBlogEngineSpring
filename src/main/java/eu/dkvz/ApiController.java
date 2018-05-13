package eu.dkvz;

import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

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
	
	@RequestMapping("/test")
	@ResponseBody
	public User testing() {
		return blogDataAccess.getUser(1);
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
	
	@RequestMapping("/articleList")
	@ResponseBody
	public List<ArticleSummary> articleList() {
		List<ArticleSummary> sums = blogDataAccess.getArticleSummariesDescFromTo(0, 5, false, "");
		if (sums != null && sums.size() > 0) {
			return sums;
		} else {
			return new ArrayList<ArticleSummary>();
		}
	}
	
	@RequestMapping("/mapList")
	@ResponseBody
	public List<Map<String, Object>> mapList() {
		List<ArticleSummary> sums = blogDataAccess.getArticleSummariesDescFromTo(0, 5, false, "");
		List<Map<String, Object>> ret = new ArrayList<Map<String, Object>>();
		if (sums != null && sums.size() > 0) {
			sums.forEach(i -> ret.add(i.toMap()));
		}
		return ret;
	}
	
}
