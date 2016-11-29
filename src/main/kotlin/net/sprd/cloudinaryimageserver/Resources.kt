package net.sprd.cloudinaryimageserver

import org.apache.http.client.fluent.Request
import org.springframework.http.HttpHeaders
import org.springframework.util.FileCopyUtils
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@RestController
open class Resources(private val cloudinaryImageServer: CloudinaryUrlGenerator) {
    private val LOGGER = Logger.getLogger(Resources::class.java.name)

    private val HEADER_EDGE_CONTENT_TAG = "Edge-Content-Tag"
    private val HEADER_EDGE_CONTROL = "Edge-Control"
    private val HEADER_X_ROBOTS_TAG = "X-Robots-Tag"

    private val X_ROBOTS_TAG_INDEX = "index";
    private val X_ROBOTS_TAG_NO_INDEX = "noindex";

    private val CACHE_TIME_ONE_YEAR = 365
    private val CACHE_TIME_ONE_DAY = TimeUnit.SECONDS.convert(1, TimeUnit.DAYS);

    private val DIRECTIVE_MAX_AGE = "max-age";
    private val DIRECTIVE_CACHE_MAXAGE = "cache-maxage"

    @GetMapping("/products/{size}/{productId}/{mods}/{seo}.jpg")
    fun renderProductImage(@PathVariable size: ImageSize, @PathVariable productId: String, @PathVariable mods: String, @PathVariable seo: String,
                           request: HttpServletRequest, response: HttpServletResponse) {
        val format = Format.fromUserAgent(request.getHeader(HttpHeaders.USER_AGENT))

        val productImage = cloudinaryImageServer.createProductImageUrl(productId, Mods.parse(mods), size, seo, format)
        LOGGER.log(Level.INFO, String.format("Fetching image from cloudinary: %s", productImage.imageUrl))

        if (productImage.redirect) {
            response.status = HttpServletResponse.SC_MOVED_PERMANENTLY
            response.setHeader(HttpHeaders.LOCATION, productImage.redirectUrl)
            response.setHeader(HttpHeaders.CACHE_CONTROL, "$DIRECTIVE_MAX_AGE=$CACHE_TIME_ONE_DAY")
            return
        }

        if (!productImage.imageFound) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST, "Product does not exist!")
            return
        }

        try {
            val cloudinaryResponse = Request.Get(productImage.imageUrl).execute()
            val cloudinaryHttpResponse = cloudinaryResponse.returnResponse()
            if (cloudinaryHttpResponse.statusLine.statusCode == HttpServletResponse.SC_OK) {
                response.contentType = format.toMediaType()
                response.setHeader(HEADER_EDGE_CONTENT_TAG, "product ${size.name} $productId")
                response.setHeader(HttpHeaders.CACHE_CONTROL, String.format("$DIRECTIVE_MAX_AGE=$CACHE_TIME_ONE_DAY", CACHE_TIME_ONE_DAY))
                response.setHeader(HEADER_EDGE_CONTROL, "$DIRECTIVE_CACHE_MAXAGE=${CACHE_TIME_ONE_YEAR}d")
                response.setHeader(HEADER_X_ROBOTS_TAG, if (size == ImageSize.small) X_ROBOTS_TAG_NO_INDEX else X_ROBOTS_TAG_INDEX)
                FileCopyUtils.copy(cloudinaryHttpResponse.entity.content, response.outputStream)
                return
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not retrieve image from Cloudinary!")
                return
            }
        } catch (e: IOException) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not retrieve image from Cloudinary!")
            return
        }

    }
}