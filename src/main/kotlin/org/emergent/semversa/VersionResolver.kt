package org.emergent.semversa

import com.github.zafarkhaja.semver.Version
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.submodule.SubmoduleWalk
import org.emergent.semversa.git.GitRepo
import org.emergent.semversa.git.GitUtil
import org.emergent.semversa.git.TagProvider
import org.emergent.semversa.util.Constants
import java.io.File
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.Pattern

class VersionResolver(val config: Config, private val repo: GitRepo) {
    constructor(config: Config, basedir: Path) : this(config, GitRepo(basedir))

    constructor(config: Config, basedir: File) : this(config, GitRepo(basedir))

    fun versionInfo(): VersionInfo {
        //if (!repo.exists()) return VersionInfo.NONE
        val resolved = resolved()
        if (resolved == Resolved.NONE) return VersionInfo.NONE
        return info(config, resolved)
    }

    fun version(): String = versionInfo().full

    fun resolved(): Resolved {
        return if (!repo.exists()) Resolved.NONE else
            repo.apply { getResolved(config, it) }
    }

    @Suppress("unused")
    private fun getBaseTags(base: String): List<String> {
        return getLastTags("^${Regex.escape(base)}\\.(\\d+)$")
    }

    private fun getLastTags(tagPattern: String): List<String> {
        val regex = Regex(tagPattern)
        return repo.apply { git ->
            (git.tagList().call() ?: emptyList()).asSequence()
                .map { GitUtil.resolveTag(git.repository, it) }
                .filter { it != null && regex.containsMatchIn(it.name) }
                .map { it!! }
                .sortedWith(Comparator { a, b ->
                    // ... sort by desc commit time
                    val timeCompare = (b.commit.timestamp).compareTo(a.commit.timestamp)
                    if (timeCompare != 0) timeCompare
                    else GitUtil.tagOrder(tagPattern).reversed().compare(a.name, b.name)
                })
                .map { it.name }
                .toList()
        }
    }

    companion object {

        @JvmStatic
        fun info(config: Config, resolved: Resolved): VersionInfo {
            val full = getFull(config, resolved)
            return VersionInfo(full, resolved)
        }

        @JvmStatic
        fun getFull(config: Config, resolved: Resolved): String {
            val values = getReplacementMap(config.releaseBranchRegex, resolved)
            return performTokenReplacements(config.versionPattern, values)
        }

        @JvmStatic
        fun getResolved(config: Config, git: Git): Resolved {
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
                tagVersion = tagVersion ?: Constants.TAG_VERSION_DEF,
                commits = commits,
                dirty = dirty,
                detached = detached
            )
        }


        @JvmStatic
        private fun getReplacementMap(releaseBranchRegex: String, resolved: Resolved): Map<String, String> {
            val commits = resolved.commits
            val isDirty = resolved.dirty
            val hash = resolved.hash
            val hashShort = resolved.shortHash
            val hasCommits = commits > 0
            val isDetached = resolved.detached
            val branch = if (isDetached) "detached" else resolved.branch
            val isReleaseBranch = branch.matches(Regex(releaseBranchRegex))
            val isRelease = isReleaseBranch && !hasCommits && !isDirty
            val isSnapshot = isReleaseBranch && !isRelease
            val tagVersion = resolved.tagVersion

            val version = if (isSnapshot) {
                val result = "^v?((\\d+)(?:\\.(\\d+)(?:\\.(\\d+))?)?)$".toRegex().matchEntire(tagVersion)?.groups
                val micro = "${result?.get(4)?.value}"
                val microInt = 1 + (micro.toIntOrNull() ?: 0)
                "${result?.get(2)?.value}.${result?.get(3)?.value}.${microInt}"
            } else {
                tagVersion
            }

            return PatternToken.entries.associate { token ->
                val replacement = when (token) {
                    PatternToken.TAG -> version
                    PatternToken.BRANCH -> branch
                    PatternToken.COMMIT -> commits.toString()
                    PatternToken.HASH_FULL -> hash
                    PatternToken.HASH_SHORT -> hashShort
                    PatternToken.SNAPSHOT -> if (hasCommits || isDirty) "SNAPSHOT" else ""
                    PatternToken.COMMIT_OPT -> if (hasCommits && !isSnapshot) commits.toString() else ""
                    PatternToken.BRANCH_OPT -> if (isReleaseBranch) "" else branch
                    PatternToken.HASH_FULL_OPT -> if (isRelease || isSnapshot) "" else hash
                    PatternToken.HASH_SHORT_OPT -> if (isRelease || isSnapshot) "" else hashShort
                    PatternToken.DIRTY -> if (isDirty && !isSnapshot) "dirty" else ""
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