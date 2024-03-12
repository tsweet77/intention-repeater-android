package com.anthroteacher.intentionrepeater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anthroteacher.intentionrepeater.ui.theme.IntentionRepeaterTheme
import kotlinx.coroutines.delay
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToLong

const val version = "Version 1.1"

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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun Greeting(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
    val savedIntention = sharedPref.getString("intention", "") ?: ""
    var targetLength by remember { mutableLongStateOf(1L) }
    var time by remember { mutableStateOf("00:00:00") }
    var timerRunning by remember { mutableStateOf(false) }
    var iterations by remember { mutableLongStateOf(0L) }
    var formattedIterations by remember { mutableStateOf("0 Iterations (0 Hz)") }
    var intention by remember { mutableStateOf(savedIntention) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
// Retrieve the saved slider position; if not found, default to 0f
    val savedSliderPosition = sharedPref.getFloat("sliderPosition", 0f)
    var sliderPosition by remember { mutableFloatStateOf(savedSliderPosition) }
    val runtime = Runtime.getRuntime()
    val maxMemory = runtime.maxMemory() // Maximum memory that the JVM will attempt to use
    val allocatedMemory = runtime.totalMemory() // Total memory currently in use by the JVM
    val freeMemory = runtime.freeMemory() // An amount of free memory in the JVM
    val totalFreeMemory = maxMemory - (allocatedMemory - freeMemory)
    val ninetyPercentOfFreeMemory: Double = totalFreeMemory * 0.9
    // Use a scrollable Column if the content exceeds screen size
    val scrollState = rememberScrollState()
    var intentionMultiplied by remember { mutableStateOf(StringBuilder()) } // Ensure this is defined in your composable
    var newIntention by remember { mutableStateOf("") }
    var multiplier by remember { mutableStateOf(0L) }
    var isIntentionProcessed by remember { mutableStateOf(false) }

    if (timerRunning && !isIntentionProcessed) {
        ProcessIntention(
            targetLength = targetLength, // Make sure this is defined and has a value
            intention = intention, // Ensure this is the user-defined intention
            onIntentionProcessed = { processedIntention, processedMultiplier ->
                // This will be executed when the intention processing is finished
                newIntention = processedIntention
                multiplier = processedMultiplier
                intentionMultiplied = StringBuilder(processedIntention) // Update the intentionMultiplied with the new intention
                isIntentionProcessed = true // Set the flag to true as the intention is now processed
            }
        )
    } else if (timerRunning && isIntentionProcessed) {
        TimerLogic(
            timerRunning = timerRunning,
            multiplier = multiplier, // Ensure this is defined and holds the correct value
            newIntention = newIntention, // Ensure this is defined and holds the user's intention
            onTimeUpdate = { updatedTime ->
                time = updatedTime // Update time with the new value from the timer logic
            },
            onIterationsUpdate = { updatedIterations ->
                formattedIterations = updatedIterations // Update formattedIterations with the new value from the timer logic
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { /* Called when the press gesture starts */ },
                    onDoubleTap = { /* Called on double tap */ },
                    onLongPress = { /* Intentionally left blank to ignore long presses */ },
                    onTap = { focusManager.clearFocus() } // Clears focus from all focusable children when the Box is clicked
                )
            }
    ) {
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Intention Repeater",
                fontSize = 24.sp,
                fontFamily = FontFamily.Serif,
                color = Color.White
            )
            Text(
                text = "by Anthro Teacher",
                fontSize = 14.sp,
                fontFamily = FontFamily.Serif,
                color = Color.White
            )

            OutlinedTextField(
                value = intention,
                enabled = !timerRunning,
                onValueChange = { intention = it },
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
                        onValueChange = { newValue ->
                            sliderPosition = newValue.roundToLong().toFloat()
                            sharedPref.edit().putFloat("sliderPosition", sliderPosition).apply()
                        },
                        valueRange = 0f..10f,
                        steps = 9,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = "10",
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Serif,
                        color = Color.White
                    )
                }
            }

            Text(
                text = time,
                fontSize = 24.sp,
                fontFamily = FontFamily.Serif,
                color = Color.White
            )

            Spacer(modifier = Modifier.size(8.dp))

            Text(
                text = formattedIterations,
                fontSize = 14.sp,
                fontFamily = FontFamily.Serif,
                color = Color.White
            )

            Spacer(modifier = Modifier.size(24.dp))

            var buttonText by remember { mutableStateOf("Start") }

            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        if (timerRunning) {
                            timerRunning = false
                            buttonText = "Start"
                        } else {
                            formattedIterations = "Loading Intention into Memory"
                            buttonText = "Stop"
                            intentionMultiplied.clear()
                            multiplier = 0
                            targetLength = sliderPosition.roundToLong() * 1024 * 1024 / 3
                            if (targetLength * 3 > ninetyPercentOfFreeMemory) {
                                targetLength = (ninetyPercentOfFreeMemory / 3).toLong()
                                sliderPosition = (targetLength / 1024 / 1024).toFloat()
                            }

                            with(sharedPref.edit()) {
                                putString("intention", intention)
                                apply()
                            }
                            timerRunning = true
                            isIntentionProcessed = false
                            //Placeholder for old intention multiplying code
                        }
                    },
                    enabled = intention.isNotBlank(),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        contentColor = Color.Black,
                        containerColor = Color.Blue
                    ),
                ) {
                    Text(
                        buttonText,
                        color = Color.Black,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.size(8.dp))

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        iterations = 0
                        formattedIterations = "0 Iterations (0 Hz)"
                        time = "00:00:00"
                    },
                    enabled = !timerRunning,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        contentColor = Color.Black,
                        containerColor = Color.Blue
                    ),
                ) {
                    Text(
                        "Reset",
                        color = Color.Black,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.size(48.dp))

            Button(
                onClick = {
                    val url = "https://www.intentionrepeater.com"
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse(url)
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.buttonColors(
                    contentColor = Color.Black,
                    containerColor = Color.Green
                ),
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(48.dp)
            ) {
                Text(
                    "Website",
                    color = Color.Black,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.size(16.dp))

            Button(
                onClick = {
                    val url = "https://intentionrepeater.boards.net/"
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse(url)
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.buttonColors(
                    contentColor = Color.Black,
                    containerColor = Color.Green
                ),
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(48.dp)
            ) {
                Text(
                    "Forum",
                    color = Color.Black,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.size(12.dp))
            Text(
                version,
                color = Color.White,
                fontSize = 14.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TimerLogic(
    timerRunning: Boolean,
    multiplier: Long,
    newIntention: String,
    onTimeUpdate: (String) -> Unit,
    onIterationsUpdate: (String) -> Unit
) {
    val elapsedTime = remember { mutableStateOf(0L) }
    val iterations = remember { mutableStateOf(0L) }
    val freq = remember { mutableStateOf(0L) }
    val startTime = System.currentTimeMillis()
    val lastUpdate = remember { mutableStateOf(startTime) }
    var processIntention = ""

    LaunchedEffect(timerRunning) {
        while (timerRunning) {
            for (i in 1..10000000) {
                processIntention = newIntention
            }
            val currentTime = System.currentTimeMillis()
            elapsedTime.value = currentTime - startTime

            iterations.value += 10000000
            freq.value += 10000000

            if (currentTime - lastUpdate.value >= 1000) {
                val hours = elapsedTime.value / 3600000
                val minutes = (elapsedTime.value / 60000) % 60
                val seconds = (elapsedTime.value / 1000) % 60

                val updatedTime = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                val updatedIterations = "${formatLargeNumber(iterations.value * multiplier)} Iterations (${formatLargeFreq(freq.value * multiplier)})"

                onTimeUpdate(updatedTime)
                onIterationsUpdate(updatedIterations)

                lastUpdate.value = currentTime
                freq.value = 0 // Reset frequency counter for the next second
            }

            delay(1) // Reduce CPU usage
        }
    }
}

@Composable
fun ProcessIntention(
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

fun formatLargeNumber(value: Long): String {
    val names = arrayOf("k", "M", "B", "T", "q", "Q", "s", "S")
    val index = (log10(value.toDouble()).toInt() / 3).takeIf { it > 0 } ?: return value.toString()
    val divisor = 10.0.pow(index * 3)
    val formattedValue = value / divisor
    return String.format("%.2f%s", formattedValue, names[index - 1])
}

fun formatLargeFreq(value: Long): String {
    val names = arrayOf("kHz", "MHz", "GHz", "THz", "PHz", "EHz")
    val index = (log10(value.toDouble()).toInt() / 3).takeIf { it > 0 } ?: return value.toString()
    val divisor = 10.0.pow(index * 3)
    val formattedValue = value / divisor
    return String.format("%.2f%s", formattedValue, names[index - 1])
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    IntentionRepeaterTheme {
        Greeting()
    }
}