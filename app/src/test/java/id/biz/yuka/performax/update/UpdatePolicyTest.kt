package id.biz.yuka.performax.update

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class UpdatePolicyTest {
    @Test fun slowCheckDoesNotBlockStartup() = runBlocking {
        assertNull(UpdatePolicy.checkWithin(30) { delay(500); "new version" })
    }
    @Test fun fastCheckKeepsReleaseDetails() = runBlocking {
        assertEquals("7.2.2", UpdatePolicy.checkWithin(1000) { "7.2.2" })
    }
    @Test fun screenCancellationIsNotTreatedAsOffline() = runBlocking {
        var continued = false
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            UpdatePolicy.checkWithin(1000) { delay(500); "new version" }
            continued = true
        }
        job.cancelAndJoin()
        assertFalse(continued)
    }
    @Test fun rejectsWrongPackageOlderVersionAndDifferentSigner() {
        assertTrue(UpdatePolicy.canInstall("app", "app", 722, 721, setOf("same"), setOf("same")))
        assertFalse(UpdatePolicy.canInstall("other", "app", 722, 721, setOf("same"), setOf("same")))
        assertFalse(UpdatePolicy.canInstall("app", "app", 721, 721, setOf("same"), setOf("same")))
        assertFalse(UpdatePolicy.canInstall("app", "app", 722, 721, setOf("new"), setOf("old")))
        assertFalse(UpdatePolicy.canInstall("app", "app", 722, 721, emptySet(), emptySet()))
    }
}
