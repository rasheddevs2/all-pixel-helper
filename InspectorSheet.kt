package com.pixelhelper.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelhelper.app.data.model.TrackingType
import com.pixelhelper.app.ui.BrowserViewModel

@Composable
fun InspectorSheet(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Meta Pixel", "GA4 & GTM", "Diagnostics")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.65f)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tracking Inspector",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { viewModel.clearEvents() }) {
                Icon(Icons.Default.Delete, contentDescription = "Clear logs")
            }
        }

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 16.dp
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        val count = when (index) {
                            0 -> uiState.events.count { it.type == TrackingType.META_PIXEL }
                            1 -> uiState.events.count { it.type != TrackingType.META_PIXEL }
                            2 -> uiState.diagnostics.size
                            else -> 0
                        }
                        Text("$title ($count)")
                    }
                )
            }
        }

        HorizontalDivider(color = DividerDefaults.color)

        when (selectedTab) {
            0 -> {
                val pixelEvents = uiState.events.filter { it.type == TrackingType.META_PIXEL }
                if (pixelEvents.isEmpty()) {
                    EmptyStateNotice("No Meta Pixel hits detected yet.")
                } else {
                    LazyColumn {
                        items(pixelEvents, key = { it.id }) { event ->
                            EventCard(event = event)
                        }
                    }
                }
            }
            1 -> {
                val nonMetaEvents = uiState.events.filter { it.type != TrackingType.META_PIXEL }
                if (nonMetaEvents.isEmpty()) {
                    EmptyStateNotice("No GA4 or GTM hits detected.")
                } else {
                    LazyColumn {
                        items(nonMetaEvents, key = { it.id }) { event ->
                            EventCard(event = event)
                        }
                    }
                }
            }
            2 -> {
                if (uiState.diagnostics.isEmpty()) {
                    EmptyStateNotice("All inspected parameters look clean! No issues found.")
                } else {
                    LazyColumn(modifier = Modifier.padding(8.dp)) {
                        items(uiState.diagnostics) { issue ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = issue.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = issue.description,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateNotice(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            fontSize = 13.sp,
            color = Color.Gray
        )
    }
}
