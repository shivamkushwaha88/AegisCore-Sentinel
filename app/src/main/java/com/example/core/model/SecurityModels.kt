package com.example.core.model

import java.util.UUID

enum class ScanType {
    QUICK,
    FULL,
    APK_PREFLIGHT
}

enum class ScanStatus {
    IDLE,
    RUNNING,
    COMPLETED,
    PARTIAL,
    FAILED
}

enum class CheckStatus {
    PASS,
    REVIEW,
    NOT_OBSERVABLE,
    NEEDS_USER_ACCESS,
    ERROR
}

enum class FindingSeverity {
    CRITICAL,
    HIGH,
    MODERATE,
    LOW,
    INFORMATIONAL
}

enum class EvidenceConfidence {
    CONFIRMED,
    STRONG_INDICATOR,
    WEAK_INDICATOR,
    INSUFFICIENT_DATA
}

enum class RemediationType {
    OPEN_SETTINGS,
    UNINSTALL_APP,
    INSPECT_FILE,
    TRUST_EXCEPTION
}

enum class FindingStatus {
    NEW,
    ACTION_PENDING,
    RESOLVED,
    DISMISSED
}

data class ScanRun(
    val id: String = UUID.randomUUID().toString(),
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val scanType: ScanType = ScanType.FULL,
    val platform: String = "ANDROID",
    val osVersion: String = "",
    val securityPatch: String = "",
    val ruleSetVersion: String = "ANDROID_BASELINE_V1",
    val status: ScanStatus = ScanStatus.RUNNING,
    val applicableChecks: Int = 0,
    val completedChecks: Int = 0,
    val unavailableChecks: Int = 0
)

data class CheckResult(
    val id: String = UUID.randomUUID().toString(),
    val scanRunId: String,
    val checkId: String,
    val title: String,
    val status: CheckStatus,
    val summary: String,
    val limitationCode: String = "",
    val observedAt: Long = System.currentTimeMillis()
)

data class AppSnapshot(
    val id: String = UUID.randomUUID().toString(),
    val scanRunId: String,
    val packageName: String,
    val displayName: String,
    val versionName: String,
    val versionCode: Long,
    val targetSdk: Int,
    val installerPackage: String?,
    val signingCertificateSha256: String,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val isSystemApp: Boolean,
    val requestedPermissions: List<String> = emptyList(),
    val grantedPermissions: List<String> = emptyList(),
    val exportedComponentsCount: Int = 0
)

data class Finding(
    val id: String = UUID.randomUUID().toString(),
    val ruleId: String,
    val ruleVersion: String = "1.0",
    val scanRunId: String,
    val targetType: String, // "APP", "DEVICE", "FILE"
    val targetId: String, // Package name, URI, etc.
    val targetName: String, // App label or file name
    val title: String,
    val summary: String,
    val whyItMatters: String,
    val severity: FindingSeverity,
    val confidence: EvidenceConfidence,
    val recommendedAction: String,
    val remediationType: RemediationType,
    val status: FindingStatus = FindingStatus.NEW,
    val observedAt: Long = System.currentTimeMillis(),
    val evidenceObservation: String = "",
    val sourceApi: String = "PackageManager",
    val limitationNote: String = ""
)

data class TrustException(
    val id: String = UUID.randomUUID().toString(),
    val platform: String = "ANDROID",
    val targetType: String = "APP",
    val packageName: String,
    val signingCertificateSha256: String = "",
    val ruleId: String, // Scoped to rule or "ALL_RULES"
    val reason: String,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null,
    val enabled: Boolean = true
)

data class DevicePosture(
    val apiLevel: Int,
    val securityPatch: String,
    val isLockSecured: Boolean,
    val isDebugActive: Boolean,
    val buildModel: String,
    val buildFingerprint: String
)

data class ApkPreflightReport(
    val fileName: String,
    val packageName: String,
    val appLabel: String,
    val versionName: String,
    val versionCode: Long,
    val targetSdk: Int,
    val certSha256: String,
    val fileSha256: String,
    val requestedPermissions: List<String>,
    val sensitivePermissions: List<String>,
    val isDebuggable: Boolean,
    val allowsBackup: Boolean,
    val exportedComponentsCount: Int,
    val fileSizeFormatted: String
)
