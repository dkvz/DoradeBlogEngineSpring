package eu.dkvz.BlogAuthoring.model;

import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
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
		return false;
	}

	@Override
	public User getUser(long id) throws DataAccessException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ArticleSummary> getArticleSummariesDescFromTo(long start, int count, String tags) throws DataAccessException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ArticleTag> getAllTags() throws DataAccessException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getCommentCount(long articleID) throws DataAccessException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getArticleCount(boolean published, String tags) throws DataAccessException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Article getArticleById(long id) throws DataAccessException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean insertArticle(Article article) throws DataAccessException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean updateArticle(Article article) throws DataAccessException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean deleteArticleById(long id) throws DataAccessException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<ArticleTag> getTagsForArticle(long id) throws DataAccessException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean changeArticleId(long previousId, long newId) throws DataAccessException {
		// TODO Auto-generated method stub
		return false;
	}

}
