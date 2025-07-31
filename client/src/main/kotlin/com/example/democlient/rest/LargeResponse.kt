package com.example.democlient.rest

data class LargeResponse(val keyValuePairs: List<KeyValuePair>) {

}

data class KeyValuePair(
    val key: String,
    val value: Long
)