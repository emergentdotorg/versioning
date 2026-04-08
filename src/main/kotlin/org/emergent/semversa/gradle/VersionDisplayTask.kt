package org.emergent.semversa.gradle

import org.emergent.semversa.VersionInfo
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

open class VersionDisplayTask @Inject constructor() : VersionBaseTask() {

    init {
        description = "Writes version information on the standard output."
    }

    @TaskAction
    fun run() {
        val p: (String?) -> Unit = { s: String? -> println(s) }
        val info = project.extensions.getByType(SemversaExtension::class.java).info.get()
        if (info === VersionInfo.NONE) {
            p("[version] No version can be computed from the SCM.")
            return
        }
        with (info) {
            p("[version] scm         = $scm")
            p("[version] branch      = $branch")
            p("[version] branchType  = $branchType")
            p("[version] branchId    = $branchId")
            p("[version] commit      = $commit")
            p("[version] full        = $full")
            p("[version] base        = $base")
            p("[version] build       = $build")
            p("[version] gradle      = ${if (project.version == "unspecified") "" else project.version}")
            p("[version] display     = $display")
            p("[version] tag         = ${tag ?: ""}")
            p("[version] lastTag     = ${lastTag ?: ""}")
            p("[version] dirty       = $dirty")
            p("[version] versionCode = ${versionNumber!!.versionCode}")
            p("[version] major       = ${versionNumber!!.major}")
            p("[version] minor       = ${versionNumber!!.minor}")
            p("[version] patch       = ${versionNumber!!.patch}")
            p("[version] qualifier   = ${versionNumber!!.qualifier}")
            p("[version] time        = ${time ?: ""}")
        }
    }
}
