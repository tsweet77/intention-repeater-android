package com.anthroteacher.intentionrepeater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.anthroteacher.intentionrepeater.ui.theme.IntentionRepeaterTheme

class SettingsActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IntentionRepeaterTheme {
                SettingsScreen()
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current

    // Set the background color to black
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Heading text in white
        Text(
            text = "Intention Repeater Settings",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Button for managing notifications
        Button(
            onClick = {
                // Open the app's notification settings page
                val intent = Intent().apply {
                    action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
                context.startActivity(intent)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Blue,
                contentColor = Color.White
            )
        ) {
            Text(
                text = "Manage Notifications"
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Website, Forum, EULA, Privacy Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                WebsiteButton()
                Spacer(modifier = Modifier.height(16.dp))
                ForumButton()
                Spacer(modifier = Modifier.height(16.dp))
                EulaButton()
                Spacer(modifier = Modifier.height(16.dp))
                PrivacyPolicyButton()
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Back Button
        Button(
            onClick = { (context as? ComponentActivity)?.finish() }
        ) {
            Text("Back")
        }
    }
}
