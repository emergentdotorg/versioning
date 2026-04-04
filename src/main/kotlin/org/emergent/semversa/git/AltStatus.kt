package org.emergent.semversa.git

class AltStatus(
    val staged: Changes,
    val unstaged: Changes,
    val conflicts: Set<String>,
) {
    fun isClean(): Boolean {
        return (staged.getAllChanges() + unstaged.getAllChanges() + conflicts).isEmpty()
    }

    class Changes(
        val added: Set<String>,
        val modified: Set<String>,
        val removed: Set<String>,
    ) {
        fun getAllChanges(): Set<String> {
            return added + modified + removed
        }
    }


}