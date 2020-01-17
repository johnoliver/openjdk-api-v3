package net.adoptopenjdk.api

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

object TestTime {
    fun now(): LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
}