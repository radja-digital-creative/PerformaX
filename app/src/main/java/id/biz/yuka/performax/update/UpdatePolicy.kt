package id.biz.yuka.performax.update

import kotlinx.coroutines.withTimeoutOrNull

object UpdatePolicy {
    suspend fun <T> checkWithin(timeoutMs: Long = 4_000, check: suspend () -> T): T? =
        withTimeoutOrNull(timeoutMs) { check() }

    fun canInstall(packageName: String, installedPackage: String, version: Long, installedVersion: Long,
                   signers: Set<String>, installedSigners: Set<String>): Boolean =
        packageName == installedPackage && version > installedVersion &&
            signers.isNotEmpty() && signers == installedSigners
}
