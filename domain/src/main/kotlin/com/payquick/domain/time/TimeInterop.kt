package com.payquick.domain.time

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.Instant as JavaInstant
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Lightweight replacement for the deprecated kotlinx-datetime utilities.
 * Wraps the JDK ZoneId so existing platform code can keep using TimeZone abstractions.
 */
data class TimeZone internal constructor(val zoneId: ZoneId) {

    val id: String get() = zoneId.id

    companion object {
        fun of(id: String): TimeZone = TimeZone(ZoneId.of(id))
        fun of(zoneId: ZoneId): TimeZone = TimeZone(zoneId)
        fun currentSystemDefault(): TimeZone = TimeZone(ZoneId.systemDefault())
    }
}

fun TimeZone.toZoneId(): ZoneId = zoneId

@OptIn(ExperimentalTime::class)
fun Instant.toLocalDateTime(timeZone: TimeZone): LocalDateTime =
    LocalDateTime.ofInstant(JavaInstant.ofEpochMilli(toEpochMilliseconds()), timeZone.zoneId)

fun LocalDateTime.toJavaLocalDateTime(): LocalDateTime = this
