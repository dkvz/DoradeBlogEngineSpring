package eu.dkvz.BlogAuthoring.model;

import java.util.*;

public class ArticleStat {
	
	private long id = -1;
	private long articleId;
	private String pseudoUa;
	private String pseudoIp;
	private String clientUa;
	private String clientIp;
	private String geoip;
	private Date date;
	
	public long getArticleId() {
		return articleId;
	}
	public void setArticleId(long articleId) {
		this.articleId = articleId;
	}
	public String getPseudoUa() {
		return pseudoUa;
	}
	public void setPseudoUa(String pseudoUa) {
		this.pseudoUa = pseudoUa;
	}
	public String getPseudoIp() {
		return pseudoIp;
	}
	public void setPseudoIp(String pseudoIp) {
		this.pseudoIp = pseudoIp;
	}
	public String getClientUa() {
		return clientUa;
	}
	public void setClientUa(String clientUa) {
		this.clientUa = clientUa;
	}
	public String getClientIp() {
		return clientIp;
	}
	public void setClientIp(String clientIp) {
		this.clientIp = clientIp;
	}
	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public String getGeoip() {
		return geoip;
	}
	public void setGeoip(String geoip) {
		this.geoip = geoip;
	}

	
	
}
