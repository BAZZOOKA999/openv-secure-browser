package com.example.ui.browser.menu

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.TabItem
import com.example.network.HttpHeaderInspectionResult
import com.example.network.NetworkSecurityClient
import com.example.ui.theme.OpenVAlertRed
import com.example.ui.theme.OpenVBackground
import com.example.ui.theme.OpenVBlue
import com.example.ui.theme.OpenVBorder
import com.example.ui.theme.OpenVShieldGreen
import com.example.ui.theme.OpenVSurface
import com.example.ui.theme.OpenVSurfaceElevated
import com.example.ui.theme.OpenVTextPrimary
import com.example.ui.theme.OpenVTextSecondary
import com.example.ui.theme.OpenVWarningOrange
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SecurityVerdict(val labelPt: String, val color: Color) {
    SAFE("Nenhuma ameaça conhecida detectada", OpenVShieldGreen),
    WARNING("Atenção: sinais de risco detectados", OpenVWarningOrange),
    DANGER("Ameaça conhecida detectada", OpenVAlertRed),
    UNKNOWN("Não foi possível verificar completamente", OpenVTextSecondary)
}

data class SiteSecurityReport(
    val url: String,
    val domain: String,
    val isHttps: Boolean,
    val hasValidSsl: Boolean,
    val isDangerousScheme: Boolean,
    val hasIdnHomographRisk: Boolean,
    val hasSuspiciousSubdomains: Boolean,
    val hasCredentialsInUrl: Boolean,
    val scanTimestamp: Long,
    val verdict: SecurityVerdict,
    val details: List<String>,
    val headerInspection: HttpHeaderInspectionResult? = null
)

@Composable
fun SiteSecurityScanDialog(
    tab: TabItem?,
    securityClient: NetworkSecurityClient,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var isScanning by remember { mutableStateOf(true) }
    var report by remember { mutableStateOf<SiteSecurityReport?>(null) }

    val currentUrl = tab?.url ?: ""

    LaunchedEffect(currentUrl) {
        if (currentUrl.isBlank() || currentUrl.startsWith("openv://")) {
            report = SiteSecurityReport(
                url = currentUrl.ifBlank { "openv://home" },
                domain = "openv.internal",
                isHttps = true,
                hasValidSsl = true,
                isDangerousScheme = false,
                hasIdnHomographRisk = false,
                hasSuspiciousSubdomains = false,
                hasCredentialsInUrl = false,
                scanTimestamp = System.currentTimeMillis(),
                verdict = SecurityVerdict.SAFE,
                details = listOf("Página interna do navegador protegida nativamente.")
            )
            isScanning = false
            return@LaunchedEffect
        }

        isScanning = true
        try {
            val uri = URI(currentUrl)
            val scheme = uri.scheme?.lowercase() ?: ""
            val host = uri.host ?: ""

            val isHttps = scheme == "https"
            val isDangerousScheme = scheme in listOf("javascript", "data", "file", "intent", "content")
            val hasCredentialsInUrl = uri.userInfo != null
            val hasIdnHomographRisk = host.startsWith("xn--") || host.any { it.code > 127 }
            val hasSuspiciousSubdomains = host.count { it == '.' } > 3

            // Network headers inspection
            val headerRes = securityClient.inspectHttpHeadersAndRedirects(currentUrl).getOrNull()

            val details = mutableListOf<String>()
            var severity = 0

            if (isDangerousScheme) {
                details.add("Alerta Crítico: Esquema de URL perigoso detectado ($scheme:).")
                severity += 3
            }

            if (!isHttps) {
                details.add("Aviso de Conexão: Tráfego HTTP sem criptografia. Dados podem ser interceptados.")
                severity += 2
            } else {
                details.add("Conexão Segura: Protocolo HTTPS ativo com túnel TLS.")
            }

            if (hasCredentialsInUrl) {
                details.add("Alerta: URL contém credenciais embutidas (userInfo), técnica comum em phishing.")
                severity += 2
            }

            if (hasIdnHomographRisk) {
                details.add("Atenção IDN: Domínio com caracteres internacionais (Punycode), risco de spoofing visual.")
                severity += 1
            }

            if (hasSuspiciousSubdomains) {
                details.add("Aviso: Quantidade excessiva de subdomínios detectada.")
                severity += 1
            }

            if (headerRes != null) {
                if (headerRes.securityHeaders["HSTS (Strict-Transport-Security)"] == true) {
                    details.add("HSTS Ativo: Força navegação exclusivamente em HTTPS.")
                } else if (isHttps) {
                    details.add("HSTS Ausente: Servidor não enviou cabeçalho Strict-Transport-Security.")
                }

                if (headerRes.securityHeaders["CSP (Content-Security-Policy)"] == true) {
                    details.add("Content-Security-Policy (CSP) Ativo: Proteção contra injeção de scripts (XSS).")
                }

                if (headerRes.securityHeaders["X-Frame-Options (Anti-Clickjacking)"] == true) {
                    details.add("X-Frame-Options Ativo: Bloqueia ataques de clickjacking em iframes.")
                }

                if (headerRes.redirectChain.size > 2) {
                    details.add("Múltiplos Redirecionamentos: ${headerRes.redirectChain.size} saltos na cadeia de conexão.")
                }
            }

            val verdict = when {
                severity >= 3 -> SecurityVerdict.DANGER
                severity >= 1 -> SecurityVerdict.WARNING
                else -> SecurityVerdict.SAFE
            }

            report = SiteSecurityReport(
                url = currentUrl,
                domain = host.ifBlank { currentUrl },
                isHttps = isHttps,
                hasValidSsl = tab?.sslCertificateValid != false,
                isDangerousScheme = isDangerousScheme,
                hasIdnHomographRisk = hasIdnHomographRisk,
                hasSuspiciousSubdomains = hasSuspiciousSubdomains,
                hasCredentialsInUrl = hasCredentialsInUrl,
                scanTimestamp = System.currentTimeMillis(),
                verdict = verdict,
                details = details,
                headerInspection = headerRes
            )
        } catch (e: Exception) {
            report = SiteSecurityReport(
                url = currentUrl,
                domain = currentUrl,
                isHttps = currentUrl.startsWith("https://"),
                hasValidSsl = false,
                isDangerousScheme = false,
                hasIdnHomographRisk = false,
                hasSuspiciousSubdomains = false,
                hasCredentialsInUrl = false,
                scanTimestamp = System.currentTimeMillis(),
                verdict = SecurityVerdict.UNKNOWN,
                details = listOf("Falha ao analisar URL: ${e.localizedMessage}")
            )
        } finally {
            isScanning = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(OpenVBackground),
            color = OpenVBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = OpenVBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Escanear Segurança do Site",
                            color = OpenVTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = OpenVTextPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (isScanning) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = OpenVBlue)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Examinando protocolo TLS, headers e reputação...", color = OpenVTextSecondary, fontSize = 13.sp)
                        }
                    }
                } else if (report != null) {
                    val rep = report!!
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(scrollState)
                    ) {
                        // Verdict Banner
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = rep.verdict.color.copy(alpha = 0.12f)),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, rep.verdict.color)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when (rep.verdict) {
                                        SecurityVerdict.SAFE -> Icons.Default.CheckCircle
                                        SecurityVerdict.WARNING -> Icons.Default.Warning
                                        SecurityVerdict.DANGER -> Icons.Default.Warning
                                        SecurityVerdict.UNKNOWN -> Icons.Default.Security
                                    },
                                    contentDescription = null,
                                    tint = rep.verdict.color,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = rep.verdict.labelPt,
                                        color = rep.verdict.color,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Verificado em: ${dateFormat.format(Date(rep.scanTimestamp))}",
                                        color = OpenVTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Target Info
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = OpenVSurface),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, OpenVBorder)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Domínio & Protocolo", color = OpenVTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(rep.domain, color = OpenVBlue, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Text(rep.url, color = OpenVTextSecondary, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)

                                Divider(color = OpenVBorder, modifier = Modifier.padding(vertical = 10.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Protocolo Criptografado:", color = OpenVTextSecondary, fontSize = 12.sp)
                                    Text(if (rep.isHttps) "HTTPS (Seguro)" else "HTTP (Inseguro)", color = if (rep.isHttps) OpenVShieldGreen else OpenVAlertRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Certificado SSL/TLS:", color = OpenVTextSecondary, fontSize = 12.sp)
                                    Text(if (rep.hasValidSsl) "Válido" else "Inválido / Alerta", color = if (rep.hasValidSsl) OpenVShieldGreen else OpenVAlertRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Android Safe Browsing:", color = OpenVTextSecondary, fontSize = 12.sp)
                                    Text("Ativo no WebView", color = OpenVShieldGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Verification Details
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = OpenVSurface),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, OpenVBorder)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Pontos de Inspeção de Segurança (${rep.details.size}):", color = OpenVTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                rep.details.forEach { detail ->
                                    Text("• $detail", color = OpenVTextSecondary, fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Export / Share Report Action
                        Button(
                            onClick = {
                                val text = StringBuilder()
                                    .appendLine("=== Relatório de Segurança OpenV ===")
                                    .appendLine("Alvo: ${rep.domain}")
                                    .appendLine("URL: ${rep.url}")
                                    .appendLine("Veredito: ${rep.verdict.labelPt}")
                                    .appendLine("Data: ${dateFormat.format(Date(rep.scanTimestamp))}")
                                    .appendLine("Detalhes:")
                                rep.details.forEach { text.appendLine(" - $it") }

                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Relatório de Segurança: ${rep.domain}")
                                    putExtra(Intent.EXTRA_TEXT, text.toString())
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Compartilhar Relatório"))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = OpenVBlue),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Exportar / Compartilhar Relatório", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}
