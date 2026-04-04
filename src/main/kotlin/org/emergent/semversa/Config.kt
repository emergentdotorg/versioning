package org.emergent.semversa

import org.emergent.semversa.util.Constants

class Config(
    val releaseBranchRegex : String = Constants.RELEASE_BRANCH_REGEX_DEF,
    val tagNameRegex : String = Constants.TAG_NAME_REGEX_DEF,
    val versionPattern : String = Constants.VERSION_PATTERN_DEF
)