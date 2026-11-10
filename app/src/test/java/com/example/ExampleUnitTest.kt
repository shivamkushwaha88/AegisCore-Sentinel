package com.example

import com.example.core.model.AppSnapshot
import com.example.core.model.DevicePosture
import com.example.core.model.FindingSeverity
import com.example.core.model.FindingStatus
import com.example.core.model.TrustException
import com.example.core.rules.VersionedRuleEvaluator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `VersionedRuleEvaluator flags sensitive permission combination`() {
        val evaluator = VersionedRuleEvaluator()
        val mockApps = listOf(
            AppSnapshot(
                scanRunId = "test-run",
                packageName = "com.sample.tracker",
                displayName = "Sample Tracker",
                versionName = "1.0",
                versionCode = 1,
                targetSdk = 34,
                installerPackage = "com.android.vending",
                signingCertificateSha256 = "AA:BB:CC",
                firstInstallTime = 0,
                lastUpdateTime = 0,
                isSystemApp = false,
                requestedPermissions = listOf(
                    "android.permission.ACCESS_BACKGROUND_LOCATION",
                    "android.permission.READ_PHONE_STATE"
                ),
                grantedPermissions = listOf(
                    "android.permission.ACCESS_BACKGROUND_LOCATION",
                    "android.permission.READ_PHONE_STATE"
                )
            )
        )

        val mockPosture = DevicePosture(
            apiLevel = 34,
            securityPatch = "2026-10-01",
            isLockSecured = true,
            isDebugActive = false,
            buildModel = "Pixel 8",
            buildFingerprint = "google/pixel8"
        )

        val findings = evaluator.evaluateAll("test-run", mockApps, mockPosture, emptyList())
        val sensitiveFinding = findings.find { it.ruleId == VersionedRuleEvaluator.RULE_SENSITIVE_COMBO }

        assertTrue("Expected sensitive combo finding to be detected", sensitiveFinding != null)
        assertEquals(FindingSeverity.HIGH, sensitiveFinding?.severity)
        assertEquals(FindingStatus.NEW, sensitiveFinding?.status)
    }

    @Test
    fun `TrustException marks matching finding as DISMISSED`() {
        val evaluator = VersionedRuleEvaluator()
        val mockApps = listOf(
            AppSnapshot(
                scanRunId = "test-run",
                packageName = "com.sample.tracker",
                displayName = "Sample Tracker",
                versionName = "1.0",
                versionCode = 1,
                targetSdk = 34,
                installerPackage = "com.android.vending",
                signingCertificateSha256 = "AA:BB:CC",
                firstInstallTime = 0,
                lastUpdateTime = 0,
                isSystemApp = false,
                requestedPermissions = listOf("android.permission.ACCESS_BACKGROUND_LOCATION", "android.permission.READ_PHONE_STATE"),
                grantedPermissions = listOf("android.permission.ACCESS_BACKGROUND_LOCATION", "android.permission.READ_PHONE_STATE")
            )
        )

        val mockPosture = DevicePosture(
            apiLevel = 34,
            securityPatch = "2026-10-01",
            isLockSecured = true,
            isDebugActive = false,
            buildModel = "Pixel 8",
            buildFingerprint = "google/pixel8"
        )

        val trustExceptions = listOf(
            TrustException(
                packageName = "com.sample.tracker",
                ruleId = VersionedRuleEvaluator.RULE_SENSITIVE_COMBO,
                reason = "Verified test tool",
                enabled = true
            )
        )

        val findings = evaluator.evaluateAll("test-run", mockApps, mockPosture, trustExceptions)
        val sensitiveFinding = findings.find { it.ruleId == VersionedRuleEvaluator.RULE_SENSITIVE_COMBO }

        assertTrue("Expected finding to exist", sensitiveFinding != null)
        assertEquals(FindingStatus.DISMISSED, sensitiveFinding?.status)
    }

    @Test
    fun `Device Posture evaluates unsecured lock screen`() {
        val evaluator = VersionedRuleEvaluator()
        val mockPosture = DevicePosture(
            apiLevel = 34,
            securityPatch = "2026-10-01",
            isLockSecured = false,
            isDebugActive = false,
            buildModel = "Pixel 8",
            buildFingerprint = "google/pixel8"
        )

        val findings = evaluator.evaluateAll("test-run", emptyList(), mockPosture, emptyList())
        val lockFinding = findings.find { it.ruleId == VersionedRuleEvaluator.RULE_LOCK_INSECURE }

        assertTrue("Expected lock screen finding", lockFinding != null)
        assertEquals(FindingSeverity.HIGH, lockFinding?.severity)
    }
}
