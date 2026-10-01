package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.JarvisTab
import com.example.ui.theme.JarvisCardBorder
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisSurfaceDark
import com.example.ui.theme.JarvisTextSecondary

data class NavItem(
    val tab: JarvisTab,
    val label: String,
    val icon: ImageVector,
    val tag: String
)

@Composable
fun JarvisBottomNav(
    selectedTab: JarvisTab,
    onTabSelected: (JarvisTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        NavItem(JarvisTab.CORE, "CORE", Icons.Default.Psychology, "nav_core"),
        NavItem(JarvisTab.AGENDA, "AGENDA", Icons.Default.CalendarToday, "nav_agenda"),
        NavItem(JarvisTab.STUDIO, "STUDIO", Icons.Default.AutoAwesome, "nav_studio"),
        NavItem(JarvisTab.CHATBOT, "CHAT", Icons.AutoMirrored.Filled.Chat, "nav_chatbot"),
        NavItem(JarvisTab.SYSTEM, "SYSTEM", Icons.Default.Memory, "nav_system"),
        NavItem(JarvisTab.SYNC, "DEVICES", Icons.Default.Devices, "nav_sync")
    )

    NavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .border(width = 0.5.dp, color = JarvisCardBorder)
            .background(JarvisSurfaceDark)
            .navigationBarsPadding(),
        containerColor = JarvisSurfaceDark,
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            val isSelected = selectedTab == item.tab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(item.tab) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        modifier = Modifier.size(20.dp)
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 10.sp,
                        letterSpacing = 0.3.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = JarvisCyan,
                    selectedTextColor = JarvisCyan,
                    unselectedIconColor = JarvisTextSecondary,
                    unselectedTextColor = JarvisTextSecondary,
                    indicatorColor = Color(0xFF14243B)
                ),
                modifier = Modifier.testTag(item.tag)
            )
        }
    }
}
