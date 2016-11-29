package net.sprd.cloudinaryimageserver

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
open class CloudinaryImageServerApplication

fun main(args: Array<String>) {
    SpringApplication.run(CloudinaryImageServerApplication::class.java, *args)
}