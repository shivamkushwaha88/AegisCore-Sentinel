package com.example.platform

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.example.core.model.AppSnapshot
import java.security.MessageDigest

class PackageCollector(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    // Sensitive permissions that warrant security review
    val sensitivePermissionsSet = setOf(
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.ACCESS_BACKGROUND_LOCATION",
        "android.permission.CAMERA",
        "android.permission.RECORD_AUDIO",
        "android.permission.READ_CONTACTS",
        "android.permission.WRITE_CONTACTS",
        "android.permission.READ_SMS",
        "android.permission.SEND_SMS",
        "android.permission.RECEIVE_SMS",
        "android.permission.READ_CALL_LOG",
        "android.permission.WRITE_CALL_LOG",
        "android.permission.READ_PHONE_STATE",
        "android.permission.SYSTEM_ALERT_WINDOW",
        "android.permission.REQUEST_INSTALL_PACKAGES",
        "android.permission.READ_EXTERNAL_STORAGE",
        "android.permission.WRITE_EXTERNAL_STORAGE",
        "android.permission.MANAGE_EXTERNAL_STORAGE",
        "android.permission.POST_NOTIFICATIONS"
    )

    fun collectInstalledApps(scanRunId: String): List<AppSnapshot> {
        val flags = PackageManager.GET_PERMISSIONS or
                PackageManager.GET_ACTIVITIES or
                PackageManager.GET_SERVICES or
                PackageManager.GET_RECEIVERS or
                PackageManager.GET_PROVIDERS or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    PackageManager.GET_SIGNING_CERTIFICATES
                } else {
                    @Suppress("DEPRECATION")
                    PackageManager.GET_SIGNATURES
                }

        val installedPackages: List<PackageInfo> = try {
            packageManager.getInstalledPackages(flags)
        } catch (e: Exception) {
            emptyList()
        }

        return installedPackages.map { pkg ->
            val appInfo = pkg.applicationInfo
            val isSystem = if (appInfo != null) {
                (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                        (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            } else false

            val displayName = try {
                appInfo?.loadLabel(packageManager)?.toString() ?: pkg.packageName
            } catch (e: Exception) {
                pkg.packageName
            }

            val (requested, granted) = extractPermissions(pkg)
            val certSha256 = extractSigningCertSha256(pkg)
            val installer = getInstallerPackage(pkg.packageName)
            val exportedCount = countExportedComponents(pkg)
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkg.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pkg.versionCode.toLong()
            }

            AppSnapshot(
                scanRunId = scanRunId,
                packageName = pkg.packageName,
                displayName = displayName,
                versionName = pkg.versionName ?: "Unknown",
                versionCode = versionCode,
                targetSdk = appInfo?.targetSdkVersion ?: 0,
                installerPackage = installer,
                signingCertificateSha256 = certSha256,
                firstInstallTime = pkg.firstInstallTime,
                lastUpdateTime = pkg.lastUpdateTime,
                isSystemApp = isSystem,
                requestedPermissions = requested,
                grantedPermissions = granted,
                exportedComponentsCount = exportedCount
            )
        }
    }

    private fun extractPermissions(pkg: PackageInfo): Pair<List<String>, List<String>> {
        val requested = pkg.requestedPermissions?.toList() ?: emptyList()
        val flags = pkg.requestedPermissionsFlags
        val granted = mutableListOf<String>()

        if (flags != null && requested.size == flags.size) {
            for (i in requested.indices) {
                if ((flags[i] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0) {
                    granted.add(requested[i])
                }
            }
        }
        return Pair(requested, granted)
    }

    private fun extractSigningCertSha256(pkg: PackageInfo): String {
        return try {
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkg.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                pkg.signatures
            }

            if (!signatures.isNullOrEmpty()) {
                val certBytes = signatures[0].toByteArray()
                val md = MessageDigest.getInstance("SHA-256")
                val digest = md.digest(certBytes)
                digest.joinToString(":") { "%02X".format(it) }
            } else {
                "UNAVAILABLE"
            }
        } catch (e: Exception) {
            "ERROR_PARSING_CERT"
        }
    }

    private fun getInstallerPackage(packageName: String): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                packageManager.getInstallSourceInfo(packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstallerPackageName(packageName)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun countExportedComponents(pkg: PackageInfo): Int {
        var count = 0
        pkg.activities?.forEach { if (it.exported && it.permission.isNullOrBlank()) count++ }
        pkg.services?.forEach { if (it.exported && it.permission.isNullOrBlank()) count++ }
        pkg.receivers?.forEach { if (it.exported && it.permission.isNullOrBlank()) count++ }
        pkg.providers?.forEach { if (it.exported && it.readPermission.isNullOrBlank() && it.writePermission.isNullOrBlank()) count++ }
        return count
    }
}
