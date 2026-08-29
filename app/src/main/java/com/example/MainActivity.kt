package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.screens.AlertsWatchlistScreen
import com.example.ui.screens.NoticeDetailDialog
import com.example.ui.screens.ObjectionVoiceScreen
import com.example.ui.screens.RadarTimelineScreen
import com.example.ui.screens.RwaDashboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.TarewViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: TarewViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                TarewApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun TarewApp(viewModel: TarewViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val filteredNotices by viewModel.filteredNotices.collectAsState()
    val openObjectionNotices by viewModel.openObjectionNotices.collectAsState()
    val subscriptions by viewModel.subscriptions.collectAsState()
    val savedObjections by viewModel.savedObjections.collectAsState()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tarew_main_scaffold"),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("tarew_bottom_nav")
            ) {
                // Tab 0: Radar & Timeline
                NavigationBarItem(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Radar,
                            contentDescription = "Radar Timeline"
                        )
                    },
                    label = { Text("Radar Feed", fontWeight = if (uiState.selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_radar")
                )

                // Tab 1: Objections & Voice Hub
                NavigationBarItem(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (openObjectionNotices.isNotEmpty()) {
                                    Badge(
                                        containerColor = Color(0xFFE11D48),
                                        contentColor = Color.White
                                    ) {
                                        Text("${openObjectionNotices.size}")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Gavel,
                                contentDescription = "Objection Voice"
                            )
                        }
                    },
                    label = { Text("Objections", fontWeight = if (uiState.selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFE11D48),
                        selectedTextColor = Color(0xFFE11D48),
                        indicatorColor = Color(0xFFFFE4E6)
                    ),
                    modifier = Modifier.testTag("nav_tab_objections")
                )

                // Tab 2: Alerts & Scrapers
                NavigationBarItem(
                    selected = uiState.selectedTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Alerts & Scraper"
                        )
                    },
                    label = { Text("24h Alerts", fontWeight = if (uiState.selectedTab == 2) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_alerts")
                )

                // Tab 3: RWA Society Intelligence
                NavigationBarItem(
                    selected = uiState.selectedTab == 3,
                    onClick = { viewModel.selectTab(3) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Apartment,
                            contentDescription = "RWA Society"
                        )
                    },
                    label = { Text("RWA Briefs", fontWeight = if (uiState.selectedTab == 3) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF7E22CE),
                        selectedTextColor = Color(0xFF7E22CE),
                        indicatorColor = Color(0xFFF3E8FF)
                    ),
                    modifier = Modifier.testTag("nav_tab_rwa")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.selectedTab) {
                0 -> RadarTimelineScreen(
                    uiState = uiState,
                    notices = filteredNotices,
                    popularAreas = viewModel.popularAreas,
                    onPincodeSelect = { area -> viewModel.selectPincode(area) },
                    onCustomPincodeSet = { pin, loc, city -> viewModel.setCustomPincode(pin, loc, city) },
                    onSearchChange = { query -> viewModel.setSearchQuery(query) },
                    onCategoryFilterChange = { cat -> viewModel.setCategoryFilter(cat) },
                    onToggleObjections = { viewModel.toggleOnlyObjections() },
                    onToggleRadarView = { viewModel.toggleRadarView() },
                    onNoticeClick = { notice -> viewModel.openNoticeDetail(notice) },
                    onVoteConcern = { id -> viewModel.voteConcern(id) },
                    onDraftObjection = { notice ->
                        viewModel.openNoticeDetail(null)
                        viewModel.selectTab(1)
                    },
                    onTriggerScrape = { viewModel.triggerScrapeSimulation() },
                    onOpenPincodePicker = { show -> viewModel.setShowPincodePicker(show) }
                )

                1 -> ObjectionVoiceScreen(
                    openNotices = openObjectionNotices,
                    savedObjections = savedObjections,
                    isGeneratingAi = uiState.isGeneratingAi,
                    generatedLetter = uiState.generatedLetter,
                    onGenerateLetter = { notice, name, society, grounds, notes ->
                        viewModel.generateObjectionLetter(notice, name, society, grounds, notes)
                    },
                    onSaveLetter = { notice, name, society, grounds, notes, body ->
                        viewModel.saveCurrentObjection(notice, name, society, grounds, notes, body)
                    },
                    onVoteConcern = { id -> viewModel.voteConcern(id) }
                )

                2 -> AlertsWatchlistScreen(
                    subscriptions = subscriptions,
                    isScraping = uiState.isScraping,
                    onTriggerScrape = { viewModel.triggerScrapeSimulation() },
                    onAddSubscription = { pin, loc, rad, email, wa, freq ->
                        viewModel.addSubscription(pin, loc, rad, email, wa, freq)
                    },
                    onDeleteSubscription = { id -> viewModel.deleteSubscription(id) }
                )

                3 -> RwaDashboardScreen(
                    profile = uiState.rwaProfile,
                    report = uiState.rwaReport,
                    isGeneratingReport = uiState.isGeneratingRwa,
                    onUpdateProfile = { prof -> viewModel.updateRwaProfile(prof) },
                    onGenerateReport = { viewModel.generateRwaReport() }
                )
            }

            // Notice Detail Dialog Overlay
            uiState.activeNoticeDetail?.let { notice ->
                NoticeDetailDialog(
                    notice = notice,
                    onDismiss = { viewModel.openNoticeDetail(null) },
                    onVoteConcern = { viewModel.voteConcern(notice.id) },
                    onDraftObjection = {
                        viewModel.openNoticeDetail(null)
                        viewModel.selectTab(1)
                    },
                    onSimplifyWithAi = { viewModel.simplifyNoticeWithAi(notice) },
                    aiSimplifiedText = uiState.simplifiedNoticeMap[notice.id],
                    isSimplifying = uiState.isAiSimplifyingNotice
                )
            }
        }
    }
}

