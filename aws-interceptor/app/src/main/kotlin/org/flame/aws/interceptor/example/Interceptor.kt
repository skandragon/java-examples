package org.flame.aws.interceptor.example

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import software.amazon.awssdk.auth.credentials.AwsCredentials
import software.amazon.awssdk.core.interceptor.Context
import software.amazon.awssdk.core.interceptor.ExecutionAttribute
import software.amazon.awssdk.core.interceptor.ExecutionAttributes
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor
import software.amazon.awssdk.http.SdkHttpRequest
import software.amazon.awssdk.regions.Region
import java.util.*

@Serializable
data class AgentToken(@SerialName("iss") val issuer: String?)

class Interceptor : ExecutionInterceptor {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun modifyHttpRequest(
        context: Context.ModifyHttpRequest,
        executionAttributes: ExecutionAttributes
    ): SdkHttpRequest {
        val credentials = executionAttributes.getAttribute(ExecutionAttribute<AwsCredentials>("AwsCredentials"))
        
        if (credentials == null) {
            logger.debug("No credentials")
            return context.httpRequest()
        }

        // if it doesn't smell like a JWT, make no modifications.
        val fields = credentials.secretAccessKey().split(".")
        if (fields.size != 3) {
            logger.debug("Not a JWT")
            return context.httpRequest()
        }

        // If it isn't issued by us, make no modifications.
        try {
            val jsonString = Base64.getDecoder().decode(fields[1])
            val agentSpec = Json.decodeFromString<AgentToken>(String(jsonString))
            if (agentSpec.issuer == null || agentSpec.issuer != "opsmx") {
                logger.debug("Not a JWT issued by opsmx")
                return context.httpRequest()
            }
        } catch(e: IllegalArgumentException) {
            logger.debug("JWT base64 decode error or json parse error")
            return context.httpRequest()
        }

        val signingRegion = executionAttributes.getAttribute(ExecutionAttribute<Region>("SigningRegion"))
        val serviceSigningName = executionAttributes.getAttribute(ExecutionAttribute<String>("ServiceSigningName"))

        return context.httpRequest().copy {
            it.putHeader("x-opsmx-original-host", it.host())
            it.putHeader("x-opsmx-original-port", it.port().toString())
            it.putHeader("x-opsmx-signing-region", signingRegion.toString())
            it.putHeader("x-opsmx-service-signing-name", serviceSigningName)
            it.putHeader("X-Opsmx-Token", credentials.secretAccessKey())

            it.host("localhost")
            it.port(5000)
        }
    }
}
