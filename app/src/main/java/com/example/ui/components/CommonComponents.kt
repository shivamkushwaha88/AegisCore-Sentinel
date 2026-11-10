package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.EvidenceConfidence
import com.example.core.model.Finding
import com.example.core.model.FindingSeverity
import com.example.core.model.FindingStatus
import com.example.core.model.RemediationType
import com.example.ui.theme.AegisBorder
import com.example.ui.theme.AegisBorderFocused
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
import com.example.ui.theme.AegisTrustContainer
import com.example.ui.theme.AegisVoid
import com.example.viewmodel.NavigationTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AegisTopBar(
    onExportReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AegisSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = "AegisCore Shield",
                        tint = AegisHealthy,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "AegisCore Sentinel",
                        color = AegisTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(AegisHealthy)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "On-Device · Data Sovereign",
                            color = AegisTextTertiary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
        },
        actions = {
            IconButton(
                onClick = onExportReport,
                modifier = Modifier.testTag("action_export_report")
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Export Audit Report",
                    tint = AegisTextSecondary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = AegisVoid,
            titleContentColor = AegisTextPrimary
        ),
        modifier = modifier
    )
}

@Composable
fun AegisBottomNavigationBar(
    currentTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        containerColor = AegisSurface,
        tonalElevation = 8.dp,
        modifier = modifier
    ) {
        val items = listOf(
            Triple(NavigationTab.OVERVIEW, "Overview", Pair(Icons.Filled.Shield, Icons.Outlined.Shield)),
            Triple(NavigationTab.APPS, "Apps", Pair(Icons.Filled.Apps, Icons.Outlined.Apps)),
            Triple(NavigationTab.INSPECT, "Inspect", Pair(Icons.Filled.Security, Icons.Outlined.Security)),
            Triple(NavigationTab.TRUST, "Trust", Pair(Icons.Filled.Policy, Icons.Outlined.Policy)),
            Triple(NavigationTab.HISTORY, "History", Pair(Icons.Filled.History, Icons.Outlined.History))
        )

        items.forEach { (tab, label, icons) ->
            val isSelected = currentTab == tab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) icons.first else icons.second,
                        contentDescription = label,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AegisHealthy,
                    selectedTextColor = AegisHealthy,
                    unselectedIconColor = AegisTextTertiary,
                    unselectedTextColor = AegisTextTertiary,
                    indicatorColor = AegisSurfaceVariant
                ),
                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
            )
        }
    }
}

@Composable
fun SeverityBadge(severity: FindingSeverity, modifier: Modifier = Modifier) {
    val (bgColor, textColor) = when (severity) {
        FindingSeverity.CRITICAL -> Pair(AegisCriticalContainer, AegisCritical)
        FindingSeverity.HIGH -> Pair(AegisCriticalContainer, AegisCritical)
        FindingSeverity.MODERATE -> Pair(AegisReviewContainer, AegisReview)
        FindingSeverity.LOW -> Pair(AegisSurfaceVariant, AegisCyan)
        FindingSeverity.INFORMATIONAL -> Pair(AegisSurfaceVariant, AegisTextSecondary)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = severity.name,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun ConfidenceBadge(confidence: EvidenceConfidence, modifier: Modifier = Modifier) {
    val (label, color) = when (confidence) {
        EvidenceConfidence.CONFIRMED -> Pair("CONFIRMED", AegisHealthy)
        EvidenceConfidence.STRONG_INDICATOR -> Pair("STRONG INDICATOR", AegisReview)
        EvidenceConfidence.WEAK_INDICATOR -> Pair("WEAK INDICATOR", AegisTextTertiary)
        EvidenceConfidence.INSUFFICIENT_DATA -> Pair("INSUFFICIENT DATA", AegisTextTertiary)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(AegisSurfaceVariant)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun FindingCard(
    finding: Finding,
    onAddTrustException: (packageName: String, ruleId: String) -> Unit,
    onDismissFinding: (id: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val borderColor = when (finding.severity) {
        FindingSeverity.CRITICAL -> AegisCritical
        FindingSeverity.HIGH -> AegisCritical.copy(alpha = 0.6f)
        FindingSeverity.MODERATE -> AegisReview.copy(alpha = 0.5f)
        else -> AegisBorder
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = AegisSurface),
        border = BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .testTag("finding_card_${finding.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SeverityBadge(severity = finding.severity)
                    Spacer(modifier = Modifier.width(6.dp))
                    ConfidenceBadge(confidence = finding.confidence)
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = AegisTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = finding.title,
                color = AegisTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "${finding.targetName} (${finding.targetId})",
                color = AegisCyan,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = finding.summary,
                color = AegisTextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    // Why it matters section
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(AegisSurfaceVariant)
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = "WHY THIS MATTERS",
                                color = AegisTextTertiary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = finding.whyItMatters,
                                color = AegisTextPrimary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Evidence Record
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(AegisVoid)
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = "EVIDENCE & SOURCE API",
                                color = AegisTextTertiary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = finding.evidenceObservation,
                                color = AegisTextPrimary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Source: ${finding.sourceApi} · Rule: ${finding.ruleId}",
                                color = AegisTextSecondary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            if (finding.limitationNote.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Limitation: ${finding.limitationNote}",
                                    color = AegisReview,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Remediation Action
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                launchRemediationAction(context, finding)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AegisHealthy),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_remediate_${finding.id}")
                        ) {
                            Text(
                                text = if (finding.targetType == "APP") "Open Settings" else "Review Setting",
                                color = AegisVoid,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (finding.targetType == "APP") {
                            OutlinedButton(
                                onClick = { onAddTrustException(finding.targetId, finding.ruleId) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AegisTrust),
                                border = BorderStroke(1.dp, AegisTrust),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("btn_trust_${finding.id}")
                            ) {
                                Text("Trust", fontSize = 12.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = { onDismissFinding(finding.id) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AegisTextTertiary),
                            border = BorderStroke(1.dp, AegisBorder),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("btn_dismiss_${finding.id}")
                        ) {
                            Text("Dismiss", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

private fun launchRemediationAction(context: Context, finding: Finding) {
    try {
        if (finding.targetType == "APP") {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", finding.targetId, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } else {
            val intent = Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    } catch (e: Exception) {
        // Fallback to general settings
        try {
            val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
        } catch (_: Exception) {}
    }
}

/**
 * Custom high-visibility cybersecurity scrollbar modifier for LazyLists.
 * Dynamically computes thumb height and scroll progress directly in DrawScope.
 */
fun Modifier.verticalScrollbar(
    state: LazyListState,
    color: Color = AegisCyan.copy(alpha = 0.85f),
    trackColor: Color = AegisBorder.copy(alpha = 0.35f),
    width: Dp = 4.dp,
    paddingEnd: Dp = 2.dp
): Modifier = this.drawWithContent {
    drawContent()
    val totalItems = state.layoutInfo.totalItemsCount
    val visibleItems = state.layoutInfo.visibleItemsInfo.size
    if (totalItems > visibleItems && visibleItems > 0) {
        val viewportHeight = size.height
        val trackWidthPx = width.toPx()
        val paddingEndPx = paddingEnd.toPx()
        val thumbHeight = (visibleItems.toFloat() / totalItems.toFloat() * viewportHeight)
            .coerceIn(36.dp.toPx(), viewportHeight / 2)
        val firstItem = state.firstVisibleItemIndex
        val maxScrollIndex = (totalItems - visibleItems).coerceAtLeast(1)
        val scrollProgress = (firstItem.toFloat() / maxScrollIndex.toFloat()).coerceIn(0f, 1f)
        val thumbY = scrollProgress * (viewportHeight - thumbHeight)

        val xPos = size.width - trackWidthPx - paddingEndPx

        // Background track
        drawRoundRect(
            color = trackColor,
            topLeft = Offset(xPos, 0f),
            size = Size(trackWidthPx, viewportHeight),
            cornerRadius = CornerRadius(trackWidthPx / 2)
        )

        // Active thumb
        drawRoundRect(
            color = color,
            topLeft = Offset(xPos, thumbY),
            size = Size(trackWidthPx, thumbHeight),
            cornerRadius = CornerRadius(trackWidthPx / 2)
        )
    }
}
