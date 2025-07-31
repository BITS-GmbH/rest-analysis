package com.example.democlient.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.DeflaterInputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.random.Random

@Service
class DemoRestClient {
    val logger = LoggerFactory.getLogger(DemoRestClient::class.java)
    val objectMapper = ObjectMapper().registerKotlinModule().registerModule(JavaTimeModule())
    val restClient = RestClient.create();

    @EventListener
    fun handleEvent(event: ApplicationReadyEvent) {
        val response = this.getLargeResponse()
        logger.info("Received get response with ${response?.keyValuePairs?.size} key-value pairs.")
        val postResponse = this.postLargeResponse()
        logger.info("Received post response with ${postResponse?.keyValuePairs?.size} key-value pairs.")
    }

    fun getLargeResponse(): LargeResponse? {
        val response = restClient.get().uri("http://localhost:8080/rest/demo/large-response")
            .header(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate")
            .exchange { request, response ->
                    //logger.info(response.headers.get(HttpHeaders.CONTENT_ENCODING)?.get(0)?.let { "Content-Encoding: $it" } ?: "No Content-Encoding header found")
                handleEncodedResponse(response)
            }
        return response
    }

    fun postLargeResponse(): LargeResponse? {
        val outputStream = ByteArrayOutputStream()
        GZIPOutputStream(outputStream).use { gzipOutputStream ->
            val json = this.objectMapper.writeValueAsString(this.createLargeResponse());
            gzipOutputStream.write(json.toByteArray(), 0, json.toByteArray().size)
            gzipOutputStream.close()
        }
        val content = outputStream.toByteArray()
        logger.info("Sending post request with content length: ${content.size} bytes")
        val response = restClient.post().uri("http://localhost:8080/rest/demo/large-response")
            .header(HttpHeaders.CONTENT_ENCODING, "gzip")
            .header(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate")
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .body(content)
            .exchange { request, response ->
                handleEncodedResponse(response)
            }
        return response
    }

    private fun createLargeResponse(): LargeResponse {
        val charPool : List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        val keyValuePairs = (1..100000).map { index ->
            val key = (1..20)
                .map { Random.nextInt(0, charPool.size).let { charPool[it] } }
                .joinToString("")
            KeyValuePair(key, Random.nextLong(0, 1000000000000000))
        }
        return LargeResponse(keyValuePairs)
    }

    private fun handleEncodedResponse(response: RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse): LargeResponse? {
        var result: LargeResponse? = null
        val responseContent = response.body.readAllBytes()
        logger.info("Response content length: ${responseContent.size} bytes")
        if (response.headers.get(HttpHeaders.CONTENT_ENCODING)?.contains("gzip") == true) {
            result = this.objectMapper.readValue(GZIPInputStream(ByteArrayInputStream( responseContent)), LargeResponse::class.java)
        } else if (response.headers.get(HttpHeaders.CONTENT_ENCODING)?.contains("deflate") == true) {
            result = this.objectMapper.readValue(DeflaterInputStream(ByteArrayInputStream( responseContent)), LargeResponse::class.java)
        } else if (response.headers.get(HttpHeaders.CONTENT_ENCODING) == null || response.headers.get(HttpHeaders.CONTENT_ENCODING)
                ?.isEmpty() == true
        ) {
            result = this.objectMapper.readValue(responseContent, LargeResponse::class.java)
        } else {
            throw IllegalStateException("Unsupported content encoding: ${response.headers.get(HttpHeaders.CONTENT_ENCODING)}")
        }
        return result
    }
}