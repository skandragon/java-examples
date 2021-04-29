package org.flame.aws.interceptor.example

import software.amazon.awssdk.core.interceptor.Context
import software.amazon.awssdk.core.interceptor.ExecutionAttribute
import software.amazon.awssdk.core.interceptor.ExecutionAttributes
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor
import software.amazon.awssdk.http.SdkHttpRequest
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
        val bucketName = "opsmx-michael-test"
        val region = Region.US_EAST_2
        val s3 = S3Client.builder()
            .region(region)
            .overrideConfiguration {
                it.addExecutionInterceptor(Interceptor())
            }
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
                println("  The name of the key is " + it.key())
                println("  The object is " + it.size() + " bytes")
                println("  The owner is " + it.owner())

            }

        } catch (e: S3Exception) {
            println(e.awsErrorDetails().errorMessage());
            exitProcess(1);
        }
    }
}

class Interceptor : ExecutionInterceptor {
    override fun beforeTransmission(context: Context.BeforeTransmission, executionAttributes: ExecutionAttributes) {
        println("beforeTransmission:")
        println(context.httpRequest())
        println("attributes: ${executionAttributes.attributes}")
        context.httpRequest().headers().forEach { header ->
            println("  ${header.key} ->")
            header.value.forEach {
                println("    $it")
            }
        }
        println()
    }

    override fun modifyHttpRequest(context: Context.ModifyHttpRequest, executionAttributes: ExecutionAttributes): SdkHttpRequest {
        println("modifyHttpRequest:")
        println("body: ${context.requestBody()}")

        val signingRegion = executionAttributes.getAttribute(ExecutionAttribute<Region>("SigningRegion"))
        val serviceSigningName = executionAttributes.getAttribute(ExecutionAttribute<String>("ServiceSigningName"))

        val req = context.httpRequest().copy {
            it.putHeader("x-opsmx-original-host", it.host())
            it.putHeader("x-opsmx-original-port", it.port().toString())
            it.putHeader("x-opsmx-signing-region", signingRegion.toString())
            it.putHeader("x-opsmx-service-signing-name", serviceSigningName)

            it.host("localhost")
            it.port(5000)
        }
        println("Authorization header: ${context.requestBody()}")
        println("attributes: ${executionAttributes.attributes}")

        return req
    }
}

fun main() {
    App().run()
}
