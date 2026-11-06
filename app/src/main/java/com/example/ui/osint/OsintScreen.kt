package com.example.ui.osint

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.OsintReportEntity
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
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OsintScreen(
    viewModel: BrowserViewModel,
    onLaunchQueryInBrowser: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    var activeTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf(
        "A. Domínios",
        "B. Endereços IP",
        "C. Dorks Avançados",
        "D. Usuários",
        "E. Imagens EXIF",
        "F. Links & Redirecionamentos",
        "G. Relatórios"
    )

    // State collections
    val dohResult by viewModel.dohQueryResult.collectAsState()
    val httpResult by viewModel.httpInspectionResult.collectAsState()
    val socialResults by viewModel.socialCheckResults.collectAsState()
    val exifResult by viewModel.exifResult.collectAsState()
    val sanitizedFile by viewModel.sanitizedImageFile.collectAsState()
    val savedReports by viewModel.osintReports.collectAsState()
    val rdapResult by viewModel.rdapResult.collectAsState()

    // Photo picker for EXIF
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.inspectExif(uri)
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
                        imageVector = Icons.Default.TravelExplore,
                        contentDescription = null,
                        tint = OpenVBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Central OSINT Completa",
                        color = OpenVTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = OpenVShieldGreen.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "OPERAÇÃO REAL",
                            color = OpenVShieldGreen,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "Inteligência de fontes abertas, análise de DNS, IP, Dorks, metadados e relatórios",
                    color = OpenVTextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // Horizontal Category Tabs
        ScrollableTabRow(
            selectedTabIndex = activeTab,
            containerColor = OpenVSurfaceElevated,
            contentColor = OpenVBlue,
            edgePadding = 12.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                    color = OpenVBlue
                )
            }
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = activeTab == index,
                    onClick = { activeTab = index },
                    text = {
                        Text(
                            text = title,
                            color = if (activeTab == index) OpenVBlue else OpenVTextSecondary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                )
            }
        }

        // Module Body
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            when (activeTab) {
                0 -> {
                    // MODULO A: DOMINIOS
                    var domainInput by remember { mutableStateOf("google.com") }
                    var selectedRecordType by remember { mutableStateOf("A") }
                    val recordTypes = listOf("A", "AAAA", "MX", "NS", "TXT", "SOA")

                    Text("Reconhecimento de Domínio", color = OpenVTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = domainInput,
                        onValueChange = { domainInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Domínio alvo (ex: exemplo.com)") },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(recordTypes) { rType ->
                            val isSel = rType == selectedRecordType
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedRecordType = rType },
                                label = { Text(rType) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = OpenVBlue,
                                    selectedLabelColor = OpenVBackground
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.executeDoHQuery(domainInput, selectedRecordType) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = OpenVBlue),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Consultar DNS DoH")
                        }

                        Button(
                            onClick = { onLaunchQueryInBrowser("https://crt.sh/?q=$domainInput") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = OpenVSurfaceElevated),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Certificados (crt.sh)", color = OpenVTextPrimary, fontSize = 12.sp)
                        }
                    }

                    if (dohResult != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        val res = dohResult!!
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = OpenVSurface),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, OpenVBorder)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Resultado DoH: ${res.statusText}", color = OpenVShieldGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("${res.latencyMs} ms", color = OpenVBlue, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                res.records.forEach { r ->
                                    Text("• ${r.type}: ${r.data} (TTL ${r.ttl}s)", color = OpenVTextPrimary, fontSize = 12.sp)
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = {
                                        val summary = res.records.joinToString("\n") { "${it.type}: ${it.data}" }
                                        viewModel.saveOsintReport(
                                            title = "DNS Recon: ${res.domain} (${res.recordType})",
                                            category = "Domínios",
                                            target = res.domain,
                                            rawData = summary,
                                            summary = "Resolvido via DoH com ${res.latencyMs}ms de latência."
                                        )
                                        Toast.makeText(context, "Salvo em Relatórios!", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Salvar em Relatórios", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // MODULO B: ENDEREÇOS IP
                    var ipInput by remember { mutableStateOf("1.1.1.1") }

                    Text("Investigação de Endereço IP & ASN", color = OpenVTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = ipInput,
                        onValueChange = { ipInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Endereço IPv4 ou IPv6") },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.lookupRdap(ipInput) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = OpenVBlue),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Consultar RDAP / ARIN")
                        }
                        Button(
                            onClick = { onLaunchQueryInBrowser("https://bgp.he.net/ip/$ipInput") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = OpenVSurfaceElevated),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Rotas BGP (HE)", color = OpenVTextPrimary, fontSize = 12.sp)
                        }
                    }

                    if (rdapResult != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = OpenVSurface),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, OpenVBorder)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Resposta RDAP Oficial (ARIN / IANA):", color = OpenVShieldGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = rdapResult!!.take(1200) + if (rdapResult!!.length > 1200) "\n... [truncado para exibição]" else "",
                                    color = OpenVTextSecondary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        viewModel.saveOsintReport(
                                            title = "RDAP IP: $ipInput",
                                            category = "Endereços IP",
                                            target = ipInput,
                                            rawData = rdapResult!!,
                                            summary = "Registro oficial de alocação de bloco IP via ARIN."
                                        )
                                        Toast.makeText(context, "Salvo em Relatórios!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = OpenVBlue),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Salvar Relatório")
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // MODULO C: PESQUISA AVANCADA / DORKS
                    var targetDomain by remember { mutableStateOf("") }
                    var dorkType by remember { mutableStateOf("PDF/DOC") }

                    Text("Gerador de Google & Bing Dorks", color = OpenVTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("Criação de filtros autorizados de busca em fontes públicas", color = OpenVTextSecondary, fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = targetDomain,
                        onValueChange = { targetDomain = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Domínio ou termo alvo (opcional)") },
                        placeholder = { Text("ex: gov.br ou empresa.com") },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    val dorkPresets = listOf(
                        Triple("Documentos Expostos", "filetype:pdf OR filetype:doc OR filetype:xlsx", "PDF/DOC"),
                        Triple("Diretórios Abertos", "intitle:\"index of /\" OR \"parent directory\"", "Index of"),
                        Triple("Painéis de Login", "inurl:admin OR inurl:login OR inurl:portal", "Admin"),
                        Triple("Arquivos de Config / Env", "filetype:env OR filetype:yaml OR filetype:conf", "Config"),
                        Triple("Subdomínios Públicos", "site:*.{target} -www.{target}", "Subdomínios")
                    )

                    dorkPresets.forEach { (label, syntax, badge) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = OpenVSurface),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, OpenVBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(label, color = OpenVTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text(syntax, color = OpenVTextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }

                                Button(
                                    onClick = {
                                        val finalQuery = if (targetDomain.isNotBlank()) {
                                            "site:$targetDomain $syntax"
                                        } else {
                                            syntax
                                        }
                                        onLaunchQueryInBrowser("https://duckduckgo.com/?q=" + URLEncoder.encode(finalQuery, "UTF-8"))
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = OpenVBlue),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Executar", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // MODULO D: USUARIOS PUBLICOS
                    var usernameInput by remember { mutableStateOf("") }

                    Text("Identificação de Nomes de Usuário Públicos", color = OpenVTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("Verifica a presença pública de um handle em múltiplas plataformas legais", color = OpenVTextSecondary, fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Nome de usuário (ex: johndoe)") },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (usernameInput.isNotBlank()) {
                                viewModel.checkUsernames(usernameInput)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = OpenVBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Verificar Perfil em Redes Públicas")
                    }

                    if (socialResults.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        socialResults.forEach { item ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { onLaunchQueryInBrowser(item.profileUrl) },
                                colors = CardDefaults.cardColors(containerColor = OpenVSurface),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, OpenVBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.platform, color = OpenVTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text(item.profileUrl, color = OpenVTextSecondary, fontSize = 11.sp)
                                    }

                                    Surface(
                                        color = if (item.exists) OpenVShieldGreen.copy(alpha = 0.15f) else OpenVAlertRed.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (item.exists) "EXISTE (200)" else "NÃO ENCONTRADO",
                                            color = if (item.exists) OpenVShieldGreen else OpenVAlertRed,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                4 -> {
                    // MODULO E: IMAGENS & EXIF
                    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

                    Text("Análise & Higienização EXIF de Imagens", color = OpenVTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("Examine coordenadas GPS, câmera e remova metadados com um toque", color = OpenVTextSecondary, fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { photoPickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = OpenVBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Selecionar Imagem do Dispositivo")
                    }

                    if (exifResult != null) {
                        val exif = exifResult!!
                        Spacer(modifier = Modifier.height(14.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = OpenVSurface),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, OpenVBorder)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Metadados Extraídos:", color = OpenVTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("• Câmera: ${exif.cameraMake} ${exif.cameraModel}", color = OpenVTextSecondary, fontSize = 12.sp)
                                Text("• Data/Hora: ${exif.dateTime}", color = OpenVTextSecondary, fontSize = 12.sp)
                                Text("• ISO / Exposição: ${exif.iso} / ${exif.exposureTime}", color = OpenVTextSecondary, fontSize = 12.sp)
                                Text("• Abertura: f/${exif.fNumber} • Foco: ${exif.focalLength}", color = OpenVTextSecondary, fontSize = 12.sp)

                                if (exif.hasGps && exif.latitude != null && exif.longitude != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Surface(color = OpenVAlertRed.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                                        Text(
                                            "ALERTA: Coordenadas GPS presentes! Lat: ${exif.latitude}, Lon: ${exif.longitude}",
                                            color = OpenVAlertRed,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(6.dp)
                                        )
                                    }
                                } else {
                                    Text("• Geolocalização GPS: Ausente (Seguro)", color = OpenVShieldGreen, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                5 -> {
                    // MODULO F: SEGURANÇA DE LINKS & REDIRECIONAMENTOS
                    var linkInput by remember { mutableStateOf("https://example.com") }

                    Text("Inspeção de Links & Redirecionamentos", color = OpenVTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = linkInput,
                        onValueChange = { linkInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("URL completa para teste") },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.inspectUrlHeaders(linkInput) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = OpenVBlue),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Inspecionar Headers")
                        }
                        Button(
                            onClick = { onLaunchQueryInBrowser("https://www.virustotal.com/gui/search/" + URLEncoder.encode(linkInput, "UTF-8")) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = OpenVSurfaceElevated),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("VirusTotal Scan", color = OpenVTextPrimary, fontSize = 12.sp)
                        }
                    }

                    if (httpResult != null) {
                        val h = httpResult!!
                        Spacer(modifier = Modifier.height(14.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = OpenVSurface),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, OpenVBorder)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Status Final: HTTP ${h.statusCode} (${h.latencyMs}ms)", color = OpenVShieldGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Cadeia de Redirecionamentos (${h.redirectChain.size} hops):", color = OpenVTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                h.redirectChain.forEachIndexed { i, url ->
                                    Text(" ${i + 1}. $url", color = OpenVTextSecondary, fontSize = 11.sp)
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Cabeçalhos de Segurança Detectados:", color = OpenVTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                h.securityHeaders.forEach { (sec, present) ->
                                    Text(
                                        text = "• $sec: " + if (present) "Presente (Ativo)" else "Ausente",
                                        color = if (present) OpenVShieldGreen else OpenVAlertRed,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                6 -> {
                    // MODULO G: RELATÓRIOS SALVOS
                    Text("Relatórios e Investigações Salvas (${savedReports.size})", color = OpenVTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (savedReports.isEmpty()) {
                        Text("Nenhum relatório salvo ainda. Execute consultas em outras abas e clique em 'Salvar Relatório'.", color = OpenVTextSecondary, fontSize = 12.sp)
                    } else {
                        savedReports.forEach { rep ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = OpenVSurface),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, OpenVBorder)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(rep.title, color = OpenVTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        IconButton(onClick = { viewModel.deleteOsintReport(rep) }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = null, tint = OpenVAlertRed, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    Text("Categoria: ${rep.category} • Alvo: ${rep.target}", color = OpenVTextSecondary, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(rep.summary, color = OpenVTextPrimary, fontSize = 12.sp)

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_SUBJECT, "Relatório OSINT OpenV: ${rep.title}")
                                                putExtra(Intent.EXTRA_TEXT, "${rep.title}\nAlvo: ${rep.target}\n\nResumo:\n${rep.summary}\n\nDados brutos:\n${rep.rawData}")
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Compartilhar Relatório OSINT"))
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = OpenVBlue),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Exportar / Compartilhar", fontSize = 11.sp)
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
}
