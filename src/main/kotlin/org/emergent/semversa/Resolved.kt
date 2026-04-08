package org.emergent.semversa

import org.emergent.semversa.util.Constants
import org.emergent.semversa.util.Util



data class Resolved(
    val gitDir: String,
    val branch: String,
    val detached : Boolean,
    val hash: String = "",
    val tagVersion: String = Constants.TAG_VERSION_DEF,
    val commits: Int = 0,
    val dirty: Boolean = false,
) {
    val shortHash: String = Util.toShortHash(hash)

    companion object {
        @JvmField val NONE = Resolved("", "", true)
    }
}