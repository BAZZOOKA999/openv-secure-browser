package com.example.ui.browser.menu

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.security.TrackerBlocker
import com.example.ui.theme.OpenVBackground
import com.example.ui.theme.OpenVBlue
import com.example.ui.theme.OpenVBorder
import com.example.ui.theme.OpenVShieldGreen
import com.example.ui.theme.OpenVSurface
import com.example.ui.theme.OpenVSurfaceElevated
import com.example.ui.theme.OpenVTextPrimary
import com.example.ui.theme.OpenVTextSecondary

@Composable
fun ExtensionsDialog(
    pageTitle: String,
    pageUrl: String,
    isDarkModeActive: Boolean,
    isReaderModeActive: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    onToggleReaderMode: (Boolean) -> Unit,
    onExecuteUserScript: (String) -> Unit,
    onViewSource: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var trackerBlockingEnabled by remember { mutableStateOf(TrackerBlocker.isEnabled) }
    var blockedCount by remember { mutableStateOf(TrackerBlocker.blockedCount) }
    var customScriptText by remember { mutableStateOf("") }
    var showScriptEditor by remember { mutableStateOf(false) }

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
                            imageVector = Icons.Default.Extension,
                            contentDescription = null,
                            tint = OpenVBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Extensões e Ferramentas",
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
                    text = "Módulos nativos integrados do OpenV. Sem lojas falsas ou extensões inseguras.",
                    color = OpenVTextSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Tool 1: Tracker & Ad Blocker
                    item {
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
                                        Icon(Icons.Default.Shield, contentDescription = null, tint = OpenVShieldGreen, modifier = Modifier.size(22.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("Bloqueador de Rastreadores", color = OpenVTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                            Text("$blockedCount domínios interceptados", color = OpenVShieldGreen, fontSize = 11.sp)
                                        }
                                    }

                                    Switch(
                                        checked = trackerBlockingEnabled,
                                        onCheckedChange = {
                                            trackerBlockingEnabled = it
                                            TrackerBlocker.isEnabled = it
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = OpenVShieldGreen,
                                            checkedTrackColor = OpenVShieldGreen.copy(alpha = 0.3f)
                                        )
                                    )
                                }

                                if (trackerBlockingEnabled) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                TrackerBlocker.resetCounter()
                                                blockedCount = 0
                                                Toast.makeText(context, "Contador zerado", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Zerar Contador", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Tool 2: Reader Mode
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = OpenVSurface),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, OpenVBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.MenuBook, contentDescription = null, tint = OpenVBlue, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("Modo Leitura (Reader Mode)", color = OpenVTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Text("Oculta barras laterais, pop-ups e formata tipografia", color = OpenVTextSecondary, fontSize = 11.sp)
                                    }
                                }

                                Switch(
                                    checked = isReaderModeActive,
                                    onCheckedChange = { active ->
                                        onToggleReaderMode(active)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = OpenVBlue,
                                        checkedTrackColor = OpenVBlue.copy(alpha = 0.3f)
                                    )
                                )
                            }
                        }
                    }

                    // Tool 3: Web Dark Mode
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = OpenVSurface),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, OpenVBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.DarkMode, contentDescription = null, tint = OpenVBlue, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("Modo Escuro Forçado", color = OpenVTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Text("Inverte e ajusta contraste em páginas claras", color = OpenVTextSecondary, fontSize = 11.sp)
                                    }
                                }

                                Switch(
                                    checked = isDarkModeActive,
                                    onCheckedChange = { active ->
                                        onToggleDarkMode(active)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = OpenVBlue,
                                        checkedTrackColor = OpenVBlue.copy(alpha = 0.3f)
                                    )
                                )
                            }
                        }
                    }

                    // Tool 4: Copy Formatted Title & URL
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = OpenVSurface),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, OpenVBorder)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = OpenVBlue, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Copiar Título e URL Formatados", color = OpenVTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val md = "[$pageTitle]($pageUrl)"
                                            clipboard.setPrimaryClip(ClipData.newPlainText("Markdown Link", md))
                                            Toast.makeText(context, "Copiado em formato Markdown!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Markdown", fontSize = 12.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val text = "$pageTitle\n$pageUrl"
                                            clipboard.setPrimaryClip(ClipData.newPlainText("Texto e URL", text))
                                            Toast.makeText(context, "Copiado em texto puro!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Texto Puro", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Tool 5: Page Source Viewer
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = OpenVSurface),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, OpenVBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Code, contentDescription = null, tint = OpenVBlue, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("Código-Fonte da Página", color = OpenVTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Text("Inspecione a estrutura HTML do site ativo", color = OpenVTextSecondary, fontSize = 11.sp)
                                    }
                                }

                                Button(
                                    onClick = {
                                        onViewSource()
                                        onDismiss()
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = OpenVBlue)
                                ) {
                                    Text("Ver Fonte", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // Tool 6: Local User Script Injector
                    item {
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
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = OpenVBlue, modifier = Modifier.size(22.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Injetor de Scripts Locais", color = OpenVTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = { showScriptEditor = !showScriptEditor },
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(if (showScriptEditor) "Recolher" else "Expandir", fontSize = 11.sp)
                                    }
                                }

                                if (showScriptEditor) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Execute trechos seguros de JavaScript na página ativa:",
                                        color = OpenVTextSecondary,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Quick presets
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                customScriptText = "document.querySelectorAll('*').forEach(e => { if (getComputedStyle(e).position === 'fixed') e.remove(); });"
                                            },
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Remover Fixos", fontSize = 10.sp)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                customScriptText = "document.designMode = document.designMode === 'on' ? 'off' : 'on';"
                                            },
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Editar Texto", fontSize = 10.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = customScriptText,
                                        onValueChange = { customScriptText = it },
                                        placeholder = { Text("console.log('OpenV script');", color = OpenVTextSecondary, fontSize = 12.sp) },
                                        modifier = Modifier.fillMaxWidth(),
                                        maxLines = 4,
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            color = OpenVTextPrimary,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp
                                        ),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = OpenVBlue,
                                            unfocusedBorderColor = OpenVBorder
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Button(
                                        onClick = {
                                            if (customScriptText.isNotBlank()) {
                                                onExecuteUserScript(customScriptText)
                                                Toast.makeText(context, "Script executado no WebView", Toast.LENGTH_SHORT).show()
                                                onDismiss()
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = OpenVShieldGreen),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = OpenVBackground, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Executar na Página", color = OpenVBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
