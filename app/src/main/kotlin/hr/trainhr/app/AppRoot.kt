package hr.trainhr.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

private enum class Tab(val label: String, val icon: String) {
    HOME("Početna", "⌂"),
    PLAN("Plan", "▦"),
    SESSIONS("Treninzi", "≡"),
    ANALYSIS("Analiza", "▲"),
}

@Composable
fun AppRoot(vm: AppViewModel) {
    val d by vm.data.collectAsState()
    val active by vm.active.collectAsState()
    val message by vm.message.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    var settings by rememberSaveable { mutableStateOf(false) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var openSession by rememberSaveable { mutableStateOf<Long?>(null) }

    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); vm.consumeMessage() }
    }

    val needsSetup = d.selected.isEmpty() || d.startDay == null
    val workout = active
    val showTabs = !needsSetup && workout == null && !editing && !settings

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (showTabs) {
                NavigationBar {
                    Tab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = tab == t,
                            onClick = { tab = t; openSession = null },
                            icon = { Text(t.icon) },
                            label = { Text(t.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            val openSettings = { settings = true }
            when {
                workout != null -> WorkoutScreen(vm, d, workout)
                needsSetup || editing -> {
                    if (editing) BackHandler { editing = false }
                    SetupScreen(
                        d,
                        onSave = { vm.saveSetup(it); editing = false; settings = false; tab = Tab.HOME },
                        onCancel = if (needsSetup) null else ({ editing = false }),
                    )
                }
                settings -> SettingsScreen(vm, onBack = { settings = false }, onEditPlan = { editing = true })
                else -> when (tab) {
                    Tab.HOME -> HomeScreen(vm, d, openSettings,
                        onOpenSession = { openSession = it; tab = Tab.SESSIONS },
                        onNewProgram = { editing = true })
                    Tab.PLAN -> PlanScreen(vm, d, openSettings)
                    Tab.SESSIONS -> SessionsScreen(vm, d, openSession, { openSession = it }, openSettings)
                    Tab.ANALYSIS -> AnalysisScreen(d, { openSession = it; tab = Tab.SESSIONS }, openSettings)
                }
            }
        }
    }
}
