package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.ElectricCyan
import com.example.viewmodel.EVLabUiState

@Composable
fun ReportExportScreen(
    uiState: EVLabUiState,
    onGenerateReportText: () -> String,
    onGenerateCsvText: () -> String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isFa = uiState.isFarsiPreferred
    val reportText = onGenerateReportText()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("report_export_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = if (isFa) "گزارش ممیزی مهندسی و صدور خروجی (Sections 9 & 10)" else "Engineering Audit Report & File Export",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (isFa)
                        "تولید فایل گزارش متنی، خروجی دیتاست زمانی CSV و اشتراک‌گذاری"
                    else
                        "Generate formatted audit text, CSV telemetry dataset, and share",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Action Buttons Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Copy Report Button
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("EV Powertrain Report", reportText)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, if (isFa) "گزارش در حافظه کپی شد" else "Report copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f).testTag("btn_copy_report")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isFa) "کپی گزارش" else "Copy", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Share Report Button
                OutlinedButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "EV Powertrain Simulation Audit Report")
                            putExtra(Intent.EXTRA_TEXT, reportText)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share EV Audit Report"))
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).testTag("btn_share_report")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isFa) "اشتراک گزارش" else "Share TXT", fontSize = 12.sp)
                }

                // Export CSV Button
                Button(
                    onClick = {
                        val csvText = onGenerateCsvText()
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/comma-separated-values"
                            putExtra(Intent.EXTRA_SUBJECT, "telemetry_dataset.csv")
                            putExtra(Intent.EXTRA_TEXT, csvText)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Export Telemetry CSV"))
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.weight(1f).testTag("btn_export_csv")
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isFa) "خروجی CSV" else "CSV Data", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Console Summary Header Preview (Section 10)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DeepNavy)
                    .border(1.dp, ElectricCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = "CONSOLE DIGITAL TWIN SUMMARY (Section 10)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = ElectricCyan,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val sim = uiState.simulationResult
                    if (sim != null) {
                        Text(
                            text = buildString {
                                appendLine("Total Cycle Distance        : ${String.format("%6.2f", sim.totalDistKm)} km")
                                appendLine("Specific Energy Consumption : ${String.format("%6.2f", sim.specEnergyWhKm)} Wh/km")
                                appendLine("Regenerative Braking Share  : ${String.format("%6.2f", sim.regenPct)} %")
                                appendLine("Projected Range (DoD Limit) : ${String.format("%6.2f", sim.estAutonomyKm)} km")
                                appendLine("Peak Winding Temperature    : ${String.format("%6.2f", sim.peakWindingTempC)} deg C")
                                appendLine("Peak Magnet Temperature     : ${String.format("%6.2f", sim.peakMagnetTempC)} deg C")
                                appendLine("All calculations converged successfully.")
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            ),
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }
            }
        }

        // Full Engineering Audit Report Box
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "simulation_report.txt",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "UTF-8 Engineering Format",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = reportText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
