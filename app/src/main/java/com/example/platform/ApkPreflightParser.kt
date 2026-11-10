package com.example.platform

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.example.core.model.ApkPreflightReport
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

class ApkPreflightParser(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    private val sensitivePermissions = setOf(
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
        "android.permission.READ_PHONE_STATE",
        "android.permission.SYSTEM_ALERT_WINDOW",
        "android.permission.REQUEST_INSTALL_PACKAGES",
        "android.permission.READ_EXTERNAL_STORAGE",
        "android.permission.WRITE_EXTERNAL_STORAGE",
        "android.permission.MANAGE_EXTERNAL_STORAGE"
    )

    fun parseApkFromUri(uri: Uri): Result<ApkPreflightReport> {
        return try {
            val contentResolver = context.contentResolver
            val tempFile = File(context.cacheDir, "preflight_${System.currentTimeMillis()}.apk")

            // Bounded stream read (max 200MB)
            val maxBytes = 200L * 1024 * 1024
            var bytesReadTotal = 0L

            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        bytesReadTotal += read
                        if (bytesReadTotal > maxBytes) {
                            tempFile.delete()
                            return Result.failure(IllegalStateException("File exceeds 200MB preflight inspection safety limit."))
                        }
                        output.write(buffer, 0, read)
                    }
                }
            } ?: return Result.failure(IllegalStateException("Could not open input stream for selected APK."))

            val fileSha256 = calculateFileSha256(tempFile)
            val fileSizeFormatted = formatBytes(tempFile.length())

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

            val pkgInfo: PackageInfo? = packageManager.getPackageArchiveInfo(tempFile.absolutePath, flags)

            if (pkgInfo == null) {
                tempFile.delete()
                return Result.failure(IllegalArgumentException("Unable to parse Android package manifest. File may be malformed or not a valid Android APK."))
            }

            // Fix appInfo sourceDir for proper label loading
            val appInfo = pkgInfo.applicationInfo
            if (appInfo != null) {
                appInfo.sourceDir = tempFile.absolutePath
                appInfo.publicSourceDir = tempFile.absolutePath
            }

            val appLabel = try {
                appInfo?.loadLabel(packageManager)?.toString() ?: pkgInfo.packageName
            } catch (e: Exception) {
                pkgInfo.packageName
            }

            val requestedPerms = pkgInfo.requestedPermissions?.toList() ?: emptyList()
            val sensitivePerms = requestedPerms.filter { it in sensitivePermissions }

            val isDebuggable = if (appInfo != null) {
                (appInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
            } else false

            val allowsBackup = if (appInfo != null) {
                (appInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP) != 0
            } else true

            val certSha256 = extractSigningCert(pkgInfo)
            val exportedCount = countExportedComponents(pkgInfo)

            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkgInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pkgInfo.versionCode.toLong()
            }

            // Cleanup temp file
            tempFile.delete()

            Result.success(
                ApkPreflightReport(
                    fileName = uri.lastPathSegment ?: "selected_package.apk",
                    packageName = pkgInfo.packageName,
                    appLabel = appLabel,
                    versionName = pkgInfo.versionName ?: "Unknown",
                    versionCode = versionCode,
                    targetSdk = appInfo?.targetSdkVersion ?: 0,
                    certSha256 = certSha256,
                    fileSha256 = fileSha256,
                    requestedPermissions = requestedPerms,
                    sensitivePermissions = sensitivePerms,
                    isDebuggable = isDebuggable,
                    allowsBackup = allowsBackup,
                    exportedComponentsCount = exportedCount,
                    fileSizeFormatted = fileSizeFormatted
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun calculateFileSha256(file: File): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    md.update(buffer, 0, read)
                }
            }
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "HASH_ERROR"
        }
    }

    private fun extractSigningCert(pkg: PackageInfo): String {
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
                "UNAVAILABLE_OR_UNSIGNED"
            }
        } catch (e: Exception) {
            "SIGNATURE_PARSE_ERROR"
        }
    }

    private fun countExportedComponents(pkg: PackageInfo): Int {
        var count = 0
        pkg.activities?.forEach { if (it.exported) count++ }
        pkg.services?.forEach { if (it.exported) count++ }
        pkg.receivers?.forEach { if (it.exported) count++ }
        pkg.providers?.forEach { if (it.exported) count++ }
        return count
    }

    private fun formatBytes(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return if (mb >= 1.0) "%.1f MB".format(mb) else "%.1f KB".format(kb)
    }
}
