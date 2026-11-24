package com.example.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager

@Composable
fun AuthScreen(onAuthSuccess: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = { },
            scope = scope
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Fullscreen Background Map Bumi Papan Selaras
        Image(
            painter = painterResource(id = com.example.R.drawable.bg_bps_menu),
            contentDescription = "Peta Kawasan Bumi Papan Selaras",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xB8050A14),
                            Color(0x98050A14),
                            Color(0xCC050A14)
                        )
                    )
                )
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
            contentAlignment = Alignment.Center
        ) {
            val isLandscape = maxWidth >= 560.dp

            if (isLandscape) {
                Row(
                    modifier = Modifier
                        .widthIn(max = 920.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Side: Branding + Feature Pills over scenic background
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "PATROLI MULTIPLAYER 3D",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            text = stringResource(com.example.R.string.app_name),
                            style = MaterialTheme.typography.displayLarge,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = stringResource(com.example.R.string.app_subtitle),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFE2E8F0)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AuthFeaturePill(
                                icon = Icons.Filled.LocationSearching,
                                title = "Peta Luas",
                                subtitle = "Mobil & Motor",
                                modifier = Modifier.weight(1f)
                            )
                            AuthFeaturePill(
                                icon = Icons.Filled.RecordVoiceOver,
                                title = "Radio PTT",
                                subtitle = "Suara & Ping",
                                modifier = Modifier.weight(1f)
                            )
                            AuthFeaturePill(
                                icon = Icons.Filled.Shield,
                                title = "Gudang",
                                subtitle = "Armor & Senjata",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Right Side: GANDI Briefing + Sign-In Action
                    Column(
                        modifier = Modifier.weight(0.95f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xDC0F172A)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f)),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(id = com.example.R.drawable.img_gandi_avatar),
                                    contentDescription = "Avatar Operator GANDI",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, MaterialTheme.colorScheme.secondary, CircleShape)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = stringResource(com.example.R.string.gandi_name),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        text = "\"Ayo cepat absen, komandan! Penyusup baru saja terlihat melintasi Blok Cemara dekat Area Masjid Bumi Papan Selaras!\"",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        if (errorMessage != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = errorMessage!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                errorMessage = null
                                isLoading = true
                                onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = {
                                        isLoading = false
                                        onAuthSuccess()
                                    },
                                    onAuthError = { msg ->
                                        isLoading = false
                                        errorMessage = msg
                                    },
                                    scope = scope,
                                    onAuthCancelled = { cancelReason ->
                                        isLoading = false
                                        errorMessage = cancelReason
                                    }
                                )
                            },
                            enabled = !isLoading,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("google_sign_in_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Icon(Icons.Filled.Security, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = stringResource(com.example.R.string.sign_in_google),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .widthIn(max = 520.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = stringResource(com.example.R.string.app_name),
                        style = MaterialTheme.typography.displayLarge,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = stringResource(com.example.R.string.app_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFE2E8F0)
                    )
                    Button(
                        onClick = {
                            errorMessage = null
                            isLoading = true
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isLoading = false
                                    onAuthSuccess()
                                },
                                onAuthError = { msg ->
                                    isLoading = false
                                    errorMessage = msg
                                },
                                scope = scope,
                                onAuthCancelled = { cancelReason ->
                                    isLoading = false
                                    errorMessage = cancelReason
                                }
                            )
                        },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("google_sign_in_button")
                    ) {
                        Text(stringResource(com.example.R.string.sign_in_google), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthFeaturePill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xD40F172A),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8)
            )
        }
    }
}
