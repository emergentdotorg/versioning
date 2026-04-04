package org.emergent.semversa

import java.time.ZonedDateTime

class SCMInfo(
    val branch: String = "",
    val commit: String = "",
    val abbreviated: String = "",
    val dateTime: ZonedDateTime? = null,
    val tag: String? = null,
    val lastTag: String? = null,
    val dirty: Boolean = false,
    val shallow: Boolean = false,
    val scm: String? = null,
    val detached: Boolean = false,
) {
    companion object {
        @JvmField val NONE = SCMInfo()
    }

    fun separator(): String {
        return if ("svn" == scm) {
            "-"
        } else {
            "/"
        }
    }
}
