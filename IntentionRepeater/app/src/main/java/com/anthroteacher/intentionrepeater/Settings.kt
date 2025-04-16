package com.anthroteacher.intentionrepeater

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.anthroteacher.intentionrepeater.ui.theme.IntentionRepeaterTheme
import java.io.File
import java.util.Locale

class SettingsActivity : ComponentActivity() {
    private lateinit var sharedPreferences: SharedPreferences

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)

        // Load and apply the saved locale when the activity is created
        loadLocale()

        setContent {
            IntentionRepeaterTheme {
                // Make the SettingsScreen scrollable
                SettingsScreen(
                    currentLocale = sharedPreferences.getString("Language", "en") ?: "en",
                    onLanguageChange = { newLocale ->
                        saveLanguageToPreferences(newLocale)
                        setLocale(this, newLocale) // Apply the new locale
                        recreate() // Recreate the activity to reflect changes
                    },
                    currentDuration = sharedPreferences.getLong("Duration", 0L).toString(),
                    onDurationChange = { newDuration ->
                        var value = 0L
                        if (newDuration.isNotEmpty() && newDuration.isDigitsOnly()) {
                            value = newDuration.toLong()
                        }
                        sharedPreferences.edit().putLong("Duration", value).apply()
                    }
                )
            }
        }
    }

    // Function to load and apply the saved locale from SharedPreferences
    private fun loadLocale() {
        val savedLanguage = sharedPreferences.getString("Language", "en") ?: "en"
        setLocale(this, savedLanguage) // Apply the saved or default locale
    }

    // Function to save the selected language to SharedPreferences
    private fun saveLanguageToPreferences(languageCode: String) {
        sharedPreferences.edit().putString("Language", languageCode).apply()
    }

    // Function to set the app's locale
    private fun setLocale(context: Context, languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val resources = context.resources
        val config = resources.configuration
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        context.createConfigurationContext(config)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    // Apply the locale whenever the activity is resumed
    override fun onResume() {
        super.onResume()
        loadLocale() // Reload and apply the locale when the activity resumes
    }
}

@SuppressLint("ServiceCast")
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun SettingsScreen(
    currentLocale: String,
    onLanguageChange: (String) -> Unit,
    currentDuration : String,
    onDurationChange: (String)->Unit
) {
    val context = LocalContext.current
    val notificationManagerCompat = NotificationManagerCompat.from(context)
    var linksVisible by remember { mutableStateOf(false)}
    val focusManager = LocalFocusManager.current
    var maxWidth by remember { mutableStateOf(0) }

    // Mutable state to track the notification status
    var notificationEnabled by remember { mutableStateOf(notificationManagerCompat.areNotificationsEnabled()) }

    // Get the lifecycle owner to observe lifecycle changes
    val lifecycleOwner = LocalLifecycleOwner.current

    // State to keep track of selected language
    var selectedLanguage by remember { mutableStateOf(currentLocale) }
    var selectedDuration by remember { mutableStateOf(currentDuration) }

    // Observe the lifecycle state of the composable
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Recheck the notification status when the user returns to the screen
                notificationEnabled = notificationManagerCompat.areNotificationsEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Create a ScrollState to control the vertical scroll
    val scrollState = rememberScrollState()

    // Set the background color to black
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(scrollState)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            }, // Make the Column scrollable vertically
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        // Heading text in white
        Text(
            text = stringResource(R.string.intention_repeater_settings),
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
                text = stringResource(R.string.manage_notifications)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Notification Status Message
        NotificationStatusMessage(enabled = notificationEnabled)

        Spacer(modifier = Modifier.height(16.dp))

        // "Open Notes File" button with matching style
        Button(
            onClick = { openNotesFile(context) },
            modifier = Modifier,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Blue,
                contentColor = Color.White
            )
        ) {
            Text(stringResource(R.string.open_notes_for_intentions))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Language dropdown
        Column(modifier = Modifier.padding(16.dp)) {
            LanguageDropdown(
                currentLocale = selectedLanguage,
                onLanguageSelected = { newLanguage ->
                    selectedLanguage = newLanguage
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // "Update Language" button
        Button(
            onClick = {
                onLanguageChange(selectedLanguage)  // Use selectedLanguage instead of currentLocale
            },
            modifier = Modifier
                .padding(8.dp)
                .align(Alignment.CenterHorizontally)
        ) {
            Text(stringResource(R.string.update_language))
        }
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ){
            DurationInputField(duration = selectedDuration) { newDuration ->
                selectedDuration=newDuration
                onDurationChange(newDuration)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            ClickableText(
                text = AnnotatedString(stringResource(R.string.links)),
                modifier = Modifier.padding(8.dp),
                onClick = {
                    linksVisible=!linksVisible;
                },
                style = TextStyle(color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))

        if(linksVisible){
            // Row for Website and Forum buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Using button composables from MainActivity
                WebsiteButton(Modifier
                    //.fillMaxWidth(0.4f)
                    .width(220.dp)
                    .height(48.dp))
                Spacer(modifier = Modifier.height(16.dp)) // Spacer between Website and Forum buttons
                ForumButton(Modifier
                    //.fillMaxWidth(0.4f)
                    .width(220.dp)
                    .height(48.dp))
                Spacer(modifier = Modifier.height(16.dp)) // Spacer between Website and Forum buttons
                EulaButton(Modifier
                    //.fillMaxWidth(0.4f)
                    .width(220.dp)
                    .height(48.dp))
                Spacer(modifier = Modifier.height(16.dp)) // Spacer between EULA and Privacy buttons
                PrivacyPolicyButton(Modifier
                    //.fillMaxWidth(0.4f)
                    .width(220.dp)
                    .height(48.dp))
            }

            Spacer(modifier = Modifier.height(16.dp)) // Spacer between rows
        }

        Button(
            onClick = {
                val url = "https://multihasher.intentionrepeater.com"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            },
            colors = ButtonDefaults.buttonColors(
                contentColor = Color.White,
                containerColor = Color.Blue
            ),
            modifier = Modifier
                .width(220.dp)
                .height(48.dp)
        ) {
            Text(
                text = stringResource(R.string.multihasher_app),
                color = Color.White,
                fontSize = 14.sp,
                fontFamily = FontFamily.Serif
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Back Button
        Button(
            onClick = { (context as? ComponentActivity)?.finish() },
            modifier = Modifier
                .width(220.dp)
                .height(52.dp)
        ) {
            Text(stringResource(R.string.back))
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
        Toast.makeText(context,
            context.getString(R.string.no_text_editor_found_please_install_one_from_the_play_store), Toast.LENGTH_LONG).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageDropdown(
    currentLocale: String,
    onLanguageSelected: (String) -> Unit // Renamed to onLanguageSelected for clarity
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf(currentLocale) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        TextField(
            readOnly = true,
            value = languages.find { it.code == selectedLanguage }?.displayName ?: "Select Language",
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            trailingIcon = {
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = "Dropdown Icon"
                )
            }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            languages.forEach { language ->
                DropdownMenuItem(
                    text = { Text(language.displayName) },
                    onClick = {
                        selectedLanguage = language.code
                        expanded = false
                        onLanguageSelected(selectedLanguage) // Pass the selected language up
                    }
                )
            }
        }
    }
}


@Composable
fun DurationInputField(duration:String,onDurationChange:(String)->Unit) {
    Column {
        Text(
            text = "Duration", // The label for the TextField
            style = MaterialTheme.typography.bodyLarge, // Set the text style as per your theme
            modifier = Modifier.fillMaxWidth()
        )
        TextField(
            value = duration.toString(),
            onValueChange = { newText ->
                // Update the state with the new text
                onDurationChange(newText)
            },
            label = { Text(stringResource(R.string.seconds)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
        )
    }
}

@Composable
fun NotificationStatusMessage(enabled: Boolean) {
    // Construct the message with color highlighting for ENABLED/ DISABLED and default white text
    val statusText = buildAnnotatedString {
        // Set the default color for the rest of the text to white
        withStyle(style = SpanStyle(color = Color.White)) {
            if (enabled) {
                append(stringResource(R.string.notifications_enabled))
            } else {
                append(stringResource(R.string.notifications_disabled))
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