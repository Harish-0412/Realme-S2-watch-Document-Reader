package com.watchdoc.prototype

import android.Manifest
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.content.ContextCompat
import com.watchdoc.prototype.data.DataStorePrototypeRepository
import com.watchdoc.prototype.notification.*
import com.watchdoc.prototype.ui.setup.SetupScreen
import com.watchdoc.prototype.ui.lab.LabScreen
import com.watchdoc.prototype.ui.reader.ReaderScreen
import com.watchdoc.prototype.ui.results.ResultsScreen

class MainActivity : ComponentActivity() {
    private lateinit var vm: PrototypeViewModel
    private var navigationRequest by mutableStateOf("Setup" to 0L)
    // Intentionally ephemeral: recreation cancels arming, never restores/resends it.
    private var lockedTestArmed by mutableStateOf(false)
    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF && lockedTestArmed) {
                lockedTestArmed = false
                vm.sendLockedSetup()
            }
        }
    }
    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        vm.refreshDiagnostics()
        if (!granted) vm.notice("Notification permission was not granted. Open app notification settings if Android no longer offers a prompt.")
    }
    private val createReport = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> vm.export(uri) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val context = applicationContext
        val checker = NotificationStatusChecker(context).also { it.ensureChannel() }
        val repository = DataStorePrototypeRepository(context)
        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(PrototypeViewModel::class.java))
                @Suppress("UNCHECKED_CAST")
                return PrototypeViewModel(repository, WatchNotificationPublisher(context, checker), checker, contentResolver) as T
            }
        }
        vm = ViewModelProvider(this, factory)[PrototypeViewModel::class.java]
        ContextCompat.registerReceiver(this, screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF), ContextCompat.RECEIVER_NOT_EXPORTED)
        if (savedInstanceState == null) navigationRequest = (intent.getStringExtra("screen") ?: "Setup") to System.nanoTime()
        setContent {
            val colors = if (isSystemInDarkTheme()) darkColorScheme(primary = Color(0xFF79D7C8)) else lightColorScheme(primary = Color(0xFF006B60), secondaryContainer = Color(0xFFE1F0EC))
            MaterialTheme(colorScheme = colors) {
                val ui by vm.ui.collectAsStateWithLifecycle()
                var screen by rememberSaveable { mutableStateOf("Setup") }
                LaunchedEffect(navigationRequest) {
                    if (navigationRequest.second != 0L) screen = navigationRequest.first
                }
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            listOf("Setup", "Lab", "Reader", "Results").forEachIndexed { i, tab ->
                                NavigationBarItem(selected = screen == tab, onClick = { screen = tab },
                                    icon = { Text("0${i + 1}", style = MaterialTheme.typography.labelLarge) }, label = { Text(tab) })
                            }
                        }
                    }
                ) { padding ->
                    Column(Modifier.fillMaxSize().padding(padding).imePadding()) {
                        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                                Text("WatchDoc Prototype", style = MaterialTheme.typography.titleLarge)
                                Text("Phone notification experiments · S2 delivery unverified", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        ui.message?.let { message ->
                            Surface(color = MaterialTheme.colorScheme.tertiaryContainer) {
                                Row(Modifier.fillMaxWidth().padding(start = 16.dp)) {
                                    Text(message, Modifier.weight(1f).padding(vertical = 12.dp), style = MaterialTheme.typography.bodySmall)
                                    TextButton({ vm.notice(null) }) { Text("Dismiss") }
                                }
                            }
                        }
                        val state = ui.state
                        if (state == null) {
                            Column(Modifier.padding(24.dp)) {
                                if (ui.loadError == null) { CircularProgressIndicator(); Text("Loading local state…") }
                                else { Text(ui.loadError!!); Button(vm::retryLoad) { Text("Retry loading") } }
                            }
                        } else when (screen) {
                            "Setup" -> SetupScreen(ui.diagnostics,
                                requestPermission = { if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS) },
                                openAppSettings = { openSettings(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)) },
                                openChannelSettings = { openSettings(Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName).putExtra(Settings.EXTRA_CHANNEL_ID, NotificationStatusChecker.CHANNEL_ID)) },
                                openAccessSettings = { openSettings(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                                refresh = vm::refreshDiagnostics, send = vm::sendSetup,
                                lockedTestArmed = lockedTestArmed, toggleLockedTest = { lockedTestArmed = !lockedTestArmed })
                            "Lab" -> LabScreen(state, vm)
                            "Reader" -> ReaderScreen(state, ui.sections, vm)
                            "Results" -> ResultsScreen(state, vm) { createReport.launch("watchdoc-report-${System.currentTimeMillis()}.json") }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        navigationRequest = (intent.getStringExtra("screen") ?: "Setup") to System.nanoTime()
    }
    override fun onResume() {
        super.onResume()
        if (::vm.isInitialized) vm.refreshDiagnostics()
    }
    override fun onDestroy() {
        lockedTestArmed = false
        unregisterReceiver(screenOffReceiver)
        super.onDestroy()
    }
    private fun openSettings(intent: Intent) {
        try { startActivity(intent) }
        catch (e: Exception) { vm.notice("Could not open settings: ${e.message}. Open the matching page manually in Android Settings.") }
    }
}
