package com.example.ui.settings

import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DnsConfiguration
import com.example.data.model.DnsProvider
import com.example.data.model.SearchEngine
import com.example.data.model.VpnProtocol
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
fun SettingsScreen(
    currentSearchEngine: SearchEngine,
    dnsConfig: DnsConfiguration,
    safeBrowsingEnabled: Boolean,
    blockThirdPartyCookies: Boolean,
    desktopModeDefault: Boolean,
    darkThemeEnabled: Boolean,
    onSelectSearchEngine: (SearchEngine) -> Unit,
    onSelectDnsProvider: (DnsProvider) -> Unit,
    onSetCustomDnsUrl: (String) -> Unit,
    onToggleSafeBrowsing: (Boolean) -> Unit,
    onToggleBlockThirdPartyCookies: (Boolean) -> Unit,
    onToggleDesktopMode: (Boolean) -> Unit,
    onToggleDarkTheme: (Boolean) -> Unit,
    onClearAllData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var customDnsInput by remember(dnsConfig.customDohUrl) { mutableStateOf(dnsConfig.customDohUrl) }

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
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = OpenVBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Configurações",
                        color = OpenVTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Preferências de navegação, segurança e módulos futuros",
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
            // Search Engine Section
            SettingsSectionTitle("Mecanismo de Pesquisa")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = OpenVSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, OpenVBorder)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    SearchEngine.values().forEach { engine ->
                        val isSelected = engine == currentSearchEngine
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSelectSearchEngine(engine) }
                                .padding(vertical = 4.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onSelectSearchEngine(engine) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = OpenVBlue,
                                    unselectedColor = OpenVTextSecondary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = engine.displayName,
                                    color = OpenVTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = if (engine == SearchEngine.DUCKDUCKGO) "Padrão recomendado (sem rastreamento)" else engine.searchUrl,
                                    color = OpenVTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Encrypted DNS Profile Section
            SettingsSectionTitle("DNS Criptografado (Preparação Etapa 2)")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = OpenVSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, OpenVBorder)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    DnsProvider.values().forEach { provider ->
                        val isSelected = provider == dnsConfig.provider
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSelectDnsProvider(provider) }
                                .padding(vertical = 4.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onSelectDnsProvider(provider) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = OpenVBlue,
                                    unselectedColor = OpenVTextSecondary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = provider.displayName,
                                    color = OpenVTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = provider.description,
                                    color = OpenVTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    if (dnsConfig.provider == DnsProvider.CUSTOM) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customDnsInput,
                            onValueChange = {
                                customDnsInput = it
                                onSetCustomDnsUrl(it)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            placeholder = { Text("https://seu-dns.com/dns-query", color = OpenVTextSecondary, fontSize = 12.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = OpenVTextPrimary,
                                unfocusedTextColor = OpenVTextPrimary,
                                focusedBorderColor = OpenVBlue,
                                unfocusedBorderColor = OpenVBorder
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Privacy & Browser Settings
            SettingsSectionTitle("Privacidade & Navegação")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = OpenVSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, OpenVBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    ToggleRow(
                        title = "Android Safe Browsing",
                        subtitle = "Verificação de reputação de ameaças web",
                        checked = safeBrowsingEnabled,
                        onCheckedChange = onToggleSafeBrowsing
                    )
                    Divider(color = OpenVBorder, modifier = Modifier.padding(vertical = 10.dp))
                    ToggleRow(
                        title = "Bloquear Cookies de Terceiros",
                        subtitle = "Impede rastreadores de publicidade",
                        checked = blockThirdPartyCookies,
                        onCheckedChange = onToggleBlockThirdPartyCookies
                    )
                    Divider(color = OpenVBorder, modifier = Modifier.padding(vertical = 10.dp))
                    ToggleRow(
                        title = "Modo Computador por Padrão",
                        subtitle = "Solicita versão desktop das páginas",
                        checked = desktopModeDefault,
                        onCheckedChange = onToggleDesktopMode
                    )
                    Divider(color = OpenVBorder, modifier = Modifier.padding(vertical = 10.dp))
                    ToggleRow(
                        title = "Tema Escuro Cyberpunk",
                        subtitle = "Estilo futurista escuro otimizado para OLED",
                        checked = darkThemeEnabled,
                        onCheckedChange = onToggleDarkTheme
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Clear Data Button
            Button(
                onClick = {
                    onClearAllData()
                    Toast.makeText(context, "Dados de navegação apagados.", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_clear_data_button"),
                colors = ButtonDefaults.buttonColors(containerColor = OpenVAlertRed.copy(alpha = 0.2f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, OpenVAlertRed),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = OpenVAlertRed, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Apagar Dados de Navegação (Cache / Cookies)", color = OpenVAlertRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // About OpenV Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = OpenVSurfaceElevated),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, OpenVBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "OpenV — Versão 0.1 Alpha (Etapa 1 de 5)",
                        color = OpenVTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Projetado para Motorola Edge 30 Neo, Android 17 / LineageOS 24. Execução 100% nativa sem dependência de root, Shizuku ou NDK.",
                        color = OpenVTextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Nesta etapa, o navegador opera em modo direto HTTPS com sandbox nativo e proteções ativas. VPN e DoH estão estruturados em nível de contrato e serão integrados no túnel na Etapa 2.",
                        color = OpenVTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        color = OpenVTextPrimary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
    )
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = OpenVTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, color = OpenVTextSecondary, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = OpenVShieldGreen,
                checkedTrackColor = OpenVSurfaceElevated,
                uncheckedThumbColor = OpenVTextSecondary,
                uncheckedTrackColor = OpenVSurfaceElevated
            )
        )
    }
}
