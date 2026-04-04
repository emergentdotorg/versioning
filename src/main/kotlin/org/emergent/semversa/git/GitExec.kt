package org.emergent.semversa.git

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.File
import java.io.IOException

object GitExec {

    fun <R> applyOp(basePath: File, work: (Git) -> R): R {
        return try {
            getRepository(basePath).use { repo ->
                Git(repo).use { git ->
                    work(git)
                }
            }
        } catch (e: RuntimeException) {
            throw e
        } catch (e: Error) {
            throw e
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    fun acceptOp(basePath: File, work: (Git) -> Unit) {
        try {
            getRepository(basePath).use { repo ->
                Git(repo).use { git ->
                    work(git)
                }
            }
        } catch (e: RuntimeException) {
            throw e
        } catch (e: Error) {
            throw e
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    fun findGitDir(basePath: File): String {
        return try {
            getRepository(basePath, true).use { repo ->
                repo.directory.absolutePath
            }
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    @Throws(IOException::class)
    fun getRepository(basePath: File, mustExist: Boolean = false): Repository {
        return FileRepositoryBuilder()
            .readEnvironment()
            .findGitDir(normalize(basePath))
            .setMustExist(mustExist)
            .build()
    }

    private fun normalize(file: File?): File? {
        return file?.absoluteFile
    }
}