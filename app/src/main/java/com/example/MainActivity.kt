package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import com.example.ui.KasirViewModel
import com.example.ui.MainScreen
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.GoogleAuthHelper
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.KasirProTheme

class MainActivity : ComponentActivity() {

    private val viewModel: KasirViewModel by viewModels()

    // =========================================================================
    // KUNCI UKURAN FONT DI LEVEL ANDROID OS CONTEXT & RESOURCES:
    // Menjamin seluruh Activity, Dialog, Window, dan Views memiliki fontScale 1.0f
    // sehingga saat pengaturan font HP diubah, font di aplikasi tidak ikut berubah.
    // =========================================================================
    override fun attachBaseContext(newBase: Context) {
        val overrideConfig = Configuration(newBase.resources.configuration).apply {
            fontScale = 1.0f
        }
        val configContext = newBase.createConfigurationContext(overrideConfig)
        super.attachBaseContext(configContext)
    }

    override fun applyOverrideConfiguration(overrideConfiguration: Configuration?) {
        overrideConfiguration?.fontScale = 1.0f
        super.applyOverrideConfiguration(overrideConfiguration)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        newConfig.fontScale = 1.0f
        super.onConfigurationChanged(newConfig)
        val config = resources.configuration
        config.fontScale = 1.0f
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Pastikan resources internal terkunci fontScale = 1.0f
        val config = resources.configuration
        if (config.fontScale != 1.0f) {
            config.fontScale = 1.0f
            @Suppress("DEPRECATION")
            resources.updateConfiguration(config, resources.displayMetrics)
        }

        enableEdgeToEdge()

        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()

            // Request Bluetooth Connect permission for thermal printer on Android 12+ (API 31+)
            val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { /* Handled gracefully in UI */ }

            val coroutineScope = rememberCoroutineScope()
            val credentialManager = remember {
                CredentialManager.create(this@MainActivity)
            }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val permissionsNeeded = mutableListOf<String>()
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        permissionsNeeded.add(Manifest.permission.BLUETOOTH_CONNECT)
                    }
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                        permissionsNeeded.add(Manifest.permission.BLUETOOTH_SCAN)
                    }
                    if (permissionsNeeded.isNotEmpty()) {
                        bluetoothPermissionLauncher.launch(permissionsNeeded.toTypedArray())
                    }
                }

                // Attempt silent auto-sign-in for returning users
                GoogleAuthHelper.attemptAutoSignIn(
                    context = this@MainActivity,
                    credentialManager = credentialManager,
                    onAuthSuccess = { /* Automatically starts Firestore sync via AuthStateListener */ },
                    onUnauthenticated = { /* Handled gracefully with offline state */ },
                    scope = coroutineScope
                )
            }

            val currentUser by viewModel.currentUser.collectAsState()
            var isOfflineSkipped by remember { mutableStateOf(false) }
            var showSplash by remember { mutableStateOf(true) }

            // KUNCI UKURAN FONT DI LEVEL COMPOSE ROOT
            val currentDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = currentDensity.density,
                    fontScale = 1.0f // Kunci fontScale tepat di 1.0f agar ukuran tampilan tetap konsisten
                )
            ) {
                KasirProTheme(darkTheme = isDarkTheme) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        if (showSplash) {
                            SplashScreen(
                                onSplashFinished = { showSplash = false }
                            )
                        } else if (currentUser == null && !isOfflineSkipped) {
                            AuthScreen(
                                onAuthSuccess = { /* Automatically switches to MainScreen when Firebase auth state updates */ },
                                onSkipOffline = { isOfflineSkipped = true }
                            )
                        } else {
                            MainScreen(
                                viewModel = viewModel,
                                onRequireLogin = { isOfflineSkipped = false }
                            )
                        }
                    }
                }
            }
        }
    }
}
