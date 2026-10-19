package hr.trainhr.app.ui

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
import hr.trainhr.app.data.AppData
import hr.trainhr.core.model.PlanSettings
import hr.trainhr.core.model.UserProfile

private enum class Tab(val label: String, val icon: String) {
    TODAY("Danas", "🏋"),
    PLAN("Plan", "📋"),
    PROGRESS("Napredak", "📈"),
    SETTINGS("Postavke", "⚙"),
}

/** Podekrani unutar glavnog dijela aplikacije. */
private enum class Sub { NONE, PLAN_SETTINGS, RETEST, PROFILE, EQUIPMENT, READINESS }

@Composable
fun AppRoot(vm: AppViewModel) {
    val d by vm.data.collectAsState()
    val active by vm.active.collectAsState()
    val message by vm.message.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var tab by rememberSaveable { mutableStateOf(Tab.TODAY) }
    var sub by rememberSaveable { mutableStateOf(Sub.NONE) }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            vm.consumeMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (showTabs(vm, d) && active == null) {
                BottomTabs(tab, sub) { tab = it; sub = Sub.NONE }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            Content(vm, d, active, tab, sub) { sub = it }
        }
    }
}

private fun showTabs(vm: AppViewModel, d: AppData) =
    d.profile != null && d.readinessDone && d.equipmentDone && vm.hasCompletedTest(d) && d.plan != null

@Composable
private fun BottomTabs(tab: Tab, sub: Sub, onSelect: (Tab) -> Unit) {
    NavigationBar {
        Tab.entries.forEach { t ->
            NavigationBarItem(
                selected = tab == t && sub == Sub.NONE,
                onClick = { onSelect(t) },
                icon = { Text(t.icon) },
                label = { Text(t.label) },
            )
        }
    }
}

@Composable
private fun Content(
    vm: AppViewModel,
    d: AppData,
    active: ActiveWorkout?,
    tab: Tab,
    sub: Sub,
    setSub: (Sub) -> Unit,
) {
    val profile = d.profile
    when {
        profile == null -> ProfileScreen(UserProfile(), "Dalje") { vm.saveProfile(it) }
        !d.readinessDone -> ReadinessScreen(profile.readinessAnswers) { a, ack -> vm.saveReadiness(a, ack) }
        !d.equipmentDone -> EquipmentScreen(profile.equipment, "Dalje") { vm.saveEquipment(it) }
        !vm.hasCompletedTest(d) -> InitialTestScreen(vm, d, retest = false, onClose = null)
        d.plan == null -> PlanSetupScreen(PlanSettings(), onCreate = { vm.createPlan(it) }, onCancel = null)
        active != null -> WorkoutScreen(vm, d, active)
        else -> MainContent(vm, d, tab, sub, setSub)
    }
}

@Composable
private fun MainContent(vm: AppViewModel, d: AppData, tab: Tab, sub: Sub, setSub: (Sub) -> Unit) {
    val profile = d.profile ?: return
    val close = { setSub(Sub.NONE) }
    if (sub != Sub.NONE) BackHandler { close() }

    when (sub) {
        Sub.PLAN_SETTINGS -> PlanSetupScreen(
            d.plan?.settings ?: PlanSettings(),
            onCreate = { vm.createPlan(it); close() },
            onCancel = close,
        )
        Sub.RETEST -> InitialTestScreen(vm, d, retest = true, onClose = close)
        Sub.PROFILE -> ProfileScreen(profile, "Spremi") { vm.saveProfile(it); close() }
        Sub.EQUIPMENT -> EquipmentScreen(profile.equipment, "Spremi") { vm.saveEquipment(it); close() }
        Sub.READINESS -> ReadinessScreen(profile.readinessAnswers) { a, ack -> vm.saveReadiness(a, ack); close() }
        Sub.NONE -> when (tab) {
            Tab.TODAY -> TodayScreen(vm, d)
            Tab.PLAN -> PlanScreen(vm, d, onChangeSettings = { setSub(Sub.PLAN_SETTINGS) })
            Tab.PROGRESS -> ProgressScreen(vm, d, onRetest = { vm.startRetest(); setSub(Sub.RETEST) })
            Tab.SETTINGS -> SettingsScreen(
                vm, d,
                onEditProfile = { setSub(Sub.PROFILE) },
                onEditEquipment = { setSub(Sub.EQUIPMENT) },
                onEditReadiness = { setSub(Sub.READINESS) },
            )
        }
    }
}
