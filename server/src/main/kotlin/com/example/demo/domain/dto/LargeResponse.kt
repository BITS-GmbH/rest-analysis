package com.example.demo.domain.dto

data class LargeResponse(val keyValuePairs: List<KeyValuePair>) {

}

data class KeyValuePair(
    val key: String,
    val value: Long
)