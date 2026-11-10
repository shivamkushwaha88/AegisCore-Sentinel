package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import com.example.ui.components.verticalScrollbar
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.TrustException
import com.example.ui.theme.AegisBorder
import com.example.ui.theme.AegisCritical
import com.example.ui.theme.AegisCyan
import com.example.ui.theme.AegisHealthy
import com.example.ui.theme.AegisReview
import com.example.ui.theme.AegisSurface
import com.example.ui.theme.AegisSurfaceVariant
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
import com.example.ui.theme.AegisTextTertiary
import com.example.ui.theme.AegisTrust
import com.example.ui.theme.AegisTrustContainer
import com.example.ui.theme.AegisVoid
import com.example.viewmodel.SentinelViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TrustScreen(
    viewModel: SentinelViewModel,
    modifier: Modifier = Modifier
) {
    val exceptions by viewModel.trustExceptions.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DEVELOPER TRUST PROFILES",
                        color = AegisTextTertiary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Scoped Risk Annotations & Tool Exceptions",
                        color = AegisTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = AegisTrust),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_add_trust_rule")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Rule", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Truthful Disclaimer Box
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = AegisTrustContainer),
                border = BorderStroke(1.dp, AegisTrust.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = AegisTrust,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Trust exceptions document your deliberate risk acceptance for AI coding assistants, terminals, and dev tools. They suppress false-positive audit findings, but do NOT grant or alter Android OS permissions or bypass system sandboxes.",
                        color = AegisTextPrimary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Quick developer presets
        item {
            Text(
                text = "DEVELOPER QUICK PRESETS",
                color = AegisTextTertiary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickPresetButton(
                    label = "Termux / Shell",
                    targetPkg = "com.termux",
                    onSelect = { pkg ->
                        viewModel.addTrustException(pkg, "ALL_RULES", "Terminal environment for developer commands")
                    },
                    modifier = Modifier.weight(1f)
                )

                QuickPresetButton(
                    label = "AI Assistant Tool",
                    targetPkg = "com.developer.aiassistant",
                    onSelect = { pkg ->
                        viewModel.addTrustException(pkg, "ALL_RULES", "Local AI coding daemon with terminal access")
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Text(
                text = "CONFIGURED EXCEPTIONS (${exceptions.size})",
                color = AegisTextTertiary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        if (exceptions.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AegisSurface),
                    border = BorderStroke(1.dp, AegisBorder),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(imageVector = Icons.Default.Policy, contentDescription = null, tint = AegisTextTertiary, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No Trust Exceptions Configured",
                            color = AegisTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Add an exception for your terminal tools or AI coding assistants to prevent routine developer capabilities from being flagged as vulnerabilities.",
                            color = AegisTextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        } else {
            items(exceptions, key = { it.id }) { ex ->
                TrustExceptionCard(
                    exception = ex,
                    onToggle = { enabled -> viewModel.toggleTrustException(ex.id, enabled) },
                    onDelete = { viewModel.deleteTrustException(ex.id) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showAddDialog) {
        AddTrustRuleDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { pkg, ruleId, reason ->
                viewModel.addTrustException(pkg, ruleId, reason)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun QuickPresetButton(
    label: String,
    targetPkg: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = { onSelect(targetPkg) },
        colors = ButtonDefaults.outlinedButtonColors(contentColor = AegisCyan),
        border = BorderStroke(1.dp, AegisCyan.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Icon(imageVector = Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun TrustExceptionCard(
    exception: TrustException,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(exception.createdAt))

    Card(
        colors = CardDefaults.cardColors(containerColor = AegisSurface),
        border = BorderStroke(1.dp, if (exception.enabled) AegisTrust.copy(alpha = 0.5f) else AegisBorder),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exception.packageName,
                        color = AegisTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Scoped Rule: ${exception.ruleId}",
                        color = AegisCyan,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = exception.enabled,
                        onCheckedChange = onToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AegisTrust,
                            checkedTrackColor = AegisSurfaceVariant
                        )
                    )
                    IconButton(onClick = onDelete) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = AegisTextTertiary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(AegisVoid)
                    .padding(8.dp)
            ) {
                Text(
                    text = "Rationale: ${exception.reason}",
                    color = AegisTextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Created: $dateStr · User Confirmed",
                color = AegisTextTertiary,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun AddTrustRuleDialog(
    onDismiss: () -> Unit,
    onConfirm: (packageName: String, ruleId: String, reason: String) -> Unit
) {
    var pkg by remember { mutableStateOf("") }
    var ruleId by remember { mutableStateOf("ALL_RULES") }
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AegisSurface,
        title = {
            Text("Add Scoped Trust Rule", color = AegisTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = pkg,
                    onValueChange = { pkg = it },
                    label = { Text("Package Name or Target ID", fontSize = 12.sp) },
                    placeholder = { Text("e.g. com.my.tool or system.device", fontSize = 11.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = AegisVoid,
                        unfocusedContainerColor = AegisVoid,
                        focusedTextColor = AegisTextPrimary,
                        unfocusedTextColor = AegisTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = ruleId,
                    onValueChange = { ruleId = it },
                    label = { Text("Scoped Rule ID", fontSize = 12.sp) },
                    placeholder = { Text("ALL_RULES or R-APP-SENSITIVE-COMBO", fontSize = 11.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = AegisVoid,
                        unfocusedContainerColor = AegisVoid,
                        focusedTextColor = AegisTextPrimary,
                        unfocusedTextColor = AegisTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Rationale & Risk Notes", fontSize = 12.sp) },
                    placeholder = { Text("e.g. Verified local development CLI", fontSize = 11.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = AegisVoid,
                        unfocusedContainerColor = AegisVoid,
                        focusedTextColor = AegisTextPrimary,
                        unfocusedTextColor = AegisTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pkg.isNotBlank()) {
                        onConfirm(pkg.trim(), ruleId.ifBlank { "ALL_RULES" }.trim(), reason.trim())
                    }
                },
                enabled = pkg.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AegisTrust)
            ) {
                Text("Save Rule", color = AegisTextPrimary, fontSize = 12.sp)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", color = AegisTextSecondary, fontSize = 12.sp)
            }
        }
    )
}
