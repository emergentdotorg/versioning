package org.emergent.semversa.git

import org.apache.commons.io.FileUtils
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.GitAPIException
import org.emergent.semversa.gradle.SemversaExtension
import org.emergent.semversa.gradle.SemversaPlugin
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import java.io.IOException
import java.nio.file.Files
import java.time.format.DateTimeFormatter

open class GitVersionTest {

    open fun configure(extension: SemversaExtension) {
        extension.versionPattern.set("%t(-%B)(-%C)(-%S)(.%D)")
    }

    @Test
    @Throws(IOException::class)
    fun gitNotPresent() {
        val wd = Files.createTempDirectory("git").toFile()
        val project = ProjectBuilder.builder().withProjectDir(wd).build()
        SemversaPlugin().apply(project)
        // val extension = project.extensions.create("versioning", VersioningExtension::class.java,
        // project)
        val extension = project.extensions.getByName("semversa") as SemversaExtension
        configure(extension)
        val info = extension.info.get()
        assertThat(info).isNotNull()
        assertThat(info.full).isEqualTo("unspecified")
        // Assert.assertNotNull(info)
        // Assert.assertEquals(VersionInfo.NONE, info)
        // Assert.assertEquals("", info.build)
        // Assert.assertEquals("", info.branch)
        // Assert.assertEquals("", info.base)
        // Assert.assertEquals("", info.branchId)
        // Assert.assertEquals("", info.branchType)
        // Assert.assertEquals("", info.commit)
        // Assert.assertEquals("", info.display)
        // Assert.assertEquals("", info.full)
        // Assert.assertEquals("n/a", info.scm)
        // Assert.assertNull(info.tag)
        // Assert.assertFalse(info.dirty)
        // Assert.assertNull(info.versionNumber)
        // Assert.assertNull(info.time)
    }

    @Test
    @Throws(IOException::class)
    fun gitMaster() {
        GitTestRepo().use { repo ->
            // Git initialisation
            for (i in 1..4) {
                repo.commit(i)
            }
            val head = repo.commitLookup("Commit 4")
            val headAbbreviated = repo.commitLookup("Commit 4", true)
            val time: String = getCommitTime(repo, head)

            val project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            SemversaPlugin().apply(project)
            val extension = project.extensions.getByName("semversa") as SemversaExtension
            configure(extension)
            val info = extension.info.get()
            assertThat(info).isNotNull()
            assertThat(info.full).isEqualTo("0.0.1-SNAPSHOT")
            // assertEquals("0.0.0-4-SNAPSHOT+f46aedde", extension.getVersion())

            // val info = extension.getInfo()
            // Assert.assertNotNull(info)
            // Assert.assertEquals(headAbbreviated, info.build)
            // Assert.assertEquals("main", info.branch)
            // Assert.assertEquals("", info.base)
            // Assert.assertEquals("main", info.branchId)
            // Assert.assertEquals("main", info.branchType)
            // Assert.assertEquals(head, info.commit)
            // Assert.assertEquals("main-" + headAbbreviated, info.display)
            // Assert.assertEquals("main-" + headAbbreviated, info.full)
            // Assert.assertEquals("git", info.scm)
            // Assert.assertNull(info.tag)
            // Assert.assertFalse(info.dirty)
            // Assert.assertNotNull(info.versionNumber)
            // Assert.assertEquals(0, info.versionNumber!!.versionCode.toLong())
            // Assert.assertEquals(time, info.time)
        }
    }

    @Test
    @Throws(IOException::class)
    fun gitDevel() {
        GitTestRepo().use { repo ->
            // Git initialisation
            for (i in 1..4) {
                repo.commit(i)
            }
            repo.branch("devel")
            val head = repo.commitLookup("Commit 4")
            val headAbbreviated = repo.commitLookup("Commit 4", true)
            val time: String = getCommitTime(repo, head)

            val project = ProjectBuilder.builder().withProjectDir(repo.dir).build()
            SemversaPlugin().apply(project)
            val extension = project.extensions.getByName("semversa") as SemversaExtension
            configure(extension)
            val info = extension.info.get()
            assertThat(info).isNotNull()
            assertThat(info.full).isEqualTo("0.0.0-devel-4-SNAPSHOT")
            // assertEquals("0.0.0-4-SNAPSHOT+f46aedde", extension.getVersion())

            // val info = extension.getInfo()
            // Assert.assertNotNull(info)
            // Assert.assertEquals(headAbbreviated, info.build)
            // Assert.assertEquals("main", info.branch)
            // Assert.assertEquals("", info.base)
            // Assert.assertEquals("main", info.branchId)
            // Assert.assertEquals("main", info.branchType)
            // Assert.assertEquals(head, info.commit)
            // Assert.assertEquals("main-" + headAbbreviated, info.display)
            // Assert.assertEquals("main-" + headAbbreviated, info.full)
            // Assert.assertEquals("git", info.scm)
            // Assert.assertNull(info.tag)
            // Assert.assertFalse(info.dirty)
            // Assert.assertNotNull(info.versionNumber)
            // Assert.assertEquals(0, info.versionNumber!!.versionCode.toLong())
            // Assert.assertEquals(time, info.time)
        }
    }

    @Test
    @Throws(IOException::class, GitAPIException::class)
    fun gitDetachedHEAD() {
        GitTestRepo().use { repo ->
            // Git initialisation
            for (i in 1..5) {
                repo.commit(i)
            }
            val commit3 = repo.commitLookup("Commit 3")
            val commit3Abbreviated = repo.commitLookup("Commit 3", true)
            val commit3Time: String = getCommitTime(repo, commit3)

            // Creates a temporary directory where to perform a detached clone operation
            val detached = Files.createTempDirectory("git").toFile()
            try {
                // Cloning
                val git =
                    Git.cloneRepository()
                        .setURI(repo.dir.toURI().toString())
                        .setDirectory(detached)
                        .call()
                // Detached HEAD
                git.checkout()
                    .setName(commit3)
                    .setStartPoint("HEADˆ") // .setCreateBranch(false)
                    .call()

                val fullBranch = git.getRepository().getFullBranch()

                val project = ProjectBuilder.builder().withProjectDir(detached).build()
                SemversaPlugin().apply(project)
                val extension = project.extensions.getByName("semversa") as SemversaExtension
                configure(extension)
                val info = extension.info.get()
                assertThat(info).isNotNull()
                assertThat(info.full).isEqualTo("0.0.0-detached-3-SNAPSHOT")
                // val info = extension.getInfo()
                // Assert.assertNotNull(info)
                // Assert.assertEquals(commit3Abbreviated, info.build)
                // Assert.assertEquals("HEAD", info.branch)
                // Assert.assertEquals("", info.base)
                // Assert.assertEquals("HEAD", info.branchId)
                // Assert.assertEquals("detached", info.branchType)
                // Assert.assertEquals(commit3, info.commit)
                // Assert.assertEquals("detached-" + commit3Abbreviated, info.display)
                // Assert.assertEquals("detached-" + commit3Abbreviated, info.full)
                // Assert.assertEquals("git", info.scm)
                // Assert.assertNull(info.tag)
                // Assert.assertFalse(info.dirty)
                // Assert.assertNotNull(info.versionNumber)
                // Assert.assertEquals(0, info.versionNumber!!.versionCode.toLong())
                // Assert.assertEquals(commit3Time, info.time)
            } finally {
                FileUtils.deleteDirectory(detached)
            }
        }
    }

    companion object {
        protected fun getCommitTime(repo: GitTestRepo, commitId: String?): String {
            return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(repo.dateTimeLookup(commitId))
        }
    }
}
