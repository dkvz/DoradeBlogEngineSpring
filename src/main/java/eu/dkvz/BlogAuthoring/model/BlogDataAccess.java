/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.dkvz.BlogAuthoring.model;

import java.sql.*;
import java.util.*;

/**
 *
 * @author william
 */
public abstract class BlogDataAccess {
    
    public abstract void connect() throws Exception;
    public abstract void disconnect() throws Exception;
    public abstract boolean isConnected() throws Exception;
    public abstract User getUser(long id) throws Exception;
    public abstract List<ArticleSummary> getArticleSummariesDescFromTo(long start, int count, boolean isShort, String tags) throws Exception;
    public abstract List<ArticleTag> getAllTags() throws Exception;
    public abstract long getCommentCount(long articleID) throws Exception;
    public abstract long getArticleCount(boolean published, String tags) throws Exception;
    public abstract Article getArticleById(long id) throws Exception;
    public abstract boolean insertArticle(Article article) throws Exception;
    public abstract boolean updateArticle(Article article) throws Exception;
    public abstract boolean deleteArticleById(long id) throws Exception;
    public abstract List<ArticleTag> getTagsForArticle(long id) throws Exception;
    public abstract boolean changeArticleId(long previousId, long newId) throws Exception;
    
}
