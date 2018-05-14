package eu.dkvz.BlogAuthoring.model;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;

import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BlogDataAccessSpring {

	@Autowired
    private JdbcTemplate jdbcTpl;
	
	public void connect() throws DataAccessException {
		// TODO Auto-generated method stub

	}

	public void disconnect() throws DataAccessException {
		// TODO Auto-generated method stub

	}

	public boolean isConnected() throws DataAccessException {
		// TODO Auto-generated method stub
		return true;
	}

	public User getUser(long id) throws DataAccessException {
		List<User> res = jdbcTpl.query("SELECT name, id FROM users WHERE id = ?", 
				new BeanPropertyRowMapper<User>(User.class), id);
		if (res.size() > 0) return res.get(0);
		else return null;
	}

	/**
	 * I have no idea why I kept this ugly code.
	 * I mean I know why, I was watching a show and copy pasting.
	 * Also I did it twice (see next method).
	 */
	public List<Article> getShortsDescFromTo(long start, int count, String tags, String order) {
		if (start < 0) {
            start = 0;
        }
        // I could do this in a single statement but going to do it in two.
        // I'm using limit and offset, which are supported by PostgreSQL and MySQL (normally) but
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
            sql = sql.concat(" (tags.id = article_tags.tag_id AND "
                    + "article_tags.article_id = articles.id) "
                    + "AND articles.short = ? ");
        } else {
        	sql = sql.concat(" WHERE articles.short = ? ");
        }
        args.add(1);
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
        List<Article> ret = null;
		List<Map<String, Object>> res = jdbcTpl.queryForList(sql, args.toArray());
		if (res.size() > 0) {
			ret = new ArrayList<>();
			for (Map<String, Object> row : res) {
				ret.add(this.processArticleRow(row, (int)row.get("id")));
			}
		}
		return ret;
	}
	
	/**
	 * I have no idea why I kept this ugly code.
	 * I mean I know why, I was watching a show and copy pasting.
	 */
	public List<ArticleSummary> getArticleSummariesDescFromTo(long start, int count, String tags, String order) throws DataAccessException {
		if (start < 0) {
            start = 0;
        }
        // I could do this in a single statement but going to do it in two.
        // I'm using limit and offset, which are supported by PostgreSQL and MySQL (normally) but
        // not most other databases.
        String sql = "SELECT articles.id, articles.title, "
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
            sql = sql.concat(" (tags.id = article_tags.tag_id AND "
                    + "article_tags.article_id = articles.id) "
                    + "AND articles.short = ? ");
        } else {
        	sql = sql.concat(" WHERE articles.short = ? ");
        }
        args.add(0);
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
				ret.add(this.processArticleSummaryRow(row, (int)row.get("id")));
			}
		}
		return ret;
	}

	public List<ArticleTag> getAllTags() throws DataAccessException {
		List<ArticleTag> tags = jdbcTpl.query("SELECT * FROM tags", 
				new BeanPropertyRowMapper<ArticleTag>(ArticleTag.class));
		return tags;
	}

	public long getCommentCount(long articleID) throws DataAccessException {
//		return jdbcTpl.queryForObject("SELECT count(*) FROM comments WHERE article_id = ?", Long.class, 
//				new Object[] {articleID});
		return jdbcTpl.queryForObject("SELECT count(*) FROM comments WHERE article_id = ?", 
				Long.class, articleID);
	}

	/**
	 * The tags thing in there actually doesn't work.
	 * It only works for one tag.
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
            sql = sql.concat(" AND (tags.id = article_tags.tag_id AND "
                    + "article_tags.article_id = articles.id)");
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
//        Object[] args = null;
//        if (tagsA != null) {
//        	args = new Object[tagsA.length];
//            for (int i = 0; i < tagsA.length; i++) {
//                // I originally meant the list to use long as index
//                // but arrays use int, so yeah... I'm using int.
//                // F*** consistency.
//                //stmt.setString(i, tagsA[i - 1]);
//            	args[i] = tagsA[i];
//            }
//        }
        ret = jdbcTpl.queryForObject(sql, Long.class, (Object[])tagsA);
        return ret;
	}

	public Article getArticleById(long id) throws DataAccessException {
		// I think you're supposed to queryForList and check the list size.
		// I used another method.
		try {
			Map<String, Object> res = jdbcTpl.queryForMap("SELECT * FROM articles WHERE id = ?", 
					id);
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
		sum.setArticleURL((String)res.get("article_url"));
		// I should include the author in the query:
		String author = "Anonymous";
		User usr = this.getUser((int)res.get("user_id"));
		if (usr != null) {
			author = usr.getName();
		}
		sum.setAuthor(author);
		sum.setId(id);
		sum.setCommentsCount(this.getCommentCount(id));
		// TODO I need to log the date because it's wrong somehow.
		int dateVal = (int)res.get("date");
		java.util.Date date = new java.util.Date((long)dateVal * 1000);
        sum.setDate(date);
        if ((int)res.get("published") > 0) {
        	sum.setPublished(true);
        } else {
        	sum.setPublished(false);
        }
        sum.setSummary((String)res.get("summary"));
        sum.setTags(this.getTagsForArticle(id));
        sum.setThumbImage((String)res.get("thumb_image"));
        sum.setTitle((String)res.get("title"));
        return sum;
	}
	
	private Article processArticleRow(Map<String, Object> res, long id) {
        Article art = new Article();
        art.setArticleSummary(this.processArticleSummaryRow(res, id));
        art.setContent((String)res.get("content"));
		return art;
	}

	public boolean insertArticle(Article article) throws DataAccessException {
		return false;
	}

	public boolean updateArticle(Article article) throws DataAccessException {
		return false;
	}

	public boolean deleteArticleById(long id) throws DataAccessException {
		return false;
	}

	public List<ArticleTag> getTagsForArticle(long id) throws DataAccessException {
		List<ArticleTag> tags = jdbcTpl.query("SELECT tags.name, tags.id, tags.main_tag "
				+ "FROM article_tags, tags WHERE article_tags.article_id = ? AND article_tags.tag_id = tags.id", 
				new BeanPropertyRowMapper<ArticleTag>(ArticleTag.class), id);
		return tags;
	}

	public boolean changeArticleId(long previousId, long newId) throws DataAccessException {
		return false;
	}

	public List<Comment> getCommentsFromTo(long start, int count, long articleId) throws DataAccessException {
		List<Comment> res = new ArrayList<Comment>();
		if (start < 0) start = 0;
		// I could do this in a single statement but going to do it in two.
		// I'm using limit and offset.
		String sql = "SELECT comments.id, comments.article_id, comments.author, " +
					"comments.comment, comments.date FROM comments, articles WHERE "
					+ "articles.id = ? AND articles.id = comments.article_id ORDER BY comments.id ASC " +
					"LIMIT ? OFFSET ?";
		List<Map<String, Object>> coms = jdbcTpl.queryForList(sql, 
				articleId, count, start);
		coms.forEach(i -> res.add(this.processCommentRow(i)));
		return res;
	}
	
	private Comment processCommentRow(Map<String, Object> row) {
		Comment com = new Comment();
		com.setArticleId((int)row.get("article_id"));
		com.setAuthor((String)row.get("author"));
		com.setClientIP((String)row.get("client_ip"));
		com.setComment((String)row.get("comment"));
		int dateVal = (int)row.get("date");
		java.util.Date date = new java.util.Date((long)dateVal * 1000);
		com.setDate(date);
		com.setId((int)row.get("id"));
		return com;
	}

	public List<Comment> getCommentsFromTo(long start, int count, String articleUrl) throws DataAccessException {
		long articleId = this.getArticleIdFromUrl(articleUrl);
		if (articleId > 0) return this.getCommentsFromTo(start, count, articleId);
		return null;
	}
	
	public long getArticleIdFromUrl(String url) throws DataAccessException {
		try {
			return jdbcTpl.queryForObject("SELECT id FROM articles WHERE article_url = ?", 
					Long.class, url);
		} catch (IncorrectResultSizeDataAccessException ex) {
			return -1;
		}
	}
	
	public void insertComment(Comment comment) throws DataAccessException {
		String sql = "INSERT INTO comments " +
					"(article_id, author, comment, date, client_ip) VALUES (?, ?, ?, ?, ?)";
//			stmt.setLong(1, comment.getArticleId());
//			stmt.setString(2, comment.getAuthor());
//			stmt.setString(3, comment.getComment());
//			if (comment.getDate() == null) {
//				java.util.Date now = new java.util.Date();
//				comment.setDate(now);
//			}
//			long stamp = comment.getDate().getTime() / 1000;
//			stmt.setLong(4, stamp);
//			stmt.setString(5, comment.getClientIP());
		if (comment.getDate() == null) {
			comment.setDate(new java.util.Date());
		}
		jdbcTpl.update(sql, 
				comment.getArticleId(), 
				comment.getAuthor(), 
				comment.getComment(),
				comment.getDate().getTime() / 1000,
				comment.getClientIP());
	}
	
	public Comment getLastComment() throws DataAccessException {
		List<Map<String, Object>> res = jdbcTpl.queryForList("SELECT * FROM comments ORDER BY id DESC LIMIT 1");
		if (res.size() > 0) {
			return this.processCommentRow(res.get(0));
		}
		return null;
	}
	
}
