package org.emergent.semversa

enum class PatternToken(val code: String) {
    TAG("t"),
    COMMIT("c"),
    COMMIT_OPT("C"),
    BRANCH("b"),
    BRANCH_OPT("B"),
    HASH_SHORT("h"),
    HASH_SHORT_OPT("H"),
    HASH_FULL("f"),
    HASH_FULL_OPT("F"),
    SNAPSHOT("S"),
    DIRTY("D");

    val token: String
        get() = "%$code"

    override fun toString(): String = token
}