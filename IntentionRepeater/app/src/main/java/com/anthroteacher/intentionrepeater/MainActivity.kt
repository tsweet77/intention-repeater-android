package com.anthroteacher.intentionrepeater

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.anthroteacher.intentionrepeater.ui.theme.IntentionRepeaterTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.math.BigInteger
import java.math.RoundingMode
import java.security.MessageDigest
import kotlin.math.roundToLong

const val version = "Version 1.29"

class MainActivity : ComponentActivity() {

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private var isChangingConfigurations = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            IntentionRepeaterTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->

        }
        when {
            ContextCompat.checkSelfPermission(this, POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED -> {
                // Request permission
                    requestPermissionLauncher.launch(POST_NOTIFICATIONS)
                }
            }
        }

    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d("TEST-R","configuration changed");
        isChangingConfigurations = true
    }

    override fun onDestroy() {
        Log.d("TEST-R","destroy called");

        if (!isChangingConfigurations && isFinishing) {
            val intent = Intent(applicationContext, TimerForegroundService::class.java)
            stopService(intent)
        }

        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        Log.d("TEST-R","resumed");

        isChangingConfigurations = false
    }
}


fun sha512(input: String): String {
    val bytes = MessageDigest.getInstance("SHA-512").digest(input.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun Greeting(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)

    val viewModel:TimerViewModel = viewModel()

    var selectedFrequency by rememberSaveable { mutableStateOf(sharedPref.getString("frequency", "7.83") ?: "7.83") }
    var isBoostEnabled by rememberSaveable { mutableStateOf(sharedPref.getBoolean("boost_enabled", false)) }
    var targetLength by remember { mutableLongStateOf(1L) }
    var time by remember { mutableStateOf("00:00:00") }
    val timerRunning by viewModel.timerRunning.observeAsState(false)
    var formattedIterations by remember { mutableStateOf("0 Iterations (0 Hz)") }
    var intention by remember { mutableStateOf(sharedPref.getString("intention", "") ?: "") }
    val focusManager = LocalFocusManager.current
    val savedSliderPosition = sharedPref.getFloat("sliderPosition", 0f)
    var sliderPosition by remember { mutableFloatStateOf(savedSliderPosition) }
    val fiftyPercentOfFreeMemory = remember {
        Runtime.getRuntime().let { it.maxMemory() - (it.totalMemory() - it.freeMemory()) } * 0.5
    }
    val scrollState = rememberScrollState()
    var intentionMultiplied by remember { mutableStateOf(StringBuilder()) }
    var newIntention by remember { mutableStateOf("") }
    var multiplier by remember { mutableStateOf(0L) }
    var isIntentionProcessed by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    val resultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                val hashedValue = hashFileContent(context, it)
                intention += hashedValue // Append the hash to the intention text box
            }
        }
    )

    val handleInsertFileClick = {
        resultLauncher.launch(arrayOf("*/*")) // Allow any file type
    }

    val maxMemoryUsageMB = 100f // Set the maximum allowed memory usage to 100 MB

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { focusManager.clearFocus() }
                )
            }
    ) {
        MainContent(
            selectedFrequency = selectedFrequency,
            intention = intention,
            onIntentionChange = { intention = it },
            timerRunning = timerRunning,
            isBoostEnabled = isBoostEnabled,
            onBoostChange = { isBoostEnabled = it },
            onFrequencyChange = { newFrequency ->
                selectedFrequency = newFrequency
            },
            sliderPosition = sliderPosition,
            onSliderPositionChange = { newValue ->
                sliderPosition = newValue.roundToLong().toFloat()
                sharedPref.edit().putFloat("sliderPosition", sliderPosition).apply()
            },
            time = time,
            formattedIterations = formattedIterations,
            buttonText = if (timerRunning) "STOP" else "START",
            onStartStopButtonClick = {
                focusManager.clearFocus()
                if (timerRunning) {
                    viewModel.setTimerRunning(false)
                    val intent = Intent(context, TimerForegroundService::class.java)
                    context.stopService(intent)

                    if(formattedIterations=="Loading Intention..."){
                        formattedIterations="0 Iterations (0 Hz)"
                    }
                } else {
                    formattedIterations = "Loading Intention..."
                    time="00:00:00"
                    intentionMultiplied.clear()
                    multiplier = 0
                    targetLength = sliderPosition.roundToLong() * 1024 * 1024 / 4
                    if (targetLength * 4 > fiftyPercentOfFreeMemory) {
                        targetLength = (fiftyPercentOfFreeMemory / 4).toLong()
                        sliderPosition = (4 * targetLength / 1024 / 1024).toFloat()
                        // Ensure sliderPosition does not exceed 100
                        sliderPosition = sliderPosition.coerceAtMost(maxMemoryUsageMB)
                    }
                    // Adjust targetLength again to make sure it's within the 100 MB limit
                    if (sliderPosition > maxMemoryUsageMB) {
                        sliderPosition = maxMemoryUsageMB
                    }
                    targetLength = sliderPosition.roundToLong() * 1024 * 1024 / 4
                    sharedPref.edit().putString("intention", intention).apply()
                    sharedPref.edit().putString("frequency", selectedFrequency).apply()
                    sharedPref.edit().putBoolean("boost_enabled", isBoostEnabled).apply()
                    viewModel.setTimerRunning(true)
                    isIntentionProcessed = true

                    val intentionBuilder = StringBuilder()
                    var localMultiplier = 0L

                    if (targetLength > 0) {
                        while (intentionBuilder.length < targetLength) {
                            intentionBuilder.append(intention)
                            localMultiplier++
                        }
                    } else {
                        localMultiplier = 1
                        intentionBuilder.append(intention)
                    }

                    val newIntention = intentionBuilder.toString()

                    intentionMultiplied = StringBuilder(newIntention)
                    multiplier=localMultiplier;

                    val intent = Intent(context, TimerForegroundService::class.java)
                    sharedPref.edit().putString("newIntention", newIntention).apply()
                    intent.putExtra("isBoostEnabled",isBoostEnabled);
                    intent.putExtra("timerRunning",timerRunning);
                    intent.putExtra("multiplier",multiplier);
                    intent.putExtra("selectedFrequency",selectedFrequency);
                    context.startService(intent)
                }
            },
            onResetButtonClick = {
                focusManager.clearFocus()
                formattedIterations = "0 Iterations (0 Hz)"
                time = "00:00:00"
            },
            onInsertFileClick = handleInsertFileClick, // Pass the file selection logic
            scrollState = scrollState,
            expanded = expanded,
            onExpandChange = {
                expanded=!expanded
            }
        )
    }

    val mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            // Get extra data included in the Intent
            val times = intent.getStringExtra("time")
            val iterations = intent.getStringExtra("iterations")

            time= times.toString();
            formattedIterations= iterations.toString();

        }
    }

    LocalBroadcastManager.getInstance(context).registerReceiver(
        mMessageReceiver, IntentFilter("IterationUpdate")
    );
}

fun hashFileContent(context: Context, uri: Uri): String {
    var inputStream: InputStream? = null
    return try {
        // Open input stream from the file URI
        inputStream = context.contentResolver.openInputStream(uri)
        val bytes = inputStream?.use { it.readBytes() } ?: byteArrayOf()

        // Generate the hash value using SHA-512
        val digest = MessageDigest.getInstance("SHA-512").digest(bytes)
        val hashValue = digest.joinToString("") { "%02x".format(it) }.uppercase()

        // Clear file content from memory
        inputStream?.close() // Explicitly close the InputStream
        inputStream = null // Nullify the reference to allow garbage collection

        // Return the hash value
        hashValue
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    } finally {
        // Ensure input stream is closed in case of an exception
        inputStream?.close()
        inputStream = null // Nullify the reference
    }
}

@Composable
private fun MainContent(
    selectedFrequency: String,
    intention: String,
    onFrequencyChange: (String) -> Unit,
    isBoostEnabled: Boolean,
    onBoostChange: (Boolean) -> Unit,
    onIntentionChange: (String) -> Unit,
    timerRunning: Boolean,
    sliderPosition: Float,
    onSliderPositionChange: (Float) -> Unit,
    time: String,
    formattedIterations: String,
    buttonText: String,
    onStartStopButtonClick: () -> Unit,
    onResetButtonClick: () -> Unit,
    onInsertFileClick: () -> Unit,
    scrollState: ScrollState,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit
) {
    var isEulaPrivacyVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppTitle()
        IntentionTextField(
            intention = intention,
            onIntentionChange = onIntentionChange,
            timerRunning = timerRunning
        )
        MultiplierSlider(
            sliderPosition = sliderPosition,
            onSliderPositionChange = onSliderPositionChange,
            timerRunning = timerRunning
        )
        FrequencyAndBoostSelector(
            selectedFrequency = selectedFrequency,
            onFrequencyChange = onFrequencyChange,
            isBoostEnabled = isBoostEnabled,
            onBoostChange = onBoostChange,
            timerRunning = timerRunning,
            expanded=expanded,
            onExpandChange=onExpandChange
        )
        TimerDisplay(time = time)
        IterationsDisplay(formattedIterations = formattedIterations)
        Spacer(modifier = Modifier.size(24.dp))
        StartStopResetButtons(
            buttonText = buttonText,
            onStartStopButtonClick = onStartStopButtonClick,
            onResetButtonClick = onResetButtonClick,
            onInsertFileClick = onInsertFileClick, // Pass the onInsertFileClick here
            timerRunning = timerRunning,
            intention = intention
        )
        Spacer(modifier = Modifier.size(24.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                WebsiteButton()
                Spacer(modifier = Modifier.size(16.dp))
                ForumButton()
            }

            Spacer(modifier = Modifier.size(12.dp))
            VersionDisplay(isEulaPrivacyVisible) { isEulaPrivacyVisible = !isEulaPrivacyVisible }

            if (isEulaPrivacyVisible) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    EulaButton()
                    PrivacyPolicyButton()
                }
            }
        }
    }
}

@Composable
private fun AppTitle() {
    Spacer(modifier = Modifier.size(16.dp))
    Text(
        text = "Intention Repeater",
        fontSize = 32.sp,
        fontFamily = FontFamily.Serif,
        color = Color.White
    )
    Text(
        text = "by Anthro Teacher",
        fontSize = 24.sp,
        fontFamily = FontFamily.Serif,
        color = Color.White
    )
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun IntentionTextField(
    intention: String,
    onIntentionChange: (String) -> Unit,
    timerRunning: Boolean
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    OutlinedTextField(
        value = intention,
        enabled = !timerRunning,
        onValueChange = onIntentionChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(192.dp),
        label = { Text("Enter Intentions", color = Color.White) },
        singleLine = false,
        keyboardOptions = KeyboardOptions.Default.copy(
            imeAction = ImeAction.Default
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                keyboardController?.hide()
                focusManager.clearFocus()
            }
        ),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            cursorColor = Color.White,
            focusedBorderColor = Color.Blue,
            unfocusedBorderColor = Color.Gray
        ),
        textStyle = LocalTextStyle.current.copy(color = Color.White, lineHeight = 24.sp),
        maxLines = Int.MAX_VALUE
    )
}

@Composable
private fun MultiplierSlider(
    sliderPosition: Float,
    onSliderPositionChange: (Float) -> Unit,
    timerRunning: Boolean
) {
    Column(
        modifier = Modifier.padding(4.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Mult [${sliderPosition.roundToLong()}]: 0",
                fontSize = 14.sp,
                fontFamily = FontFamily.Serif,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(4.dp))
            Slider(
                value = sliderPosition,
                enabled = !timerRunning,
                onValueChange = onSliderPositionChange,
                valueRange = 0f..100f,
                steps = 19,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "100",
                fontSize = 14.sp,
                fontFamily = FontFamily.Serif,
                color = Color.White
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrequencyAndBoostSelector(
    selectedFrequency: String,
    onFrequencyChange: (String) -> Unit,
    isBoostEnabled: Boolean,
    onBoostChange: (Boolean) -> Unit,
    timerRunning: Boolean,
    expanded: Boolean,
    onExpandChange:(Boolean) -> Unit,
) {
    data class Option(val title: String, val value: String)
    val options = listOf(Option("3 Hz (Classic)","3"), Option("7.83 Hz Schumann Res. (Optimal)","7.83"), Option("Maximum Frequency","0"))

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = {
                if(!timerRunning){
                    onExpandChange(!expanded)
                }
            }
        ) {
            TextField(
                readOnly = true,
                value = if(selectedFrequency=="3") options.get(0).title else if(selectedFrequency=="7.83") options.get(1).title else options.get(2).title,
                onValueChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = ""
                    )
                }
            )
            ExposedDropdownMenu(
                expanded = expanded,
                modifier = Modifier.fillMaxWidth(),
                onDismissRequest = { onExpandChange(false) }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.title) },
                        onClick = {
                            onFrequencyChange(option.value)
                            onExpandChange(false)
                        }
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp), // Ensure the row height is at least 48dp
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start // Align items to the start
        ) {
            Checkbox(
                checked = isBoostEnabled,
                onCheckedChange = { onBoostChange(it) },
                enabled = !timerRunning,
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = "Power Boost (Enables SHA-512 Encoding)" }
            )
            Text(
                text = "Power Boost - Uses Sha512 Encoding",
                color = Color.White,
                fontSize = 16.sp,
                fontFamily = FontFamily.Serif,
                modifier = Modifier.padding(start = 4.dp)

            )
        }
    }
}

@Composable
private fun BoostCheckbox(
    isBoostEnabled: Boolean,
    onBoostChange: (Boolean) -> Unit,
    timerRunning: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Checkbox(
            checked = isBoostEnabled,
            onCheckedChange = { onBoostChange(it) },
            enabled = !timerRunning,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = "Boost",
            color = Color.White,
            fontSize = 14.sp,
            fontFamily = FontFamily.Serif,
            modifier = Modifier.padding(start = 4.dp) // Add padding between checkbox and label
        )
    }
}

@Composable
private fun TimerDisplay(time: String) {
    Text(
        text = time,
        fontSize = 48.sp,
        fontFamily = FontFamily.Serif,
        color = Color.White
    )
}

@Composable
private fun IterationsDisplay(formattedIterations: String) {
    Text(
        text = formattedIterations,
        fontSize = 20.sp,
        fontFamily = FontFamily.Serif,
        color = Color.White
    )
}

@Composable
private fun StartStopResetButtons(
    buttonText: String,
    onStartStopButtonClick: () -> Unit,
    onResetButtonClick: () -> Unit,
    onInsertFileClick: () -> Unit,
    timerRunning: Boolean,
    intention: String
) {
    // Start and Reset buttons on the first line
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(
            onClick = onStartStopButtonClick,
            enabled = intention.isNotBlank(),
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                contentColor = Color.White,
                containerColor = if(timerRunning) Color.Red else Color.Green
            )
        ) {
            Text(
                text = buttonText,
                color = if(timerRunning) Color.White else Color.Black,
                fontSize = 24.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.size(8.dp))
        Button(
            onClick = onResetButtonClick,
            enabled = !timerRunning,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                contentColor = Color.White,
                containerColor = Color.Blue
            )
        ) {
            Text(
                text = "RESET",
                color = Color.White,
                fontSize = 24.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold
            )
        }
    }

    Spacer(modifier = Modifier.size(8.dp))

    // Insert File button on its own line
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(
            onClick = onInsertFileClick,
            enabled = !timerRunning,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                contentColor = Color.White,
                containerColor = Color.Blue
            )
        ) {
            Text(
                text = "LOAD FILE",
                color = Color.White,
                fontSize = 24.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun WebsiteButton() {
    val context = LocalContext.current
    Button(
        onClick = {
            val url = "https://www.intentionrepeater.com"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        },
        colors = ButtonDefaults.buttonColors(
            contentColor = Color.Black,
            containerColor = Color.Green
        ),
        modifier = Modifier
            //.fillMaxWidth(0.4f)
            .width(150.dp)
            .height(48.dp)
    ) {
        Text(
            text = "Website",
            color = Color.Black,
            fontSize = 24.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ForumButton() {
    val context = LocalContext.current

    Button(
        onClick = {
            val url = "https://intentionrepeater.boards.net/"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        },
        colors = ButtonDefaults.buttonColors(
            contentColor = Color.Black,
            containerColor = Color.Green
        ),
        modifier = Modifier
            .width(150.dp)
            .height(48.dp)
    ) {
        Text(
            text = "Forum",
            color = Color.Black,
            fontSize = 24.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun VersionDisplay(isEulaPrivacyVisible: Boolean, onToggleVisibility: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = "$version ...",
            color = Color.White,
            fontSize = 14.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.pointerInput(Unit) {
                detectTapGestures(onTap = { onToggleVisibility() })
            }
        )
    }
}

@Composable
private fun EulaButton() {
    val context = LocalContext.current

    Button(
        onClick = {
            val url = "https://www.intentionrepeater.com/android_eula.html"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        },
        colors = ButtonDefaults.buttonColors(
            contentColor = Color.Black,
            containerColor = Color.Green
        ),
        modifier = Modifier
            .width(150.dp)
            .height(48.dp)
    ) {
        Text(
            text = "EULA",
            color = Color.Black,
            fontSize = 24.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PrivacyPolicyButton() {
    val context = LocalContext.current

    Button(
        onClick = {
            val url = "https://www.intentionrepeater.com/android_privacy_policy.html"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        },
        colors = ButtonDefaults.buttonColors(
            contentColor = Color.Black,
            containerColor = Color.Green
        ),
        modifier = Modifier
            .width(150.dp)
            .height(48.dp)
    ) {
        Text(
            text = "Privacy",
            color = Color.Black,
            fontSize = 24.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold
        )
    }
}

fun formatDecimalNumber(value:Float):String{
    val units = arrayOf("Hz", "kHz", "MHz", "GHz", "THz", "PHz", "EHz")
    var adjustedValue = value
    var unitIndex = 0

    // Adjust the value to the correct unit
    while (adjustedValue >= 1000 && unitIndex < units.size - 1) {
        adjustedValue /= 1000
        unitIndex++
    }

    // Format the frequency
    return if(adjustedValue==7.83.toFloat()) {
        // Otherwise, show it with three decimal places
        String.format("%.2f %s", adjustedValue, units[unitIndex])
    }else{
        String.format("%.3f %s", adjustedValue, units[unitIndex])
    }
}


fun formatLargeNumber(value: BigInteger): String {
    if (value < BigInteger("1000")) {
        return value.toString()
    }

    val names = arrayOf("", "k", "M", "B", "T", "q", "Q", "s", "S")
    val magnitude = value.toString().length
    val index = (magnitude - 1) / 3

    if (index >= names.size) {
        return value.toString()
    }

    val divisor = BigInteger.TEN.pow(index * 3)
    val formattedValue =
        value.toBigDecimal().divide(divisor.toBigDecimal(), 3, RoundingMode.HALF_UP)

    return String.format("%.3f%s", formattedValue, names[index])
}

fun formatLargeFreq(value: Float): String {
    val units = arrayOf("Hz", "kHz", "MHz", "GHz", "THz", "PHz", "EHz")
    var adjustedValue = value
    var unitIndex = 0

    // Adjust the value to the correct unit
    while (adjustedValue >= 1000 && unitIndex < units.size - 1) {
        adjustedValue /= 1000
        unitIndex++
    }

    // Format the frequency
    return if(unitIndex==0) {
        // Otherwise, show it with three decimal places
        String.format("%.0f %s", adjustedValue, units[unitIndex])
    }else{
        String.format("%.3f %s", adjustedValue, units[unitIndex])
    }
}

class TimerForegroundService : Service() {
    companion object {
        const val NOTIFICATION_ID = 1
    }

    private lateinit var wakeLock: PowerManager.WakeLock

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private var timerRunning = false
    private var multiplier: Long = 0L
    private var selectedFrequency: String = "0"
    private var newIntention: String = ""
    private var isBoostEnabled: Boolean = false

    private var elapsedTime = 0L
    private var iterations = 0.0.toFloat()
    private var startTime = System.nanoTime()
    private var lastUpdate = startTime
    private var mutableIntention = newIntention


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        if(intent!=null){
            val notification = createNotification("Intention Repeater 00:00:00","Loading Intention...")
            startForeground(NOTIFICATION_ID, notification)

            // Acquire a partial wake lock
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "TimerForegroundService::WakeLock"
            )
            wakeLock.acquire(10*60*1000L /*10 minutes*/)
            val sharedPref = applicationContext.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)


            newIntention= sharedPref.getString("newIntention","").toString()
            isBoostEnabled=intent.getBooleanExtra("isBoostEnabled",false)
            timerRunning= intent.getBooleanExtra("timerRunning",true)
            selectedFrequency= intent.getStringExtra("selectedFrequency").toString()
            multiplier=intent.getLongExtra("multiplier",0L)
            val intentUpdate = Intent("IterationUpdate")

            if (timerRunning) {
                GlobalScope.launch(Dispatchers.Default) {
                    startTimer(onTimeUpdate = {
                        intentUpdate.putExtra("time", it)

                        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intentUpdate)
                    }, onIterationsUpdate = {
                        intentUpdate.putExtra("iterations", it)

                        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intentUpdate)
                    })
                }
            }
        }

        return START_STICKY
    }


    suspend fun startTimer(onTimeUpdate: (String) -> Unit, onIterationsUpdate: (String) -> Unit){
        var iterationsInLastSecond = 0.0.toFloat()
        var lastSecond = System.nanoTime()

        while (timerRunning) {
            val loopStartTime = System.nanoTime()

            // Process the intention
            var processIntention = newIntention

            if (isBoostEnabled) {
                mutableIntention = sha512("$mutableIntention: $newIntention")
            }

             if(selectedFrequency!="3"&&selectedFrequency!="7.83"){
                 iterationsInLastSecond++
             }else{
                 val timeTakenForHashingNs = System.nanoTime() - loopStartTime
                 val frequency=if(selectedFrequency=="3") 3 else 7.83
                 val delayMilliseconds = (1.toFloat() / frequency.toDouble()) * 1000.0

                 val remainingDelayMilliseconds = delayMilliseconds - (timeTakenForHashingNs / 1_000_000.0)
                 preciseDelay(remainingDelayMilliseconds)
             }

            // Update every second
            val now = System.nanoTime()
            if (now - lastSecond >= 1_000_000_000L) {
                if(selectedFrequency=="3"){
                    iterationsInLastSecond=3.toFloat()
                }else if(selectedFrequency=="7.83"){
                    iterationsInLastSecond=7.83.toFloat()
                }
                elapsedTime = (now - startTime) / 1_000_000L // Convert to ms

                val hours = elapsedTime / 3600000
                val minutes = (elapsedTime / 60000) % 60
                val seconds = (elapsedTime / 1000) % 60

                iterations += (iterationsInLastSecond.toFloat() * multiplier)

                // Calculate frequency for the last second
                val actualFrequency = iterationsInLastSecond.toFloat()

                val updatedTime = String.format("%02d:%02d:%02d", hours, minutes, seconds)

                val updatedIterations =  "${ formatLargeNumber(BigInteger.valueOf(iterations.toLong()))} Iterations (${ if(selectedFrequency=="7.83") formatDecimalNumber(7.83.toFloat() * multiplier) else formatLargeFreq((if(selectedFrequency=="3") "3".toFloat() else actualFrequency) * multiplier)})"

                withContext(Dispatchers.Main) {
                    onTimeUpdate(updatedTime)
                    onIterationsUpdate(updatedIterations)

                    if(timerRunning){
                        // do something
                        val notification: Notification = createNotification("Intention Repeater "+updatedTime,updatedIterations)

                        val mNotificationManager =
                            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                        mNotificationManager.notify(NOTIFICATION_ID, notification)
                    }
                }

                // Reset for the next second
                iterationsInLastSecond = 0.0.toFloat()
                lastSecond = now
            }
        }
    }

    fun preciseDelay(milliseconds: Double) {
        // Split milliseconds into whole and fractional parts
        val wholeMilliseconds = milliseconds.toLong() // Whole part of the milliseconds
        val fractionalMilliseconds = milliseconds - wholeMilliseconds // Fractional part
        val nanoseconds = (fractionalMilliseconds * 1_000_000).toInt().coerceIn(0, 999999) // Convert to nanoseconds

        // Use Thread.sleep for the precise delay
        if (wholeMilliseconds > 0 || nanoseconds > 0) {
            Thread.sleep(wholeMilliseconds, nanoseconds)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release the wake lock when the service is destroyed
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
        timerRunning=false
        stopForeground(true)
        stopSelf()
    }

    private fun createNotification(title:String,text:String): Notification {
        val notificationBuilder = NotificationCompat.Builder(this, "Intention Repeater")

        if (Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.S
        ) {
            notificationBuilder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }

        val pendingIntent = createPendingIntent(applicationContext)


        return notificationBuilder
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    fun createPendingIntent(context: Context): PendingIntent {
        // Intent to start an activity when the notification is tapped
        val intent = Intent(context, MainActivity::class.java)

        // Create a PendingIntent for the intent
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_MUTABLE
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "Intention Repeater",
                "Intention Repeater is running",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager =
                getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    IntentionRepeaterTheme {
        Greeting()
    }
}