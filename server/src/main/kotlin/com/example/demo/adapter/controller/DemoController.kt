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