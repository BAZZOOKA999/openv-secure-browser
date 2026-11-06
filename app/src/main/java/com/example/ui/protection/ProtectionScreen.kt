package com.example.ui.protection

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DnsConfiguration
import com.example.data.model.VpnConnectionState
import com.example.ui.browser.ConnectivityDiagnostic
import com.example.ui.theme.OpenVAlertRed
import com.example.ui.theme.OpenVBackground
import com.example.ui.theme.OpenVBlue
import com.example.ui.theme.OpenVBorder
import com.example.ui.theme.OpenVShieldGreen
import com.example.ui.theme.OpenVSurface
import com.example.ui.theme.OpenVSurfaceElevated
import com.example.ui.theme.OpenVSurfaceVariant
import com.example.ui.theme.OpenVTextPrimary
import com.example.ui.theme.OpenVTextSecondary
import com.example.ui.theme.OpenVWarningOrange

@Composable
fun ProtectionScreen(
    vpnState: VpnConnectionState,
    dnsConfig: DnsConfiguration,
    safeBrowsingEnabled: Boolean,
    blockThirdPartyCookies: Boolean,
    diagnosticState: ConnectivityDiagnostic?,
    onToggleSafeBrowsing: (Boolean) -> Unit,
    onToggleBlockThirdPartyCookies: (Boolean) -> Unit,
    onRunDiagnostics: () -> Unit,
    onClearBrowsingData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OpenVBackground)
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = OpenVSurface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = OpenVShieldGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Central de Proteção",
                        color = OpenVTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Monitoramento real e controles de privacidade do navegador",
                    color = OpenVTextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // VPN Status Card
            ProtectionCard(
                icon = Icons.Default.VpnKey,
                iconTint = OpenVWarningOrange,
                title = "Módulo VPN",
                subtitle = "Arquitetura preparada para túneis WireGuard / OpenVPN",
                statusLabel = vpnState.labelPt,
                statusColor = OpenVWarningOrange,
                description = "Nenhum túnel fictício em execução. Na Etapa 1 os contratos e perfis foram gerados; a conexão ao backend de servidores reais será inicializada na Etapa 2."
            )

            Spacer(modifier = Modifier.height(12.dp))

            // DNS Status Card
            ProtectionCard(
                icon = Icons.Default.Dns,
                iconTint = OpenVBlue,
                title = "DNS Criptografado",
                subtitle = "Provedor configurado: ${dnsConfig.provider.displayName}",
                statusLabel = dnsConfig.state.labelPt,
                statusColor = OpenVTextSecondary,
                description = "O provedor selecionado está armazenado no perfil. O encaminhamento do tráfego do WebView via DoH será ativado na Etapa 2."
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Safe Browsing Card with real toggle
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = OpenVSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, OpenVBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(OpenVShieldGreen.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Policy,
                                    contentDescription = null,
                                    tint = OpenVShieldGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Android Safe Browsing",
                                    color = OpenVTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (safeBrowsingEnabled) "Proteção nativa ativada" else "Desativado",
                                    color = if (safeBrowsingEnabled) OpenVShieldGreen else OpenVAlertRed,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Switch(
                            checked = safeBrowsingEnabled,
                            onCheckedChange = onToggleSafeBrowsing,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = OpenVShieldGreen,
                                checkedTrackColor = OpenVSurfaceElevated,
                                uncheckedThumbColor = OpenVTextSecondary,
                                uncheckedTrackColor = OpenVSurfaceElevated
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Utiliza a API nativa AndroidX WebKit Safe Browsing para alertar sobre sites conhecidos de phishing e distribuição de malware.",
                        color = OpenVTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Cookies & Tracking Policy Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = OpenVSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, OpenVBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(OpenVBlue.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cookie,
                                    contentDescription = null,
                                    tint = OpenVBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Cookies de Terceiros",
                                    color = OpenVTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (blockThirdPartyCookies) "Bloqueados no WebView" else "Permitidos",
                                    color = if (blockThirdPartyCookies) OpenVShieldGreen else OpenVWarningOrange,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Switch(
                            checked = blockThirdPartyCookies,
                            onCheckedChange = onToggleBlockThirdPartyCookies,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = OpenVShieldGreen,
                                checkedTrackColor = OpenVSurfaceElevated,
                                uncheckedThumbColor = OpenVTextSecondary,
                                uncheckedTrackColor = OpenVSurfaceElevated
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Bloquear cookies de terceiros impede que redes de publicidade rastreiem sua navegação entre múltiplos sites.",
                        color = OpenVTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Real Connectivity Diagnostics Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = OpenVSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, OpenVBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NetworkCheck,
                                contentDescription = null,
                                tint = OpenVBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Diagnóstico de Rede Real",
                                color = OpenVTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = onRunDiagnostics,
                            colors = ButtonDefaults.buttonColors(containerColor = OpenVBlue),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("run_diagnostics_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Testar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (diagnosticState != null) {
                        DiagnosticRow("Acesso à Internet", if (diagnosticState.hasInternet) "Conectado" else "Sem acesso", if (diagnosticState.hasInternet) OpenVShieldGreen else OpenVAlertRed)
                        DiagnosticRow("Tipo de Transporte", diagnosticState.transportType, OpenVTextPrimary)
                        DiagnosticRow("Conexão Limitada (Dados)", if (diagnosticState.isMetered) "Sim (Móvel/Tarifada)" else "Não", OpenVTextSecondary)
                        DiagnosticRow("Resolução DNS Real", if (diagnosticState.dnsResolved) "Operacional" else "Falha ao resolver", if (diagnosticState.dnsResolved) OpenVShieldGreen else OpenVAlertRed)
                        DiagnosticRow("IP de Interface Local", diagnosticState.localIp, OpenVTextPrimary)
                    } else {
                        Text(
                            text = "Toque em 'Testar' para diagnosticar a conectividade em tempo real via ConnectivityManager e resolução DNS.",
                            color = OpenVTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Data Wipe Button
            Button(
                onClick = {
                    onClearBrowsingData()
                    Toast.makeText(context, "Cookies, cache e histórico limpos com sucesso.", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wipe_data_button"),
                colors = ButtonDefaults.buttonColors(containerColor = OpenVAlertRed.copy(alpha = 0.2f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, OpenVAlertRed),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = OpenVAlertRed, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Limpar Todos os Dados de Navegação", color = OpenVAlertRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun ProtectionCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    statusLabel: String,
    statusColor: Color,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = OpenVSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, OpenVBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(iconTint.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = title, color = OpenVTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(text = subtitle, color = OpenVTextSecondary, fontSize = 11.sp)
                    }
                }

                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = statusLabel,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = description, color = OpenVTextSecondary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = OpenVTextSecondary, fontSize = 12.sp)
        Text(text = value, color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
