package org.emergent.semversa

import org.emergent.semversa.util.Util

class Resolved(
    val gitDir: String,
    val branch: String,
    val detached : Boolean,
    val hash: String = "",
    val tagVersion: String = TAG_VERSION_DEF,
    val commits: Int,
    val dirty: Boolean = false
) {
    fun shortHash(): String {
        return Util.toShortHash(hash)
    }

    companion object {
        const val TAG_VERSION_DEF: String = "0.0.0"
    }
}