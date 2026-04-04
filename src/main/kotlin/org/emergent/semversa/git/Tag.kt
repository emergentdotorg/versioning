package org.emergent.semversa.git

import org.eclipse.jgit.lib.Repository
import java.time.ZonedDateTime

class Tag (
    /** The commit this tag points to. */
    val commit: Commit,
    /** The person who created the tag. */
    val tagger: Person,
    /** The full name of this tag. */
    val fullName: String,
    /** The full tag message. */
    val fullMessage: String,
    /** The shortened tag message. */
    val shortMessage: String,
    /** The time the commit was created with the time zone of the committer, if available. */
    val dateTime: ZonedDateTime,
    val name: String = Repository.shortenRefName(fullName)
)
