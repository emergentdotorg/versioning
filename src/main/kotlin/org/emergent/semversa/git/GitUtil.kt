package org.emergent.semversa.git

import org.eclipse.jgit.api.Status
import org.eclipse.jgit.errors.IncorrectObjectTypeException
import org.eclipse.jgit.lib.BranchConfig
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevObject
import org.eclipse.jgit.revwalk.RevWalk
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

class GitUtil private constructor() {

    init {
        throw AssertionError("Cannot instantiate this class.")
    }

    companion object {
        @JvmStatic
        fun getStatus(dir: File): Status {
            return GitExec.applyOp(dir) { git -> git.status().call() }
        }

        @JvmStatic
        fun isGitTreeDirty(status: Status): Boolean {
            return !isClean(status)
        }

        @JvmStatic
        private fun isClean(status: Status): Boolean {
            val s = GitUtil.convertStatus(status)
            return s.staged.getAllChanges().isEmpty() &&
                    s.unstaged.getAllChanges().none { !it.startsWith("userHome/") }
        }


        /**
         * Resolves a JGit {@code ObjectId} using the given revision string.
         * @param repo the Grgit repository to resolve the object from
         * @param revstr the revision string to use
         * @return the resolved object
         */
        @JvmStatic
        fun resolveObject(repo: Repository, revstr: String?): ObjectId? {
            return if (revstr != null) {
                repo.resolve(revstr)
            } else {
                null
            }
        }

        /**
         * Resolves a JGit {@code RevObject} using the given revision string.
         * @param repo the Grgit repository to resolve the object from
         * @param revstr the revision string to use
         * @param peel whether or not to peel the resolved object
         * @return the resolved object
         */
        @JvmStatic
        fun resolveRevObject(repo: Repository, revstr: String?, peel: Boolean = false): RevObject? {
            val id = resolveObject(repo, revstr)
            return if (id != null) {
                val walk = RevWalk(repo)
                val rev = walk.parseAny(id)
                if (peel) walk.peel(rev) else rev
            } else {
                null
            }
        }

        /**
         * Resolves the parents of an object.
         * @param repo the Grgit repository to resolve the parents from
         * @param id the object to get the parents of
         * @return the parents of the commit
         */
        @JvmStatic
        fun resolveParents(repo: Repository, id: ObjectId): Set<ObjectId> {
            val walk = RevWalk(repo)
            val rev = walk.parseCommit(id)
            return rev.parents.mapTo(mutableSetOf()) { walk.parseCommit(it) }
        }

        /**
         * Resolves a Grgit {@code Commit} using the given revision string.
         * @param repo the Grgit repository to resolve the commit from
         * @param revstr the revision string to use
         * @return the resolved commit
         */
        @JvmStatic
        fun resolveCommit(repo: Repository, revstr: String?): Commit? {
            return if (revstr != null) {
                val id = resolveObject(repo, revstr)
                resolveCommit(repo, id)
            } else {
                null
            }
        }

        /**
         * Resolves a Grgit {@code Commit} using the given object.
         * @param repo the Grgit repository to resolve the commit from
         * @param id the object id of the commit to resolve
         * @return the resolved commit
         */
        @JvmStatic
        fun resolveCommit(repo: Repository, id: ObjectId?): Commit? {
            return if (id != null) {
                val walk = RevWalk(repo)
                convertCommit(repo, walk.parseCommit(id))
            } else {
                null
            }
        }

        /**
         * Converts a JGit commit to a Grgit commit.
         * @param rev the JGit commit to convert
         * @return a corresponding Grgit commit
         */
        @JvmStatic
        fun convertCommit(repo: Repository, rev: RevCommit): Commit {
            val id = ObjectId.toString(rev)
            val reader = repo.newObjectReader()
            val abbrevId = reader.use { reader ->
                reader.abbreviate(rev).name()
            }
            val committer = rev.committerIdent
            val committerPerson = Person(committer.name, committer.emailAddress)
            val author = rev.authorIdent
            val authorPerson = Person(author.name, author.emailAddress)

            val instant = Instant.ofEpochSecond(rev.commitTime.toLong())
            val zone = rev.committerIdent.zoneId ?: ZoneOffset.UTC
            val dateTime = ZonedDateTime.ofInstant(instant, zone)

            val fullMessage = rev.fullMessage
            val shortMessage = rev.shortMessage
            val parentIds : List<String> = rev.parents.map { ObjectId.toString(it) }
            return Commit(
                id = id,
                abbreviatedId = abbrevId,
                parentIds = parentIds,
                author = authorPerson,
                committer = committerPerson,
                dateTime = dateTime,
                fullMessage = fullMessage,
                shortMessage = shortMessage
            )
        }

        /**
         * Resolves a Grgit tag from a name.
         * @param repo the Grgit repository to resolve from
         * @param name the name of the tag to resolve
         * @return the resolved tag
         */
        @JvmStatic
        fun resolveTag(repo: Repository, name: String): Tag? {
            val ref = repo.findRef(name)
            return resolveTag(repo, ref)
        }

        /**
         * Resolves a Grgit Tag from a JGit ref.
         * @param repo the Grgit repository to resolve from
         * @param ref the JGit ref to resolve
         * @return the resolved tag
         */
        @JvmStatic
        fun resolveTag(repo: Repository, ref: Ref?): Tag? {
            if (ref == null) return null
            try {
                val walk = RevWalk(repo)
                val rev = walk.parseTag(ref.objectId)
                val target = walk.peel(rev)
                walk.parseBody(rev.`object`)
                val commit = convertCommit(repo, target as RevCommit)
                val tagger = rev.taggerIdent
                val taggerPerson = Person(tagger.name, tagger.emailAddress)
                val fullMessage = rev.fullMessage
                val shortMessage = rev.shortMessage

                val instant = rev.taggerIdent.whenAsInstant
                val zone = rev.taggerIdent.zoneId ?: ZoneOffset.UTC
                val dateTime = ZonedDateTime.ofInstant(instant, zone)
                return Tag(
                    commit = commit,
                    fullName = ref.name,
                    tagger = taggerPerson,
                    fullMessage = fullMessage,
                    shortMessage = shortMessage,
                    dateTime = dateTime,
                )
            } catch (e: IncorrectObjectTypeException) {
                e.printStackTrace()
                throw RuntimeException("Cannot resolve tag: $ref", e)
                //val commit = resolveCommit(repo, ref.objectId)
                //return Tag(
                //    commit = commit,
                //)
            }
        }

        /**
         * Resolves a Grgit branch from a name.
         * @param repo the Grgit repository to resolve from
         * @param name the name of the branch to resolve
         * @return the resolved branch
         */
        @JvmStatic
        fun resolveBranch(repo: Repository, name: String?): Branch? {
            if (name == null) return null
            val ref = repo.findRef(name)
            return resolveBranch(repo, ref)
        }

        /**
         * Resolves a Grgit branch from a JGit ref.
         * @param repo the Grgit repository to resolve from
         * @param ref the JGit ref to resolve
         * @return the resolved branch or {@code null} if the {@code ref} is
         * {@code null}
         */
        @JvmStatic
        fun resolveBranch(repo: Repository, ref: Ref?): Branch? {
            if (ref == null) return null
            val fullName = ref.name
            val shortName = Repository.shortenRefName(fullName)
            val config = repo.config
            val branchConfig = BranchConfig(config, shortName)
            val trackingBranch = resolveBranch(repo, branchConfig.trackingBranch)
            return Branch(
                fullName = fullName,
                trackingBranch = trackingBranch
            )
        }

        /**
         * Converts a JGit status to an AltStatus instance.
         */
        fun convertStatus(jgitStatus: Status): AltStatus {
            return AltStatus(
                staged = AltStatus.Changes(
                    added = jgitStatus.added,
                    modified = jgitStatus.changed,
                    removed = jgitStatus.removed
                ),
                unstaged = AltStatus.Changes(
                    added = jgitStatus.untracked,
                    modified = jgitStatus.modified,
                    removed = jgitStatus.missing
                ),
                conflicts = jgitStatus.conflicting
            )
        }

        ///**
        // * Converts a JGit remote to a Grgit remote.
        // * @param rc the remote config to convert
        // * @return the converted remote
        // */
        //fun convertRemote(rc: RemoteConfig): Remote {
        //    return Remote(
        //        name = rc.name,
        //        url = rc.uris.firstOrNull(),
        //        pushUrl = rc.pushURIs.firstOrNull(),
        //        fetchRefSpecs = rc.fetchRefSpecs.map { it.toString() },
        //        pushRefSpecs = rc.pushRefSpecs.map { it.toString() },
        //        mirror = rc.isMirror
        //    )
        //}

        /**
         * Checks if {@code base} is an ancestor of {@code tip}.
         * @param repo the repository to look in
         * @param base the version that might be an ancestor
         * @param tip the tip version
         * @since 0.2.2
         */
        @JvmStatic
        fun isAncestorOf(repo: Repository, base: Commit, tip: Commit): Boolean {
            val jgit = repo
            val revWalk = RevWalk(jgit)
            val baseCommit = revWalk.lookupCommit(jgit.resolve(base.id))
            val tipCommit = revWalk.lookupCommit(jgit.resolve(tip.id))
            return revWalk.isMergedInto(baseCommit, tipCommit)
        }

        @JvmStatic
        fun tagOrder(tagPattern: String): Comparator<String> {
            val regex = Regex(tagPattern)
            return Comparator { o1, o2 ->
                val m1 = regex.matchEntire(o1)
                val m2 = regex.matchEntire(o2)
                if (m1?.groups == null || m2?.groups == null) {
                    val tagName = if (m1?.groups == null) o1 else o2
                    throw IllegalStateException("Tag $tagName should have matched $tagPattern")
                } else {
                     listOf(1, 2, 3).map {
                        val val1 = m1.groups[it]?.value?.toInt() ?: -1
                        val val2 = m2.groups[it]?.value?.toInt() ?: -1
                        return@map val1.compareTo(val2)
                    }.first { it != 0 }.or(0)
                }
            }
        }

    }
}