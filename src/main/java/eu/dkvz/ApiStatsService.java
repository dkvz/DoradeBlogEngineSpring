package eu.dkvz;

import java.util.concurrent.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import eu.dkvz.BlogAuthoring.model.*;
import eu.dkvz.utils.IpUtils;

@Service
public class ApiStatsService {
	
	// Do async stuff to save stats after a response
	// has been sent.
	
	@Autowired
	private StatsDataAccessSpring blogDataAccess;
	
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
		stat.setGeoInfo(geoipService.getLocalization(clientIp));
		try {
			stat.setPseudoIp(ApiStatsService.capitalizeFirstLetter(pseudonymiserService.hashAndFind(clientIp)));
			stat.setPseudoUa(ApiStatsService.capitalizeFirstLetter(pseudonymiserService.hashAndFind(clientUa)));
		} catch (Exception ex) {
			// Could not pseudonymise, just don't insert anything there.
			ex.printStackTrace();
			stat.setPseudoIp("");
			stat.setPseudoUa(stat.getPseudoIp());
		}
		try {
			blogDataAccess.insertArticleStat(stat);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return CompletableFuture.completedFuture(true);
	}
	
	public static String capitalizeFirstLetter(String input) {
		return input.substring(0, 1).toUpperCase() + input.substring(1);
	}
	
}
