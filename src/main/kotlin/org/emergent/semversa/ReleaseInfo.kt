package org.emergent.semversa

data class ReleaseInfo(
    val type: String,
    val base: String,
) {
    companion object {

        @JvmStatic
        fun new(type: String, base: String): ReleaseInfo {
            return ReleaseInfo(type = type, base = base)
        }

        @JvmStatic
        fun new(scmInfo: SCMInfo, separator: String = "/"): ReleaseInfo {
            if (scmInfo.detached) {
                return ReleaseInfo(type = "detached", base = scmInfo.tag.orEmpty())
            }
            val parts = scmInfo.branch.split(separator, limit = 2)
            val type = parts[0]
            val base = if (parts.size > 1) parts[1] else ""
            return ReleaseInfo(type = type, base = base)
        }
    }
}
