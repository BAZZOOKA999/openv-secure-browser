package com.example.ui.search

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.data.local.HistoryEntity
import com.example.data.model.SearchCategory
import com.example.data.model.SearchEngine
import com.example.ui.theme.OpenVBackground
import com.example.ui.theme.OpenVBlue
import com.example.ui.theme.OpenVBorder
import com.example.ui.theme.OpenVShieldGreen
import com.example.ui.theme.OpenVSurface
import com.example.ui.theme.OpenVSurfaceElevated
import com.example.ui.theme.OpenVTextPrimary
import com.example.ui.theme.OpenVTextSecondary

@Composable
fun SearchScreen(
    searchEngine: SearchEngine,
    history: List<HistoryEntity>,
    onSelectEngine: (SearchEngine) -> Unit,
    onExecuteSearch: (String, SearchCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(SearchCategory.WEB) }
    val focusManager = LocalFocusManager.current

    val categoryIcons = mapOf(
        SearchCategory.WEB to Icons.Default.Language,
        SearchCategory.IMAGES to Icons.Default.Image,
        SearchCategory.VIDEOS to Icons.Default.Videocam,
        SearchCategory.NEWS to Icons.Default.Article,
        SearchCategory.DOCUMENTS to Icons.Default.Description,
        SearchCategory.CODE to Icons.Default.Code,
        SearchCategory.OSINT to Icons.Default.TravelExplore
    )

    val filteredHistory = remember(query, history) {
        if (query.isBlank()) {
            history.take(8)
        } else {
            history.filter {
                it.title.contains(query, ignoreCase = true) || it.url.contains(query, ignoreCase = true)
            }.take(8)
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
                Text(
                    text = "Pesquisa Global",
                    color = OpenVTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Mecanismo ativo: ${searchEngine.displayName} • Categoria: ${selectedCategory.labelPt}",
                    color = OpenVTextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Search Box
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_tab_input"),
                    placeholder = {
                        Text("Pesquisar ${selectedCategory.labelPt.lowercase()}...", color = OpenVTextSecondary, fontSize = 14.sp)
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = OpenVBlue)
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Limpar", tint = OpenVTextSecondary)
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (query.isNotBlank()) {
                                onExecuteSearch(query, selectedCategory)
                                focusManager.clearFocus()
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = OpenVTextPrimary,
                        unfocusedTextColor = OpenVTextPrimary,
                        focusedBorderColor = OpenVBlue,
                        unfocusedBorderColor = OpenVBorder,
                        cursorColor = OpenVBlue
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category Chips Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(SearchCategory.values().toList()) { cat ->
                        val isSelected = cat == selectedCategory
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.labelPt, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            leadingIcon = {
                                categoryIcons[cat]?.let { icon ->
                                    Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OpenVBlue,
                                selectedLabelColor = OpenVBackground,
                                selectedLeadingIconColor = OpenVBackground,
                                containerColor = OpenVSurfaceElevated,
                                labelColor = OpenVTextSecondary,
                                iconColor = OpenVTextSecondary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Engine Switcher Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(SearchEngine.values().toList()) { engine ->
                        val isSelected = engine == searchEngine
                        Surface(
                            color = if (isSelected) OpenVShieldGreen else OpenVSurfaceElevated,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSelectEngine(engine) }
                        ) {
                            Text(
                                text = engine.displayName,
                                color = if (isSelected) OpenVBackground else OpenVTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Suggestions / Recent History
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (query.isBlank()) "Histórico Recente" else "Resultados Correspondentes",
                    color = OpenVTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${filteredHistory.size} itens",
                    color = OpenVTextSecondary,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredHistory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (query.isBlank()) "Nenhum histórico recente" else "Nenhum histórico correspondente à busca",
                        color = OpenVTextSecondary,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredHistory, key = { it.id }) { item ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onExecuteSearch(item.url, SearchCategory.WEB) },
                            color = OpenVSurface,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, OpenVBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = OpenVTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        color = OpenVTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.url,
                                        color = OpenVTextSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
