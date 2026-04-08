package org.emergent.semversa.gradle

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

open class VersionFileTask @Inject constructor() : VersionBaseTask() {

    init {
        description = "Writes version information into a file."
    }

    /** File to write the information info. */
    @OutputFile
    var file: File = project.layout.buildDirectory.file("version.properties").get().asFile

    /** Prefix to apply. Defaults to VERSION_ */
    @Input
    var prefix: String = "VERSION_"

    @TaskAction
    fun run() {
        val info = project.extensions.getByType(SemversaExtension::class.java).info.get()
        getVerifiedParent(file)
        with(info) {
            file.writeText(
                "" +
                        "${prefix}BUILD=${build}\n" +
                        "${prefix}BRANCH=${branch}\n" +
                        "${prefix}BASE=${base}\n" +
                        "${prefix}BRANCHID=${branchId}\n" +
                        "${prefix}BRANCHTYPE=${branchType}\n" +
                        "${prefix}COMMIT=${commit}\n" +
                        "${prefix}GRADLE=${if (project.version == "unspecified") "" else project.version}\n" +
                        "${prefix}DISPLAY=${display}\n" +
                        "${prefix}FULL=${full}\n" +
                        "${prefix}SCM=${scm}\n" +
                        "${prefix}TAG=${tag ?: ""}\n" +
                        "${prefix}LAST_TAG=${lastTag ?: ""}\n" +
                        "${prefix}DIRTY=${dirty}\n" +
                        "${prefix}VERSIONCODE=${versionNumber?.versionCode}\n" +
                        "${prefix}MAJOR=${versionNumber?.major}\n" +
                        "${prefix}MINOR=${versionNumber?.minor}\n" +
                        "${prefix}PATCH=${versionNumber?.patch}\n" +
                        "${prefix}QUALIFIER=${versionNumber?.qualifier}\n" +
                        "${prefix}TIME=${time ?: ""}\n"
            )
        }
    }
}
