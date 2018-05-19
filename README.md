Database not included. I think.

# Endpoints
Let's remake the API endpoints.

Original routes code was as such:
```
GET		/articles-starting-from/:articleId		controllers.Application.articlesStartingFrom(articleId: Long, max: Integer ?= 30, tags: String ?= "", order: String ?= "desc")
GET		/shorts-starting-from/:articleId		controllers.Application.shortsStartingFrom(articleId: Long, max: Integer ?= 30, tags: String ?= "", order: String ?= "desc")
GET		/article/:articleURL					controllers.Application.article(articleURL: String)
GET		/comments-starting-from/:articleURL				controllers.Application.commentsStartingFrom(articleURL: String, start: Integer ?= 0, max: Integer ?= 30) 
GET		/test		controllers.Application.test
GET		/tags		controllers.Application.tags
POST	/comments	controllers.Application.saveComment
GET		/gimme-sitemap/		controllers.Application.sitemap(articlesRoot: String)
GET /last-comment controllers.Application.lastComment
```

## GET /articles-starting-from/:articleId
Args:
* max ; default 30
* tags ; default ""
* order ; default "desc"

## GET /shorts-starting-from/:articleId
Same endpoint as above but for shorts.

## GET /article/:articleUrl
I'm going to return a 404 if it doesn't exist but I'm not sure what was happening with the other service.

## GET /comments-starting-from/:articleUrl
Args:
* start ; default 0
* max ; default 30

## GET /tags
The endpoint to get the list of available tags.

## POST /comments
Works like the original. Which is kind of weird.

Only difference is that it throws Bad Request exceptions instead of only 500. It can still send a 500 in case of a database issue.

Used to return the string "OK" if the insert worked. Spring boot inserts this in an HMTL page.
So I changed it to return the comment in JSON form.

Requires a body in form-www-urlencoded format, with either "articleurl" or "article_id" as the way to link an article to the comment.

## GET /gimme-sitemap
Expects a query parameter called "articlesRoot" which is the domain name root for the site map without http:// and with no trailing slash.

By default my XML-in-a-string was returned inside an HTML body. I had to specify the content-type for it to work.

## GET /last-comment
Very simple.

## GET /render-article/:articleUrl
Creates a prerendered HTML page for social media site robots complete with Open Graph meta tags and the full article content (bar the table of content).

Will generate URI for "shorts" if the article ID is numeric, otherwise the item will be seen as an "article".


# TODO
* The spring-boot-devtools dependency is nice but I should check what "optional" means and if it does anything when building to prod.
* What happens if you don't use an integer in /articles-starting-from/{articleId}?
* Change the favicon.
