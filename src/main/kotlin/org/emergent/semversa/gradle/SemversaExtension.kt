package org.emergent.semversa.gradle

import org.eclipse.jgit.api.Git
import org.emergent.semversa.Config
import org.emergent.semversa.VersionInfo
import org.emergent.semversa.VersionResolver
import org.emergent.semversa.git.GitExec
import org.emergent.semversa.git.GitUtil
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import java.io.File

abstract class SemversaExtension(project: Project) {

    @get:Input abstract val releaseBranchRegex: Property<String>
    @get:Input abstract val tagNameRegex: Property<String>
    @get:Input abstract val versionPattern: Property<String>

    /** The root git repository directory. */
    @get:Input abstract val repoRoot: DirectoryProperty

    /** A list of environment variables to read the branch name from. */
    @get:Input abstract val branchEnv: ListProperty<String>

    /** Pattern used to match when looking for the last tag. */
    @get:Input abstract val lastTagPattern: Property<String>

    val info: Provider<Info> = project.providers.provider {
        if (!repoRoot.dir(".git").get().asFile.isDirectory()) {
            Info.NONE
        } else {
            Info(full = resolver().version())
        }
    }

    fun getInfo(): VersionInfo {
        val strat: VersionResolver = resolver()
        val resolved = strat.resolved
        return VersionInfo(
            full = strat.version(),
            branch = resolved.branch,
            commit = resolved.hash,
            branchType = if (resolved.detached) "detached" else "branch",
            dirty = resolved.dirty,
        )
    }

    fun getVersion(): String {
        if (!repoRoot.dir(".git").get().asFile.exists()) {
            return "unspecified"
        }
        return resolver().version()
    }

    private fun resolver(): VersionResolver {
        return VersionResolver.getPatternStrategy(getConfig(), repoRoot.get().asFile)
    }

    fun getConfig(): Config {
        return Config(
            releaseBranchRegex = releaseBranchRegex.get(),
            tagNameRegex = tagNameRegex.get(),
            versionPattern = versionPattern.get()
        )
    }

    private fun getGitDir(): File {
        return repoRoot.get().asFile
    }

    fun getBaseTags(base: String): List<String> {
        return getLastTags("^${Regex.escape(base)}\\.(\\d+)$")
    }

    fun getLastTags(tagPattern: String): List<String> {
        val regex = Regex(tagPattern)
        // List all tags
        return applyOp { git ->
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

    fun <R : Any> applyOp(op: (Git) -> R): R = GitExec.applyOp(getGitDir(), op)
    fun acceptOp(op: (Git) -> Unit) = GitExec.acceptOp(getGitDir() , op)

    class Info(val scmExists: Boolean = true, val full: String) {
        companion object {
            @JvmField val NONE = Info(false, "unspecified")
        }
    }
}
