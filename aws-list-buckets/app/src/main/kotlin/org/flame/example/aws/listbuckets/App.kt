package org.flame.example.aws.listbuckets

import org.slf4j.LoggerFactory
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.ListObjectsRequest
import software.amazon.awssdk.services.s3.model.S3Exception
import software.amazon.awssdk.services.s3.model.S3Object
import kotlin.system.exitProcess

class App {
    private val logger = LoggerFactory.getLogger(javaClass)

    init {
        logger.info("Running...")
    }

    fun run() {
        val bucketName = "opsmx-michael-test"
        val region = Region.US_EAST_2
        val s3 = S3Client.builder()
            .region(region)
            .build()

        listBucketObjects(s3, bucketName)
        s3.close()
    }

    private fun listBucketObjects(s3: S3Client, bucketName: String) {
        try {
            val listObjects = ListObjectsRequest
                .builder()
                .bucket(bucketName)
                .build()
            val res = s3.listObjects(listObjects)
            val objects = res.contents()
            val iterVals = objects.listIterator()
            while (iterVals.hasNext()) {
                val myValue = iterVals.next() as S3Object
                println("The name of the key is ${myValue.key()}")
                println("The object is ${myValue.size()} bytes")
                println("The owner is ${myValue.owner()}")
            }
        } catch (e: S3Exception) {
            println(e.awsErrorDetails().errorMessage())
            exitProcess(1)
        }
    }

}

fun main() {
    App().run()
}
