package com.puetsnao.logviewer

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class LogviewerApplication

fun main(args: Array<String>) {
	runApplication<LogviewerApplication>(*args)
}
