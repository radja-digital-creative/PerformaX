package id.biz.yuka.performax.ui.screen.account

/** UI guards only; the server must independently enforce these permissions. */
object AccountAccessPolicy {
    fun canEditCredentials(actorId: String?, actorRole: String?, targetId: String): Boolean =
        !actorId.isNullOrBlank() && targetId.isNotBlank() &&
            (actorId == targetId || actorRole?.trim()?.lowercase() == "owner")

    fun creatableRoles(actorRole: String?): List<Pair<Int, String>> = when (actorRole?.trim()?.lowercase()) {
        "owner" -> listOf(1 to "Owner", 3 to "Manager", 2 to "Admin", 4 to "Staff")
        "manager" -> listOf(4 to "Staff")
        else -> emptyList()
    }
}
