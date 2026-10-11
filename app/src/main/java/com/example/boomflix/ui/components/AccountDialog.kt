package com.example.boomflix.ui.components

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import com.example.boomflix.BuildConfig
import com.example.boomflix.data.auth.AuthManager
import com.example.boomflix.data.turso.TursoSyncManager
import com.example.boomflix.theme.BoomflixBackground
import com.example.boomflix.theme.BoomflixCardSurface
import com.example.boomflix.theme.BoomflixDarkSurface
import com.example.boomflix.theme.BoomflixRed
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AccountDialog(
    onDismiss: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val authManager = remember { AuthManager.getInstance(context) }
    val syncManager = remember { TursoSyncManager.getInstance(context) }
    val currentUser by authManager.currentUser.collectAsState()
    val scope = rememberCoroutineScope()

    var syncMessage by remember { mutableStateOf<String?>(null) }
    var syncError by remember { mutableStateOf<String?>(null) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                authManager.handleGoogleSignInResult(account)
                syncMessage = "Signed in as ${account.email}"
            }
        } catch (e: Exception) {
            syncError = "Sign-In error: ${e.message}"
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF141414),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E2E2E)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Account & Cloud Sync",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // User Profile Section
                val user = currentUser
                if (user != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1A1A1A), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF2E2E2E), RoundedCornerShape(8.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!user.photoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = user.photoUrl,
                                contentDescription = user.displayName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(if (user.isAnonymous) Color(0xFF333333) else BoomflixRed),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = user.displayInitial,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = user.displayName ?: if (user.isAnonymous) "Guest User" else "User",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!user.email.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = user.email,
                                    fontSize = 12.sp,
                                    color = Color(0xFFAAAAAA),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (user.isAnonymous) Color(0x33888888) else Color(0x3300C853)
                            ) {
                                Text(
                                    text = if (user.isAnonymous) "Guest Session" else "Google Account",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (user.isAnonymous) Color(0xFFB0B0B0) else Color(0xFF00E676),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (user.isAnonymous) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                if (activity != null) {
                                    val client = authManager.getGoogleSignInClient(activity)
                                    googleSignInLauncher.launch(client.signInIntent)
                                } else {
                                    onDismiss()
                                    onNavigateToLogin()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF444444))
                        ) {
                            Text(
                                text = "Switch to Google Account",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    // Not signed in
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1A1A1A), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF2E2E2E), RoundedCornerShape(8.dp))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "User",
                            tint = Color.Gray,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Not Signed In",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Sign in to sync your watchlist and playback history across devices.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            lineHeight = 16.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                onDismiss()
                                onNavigateToLogin()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            )
                        ) {
                            Text(
                                text = "Sign In with Google",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Turso Cloud Sync Card
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF181818),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E2E2E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = "Turso Cloud",
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Turso LibSQL Cloud",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Direct LibSQL pipeline sync with the Boomflix web database. Watchlist and continue watching stay synchronized.",
                            fontSize = 11.sp,
                            color = Color(0xFF888888),
                            lineHeight = 16.sp
                        )

                        if (syncMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF00E676),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = syncMessage ?: "",
                                    fontSize = 11.sp,
                                    color = Color(0xFF00E676)
                                )
                            }
                        }

                        if (syncError != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = syncError ?: "",
                                fontSize = 11.sp,
                                color = Color(0xFFFF8080)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF162518), RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(if (currentUser != null) Color(0xFF00E676) else Color(0xFF888888), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (currentUser != null) "Automatic background sync active" else "Sign in to enable cloud sync",
                                fontSize = 12.sp,
                                color = if (currentUser != null) Color(0xFF00E676) else Color(0xFFAAAAAA),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Sign out button if user is signed in
                if (currentUser != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    TextButton(
                        onClick = {
                            authManager.signOut(activity)
                            syncMessage = "Signed out"
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Sign Out",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Sign Out",
                            color = Color(0xFFFF5252),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
