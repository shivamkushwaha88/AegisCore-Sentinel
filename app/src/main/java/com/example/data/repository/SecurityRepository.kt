package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.core.database.AegisDatabase
import com.example.core.database.AppSnapshotEntity
import com.example.core.database.CheckResultEntity
import com.example.core.database.FindingEntity
import com.example.core.database.ScanRunEntity
import com.example.core.database.TrustExceptionEntity
import com.example.core.model.ApkPreflightReport
import com.example.core.model.AppSnapshot
import com.example.core.model.CheckResult
import com.example.core.model.CheckStatus
import com.example.core.model.EvidenceConfidence
import com.example.core.model.Finding
import com.example.core.model.FindingSeverity
import com.example.core.model.FindingStatus
import com.example.core.model.RemediationType
import com.example.core.model.ScanRun
import com.example.core.model.ScanStatus
import com.example.core.model.ScanType
import com.example.core.model.TrustException
import com.example.core.rules.VersionedRuleEvaluator
import com.example.platform.ApkPreflightParser
import com.example.platform.DevicePostureCollector
import com.example.platform.PackageCollector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class SecurityRepository(private val context: Context) {

    private val db = AegisDatabase.getInstance(context)
    private val packageCollector = PackageCollector(context)
    private val postureCollector = DevicePostureCollector(context)
    private val preflightParser = ApkPreflightParser(context)
    private val ruleEvaluator = VersionedRuleEvaluator()

    val latestScanRun: Flow<ScanRun?> = db.scanRunDao().getLatestScanRun().map { entity ->
        entity?.toDomain()
    }

    val allFindings: Flow<List<Finding>> = db.findingDao().getAllFindings().map { list ->
        list.map { it.toDomain() }
    }

    val trustExceptions: Flow<List<TrustException>> = db.trustExceptionDao().getAllExceptions().map { list ->
        list.map { it.toDomain() }
    }

    val recentApps: Flow<List<AppSnapshot>> = db.appSnapshotDao().getRecentSnapshots().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun runAudit(
        isQuick: Boolean = false,
        onProgress: suspend (step: String, fraction: Float) -> Unit
    ): ScanRun = withContext(Dispatchers.IO) {
        val scanId = UUID.randomUUID().toString()
        val startTime = System.currentTimeMillis()

        onProgress("Initializing Security Sentinel...", 0.10f)

        // 1. Collect Device Posture
        onProgress("Checking device security posture...", 0.25f)
        val posture = postureCollector.collectPosture()
        val postureChecks = postureCollector.evaluatePostureChecks(scanId, posture)

        // 2. Collect Installed Packages
        onProgress("Auditing installed packages and permissions...", 0.50f)
        val allApps = packageCollector.collectInstalledApps(scanId)
        val appsToAudit = if (isQuick) allApps.filter { !it.isSystemApp } else allApps

        // 3. Evaluate Rule Engine with active Trust Exceptions
        onProgress("Evaluating vulnerability rules and trust exceptions...", 0.75f)
        val activeExceptions = db.trustExceptionDao().getActiveExceptions().first().map { it.toDomain() }
        val findings = ruleEvaluator.evaluateAll(scanId, appsToAudit, posture, activeExceptions)

        // Calculate checks
        val applicableChecks = postureChecks.size + (appsToAudit.size * 4)
        val unavailableChecks = postureChecks.count { it.status == CheckStatus.NOT_OBSERVABLE }
        val completedChecks = applicableChecks - unavailableChecks

        val completedScanRun = ScanRun(
            id = scanId,
            startedAt = startTime,
            completedAt = System.currentTimeMillis(),
            scanType = if (isQuick) ScanType.QUICK else ScanType.FULL,
            platform = "ANDROID",
            osVersion = "Android API ${posture.apiLevel}",
            securityPatch = posture.securityPatch,
            ruleSetVersion = VersionedRuleEvaluator.RULE_SET_VERSION,
            status = ScanStatus.COMPLETED,
            applicableChecks = applicableChecks,
            completedChecks = completedChecks,
            unavailableChecks = unavailableChecks
        )

        // Persist to Room
        onProgress("Persisting local audit report...", 0.90f)
        db.scanRunDao().insertScanRun(completedScanRun.toEntity())
        db.checkResultDao().insertAll(postureChecks.map { it.toEntity() })
        db.appSnapshotDao().insertAll(appsToAudit.map { it.toEntity() })
        db.findingDao().insertAll(findings.map { it.toEntity() })

        onProgress("Audit complete", 1.0f)
        completedScanRun
    }

    suspend fun addTrustException(exception: TrustException) = withContext(Dispatchers.IO) {
        db.trustExceptionDao().insertException(exception.toEntity())
        // Mark existing matching findings as dismissed
        val findings = db.findingDao().getAllFindings().first()
        for (f in findings) {
            if (f.targetId == exception.packageName && (exception.ruleId == f.ruleId || exception.ruleId == "ALL_RULES")) {
                db.findingDao().updateStatus(f.id, FindingStatus.DISMISSED.name)
            }
        }
    }

    suspend fun toggleTrustException(id: String, enabled: Boolean) = withContext(Dispatchers.IO) {
        db.trustExceptionDao().updateEnabled(id, enabled)
    }

    suspend fun deleteTrustException(id: String) = withContext(Dispatchers.IO) {
        db.trustExceptionDao().deleteById(id)
    }

    suspend fun updateFindingStatus(id: String, status: FindingStatus) = withContext(Dispatchers.IO) {
        db.findingDao().updateStatus(id, status.name)
    }

    fun preflightApk(uri: Uri): Result<ApkPreflightReport> {
        return preflightParser.parseApkFromUri(uri)
    }

    suspend fun generateSanitizedReport(): String = withContext(Dispatchers.IO) {
        val latest = latestScanRun.first()
        val findings = allFindings.first()
        val exceptions = trustExceptions.first()

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val dateStr = sdf.format(Date(latest?.startedAt ?: System.currentTimeMillis()))

        val sb = StringBuilder()
        sb.appendLine("==================================================")
        sb.appendLine("           AEGISCORE SENTINEL AUDIT REPORT        ")
        sb.appendLine("==================================================")
        sb.appendLine("Generated: $dateStr")
        sb.appendLine("Platform: Android (${latest?.osVersion ?: "N/A"})")
        sb.appendLine("Security Patch Level: ${latest?.securityPatch ?: "N/A"}")
        sb.appendLine("Rule Set: ${latest?.ruleSetVersion ?: "ANDROID_BASELINE_V1"}")
        sb.appendLine("Coverage: ${latest?.completedChecks ?: 0} / ${latest?.applicableChecks ?: 0} checks completed")
        sb.appendLine("Total Findings Recorded: ${findings.size}")
        sb.appendLine("Active Trust Exceptions: ${exceptions.count { it.enabled }}")
        sb.appendLine("--------------------------------------------------")
        sb.appendLine("FINDINGS SUMMARY:")
        if (findings.isEmpty()) {
            sb.appendLine("No high-priority findings detected in the areas inspected.")
        } else {
            findings.forEachIndexed { i, f ->
                sb.appendLine("\n[#${i + 1}] [${f.severity}] [${f.confidence}] ${f.title}")
                sb.appendLine("Target: ${f.targetName} (${f.targetId})")
                sb.appendLine("Summary: ${f.summary}")
                sb.appendLine("Why it matters: ${f.whyItMatters}")
                sb.appendLine("Evidence: ${f.evidenceObservation}")
                sb.appendLine("Status: ${f.status} | Recommended Action: ${f.recommendedAction}")
                if (f.limitationNote.isNotBlank()) {
                    sb.appendLine("Limitation: ${f.limitationNote}")
                }
            }
        }
        sb.appendLine("\n--------------------------------------------------")
        sb.appendLine("DEVELOPER / AI TRUST EXCEPTIONS:")
        if (exceptions.isEmpty()) {
            sb.appendLine("No trust exceptions configured.")
        } else {
            exceptions.forEach { ex ->
                sb.appendLine("- ${ex.packageName} [Rule: ${ex.ruleId}] Enabled: ${ex.enabled}")
                sb.appendLine("  Rationale: ${ex.reason}")
            }
        }
        sb.appendLine("\n==================================================")
        sb.appendLine("Note: Generated locally on-device. Zero telemetry was transmitted.")
        sb.appendLine("==================================================")
        sb.toString()
    }

    // Mapping extensions
    private fun ScanRunEntity.toDomain() = ScanRun(
        id = id,
        startedAt = startedAt,
        completedAt = completedAt,
        scanType = ScanType.valueOf(scanType),
        platform = platform,
        osVersion = osVersion,
        securityPatch = securityPatch,
        ruleSetVersion = ruleSetVersion,
        status = ScanStatus.valueOf(status),
        applicableChecks = applicableChecks,
        completedChecks = completedChecks,
        unavailableChecks = unavailableChecks
    )

    private fun ScanRun.toEntity() = ScanRunEntity(
        id = id,
        startedAt = startedAt,
        completedAt = completedAt,
        scanType = scanType.name,
        platform = platform,
        osVersion = osVersion,
        securityPatch = securityPatch,
        ruleSetVersion = ruleSetVersion,
        status = status.name,
        applicableChecks = applicableChecks,
        completedChecks = completedChecks,
        unavailableChecks = unavailableChecks
    )

    private fun CheckResult.toEntity() = CheckResultEntity(
        id = id,
        scanRunId = scanRunId,
        checkId = checkId,
        title = title,
        status = status.name,
        summary = summary,
        limitationCode = limitationCode,
        observedAt = observedAt
    )

    private fun AppSnapshot.toEntity() = AppSnapshotEntity(
        id = id,
        scanRunId = scanRunId,
        packageName = packageName,
        displayName = displayName,
        versionName = versionName,
        versionCode = versionCode,
        targetSdk = targetSdk,
        installerPackage = installerPackage,
        signingCertificateSha256 = signingCertificateSha256,
        firstInstallTime = firstInstallTime,
        lastUpdateTime = lastUpdateTime,
        isSystemApp = isSystemApp,
        requestedPermissionsRaw = requestedPermissions.joinToString(";;"),
        grantedPermissionsRaw = grantedPermissions.joinToString(";;"),
        exportedComponentsCount = exportedComponentsCount
    )

    private fun AppSnapshotEntity.toDomain() = AppSnapshot(
        id = id,
        scanRunId = scanRunId,
        packageName = packageName,
        displayName = displayName,
        versionName = versionName,
        versionCode = versionCode,
        targetSdk = targetSdk,
        installerPackage = installerPackage,
        signingCertificateSha256 = signingCertificateSha256,
        firstInstallTime = firstInstallTime,
        lastUpdateTime = lastUpdateTime,
        isSystemApp = isSystemApp,
        requestedPermissions = if (requestedPermissionsRaw.isBlank()) emptyList() else requestedPermissionsRaw.split(";;"),
        grantedPermissions = if (grantedPermissionsRaw.isBlank()) emptyList() else grantedPermissionsRaw.split(";;"),
        exportedComponentsCount = exportedComponentsCount
    )

    private fun Finding.toEntity() = FindingEntity(
        id = id,
        ruleId = ruleId,
        ruleVersion = ruleVersion,
        scanRunId = scanRunId,
        targetType = targetType,
        targetId = targetId,
        targetName = targetName,
        title = title,
        summary = summary,
        whyItMatters = whyItMatters,
        severity = severity.name,
        confidence = confidence.name,
        recommendedAction = recommendedAction,
        remediationType = remediationType.name,
        status = status.name,
        observedAt = observedAt,
        evidenceObservation = evidenceObservation,
        sourceApi = sourceApi,
        limitationNote = limitationNote
    )

    private fun FindingEntity.toDomain() = Finding(
        id = id,
        ruleId = ruleId,
        ruleVersion = ruleVersion,
        scanRunId = scanRunId,
        targetType = targetType,
        targetId = targetId,
        targetName = targetName,
        title = title,
        summary = summary,
        whyItMatters = whyItMatters,
        severity = FindingSeverity.valueOf(severity),
        confidence = EvidenceConfidence.valueOf(confidence),
        recommendedAction = recommendedAction,
        remediationType = RemediationType.valueOf(remediationType),
        status = FindingStatus.valueOf(status),
        observedAt = observedAt,
        evidenceObservation = evidenceObservation,
        sourceApi = sourceApi,
        limitationNote = limitationNote
    )

    private fun TrustException.toEntity() = TrustExceptionEntity(
        id = id,
        platform = platform,
        targetType = targetType,
        packageName = packageName,
        signingCertificateSha256 = signingCertificateSha256,
        ruleId = ruleId,
        reason = reason,
        createdAt = createdAt,
        expiresAt = expiresAt,
        enabled = enabled
    )

    private fun TrustExceptionEntity.toDomain() = TrustException(
        id = id,
        platform = platform,
        targetType = targetType,
        packageName = packageName,
        signingCertificateSha256 = signingCertificateSha256,
        ruleId = ruleId,
        reason = reason,
        createdAt = createdAt,
        expiresAt = expiresAt,
        enabled = enabled
    )
}
