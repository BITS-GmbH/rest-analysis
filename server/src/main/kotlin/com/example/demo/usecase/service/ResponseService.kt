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
package com.example.demo.usecase.service

import com.example.demo.domain.dto.KeyValuePair
import com.example.demo.domain.dto.LargeResponse
import org.apache.catalina.connector.Connector
import org.apache.coyote.http2.Http2Protocol
import org.slf4j.LoggerFactory
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service
import kotlin.random.Random


@Service
class ResponseService {
    val logger = LoggerFactory.getLogger(ResponseService::class.java)

    fun createLargeResponse(largeResponse: LargeResponse?): LargeResponse {
        logger.info("Received response with ${largeResponse?.keyValuePairs?.size} key-value pairs.")
        return this.createLargeResponse()
    }

    fun createLargeResponse(): LargeResponse {
        val charPool : List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        val keyValuePairs = (1..100000).map { index ->
            val key = (1..20)
                .map { Random.nextInt(0, charPool.size).let { charPool[it] } }
                .joinToString("")
            KeyValuePair(key, Random.nextLong(0, 1000000000000000))
        }
        return LargeResponse(keyValuePairs)
    }

    @Bean
    fun customizer(): TomcatConnectorCustomizer {
        return TomcatConnectorCustomizer { connector: Connector? ->
            for (protocol in connector!!.findUpgradeProtocols()) {
                if (protocol is Http2Protocol) {
                    protocol.setOverheadWindowUpdateThreshold(0)
                }
            }
        }
    }
}