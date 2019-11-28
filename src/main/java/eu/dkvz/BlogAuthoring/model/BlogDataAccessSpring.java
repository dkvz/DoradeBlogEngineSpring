package eu.dkvz.BlogAuthoring.model;

import java.sql.Statement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class BlogDataAccessSpring extends BlogDataAccess {

	@Autowired
	private JdbcTemplate jdbcTpl;

	public User getUser(long id) throws DataAccessException {
		List<User> res = jdbcTpl.query("SELECT name, id FROM users WHERE id = ?",
				new BeanPropertyRowMapper<User>(User.class), id);
		if (res.size() > 0)
			return res.get(0);
		else
			return null;
	}

	/**
	 * I have no idea why I kept this ugly code. I mean I know why, I was watching a
	 * show and copy pasting. Also I did it twice (see next method).
	 */
	public List<Article> getShortsDescFromTo(long start, int count, String tags, String order) {
		if (start < 0) {
			start = 0;
		}
		// I could do this in a single statement but going to do it in two.
		// I'm using limit and offset, which are supported by PostgreSQL and MySQL
		// (normally) but
		// not most other databases.
		String sql = "SELECT articles.id, articles.title, articles.content, "
				+ "articles.article_url, articles.thumb_image, articles.date, "
				+ "articles.user_id, articles.summary, articles.published FROM articles ";
		String[] tagsA = null;
		List<Object> args = new ArrayList<>();
		if (tags != null && !tags.isEmpty()) {
			sql = sql.concat(", article_tags, tags WHERE");
			boolean firstAnd = true;
			tagsA = tags.split(",");
			for (int a = 0; a < tagsA.length; a++) {
				if (firstAnd) {
					sql = sql.concat(" tags.name = ?");
					firstAnd = false;
				} else {
					sql = sql.concat(" AND tags.name = ?");
				}
			}
			// Adding the join code:
			if (!firstAnd) {
				sql = sql.concat(" AND");
			}
			args.addAll(Arrays.asList(tagsA));
			sql = sql.concat(" (tags.id = article_tags.tag_id AND " + "article_tags.article_id = articles.id) "
					+ "AND articles.short = ? AND articles.published = 1 ");
		} else {
			sql = sql.concat(" WHERE articles.short = ? AND articles.published = 1 ");
		}
		args.add(1);
		if (order.toLowerCase().contains("asc")) {
			sql = sql.concat(" ORDER BY articles.id ASC");
		} else {
			sql = sql.concat(" ORDER BY articles.id DESC");
		}
		if (count >= 1) {
			sql = sql.concat(" LIMIT ? OFFSET ?");
		}
		if (count >= 1) {
			args.add(count); // LIMIT clause value
			args.add(start); // OFFSET is start
		}
		List<Article> ret = null;
		List<Map<String, Object>> res = jdbcTpl.queryForList(sql, args.toArray());
		if (res.size() > 0) {
			ret = new ArrayList<>();
			for (Map<String, Object> row : res) {
				ret.add(this.processArticleRow(row, (int) row.get("id")));
			}
		}
		return ret;
	}

	/**
	 * I have no idea why I kept this ugly code. I mean I know why, I was watching a
	 * show and copy pasting.
	 * 
	 * The query should be a StringBuffer, this is awful.
	 */
	public List<ArticleSummary> getArticleSummariesDescFromTo(long start, int count, String tags, String order,
			boolean isShort) throws DataAccessException {
		if (start < 0) {
			start = 0;
		}
		// I could do this in a single statement but going to do it in two.
		// I'm using limit and offset, which are supported by PostgreSQL and MySQL
		// (normally) but
		// not most other databases.
		String sql = "SELECT articles.id, articles.title, " + "articles.article_url, articles.thumb_image, articles.date, "
				+ "articles.user_id, articles.summary, articles.published FROM articles ";
		String[] tagsA = null;
		List<Object> args = new ArrayList<>();
		if (tags != null && !tags.isEmpty()) {
			sql = sql.concat(", article_tags, tags WHERE");
			boolean firstAnd = true;
			tagsA = tags.split(",");
			for (int a = 0; a < tagsA.length; a++) {
				if (firstAnd) {
					sql = sql.concat(" tags.name = ?");
					firstAnd = false;
				} else {
					sql = sql.concat(" AND tags.name = ?");
				}
			}
			// Adding the join code:
			if (!firstAnd) {
				sql = sql.concat(" AND");
			}
			args.addAll(Arrays.asList(tagsA));
			sql = sql.concat(" (tags.id = article_tags.tag_id AND " + "article_tags.article_id = articles.id) "
					+ "AND articles.short = ? AND articles.published = 1 ");
		} else {
			sql = sql.concat(" WHERE articles.short = ? AND articles.published = 1 ");
		}
		args.add(isShort ? 1 : 0);
		if (order.toLowerCase().contains("asc")) {
			sql = sql.concat("ORDER BY articles.id ASC");
		} else {
			sql = sql.concat("ORDER BY articles.id DESC");
		}
		if (count >= 1) {
			sql = sql.concat(" LIMIT ? OFFSET ?");
		}
		if (count >= 1) {
			args.add(count); // LIMIT clause value
			args.add(start); // OFFSET is start
		}
		List<ArticleSummary> ret = null;
		List<Map<String, Object>> res = jdbcTpl.queryForList(sql, args.toArray());
		if (res.size() > 0) {
			ret = new ArrayList<>();
			for (Map<String, Object> row : res) {
				ret.add(this.processArticleSummaryRow(row, (int) row.get("id")));
			}
		}
		return ret;
	}

	/**
	 * Gets all the published articles with their content, including shorts, ordered
	 * by id
	 * 
	 * @param max   What will be used with the limit parameter, 0 means no limit
	 * @param order Expected to be asc or desc, case insensitive. Will not be
	 *              escaped in the SQL statement so it should be hardcoded
	 * @return a list of Article items
	 */
	public List<Article> getAllPublishedArticles(int max, String order) {
		String sql = "SELECT articles.id, articles.title, " + "articles.article_url, articles.thumb_image, articles.date, "
				+ "articles.user_id, articles.summary, articles.published, "
				+ "articles.content, articles.short FROM articles WHERE " + "articles.published = 1 ";
		sql += BlogDataAccess.generateOrderBy("articles.id", order);
		if (max > 0)
			sql += " LIMIT " + max;
		// Processing the rows will fetch the tags.
		List<Map<String, Object>> res = jdbcTpl.queryForList(sql);
		List<Article> ret = new ArrayList<Article>();
		if (res.size() > 0) {
			for (Map<String, Object> row : res) {
				ret.add(this.processArticleRow(row, (int) row.get("id")));
			}
		}
		return ret;
	}

	public List<ArticleTag> getAllTags() throws DataAccessException {
		List<ArticleTag> tags = jdbcTpl.query("SELECT * FROM tags ORDER BY name ASC",
				new BeanPropertyRowMapper<ArticleTag>(ArticleTag.class));
		return tags;
	}

	public long getCommentCount(long articleID) throws DataAccessException {
		// return jdbcTpl.queryForObject("SELECT count(*) FROM comments WHERE article_id
		// = ?", Long.class,
		// new Object[] {articleID});
		return jdbcTpl.queryForObject("SELECT count(*) FROM comments WHERE article_id = ?", Long.class, articleID);
	}

	/**
	 * The tags thing in there actually doesn't work. It only works for one tag.
	 * Also this looks awful.
	 */
	public long getArticleCount(boolean published, boolean isShort, String tags) throws DataAccessException {
		long ret = 0l;
		String sql = "SELECT count(*) FROM articles";
		String[] tagsA = null;
		boolean where = false;
		if (tags != null && !tags.isEmpty()) {
			tagsA = tags.split(",");
			// TODO: I should use StringBuilder here instead
			// of recreating a String at every concat.
			sql = sql.concat(", tags, article_tags");
			if (published) {
				sql = sql.concat(" WHERE articles.published = 1");
				where = true;
			}
			for (int a = 0; a < tagsA.length; a++) {
				if (!where && !published) {
					sql = sql.concat(" WHERE");
					where = true;
				} else {
					sql = sql.concat(" AND");
				}
				sql = sql.concat(" tags.name = ?");
			}
			// Adding the join code:
			sql = sql.concat(" AND (tags.id = article_tags.tag_id AND " + "article_tags.article_id = articles.id)");
		} else {
			if (published) {
				sql = sql.concat(" WHERE articles.published = 1");
				where = true;
			}
		}
		if (!where) {
			sql = sql.concat(" WHERE");
		} else {
			sql = sql.concat(" AND");
		}
		sql = sql.concat(" articles.short = ").concat(isShort ? "1" : "0");
		// Object[] args = null;
		// if (tagsA != null) {
		// args = new Object[tagsA.length];
		// for (int i = 0; i < tagsA.length; i++) {
		// // I originally meant the list to use long as index
		// // but arrays use int, so yeah... I'm using int.
		// // F*** consistency.
		// //stmt.setString(i, tagsA[i - 1]);
		// args[i] = tagsA[i];
		// }
		// }
		ret = jdbcTpl.queryForObject(sql, Long.class, (Object[]) tagsA);
		return ret;
	}

	public Article getArticleById(long id) throws DataAccessException {
		// I think you're supposed to queryForList and check the list size.
		// I used another method.
		try {
			Map<String, Object> res = jdbcTpl.queryForMap("SELECT * FROM articles WHERE id = ?", id);
			return this.processArticleRow(res, id);
		} catch (IncorrectResultSizeDataAccessException ex) {
			return null;
		}
	}

	public Article getArticleByUrl(String articleUrl) throws DataAccessException {
		long artId = this.getArticleIdFromUrl(articleUrl);
		if (artId > 0) {
			return this.getArticleById(artId);
		}
		return null;
	}

	private ArticleSummary processArticleSummaryRow(Map<String, Object> res, long id) {
		ArticleSummary sum = new ArticleSummary();
		// Just a reminder: casting null into anything won't raise an exception.
		sum.setArticleURL((String) res.get("article_url"));
		// I should include the author in the query:
		String author = "Anonymous";
		User usr = this.getUser((int) res.get("user_id"));
		if (usr != null) {
			author = usr.getName();
		}
		sum.setAuthor(author);
		sum.setId(id);
		sum.setCommentsCount(this.getCommentCount(id));
		// TODO I need to log the date because it's wrong somehow.
		int dateVal = (int) res.get("date");
		java.util.Date date = new java.util.Date((long) dateVal * 1000);
		sum.setDate(date);
		if ((int) res.get("published") > 0) {
			sum.setPublished(true);
		} else {
			sum.setPublished(false);
		}
		if (res.get("short") != null) {
			// Also set if this is a short:
			sum.setShortArticle((int) res.get("short") > 0 ? true : false);
		}
		sum.setSummary((String) res.get("summary"));
		sum.setTags(this.getTagsForArticle(id));
		sum.setThumbImage((String) res.get("thumb_image"));
		sum.setTitle((String) res.get("title"));
		return sum;
	}

	private Article processArticleRow(Map<String, Object> res, long id) {
		Article art = new Article();
		art.setArticleSummary(this.processArticleSummaryRow(res, id));
		art.setContent((String) res.get("content"));
		return art;
	}

	private boolean insertArticle(Article article, boolean shortArticle) throws DataAccessException {
		String sql = "INSERT INTO articles "
				+ "(title, article_url, thumb_image, date, user_id, summary, content, published, short) "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
		if (article.getArticleSummary().getDate() == null) {
			article.getArticleSummary().setDate(new java.util.Date());
		}
		// We assume the uniqueness or article_url has been checked before.
		// It will throw a SQL error anyway if the url is not unique.
		KeyHolder key = new GeneratedKeyHolder();
		int res = this.jdbcTpl.update(new PreparedStatementCreator() {

			@Override
			public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
				final PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
				ps.setString(1, article.getArticleSummary().getTitle());
				ps.setString(2, article.getArticleSummary().getArticleURL());
				ps.setString(3, article.getArticleSummary().getThumbImage());
				ps.setLong(4, article.getArticleSummary().getDate().getTime() / 1000);
				ps.setLong(5, article.getArticleSummary().getUser().getId());
				ps.setString(6, article.getArticleSummary().getSummary());
				ps.setString(7, article.getContent());
				ps.setInt(8, article.getArticleSummary().isPublished() ? 1 : 0);
				ps.setInt(9, shortArticle ? 1 : 0);
				return ps;
			}

		}, key);
		if (res <= 0)
			return false;
		// Set the inserted key:
		article.getArticleSummary().setId(key.getKey().longValue());
		// Add the tags.
		// We should use a transaction for this but uh... Yeah.
		// Now we can call another method to insert the tags.
		// Which won't check if the tags exist.
		this.insertTagsForArticle(article.getArticleSummary().getTags(), article.getArticleSummary().getId());
		// Add the fulltext entry:
		this.insertArticleFulltext(article);
		// At this point always return true. Because we're not
		// using transactions, remember?
		return true;
	}

	public void insertTagsForArticle(List<ArticleTag> tags, long articleId) throws DataAccessException {
		for (ArticleTag tag : tags) {
			this.insertTagForArticle(tag, articleId);
		}
	}

	public void insertTagForArticle(ArticleTag tag, long articleId) throws DataAccessException {
		String sql = "INSERT INTO article_tags (article_id, tag_id) VALUES(?, ?)";
		this.jdbcTpl.update(sql, articleId, tag.getId());
	}

	public void deleteTagForArticle(ArticleTag tag, long articleId) throws DataAccessException {
		String sql = "DELETE FROM article_tags WHERE article_id = ? AND tag_id = ?";
		this.jdbcTpl.update(sql, articleId, tag.getId());
	}

	public boolean insertArticle(Article article) throws DataAccessException {
		return this.insertArticle(article, false);
	}

	public boolean insertShort(Article article) throws DataAccessException {
		// We have to nullify articleUrl:
		article.getArticleSummary().setArticleURL(null);
		return this.insertArticle(article, true);
	}

	// Oh god this is SO UGLY
	public boolean updateArticle(Article article) throws DataAccessException {
		// This method does not check if the article exists.
		String sql = "UPDATE articles SET ";
		String eqm = " = ?";
		List<String> toSet = new ArrayList<>();
		List<Object> args = new ArrayList<>();
		if (article.getArticleSummary().getTitle() != null) {
			toSet.add("title".concat(eqm));
			args.add(article.getArticleSummary().getTitle());
		}
		if (article.getArticleSummary().getDate() != null) {
			toSet.add("date".concat(eqm));
			args.add(article.getArticleSummary().getDate().getTime() / 1000);
		}
		if (article.getArticleSummary().getArticleURL() != null) {
			toSet.add("article_url".concat(eqm));
			args.add(article.getArticleSummary().getArticleURL());
		}
		if (article.getArticleSummary().getUser() != null) {
			toSet.add("user_id".concat(eqm));
			args.add(article.getArticleSummary().getUser().getId());
		}
		if (article.getArticleSummary().getSummary() != null) {
			toSet.add("summary".concat(eqm));
			args.add(article.getArticleSummary().getSummary());
		}
		if (article.getArticleSummary().getThumbImage() != null) {
			toSet.add("thumb_image".concat(eqm));
			args.add(article.getArticleSummary().getThumbImage());
		}
		if (article.getContent() != null) {
			toSet.add("content".concat(eqm));
			args.add(article.getContent());
		}
		// Check if we need to modify tags
		if (article.getArticleSummary().getTags() != null) {
			// Check with current tags for this article.
			List<ArticleTag> currentTags = this.getTagsForArticle(article.getArticleSummary().getId());
			for (ArticleTag t : article.getArticleSummary().getTags()) {
				if (!currentTags.contains(t)) {
					// This tag wasn't present before.
					// Add it:
					this.insertTagForArticle(t, article.getArticleSummary().getId());
				}
			}
			// Now what if we removed tags?
			for (ArticleTag t : currentTags) {
				if (!article.getArticleSummary().getTags().contains(t)) {
					// Tag vanished, remove it.
					this.deleteTagForArticle(t, article.getArticleSummary().getId());
				}
			}
		}
		toSet.add("published".concat(eqm));
		args.add(article.getArticleSummary().isPublished());
		// Don't do anything if for some reason toSet is empty.
		// In fact it's never empty because we have a default value
		// for published.
		// TODO lol
		if (!toSet.isEmpty()) {
			// Add the rest of the query.
			// Also add the article ID to args.
			sql += String.join(", ", toSet);
			// Add the where clause:
			sql += " WHERE id = ?";
			args.add(article.getArticleSummary().getId());
			// int updt = this.jdbcTpl.update(sql, args.toArray());
			this.jdbcTpl.update(sql, args.toArray());
		}
		// Update the fulltext entry:
		this.updateArticleFulltext(article);
		// ON L'APPELLE
		// Le booléen qui sert à rien
		return true;
	}

	// These methods return booleans for historical reasons.
	@Transactional
	public boolean deleteArticleById(long id) throws DataAccessException {
		// We need to remove associated tags, the fulltext indexes and
		// comments.
		jdbcTpl.update("DELETE FROM comments WHERE article_id = ?", id);
		jdbcTpl.update("DELETE FROM article_tags WHERE article_id = ?", id);
		this.deleteArticleFulltext(id);
		int res = jdbcTpl.update("DELETE FROM articles WHERE id = ?", id);
		return res > 0;
	}

	public List<ArticleTag> getTagsForArticle(long id) throws DataAccessException {
		List<ArticleTag> tags = jdbcTpl.query(
				"SELECT tags.name, tags.id, tags.main_tag "
						+ "FROM article_tags, tags WHERE article_tags.article_id = ? AND article_tags.tag_id = tags.id",
				new BeanPropertyRowMapper<ArticleTag>(ArticleTag.class), id);
		return tags;
	}

	public boolean changeArticleId(long previousId, long newId) throws DataAccessException {
		return false;
	}

	public List<Comment> getCommentsFromTo(long start, int count, long articleId) throws DataAccessException {
		List<Comment> res = new ArrayList<Comment>();
		if (start < 0)
			start = 0;
		// I could do this in a single statement but going to do it in two.
		// I'm using limit and offset.
		String sql = "SELECT comments.id, comments.article_id, comments.author, "
				+ "comments.comment, comments.date FROM comments, articles WHERE "
				+ "articles.id = ? AND articles.id = comments.article_id ORDER BY comments.id ASC " + "LIMIT ? OFFSET ?";
		List<Map<String, Object>> coms = jdbcTpl.queryForList(sql, articleId, count, start);
		coms.forEach(i -> res.add(this.processCommentRow(i)));
		return res;
	}

	private Comment processCommentRow(Map<String, Object> row) {
		Comment com = new Comment();
		com.setArticleId((int) row.get("article_id"));
		com.setAuthor((String) row.get("author"));
		com.setClientIP((String) row.get("client_ip"));
		com.setComment((String) row.get("comment"));
		int dateVal = (int) row.get("date");
		java.util.Date date = new java.util.Date((long) dateVal * 1000);
		com.setDate(date);
		com.setId((int) row.get("id"));
		return com;
	}

	public List<Comment> getCommentsFromTo(long start, int count, String articleUrl) throws DataAccessException {
		long articleId = this.getArticleIdFromUrl(articleUrl);
		if (articleId > 0)
			return this.getCommentsFromTo(start, count, articleId);
		return null;
	}

	public long getArticleIdFromUrl(String url) throws DataAccessException {
		try {
			return jdbcTpl.queryForObject("SELECT id FROM articles WHERE article_url = ?", Long.class, url);
		} catch (IncorrectResultSizeDataAccessException ex) {
			return -1;
		}
	}

	public void insertComment(Comment comment) throws DataAccessException {
		String sql = "INSERT INTO comments " + "(article_id, author, comment, date, client_ip) VALUES (?, ?, ?, ?, ?)";
		// stmt.setLong(1, comment.getArticleId());
		// stmt.setString(2, comment.getAuthor());
		// stmt.setString(3, comment.getComment());
		// if (comment.getDate() == null) {
		// java.util.Date now = new java.util.Date();
		// comment.setDate(now);
		// }
		// long stamp = comment.getDate().getTime() / 1000;
		// stmt.setLong(4, stamp);
		// stmt.setString(5, comment.getClientIP());
		if (comment.getDate() == null) {
			comment.setDate(new java.util.Date());
		}
		jdbcTpl.update(sql, comment.getArticleId(), comment.getAuthor(), comment.getComment(),
				comment.getDate().getTime() / 1000, comment.getClientIP());
	}

	public Comment getLastComment() throws DataAccessException {
		List<Map<String, Object>> res = jdbcTpl.queryForList("SELECT * FROM comments ORDER BY id DESC LIMIT 1");
		if (res.size() > 0) {
			return this.processCommentRow(res.get(0));
		}
		return null;
	}

	/**
	 * Stats were moved to StatsDataAccess.java, so this method should not be used.
	 */
	public void insertArticleStat(ArticleStat stat) throws DataAccessException {
		String sql = "INSERT INTO article_stats (article_id, pseudo_ua, pseudo_ip, country, region, city, client_ua, client_ip, date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
		if (stat.getDate() == null) {
			stat.setDate(new java.util.Date());
		}
		jdbcTpl.update(sql, stat.getArticleId(), stat.getPseudoUa(), stat.getPseudoIp(), stat.getGeoInfo().getCountry(),
				stat.getGeoInfo().getRegion(), stat.getGeoInfo().getCity(), stat.getClientUa(), stat.getClientIp(),
				stat.getDate().getTime() / 1000);
	}

	public void insertArticleFulltext(Article art) throws DataAccessException {
		// Insert article data into the fulltext database.
		// Does NOT error if a row with the same ID already exists as
		// the FT table can't have keys of any kind.
		jdbcTpl.update("INSERT INTO articles_ft (id, title, content) VALUES (?, ?, ?)", art.getArticleSummary().getId(),
				BlogDataAccess.purifyText(art.getArticleSummary().getTitle()), BlogDataAccess.purifyText(art.getContent()));
	}

	public void updateArticleFulltext(Article art) throws DataAccessException {
		jdbcTpl.update("UPDATE articles_ft SET title = ?, content = ? WHERE id = ?",
				BlogDataAccess.purifyText(art.getArticleSummary().getTitle()), BlogDataAccess.purifyText(art.getContent()),
				art.getArticleSummary().getId());
	}

	public void deleteArticleFulltext(long id) {
		jdbcTpl.update("DELETE FROM articles_ft WHERE id = ?", id);
	}

	public void cleanUpDatabase() throws DataAccessException {
		jdbcTpl.execute("VACUUM");
	}

	@Transactional
	public int rebuildFulltext() throws DataAccessException {
		// Remove all the content from the table and run VACUUM:
		jdbcTpl.execute("DELETE FROM articles_ft");
		String sql = "SELECT id, title, content FROM articles WHERE published = 1 ORDER BY id ASC";
		List<Map<String, Object>> res = jdbcTpl.queryForList(sql);
		if (res != null && res.size() > 0) {
			for (Map<String, Object> row : res) {
				Article art = new Article();
				ArticleSummary sum = new ArticleSummary();
				sum.setId((int) row.get("id"));
				sum.setTitle((String) row.get("title"));
				art.setContent((String) row.get("content"));
				art.setArticleSummary(sum);
				this.insertArticleFulltext(art);
			}
		}
		return res.size();
	}

	public List<ArticleSummary> fulltextSearchPublishedArticles(Search search) throws DataAccessException {
		// I added an arbitrary limit to the query.
		String sql = "SELECT articles_ft.id, articles_ft.title, articles.article_url, snippet(articles_ft, 2, '<b>', '</b>', ' [...] ', 50) AS snippet FROM articles_ft, articles WHERE articles_ft MATCH ? and articles.id = articles_ft.id and articles.published = 1 ORDER BY rank LIMIT 15";
		List<ArticleSummary> ret = new ArrayList<>();
		List<Map<String, Object>> arts = jdbcTpl.queryForList(sql, search.toQueryString());
		arts.forEach(a -> {
			ArticleSummary sum = new ArticleSummary();
			sum.setId((int) a.get("id"));
			sum.setTitle((String) a.get("title"));
			if (a.get("article_url") != null) {
				sum.setArticleURL((String) a.get("article_url"));
			}
			sum.setSummary((String) a.get("snippet"));
			ret.add(sum);
		});
		return ret;
	}

}
