package org.emergent.semversa.git

import org.apache.commons.lang3.StringUtils
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevObject
import org.eclipse.jgit.revwalk.RevTag
import org.emergent.semversa.util.Util
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.function.Supplier
import java.util.regex.Pattern
import java.util.stream.Collectors

class TagProvider(tagNameRegex: String, private val git: Git) {

    private val pattern: Pattern
    private val tagMap: Supplier<Map<ObjectId, List<com.github.zafarkhaja.semver.Version>>>

    init {
        var regex = requireNotNull(tagNameRegex)
        regex = (if (regex.startsWith("^")) "" else "^") + regex
        regex += if (regex.endsWith("$")) "" else "$"
        this.pattern = Pattern.compile(regex)
        requireNotNull(git) { "git must not be null" }
        tagMap = Util.memoize { createTagMap() }
    }

    private fun createTagMap(): Map<ObjectId, List<com.github.zafarkhaja.semver.Version>> {
        return try {
            git.tagList().call().stream()
                .map { ref ->
                    val refName = ref.leaf.name
                    val tagName = StringUtils.substringAfter(refName, "refs/tags/")
                    Pair(ref, pattern.matcher(tagName))
                }
                .filter { it.second.matches() }
                .collect(
                    Collectors.groupingBy(
                        { getObjectId(it.first) },
                        Collectors.mapping({
                            com.github.zafarkhaja.semver.Version.parse(it.second.group(1))
                        }, Collectors.toList())
                    )
                )
        } catch (e: GitAPIException) {
            throw RuntimeException(e)
        }
    }

    fun getTag(commit: RevCommit): Optional<com.github.zafarkhaja.semver.Version> {
        val tags = tagMap.get()[commit.id] ?: emptyList()
        return tags.stream().max(Comparator.naturalOrder())
    }

    fun getObjectId(ref: Ref): ObjectId? {
        return getObjectId(resolveRevObject(ref).lastOrNull())
    }

    fun getObjectId(objectId: ObjectId): ObjectId? {
        return getObjectId(resolveRevObject(objectId).lastOrNull())
    }

    fun getObjectId(revObj: RevObject?): ObjectId? {
        return revObj?.id
    }

    fun resolveRevObject(ref: Ref): LinkedList<RevObject> {
        val objectIdImmediate = getObjectIdImmediate(ref)
        return resolveRevObject(objectIdImmediate)
    }

    fun resolveRevObject(objectId: ObjectId): LinkedList<RevObject> {
        return getTargetRevObject(getRevObject(objectId))
    }

    private fun getRevObject(objectId: ObjectId): RevObject? {
        git.repository.newObjectReader().use { reader ->
            return try {
                val loader = reader.open(objectId)
                val objectType = loader.type
                val rawData = loader.bytes
                when (objectType) {
                    Constants.OBJ_EXT -> {
                        println("\text: ${objectId.name}")
                        null
                    }

                    Constants.OBJ_COMMIT -> RevCommit.parse(rawData)
                    Constants.OBJ_TREE -> {
                        println("\ttree: ${objectId.name}")
                        null
                    }

                    Constants.OBJ_BLOB -> {
                        val content = String(rawData, StandardCharsets.UTF_8)
                        println("\tblob: ${objectId.name}, content: $content")
                        null
                    }

                    Constants.OBJ_TAG -> RevTag.parse(rawData)
                    else -> {
                        println("\tother: ${objectId.name}")
                        null
                    }
                }
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }
    }

    fun getObjectIdImmediate(ref: Ref): ObjectId {
        Util.check(ref.isPeeled == (ref.peeledObjectId != null))
        return ref.peeledObjectId ?: ref.objectId
    }

    private fun getTargetRevObject(revObj: RevObject?): LinkedList<RevObject> {
        return getTargetRevObject(revObj, LinkedList())
    }

    private fun getTargetRevObject(revObj: RevObject?, results: LinkedList<RevObject>): LinkedList<RevObject> {
        return when (revObj?.type) {
            Constants.OBJ_COMMIT -> {
                results.add(revObj)
                results
            }

            Constants.OBJ_TAG -> {
                // Annotated tag
                results.add(revObj)
                val target = (revObj as RevTag).`object`
                if (target == null) {
                    results
                } else {
                    getTargetRevObject(target, results)
                }
            }

            else -> results
        }
    }

    companion object {
        @JvmStatic
        fun tagOrder(tagPattern: String, tagName: String): Int {
            val regex = Regex(tagPattern)
            val m = regex.find(tagName)
            if (m != null) {
                val ngroups = m.groupValues.size - 1
                if (ngroups < 1) {
                    throw IllegalArgumentException(
                        "Tag pattern is expected to have at least one number grouping instruction: $tagPattern"
                    )
                } else {
                    return m.groupValues[1].toInt()
                }
            } else {
                throw IllegalStateException("Tag $tagName should have matched $tagPattern")
            }
        }
    }
}