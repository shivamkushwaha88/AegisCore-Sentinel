package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.rememberLazyListState
import com.example.ui.components.verticalScrollbar
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import com.example.ui.theme.AegisBorder
import com.example.ui.theme.AegisCyan
import com.example.ui.theme.AegisHealthy
import com.example.ui.theme.AegisSurface
import com.example.ui.theme.AegisSurfaceVariant
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
import com.example.ui.theme.AegisTextTertiary
import com.example.ui.theme.AegisVoid
import com.example.viewmodel.SentinelViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: SentinelViewModel,
    modifier: Modifier = Modifier
) {
    val latestScan by viewModel.latestScanRun.collectAsState()
    val allFindings by viewModel.allFindings.collectAsState()
    val context = LocalContext.current

    val lastAuditDate = latestScan?.completedAt?.let {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(it))
    } ?: "No audits completed yet"

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
                text = "AUDIT TIMELINE & EVIDENCE LOG",
                color = AegisTextTertiary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Persistent on-device audit snapshots with exportable evidence reports.",
                color = AegisTextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }

        // Export Report Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = AegisSurface),
                border = BorderStroke(1.dp, AegisCyan.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Export Sanitized Report",
                                color = AegisTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Formatted evidence report for offline review",
                                color = AegisTextTertiary,
                                fontSize = 11.sp
                            )
                        }
                        Button(
                            onClick = {
                                viewModel.exportReport { reportText ->
                                    shareReportText(context, reportText)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AegisCyan),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("btn_export_audit_report")
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = AegisVoid, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export", color = AegisVoid, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Latest Snapshot Details
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = AegisSurface),
                border = BorderStroke(1.dp, AegisBorder),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "MOST RECENT AUDIT SNAPSHOT",
                        color = AegisTextTertiary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(AegisVoid)
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            HistoryRow("Snapshot Timestamp", lastAuditDate)
                            HistoryRow("Platform Target", "Android (${latestScan?.osVersion ?: "N/A"})")
                            HistoryRow("Security Patch", latestScan?.securityPatch ?: "N/A")
                            HistoryRow("Rule Set Version", latestScan?.ruleSetVersion ?: "ANDROID_BASELINE_V1")
                            HistoryRow("Checks Completed", "${latestScan?.completedChecks ?: 0} of ${latestScan?.applicableChecks ?: 0}")
                            HistoryRow("Recorded Findings", "${allFindings.size} items")
                        }
                    }
                }
            }
        }

        // Privacy & Architecture Commitments
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = AegisSurface),
                border = BorderStroke(1.dp, AegisBorder),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = AegisHealthy, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PRIVACY ARCHITECTURE COMMITMENTS",
                            color = AegisHealthy,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• 100% Offline: No servers, analytics endpoints, or cloud accounts.\n" +
                                "• Evidence Transparency: Every finding displays the specific API observation that triggered it.\n" +
                                "• Scoped Filesystem Access: AegisCore only inspects files you explicitly pick via Android SAF.\n" +
                                "• Honest Limitations: Unsupported or platform-restricted checks are reported as unavailable, never false green checkmarks.",
                        color = AegisTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HistoryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = AegisTextTertiary, fontSize = 11.sp)
        Text(text = value, color = AegisTextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}

private fun shareReportText(context: Context, text: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Share AegisCore Audit Report")
    shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(shareIntent)
}
