package com.example.ui.osint

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.OsintToolItem
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

data class OsintCategoryDef(
    val name: String,
    val icon: ImageVector,
    val description: String,
    val tools: List<OsintToolItem>
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OsintScreen(
    onLaunchQueryInBrowser: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    var selectedCategoryIndex by remember { mutableStateOf(0) }
    var targetInput by remember { mutableStateOf("") }

    val categories = remember {
        listOf(
            OsintCategoryDef(
                name = "Domínios",
                icon = Icons.Default.Language,
                description = "Investigação de domínios, WHOIS e histórico de registro",
                tools = listOf(
                    OsintToolItem(
                        id = "whois_lookup",
                        title = "WHOIS & DNS (DomainTools)",
                        description = "Consulta pública de registro de domínio",
                        category = "Domínios",
                        urlTemplate = "https://whois.domaintools.com/{target}",
                        placeholderHint = "ex: exemplo.com"
                    ),
                    OsintToolItem(
                        id = "rdap_lookup",
                        title = "Registro RDAP Oficial (IANA)",
                        description = "Protocolo de Acesso a Dados de Registro da IANA",
                        category = "Domínios",
                        urlTemplate = "https://rdap.arin.net/registry/ip/{target}",
                        placeholderHint = "ex: exemplo.com ou IP"
                    ),
                    OsintToolItem(
                        id = "crt_sh_domain",
                        title = "Certificados crt.sh",
                        description = "Transparência de certificados SSL/TLS para subdomínios",
                        category = "Domínios",
                        urlTemplate = "https://crt.sh/?q={target}",
                        placeholderHint = "ex: %.exemplo.com"
                    )
                )
            ),
            OsintCategoryDef(
                name = "Endereços IP",
                icon = Icons.Default.Public,
                description = "Geolocalização, ASN e blocos CIDR de rede",
                tools = listOf(
                    OsintToolItem(
                        id = "ipinfo_lookup",
                        title = "IPinfo Geolocation & ASN",
                        description = "Dados geográficos, provedor e ASN do endereço IP",
                        category = "Endereços IP",
                        urlTemplate = "https://ipinfo.io/{target}",
                        placeholderHint = "ex: 1.1.1.1"
                    ),
                    OsintToolItem(
                        id = "bgp_he_lookup",
                        title = "BGP Routing & Peering (HE)",
                        description = "Tabela de rotas e conexões BGP Hurricane Electric",
                        category = "Endereços IP",
                        urlTemplate = "https://bgp.he.net/ip/{target}",
                        placeholderHint = "ex: 8.8.8.8"
                    )
                )
            ),
            OsintCategoryDef(
                name = "DNS",
                icon = Icons.Default.Dns,
                description = "Resoluções A, AAAA, MX, TXT e mapeamento de zona",
                tools = listOf(
                    OsintToolItem(
                        id = "dnsdumpster",
                        title = "DNSDumpster Recon",
                        description = "Mapeamento gráfico de zonas e subdomínios DNS",
                        category = "DNS",
                        urlTemplate = "https://dnsdumpster.com",
                        placeholderHint = "Acesse a interface online"
                    ),
                    OsintToolItem(
                        id = "google_dns_api",
                        title = "Google Web DNS Resolver",
                        description = "Consulta direta via DNS-over-HTTPS da Google",
                        category = "DNS",
                        urlTemplate = "https://dns.google/resolve?name={target}",
                        placeholderHint = "ex: exemplo.com"
                    )
                )
            ),
            OsintCategoryDef(
                name = "Links Suspeitos",
                icon = Icons.Default.Link,
                description = "Triagem de phishing, reputação e malware",
                tools = listOf(
                    OsintToolItem(
                        id = "virustotal_search",
                        title = "VirusTotal Intelligence",
                        description = "Escaneamento de hashes, domínios e URLs por dezenas de antivírus",
                        category = "Links Suspeitos",
                        urlTemplate = "https://www.virustotal.com/gui/search/{target}",
                        placeholderHint = "ex: hash, domínio ou URL"
                    ),
                    OsintToolItem(
                        id = "urlscan_search",
                        title = "URLScan.io Scanner",
                        description = "Inspeção visual e tecnológica de comportamento de sites",
                        category = "Links Suspeitos",
                        urlTemplate = "https://urlscan.io/search/#{target}",
                        placeholderHint = "ex: domínio ou IP"
                    )
                )
            ),
            OsintCategoryDef(
                name = "Pesquisa Avançada",
                icon = Icons.Default.Code,
                description = "Google Dorks e filtros estruturados de busca",
                tools = listOf(
                    OsintToolItem(
                        id = "dork_filetype",
                        title = "Dork: Arquivos Sensíveis (PDF/DOC)",
                        description = "Filtro filetype para documentos vazados ou expostos",
                        category = "Pesquisa Avançada",
                        urlTemplate = "https://duckduckgo.com/?q=filetype%3Apdf+{target}",
                        placeholderHint = "ex: termo ou site:alvo.com"
                    ),
                    OsintToolItem(
                        id = "dork_indexing",
                        title = "Dork: Diretórios Abertos (Index of)",
                        description = "Procura por servidores com listagem de diretório aberta",
                        category = "Pesquisa Avançada",
                        urlTemplate = "https://duckduckgo.com/?q=intitle%3A%22index+of%22+{target}",
                        placeholderHint = "ex: backups, config"
                    )
                )
            ),
            OsintCategoryDef(
                name = "Metadados de Imagens",
                icon = Icons.Default.Image,
                description = "Análise EXIF e busca reversa visual",
                tools = listOf(
                    OsintToolItem(
                        id = "exif_viewer",
                        title = "Jeffrey's Image Metadata Viewer",
                        description = "Extrator público detalhado de dados EXIF, geolocalização e câmera",
                        category = "Metadados de Imagens",
                        urlTemplate = "http://exif.regex.info/exif.cgi",
                        placeholderHint = "Ferramenta externa de análise"
                    )
                )
            )
        )
    }

    val currentCategory = categories[selectedCategoryIndex]

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
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = OpenVBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Central OSINT",
                        color = OpenVTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = OpenVBlue.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "ALPHA 0.1",
                            color = OpenVBlue,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "Acesso direto a ferramentas e fontes públicas de inteligência",
                    color = OpenVTextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // Horizontal Category Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedCategoryIndex,
            containerColor = OpenVSurfaceElevated,
            contentColor = OpenVBlue,
            edgePadding = 12.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedCategoryIndex]),
                    color = OpenVBlue
                )
            }
        ) {
            categories.forEachIndexed { index, cat ->
                Tab(
                    selected = selectedCategoryIndex == index,
                    onClick = { selectedCategoryIndex = index },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = cat.icon,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (selectedCategoryIndex == index) OpenVBlue else OpenVTextSecondary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = cat.name,
                                color = if (selectedCategoryIndex == index) OpenVBlue else OpenVTextSecondary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                )
            }
        }

        // Category Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Target Input Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = OpenVSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, OpenVBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Alvo de Investigação",
                        color = OpenVTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = currentCategory.description,
                        color = OpenVTextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                    )

                    OutlinedTextField(
                        value = targetInput,
                        onValueChange = { targetInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("osint_target_input"),
                        placeholder = {
                            Text(
                                "Digite domínio, IP, hash ou termo...",
                                color = OpenVTextSecondary,
                                fontSize = 13.sp
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = OpenVTextPrimary,
                            unfocusedTextColor = OpenVTextPrimary,
                            focusedBorderColor = OpenVBlue,
                            unfocusedBorderColor = OpenVBorder,
                            cursorColor = OpenVBlue
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Ferramentas Disponíveis (${currentCategory.tools.size})",
                color = OpenVTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            currentCategory.tools.forEach { tool ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            val encodedTarget = try {
                                URLEncoder.encode(targetInput.trim(), "UTF-8")
                            } catch (_: Exception) {
                                targetInput.trim()
                            }
                            val finalUrl = if (tool.urlTemplate.contains("{target}")) {
                                if (encodedTarget.isBlank()) {
                                    tool.urlTemplate.replace("{target}", "")
                                } else {
                                    tool.urlTemplate.replace("{target}", encodedTarget)
                                }
                            } else {
                                tool.urlTemplate
                            }
                            onLaunchQueryInBrowser(finalUrl)
                        },
                    color = OpenVSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OpenVBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(OpenVSurfaceElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInBrowser,
                                contentDescription = null,
                                tint = OpenVBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tool.title,
                                color = OpenVTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = tool.description,
                                color = OpenVTextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = "Abrir consulta",
                            tint = OpenVBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Honest Disclaimer regarding Stage 4
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = OpenVSurfaceElevated),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, OpenVBorder)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = OpenVWarningOrange,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Módulo de Análise Automatizada",
                            color = OpenVTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Nesta Etapa 1, as consultas são abertas diretamente no navegador através de serviços públicos verificados. Os motores de correlação cruzada e parser local estão agendados para a Etapa 4.",
                            color = OpenVTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
