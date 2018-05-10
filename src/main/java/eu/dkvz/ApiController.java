package eu.dkvz;

import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import eu.dkvz.BlogAuthoring.model.BlogDataAccessSpring;

@Controller
public class ApiController {
	
	@Autowired
    public BlogDataAccessSpring blogDataAccess;
	
	@RequestMapping("/")
    @ResponseBody
    public String index() {
    	return "Nothing here";
    }
	
}
