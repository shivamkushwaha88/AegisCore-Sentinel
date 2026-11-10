package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.FindingSeverity
import com.example.core.model.FindingStatus
import androidx.compose.foundation.lazy.rememberLazyListState
import com.example.ui.components.verticalScrollbar
import com.example.ui.components.FindingCard
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
import com.example.ui.theme.AegisTrust
import com.example.ui.theme.AegisVoid
import com.example.viewmodel.NavigationTab
import com.example.viewmodel.ScanUiState
import com.example.viewmodel.SentinelViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun OverviewScreen(
    viewModel: SentinelViewModel,
    onNavigateToTab: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val scanState by viewModel.scanUiState.collectAsState()
    val latestScan by viewModel.latestScanRun.collectAsState()
    val findings by viewModel.filteredFindings.collectAsState()
    val allFindings by viewModel.allFindings.collectAsState()
    val selectedFilter by viewModel.severityFilter.collectAsState()
    val apps by viewModel.allApps.collectAsState()
    val exceptions by viewModel.trustExceptions.collectAsState()

    val activeFindings = allFindings.filter { it.status == FindingStatus.NEW }
    val criticalOrHighCount = activeFindings.count { it.severity == FindingSeverity.CRITICAL || it.severity == FindingSeverity.HIGH }
    val moderateCount = activeFindings.count { it.severity == FindingSeverity.MODERATE }

    val coveragePercent = if (latestScan != null && latestScan!!.applicableChecks > 0) {
        ((latestScan!!.completedChecks.toFloat() / latestScan!!.applicableChecks) * 100).toInt()
    } else 0

    val lastAuditDate = latestScan?.completedAt?.let {
        SimpleDateFormat("MMM dd, HH:mm", Locale.US).format(Date(it))
    } ?: "Pending Audit"

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
            // 1. Truthful Assessment Banner
            AssessmentBanner(
                criticalOrHighCount = criticalOrHighCount,
                moderateCount = moderateCount,
                coveragePercent = coveragePercent,
                lastAuditDate = lastAuditDate
            )
        }

        // Live Scanning Progress
        item {
            if (scanState is ScanUiState.Scanning) {
                val scanning = scanState as ScanUiState.Scanning
                ScanningProgressCard(stepName = scanning.stepName, progress = scanning.progressFraction)
            }
        }

        // 2. Audit Actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.runAudit(isQuick = false) },
                    enabled = scanState !is ScanUiState.Scanning,
                    colors = ButtonDefaults.buttonColors(containerColor = AegisHealthy),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_run_full_audit")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = AegisVoid,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Run Full Audit",
                        color = AegisVoid,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = { viewModel.runAudit(isQuick = true) },
                    enabled = scanState !is ScanUiState.Scanning,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AegisTextPrimary),
                    border = BorderStroke(1.dp, AegisBorder),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_quick_scan")
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = AegisCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Quick Scan",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // 3. Platform Modules Grid
        item {
            Text(
                text = "AUDIT COVERAGE & MODULES",
                color = AegisTextTertiary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ModuleCard(
                    title = "App Risk Explorer",
                    stat = "${apps.size} visible apps",
                    subtext = "${apps.count { it.grantedPermissions.any { p -> p.contains("LOCATION") || p.contains("CAMERA") } }} sensitive",
                    icon = Icons.Default.Apps,
                    tint = AegisCyan,
                    onClick = { onNavigateToTab(NavigationTab.APPS) },
                    modifier = Modifier.weight(1f)
                )

                ModuleCard(
                    title = "Device Posture",
                    stat = latestScan?.osVersion ?: "Android",
                    subtext = "Patch: ${latestScan?.securityPatch?.take(10) ?: "Checked"}",
                    icon = Icons.Default.Security,
                    tint = AegisHealthy,
                    onClick = { },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ModuleCard(
                    title = "APK Preflight",
                    stat = "Safe SAF Parser",
                    subtext = "Verify before install",
                    icon = Icons.Default.Security,
                    tint = AegisReview,
                    onClick = { onNavigateToTab(NavigationTab.INSPECT) },
                    modifier = Modifier.weight(1f)
                )

                ModuleCard(
                    title = "Trust Exceptions",
                    stat = "${exceptions.count { it.enabled }} active",
                    subtext = "Developer risk notes",
                    icon = Icons.Default.Policy,
                    tint = AegisTrust,
                    onClick = { onNavigateToTab(NavigationTab.TRUST) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 4. Priority Findings Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EVIDENCE-BACKED FINDINGS (${findings.size})",
                    color = AegisTextTertiary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))

            // Severity Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    null to "All",
                    FindingSeverity.CRITICAL to "Critical",
                    FindingSeverity.HIGH to "High",
                    FindingSeverity.MODERATE to "Moderate"
                ).forEach { (severity, label) ->
                    val isSelected = selectedFilter == severity
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setSeverityFilter(severity) },
                        label = { Text(text = label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = AegisSurface,
                            labelColor = AegisTextSecondary,
                            selectedContainerColor = AegisSurfaceVariant,
                            selectedLabelColor = AegisTextPrimary
                        ),
                        border = BorderStroke(1.dp, if (isSelected) AegisHealthy else AegisBorder)
                    )
                }
            }
        }

        if (findings.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AegisSurface),
                    border = BorderStroke(1.dp, AegisBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = AegisHealthy,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No High-Priority Findings",
                            color = AegisTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "No evidence-backed exposures detected in the inspected areas. Continue reviewing app permissions periodically.",
                            color = AegisTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        } else {
            items(findings, key = { it.id }) { finding ->
                FindingCard(
                    finding = finding,
                    onAddTrustException = { pkg, ruleId ->
                        viewModel.addTrustException(pkg, ruleId, "User marked developer exception")
                    },
                    onDismissFinding = { id ->
                        viewModel.dismissFinding(id)
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun AssessmentBanner(
    criticalOrHighCount: Int,
    moderateCount: Int,
    coveragePercent: Int,
    lastAuditDate: String,
    modifier: Modifier = Modifier
) {
    val needsAttention = criticalOrHighCount > 0 || moderateCount > 0
    val bannerBg = if (needsAttention) AegisSurface else AegisSurface
    val bannerBorder = if (criticalOrHighCount > 0) AegisCritical else if (moderateCount > 0) AegisReview else AegisHealthy

    Card(
        colors = CardDefaults.cardColors(containerColor = bannerBg),
        border = BorderStroke(1.dp, bannerBorder.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (needsAttention) AegisReview else AegisHealthy)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (needsAttention) "Assessment: Review Recommended" else "Assessment: Inspected Areas Clean",
                        color = AegisTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(AegisSurfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "$coveragePercent% Coverage",
                        color = AegisHealthy,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (needsAttention) {
                    "$criticalOrHighCount high-priority items and $moderateCount moderate items need review."
                } else {
                    "No high-priority findings were detected in the areas inspected."
                },
                color = AegisTextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Last full audit: $lastAuditDate",
                    color = AegisTextTertiary,
                    fontSize = 11.sp
                )
                Text(
                    text = "Network monitor: Snapshot",
                    color = AegisTextTertiary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun ScanningProgressCard(stepName: String, progress: Float, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AegisSurface),
        border = BorderStroke(1.dp, AegisCyan.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stepName,
                    color = AegisCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    color = AegisCyan,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                color = AegisCyan,
                trackColor = AegisSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
fun ModuleCard(
    title: String,
    stat: String,
    subtext: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AegisSurface),
        border = BorderStroke(1.dp, AegisBorder),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    color = AegisTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stat,
                color = AegisTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtext,
                color = AegisTextTertiary,
                fontSize = 10.sp
            )
        }
    }
}
