package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.BiruMudaHeaderEnd
import com.example.ui.theme.BiruMudaHeaderStart
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animasi scale & alpha halus saat splash screen tampil
    val transitionState = remember { MutableTransitionState(false) }
    LaunchedEffect(Unit) {
        transitionState.targetState = true
        delay(1800) // Tampilkan splash ~1.8 detik
        onSplashFinished()
    }

    val transition = updateTransition(transitionState, label = "SplashTransition")
    val scale by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 800, easing = FastOutSlowInEasing) },
        label = "LogoScale"
    ) { state -> if (state) 1f else 0.8f }

    val alpha by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 700) },
        label = "LogoAlpha"
    ) { state -> if (state) 1f else 0f }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFBCE1FD), // Biru muda di atas
                        Color(0xFFE8F3FD), // Campur biru muda lembut
                        Color(0xFFFFFFFF), // Putih di tengah
                        Color(0xFFE3F0FD)  // Sentuhan biru lembut di bawah
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(24.dp)
                .scale(scale)
                .alpha(alpha)
        ) {
            // Logo / Icon Aplikasi di tengah
            Surface(
                modifier = Modifier
                    .size(140.dp)
                    .shadow(elevation = 16.dp, shape = RoundedCornerShape(28.dp), spotColor = Color(0xFF1E88E5).copy(alpha = 0.35f)),
                shape = RoundedCornerShape(28.dp),
                color = Color.White
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "Logo KasirKu Pintar",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .clip(RoundedCornerShape(22.dp))
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Nama Aplikasi
            Text(
                text = "KasirKu Pintar",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = BiruMudaHeaderStart,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Creator Credit: by Rafline Dev
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = BiruMudaHeaderStart.copy(alpha = 0.1f),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(BiruMudaHeaderStart)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "by Rafline Dev",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = BiruMudaHeaderStart
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Versi 1.0 Full Version",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = AccentEmerald
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Loading indicator minimalis
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = BiruMudaHeaderEnd,
                strokeWidth = 2.5.dp
            )
        }
    }
}
