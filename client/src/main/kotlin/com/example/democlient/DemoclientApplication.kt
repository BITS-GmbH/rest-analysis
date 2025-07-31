package com.example.democlient

import com.example.democlient.rest.DemoRestClient
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.runApplication
import org.springframework.context.event.EventListener

@SpringBootApplication
class DemoclientApplication

	fun main(args: Array<String>) {
		runApplication<DemoclientApplication>(*args)
	}
