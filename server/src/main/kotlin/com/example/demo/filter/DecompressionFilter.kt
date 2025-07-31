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
package com.example.demo.filter

import jakarta.servlet.*
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import java.io.IOException
import java.util.zip.DeflaterInputStream
import java.util.zip.GZIPInputStream

@Component
class DecompressionFilter: Filter {

    override fun doFilter(
        request: ServletRequest?,
        response: ServletResponse?,
        filterChain: FilterChain?
    ) {
        val req: HttpServletRequest = request as HttpServletRequest
        filterChain?.doFilter(DecompressionWrapper(req, req.getHeader(HttpHeaders.CONTENT_ENCODING) ?: ""), response)
    }
}

class DecompressionWrapper(request: HttpServletRequest, val contentEncoding: String): HttpServletRequestWrapper(request) {
    enum class ContentEncoding(val value: String) {
        GZIP("gzip"), DEFLATE("deflate")
    }
    val sourceStream = if(contentEncoding == ContentEncoding.GZIP.value)  GZIPInputStream(request.inputStream) else if(contentEncoding == ContentEncoding.DEFLATE.value) DeflaterInputStream(request.inputStream) else request.inputStream
    private var finished = false

    override fun getInputStream(): ServletInputStream {
        val decompressionObject =  object : ServletInputStream() {

            @Throws(IOException::class)
            override fun read(): Int {
                val data: Int = sourceStream.read()
                if (data == -1) {
                    finished = true
                }

                return data
            }

            @Throws(IOException::class)
            override fun available(): Int {
                return sourceStream.available()
            }

            @Throws(IOException::class)
            override fun close() {
                super.close()
                sourceStream.close()
            }

            override fun isFinished(): Boolean {
                return finished
            }

            override fun isReady(): Boolean {
                return true
            }

            override fun setReadListener(readListener: ReadListener) {
                throw UnsupportedOperationException()
            }
        }
        return if( contentEncoding == ContentEncoding.GZIP.value || contentEncoding == ContentEncoding.DEFLATE.value) {
            decompressionObject
        } else {
            request.inputStream
        }
    }
}