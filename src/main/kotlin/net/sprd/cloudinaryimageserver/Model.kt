package net.sprd.cloudinaryimageserver

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.http.MediaType
import java.util.regex.Pattern

val MOD_VIEW = "v";
val MOD_APPEARANCE = "a";

val MEDIA_TYPE_WEBP = "image/webp"

val BROWSER_CHROME = "Chrome"

val DEFAULT_VIEW = "1";

data class Design(var name: String, var x: Int, var y: Int, var width: Int, var height: Int)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Product(var version: Int = 0, var productType: Reference, var appearance: Reference, var configurations: List<ProductConfiguration> = listOf())

@JsonIgnoreProperties(ignoreUnknown = true)
data class Reference(var id: String)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProductConfiguration(var printArea: Reference, var offset: Offset, var content: Content, var type: String) {
    companion object {
        val TYPE_DESIGN = "design";
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Offset(var x: Double = 0.toDouble(), var y: Double = 0.toDouble())

@JsonIgnoreProperties(ignoreUnknown = true)
data class Content(var svg: Svg)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Svg(var image: Image)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Image(var designId: String, var width: Double = 0.toDouble(), var height: Double = 0.toDouble())

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProductType(var version: Int = 0, var name: String, var views: List<View> = listOf(), var printAreas: List<PrintArea> = listOf()) {
    fun isPrintAreaOnView(printAreaId: String, viewId: String) =
            !printAreas
                    .filter({ printArea -> printArea.id == printAreaId })
                    .filter({ printArea -> printArea.defaultView.id == viewId })
                    .isEmpty()

    fun getView(viewId: String) = views.filter({ view -> view.id == viewId }).get(0)
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class View(var id: String, var size: Size, var viewMaps: List<ViewMap> = listOf()) {
    fun getViewMap(printAreaId: String) = viewMaps.filter({ viewMap -> viewMap.printArea.id == printAreaId }).get(0)
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class ViewMap(var printArea: Reference, var offset: Offset)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PrintArea(var id: String, var defaultView: Reference)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Size(var width: Double = 0.toDouble(), var height: Double = 0.toDouble())

data class Mods(var view: String = DEFAULT_VIEW, var appearance: String ? = null) {
    fun valueOf(): String {
        var value = MOD_VIEW + view
        if (appearance != null) {
            value += MOD_APPEARANCE + appearance!!
        }
        return value
    }

    companion object {
        val pattern = Pattern.compile("(($MOD_VIEW)(\\d*))?(($MOD_APPEARANCE)(\\d*))?")

        fun parse(modString: String): Mods {
            val mods = Mods()
            val matcher = pattern.matcher(modString)
            if (matcher.find()) {
                var i = 0
                while (i < matcher.groupCount()) {
                    val key = matcher.group(i + 2)
                    val value = matcher.group(i + 3)
                    if (key != null) {
                        when (key) {
                            MOD_VIEW -> mods.view = value
                            MOD_APPEARANCE -> mods.appearance = value
                        }
                    }
                    i += 3
                }
            }
            return mods;
        }
    }
}

enum class Format {
    jpg, webp, auto;

    fun toMediaType() = when (this) {
        webp -> MEDIA_TYPE_WEBP
        else -> MediaType.IMAGE_JPEG_VALUE
    }

    companion object {
        fun fromUserAgent(userAgent: String) = when {
            userAgent.contains(BROWSER_CHROME) -> Format.webp
            else -> Format.jpg
        }
    }
}

enum class ImageSize {
    small, medium, large
}

data class ProductImage(var redirect: Boolean = false, var redirectUrl: String? = null, var imageFound: Boolean = false, var imageUrl: String? = null, var error: Boolean = false)
