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

# TODO
* The spring-boot-devtools dependency is nice but I should check what "optional" means and if it does anything when building to prod.
* What happens if you don't use an integer in /articles-starting-from/{articleId}?
* Try to create a SQL error to see if the @ExceptionHandler thingy is working.