package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.JarvisProtocol
import com.example.data.local.ScheduleItem
import com.example.ui.JarvisViewModel
import com.example.ui.theme.JarvisBackgroundDark
import com.example.ui.theme.JarvisCardBorder
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisRed
import com.example.ui.theme.JarvisSurfaceDark
import com.example.ui.theme.JarvisSurfaceVariantDark
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary
import com.example.ui.theme.JarvisTextTertiary

enum class AgendaFilter {
    ALL,
    TODAY,
    HIGH_PRIORITY,
    COMPLETED
}

@Composable
fun AgendaScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val items by viewModel.scheduleItems.collectAsState()
    val protocols by viewModel.protocols.collectAsState()
    var selectedFilter by remember { mutableStateOf(AgendaFilter.ALL) }
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredItems = remember(items, selectedFilter) {
        when (selectedFilter) {
            AgendaFilter.ALL -> items
            AgendaFilter.TODAY -> items.filter { it.date.equals("Today", ignoreCase = true) }
            AgendaFilter.HIGH_PRIORITY -> items.filter { it.priority == "HIGH" }
            AgendaFilter.COMPLETED -> items.filter { it.isCompleted }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisBackgroundDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // AI Schedule Optimizer Header Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_schedule_optimizer_card"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1B2A)),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(JarvisCyan.copy(alpha = 0.5f))
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = JarvisCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "J.A.R.V.I.S. AGENDA OPTIMIZER",
                                style = MaterialTheme.typography.labelSmall,
                                color = JarvisCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${items.count { !it.isCompleted }} pending deliverables in queue.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = JarvisTextSecondary
                        )
                    }

                    Button(
                        onClick = { viewModel.optimizeScheduleWithAi() },
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("optimize_agenda_button")
                    ) {
                        Text(
                            text = "OPTIMIZE",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Operational Protocols Section
            Text(
                text = "// ACTIVE PROTOCOLS ARRAY",
                style = MaterialTheme.typography.labelSmall,
                color = JarvisTextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                protocols.forEach { protocol ->
                    ProtocolPillCard(
                        protocol = protocol,
                        onTrigger = { viewModel.executeProtocol(protocol) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Filters & Clear Completed Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "// DAILY SCHEDULE TIMELINE",
                    style = MaterialTheme.typography.labelSmall,
                    color = JarvisTextSecondary
                )

                if (items.any { it.isCompleted }) {
                    Text(
                        text = "PURGE COMPLETED",
                        style = MaterialTheme.typography.labelSmall,
                        color = JarvisGold,
                        modifier = Modifier
                            .clickable { viewModel.clearCompletedSchedules() }
                            .testTag("clear_completed_schedules_button")
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AgendaFilter.values().forEach { filter ->
                    val isSelected = selectedFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = filter },
                        label = {
                            Text(
                                text = filter.name.replace("_", " "),
                                fontSize = 11.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = JarvisCyan,
                            selectedLabelColor = Color.Black,
                            containerColor = JarvisSurfaceDark,
                            labelColor = JarvisTextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) JarvisCyan else JarvisCardBorder
                        ),
                        modifier = Modifier.testTag("filter_${filter.name}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Schedule Items List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (filteredItems.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "All systems clear. No missions scheduled.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = JarvisTextTertiary
                            )
                        }
                    }
                } else {
                    items(filteredItems, key = { it.id }) { item ->
                        ScheduleItemCard(
                            item = item,
                            onToggleComplete = { viewModel.toggleScheduleCompleted(item) },
                            onDelete = { viewModel.deleteScheduleItem(item) }
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }

        // Floating Action Button to Add Task
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_schedule_fab"),
            containerColor = JarvisCyan,
            contentColor = Color.Black,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Schedule Item")
        }

        if (showAddDialog) {
            AddScheduleDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { title, time, date, priority, category ->
                    viewModel.addScheduleItem(title, time, date, priority, category)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun ProtocolPillCard(
    protocol: JarvisProtocol,
    onTrigger: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(180.dp)
            .clickable(onClick = onTrigger)
            .testTag("protocol_card_${protocol.id}"),
        colors = CardDefaults.cardColors(containerColor = JarvisSurfaceDark),
        shape = RoundedCornerShape(10.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (protocol.isActive) JarvisGreen else JarvisCardBorder
            )
        )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = protocol.title,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (protocol.isActive) JarvisGreen else JarvisCyan,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (protocol.isActive) JarvisGreen else JarvisTextTertiary)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = protocol.description,
                style = MaterialTheme.typography.bodySmall,
                color = JarvisTextSecondary,
                maxLines = 2,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "TRIGGER: \"${protocol.triggerKeyword}\"",
                style = MaterialTheme.typography.labelSmall,
                color = JarvisGold,
                fontSize = 9.sp
            )
        }
    }
}

@Composable
fun ScheduleItemCard(
    item: ScheduleItem,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("schedule_item_${item.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isCompleted) Color(0xFF0E1420) else JarvisSurfaceDark
        ),
        shape = RoundedCornerShape(10.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (item.isCompleted) JarvisCardBorder.copy(alpha = 0.5f) else JarvisCardBorder
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggleComplete,
                modifier = Modifier
                    .size(28.dp)
                    .testTag("toggle_complete_${item.id}")
            ) {
                Icon(
                    imageVector = if (item.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Toggle Complete",
                    tint = if (item.isCompleted) JarvisGreen else JarvisCyan
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (item.isCompleted) JarvisTextSecondary else JarvisTextPrimary,
                        textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (item.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = JarvisTextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Time pill
                    Text(
                        text = "${item.date} • ${item.time}",
                        style = MaterialTheme.typography.labelSmall,
                        color = JarvisCyan
                    )

                    // Priority indicator
                    val priorityColor = when (item.priority) {
                        "HIGH" -> JarvisRed
                        "MEDIUM" -> JarvisGold
                        else -> JarvisGreen
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(priorityColor)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = item.priority,
                            style = MaterialTheme.typography.labelSmall,
                            color = priorityColor,
                            fontSize = 10.sp
                        )
                    }

                    // Category pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF142236))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = JarvisTextSecondary,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("delete_item_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete Item",
                    tint = JarvisTextTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun AddScheduleDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, time: String, date: String, priority: String, category: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("09:00 AM") }
    var date by remember { mutableStateOf("Today") }
    var priority by remember { mutableStateOf("HIGH") }
    var category by remember { mutableStateOf("WORK") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "SCHEDULE DIRECTIVE",
                style = MaterialTheme.typography.titleMedium,
                color = JarvisCyan
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Directive Title") },
                    placeholder = { Text("e.g. Mark 85 Flight Test") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_directive_title"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisCyan,
                        unfocusedBorderColor = JarvisCardBorder,
                        focusedTextColor = JarvisTextPrimary,
                        unfocusedTextColor = JarvisTextPrimary
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it },
                        label = { Text("Time") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_directive_time"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisCyan,
                            unfocusedBorderColor = JarvisCardBorder,
                            focusedTextColor = JarvisTextPrimary,
                            unfocusedTextColor = JarvisTextPrimary
                        )
                    )

                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Date") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_directive_date"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisCyan,
                            unfocusedBorderColor = JarvisCardBorder,
                            focusedTextColor = JarvisTextPrimary,
                            unfocusedTextColor = JarvisTextPrimary
                        )
                    )
                }

                // Priority Selector
                Column {
                    Text("Priority Level", style = MaterialTheme.typography.labelSmall, color = JarvisTextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("HIGH", "MEDIUM", "LOW").forEach { p ->
                            FilterChip(
                                selected = priority == p,
                                onClick = { priority = p },
                                label = { Text(p, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (p == "HIGH") JarvisRed else JarvisCyan,
                                    selectedLabelColor = Color.Black
                                ),
                                modifier = Modifier.testTag("priority_chip_$p")
                            )
                        }
                    }
                }

                // Category Selector
                Column {
                    Text("Category", style = MaterialTheme.typography.labelSmall, color = JarvisTextSecondary)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("WORK", "PROTOCOL", "SYSTEM", "SECURITY", "PERSONAL").forEach { c ->
                            FilterChip(
                                selected = category == c,
                                onClick = { category = c },
                                label = { Text(c, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = JarvisCyan,
                                    selectedLabelColor = Color.Black
                                ),
                                modifier = Modifier.testTag("category_chip_$c")
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title, time, date, priority, category)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan),
                modifier = Modifier.testTag("confirm_add_directive_button")
            ) {
                Text("RECORD DIRECTIVE", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("DISMISS", color = JarvisTextSecondary)
            }
        },
        containerColor = JarvisSurfaceDark
    )
}
