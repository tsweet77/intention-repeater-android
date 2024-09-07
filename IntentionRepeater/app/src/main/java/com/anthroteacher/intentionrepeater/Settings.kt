package com.anthroteacher.intentionrepeater

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.anthroteacher.intentionrepeater.ui.theme.IntentionRepeaterTheme
import java.io.File

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

@SuppressLint("ServiceCast")
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Mutable state to track the notification status
    var notificationEnabled by remember { mutableStateOf(notificationManager.areNotificationsEnabled()) }

    // Get the lifecycle owner to observe lifecycle changes
    val lifecycleOwner = LocalLifecycleOwner.current

    // Observe the lifecycle state of the composable
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Recheck the notification status when the user returns to the screen
                notificationEnabled = notificationManager.areNotificationsEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

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

        Spacer(modifier = Modifier.height(8.dp))

        // Notification Status Message
        NotificationStatusMessage(enabled = notificationEnabled)

        Spacer(modifier = Modifier.height(24.dp))

        // "Open Notes File" button with matching style
        Button(
            onClick = { openNotesFile(context) },
            modifier = Modifier,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Blue,
                contentColor = Color.White
            )
        ) {
            Text("Open Notes for Intentions")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Row for Website and Forum buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            // Using button composables from MainActivity
            WebsiteButton()
            Spacer(modifier = Modifier.width(16.dp)) // Spacer between Website and Forum buttons
            ForumButton()
        }

        Spacer(modifier = Modifier.height(16.dp)) // Spacer between rows

        // Row for EULA and Privacy buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            // Using button composables from MainActivity
            EulaButton()
            Spacer(modifier = Modifier.width(16.dp)) // Spacer between EULA and Privacy buttons
            PrivacyPolicyButton()
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

// Function to handle opening the notes file
@SuppressLint("QueryPermissionsNeeded")
// Function to handle opening the notes file
fun openNotesFile(context: Context) {
    val notesFile = File(context.filesDir, "intention_repeater_notes.txt")

    // Check if the file exists, and create it if it doesn't
    if (!notesFile.exists()) {
        notesFile.createNewFile()
    }

    // Create a URI for the file using FileProvider
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        notesFile
    )

    // Create an intent to open the file
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "text/plain")
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    }

    // Check if there is an app to handle the intent
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    } else {
        // Show a message or prompt the user to install a text editor
        Toast.makeText(context, "No text editor found. Please install one from the Play Store.", Toast.LENGTH_LONG).show()
    }
}

@Composable
fun NotificationStatusMessage(enabled: Boolean) {
    // Construct the message with color highlighting for ENABLED/ DISABLED and default white text
    val statusText = buildAnnotatedString {
        // Set the default color for the rest of the text to white
        withStyle(style = SpanStyle(color = Color.White)) {
            append("Notifications are ")

            if (enabled) {
                withStyle(style = SpanStyle(color = Color.Green)) {
                    append("ENABLED")
                }
                append(". When running, the notification will update each second with elapsed time, current frequency, and number of iterations.")
            } else {
                withStyle(style = SpanStyle(color = Color.Red)) {
                    append("DISABLED")
                }
                append(". You can enable notifications to view elapsed time, current frequency, and number of iterations when running as a notification.")
            }
        }
    }

    // Display the styled text
    Text(
        text = statusText,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(16.dp)
    )
}