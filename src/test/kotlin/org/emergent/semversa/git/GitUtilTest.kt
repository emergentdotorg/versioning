package org.emergent.semversa.git

import org.assertj.core.api.Assertions.assertThat
import org.emergent.semversa.TestUtils.Companion.leftShift
import org.emergent.semversa.TestUtils.Companion.setText
import org.emergent.semversa.git.GitConstants.Companion.getStatus
import org.emergent.semversa.git.GitConstants.Companion.isGitTreeDirty
import org.junit.jupiter.api.Test
import java.io.File

class GitUtilTest {

    @Test
    fun git___clean() {
        GitRepo().use { repo ->
            repo.commit(1)
            val status = getStatus(repo.dir)
            assertThat(isGitTreeDirty(status)).isFalse.withFailMessage("Git tree clean")
        }
    }

    @Test
    fun git___unstaged() {
        GitRepo().use { repo ->
            repo.commit(1)
            // Need to modify a tracked file, not just create a new untracked file
            leftShift(File(repo.dir, "file1"), "Add some content")
            val status = getStatus(repo.dir)
            assertThat(isGitTreeDirty(status)).isTrue.withFailMessage { "Unstaged changes" }
        }
    }

    @Test
    fun git___uncommitted() {
        GitRepo().use { repo ->
            repo.commit(1)
            // Add a file, without committing it
            setText(File(repo.dir, "test.txt"), "Test")
            repo.add("test.txt")
            val status = getStatus(repo.dir)
            assertThat(isGitTreeDirty(status)).isTrue.withFailMessage { "Uncommitted changes" }
        }
    }
}
