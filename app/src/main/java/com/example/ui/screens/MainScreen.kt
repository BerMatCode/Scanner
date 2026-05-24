package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ScanViewModel
import com.example.ui.theme.DarkPurple
import com.example.ui.theme.ElectricBlue

@Composable
fun MainScreen(
    viewModel: ScanViewModel,
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableStateOf(MainTab.SCANNER) }
    val isDark = isSystemInDarkTheme()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            // Elegant Glassmorphic Top Brand Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.05f))
                    .padding(vertical = 12.dp, horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Small Techy Hex/Scanner Logo
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(DarkPurple, ElectricBlue)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Brand Text using rich style
                    Row {
                        Text(
                            text = "BerMat",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isDark) Color.White else DarkPurple
                        )
                        Text(
                            text = "Scanner",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ElectricBlue
                        )
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("app_navigation_bar"),
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                tonalElevation = 8.dp
            ) {
                // Tab 1: Escanear
                NavigationBarItem(
                    selected = currentTab == MainTab.SCANNER,
                    onClick = { currentTab = MainTab.SCANNER },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == MainTab.SCANNER) Icons.Filled.QrCodeScanner else Icons.Outlined.QrCodeScanner,
                            contentDescription = "Escanear"
                        )
                    },
                    label = { 
                        Text(
                            "Escanear", 
                            fontWeight = if (currentTab == MainTab.SCANNER) FontWeight.Bold else FontWeight.Normal
                        ) 
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DarkPurple,
                        selectedTextColor = DarkPurple,
                        indicatorColor = DarkPurple.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier.testTag("nav_tab_scanner")
                )

                // Tab 2: Historial
                NavigationBarItem(
                    selected = currentTab == MainTab.HISTORY,
                    onClick = { currentTab = MainTab.HISTORY },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == MainTab.HISTORY) Icons.Filled.History else Icons.Outlined.History,
                            contentDescription = "Historial"
                        )
                    },
                    label = { 
                        Text(
                            "Historial", 
                            fontWeight = if (currentTab == MainTab.HISTORY) FontWeight.Bold else FontWeight.Normal
                        ) 
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ElectricBlue,
                        selectedTextColor = ElectricBlue,
                        indicatorColor = ElectricBlue.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier.testTag("nav_tab_history")
                )
            }
        }
    ) { innerPadding ->
        // Animated transition between Tabs
        AnimatedContent(
            targetState = currentTab,
            transitionSpec = {
                if (targetState == MainTab.HISTORY) {
                    (slideInHorizontally(animationSpec = tween(300)) { it } + fadeIn(animationSpec = tween(300)))
                        .togetherWith(slideOutHorizontally(animationSpec = tween(300)) { -it } + fadeOut(animationSpec = tween(300)))
                } else {
                    (slideInHorizontally(animationSpec = tween(300)) { -it } + fadeIn(animationSpec = tween(300)))
                        .togetherWith(slideOutHorizontally(animationSpec = tween(300)) { it } + fadeOut(animationSpec = tween(300)))
                }
            },
            label = "tab_fade",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { tabSelection ->
            when (tabSelection) {
                MainTab.SCANNER -> {
                    ScannerScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                }
                MainTab.HISTORY -> {
                    HistoryScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

enum class MainTab {
    SCANNER,
    HISTORY
}
