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
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.math.BigInteger
import java.math.RoundingMode
import java.security.MessageDigest
import java.util.Locale
import kotlin.math.roundToLong

const val version = "1.55.4"
private const val SETTINGS_REQUEST_CODE = 100

class MainActivity : ComponentActivity() {

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private var isChangingConfigurations = false
    private var currentLanguage: String = "en" // Default to English

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize SharedPreferences
        val sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)

        // Load and apply the saved locale when the activity is created
        loadLocale(sharedPreferences)

        // Setup the content UI
        setupContent()

        val isFirstLaunch = sharedPreferences.getBoolean("isFirstLaunch", true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                // Handle the permission result here if needed
            }

            // Check if this is the first launch
            if (isFirstLaunch) {
                // Check if notification permission is granted
                if (ContextCompat.checkSelfPermission(this, POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    // Request permission
                    requestPermissionLauncher.launch(POST_NOTIFICATIONS)
                }

                // Update SharedPreferences to mark that the permission has been requested
                sharedPreferences.edit().putBoolean("isFirstLaunch", false).apply()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload and apply the saved locale when the activity resumes
        val sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val savedLanguage = sharedPreferences.getString("Language", "en") ?: "en"

        // Check if the language has changed
        if (currentLanguage != savedLanguage) {
            currentLanguage = savedLanguage
            setLocale(this, savedLanguage)
            recreate() // Only recreate if the language has changed
        }


        isChangingConfigurations = false
    }

    // Handle result from SettingsActivity
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SETTINGS_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                // Reload locale after returning from settings
                val sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
                val savedLanguage = sharedPreferences.getString("Language", "en") ?: "en"

                // Check if the language has changed
                if (currentLanguage != savedLanguage) {
                    currentLanguage = savedLanguage
                    setLocale(this, savedLanguage)
                    recreate() // Only recreate if the language has changed
                }
            }
        }
    }

    private fun setupContent() {
        setContent {
            IntentionRepeaterTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    private fun loadLocale(sharedPreferences: SharedPreferences) {
        val savedLanguage = sharedPreferences.getString("Language", "en") ?: "en"
        currentLanguage = savedLanguage
        setLocale(this, savedLanguage)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        isChangingConfigurations = true
    }

    override fun onDestroy() {
        if (!isChangingConfigurations && isFinishing) {
            val intent = Intent(applicationContext, TimerForegroundService::class.java)
            stopService(intent)
        }

        super.onDestroy()
    }

}

// Function to set the app's locale
fun setLocale(context: Context, languageCode: String) {
    val locale = Locale(languageCode)
    Locale.setDefault(locale)
    val config = Configuration()
    config.setLocale(locale)
    context.createConfigurationContext(config)
    context.resources.updateConfiguration(config, context.resources.displayMetrics)
}

fun sha512(input: String): String {
    val bytes = MessageDigest.getInstance("SHA-512").digest(input.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun Greeting(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    //val resources = context.resources
    //val configuration = resources.configuration

    //val currentLocale = configuration.locales.get(0)

    // To set a new locale:
    //val newLocale = Locale("sa") // Example: Sanskrit
    //configuration.setLocale(newLocale)
    //resources.updateConfiguration(configuration, resources.displayMetrics)

    val sharedPref = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
    val locale=Locale(sharedPref.getString("Language","en").toString())

    val config = context.resources.configuration
    if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.locales[0] != locale
        } else {
            config.locale !=locale
        }
    ) {
        val newConfig = config.apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        context.createConfigurationContext(newConfig)
        context.resources.updateConfiguration(newConfig, context.resources.displayMetrics)
    }
    val viewModel:TimerViewModel = viewModel()

    var selectedFrequency by rememberSaveable { mutableStateOf(sharedPref.getString("frequency", "7.83") ?: "7.83") }
    var isBoostEnabled by rememberSaveable { mutableStateOf(sharedPref.getBoolean("boost_enabled", false)) }
    var targetLength by remember { mutableLongStateOf(1L) }
    var time by remember { mutableStateOf("00:00:00") }
    val timerRunning by viewModel.timerRunning.observeAsState(false)
    var formattedIterations by remember { mutableStateOf(context.getString(R.string.iterations_zero_hz)) }
    var formattedIterationsCount by remember { mutableStateOf(context.getString(R.string.iterations_zero_hz)) }
    var intention by remember { mutableStateOf(sharedPref.getString("intention", "") ?: "") }
    val focusManager = LocalFocusManager.current
    val savedSliderPosition = sharedPref.getFloat("sliderPosition", 0f)
    var sliderPosition by remember { mutableFloatStateOf(savedSliderPosition) }
    val fiftyPercentOfFreeMemory = remember {
        Runtime.getRuntime().let { it.maxMemory() - (it.totalMemory() - it.freeMemory()) } * 0.5
    }
    val scrollState = rememberScrollState()
    var multiplier by remember { mutableStateOf(0L) }
    var isIntentionProcessed by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    // Initialize `isKeepAwakeEnabled` with a default value, saving its state across recompositions
    var isKeepAwakeEnabled by rememberSaveable { mutableStateOf(sharedPref.getBoolean("keep_awake_enabled", false)) }


    val loadingText=context.getString(R.string.loading_intention)
    val zeroIteration=context.getString(R.string.iterations_zero_hz)

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
            buttonText = if (timerRunning) stringResource(R.string.str_stop) else stringResource(R.string.str_start),
            onStartStopButtonClick = {
                focusManager.clearFocus()

                if (timerRunning) {
                    viewModel.setTimerRunning(false)
                    val intent = Intent(context, TimerForegroundService::class.java)
                    context.stopService(intent)

                    formattedIterations = context.getString(R.string.finished,formattedIterationsCount)
                } else {
                    formattedIterations = loadingText
                    time = "00:00:00"
                    multiplier = 0
                    targetLength = sliderPosition.roundToLong() * 1024 * 1024 / 4


                    if (targetLength * 4 > fiftyPercentOfFreeMemory) {
                        targetLength = (fiftyPercentOfFreeMemory / 4).toLong()
                        sliderPosition = (4 * targetLength / 1024 / 1024).toFloat()
                        sliderPosition = sliderPosition.coerceAtMost(maxMemoryUsageMB)
                    }
                    if (sliderPosition > maxMemoryUsageMB) {
                        sliderPosition = maxMemoryUsageMB
                    }
                    targetLength = sliderPosition.roundToLong() * 1024 * 1024 / 4
                    sharedPref.edit().putString("intention", intention).apply()
                    sharedPref.edit().putString("frequency", selectedFrequency).apply()
                    sharedPref.edit().putBoolean("boost_enabled", isBoostEnabled).apply()
                    sharedPref.edit().putBoolean("keep_awake_enabled", isKeepAwakeEnabled).apply() // Ensure it saves state properly
                    viewModel.setTimerRunning(true)
                    isIntentionProcessed = true


                    val intent = Intent(context, TimerForegroundService::class.java)
                    intent.putExtra("intention",intention);
                    intent.putExtra("targetLength",targetLength);
                    intent.putExtra("isBoostEnabled", isBoostEnabled)
                    intent.putExtra("timerRunning", timerRunning)
                    intent.putExtra("selectedFrequency", selectedFrequency)
                    intent.putExtra("isKeepAwakeEnabled", isKeepAwakeEnabled) // Pass keep awake state to the service

                    context.startService(intent)
                }
            },
            onResetButtonClick = {
                focusManager.clearFocus()
                formattedIterations = zeroIteration
                time = "00:00:00"
            },
            onInsertFileClick = handleInsertFileClick, // Pass the file selection logic
            scrollState = scrollState,
            expanded = expanded,
            onExpandChange = {
                expanded = !expanded
            },
            isKeepAwakeEnabled = isKeepAwakeEnabled,
            onKeepAwakeChange = { newValue ->
                isKeepAwakeEnabled = newValue
                sharedPref.edit().putBoolean("keep_awake_enabled", newValue).apply()
            }
        )
    }

    val mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            // Get extra data included in the Intent

            if(intent.hasExtra("stopTimer")){
                viewModel.setTimerRunning(false)
                val intents = Intent(context, TimerForegroundService::class.java)
                context!!.stopService(intents)

                formattedIterations = context.getString(R.string.finished,formattedIterationsCount)
            }

            if(timerRunning){
                if(intent.hasExtra("time")){
                    val times = intent.getStringExtra("time")
                    time= times.toString();
                }

                if(intent.hasExtra("iterations")){
                    val iterations = intent.getStringExtra("iterations")
                    val iterationsCount=intent.getStringExtra("iterationsCount")

                    formattedIterations= iterations.toString();
                    formattedIterationsCount=iterationsCount.toString()
                }
            }

        }
    }

    LocalBroadcastManager.getInstance(context).registerReceiver(
        mMessageReceiver, IntentFilter("IterationUpdate")
    );
}

fun getCurrentLocale(context: Context): Locale {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        context.resources.configuration.locales[0]
    } else {
        context.resources.configuration.locale
    }
}

fun hashFileContent(context: Context, uri: Uri): String {
    var inputStream: InputStream? = null
    return try {
        inputStream = context.contentResolver.openInputStream(uri)
        inputStream?.use { stream ->
            val bytes = stream.readBytes()
            val digest = MessageDigest.getInstance("SHA-512").digest(bytes)
            digest.joinToString("") { "%02x".format(it) }.uppercase()
        } ?: ""
    } catch (e: Exception) {
        e.printStackTrace()
        "" // Return empty string in case of any exception
    } finally {
        inputStream?.close()
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
    onExpandChange: (Boolean) -> Unit,
    isKeepAwakeEnabled: Boolean, // Add parameter for keep awake state
    onKeepAwakeChange: (Boolean) -> Unit // Callback for changing the keep awake state
) {
    val context = LocalContext.current // Correctly obtain the context in Compose

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
        // Checkbox for keeping the device awake
        KeepDeviceAwakeCheckbox(
            selectedFrequency = selectedFrequency,
            isKeepAwakeEnabled = isKeepAwakeEnabled,
            onKeepAwakeChange = onKeepAwakeChange,
            timerRunning = timerRunning
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

        Row(
            verticalAlignment = Alignment.CenterVertically, // Align items vertically in the center
            horizontalArrangement = Arrangement.Center, // Center the items horizontally within the row
            modifier = Modifier.fillMaxWidth() // Make the row fill the available width
        ) {
            // Gear Icon Button to open SettingsActivity
            IconButton(
                onClick = {
                    val intent = Intent(context, SettingsActivity::class.java)
                    (context as? MainActivity)?.startActivityForResult(intent, SETTINGS_REQUEST_CODE)
                },
                modifier = Modifier.size(56.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_settings_gear),
                    contentDescription = stringResource(R.string.settings),
                    modifier = Modifier.size(56.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp)) // Optional spacer to add space between items

            // Version Display
            VersionDisplay() // Remove the modifier parameter since it's not defined in VersionDisplay
        }

    }
}

@Composable
private fun SettingsButton() {
    val context = LocalContext.current
    Button(
        onClick = {
            val intent = Intent(context, SettingsActivity::class.java)
            context.startActivity(intent)
        },
        colors = ButtonDefaults.buttonColors(
            contentColor = Color.White,
            containerColor = Color.Blue
        ),
        modifier = Modifier
            .width(150.dp)
            .height(48.dp)
    ) {
        Text(
            text = stringResource(R.string.settings),
            color = Color.White,
            fontSize = 24.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AppTitle() {
    Spacer(modifier = Modifier.size(16.dp))
    val text = stringResource(R.string.intention_repeater_header)
    Text(
        text = text,
        fontSize = 24.sp,
        fontFamily = FontFamily.Serif,
        color = Color.White
    )
    Text(
        text = stringResource(R.string.by_anthro_teacher),
        fontSize = 20.sp,
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
        label = { Text(stringResource(R.string.enter_intentions), color = Color.White) },
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
                text = stringResource(R.string.multiplier, sliderPosition.roundToLong()),
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
    val options = listOf(
        Option(stringResource(R.string.three_herz_classic),"3"),
        Option(stringResource(R.string.schumann_resonance),"7.83"),
        Option(stringResource(R.string.str_maximum_frequency),"0")
    )

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
                value = if(selectedFrequency=="3") options[0].title else if(selectedFrequency=="7.83") options[1].title else options[2].title,
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
                .padding(start = 8.dp) // Aligns both checkboxes to the left
                .height(48.dp), // Set the height of the row to 48 dp
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Checkbox(
                checked = isBoostEnabled,
                onCheckedChange = { onBoostChange(it) },
                enabled = !timerRunning,
                modifier = Modifier
                    .size(24.dp) // Size of the checkbox itself
                    .width(56.dp)
                    .semantics { contentDescription = "Power Boost (Enables SHA-512 Encoding)" }
            )
            Spacer(modifier = Modifier.width(8.dp)) // Add spacing between the checkbox and text
            Text(
                text = stringResource(R.string.power_boost_uses_sha512_encoding),
                color = Color.White,
                fontSize = 14.sp, // Match the font size
                fontFamily = FontFamily.Serif,
                modifier = Modifier
                    .weight(1f) // Make the text take up remaining space in the row
                    .height(48.dp) // Ensures the text aligns vertically within the 48dp height
                    .wrapContentHeight(Alignment.CenterVertically) // Centers the text vertically within its container
            )
        }

    }
}

@Composable
fun KeepDeviceAwakeCheckbox(
    selectedFrequency: String,
    isKeepAwakeEnabled: Boolean,
    onKeepAwakeChange: (Boolean) -> Unit,
    timerRunning: Boolean
) {
    // Determine if the checkbox should be enabled or disabled
    val isCheckboxEnabled = (selectedFrequency == "3" || selectedFrequency == "7.83") && !timerRunning

    // Tooltip state
    var showTooltip by remember { mutableStateOf(false) }

    Box(modifier = Modifier.padding(start = 8.dp, top = 8.dp)) { // Align with other checkbox
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { showTooltip = true },
                        onTap = { showTooltip = false }
                    )
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp), // Set the row height to 48.dp
                verticalAlignment = Alignment.CenterVertically // Center the contents vertically
            ) {
                Checkbox(
                    checked = isKeepAwakeEnabled, // Always use the state without modification
                    onCheckedChange = {
                        if (isCheckboxEnabled) {
                            onKeepAwakeChange(it)
                        }
                    },
                    enabled = isCheckboxEnabled,
                    modifier = Modifier
                        .size(24.dp) // Size of the checkbox itself
                        .width(56.dp)
                        .semantics { contentDescription = "Keep Device Awake" }
                )
                Spacer(modifier = Modifier.width(8.dp)) // Add spacing between the checkbox and text
                Text(
                    text = stringResource(R.string.keep_device_awake),
                    color = if (isCheckboxEnabled) Color.White else Color.Gray,
                    fontSize = 14.sp, // Match the font size with "Power Boost"
                    fontFamily = FontFamily.Serif,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .align(Alignment.CenterVertically) // Center the text vertically within the row
                )
            }

        }

        if (showTooltip) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 36.dp)
                    .background(
                        Color.DarkGray,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                    )
                    .padding(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.prevents_device_sleeping),
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
        }
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
            contentPadding = PaddingValues(all = 8.dp),
            modifier = Modifier
                .weight(1f)
                .defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                contentColor = Color.White,
                containerColor = if(timerRunning) Color.Red else Color.Green
            )
        ) {
            Text(
                text = buttonText,
                color = if(timerRunning) Color.White else Color.Black,
                fontSize = 14.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.size(8.dp))
        Button(
            onClick = onResetButtonClick,
            enabled = !timerRunning,
            contentPadding = PaddingValues(all = 8.dp),
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .defaultMinSize(minHeight = 1.dp, minWidth = 1.dp),
            colors = ButtonDefaults.buttonColors(
                contentColor = Color.White,
                containerColor = Color.Blue
            )
        ) {
            Text(
                text = stringResource(R.string.reset),
                color = Color.White,
                fontSize = 14.sp,
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
            contentPadding = PaddingValues(all = 8.dp),
            modifier = Modifier
                .weight(1f)
                .defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                contentColor = Color.White,
                containerColor = Color.Blue
            )
        ) {
            Text(
                text = stringResource(R.string.str_load_file),
                color = Color.White,
                fontSize = 14.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun WebsiteButton(modifier: Modifier) {
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
        modifier = modifier
    ) {
        Text(
            text = stringResource(R.string.website),
            color = Color.Black,
            fontSize = 14.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ForumButton(modifier: Modifier) {
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
        modifier = modifier
    ) {
        Text(
            text = stringResource(R.string.forum),
            color = Color.Black,
            fontSize = 14.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun VersionDisplay() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.str_version, version),
            color = Color.White,
            fontSize = 14.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun EulaButton(modifier: Modifier) {
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
        modifier = modifier
    ) {
        Text(
            text = stringResource(R.string.eula),
            color = Color.Black,
            fontSize = 14.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PrivacyPolicyButton(modifier: Modifier) {
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
        modifier = modifier
    ) {
        Text(
            text = stringResource(R.string.privacy),
            color = Color.Black,
            fontSize = 14.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold
        )
    }
}

fun formatDecimalNumber(context: Context,value:Float):String{
    val units = arrayOf(context.getString(R.string.Hz), context.getString(R.string.kHz), context.getString(R.string.MHz), context.getString(R.string.GHz), context.getString(R.string.THz), context.getString(R.string.PHz), context.getString(R.string.EHz))
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


fun formatLargeNumber(context:Context,value: BigInteger): String {
    if (value < BigInteger("1000")) {
        return value.toString()
    }

    val names = arrayOf("", context.getString(R.string.k), context.getString(R.string.M), context.getString(R.string.B), context.getString(R.string.T), context.getString(R.string.q), context.getString(R.string.Q), context.getString(R.string.s), context.getString(R.string.S))
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

fun formatLargeFreq(context: Context,value: Float): String {
    val units = arrayOf(context.getString(R.string.Hz), context.getString(R.string.kHz), context.getString(R.string.MHz), context.getString(R.string.GHz), context.getString(R.string.THz), context.getString(R.string.PHz), context.getString(R.string.EHz))
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


    private lateinit var sharedPreferences:SharedPreferences
    private lateinit var context:Context;

    override fun onCreate() {
        super.onCreate()
        sharedPreferences=getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val savedLanguage = sharedPreferences.getString("Language", "en") ?: "en"
        context=this;

        setLocale(context, savedLanguage)
    }

    fun updateLocale(locale: Locale) {
        Locale.setDefault(locale)
        val config = context.resources.configuration
        config.setLocale(locale)
        config.setLayoutDirection(locale) // Important for RTL languages
        context= context.createConfigurationContext(config)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }

    private var timerRunning = false
    private var multiplier: Long = 0L
    private var selectedFrequency: String = "0"
    private var newIntention: String = ""
    private var isBoostEnabled: Boolean = false

    private var elapsedTime = 0L
    private var durationSec=0L
    private var iterations = 0.0.toFloat()
    private var startTime = System.nanoTime()
    private var lastTime="00:00:00";
    private var mutableIntention = newIntention

    private var updatedIterationCount="";


    @OptIn(DelicateCoroutinesApi::class)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)

        // Use safe call operator and provide default values
        intent?.let { safeIntent ->
            val notification = createNotification(context.getString(R.string.intention_repeater_header)+" 00:00:00", context.getString(R.string.loading_intention))
            startForeground(NOTIFICATION_ID, notification)

            // Acquire a partial wake lock
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "TimerForegroundService::WakeLock"
            )

            durationSec=sharedPreferences.getLong("Duration",0L) * 1000

            isBoostEnabled = intent.getBooleanExtra("isBoostEnabled", false)
            timerRunning = intent.getBooleanExtra("timerRunning", true)
            selectedFrequency = intent.getStringExtra("selectedFrequency").toString()
            val keepAwake = intent.getBooleanExtra("isKeepAwakeEnabled", false)

            val targetLength=intent.getLongExtra("targetLength",0);
            val intention=intent.getStringExtra("intention");


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

            newIntention = intentionBuilder.toString()
            multiplier = localMultiplier

            startTime = System.nanoTime()


            // Determine the wakelock duration based on the frequency and keep awake state
            if (selectedFrequency == "0") {
                // Always use a 10-minute wakelock if selectedFrequency is "0"
                wakeLock.acquire(10 * 60 * 1000L /* 10 minutes */)
            } else if (keepAwake) {
                // Indefinite wakelock if "Keep Device Awake" is enabled
                wakeLock.acquire()
            } else {
                // Fallback wakelock for 10 minutes
                wakeLock.acquire(10 * 60 * 1000L /* 10 minutes */)
            }

            val intentUpdate = Intent("IterationUpdate")

            if (timerRunning) {
                GlobalScope.launch(Dispatchers.Default) {
                    startTimer(onTimeUpdate = {
                        intentUpdate.putExtra("time", it)

                        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intentUpdate)
                    }, onIterationsUpdate ={ s: String, s1: String ->
                        intentUpdate.putExtra("iterations", s)
                        intentUpdate.putExtra("iterationsCount",s1);

                        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intentUpdate)
                    }, onTimerStop = {
                        intentUpdate.putExtra("stopTimer",true);

                        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intentUpdate)
                    })
                }
            }
        } ?: run {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }


    suspend fun startTimer(onTimeUpdate: (String) -> Unit, onIterationsUpdate: (String,String) -> Unit,onTimerStop:(Boolean)->Unit){
        var iterationsInLastSecond = 0.0.toFloat()
        var lastSecond = System.nanoTime()

        while (timerRunning) {

            val savedLanguage = sharedPreferences.getString("Language", "en") ?: "en"
            if(savedLanguage!=context.resources.configuration.locale.toString()){
                updateLocale(Locale(savedLanguage))
            }

            val loopStartTime = System.nanoTime()

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

                val updatedTime = String.format(Locale.ENGLISH,"%02d:%02d:%02d", hours, minutes, seconds)
                lastTime=updatedTime;

                val updatedIterations = context.getString(
                    R.string.str_iterations,
                    formatLargeNumber(context = this,BigInteger.valueOf(iterations.toLong())),
                    if (selectedFrequency == "7.83") formatDecimalNumber(context = context,7.83.toFloat() * multiplier) else formatLargeFreq(context = context,
                        (if (selectedFrequency == "3") "3".toFloat() else actualFrequency) * multiplier
                    )
                )

                updatedIterationCount=formatLargeNumber(context = context,BigInteger.valueOf(iterations.toLong()))

                if(durationSec>0&&elapsedTime-1000>=durationSec){
                    withContext(Dispatchers.Main){
                        onTimerStop(true)
                    }
                }else{
                    withContext(Dispatchers.Main) {
                        onTimeUpdate(updatedTime)
                        onIterationsUpdate(updatedIterations,updatedIterationCount)

                        if(timerRunning){
                            // do something
                            val notification: Notification = createNotification(context.getString(R.string.intention_repeater_header)+" "+updatedTime,updatedIterations)

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
        val notification=createNotification(context.getString(R.string.intention_repeater_finished),context.getString(R.string.str_iterations,updatedIterationCount,lastTime))

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.notify(100,notification)
    }

    private fun createNotification(title:String,text:String): Notification {
        val notificationBuilder = NotificationCompat.Builder(context, context.getString(R.string.app_name))

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
                getString(R.string.app_name),
                getString(R.string.show_intentions_update),
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