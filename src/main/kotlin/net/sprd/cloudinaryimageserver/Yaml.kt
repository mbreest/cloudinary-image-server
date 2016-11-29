package net.sprd.cloudinaryimageserver

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import javax.validation.constraints.NotNull

@Configuration
@ConfigurationProperties(prefix = "cloudinary")
open class CloudinaryConfiguration() {
    @NotNull lateinit var cloudName:String
    @NotNull lateinit var apiKey:String
    @NotNull lateinit var secret:String
}


