package com.example.attendance.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.attendance.location.PoiSearchManager

@Composable
fun PoiSearchBox(
    searchManager: PoiSearchManager,
    onPoiSelected: (PoiSearchManager.PoiResultInfo) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<PoiSearchManager.PoiResultInfo>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var noResults by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var showResults by remember { mutableStateOf(false) }

    Column {
        TextField(
            value = query,
            onValueChange = { value ->
                query = value
                if (value.length >= 2) {
                    isSearching = true
                    hasError = false
                    noResults = false
                    showResults = true
                    searchManager.search(
                        keyword = value,
                        onResult = { list ->
                            isSearching = false
                            results = list
                            noResults = list.isEmpty()
                        },
                        onError = {
                            isSearching = false
                            hasError = true
                        }
                    )
                } else {
                    showResults = false
                    results = emptyList()
                }
            },
            label = { Text("搜索公司或地址") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (showResults) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                when {
                    isSearching -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    hasError -> {
                        Text(
                            text = "搜索失败，请重试",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    noResults -> {
                        Text(
                            text = "未找到结果",
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    else -> {
                        LazyColumn {
                            items(results) { poi ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onPoiSelected(poi) }
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = poi.name,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    if (poi.address.isNotBlank()) {
                                        Text(
                                            text = poi.address,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
}