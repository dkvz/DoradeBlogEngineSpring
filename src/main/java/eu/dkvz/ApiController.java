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
		Article art = blogDataAccess.getArticleById(130);
		if (art == null) {
			throw new NotFoundException();
		} else {
			return art.toMap();
		}
	}
	
	@RequestMapping("/articleList")
	@ResponseBody
	public List<ArticleSummary> articleList() {
		List<ArticleSummary> sums = blogDataAccess.getArticleSummariesDescFromTo(0, 5, "");
		if (sums != null && sums.size() > 0) {
			return sums;
		} else {
			return new ArrayList<ArticleSummary>();
		}
	}
	
}
