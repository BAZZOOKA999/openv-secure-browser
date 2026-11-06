package com.example.ui.vpndns

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VpnKey
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.DnsConfiguration
import com.example.data.model.DnsProvider
import com.example.data.model.PublicIpInfo
import com.example.data.model.VpnConnectionState
import com.example.data.model.VpnProfile
import com.example.data.model.VpnProtocol
import com.example.network.DoHQueryResult
import com.example.ui.browser.BrowserViewModel
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
fun VpnDnsScreen(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scrollState = rememberScrollState()

    val vpnState by viewModel.vpnManager.connectionState.collectAsState()
    val connectedServer by viewModel.vpnManager.connectedServer.collectAsState()
    val savedProfiles by viewModel.vpnManager.savedProfiles.collectAsState()
    val activeProfile by viewModel.vpnManager.activeProfile.collectAsState()
    val killSwitchEnabled by viewModel.vpnManager.killSwitchEnabled.collectAsState()
    val autoRotate by viewModel.vpnManager.autoRotatePerSession.collectAsState()
    val publicIpInfo by viewModel.vpnManager.currentPublicIp.collectAsState()
    val dnsConfig by viewModel.dnsConfiguration.collectAsState()
    val dohQueryResult by viewModel.dohQueryResult.collectAsState()

    var showAddProfileDialog by remember { mutableStateOf(false) }
    var testDomainInput by remember { mutableStateOf("cloudflare.com") }

    // Official Android VpnService permission launcher
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.vpnManager.startVpn(activeProfile)
            Toast.makeText(context, "Permissão VPN concedida. Conectando...", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Permissão VPN cancelada pelo usuário.", Toast.LENGTH_SHORT).show()
        }
    }

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
                        imageVector = Icons.Default.VpnKey,
                        contentDescription = null,
                        tint = OpenVBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "VPN & DNS Seguro",
                        color = OpenVTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Gerenciamento de túnel nativo, perfis e resolução DoH",
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
            // Real Public IP Verification Card
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
                            Icon(Icons.Default.Public, contentDescription = null, tint = OpenVBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Endereço IP Público Real",
                                color = OpenVTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = { viewModel.vpnManager.refreshPublicIp() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Atualizar IP", tint = OpenVBlue, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (publicIpInfo != null) {
                        val info = publicIpInfo!!
                        Text(
                            text = info.ip,
                            color = OpenVTextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${info.city}, ${info.country} (${info.countryCode}) • ${info.isp} ${info.asNumber}",
                            color = OpenVTextSecondary,
                            fontSize = 12.sp
                        )
                    } else {
                        Text(
                            text = "Obtendo dados de rede pública via api.ipify.org...",
                            color = OpenVTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Native Android VpnService Control Card
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
                        Column {
                            Text(
                                text = "Túnel Nativo (VpnService)",
                                color = OpenVTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Servidor ativo: ${activeProfile?.name ?: "Nenhum"}",
                                color = OpenVTextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        val statusColor = when (vpnState) {
                            VpnConnectionState.CONNECTED -> OpenVShieldGreen
                            VpnConnectionState.CONNECTING, VpnConnectionState.RECONNECTING -> OpenVWarningOrange
                            VpnConnectionState.ERROR -> OpenVAlertRed
                            else -> OpenVTextSecondary
                        }

                        Surface(
                            color = statusColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = vpnState.labelPt,
                                color = statusColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("O motor VPN ainda não encaminha nem criptografa tráfego. Conexão, rotação de IP e kill switch estão indisponíveis. Use uma VPN externa.", color = OpenVWarningOrange, fontSize = 12.sp)
                    // Connect / Disconnect Action
                    if (vpnState == VpnConnectionState.CONNECTED) {
                        Button(
                            onClick = { viewModel.vpnManager.stopVpn() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = OpenVAlertRed),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Desconectar VPN", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            enabled = com.example.vpn.VpnManager.ENGINE_AVAILABLE,
                            onClick = {
                                if (activity != null && com.example.vpn.VpnManager.ENGINE_AVAILABLE) {
                                    val intent = viewModel.vpnManager.checkVpnPermission(activity)
                                    if (intent != null) {
                                        vpnPermissionLauncher.launch(intent)
                                    } else {
                                        viewModel.vpnManager.startVpn(activeProfile)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = OpenVBlue),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("VPN indisponível nesta versão", fontWeight = FontWeight.Bold)
                        }
                    }

                    Divider(color = OpenVBorder, modifier = Modifier.padding(vertical = 12.dp))

                    // Rotation per session toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Rotação por Nova Sessão", color = OpenVTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Altera o servidor ao iniciar uma nova sessão", color = OpenVTextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = autoRotate,
                            onCheckedChange = { viewModel.vpnManager.toggleAutoRotate(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = OpenVBlue,
                                checkedTrackColor = OpenVSurfaceElevated,
                                uncheckedThumbColor = OpenVTextSecondary,
                                uncheckedTrackColor = OpenVSurfaceElevated
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Kill Switch toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Kill Switch de Navegação", color = OpenVTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Bloqueia novas requisições se a VPN estiver inativa", color = OpenVTextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = killSwitchEnabled,
                            onCheckedChange = { viewModel.vpnManager.toggleKillSwitch(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = OpenVAlertRed,
                                checkedTrackColor = OpenVSurfaceElevated,
                                uncheckedThumbColor = OpenVTextSecondary,
                                uncheckedTrackColor = OpenVSurfaceElevated
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // VPN Server Profiles List
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Servidores VPN (${savedProfiles.size})",
                    color = OpenVTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = { showAddProfileDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Adicionar Servidor", tint = OpenVBlue)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            savedProfiles.forEach { profile ->
                val isSelected = profile.id == activeProfile?.id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { viewModel.vpnManager.selectProfile(profile) }
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) OpenVBlue else OpenVBorder,
                            shape = RoundedCornerShape(10.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) OpenVSurfaceElevated else OpenVSurface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { viewModel.vpnManager.selectProfile(profile) },
                            colors = RadioButtonDefaults.colors(selectedColor = OpenVBlue)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile.name,
                                color = OpenVTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${profile.serverHost}:${profile.serverPort} • ${profile.protocol.displayName}",
                                color = OpenVTextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        IconButton(
                            onClick = { viewModel.vpnManager.deleteProfile(profile.id) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Excluir perfil", tint = OpenVTextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // DNS-over-HTTPS (DoH) Client & Real Live Benchmark
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = OpenVSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, OpenVBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Dns, contentDescription = null, tint = OpenVShieldGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cliente DNS-over-HTTPS (DoH)",
                            color = OpenVTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Provedor ativo: ${dnsConfig.provider.displayName}",
                        color = OpenVTextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // DoH Test Execution
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = testDomainInput,
                            onValueChange = { testDomainInput = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Domínio para testar DoH...", color = OpenVTextSecondary, fontSize = 12.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = OpenVTextPrimary,
                                unfocusedTextColor = OpenVTextPrimary,
                                focusedBorderColor = OpenVBlue,
                                unfocusedBorderColor = OpenVBorder
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.executeDoHQuery(testDomainInput, "A")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OpenVShieldGreen),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = OpenVBackground, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Testar", color = OpenVBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    if (dohQueryResult != null) {
                        val result = dohQueryResult!!
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = OpenVSurfaceElevated,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${result.domain} (${result.recordType})",
                                        color = OpenVTextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${result.latencyMs} ms",
                                        color = if (result.latencyMs < 100) OpenVShieldGreen else OpenVWarningOrange,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = result.statusText,
                                    color = if (result.isSuccess) OpenVShieldGreen else OpenVAlertRed,
                                    fontSize = 11.sp
                                )

                                if (result.records.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    result.records.forEach { r ->
                                        Text(
                                            text = "• ${r.type}: ${r.data} (TTL ${r.ttl}s)",
                                            color = OpenVTextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // Add Profile Dialog
    if (showAddProfileDialog) {
        var profileName by remember { mutableStateOf("") }
        var serverHost by remember { mutableStateOf("") }
        var serverPort by remember { mutableStateOf("51820") }
        var selectedProtocol by remember { mutableStateOf(VpnProtocol.WIREGUARD) }

        Dialog(onDismissRequest = { showAddProfileDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = OpenVSurface),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, OpenVBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Cadastrar Servidor VPN",
                        color = OpenVTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = profileName,
                        onValueChange = { profileName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Nome do Servidor") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = serverHost,
                        onValueChange = { serverHost = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Endereço / Host") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = serverPort,
                        onValueChange = { serverPort = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Porta") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { showAddProfileDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = OpenVSurfaceElevated)
                        ) {
                            Text("Cancelar", color = OpenVTextSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (profileName.isNotBlank() && serverHost.isNotBlank()) {
                                    viewModel.vpnManager.addProfile(
                                        VpnProfile(
                                            name = profileName,
                                            serverHost = serverHost,
                                            serverPort = serverPort.toIntOrNull() ?: 51820,
                                            protocol = selectedProtocol
                                        )
                                    )
                                    showAddProfileDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OpenVBlue)
                        ) {
                            Text("Salvar")
                        }
                    }
                }
            }
        }
    }
}
