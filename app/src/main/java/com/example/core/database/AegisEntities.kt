package com.example.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.example.core.model.CheckStatus
import com.example.core.model.EvidenceConfidence
import com.example.core.model.FindingSeverity
import com.example.core.model.FindingStatus
import com.example.core.model.RemediationType
import com.example.core.model.ScanStatus
import com.example.core.model.ScanType

@Entity(tableName = "scan_runs")
data class ScanRunEntity(
    @PrimaryKey val id: String,
    val startedAt: Long,
    val completedAt: Long?,
    val scanType: String,
    val platform: String,
    val osVersion: String,
    val securityPatch: String,
    val ruleSetVersion: String,
    val status: String,
    val applicableChecks: Int,
    val completedChecks: Int,
    val unavailableChecks: Int
)

@Entity(tableName = "check_results")
data class CheckResultEntity(
    @PrimaryKey val id: String,
    val scanRunId: String,
    val checkId: String,
    val title: String,
    val status: String,
    val summary: String,
    val limitationCode: String,
    val observedAt: Long
)

@Entity(tableName = "app_snapshots")
data class AppSnapshotEntity(
    @PrimaryKey val id: String,
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
    val requestedPermissionsRaw: String, // comma-separated
    val grantedPermissionsRaw: String,   // comma-separated
    val exportedComponentsCount: Int
)

@Entity(tableName = "findings")
data class FindingEntity(
    @PrimaryKey val id: String,
    val ruleId: String,
    val ruleVersion: String,
    val scanRunId: String,
    val targetType: String,
    val targetId: String,
    val targetName: String,
    val title: String,
    val summary: String,
    val whyItMatters: String,
    val severity: String,
    val confidence: String,
    val recommendedAction: String,
    val remediationType: String,
    val status: String,
    val observedAt: Long,
    val evidenceObservation: String,
    val sourceApi: String,
    val limitationNote: String
)

@Entity(tableName = "trust_exceptions")
data class TrustExceptionEntity(
    @PrimaryKey val id: String,
    val platform: String,
    val targetType: String,
    val packageName: String,
    val signingCertificateSha256: String,
    val ruleId: String,
    val reason: String,
    val createdAt: Long,
    val expiresAt: Long?,
    val enabled: Boolean
)

class AegisTypeConverters {
    @TypeConverter
    fun fromStringList(list: List<String>?): String = list?.joinToString(";;") ?: ""

    @TypeConverter
    fun toStringList(data: String?): List<String> =
        if (data.isNullOrBlank()) emptyList() else data.split(";;")
}
