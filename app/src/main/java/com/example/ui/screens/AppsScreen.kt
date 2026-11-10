package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.AppSnapshot
import com.example.ui.theme.AegisBorder
import com.example.ui.theme.AegisCritical
import com.example.ui.theme.AegisCyan
import com.example.ui.theme.AegisHealthy
import com.example.ui.theme.AegisReview
import com.example.ui.theme.AegisReviewContainer
import com.example.ui.theme.AegisSurface
import com.example.ui.theme.AegisSurfaceVariant
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
import com.example.ui.theme.AegisTextTertiary
import com.example.ui.theme.AegisTrust
import com.example.ui.theme.AegisVoid
import com.example.viewmodel.SentinelViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(
    viewModel: SentinelViewModel,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.appSearchQuery.collectAsState()
    val apps by viewModel.filteredApps.collectAsState()
    var filterUserOnly by remember { mutableStateOf(true) }
    var filterSensitiveOnly by remember { mutableStateOf(false) }
    var selectedApp by remember { mutableStateOf<AppSnapshot?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val displayedApps = apps.filter { app ->
        (!filterUserOnly || !app.isSystemApp) &&
                (!filterSensitiveOnly || app.grantedPermissions.any { isSensitivePermission(it) })
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AegisVoid)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setAppSearchQuery(it) },
            placeholder = { Text("Search installed packages or labels...", color = AegisTextTertiary, fontSize = 13.sp) },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = AegisTextTertiary, modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setAppSearchQuery("") }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = AegisTextTertiary, modifier = Modifier.size(16.dp))
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = AegisSurface,
                unfocusedContainerColor = AegisSurface,
                focusedBorderColor = AegisCyan,
                unfocusedBorderColor = AegisBorder,
                focusedTextColor = AegisTextPrimary,
                unfocusedTextColor = AegisTextPrimary
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_search_apps")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter toggles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filterUserOnly,
                onClick = { filterUserOnly = !filterUserOnly },
                label = { Text("User Apps", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = AegisSurface,
                    labelColor = AegisTextSecondary,
                    selectedContainerColor = AegisSurfaceVariant,
                    selectedLabelColor = AegisTextPrimary
                ),
                border = BorderStroke(1.dp, if (filterUserOnly) AegisCyan else AegisBorder)
            )

            FilterChip(
                selected = filterSensitiveOnly,
                onClick = { filterSensitiveOnly = !filterSensitiveOnly },
                label = { Text("Sensitive Permissions Only", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = AegisSurface,
                    labelColor = AegisTextSecondary,
                    selectedContainerColor = AegisSurfaceVariant,
                    selectedLabelColor = AegisTextPrimary
                ),
                border = BorderStroke(1.dp, if (filterSensitiveOnly) AegisReview else AegisBorder)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "SHOWING ${displayedApps.size} PACKAGES",
            color = AegisTextTertiary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(6.dp))

        val appListState = rememberLazyListState()

        LazyColumn(
            state = appListState,
            modifier = Modifier
                .fillMaxSize()
                .verticalScrollbar(appListState),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(displayedApps, key = { it.packageName }) { app ->
                AppListItem(
                    app = app,
                    onClick = { selectedApp = app }
                )
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    selectedApp?.let { app ->
        ModalBottomSheet(
            onDismissRequest = { selectedApp = null },
            sheetState = sheetState,
            containerColor = AegisSurface
        ) {
            AppDetailSheet(
                app = app,
                onClose = { selectedApp = null },
                onAddTrustException = { pkg, ruleId ->
                    viewModel.addTrustException(pkg, ruleId, "Developer exception for $pkg")
                    selectedApp = null
                }
            )
        }
    }
}

@Composable
fun AppListItem(
    app: AppSnapshot,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sensitiveCount = app.grantedPermissions.count { isSensitivePermission(it) }

    Card(
        colors = CardDefaults.cardColors(containerColor = AegisSurface),
        border = BorderStroke(1.dp, if (sensitiveCount >= 3) AegisReview.copy(alpha = 0.5f) else AegisBorder),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("app_item_${app.packageName}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Letter avatar
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (app.isSystemApp) AegisSurfaceVariant else AegisCyan.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = app.displayName.take(1).uppercase(),
                    color = if (app.isSystemApp) AegisTextSecondary else AegisCyan,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = app.displayName,
                        color = AegisTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (app.isSystemApp) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(AegisSurfaceVariant)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text("SYS", color = AegisTextTertiary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
                Text(
                    text = app.packageName,
                    color = AegisTextTertiary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "SDK ${app.targetSdk}",
                        color = if (app.targetSdk < 30) AegisReview else AegisHealthy,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "·",
                        color = AegisTextTertiary,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "${app.grantedPermissions.size}/${app.requestedPermissions.size} granted",
                        color = AegisTextSecondary,
                        fontSize = 10.sp
                    )
                    if (sensitiveCount > 0) {
                        Text(
                            text = "·",
                            color = AegisTextTertiary,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "$sensitiveCount sensitive",
                            color = AegisReview,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppDetailSheet(
    app: AppSnapshot,
    onClose: () -> Unit,
    onAddTrustException: (packageName: String, ruleId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = app.displayName,
                    color = AegisTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = app.packageName,
                    color = AegisCyan,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            IconButton(onClick = onClose) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = AegisTextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Metadata box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(AegisVoid)
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                MetaRow("Target SDK", "Android API ${app.targetSdk}")
                MetaRow("Version", "${app.versionName} (${app.versionCode})")
                MetaRow("Installer Source", app.installerPackage ?: "Direct / Sideloaded")
                MetaRow("Exported Components", "${app.exportedComponentsCount} exposed")
                MetaRow("Signing Cert SHA-256", app.signingCertificateSha256.take(24) + "...")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "PERMISSIONS BREAKDOWN",
            color = AegisTextTertiary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(6.dp))

        val requested = app.requestedPermissions
        val grantedSet = app.grantedPermissions.toSet()

        if (requested.isEmpty()) {
            Text("No permissions declared by this application.", color = AegisTextSecondary, fontSize = 12.sp)
        } else {
            val permListState = rememberLazyListState()
            LazyColumn(
                state = permListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .verticalScrollbar(permListState, width = 3.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(requested) { perm ->
                    val isGranted = perm in grantedSet
                    val isSensitive = isSensitivePermission(perm)
                    val shortName = perm.substringAfterLast(".")

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSensitive && isGranted) AegisReviewContainer else AegisSurfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = shortName,
                            color = if (isSensitive && isGranted) AegisReview else AegisTextPrimary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isSensitive) FontWeight.Bold else FontWeight.Normal
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isGranted) AegisHealthy.copy(alpha = 0.2f) else AegisVoid)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isGranted) "GRANTED" else "REQUESTED",
                                color = if (isGranted) AegisHealthy else AegisTextTertiary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", app.packageName, null)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AegisHealthy),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("btn_app_open_settings")
            ) {
                Icon(imageVector = Icons.Default.Launch, contentDescription = null, tint = AegisVoid, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Open App Settings", color = AegisVoid, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = { onAddTrustException(app.packageName, "ALL_RULES") },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AegisTrust),
                border = BorderStroke(1.dp, AegisTrust),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .height(44.dp)
                    .testTag("btn_app_add_trust")
            ) {
                Icon(imageVector = Icons.Default.Policy, contentDescription = null, tint = AegisTrust, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Trust Note", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = AegisTextTertiary, fontSize = 11.sp)
        Text(text = value, color = AegisTextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}

private fun isSensitivePermission(perm: String): Boolean {
    return perm.contains("LOCATION") || perm.contains("CAMERA") ||
            perm.contains("AUDIO") || perm.contains("SMS") ||
            perm.contains("CONTACTS") || perm.contains("PHONE") ||
            perm.contains("STORAGE") || perm.contains("SYSTEM_ALERT_WINDOW")
}
