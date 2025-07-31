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
package com.example.demo.adapter.controller

import com.example.demo.domain.dto.LargeResponse
import com.example.demo.usecase.service.ResponseService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/rest/demo")
class DemoController(val responseService: ResponseService) {
    val logger = LoggerFactory.getLogger(DemoController::class.java)

    @GetMapping("/large-response")
    fun getLargeResponse(@RequestHeader(HttpHeaders.ACCEPT_ENCODING) acceptEncoding: String): LargeResponse {
        //logger.info("Content-Encoding: $acceptEncoding")
        return responseService.createLargeResponse()
    }

    @PostMapping("/large-response")
    fun postLargeResponse(@RequestHeader(HttpHeaders.CONTENT_ENCODING) contentEncoding: String, @RequestBody largeResponse: LargeResponse? ): LargeResponse {
        return responseService.createLargeResponse(largeResponse)
    }
}