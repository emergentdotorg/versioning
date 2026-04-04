package org.emergent.semversa.git

import org.eclipse.jgit.lib.Repository

class Branch(
    /**
     * The fully qualified name of this branch.
     */
    val fullName: String,
    /**
     * This branch's upstream branch. {@code null} if this branch isn't tracking an upstream.
     */
    val trackingBranch: Branch? = null
) {
    /**
     * The simple name of the branch.
     * @return the simple name
     */
    fun getName(): String {
        return Repository.shortenRefName(fullName)
    }
}