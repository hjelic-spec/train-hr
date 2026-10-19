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
    PLAN("Plan", "🏋"),
    PROGRESS("Napredak", "📈"),
    SETTINGS("Postavke", "⚙"),
}

@Composable
fun AppRoot(vm: AppViewModel) {
    val d by vm.data.collectAsState()
    val active by vm.active.collectAsState()
    val message by vm.message.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var tab by rememberSaveable { mutableStateOf(Tab.PLAN) }
    var editingPlan by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            vm.consumeMessage()
        }
    }

    val needsSetup = d.plan.isEmpty()
    val workout = active

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (!needsSetup && workout == null && !editingPlan) {
                NavigationBar {
                    Tab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = tab == t,
                            onClick = { tab = t },
                            icon = { Text(t.icon) },
                            label = { Text(t.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when {
                needsSetup -> SetupScreen(d, onSave = { days, range, sel, custom -> vm.saveSetup(days, range, sel, custom) }, onCancel = null)
                workout != null -> WorkoutScreen(vm, d, workout)
                editingPlan -> {
                    BackHandler { editingPlan = false }
                    SetupScreen(
                        d,
                        onSave = { days, range, sel, custom ->
                            vm.saveSetup(days, range, sel, custom)
                            editingPlan = false
                            tab = Tab.PLAN
                        },
                        onCancel = { editingPlan = false },
                    )
                }
                else -> when (tab) {
                    Tab.PLAN -> PlanScreen(vm, d)
                    Tab.PROGRESS -> ProgressScreen(vm, d)
                    Tab.SETTINGS -> SettingsScreen(vm, onEditPlan = { editingPlan = true })
                }
            }
        }
    }
}
