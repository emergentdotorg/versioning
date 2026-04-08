package org.emergent.semversa.git

import org.eclipse.jgit.api.Git
import org.emergent.semversa.TestUtils
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.time.ZonedDateTime

class GitTestRepo @JvmOverloads constructor(val dir: File = createTempDir("git", "")) : Closeable {

    init {
        Git.init().setDirectory(dir).call()
    }

    override fun toString(): String {
        return dir.toString()
    }

    override fun close() {
        deleteDir(dir)
    }

    fun commit(no: Int?) {
        GitExec.acceptOp(dir) { git ->
            try {
                val fileName = "file${no}"
                TestUtils.setText(File(dir, fileName), "Text for commit $no")
                git.add().addFilepattern(fileName).call()
                git.commit().setMessage("Commit $no").setAll(true).call()
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }
    }

    fun add(vararg paths: String?) {
        GitExec.acceptOp(dir) { git -> git.add().addFilepatterns(*paths).call() }
    }

    fun branch(name: String?) {
        GitExec.acceptOp(dir) { git -> git.checkout().setName(name).setCreateBranch(true).call() }
    }

    fun checkout(name: String?) {
        GitExec.acceptOp(dir) { git -> git.checkout().setName(name).call() }
    }

    fun tag(name: String?) {
        GitExec.acceptOp(dir) { git -> git.tag().setName(name).call() }
    }

    //@JvmOverloads
    fun commitLookup(message: String, abbreviated: Boolean = false): String {
        return GitExec.applyOp(dir) { git ->
            val commit = git.log().call()
                .map { GitUtil.convertCommit(git.repository, it) }
                .firstOrNull { it: Commit? -> it!!.fullMessage.contains(message) }
            if (commit == null) {
                throw RuntimeException("Cannot find commit for message $message")
            }
            return@applyOp if (abbreviated) commit.abbreviatedId else commit.id
        }
    }

    fun dateTimeLookup(commitId: String?): ZonedDateTime? {
        return GitExec.applyOp(dir) { git ->
            val commit = git.log().call()
                .map { GitUtil.convertCommit(git.repository, it) }
                .firstOrNull { it: Commit? -> it?.id == commitId }
            if (commit == null) {
                throw RuntimeException("Cannot find commit for ID $commitId")
            }
            return@applyOp commit.dateTime
        }
    }

    companion object {
        @JvmStatic
        fun createTempDir(prefix: String?, suffix: String?): File {
            return java.nio.file.Files.createTempDirectory(prefix).toFile()
        }

        @JvmStatic
        fun deleteDir(self: File): Boolean {
            if (!self.exists()) {
                return true
            } else if (!self.isDirectory()) {
                return false
            } else {
                val files = self.listFiles()
                if (files == null) {
                    return false
                } else {
                    var result = true
                    for (file in files) {
                        if (file.isDirectory()) {
                            if (!deleteDir(file)) {
                                result = false
                            }
                        } else if (!file.delete()) {
                            result = false
                        }
                    }
                    if (!self.delete()) {
                        result = false
                    }
                    return result
                }
            }
        }

    }
}
