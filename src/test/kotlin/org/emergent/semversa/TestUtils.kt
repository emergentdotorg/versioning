package org.emergent.semversa

class TestUtils {

    companion object {
        @JvmStatic
        fun leftShift(file: Any, string: String) {
            val f =
                when (file) {
                    is java.io.File -> file
                    is java.nio.file.Path -> file.toFile()
                    else -> java.io.File(file.toString())
                }
            f.appendText(string)
        }

        @JvmStatic
        fun setText(file: Any, string: String) {
            val f =
                when (file) {
                    is java.io.File -> file
                    is java.nio.file.Path -> file.toFile()
                    else -> java.io.File(file.toString())
                }
            f.writeText(string)
        }
    }
}
