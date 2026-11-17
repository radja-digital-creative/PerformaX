package id.biz.yuka.performax.ui.screen.account

import org.junit.Assert.*
import org.junit.Test

class AccountAccessPolicyTest {
    @Test fun onlySelfOrOwnerMayChangeCredentials() {
        listOf("owner", "manager", "admin", "staff").forEach { role ->
            assertTrue(AccountAccessPolicy.canEditCredentials("me", role, "me"))
            assertEquals(role == "owner", AccountAccessPolicy.canEditCredentials("me", role, "other"))
        }
        assertFalse(AccountAccessPolicy.canEditCredentials(null, "owner", "other"))
        assertFalse(AccountAccessPolicy.canEditCredentials("", null, ""))
    }
    @Test fun creationPermissionsAreExplicitAndFailClosed() {
        assertEquals(setOf(1, 2, 3, 4), AccountAccessPolicy.creatableRoles("owner").map { it.first }.toSet())
        assertEquals(listOf(4), AccountAccessPolicy.creatableRoles("manager").map { it.first })
        listOf("admin", "staff", "unknown", null).forEach { assertTrue(AccountAccessPolicy.creatableRoles(it).isEmpty()) }
    }
}
