package org.emergent.semversa.gradle

import org.emergent.semversa.util.Constants.LAST_TAG_PATTERN_DEF
import org.emergent.semversa.util.Constants.RELEASE_BRANCH_REGEX_DEF
import org.emergent.semversa.util.Constants.TAG_NAME_REGEX_DEF
import org.emergent.semversa.util.Constants.VERSION_PATTERN_DEF
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.internal.extensions.core.extra

class SemversaPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val ext = project.extensions.create("versioning", SemversaExtension::class.java, project)

        ext.repoRoot.convention(gitRootProvider(project))

        ext.releaseBranchRegex.convention(prov(project, "releaseBranchRegex", RELEASE_BRANCH_REGEX_DEF))
        ext.tagNameRegex.convention(prov(project, "tagNameRegex", TAG_NAME_REGEX_DEF))
        ext.versionPattern.convention(prov(project, "versionPattern", VERSION_PATTERN_DEF))
        ext.lastTagPattern.convention(prov(project, "lastTagPattern", LAST_TAG_PATTERN_DEF))

        project.tasks.register("versionDisplay", VersionDisplayTask::class.java)
        project.tasks.register("versionFile", VersionFileTask::class.java)

        val info = ext.info.get()
        if (info.scmExists && !project.extra.has("version")) {
            project.extra["version"] = info.full
        }
    }

    private fun gitRootProvider(project: Project): Provider<Directory> = project.providers.provider {
        var p: Project = project
        while (p.parent != null && !p.layout.projectDirectory.dir(".git").asFile.exists()) {
            p = p.parent!!
        }
        p.layout.projectDirectory
    }

    private fun prov(project: Project, name: String, def: String): Provider<String> {
        return project.providers.provider {
            project.properties.getOrDefault("versioning.$name", def) as String
        }
    }
}
