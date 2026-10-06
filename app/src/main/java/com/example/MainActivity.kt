package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.data.model.DailyReportWithDetails
import com.example.data.model.WorkItemWithPhotos
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ProfileViewModel
import com.example.ui.viewmodel.ReportViewModel
import com.example.ui.viewmodel.WorkItemViewModel

enum class Screen {
    HOME,
    REPORT_FORM,
    ONGOING_WORK,
    WORK_DETAIL,
    HISTORY,
    SEARCH,
    PROFILE,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private val reportViewModel: ReportViewModel by viewModels()
    private val workItemViewModel: WorkItemViewModel by viewModels()
    private val profileViewModel: ProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var currentScreen by remember { mutableStateOf(Screen.HOME) }
                    var screenStack by remember { mutableStateOf(listOf(Screen.HOME)) }
                    var selectedWorkItemId by remember { mutableLongStateOf(0L) }

                    fun navigateTo(screen: Screen) {
                        screenStack = screenStack + screen
                        currentScreen = screen
                    }

                    fun navigateBack() {
                        if (screenStack.size > 1) {
                            val newStack = screenStack.dropLast(1)
                            screenStack = newStack
                            currentScreen = newStack.last()
                        } else {
                            currentScreen = Screen.HOME
                        }
                    }

                    when (currentScreen) {
                        Screen.HOME -> {
                            HomeScreen(
                                viewModel = reportViewModel,
                                onCreateReport = { navigateTo(Screen.REPORT_FORM) },
                                onViewOngoing = { navigateTo(Screen.ONGOING_WORK) },
                                onViewHistory = { navigateTo(Screen.HISTORY) },
                                onViewSearch = { navigateTo(Screen.SEARCH) },
                                onViewProfile = { navigateTo(Screen.PROFILE) },
                                onViewSettings = { navigateTo(Screen.SETTINGS) },
                                onEditReport = { reportDetails: DailyReportWithDetails ->
                                    reportViewModel.loadReportForEdit(reportDetails)
                                    navigateTo(Screen.REPORT_FORM)
                                }
                            )
                        }

                        Screen.REPORT_FORM -> {
                            BackHandler { navigateBack() }
                            ReportFormScreen(
                                viewModel = reportViewModel,
                                onNavigateBack = { navigateBack() },
                                onNavigateToOngoing = { navigateTo(Screen.ONGOING_WORK) }
                            )
                        }

                        Screen.ONGOING_WORK -> {
                            BackHandler { navigateBack() }
                            OngoingWorkScreen(
                                viewModel = workItemViewModel,
                                onNavigateBack = { navigateBack() },
                                onContinueWork = { ongoingItem: WorkItemWithPhotos ->
                                    reportViewModel.resetForm()
                                    reportViewModel.importOngoingWork(ongoingItem)
                                    navigateTo(Screen.REPORT_FORM)
                                },
                                onViewDetail = { id: Long ->
                                    selectedWorkItemId = id
                                    navigateTo(Screen.WORK_DETAIL)
                                }
                            )
                        }

                        Screen.WORK_DETAIL -> {
                            BackHandler { navigateBack() }
                            WorkItemDetailScreen(
                                workItemId = selectedWorkItemId,
                                viewModel = workItemViewModel,
                                onNavigateBack = { navigateBack() }
                            )
                        }

                        Screen.HISTORY -> {
                            BackHandler { navigateBack() }
                            HistoryScreen(
                                viewModel = reportViewModel,
                                onNavigateBack = { navigateBack() },
                                onSelectReport = { reportDetails: DailyReportWithDetails ->
                                    reportViewModel.loadReportForEdit(reportDetails)
                                    navigateTo(Screen.REPORT_FORM)
                                }
                            )
                        }

                        Screen.SEARCH -> {
                            BackHandler { navigateBack() }
                            SearchScreen(
                                viewModel = workItemViewModel,
                                onNavigateBack = { navigateBack() },
                                onSelectWorkItem = { id: Long ->
                                    selectedWorkItemId = id
                                    navigateTo(Screen.WORK_DETAIL)
                                }
                            )
                        }

                        Screen.PROFILE -> {
                            BackHandler { navigateBack() }
                            ProfileScreen(
                                viewModel = profileViewModel,
                                onNavigateBack = { navigateBack() }
                            )
                        }

                        Screen.SETTINGS -> {
                            BackHandler { navigateBack() }
                            SettingsScreen(
                                profileViewModel = profileViewModel,
                                reportViewModel = reportViewModel,
                                onNavigateBack = { navigateBack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
