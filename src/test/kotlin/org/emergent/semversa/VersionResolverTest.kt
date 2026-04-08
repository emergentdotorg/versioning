package org.emergent.semversa

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class VersionResolverTest {

    @Test
    fun testReleaseSansCommits() {
        val strategy = getPatternStrategy()
        assertThat(strategy.version()).isNotNull()
            .isEqualTo("1.2.3")
    }

    @Test
    fun testDevelSansCommits() {
        val strategy = getPatternStrategy()
        val config = strategy.config
        val resolved = strategy.resolved().copy(branch = "development")
        val updatedStrategy = VersionResolver.info(
            Config(
                "main",
                tagNameRegex = config.tagNameRegex,
                versionPattern = config.versionPattern
            ),
            resolved
        )
        assertThat(updatedStrategy.version()).isNotNull()
            .isEqualTo("1.2.3-development+c9f54782")
    }

    @Test
    fun testReleaseWithCommits() {
        val strategy = getPatternStrategy()
        val config = strategy.config
        val resolved = strategy.resolved()
            .copy(commits = 1)
        val updatedStrategy = VersionResolver.info(config, resolved)
        assertThat(updatedStrategy.version()).isNotNull()
            //.isEqualTo("1.2.3-1-SNAPSHOT+c9f54782")
            .isEqualTo("1.2.4-SNAPSHOT")
    }

    @Test
    fun testDevelopmentWithCommits() {
        val strategy = getPatternStrategy()
        val config = strategy.config
        val resolved = strategy.resolved()
            .copy(branch = "development", commits = 1)
        val updatedStrategy = VersionResolver.info(config, resolved)
        assertThat(updatedStrategy.version()).isNotNull()
            .isEqualTo("1.2.3-development-1-SNAPSHOT+c9f54782")
    }

    @Test
    fun testDirty() {
        val strategy = getPatternStrategy()
        val config = strategy.config
        val resolved = strategy.resolved()
            .copy(dirty = true)
        val updatedStrategy = VersionResolver.info(config, resolved)
        assertThat(updatedStrategy.version()).isNotNull()
            //.isEqualTo("1.2.3.dirty")
            .isEqualTo("1.2.4-SNAPSHOT")
    }

    @Test
    fun testPatternSansHash() {
        val strategy = getPatternStrategy()
        val config = strategy.config
        val resolved = strategy.resolved()
        val updatedStrategy = VersionResolver.info(
            Config(
                releaseBranchRegex = config.releaseBranchRegex,
                tagNameRegex = config.tagNameRegex,
                versionPattern = "%t(-%B)(-%C)(-%S)(.%D)"
            ),
            resolved
        )
        assertThat(updatedStrategy.version()).isNotNull()
            .isEqualTo("1.2.3")
    }

    // @Test
    // fun testPropertiesNames() {
    //     val strategy = getPatternStrategy()
    //     val props = strategy.asMap()
    //     val collect = props.entries.joinToString(
    //         separator = "\n\t",
    //         prefix = "\n\t",
    //         postfix = "\n"
    //     ) { (key, value) -> "$key=$value" }
    //     println("props:$collect")
    //     val reborn = toPatternStrategy(props)
    //     assertThat(reborn).isEqualTo(strategy)
    //     val def = PatternStrategy()
    //     val map = def.asMap()
    //     assertThat(map).isEqualTo(EMPTY)
    // }

    // @Test
    // fun testPropertiesRoundTrip() {
    //     val strategy = getPatternStrategy()
    //     val props = strategy.asMap()
    //     val reborn = toPatternStrategy(props)
    //     assertThat(reborn).isEqualTo(strategy)
    //     val def = PatternStrategy()
    //     val map = def.asMap()
    //     assertThat(map).isEqualTo(EMPTY)
    // }

    @Test
    fun testXmlOutput() {
        // val strategy = getPatternStrategy()
        // val xml = PropCodec.toXml(strategy)
        // assertThat(xml).isEqualTo("")
    }

    companion object {
        private val EMPTY = emptyMap<Any, Any>()

        private fun getPatternStrategy(): ConfigAndResolved {
            val config = getConf()
            val resolved = getCalc()
            return ConfigAndResolved(resolved, config)
        }

        private fun getCalc(): Resolved {
            return Resolved(
                gitDir = "",
                detached = false,
                tagVersion = "1.2.3",
                branch = "release",
                hash = "c9f54782",
                commits = 0,
                dirty = false)
        }

        private fun getConf(): Config {
            return Config(
                releaseBranchRegex = "^(release|stable)$",
                tagNameRegex = "v?([0-9]+\\.[0-9]+\\.[0-9]+)",
                versionPattern = "%t(-%B)(-%C)(-%S)(+%H)(.%D)")
        }
    }

    class ConfigAndResolved(
        val resolved: Resolved,
        val config: Config
    ) {
        fun version(): String = VersionResolver.getFull(config, resolved)
        fun resolved(): Resolved = resolved
        fun config(): Config = config
    }

}