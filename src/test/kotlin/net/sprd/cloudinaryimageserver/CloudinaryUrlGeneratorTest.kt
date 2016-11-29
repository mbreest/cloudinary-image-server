package net.sprd.cloudinaryimageserver

import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CloudinaryUrlGeneratorTest {
    var cloudinaryUrlGenerator:CloudinaryUrlGenerator? = null;

    @Before
    fun setup() {
        val config = CloudinaryConfiguration();
        config.cloudName = "xxx";
        config.apiKey = "";
        config.secret = "";
        cloudinaryUrlGenerator = CloudinaryUrlGenerator(config)
    }

    @Test
    fun testCreateProductUrl() {
        val expectedUrl = "http://res.cloudinary.com/xxx/image/upload/c_scale,w_600/c_scale,g_north_west,h_392,l_designs:15085750,w_265,x_172,y_60/c_scale,f_auto,q_auto,w_150/v1478003241/productTypes/6/views/1/appearances/4%2Cwidth%3D1200%2Cheight%3D1200%2CmediaType%3Dpng"

        val productImage = cloudinaryUrlGenerator?.createProductImageUrl("105007578", Mods(), ImageSize.small, "men's-t-shirt", Format.auto)

        assertTrue(productImage?.imageUrl == expectedUrl)
    }
}


