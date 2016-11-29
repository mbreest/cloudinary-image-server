package net.sprd.cloudinaryimageserver

import com.cloudinary.Cloudinary
import com.cloudinary.Transformation
import com.cloudinary.utils.ObjectUtils
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import org.apache.http.HttpStatus
import org.apache.http.client.fluent.Request
import org.apache.http.util.EntityUtils
import org.springframework.stereotype.Component
import java.io.IOException
import java.net.URLEncoder


@Component
open class CloudinaryUrlGenerator(val cloudinaryConfiguration: CloudinaryConfiguration) {
    private val cloudinary: Cloudinary

    private val productTypeCache: LoadingCache<String, ProductType>
    private val productCache: LoadingCache<String, Product>

    private val API_URL_PRODUCT_TYPE = "http://www.spreadshirt.de/api/v1/shops/205909/productTypes/%s?mediaType=json"
    private val API_URL_PRODUCT = "http://www.spreadshirt.de/api/v1/shops/205909/products/%s?mediaType=json"
    private val REDIRECT_URL_PRODUCT = "/products/%s/%s/%s/%s.jpg"
    private val IMAGE_URL_PRODUCT_TYPE = "/productTypes/%s/views/%s/appearances/%s,width=1200,height=1200,mediaType=png"
    private val IMAGE_URL_DESIGN = "designs:%s"

    private val SIZE_SMALL = 150
    private val SIZE_MEDIUM = 300
    private val SIZE_LARGE = 600

    private val SCALE = "scale"
    private val NORTH_WEST = "north_west"
    private val AUTO = "auto"

    init {
        cloudinary = Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudinaryConfiguration.cloudName,
                "api_key", cloudinaryConfiguration.apiKey,
                "api_secret", cloudinaryConfiguration.secret))

        val objectMapper = ObjectMapper().registerKotlinModule()
                .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)

        productTypeCache = CacheBuilder.newBuilder().build(object : CacheLoader<String, ProductType>() {
            @Throws(Exception::class)
            override fun load(productTypeId: String): ProductType {
                val productUrl = String.format(API_URL_PRODUCT_TYPE, productTypeId)
                val response = Request.Get(productUrl).execute()
                val httpResponse = response.returnResponse()
                if (httpResponse.statusLine.statusCode == HttpStatus.SC_OK) {
                    val productData = EntityUtils.toString(httpResponse.entity)
                    return objectMapper.readValue(productData, ProductType::class.java)
                } else {
                    throw IOException("ProductType not loadable from origin.")
                }
            }
        })

        productCache = CacheBuilder.newBuilder().build(object : CacheLoader<String, Product>() {
            @Throws(Exception::class)
            override fun load(productId: String): Product {
                val productUrl = String.format(API_URL_PRODUCT, productId)
                val response = Request.Get(productUrl).execute()
                val httpResponse = response.returnResponse()
                if (httpResponse.statusLine.statusCode == HttpStatus.SC_OK) {
                    val productData = EntityUtils.toString(httpResponse.entity)
                    return objectMapper.readValue(productData, Product::class.java)
                } else {
                    throw IOException("Product not loadable from origin.")
                }
            }
        })
    }

    fun createProductImageUrl(productId: String, mods: Mods, imageSize: ImageSize, seoPart: String, format: Format): ProductImage {
        val defaultImageSize = 600
        val targetImageSize = translateImageSizeToValue(imageSize)

        val product = loadSpreadshirtProduct(productId) ?: return ProductImage(imageFound = false)

        val productTypeId = product.productType.id
        val appearanceId = if (mods.appearance == null) product.appearance.id else mods.appearance
        val viewId = mods.view

        val productType: ProductType = loadSpreadshirtProductType(productTypeId) ?: return ProductImage(imageFound = false)

        val generatedSeoPart = productType.name.toLowerCase().replace(" ".toRegex(), "-")
        if (!seoPart.equals(generatedSeoPart)) {
            return ProductImage(redirect = true, redirectUrl = String.format(REDIRECT_URL_PRODUCT, imageSize.name, productId, mods.valueOf(), URLEncoder.encode(generatedSeoPart)))
        }

        val productTypeImageUrl = String.format(IMAGE_URL_PRODUCT_TYPE, productTypeId, viewId, appearanceId)
        val designs = product.configurations
                .filter({ config -> config.type.equals(ProductConfiguration.TYPE_DESIGN) })
                .map({ config -> config })
                .filter({ config -> productType.isPrintAreaOnView(config.printArea.id, viewId) })
                .map({ config ->
                    val designUrl = String.format(IMAGE_URL_DESIGN, config.content.svg.image.designId)


                    val view = productType.getView(viewId)
                    val viewMap = view.getViewMap(config.printArea.id)

                    val viewScaleX = defaultImageSize.toDouble() / view.size.width
                    val viewScaleY = defaultImageSize.toDouble() / view.size.height

                    val viewMapX = viewMap.offset.x * viewScaleX
                    val viewMapY = viewMap.offset.y * viewScaleY

                    val designConfigurationOffsetX = config.offset.x * viewScaleX
                    val designConfigurationOffsetY = config.offset.y * viewScaleY
                    val designConfigurationWidth = config.content.svg.image.width * viewScaleX
                    val designConfigurationHeight = config.content.svg.image.height * viewScaleY

                    val x = (viewMapX + designConfigurationOffsetX).toInt()
                    val y = (viewMapY + designConfigurationOffsetY).toInt()
                    val width = designConfigurationWidth.toInt()
                    val height = designConfigurationHeight.toInt()

                    Design(designUrl, x, y, width, height)
                }).filterNotNull()

        // deliver product type version from cloudinary cache if no designs are on shirt
        val imageVersion = if (designs.isEmpty()) productType.version else product.version

        return ProductImage(imageFound = true, imageUrl = generateProductImageUrl(productTypeImageUrl, defaultImageSize, targetImageSize, designs, imageVersion, format))
    }

    private fun generateProductImageUrl(productType: String, defaultImageSize: Int, targetImageSize: Int, designs: List<Design>, version: Int, format: Format): String {
        val transformation = Transformation()
        // default image size

        transformation.width(defaultImageSize).crop(SCALE)
        // apply design to product
        designs.forEach {
            design -> transformation.chain().overlay(design.name).gravity(NORTH_WEST).crop(SCALE).x(design.x).y(design.y).width(design.width).height(design.height)
        }
        // scale down to desired size
        transformation.chain().crop(SCALE).width(targetImageSize).quality(AUTO).fetchFormat(format.name)

        return cloudinary.url().version(version).transformation(transformation).generate(productType)
    }

    private fun loadSpreadshirtProduct(productId: String): Product? {
        try {
            return productCache.get(productId)
        } catch (e: Exception) {
            return null
        }

    }

    private fun loadSpreadshirtProductType(productTypeId: String?): ProductType? {
        try {
            return productTypeCache.get(productTypeId)
        } catch (e: Exception) {
            return null
        }

    }

    private fun translateImageSizeToValue(imageSize: ImageSize) = when (imageSize) {
        ImageSize.small -> SIZE_SMALL
        ImageSize.medium -> SIZE_MEDIUM
        else -> SIZE_LARGE
    }
}