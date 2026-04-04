package org.emergent.semversa.git

import java.time.ZonedDateTime
import java.util.*

class Commit(
    val id: String = "",
    val abbreviatedId: String = "",
    val parentIds: List<String> = emptyList(),
    val author: Person? = null,
    val committer: Person? = null,
    val dateTime: ZonedDateTime? = null,
    val fullMessage: String = "",
    val shortMessage: String = ""
) {
    val timestamp: Long = dateTime?.toEpochSecond() ?: 0L

    @Deprecated("use Commit#dateTime")
    fun getTime(): Long {
        return dateTime?.toEpochSecond() ?: 0L
    }

    @Deprecated("use Commit#dateTime")
    fun getDate(): Date? {
        return dateTime?.toInstant()?.let { Date.from(it) }
    }
}