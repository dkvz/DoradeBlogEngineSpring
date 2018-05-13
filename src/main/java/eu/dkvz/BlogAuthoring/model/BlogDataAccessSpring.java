package eu.dkvz.BlogAuthoring.model;

import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BlogDataAccessSpring extends BlogDataAccess {

	@Autowired
    private JdbcTemplate jdbcTpl;
	
	@Override
	public void connect() throws DataAccessException {
		// TODO Auto-generated method stub

	}

	@Override
	public void disconnect() throws DataAccessException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isConnected() throws DataAccessException {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public User getUser(long id) throws DataAccessException {
		List<User> res = jdbcTpl.query("SELECT name, id FROM users WHERE id = ?", 
				new BeanPropertyRowMapper<User>(User.class), id);
		if (res.size() > 0) return res.get(0);
		else return null;
	}

	/**
	 * I have no idea why I kept this ugly code.
	 * I mean I know why, I was watching a show and copy pasting.
	 */
	@Override
	public List<ArticleSummary> getArticleSummariesDescFromTo(long start, int count, boolean isShort, String tags) throws DataAccessException {
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
        if (isShort) args.add(1);
        else args.add(0);
        if (count < 1) {
            sql = sql.concat("ORDER BY articles.id DESC");
        } else {
            sql = sql.concat("ORDER BY articles.id DESC LIMIT ? OFFSET ?");
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

	@Override
	public List<ArticleTag> getAllTags() throws DataAccessException {
		List<ArticleTag> tags = jdbcTpl.query("SELECT * FROM tags", 
				new BeanPropertyRowMapper<ArticleTag>(ArticleTag.class));
		return tags;
	}

	@Override
	public long getCommentCount(long articleID) throws DataAccessException {
//		return jdbcTpl.queryForObject("SELECT count(*) FROM comments WHERE article_id = ?", Long.class, 
//				new Object[] {articleID});
		return jdbcTpl.queryForObject("SELECT count(*) FROM comments WHERE article_id = ?", 
				Long.class, articleID);
	}

	/**
	 * The tags thing in there actually doesn't work.
	 * It only works for one tag.
	 */
	@Override
	public long getArticleCount(boolean published, String tags) throws DataAccessException {
		long ret = 0l;
        String sql = "SELECT count(*) FROM articles";
        String[] tagsA = null;
        if (tags != null && !tags.isEmpty()) {
            tagsA = tags.split(",");
            // TODO: I should use StringBuilder here instead
            // of recreating a String at every concat.
            sql = sql.concat(", tags, article_tags");
            if (published) {
                sql = sql.concat(" WHERE articles.published = 1");
            }
            boolean where = false;
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
            }
        }
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

	@Override
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

	@Override
	public boolean insertArticle(Article article) throws DataAccessException {
		return false;
	}

	@Override
	public boolean updateArticle(Article article) throws DataAccessException {
		return false;
	}

	@Override
	public boolean deleteArticleById(long id) throws DataAccessException {
		return false;
	}

	@Override
	public List<ArticleTag> getTagsForArticle(long id) throws DataAccessException {
		List<ArticleTag> tags = jdbcTpl.query("SELECT tags.name, tags.id, tags.main_tag "
				+ "FROM article_tags, tags WHERE article_tags.article_id = ? AND article_tags.tag_id = tags.id", 
				new BeanPropertyRowMapper<ArticleTag>(ArticleTag.class), id);
		return tags;
	}

	@Override
	public boolean changeArticleId(long previousId, long newId) throws DataAccessException {
		return false;
	}

}
