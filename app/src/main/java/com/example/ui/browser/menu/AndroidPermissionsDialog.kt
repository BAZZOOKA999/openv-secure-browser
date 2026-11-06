package com.example.ui.browser.menu

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
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

data class PermissionItem(
    val id: String,
    val permission: String?,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val whyNeeded: String
)

@Composable
fun AndroidPermissionsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var refreshKey by remember { mutableStateOf(0) }

    var requestingPermission by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        refreshKey++
    }

    val permissionList = remember {
        listOf(
            PermissionItem(
                id = "notifications",
                permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.POST_NOTIFICATIONS else null,
                title = "Notificações do Sistema",
                description = "Status de downloads, alertas de VPN e finalização de tarefas OSINT.",
                icon = Icons.Default.Notifications,
                whyNeeded = "Avisar quando um download for concluído ou quando a proteção de rede for alternada."
            ),
            PermissionItem(
                id = "camera",
                permission = Manifest.permission.CAMERA,
                title = "Câmera (WebRTC)",
                description = "Acesso a videochamadas WebRTC e leitura de QR Codes em sites.",
                icon = Icons.Default.CameraAlt,
                whyNeeded = "Permitir reuniões ou leitura de QR Codes iniciados estritamente sob sua solicitação."
            ),
            PermissionItem(
                id = "mic",
                permission = Manifest.permission.RECORD_AUDIO,
                title = "Microfone (Áudio)",
                description = "Pesquisa por voz e comunicação em chamadas web.",
                icon = Icons.Default.Mic,
                whyNeeded = "Permitir mensagens de voz em mensageiros web e conferências."
            ),
            PermissionItem(
                id = "location",
                permission = Manifest.permission.ACCESS_FINE_LOCATION,
                title = "Localização Precisa",
                description = "Geolocalização para mapas interativos e serviços de clima.",
                icon = Icons.Default.MyLocation,
                whyNeeded = "Sites de mapas solicitam latitude/longitude. O OpenV isola e nunca compartilha sem confirmação."
            ),
            PermissionItem(
                id = "storage",
                permission = null,
                title = "Armazenamento & Downloads",
                description = "Gerenciado nativamente pelo DownloadManager do Android.",
                icon = Icons.Default.FolderSpecial,
                whyNeeded = "O OpenV utiliza o DownloadManager seguro e Photo Picker do Android, sem exigir acesso aos seus arquivos pessoais."
            )
        )
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
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = OpenVBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Permissões do Android",
                            color = OpenVTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = OpenVTextPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Gerenciamento transparente de privacidade. Nenhum recurso é acessado em segundo plano.",
                    color = OpenVTextSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(permissionList.size) { idx ->
                        val item = permissionList[idx]
                        val isGranted = if (item.permission != null) {
                            ContextCompat.checkSelfPermission(context, item.permission) == PackageManager.PERMISSION_GRANTED
                        } else {
                            true // e.g. scoped storage / DownloadManager
                        }

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
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(item.icon, contentDescription = null, tint = OpenVBlue, modifier = Modifier.size(22.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(item.title, color = OpenVTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                            Text(item.description, color = OpenVTextSecondary, fontSize = 11.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Surface(
                                        color = if (isGranted) OpenVShieldGreen.copy(alpha = 0.15f) else OpenVAlertRed.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = if (isGranted) OpenVShieldGreen else OpenVAlertRed,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isGranted) "Concedida" else "Não Concedida",
                                                color = if (isGranted) OpenVShieldGreen else OpenVAlertRed,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "Finalidade: ${item.whyNeeded}",
                                    color = OpenVTextSecondary,
                                    fontSize = 11.sp
                                )

                                if (item.permission != null && !isGranted) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Button(
                                            onClick = {
                                                requestingPermission = item.permission
                                                permissionLauncher.launch(item.permission)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = OpenVBlue),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text("Solicitar Permissão", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // System Settings Shortcut
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            val generalSettings = Intent(Settings.ACTION_SETTINGS)
                            context.startActivity(generalSettings)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = OpenVSurfaceElevated),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OpenVBorder)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = OpenVTextPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Abrir Configurações do App no Android",
                        color = OpenVTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
