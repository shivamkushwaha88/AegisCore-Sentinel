package com.example.core.rules

import com.example.core.model.AppSnapshot
import com.example.core.model.DevicePosture
import com.example.core.model.EvidenceConfidence
import com.example.core.model.Finding
import com.example.core.model.FindingSeverity
import com.example.core.model.FindingStatus
import com.example.core.model.RemediationType
import com.example.core.model.TrustException

class VersionedRuleEvaluator {

    companion object {
        const val RULE_SET_VERSION = "ANDROID_BASELINE_V1"

        const val RULE_SENSITIVE_COMBO = "R-APP-SENSITIVE-COMBO"
        const val RULE_OUTDATED_SDK = "R-APP-OUTDATED-SDK"
        const val RULE_EXPORTED_UNPROTECTED = "R-APP-EXPORTED-UNPROTECTED"
        const val RULE_SIDELOADED_SENSITIVE = "R-APP-SIDELOADED-SENSITIVE"
        const val RULE_PATCH_STALE = "R-DEV-PATCH-STALE"
        const val RULE_LOCK_INSECURE = "R-DEV-LOCK-INSECURE"
        const val RULE_ADB_ACTIVE = "R-DEV-ADB-ACTIVE"
    }

    fun evaluateAll(
        scanRunId: String,
        apps: List<AppSnapshot>,
        posture: DevicePosture,
        activeExceptions: List<TrustException>
    ): List<Finding> {
        val findings = mutableListOf<Finding>()

        // 1. Evaluate Device Posture Rules
        evaluateDevicePosture(scanRunId, posture, activeExceptions, findings)

        // 2. Evaluate Installed App Rules
        for (app in apps) {
            evaluateApp(scanRunId, app, activeExceptions, findings)
        }

        return findings
    }

    private fun evaluateDevicePosture(
        scanRunId: String,
        posture: DevicePosture,
        exceptions: List<TrustException>,
        findings: MutableList<Finding>
    ) {
        // Lock screen check
        if (!posture.isLockSecured) {
            val isExempt = isRuleExempt("system.device", RULE_LOCK_INSECURE, exceptions)
            findings.add(
                Finding(
                    ruleId = RULE_LOCK_INSECURE,
                    ruleVersion = "1.0",
                    scanRunId = scanRunId,
                    targetType = "DEVICE",
                    targetId = "system.device",
                    targetName = "Lock Screen Security",
                    title = "Device Lock Screen Not Configured",
                    summary = "The device lacks a secure PIN, pattern, or biometric authentication lock.",
                    whyItMatters = "Without a secure lock screen, anyone with physical access can inspect your apps, credentials, and local data without impediment.",
                    severity = FindingSeverity.HIGH,
                    confidence = EvidenceConfidence.CONFIRMED,
                    recommendedAction = "Configure a PIN, password, or biometric fingerprint lock in System Settings.",
                    remediationType = RemediationType.OPEN_SETTINGS,
                    status = if (isExempt) FindingStatus.DISMISSED else FindingStatus.NEW,
                    evidenceObservation = "KeyguardManager.isDeviceSecure returned false.",
                    sourceApi = "KeyguardManager",
                    limitationNote = "Direct verification via Android public KeyguardManager."
                )
            )
        }

        // ADB Active
        if (posture.isDebugActive) {
            val isExempt = isRuleExempt("system.device", RULE_ADB_ACTIVE, exceptions)
            findings.add(
                Finding(
                    ruleId = RULE_ADB_ACTIVE,
                    ruleVersion = "1.0",
                    scanRunId = scanRunId,
                    targetType = "DEVICE",
                    targetId = "system.device",
                    targetName = "Developer USB Debugging",
                    title = "USB Debugging (ADB) Is Active",
                    summary = "Developer ADB interface is currently enabled on this device.",
                    whyItMatters = "ADB grants shell-level commands and app installation when connected to an authorized PC. Ensure you only authorize trusted workstations.",
                    severity = FindingSeverity.LOW,
                    confidence = EvidenceConfidence.CONFIRMED,
                    recommendedAction = "Disable USB Debugging in Developer Options when not actively developing.",
                    remediationType = RemediationType.OPEN_SETTINGS,
                    status = if (isExempt) FindingStatus.DISMISSED else FindingStatus.NEW,
                    evidenceObservation = "Settings.Global.ADB_ENABLED is set to 1.",
                    sourceApi = "Settings.Global",
                    limitationNote = "Legitimate for developers. Scoped risk annotation recommended if intentional."
                )
            )
        }
    }

    private fun evaluateApp(
        scanRunId: String,
        app: AppSnapshot,
        exceptions: List<TrustException>,
        findings: MutableList<Finding>
    ) {
        val granted = app.grantedPermissions.toSet()

        // Rule: Sensitive permission combination
        val hasBgLocation = "android.permission.ACCESS_BACKGROUND_LOCATION" in granted
        val hasLocation = "android.permission.ACCESS_FINE_LOCATION" in granted || "android.permission.ACCESS_COARSE_LOCATION" in granted
        val hasCamera = "android.permission.CAMERA" in granted
        val hasMic = "android.permission.RECORD_AUDIO" in granted
        val hasSms = "android.permission.READ_SMS" in granted || "android.permission.RECEIVE_SMS" in granted
        val hasPhone = "android.permission.READ_PHONE_STATE" in granted || "android.permission.READ_CALL_LOG" in granted
        val hasOverlay = "android.permission.SYSTEM_ALERT_WINDOW" in granted

        // Combo 1: Background Location + Phone or SMS
        if ((hasBgLocation || hasLocation) && (hasSms || hasPhone)) {
            val isExempt = isRuleExempt(app.packageName, RULE_SENSITIVE_COMBO, exceptions)
            findings.add(
                Finding(
                    ruleId = RULE_SENSITIVE_COMBO,
                    ruleVersion = "1.0",
                    scanRunId = scanRunId,
                    targetType = "APP",
                    targetId = app.packageName,
                    targetName = app.displayName,
                    title = "High-Impact Capability Combination",
                    summary = "App holds both precise Location and Telephony/SMS access concurrently.",
                    whyItMatters = "Combining background movement telemetry with cellular/SMS identity allows detailed behavioral profiling if abused by third-party tracking SDKs.",
                    severity = FindingSeverity.HIGH,
                    confidence = EvidenceConfidence.CONFIRMED,
                    recommendedAction = "Review and revoke unnecessary background permissions in Android App Settings.",
                    remediationType = RemediationType.OPEN_SETTINGS,
                    status = if (isExempt) FindingStatus.DISMISSED else FindingStatus.NEW,
                    evidenceObservation = "Granted: ${granted.filter { it.contains("LOCATION") || it.contains("SMS") || it.contains("PHONE") }.joinToString(", ")}",
                    sourceApi = "PackageManager",
                    limitationNote = "Checks granted runtime permissions only. Does not assert that active recording is taking place."
                )
            )
        }

        // Combo 2: Camera + Microphone + Overlay
        if (hasCamera && hasMic && hasOverlay) {
            val isExempt = isRuleExempt(app.packageName, RULE_SENSITIVE_COMBO, exceptions)
            findings.add(
                Finding(
                    ruleId = RULE_SENSITIVE_COMBO,
                    ruleVersion = "1.0",
                    scanRunId = scanRunId,
                    targetType = "APP",
                    targetId = app.packageName,
                    targetName = app.displayName,
                    title = "Media Recording & System Overlay Exposure",
                    summary = "App holds Camera, Microphone, and Display-over-other-apps permissions.",
                    whyItMatters = "Overlay capability combined with media recording can potentially mask user activity or capture sensitive information if malicious.",
                    severity = FindingSeverity.CRITICAL,
                    confidence = EvidenceConfidence.CONFIRMED,
                    recommendedAction = "Inspect if overlay permission is genuinely required by this application.",
                    remediationType = RemediationType.OPEN_SETTINGS,
                    status = if (isExempt) FindingStatus.DISMISSED else FindingStatus.NEW,
                    evidenceObservation = "Granted: CAMERA, RECORD_AUDIO, and SYSTEM_ALERT_WINDOW.",
                    sourceApi = "PackageManager",
                    limitationNote = "Permission declaration verified via PackageManager."
                )
            )
        }

        // Rule: Outdated Target SDK (Less than Android 11 / API 30) for non-system apps
        if (!app.isSystemApp && app.targetSdk in 1..29) {
            val isExempt = isRuleExempt(app.packageName, RULE_OUTDATED_SDK, exceptions)
            findings.add(
                Finding(
                    ruleId = RULE_OUTDATED_SDK,
                    ruleVersion = "1.0",
                    scanRunId = scanRunId,
                    targetType = "APP",
                    targetId = app.packageName,
                    targetName = app.displayName,
                    title = "Legacy Target SDK (Android ${app.targetSdk})",
                    summary = "Application targets an outdated Android API level (${app.targetSdk} < 30).",
                    whyItMatters = "Legacy target SDKs allow applications to bypass newer Android security restrictions such as modern Scoped Storage and runtime permission dialogs.",
                    severity = FindingSeverity.MODERATE,
                    confidence = EvidenceConfidence.CONFIRMED,
                    recommendedAction = "Check if an updated version is available from the original developer.",
                    remediationType = RemediationType.OPEN_SETTINGS,
                    status = if (isExempt) FindingStatus.DISMISSED else FindingStatus.NEW,
                    evidenceObservation = "ApplicationInfo.targetSdkVersion = ${app.targetSdk}",
                    sourceApi = "PackageManager",
                    limitationNote = "Calculated from binary manifest header."
                )
            )
        }

        // Rule: Exported Unprotected Components
        if (!app.isSystemApp && app.exportedComponentsCount >= 4) {
            val isExempt = isRuleExempt(app.packageName, RULE_EXPORTED_UNPROTECTED, exceptions)
            findings.add(
                Finding(
                    ruleId = RULE_EXPORTED_UNPROTECTED,
                    ruleVersion = "1.0",
                    scanRunId = scanRunId,
                    targetType = "APP",
                    targetId = app.packageName,
                    targetName = app.displayName,
                    title = "Broad Exported Component Exposure (${app.exportedComponentsCount})",
                    summary = "App declares ${app.exportedComponentsCount} exported activities/receivers without explicit permission guards.",
                    whyItMatters = "Exported components can be invoked by any other application on the device, potentially allowing unauthorized intent triggers or data leakage.",
                    severity = FindingSeverity.MODERATE,
                    confidence = EvidenceConfidence.STRONG_INDICATOR,
                    recommendedAction = "Verify whether this app is from a reputable source.",
                    remediationType = RemediationType.OPEN_SETTINGS,
                    status = if (isExempt) FindingStatus.DISMISSED else FindingStatus.NEW,
                    evidenceObservation = "Found ${app.exportedComponentsCount} components with exported=true and permission=null.",
                    sourceApi = "PackageManager",
                    limitationNote = "Static manifest analysis. Exported components with intent-filters may be intentional for entry points."
                )
            )
        }

        // Rule: Sideloaded / Unknown Installer with High Privileges
        val isSideloaded = app.installerPackage.isNullOrBlank() ||
                app.installerPackage == "com.google.android.packageinstaller" ||
                app.installerPackage == "com.android.packageinstaller"
        if (!app.isSystemApp && isSideloaded && app.grantedPermissions.size >= 5) {
            val isExempt = isRuleExempt(app.packageName, RULE_SIDELOADED_SENSITIVE, exceptions)
            findings.add(
                Finding(
                    ruleId = RULE_SIDELOADED_SENSITIVE,
                    ruleVersion = "1.0",
                    scanRunId = scanRunId,
                    targetType = "APP",
                    targetId = app.packageName,
                    targetName = app.displayName,
                    title = "Sideloaded App With Multiple Granted Permissions",
                    summary = "App was installed outside Google Play and holds ${app.grantedPermissions.size} permissions.",
                    whyItMatters = "Sideloaded packages do not undergo centralized Play Protect signature validation on build updates. Review permissions periodically.",
                    severity = FindingSeverity.LOW,
                    confidence = EvidenceConfidence.STRONG_INDICATOR,
                    recommendedAction = "Confirm that this APK was obtained from a trustworthy developer or official repo.",
                    remediationType = RemediationType.OPEN_SETTINGS,
                    status = if (isExempt) FindingStatus.DISMISSED else FindingStatus.NEW,
                    evidenceObservation = "Installer source: ${app.installerPackage ?: "Direct Manual Install"}. Cert SHA-256: ${app.signingCertificateSha256.take(23)}...",
                    sourceApi = "PackageManager.getInstallSourceInfo",
                    limitationNote = "Sideloading is common for developers and open-source tools. Use Trust Manager to mark as known tool."
                )
            )
        }
    }

    private fun isRuleExempt(targetId: String, ruleId: String, exceptions: List<TrustException>): Boolean {
        return exceptions.any { ex ->
            ex.enabled && ex.packageName == targetId && (ex.ruleId == ruleId || ex.ruleId == "ALL_RULES")
        }
    }
}
