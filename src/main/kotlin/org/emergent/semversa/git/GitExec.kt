package org.emergent.semversa.git

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import java.io.File
import java.io.IOException

object GitExec {

    fun <R> applyOp(basePath: File, work: (Git) -> R): R {
        return getRepository(basePath).use { repo ->
            Git(repo).use { git ->
                work(git)
            }
        }
    }

    fun acceptOp(basePath: File, work: (Git) -> Unit) {
        getRepository(basePath).use { repo ->
            Git(repo).use { git ->
                work(git)
            }
        }
    }

    @Throws(IOException::class)
    fun getRepository(basePath: File, mustExist: Boolean = false): Repository {
        return GitUtil.getRepository(basePath, mustExist)
    }
}