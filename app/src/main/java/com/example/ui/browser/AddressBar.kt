package com.example.ui.browser

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TabItem
import com.example.security.TrackerBlocker
import com.example.ui.theme.OpenVAlertRed
import com.example.ui.theme.OpenVBlue
import com.example.ui.theme.OpenVBorder
import com.example.ui.theme.OpenVShieldGreen
import com.example.ui.theme.OpenVSurface
import com.example.ui.theme.OpenVSurfaceElevated
import com.example.ui.theme.OpenVSurfaceVariant
import com.example.ui.theme.OpenVTextPrimary
import com.example.ui.theme.OpenVTextSecondary
import java.net.URLEncoder

@Composable
fun AddressBar(
    tab: TabItem?,
    tabCount: Int,
    isBookmarked: Boolean,
    onNavigate: (String) -> Unit,
    onReload: () -> Unit,
    onHome: () -> Unit,
    onOpenTabSwitcher: () -> Unit,
    onOpenBookmarksAndHistory: () -> Unit,
    onOpenDownloads: () -> Unit,
    onToggleBookmark: () -> Unit,
    onToggleDesktopMode: () -> Unit,
    onOpenNewPrivateTab: () -> Unit,
    onClearData: () -> Unit,
    onFindInPage: (String?) -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    var isEditing by remember { mutableStateOf(false) }
    var isFindingInPage by remember { mutableStateOf(false) }
    var findText by remember { mutableStateOf("") }

    var inputText by remember(tab?.url) {
        val displayUrl = when {
            tab == null || tab.url == "openv://home" || tab.url == "about:blank" -> ""
            else -> tab.url
        }
        mutableStateOf(displayUrl)
    }
    var menuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(OpenVSurface)
    ) {
        // Find in page bar
        AnimatedVisibility(visible = isFindingInPage) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(OpenVSurfaceElevated)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.FindInPage, contentDescription = null, tint = OpenVBlue, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                OutlinedTextField(
                    value = findText,
                    onValueChange = {
                        findText = it
                        onFindInPage(it)
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Localizar no texto da página...", color = OpenVTextSecondary, fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = OpenVTextPrimary,
                        unfocusedTextColor = OpenVTextPrimary,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    )
                )
                IconButton(onClick = {
                    isFindingInPage = false
                    findText = ""
                    onFindInPage(null)
                }) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar busca na página", tint = OpenVTextSecondary)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home button
            IconButton(
                onClick = onHome,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("home_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Página Inicial",
                    tint = if (tab?.url == "openv://home") OpenVBlue else OpenVTextSecondary
                )
            }

            // Address / Search Input Pill
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .testTag("address_bar_container"),
                color = OpenVSurfaceVariant,
                shape = RoundedCornerShape(22.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isHttps = tab?.url?.startsWith("https://") == true
                    val isHome = tab?.url == "openv://home" || tab?.url == "about:blank"
                    val iconColor = when {
                        isHome -> OpenVBlue
                        isHttps -> OpenVShieldGreen
                        tab?.errorMessage != null -> OpenVAlertRed
                        else -> OpenVAlertRed
                    }

                    Icon(
                        imageVector = if (tab?.isPrivate == true) Icons.Default.Security else if (isHttps) Icons.Default.Lock else Icons.Default.Warning,
                        contentDescription = "Status de Segurança",
                        tint = iconColor,
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    if (isEditing) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester)
                                .testTag("address_input_field"),
                            placeholder = {
                                Text("Buscar ou digitar URL...", color = OpenVTextSecondary, fontSize = 14.sp)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Go
                            ),
                            keyboardActions = KeyboardActions(
                                onGo = {
                                    onNavigate(inputText)
                                    isEditing = false
                                    focusManager.clearFocus()
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = OpenVTextPrimary,
                                unfocusedTextColor = OpenVTextPrimary,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                cursorColor = OpenVBlue
                            )
                        )

                        LaunchedEffect(Unit) {
                            focusRequester.requestFocus()
                        }

                        IconButton(
                            onClick = {
                                isEditing = false
                                inputText = if (tab?.url == "openv://home") "" else (tab?.url ?: "")
                                focusManager.clearFocus()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancelar edição",
                                tint = OpenVTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        val displayText = when {
                            tab == null || tab.url == "openv://home" || tab.url == "about:blank" -> "Pesquisar ou digitar URL..."
                            else -> tab.url.removePrefix("https://").removePrefix("http://")
                        }

                        Text(
                            text = displayText,
                            color = if (isHome) OpenVTextSecondary else OpenVTextPrimary,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    isEditing = true
                                    inputText = if (isHome) "" else (tab?.url ?: "")
                                }
                        )

                        if (!isHome) {
                            IconButton(
                                onClick = onReload,
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("reload_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Recarregar página",
                                    tint = OpenVTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Tabs button with badge
            IconButton(
                onClick = onOpenTabSwitcher,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("tabs_switcher_button")
            ) {
                BadgedBox(
                    badge = {
                        Badge(
                            containerColor = if (tab?.isPrivate == true) OpenVShieldGreen else OpenVBlue,
                            contentColor = OpenVSurface
                        ) {
                            Text(text = "$tabCount", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                    }
                ) {
                    Icon(imageVector = Icons.Default.Tab, contentDescription = "Abas abertas", tint = OpenVTextPrimary)
                }
            }

            // Overflow Menu
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("menu_button")
                ) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Mais opções", tint = OpenVTextPrimary)
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(OpenVSurface)
                ) {
                    // Bookmark toggle
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (isBookmarked) "Remover dos Favoritos" else "Adicionar aos Favoritos",
                                color = OpenVTextPrimary
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                tint = if (isBookmarked) OpenVBlue else OpenVTextSecondary
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onToggleBookmark()
                        }
                    )

                    // Find in Page
                    DropdownMenuItem(
                        text = { Text("Localizar na Página", color = OpenVTextPrimary) },
                        leadingIcon = { Icon(Icons.Default.FindInPage, contentDescription = null, tint = OpenVTextSecondary) },
                        onClick = {
                            menuExpanded = false
                            isFindingInPage = true
                        }
                    )

                    // Zoom In & Zoom Out
                    DropdownMenuItem(
                        text = { Text("Aumentar Zoom (+)", color = OpenVTextPrimary) },
                        leadingIcon = { Icon(Icons.Default.ZoomIn, contentDescription = null, tint = OpenVTextSecondary) },
                        onClick = {
                            menuExpanded = false
                            onZoomIn()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Diminuir Zoom (-)", color = OpenVTextPrimary) },
                        leadingIcon = { Icon(Icons.Default.ZoomOut, contentDescription = null, tint = OpenVTextSecondary) },
                        onClick = {
                            menuExpanded = false
                            onZoomOut()
                        }
                    )

                    // Translate Page
                    DropdownMenuItem(
                        text = { Text("Traduzir Página (Web)", color = OpenVTextPrimary) },
                        leadingIcon = { Icon(Icons.Default.Translate, contentDescription = null, tint = OpenVTextSecondary) },
                        onClick = {
                            menuExpanded = false
                            tab?.url?.let {
                                if (!it.startsWith("openv://")) {
                                    val translateUrl = "https://translate.google.com/translate?sl=auto&tl=pt&u=" + URLEncoder.encode(it, "UTF-8")
                                    onNavigate(translateUrl)
                                }
                            }
                        }
                    )

                    // Bookmarks & History
                    DropdownMenuItem(
                        text = { Text("Favoritos e Histórico", color = OpenVTextPrimary) },
                        leadingIcon = { Icon(Icons.Default.History, contentDescription = null, tint = OpenVTextSecondary) },
                        onClick = {
                            menuExpanded = false
                            onOpenBookmarksAndHistory()
                        }
                    )

                    // Downloads Manager
                    DropdownMenuItem(
                        text = { Text("Downloads", color = OpenVTextPrimary) },
                        leadingIcon = { Icon(Icons.Default.Download, contentDescription = null, tint = OpenVTextSecondary) },
                        onClick = {
                            menuExpanded = false
                            onOpenDownloads()
                        }
                    )

                    // Desktop mode toggle
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (tab?.isDesktopMode == true) "Modo Celular" else "Modo Computador",
                                color = OpenVTextPrimary
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Computer,
                                contentDescription = null,
                                tint = if (tab?.isDesktopMode == true) OpenVShieldGreen else OpenVTextSecondary
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onToggleDesktopMode()
                        }
                    )

                    // Share URL
                    DropdownMenuItem(
                        text = { Text("Compartilhar Link", color = OpenVTextPrimary) },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = OpenVTextSecondary) },
                        onClick = {
                            menuExpanded = false
                            tab?.url?.let { url ->
                                if (url != "openv://home") {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, url)
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Compartilhar Link"))
                                }
                            }
                        }
                    )

                    // New Private Tab
                    DropdownMenuItem(
                        text = { Text("Nova Aba Privada", color = OpenVShieldGreen) },
                        leadingIcon = { Icon(Icons.Default.Security, contentDescription = null, tint = OpenVShieldGreen) },
                        onClick = {
                            menuExpanded = false
                            onOpenNewPrivateTab()
                        }
                    )

                    // Clear browsing data
                    DropdownMenuItem(
                        text = { Text("Limpar Dados de Navegação", color = OpenVAlertRed) },
                        leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = OpenVAlertRed) },
                        onClick = {
                            menuExpanded = false
                            onClearData()
                        }
                    )
                }
            }
        }

        // Real Progress Indicator
        AnimatedVisibility(visible = tab?.isLoading == true && tab.progress in 1..99) {
            LinearProgressIndicator(
                progress = { (tab?.progress ?: 0) / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
                color = if (tab?.isPrivate == true) OpenVShieldGreen else OpenVBlue,
                trackColor = Color.Transparent
            )
        }
    }
}
