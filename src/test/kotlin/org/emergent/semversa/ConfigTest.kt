package org.emergent.semversa

import org.assertj.core.api.Assertions
import org.emergent.semversa.util.Constants
import org.junit.jupiter.api.Test

class ConfigTest {

    @Test
    fun getDefaults() {
        val config = Config()
        Assertions.assertThat(config)
            .extracting("versionPattern", "releaseBranchRegex")
            .containsExactly(Constants.VERSION_PATTERN_DEF, "^(main|master)$")
    }

    @Test
    fun setMiscellaneous() {
        val config = getConf()
        Assertions.assertThat(config)
            .extracting("versionPattern", "releaseBranchRegex")
            .containsExactly("%t(-%C)", "^(release|stable)$")
    }

    // @Test
    // fun testPropertiesRoundTrip() {
    //     val config = getConf()
    //     val props = config.asMap()
    //     val reborn = Config.from(props)
    //     assertThat(reborn).isEqualTo(config)
    //
    //     val def = Config.builder().build()
    //     val map = def.asMap()
    //     assertThat(map).isEqualTo(EMPTY)
    // }

    private fun getConf(): Config {
        return Config(
            releaseBranchRegex = "^(release|stable)$",
            tagNameRegex = "v?([0-9]+\\.[0-9]+\\.[0-9]+)",
            versionPattern = "%t(-%C)"
        )
    }
}