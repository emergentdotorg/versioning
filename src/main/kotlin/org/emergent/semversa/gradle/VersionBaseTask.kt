package org.emergent.semversa.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import java.io.File

abstract class VersionBaseTask : DefaultTask() {

    init {
        group = "Versioning"
    }

    /**
     * A cached instance for use in Gradle 9+ where calling super.getProject() while the task is running is deprecated.
     */
    private val proj: Project = super.getProject()

    override fun getProject(): Project {
        return proj
    }

    companion object {
        @JvmStatic
        fun getVerifiedParent(file: File): File {
            val parent = file.parentFile
            if (parent != null && !parent.exists()) {
                parent.mkdirs()
            }
            return parent
        }
    }
}
