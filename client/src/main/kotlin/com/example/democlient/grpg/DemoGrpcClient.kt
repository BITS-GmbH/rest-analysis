/**
 *    Copyright 2025 Bits GmbH

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
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