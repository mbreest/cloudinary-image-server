Copy cloudinary-template.yaml to cloudinary.yaml and add your cloudinary API key.

Start cloudinary image server

```bash
mvn package && java -jar target/cloudinary-image-server-1.0-SNAPSHOT.jar --spring.config.location=cloudinary.yaml
```

Test different image sizes:

* http://localhost:8080/products/small/105007578/v1/men%27s-t-shirt.jpg
* http://localhost:8080/products/medium/105007578/v1/men%27s-t-shirt.jpg
* http://localhost:8080/products/large/105007578/v1/men%27s-t-shirt.jpg

Test different views:

* http://localhost:8080/products/medium/105007578/v1/men%27s-t-shirt.jpg
* http://localhost:8080/products/medium/105007578/v2/men%27s-t-shirt.jpg

Test different appearances:

* http://localhost:8080/products/medium/105007578/v2a1/men%27s-t-shirt.jpg
* http://localhost:8080/products/medium/105007578/v1a2/men%27s-t-shirt.jpg
* http://localhost:8080/products/medium/105007578/v2a3/men%27s-t-shirt.jpg
* http://localhost:8080/products/medium/105007578/v2a5/men%27s-t-shirt.jpg

Test SEO redirects:

* http://localhost:8080/products/medium/105007578/v1/seo-test.jpg

Test different products:

* http://localhost:8080/products/medium/105007578/v1/men%27s-t-shirt.jpg
* http://localhost:8080/products/medium/130122809/v1/men%27s-t-shirt.jpg
* http://localhost:8080/products/medium/138465641/v1/men%27s-t-shirt.jpg
* http://localhost:8080/products/medium/109487335/v1/men%27s-t-shirt.jpg
* http://localhost:8080/products/medium/139263897/v1/men%27s-t-shirt.jpg
* http://localhost:8080/products/medium/138321764/v1/men%27s-t-shirt.jpg
* http://localhost:8080/products/medium/110730808/v1/men%27s-t-shirt.jpg
* ...

Images are generated using our Cloudinary account.

Open Issues:
 
* handling of all kinds of print types
* handling of vector design colors
* handling of text
