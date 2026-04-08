package org.emergent.semversa

import java.nio.file.Paths

class Main {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val repopath = args.firstOrNull() ?: ""
            val resolver = VersionResolver(Config(), Paths.get(repopath))
            println("info.full: ${resolver.version()}")
        }
    }
}