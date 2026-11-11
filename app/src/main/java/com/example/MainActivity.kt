package com.example

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.ui.KasirViewModel
import com.example.ui.MainScreen
import com.example.ui.theme.KasirProTheme

class MainActivity : ComponentActivity() {

    private val viewModel: KasirViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()

            // Request Bluetooth Connect permission for thermal printer on Android 12+ (API 31+)
            val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { /* Handled gracefully in UI */ }

            val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
            val credentialManager = androidx.compose.runtime.remember {
                androidx.credentials.CredentialManager.create(this@MainActivity)
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
                com.example.ui.auth.GoogleAuthHelper.attemptAutoSignIn(
                    context = this@MainActivity,
                    credentialManager = credentialManager,
                    onAuthSuccess = { /* Automatically starts Firestore sync via AuthStateListener */ },
                    onUnauthenticated = { /* Handled gracefully with offline state */ },
                    scope = coroutineScope
                )
            }

            val currentUser by viewModel.currentUser.collectAsState()
            var isOfflineSkipped by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

            KasirProTheme(darkTheme = isDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (currentUser == null && !isOfflineSkipped) {
                        com.example.ui.auth.AuthScreen(
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
