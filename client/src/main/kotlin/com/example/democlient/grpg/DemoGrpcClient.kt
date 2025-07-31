package com.example.democlient.grpg

import com.example.democlient.rpc.Empty
import com.example.democlient.rpc.LargeResponse
import com.example.democlient.rpc.LargeResponseProviderGrpc
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

@Service
class DemoGrpcClient(val provider: LargeResponseProviderGrpc.LargeResponseProviderBlockingStub) {
    val logger = LoggerFactory.getLogger(DemoGrpcClient::class.java)

    @EventListener
    fun handleEvent(event: ApplicationReadyEvent) {
        val response = this.getLargeResponse()
        logger.info("Response content length: ${response.serializedSize} bytes")
        logger.info("Received response with ${response.keyValuePairsList.size} key-value pairs.")
    }

    fun getLargeResponse(): LargeResponse {
        return this.provider.getLargeResponse(Empty.newBuilder().build())
    }
}