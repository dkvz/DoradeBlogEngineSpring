package eu.dkvz;

import java.util.concurrent.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import eu.dkvz.BlogAuthoring.model.*;
import eu.dkvz.utils.IpUtils;

@Service
public class ApiStatsService {
	
	// Do async stuff to save states after a response
	// has been sent.
	
	@Autowired
    private BlogDataAccessSpring blogDataAccess;
	
	@Autowired
	private GeoIPService geoipService;
	
	@Autowired
	private PseudonymiserService pseudonymiserService;
	
	@Async
	public CompletableFuture<Boolean> insertStats(String clientUa, String clientIp, long articleId) {
		ArticleStat stat = new ArticleStat();
		stat.setArticleId(articleId);
		stat.setClientIp(IpUtils.extractFirstBytes(clientIp));
		stat.setClientUa(clientUa);
		stat.setGeoip(geoipService.getLocalization(clientIp));
		try {
			stat.setPseudoIp(pseudonymiserService.hashAndFind(clientIp));
			stat.setPseudoUa(pseudonymiserService.hashAndFind(clientUa));
		} catch (Exception ex) {
			// Could not pseudonymise, just don't insert anything there.
			ex.printStackTrace();
			stat.setPseudoIp("");
			stat.setPseudoUa(stat.getPseudoIp());
		}
		blogDataAccess.insertArticleStat(stat);
		return CompletableFuture.completedFuture(true);
	}
	
}