package eu.dkvz.BlogAuthoring.model;

import java.sql.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.dao.DataAccessException;

@Repository
public class StatsDataAccess {

  @Autowired
  @Qualifier("jdbcStats")
  private JdbcTemplate jdbcTpl;

  public void insertArticleStat(ArticleStat stat) throws DataAccessException {
		String sql = "INSERT INTO article_stats (article_id, pseudo_ua, pseudo_ip, country, region, city, client_ua, client_ip, date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
		if (stat.getDate() == null) {
			stat.setDate(new java.util.Date());
		}
		jdbcTpl.update(sql,
				stat.getArticleId(),
				stat.getPseudoUa(),
				stat.getPseudoIp(),
				stat.getGeoInfo().getCountry(),
				stat.getGeoInfo().getRegion(),
				stat.getGeoInfo().getCity(),
				stat.getClientUa(),
				stat.getClientIp(),
				stat.getDate().getTime() / 1000);
	}

}