package org.emergent.semversa.git

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.internal.JGitText
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.lib.RepositoryCache
import org.eclipse.jgit.util.FS
import org.eclipse.jgit.util.IO
import org.eclipse.jgit.util.RawParseUtils
import org.eclipse.jgit.util.SystemReader
import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.text.MessageFormat

class GitRepo(val basePath: File) {

    constructor(basePath: Path) : this(basePath.toFile())
    constructor(basePath: String) : this(File(basePath))

    fun <R> apply(work: (Git) -> R): R {
        return git().use(work)
    }

    fun accept(work: (Git) -> Unit) {
        git().use(work)
    }

    fun exists(): Boolean {
        val dir = findGitDir(null)
        return dir != null
    }

    private fun git(): Git = Git(repository())

    private fun repository(mustExist: Boolean = true): Repository = GitUtil.getRepository(basePath, mustExist)

    fun findGitDir(ceilingDirectories : List<File>?): File? {
        var current: File? = basePath.absoluteFile
        val tryFS: FS = FS.DETECTED
        while (current != null) {
            val dir: File = File(current, Constants.DOT_GIT)
            if (RepositoryCache.FileKey.isGitRepository(dir, tryFS)) {
                return dir
            } else if (dir.isFile()) {
                try {
                    return getSymRef(current, dir, tryFS)
                } catch (ignored: IOException) {
                    // Continue searching if gitdir ref isn't found
                }
            } else if (RepositoryCache.FileKey.isGitRepository(current, tryFS)) {
                return current
            }

            current = current.getParentFile()
            if (current != null && ceilingDirectories != null && ceilingDirectories.contains(current)){
                break
            }
        }
        return current
    }

    @Throws(IOException::class)
    fun getSymRef(workTree: File?, dotGit: File, fs: FS): File {
        val content = IO.readFully(dotGit)
        if (!isSymRef(content)) {
            throw IOException(
                MessageFormat.format(
                    JGitText.get().invalidGitdirRef, dotGit.getAbsolutePath()
                )
            )
        }

        val pathStart = 8
        var lineEnd = RawParseUtils.nextLF(content, pathStart)
        while (content[lineEnd - 1] == '\n'.code.toByte() ||
            (content[lineEnd - 1] == '\r'.code.toByte()
                    && SystemReader.getInstance().isWindows())
        ) {
            lineEnd--
        }
        if (lineEnd == pathStart) {
            throw IOException(
                MessageFormat.format(
                    JGitText.get().invalidGitdirRef, dotGit.getAbsolutePath()
                )
            )
        }

        val gitdirPath = RawParseUtils.decode(content, pathStart, lineEnd)
        val gitdirFile = fs.resolve(workTree, gitdirPath)
        if (gitdirFile.isAbsolute()) {
            return gitdirFile
        }
        return File(workTree, gitdirPath).getCanonicalFile()
    }

    private fun isSymRef(ref: ByteArray): Boolean {
        if (ref.size < 9) return false
        return  /**/ref[0] == 'g'.code.toByte() //
                && ref[1] == 'i'.code.toByte() //
                && ref[2] == 't'.code.toByte() //
                && ref[3] == 'd'.code.toByte() //
                && ref[4] == 'i'.code.toByte() //
                && ref[5] == 'r'.code.toByte() //
                && ref[6] == ':'.code.toByte() //
                && ref[7] == ' '.code.toByte()
    }

}