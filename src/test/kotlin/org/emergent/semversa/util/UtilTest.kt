package org.emergent.semversa.util

import org.apache.commons.lang3.StringUtils
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import java.util.*
import java.util.stream.Stream

class UtilTest {

    @Test
    fun treeMapHoldsEmptyValues() {
        val exception = Assertions.catchThrowable { TreeMap<String, Any?>()["foo"] = null }
        Assertions.assertThat(exception).isNull()
    }

    @ParameterizedTest
    @CsvSource("refs/tags/v0.1.0,true", "refs/tags/v0.15.0,true", "v0.15.0,true", "0.15.0,true", "hoopla,false")
    fun matches(tagName: String, expected: Boolean) {
        Assertions.assertThat(Constants.VERSION_REGEX.matcher(tagName).matches()).isEqualTo(expected)
        Assertions.assertThat(Constants.VERSION_REGEX.asMatchPredicate().test(tagName)).isEqualTo(expected)
    }

    @ParameterizedTest
    @CsvSource("3.1.2,3.1.2", "v0.15.0,v0.15.0", "refs/tags/v3.1.2,v3.1.2")
    fun extractTagName(refName: String, expectedTag: String) {
        val pattern = Constants.VERSION_REGEX
        Assertions.assertThat(pattern.asMatchPredicate().test(refName)).isTrue
        val matcher = pattern.matcher(refName)
        Assertions.assertThat(matcher.matches()).isTrue
        val tag = matcher.group("tag")
        Assertions.assertThat(tag).isEqualTo(expectedTag)
        val version = matcher.group("version")
        Assertions.assertThat(version).isEqualTo(StringUtils.stripStart(expectedTag, "v"))
    }

    @ParameterizedTest
    @MethodSource("emptyStringProvider")
    fun isNotBlank(value: String?, result: Boolean) {
        Assertions.assertThat(Util.isNotBlank(value)).isEqualTo(result)
    }

    @ParameterizedTest
    @CsvSource("1,true", "0,true", "-1,false")
    fun assertNotNegative(value: Int?, valid: Boolean) {
        val exception = Assertions.catchThrowable { Util.assertNotNegative(value) }
        if (valid) {
            Assertions.assertThat(exception).isNull()
        } else {
            Assertions.assertThat(exception)
                .isNotNull
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("Number $value must be a non-negative integer")
        }
    }

    @ParameterizedTest
    @CsvSource("-1,1,1,-1", "2,-2,2,-2", "3,3,-3,-3")
    @DisplayName("Negative version component value")
    fun negativeVersionTest(major: Int, minor: Int, patch: Int, negativeValue: Int) {
        val exception = Assertions.catchThrowable {
            Util.assertNotNegative(major)
            Util.assertNotNegative(minor)
            Util.assertNotNegative(patch)
        }
        Assertions.assertThat(exception)
            .isNotNull
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("$negativeValue must be a non-negative integer")
    }

    companion object {
        @JvmStatic
        fun emptyStringProvider(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(null, false),
                Arguments.of("", false),
                Arguments.of("  ", false),
                Arguments.of("something", true)
            )
        }
    }
}