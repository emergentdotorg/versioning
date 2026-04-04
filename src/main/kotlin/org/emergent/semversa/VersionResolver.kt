package org.emergent.semversa

import com.github.zafarkhaja.semver.Version
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.submodule.SubmoduleWalk
import org.emergent.semversa.git.GitExec
import org.emergent.semversa.git.TagProvider
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.Pattern

class VersionResolver(
    val config: Config,
    val resolved: Resolved
) {

    fun version(): String = calculateVersion()

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

    private fun calculateVersion(): String {
        val values = getReplacementMap(config.releaseBranchRegex, resolved)
        return performTokenReplacements(config.versionPattern, values)
    }

    companion object {

        @Suppress("unused")
        @JvmStatic
        fun getPatternStrategy(config: Config, gitDir: File): VersionResolver {
            return GitExec.applyOp(gitDir) { git -> getPatternStrategy(config, git) }
        }

        @JvmStatic
        private fun getPatternStrategy(config: Config, git: Git): VersionResolver {
            val resolved = getResolved(config, git)
            return VersionResolver(config, resolved)
        }

        @JvmStatic
        private fun getResolved(config: Config, git: Git): Resolved {
            val repository = git.repository
            val headId = repository.resolve(Constants.HEAD)
                ?: throw IllegalStateException("headId is null")
            val fullBranch = repository.fullBranch.orEmpty()
            val detached = !fullBranch.startsWith("refs/")
            val branch = Repository.shortenRefName(fullBranch)

            var tagVersion: String? = null
            val tagProvider = TagProvider(config.tagNameRegex, git)
            var commits = 0
            for (commit in git.log().add(headId).call()) {
                if (commit.parentCount <= 1) {
                    val cmpVer: Version? = tagProvider.getTag(commit).orElse(null)
                    if (cmpVer != null) {
                        tagVersion = cmpVer.toString()
                        break
                    }
                }
                commits++
            }
            val status = git.status().setIgnoreSubmodules(SubmoduleWalk.IgnoreSubmoduleMode.UNTRACKED).call()

            val dirty = status.hasUncommittedChanges()
            return Resolved(
                gitDir = repository.directory.absolutePath,
                branch = branch,
                hash = headId.name,
                tagVersion = tagVersion ?: Resolved.TAG_VERSION_DEF,
                commits = commits,
                dirty = dirty,
                detached = detached
            )
        }
        private fun getReplacementMap(releaseBranchRegex: String, resolved: Resolved): Map<String, String> {
            val commits = resolved.commits
            val isDirty = resolved.dirty
            val hash = resolved.hash
            val hashShort = resolved.shortHash()
            val hasCommits = commits > 0
            val isDetached = resolved.detached
            val branch = if (isDetached) "detached" else resolved.branch
            val isReleaseBranch = branch.matches(Regex(releaseBranchRegex))
            val isRelease = isReleaseBranch && !hasCommits

            return PatternToken.entries.associate { token ->
                val replacement = when (token) {
                    PatternToken.TAG -> resolved.tagVersion
                    PatternToken.BRANCH -> branch
                    PatternToken.COMMIT -> commits.toString()
                    PatternToken.HASH_FULL -> hash
                    PatternToken.HASH_SHORT -> hashShort
                    PatternToken.SNAPSHOT -> if (hasCommits) "SNAPSHOT" else ""
                    PatternToken.COMMIT_OPT -> if (hasCommits) commits.toString() else ""
                    PatternToken.BRANCH_OPT -> if (isReleaseBranch) "" else branch
                    PatternToken.HASH_FULL_OPT -> if (isRelease) "" else hash
                    PatternToken.HASH_SHORT_OPT -> if (isRelease) "" else hashShort
                    PatternToken.DIRTY -> if (isDirty) "dirty" else ""
                }
                token.token to (replacement)
            }
        }

        private fun performTokenReplacements(versionPattern: String, codeReplMap: Map<String, String>): String {
            val codes = PatternToken.entries.joinToString("") { it.code }
            val tokenRegex = Regex("%[$codes]")
            val patternx = Pattern.compile(
                "(?<nakedToken>$tokenRegex)" +
                        "|\\(" +
                        "(?<groupPrefix>[^()%]+)?(?<groupToken>$tokenRegex)(?<groupSuffix>[^()%]+)?" +
                        "\\)"
            )

            val matcher = patternx.matcher(versionPattern)
            val priorEnd = AtomicInteger(-1)

            patternx.matcher(versionPattern).results().forEach {}

            var result = matcher.results().toList().flatMap { match ->
                val nakedToken = match.group(1)
                val groupPrefix = match.group(2)
                val groupToken = match.group(3)
                val groupSuffix = match.group(4)
                val replacements = mutableListOf<String>()

                if (!nakedToken.isNullOrBlank()) {
                    replacements.add(codeReplMap[nakedToken] ?: "")
                } else if (!groupToken.isNullOrBlank()) {
                    val repl = codeReplMap[groupToken].orEmpty()
                    if (repl.isNotEmpty()) {
                        listOfNotNull(groupPrefix, repl, groupSuffix).forEach { replacements.add(it) }
                    }
                }

                val priorMatchEnd = priorEnd.getAndUpdate { match.end() }
                if (priorMatchEnd > -1 && priorMatchEnd < match.start()) {
                    replacements.add(0, versionPattern.substring(priorMatchEnd, match.start()))
                }

                replacements
            }.joinToString("")

            val lastMatchEndIdx = priorEnd.get()
            if (lastMatchEndIdx in 0 until versionPattern.length) {
                result += versionPattern.substring(lastMatchEndIdx)
            }
            return result
        }
    }
}