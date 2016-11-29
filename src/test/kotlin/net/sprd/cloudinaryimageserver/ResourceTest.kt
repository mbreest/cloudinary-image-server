package net.sprd.cloudinaryimageserver

import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringRunner

import org.assertj.core.api.Assertions.assertThat
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ResourcesTest {
    @Autowired
    lateinit var restTemplate: TestRestTemplate;

    init {
        System.setProperty("spring.config.location", "cloudinary.yaml")
    }

    @Test
    fun shouldReturnOKForSmallSize() {
        val response = this.restTemplate.getForEntity("/products/small/105007578/v1/men's-t-shirt.jpg", Void::class.java)
        assertThat(response.statusCode.value()).isEqualTo(200)
    }

    @Test
    fun shouldReturnOKForMediumSize() {
        val response = this.restTemplate.getForEntity("/products/medium/105007578/v1/men's-t-shirt.jpg", Void::class.java)
        assertThat(response.statusCode.value()).isEqualTo(200)
    }

    @Test
    fun shouldReturnOKForLargeSize() {
        val response = this.restTemplate.getForEntity("/products/large/105007578/v1/men's-t-shirt.jpg", Void::class.java)
        assertThat(response.statusCode.value()).isEqualTo(200)
    }

    @Test
    fun shouldCacheOKResponse() {
        val response = this.restTemplate.getForEntity("/products/large/105007578/v1/men's-t-shirt.jpg", Void::class.java)
        assertThat(response.statusCode.value()).isEqualTo(200)
        assertThat(response.headers.cacheControl).isEqualTo("max-age=86400")
        assertThat(response.headers.getFirst("Edge-Control")).isEqualTo("cache-maxage=365d")
    }

    @Test
    fun shouldReturnBadRequestForWrongSize() {
        val response = this.restTemplate.getForEntity("/products/wrong/105007578/v1/men's-t-shirt.jpg", Void::class.java)
        assertThat(response.statusCode.value()).isEqualTo(400)
    }

    @Test
    fun shouldReturnJPGAsDefault() {
        val response = this.restTemplate.getForEntity("/products/large/105007578/v1/men's-t-shirt.jpg", Void::class.java)
        assertThat(response.statusCode.value()).isEqualTo(200)
        assertThat(response.headers.contentType).isEqualTo(MediaType.IMAGE_JPEG)
    }

    @Test
    fun shouldReturnWebPForChromeBrowser() {
        val headers = HttpHeaders();
        headers.add(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.98 Safari/537.36");

        val response = this.restTemplate.exchange("/products/large/105007578/v1/men's-t-shirt.jpg", HttpMethod.GET, HttpEntity<Any>(headers), Void::class.java);

        assertThat(response.statusCode.value()).isEqualTo(200)
        assertThat(response.headers.contentType).isEqualTo(MediaType("image","webp"))
    }

    @Test
    fun shouldRedirectOnWrongSEOURL() {
        val response = this.restTemplate.getForEntity("/products/medium/105007578/v1/seo-test.jpg", Void::class.java)
        assertThat(response.statusCode.value()).isEqualTo(301)
        assertThat(response.headers.location.path).isEqualTo("/products/medium/105007578/v1/men's-t-shirt.jpg")
    }

    @Test
    fun shouldCacheRedirect() {
        val response = this.restTemplate.getForEntity("/products/medium/105007578/v1/seo-test.jpg", Void::class.java)
        assertThat(response.statusCode.value()).isEqualTo(301)
        assertThat(response.headers.cacheControl).isEqualTo("max-age=86400")
    }

    @Test
    fun shouldReturnBadRequestForNonExistingProductId() {
        val response = this.restTemplate.getForEntity("/products/medium/does-not-exist/v1/seo-test.jpg", Void::class.java)
        assertThat(response.statusCode.value()).isEqualTo(400)
    }

    @Test
    fun shouldReturnRobotsNoIndexForSmallImages() {
        val response = this.restTemplate.getForEntity("/products/small/105007578/v1/men's-t-shirt.jpg", Void::class.java)
        assertThat(response.statusCode.value()).isEqualTo(200)
        assertThat(response.headers.getFirst("X-Robots-Tag")).isEqualTo("noindex")
    }

    @Test
    fun shouldReturnRobotsIndexForMediumImages() {
        val response = this.restTemplate.getForEntity("/products/medium/105007578/v1/men's-t-shirt.jpg", Void::class.java)
        assertThat(response.statusCode.value()).isEqualTo(200)
        assertThat(response.headers.getFirst("X-Robots-Tag")).isEqualTo("index")
    }

    @Test
    fun shouldReturnRobotsIndexForLargeImages() {
        val response = this.restTemplate.getForEntity("/products/medium/105007578/v1/men's-t-shirt.jpg", Void::class.java)
        assertThat(response.statusCode.value()).isEqualTo(200)
        assertThat(response.headers.getFirst("X-Robots-Tag")).isEqualTo("index")
    }

    @Test
    fun shouldReturnEdgeContentTag() {
        val response = this.restTemplate.getForEntity("/products/medium/105007578/v1/men's-t-shirt.jpg", Void::class.java)
        assertThat(response.statusCode.value()).isEqualTo(200)
        assertThat(response.headers.getFirst("Edge-Content-Tag")).isEqualTo("product medium 105007578")
    }
}
