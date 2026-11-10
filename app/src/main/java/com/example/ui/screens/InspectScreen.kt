package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import com.example.ui.components.verticalScrollbar
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.ApkPreflightReport
import com.example.ui.theme.AegisBorder
import com.example.ui.theme.AegisCritical
import com.example.ui.theme.AegisCriticalContainer
import com.example.ui.theme.AegisCyan
import com.example.ui.theme.AegisHealthy
import com.example.ui.theme.AegisHealthyContainer
import com.example.ui.theme.AegisReview
import com.example.ui.theme.AegisReviewContainer
import com.example.ui.theme.AegisSurface
import com.example.ui.theme.AegisSurfaceVariant
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
import com.example.ui.theme.AegisTextTertiary
import com.example.ui.theme.AegisVoid
import com.example.viewmodel.PreflightUiState
import com.example.viewmodel.SentinelViewModel

@Composable
fun InspectScreen(
    viewModel: SentinelViewModel,
    modifier: Modifier = Modifier
) {
    val preflightState by viewModel.preflightUiState.collectAsState()
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.inspectApk(it) }
    }

    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(AegisVoid)
            .verticalScrollbar(listState)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "APK PREFLIGHT INSPECTOR",
                color = AegisTextTertiary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Inspect APK metadata, signature hashes, and sensitive permissions before installation.",
                color = AegisTextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }

        when (val state = preflightState) {
            is PreflightUiState.Idle -> {
                item {
                    ApkSelectionPlaceholder(
                        onSelectFile = { filePickerLauncher.launch("*/*") }
                    )
                }
            }

            is PreflightUiState.Loading -> {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AegisSurface),
                        border = BorderStroke(1.dp, AegisCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = AegisCyan, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Inspecting Package Manifest & Signatures...",
                                color = AegisTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Bounded read in memory · No execution",
                                color = AegisTextTertiary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            is PreflightUiState.Success -> {
                item {
                    ApkReportCard(
                        report = state.report,
                        onReset = { viewModel.clearPreflight() }
                    )
                }
            }

            is PreflightUiState.Error -> {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AegisCriticalContainer),
                        border = BorderStroke(1.dp, AegisCritical),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = AegisCritical, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Inspection Error", color = AegisCritical, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = state.message, color = AegisTextPrimary, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.clearPreflight() },
                                colors = ButtonDefaults.buttonColors(containerColor = AegisSurfaceVariant),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Try Another File", color = AegisTextPrimary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        item {
            // Safety and Privacy Guarantee
            Card(
                colors = CardDefaults.cardColors(containerColor = AegisSurface),
                border = BorderStroke(1.dp, AegisBorder),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "SAFE PREFLIGHT GUARANTEE",
                        color = AegisTextTertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "AegisCore uses the Storage Access Framework (SAF) to inspect only the file you explicitly select. It calculates SHA-256 digests and extracts binary AndroidManifest declarations in memory without executing code or communicating with external servers.",
                        color = AegisTextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun ApkSelectionPlaceholder(onSelectFile: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AegisSurface),
        border = BorderStroke(1.dp, AegisCyan.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelectFile() }
            .testTag("btn_select_apk")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(AegisCyan.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FileUpload,
                    contentDescription = "Select APK",
                    tint = AegisCyan,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Select Android Package (APK)",
                color = AegisTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tap to open system file picker and preflight permissions",
                color = AegisTextSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onSelectFile,
                colors = ButtonDefaults.buttonColors(containerColor = AegisCyan),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "Choose File", color = AegisVoid, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ApkReportCard(
    report: ApkPreflightReport,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        colors = CardDefaults.cardColors(containerColor = AegisSurface),
        border = BorderStroke(1.dp, AegisBorder),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = report.appLabel,
                        color = AegisTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = report.packageName,
                        color = AegisCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(AegisSurfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = report.fileSizeFormatted,
                        color = AegisTextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Metadata box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AegisVoid)
                    .padding(10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PreflightMetaRow("Target SDK", "API ${report.targetSdk} ${if (report.targetSdk < 30) "(Legacy)" else "(Modern)"}")
                    PreflightMetaRow("Version", "${report.versionName} (${report.versionCode})")
                    PreflightMetaRow("Debuggable Flag", if (report.isDebuggable) "TRUE (Warning: debug build)" else "False")
                    PreflightMetaRow("Allow Backup", if (report.allowsBackup) "Enabled" else "Disabled")
                    PreflightMetaRow("Exported Components", "${report.exportedComponentsCount} declared")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("File SHA-256", color = AegisTextTertiary, fontSize = 11.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(report.fileSha256.take(12) + "...", color = AegisTextPrimary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            IconButton(
                                onClick = {
                                    val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cb.setPrimaryClip(ClipData.newPlainText("SHA256", report.fileSha256))
                                    Toast.makeText(context, "SHA-256 copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = AegisTextSecondary, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sensitive Permissions Warning Box
            if (report.sensitivePermissions.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(AegisReviewContainer)
                        .padding(10.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = AegisReview, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${report.sensitivePermissions.size} HIGH-IMPACT PERMISSIONS DECLARED",
                                color = AegisReview,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        report.sensitivePermissions.forEach { perm ->
                            Text(
                                text = "• " + perm.substringAfterLast("."),
                                color = AegisTextPrimary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(AegisHealthyContainer)
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = AegisHealthy, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "No dangerous runtime capabilities declared in manifest.",
                            color = AegisHealthy,
                            fontSize = 11.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            OutlinedButton(
                onClick = onReset,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AegisTextPrimary),
                border = BorderStroke(1.dp, AegisBorder),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Inspect Another Package", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun PreflightMetaRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = AegisTextTertiary, fontSize = 11.sp)
        Text(text = value, color = AegisTextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}
