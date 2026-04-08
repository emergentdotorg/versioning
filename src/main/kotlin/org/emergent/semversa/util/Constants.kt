package org.emergent.semversa.util

import java.util.regex.Pattern

object Constants {

    const val VERSION_REGEX_STRING =
        "^(refs/tags/)?(?<tag>v?(?<version>(?<major>[0-9]+)\\.(?<minor>[0-9]+)\\.(?<patch>[0-9]+)))$"

    val VERSION_REGEX: Pattern = Pattern.compile(VERSION_REGEX_STRING)

    const val RELEASE_BRANCH_REGEX_DEF = "^(main|master)$"

    const val TAG_NAME_REGEX_DEF: String = "v?((\\d+)(?:\\.(\\d+)(?:\\.(\\d+))?)?)$"

    const val VERSION_PATTERN_DEF = "%t(-%B)(-%C)(-%S)(+%H)(.%D)"

    const val LAST_TAG_PATTERN_DEF = "v?(?<major>\\d+)(?:\\.(?<minor>\\d+)(?:\\.(?<micro>\\d+))?)?$"

    const val TAG_VERSION_DEF: String = "0.0.0"

    const val HEAD: String = org.eclipse.jgit.lib.Constants.HEAD
}