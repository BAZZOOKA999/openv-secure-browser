package com.example.ui.browser.menu

import android.webkit.CookieManager
import android.webkit.WebStorage
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class CookieCleanScope {
    ALL_COOKIES,
    SESSION_ONLY
}

@Composable
fun ClearCookiesDialog(
    onClearComplete: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var cleanScope by remember { mutableStateOf(CookieCleanScope.ALL_COOKIES) }
    var alsoClearStorage by remember { mutableStateOf(true) }
    var alsoClearCache by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { if (!isProcessing) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = OpenVSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, OpenVBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Cookie,
                            contentDescription = null,
                            tint = OpenVWarningOrange,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Limpar Cookies e Sessões",
                            color = OpenVTextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (!isProcessing) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar", tint = OpenVTextSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Warning Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = OpenVSurfaceElevated),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = OpenVAlertRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Atenção: A exclusão de cookies desconectará suas contas em sites ativos e limpará tokens de autenticação armazenados.",
                            color = OpenVTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Selecione o escopo da limpeza:",
                    color = OpenVTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Radio 1: All Cookies
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = cleanScope == CookieCleanScope.ALL_COOKIES,
                        onClick = { cleanScope = CookieCleanScope.ALL_COOKIES },
                        colors = RadioButtonDefaults.colors(selectedColor = OpenVBlue)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text("Todos os Cookies", color = OpenVTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("Remove cookies permanentes e temporários de todos os sites", color = OpenVTextSecondary, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Radio 2: Session Only
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = cleanScope == CookieCleanScope.SESSION_ONLY,
                        onClick = { cleanScope = CookieCleanScope.SESSION_ONLY },
                        colors = RadioButtonDefaults.colors(selectedColor = OpenVBlue)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text("Apenas Cookies de Sessão", color = OpenVTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("Mantém logins permanentes e remove rastreadores de sessão", color = OpenVTextSecondary, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Checkboxes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = alsoClearStorage,
                        onCheckedChange = { alsoClearStorage = it },
                        colors = CheckboxDefaults.colors(checkedColor = OpenVBlue, checkmarkColor = OpenVBackground)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text("Limpar WebStorage (localStorage / sessionStorage)", color = OpenVTextPrimary, fontSize = 12.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = alsoClearCache,
                        onCheckedChange = { alsoClearCache = it },
                        colors = CheckboxDefaults.colors(checkedColor = OpenVBlue, checkmarkColor = OpenVBackground)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text("Limpar Cache HTTP do WebView", color = OpenVTextPrimary, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !isProcessing,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Cancelar", fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            isProcessing = true
                            coroutineScope.launch {
                                withContext(Dispatchers.IO) {
                                    val cookieManager = CookieManager.getInstance()
                                    if (cleanScope == CookieCleanScope.ALL_COOKIES) {
                                        cookieManager.removeAllCookies(null)
                                    } else {
                                        cookieManager.removeSessionCookies(null)
                                    }
                                    cookieManager.flush()

                                    if (alsoClearStorage) {
                                        WebStorage.getInstance().deleteAllData()
                                    }
                                }

                                Toast.makeText(context, "Cookies removidos com sucesso!", Toast.LENGTH_SHORT).show()
                                isProcessing = false
                                onClearComplete()
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1.2f),
                        enabled = !isProcessing,
                        colors = ButtonDefaults.buttonColors(containerColor = OpenVAlertRed),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                color = OpenVBackground,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Limpando...", color = OpenVBackground, fontSize = 13.sp)
                        } else {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = OpenVBackground, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Limpar Agora", color = OpenVBackground, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
