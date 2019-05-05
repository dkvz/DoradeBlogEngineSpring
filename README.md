# Dorade Blog Engine
REST API used by my blog, which is a single page web app.

For some reasons it's completely independent from the actual web site and even responds on another domain name.

## Open the project
It's best to use the Spring Tool Suite. Clone the repository and import the folder in SpringToolSuite.

## The database
I may not have included the SQLite database. Project won't work without it.

I'll fix this one day maybe.

Now it's even worse because I added a second database to hold the stats hoping I would be able to open it read write in other projects.

An empty stats DB is in the repo, has to be renamed "stats.sqlite" to work.

I added a @Configuration file called `DatasourcesConfiguration.java` which defines the datasources and JDBCTemplate objects to inject in other beans.

Nothing special is necessary to inject the primary database. For the stats database this is how you import the JDBCTemplate:
```java
@Autowired
@Qualifier("jdbcStats")
private JdbcTemplate jdbcTpl;
```

## GeoIP database
The app requires a GeoIP2 database to be present at the root of the project or it won't start.

It has to be the "City" database, and I never tested with anything else than the free "GeoIP Lite" one.

## Creating the deployable jar
Go to the project root directory and run:
```
./mvnw package
```

The produced jar is "executable". I might change that in the future though.

## Deploying the jar
I'm still figuring this out as my method using links and systemd has caused a server crash once. Also nobody will read this ever.

## Cronjob
The rss endpoint is intended to be called at least once a day through a cronjob.

I use this line for the moment:
```
wget "http://localhost:9001/rss" -O /srv/vhosts/dorade_site/httpdocs/rss.xml
```

## Endpoints
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

### GET /shorts-starting-from/:articleId
Same endpoint as above but for shorts.

### GET /article/:articleUrl
I'm going to return a 404 if it doesn't exist but I'm not sure what was happening with the other service.

### GET /comments-starting-from/:articleUrl
Args:
* start ; default 0
* max ; default 30

### GET /tags
The endpoint to get the list of available tags.

### POST /comments
Works like the original. Which is kind of weird.

Only difference is that it throws Bad Request exceptions instead of only 500. It can still send a 500 in case of a database issue.

Used to return the string "OK" if the insert worked. Spring boot inserts this in an HMTL page.
So I changed it to return the comment in JSON form.

Requires a body in form-www-urlencoded format, with either "articleurl" or "article_id" as the way to link an article to the comment.

### GET /gimme-sitemap
Expects a query parameter called "articlesRoot" which is the domain name root for the site map without http:// and with no trailing slash.

By default my XML-in-a-string was returned inside an HTML body. I had to specify the content-type for it to work.

### GET /last-comment
Very simple.

### GET /render-article/:articleUrl
Creates a prerendered HTML page for social media site robots complete with Open Graph meta tags and the full article content (bar the table of content).

Will generate URI for "shorts" if the article ID is numeric, otherwise the item will be seen as an "article".

### GET /import-articles
Initiates the import from the local import folder on the server.

It's expecting to find JSON files with a format similar to this:
```
{
  "id":35,
  "articleURL":"truc_machin",
  "title":"Mon Super Titre",
  "summary":"Salut\nEt oui",
  "content":"Contenu de l'article",
  "published":false,
  "thumbImage":"img.png",
  "userId": 2,
  "date": "2018-07-25T21:35:04.887Z",
  "tags": [
    {"id":"21"},
    {"id":"44"}
  ],
  "short": false
}
```
Do pay attention to the date format, it's one of the Javascript outputs. Don't remember if it's the UTC date ot another.

The tags array has to be the complete list of tags for the article, putting in an empty one will remove all tags.

The "id" field is only mandatory when updating an existing article. In that case, some fields can be left to null and will be ignored by the update operation.

However, **published MUST always be present**, if it's not it's assumed to be false. I think.

The importer ignores files that it doesn't have write access to. And it won't tell you about it in the response, it's just discarded.

The field "articleUrl" is allowed instead of "articleURL".

When adding an article, if articleUrl is not present, it automatically becomes a short. Even if you had set short to false.

Response is uh... I've yet to decide.

The call is synchronous, when you receive a response from the server, it's done adding the data to the database.

# TODO
* I refactored a static method to create order by statements, I need to use it everywhere.
* The spring-boot-devtools dependency is nice but I should check what "optional" means and if it does anything when building to prod.
* What happens if you don't use an integer in /articles-starting-from/{articleId}?
* Change the favicon.
* getRSS should return a 403 when the requesting IP address is not allowed.
* Add statistics such as the amount of views.
* The methods inserting stuff into the database could set the inserted id in the Java Bean that was inserted.
* The way I check for existence of stuff, especially in ArticleImportService, is getting the whole data. I could make more efficient checks.

# Testing the article import
* Try updating an article with a date set, then with no date set.

# More notes

## The day I switched out Hikari
I have an issue with my dual datasource setup in that it works on my dev machine but not on the server, and only the second datasource is not working.

So... I tried using the Tomcat connection pool instead of Hikari to see if that helped.

It does not.

But I think it's nice to document how I did.

Initially in pom.xml I just had:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jdbc</artifactId>
    <exclusions>
</dependency>
```

To exclude Hikari and add the Tomcat CP:
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-jdbc</artifactId>
  <exclusions>
    <exclusion>
        <groupId>com.zaxxer</groupId>
        <artifactId>HikariCP</artifactId>
    </exclusion>
  </exclusions>
</dependency>
<dependency>
  <groupId>org.apache.tomcat</groupId>
  <artifactId>tomcat-jdbc</artifactId>
</dependency>
```

Then I had to change they configuration keys in `application.properties` to use "url" instead of "jdbc-url" as in:
```
spring.datasource.url=jdbc:sqlite:./db.sqlite
```