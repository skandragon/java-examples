package org.flame.aws.interceptor.example

import kotlin.system.exitProcess

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.S3Exception

class App {
    val greeting: String
        get() {
            return "Hello World!"
        }

    fun run() {
        val bucketName = "mybucket"
        val region = Region.US_WEST_2
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

            objects.forEach {
                print("\n The name of the key is " + it.key())
                print("\n The object is " + it.size() + " bytes")
                print("\n The owner is " + it.owner())

            }

        } catch (e: S3Exception) {
            System.err.println(e.awsErrorDetails().errorMessage());
            exitProcess(1);
        }
    }

}

fun main() {
    App().run()
}
