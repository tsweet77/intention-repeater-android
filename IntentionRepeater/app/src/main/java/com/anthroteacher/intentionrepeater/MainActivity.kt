package com.anthroteacher.intentionrepeater

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import com.anthroteacher.intentionrepeater.ui.theme.IntentionRepeaterTheme
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigInteger
import java.math.RoundingMode
import java.security.MessageDigest
import kotlin.math.roundToLong

const val version = "Version 1.12"

class MainActivity : ComponentActivity() {
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
    }
}

fun sha256(input: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun Greeting(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)

    var selectedFrequency by rememberSaveable { mutableStateOf(sharedPref.getString("frequency", "3") ?: "3") }
    var isBoostEnabled by rememberSaveable { mutableStateOf(sharedPref.getBoolean("boost_enabled", false)) } // Load Boost value from SharedPreferences
    var targetLength by remember { mutableLongStateOf(1L) }
    var time by remember { mutableStateOf("00:00:00") }
    var timerRunning by remember { mutableStateOf(false) }
    var formattedIterations by remember { mutableStateOf("0 Iterations (0 Hz)") }
    var intention by remember { mutableStateOf(sharedPref.getString("intention", "") ?: "") }
    val focusManager = LocalFocusManager.current
    val savedSliderPosition = sharedPref.getFloat("sliderPosition", 0f)
    var sliderPosition by remember { mutableFloatStateOf(savedSliderPosition) }
    val ninetyPercentOfFreeMemory = remember {
        Runtime.getRuntime().let { it.maxMemory() - (it.totalMemory() - it.freeMemory()) } * 0.9
    }
    val scrollState = rememberScrollState()
    var intentionMultiplied by remember { mutableStateOf(StringBuilder()) }
    var newIntention by remember { mutableStateOf("") }
    var multiplier by remember { mutableStateOf(0L) }
    var isIntentionProcessed by remember { mutableStateOf(false) }

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
            buttonText = if (timerRunning) "Stop" else "Start",
            onStartStopButtonClick = {
                focusManager.clearFocus()
                if (timerRunning) {
                    timerRunning = false
                } else {
                    formattedIterations = "Loading Intention..."
                    intentionMultiplied.clear()
                    multiplier = 0
                    targetLength = sliderPosition.roundToLong() * 1024 * 1024 / 3
                    if (targetLength * 3 > ninetyPercentOfFreeMemory) {
                        targetLength = (ninetyPercentOfFreeMemory / 3).toLong()
                        sliderPosition = (targetLength / 1024 / 1024).toFloat()
                    }
                    sharedPref.edit().putString("intention", intention).apply()
                    sharedPref.edit().putString("frequency", selectedFrequency).apply()
                    sharedPref.edit().putBoolean("boost_enabled", isBoostEnabled).apply() // Save Boost value to SharedPreferences
                    timerRunning = true
                    isIntentionProcessed = false
                }
            },
            onResetButtonClick = {
                focusManager.clearFocus()
                formattedIterations = "0 Iterations (0 Hz)"
                time = "00:00:00"
            },
            scrollState = scrollState
        )
    }

    if (timerRunning && !isIntentionProcessed) {
        ProcessIntentionMultiplication(
            targetLength = targetLength,
            intention = intention,
            onIntentionProcessed = { processedIntention, processedMultiplier ->
                newIntention = processedIntention
                multiplier = processedMultiplier
                intentionMultiplied = StringBuilder(processedIntention)
                isIntentionProcessed = true
            }
        )
    } else if (timerRunning && isIntentionProcessed) {
        TimerLogic(
            timerRunning = timerRunning,
            selectedFrequency = selectedFrequency,
            multiplier = multiplier,
            newIntention = newIntention,
            isBoostEnabled = isBoostEnabled, // Pass Boost value to TimerLogic
            onTimeUpdate = { time = it },
            onIterationsUpdate = { formattedIterations = it }
        )
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
    scrollState: ScrollState
) {
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
            timerRunning = timerRunning
        )
        TimerDisplay(time = time)
        IterationsDisplay(formattedIterations = formattedIterations)
        Spacer(modifier = Modifier.size(24.dp))
        StartStopResetButtons(
            buttonText = buttonText,
            onStartStopButtonClick = onStartStopButtonClick,
            onResetButtonClick = onResetButtonClick,
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

            Spacer(modifier = Modifier.size(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                EulaButton()
                Spacer(modifier = Modifier.size(16.dp))
                PrivacyPolicyButton()
            }
        }
        Spacer(modifier = Modifier.size(12.dp))
        VersionDisplay()
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
            .heightIn(min = 192.dp),
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
    timerRunning: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp), // Ensure the row height is at least 48dp
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start // Align items to the start
        ) {
            RadioButton(
                selected = selectedFrequency == "3",
                onClick = { if (!timerRunning) onFrequencyChange("3") },
                enabled = !timerRunning,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "3 Hz (Optimal)",
                color = Color.White,
                fontSize = 16.sp,
                fontFamily = FontFamily.Serif,
                modifier = Modifier.padding(start = 4.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            RadioButton(
                selected = selectedFrequency == "0",
                onClick = { if (!timerRunning) onFrequencyChange("0") },
                enabled = !timerRunning,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "Max Freq",
                color = Color.White,
                fontSize = 16.sp,
                fontFamily = FontFamily.Serif,
                modifier = Modifier.padding(start = 4.dp)
            )
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
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "Power Boost - Uses Sha256 Encoding",
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
    timerRunning: Boolean,
    intention: String
) {
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
                containerColor = Color.Blue
            )
        ) {
            Text(
                text = buttonText,
                color = Color.White,
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
                text = "Reset",
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

@Composable
private fun VersionDisplay() {
    Text(
        text = version,
        color = Color.White,
        fontSize = 14.sp,
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun TimerLogic(
    timerRunning: Boolean,
    multiplier: Long,
    selectedFrequency: String,
    newIntention: String,
    onTimeUpdate: (String) -> Unit,
    onIterationsUpdate: (String) -> Unit,
    isBoostEnabled: Boolean,
) {
    val elapsedTime = remember { mutableStateOf(0L) }
    val iterations = remember { mutableStateOf(BigInteger.ZERO) }
    val startTime = remember { mutableStateOf(System.nanoTime()) }
    val lastUpdate = remember { mutableStateOf(startTime.value) }
    var mutableIntention = newIntention

    LaunchedEffect(timerRunning) {
        var iterationsInLastSecond = 0L

        while (timerRunning) {
            val loopStartTime = System.nanoTime()

            // Process the intention
            var processIntention = newIntention

            if (isBoostEnabled) {
                mutableIntention = sha256(mutableIntention)
            }

            iterationsInLastSecond++

            // Calculate actual loop time
            //val actualLoopTimeNs = System.nanoTime() - loopStartTime

            if (selectedFrequency == "3") {
                delay(333)
            }

            // Update every second
            if (System.nanoTime() - lastUpdate.value >= 1_000_000_000L) {
                elapsedTime.value = (System.nanoTime() - startTime.value) / 1_000_000L // Convert to ms

                val hours = elapsedTime.value / 3600000
                val minutes = (elapsedTime.value / 60000) % 60
                val seconds = (elapsedTime.value / 1000) % 60

                iterations.value += BigInteger.valueOf(iterationsInLastSecond * multiplier)

                // Calculate frequency for the last second
                val actualFrequency = iterationsInLastSecond.toFloat()

                val updatedTime = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                val updatedIterations = "${formatLargeNumber(iterations.value)} Iterations (${formatLargeFreq(actualFrequency*multiplier)})"

                withContext(Dispatchers.Main) {
                    onTimeUpdate(updatedTime)
                    onIterationsUpdate(updatedIterations)
                }

                // Reset for the next second
                iterationsInLastSecond = 0L
                lastUpdate.value = System.nanoTime()
            }
        }
    }
}

@Composable
fun ProcessIntentionMultiplication(
    targetLength: Long,
    intention: String,
    onIntentionProcessed: (String, Long) -> Unit
) {
    LaunchedEffect(targetLength, intention) {
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
        onIntentionProcessed(
            newIntention,
            localMultiplier
        ) // Callback with the new intention and multiplier
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
    return if (unitIndex == 0) {
        // If the value is still in Hz, show it without decimals
        String.format("%.0f %s", adjustedValue, units[unitIndex])
    } else {
        // Otherwise, show it with three decimal places
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

    @OptIn(DelicateCoroutinesApi::class)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        // Acquire a partial wake lock
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "TimerForegroundService::WakeLock"
        )
        wakeLock.acquire(10*60*1000L /*10 minutes*/)

        // Start a coroutine to perform your timer logic
        GlobalScope.launch {
            while (true) {
                // Perform your timer logic here
                // For example, update the elapsed time, iterations, etc.

                delay(1000) // Delay for 1 second
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release the wake lock when the service is destroyed
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }

    private fun createNotification(): Notification {
        val notificationBuilder = NotificationCompat.Builder(this, "default")

        return notificationBuilder
            .setContentTitle("Intention Repeater is running")
            .setContentText("Intention Repeater is running in the background")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    IntentionRepeaterTheme {
        Greeting()
    }
}