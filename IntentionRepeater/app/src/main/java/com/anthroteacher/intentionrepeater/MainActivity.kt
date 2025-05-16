package com.anthroteacher.intentionrepeater

import Intention
import IntentionViewModel
import android.Manifest.permission.POST_NOTIFICATIONS
import android.R.attr.maxLines
import android.annotation.SuppressLint
import android.app.Activity
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
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.anthroteacher.intentionrepeater.db.IntentionDBHelper
import com.anthroteacher.intentionrepeater.ui.theme.IntentionRepeaterTheme
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bouncycastle.jcajce.provider.digest.SHA3
import org.burnoutcrew.reorderable.*
import java.io.InputStream
import java.math.BigInteger
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong


const val version = "2.0"
private const val SETTINGS_REQUEST_CODE = 100
private const val VOICE_INTENTION_REQUEST_CODE = 101

class MainActivity : ComponentActivity() {

    private val intentionViewModel: IntentionViewModel by viewModels()
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private var isChangingConfigurations = false
    private var currentLanguage: String = "en"

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
        }else if(requestCode == VOICE_INTENTION_REQUEST_CODE){
            if(resultCode== RESULT_OK){
                Log.d("TESTH","Coming in traditional");
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

            intentionViewModel.stopAllIntentions();
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
    val bytes = SHA3.Digest512().digest(input.toByteArray())
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
    val intentionViewModel: IntentionViewModel = viewModel()

    var selectedFrequency by rememberSaveable { mutableStateOf("1") }
    var isBoostEnabled by rememberSaveable { mutableStateOf( false) }
    var targetLength by remember { mutableLongStateOf(1L) }
    var time by remember { mutableStateOf("00:00:00") }
    var timerRunning by remember { mutableStateOf(false) }
    var formattedIterations by remember { mutableStateOf(context.getString(R.string.iterations_zero_hz)) }
    var formattedIterationsCount by remember { mutableStateOf(context.getString(R.string.iterations_zero_hz)) }
    var intention by remember { mutableStateOf( "") }
    val focusManager = LocalFocusManager.current
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    val fiftyPercentOfFreeMemory = remember {
        Runtime.getRuntime().let { it.maxMemory() - (it.totalMemory() - it.freeMemory()) } * 0.5
    }
    val intentions = intentionViewModel.tabs

    val scrollState = rememberScrollState()
//    var multiplier by remember { mutableStateOf(0L) }
    var isIntentionProcessed by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    // Initialize `isKeepAwakeEnabled` with a default value, saving its state across recompositions
    var isKeepAwakeEnabled by rememberSaveable { mutableStateOf(false) }
    var selectedTabId by remember { mutableStateOf(intentions.first().id) }
    var selectedTab =intentions.first { it.id==selectedTabId }

    intention=selectedTab.intention;
    sliderPosition=selectedTab.multiplier.toFloat();
    isKeepAwakeEnabled=selectedTab.awakeDevice;
    selectedFrequency=selectedTab.frequency;
    isBoostEnabled=selectedTab.boostPower;
    timerRunning=selectedTab.timerRunning;


    val loadingText=context.getString(R.string.loading_intention)
    val zeroIteration=context.getString(R.string.iterations_zero_hz)

    val resultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                val hashedValue = hashFileContent(context, it)
                intention += hashedValue // Append the hash to the intention text box
                selectedTab.intention=intention;
                intentionViewModel.updateIntention(selectedTab);
            }
        }
    )


    Log.d("TESTH","Composer recreated");
    val audioRecordLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            Log.d("TESTH","In reult");
            if (result.resultCode == Activity.RESULT_OK) {
                Log.d("TESTH","Result ok");
                val savedAudioHash = sharedPref.getString("audioHash", "").toString()
                Log.d("TESTH",savedAudioHash);
                intention += savedAudioHash

                selectedTab.intention=intention;
                intentionViewModel.updateIntention(selectedTab);
            }
        }
    )

    val handleInsertFileClick = {
        resultLauncher.launch(arrayOf("*/*")) // Allow any file type
    }

    val onRecordVoiceIntentionClicked={
        val intent = Intent(context, RecordVoiceIntentionActivity::class.java)
        audioRecordLauncher.launch(intent)
    }

    val onAddClick={
        val id= intentionViewModel.addIntention(Intention(
            title = "",
            intention = "",
            multiplier = 0.0,
            frequency = "1",
            awakeDevice = false,
            boostPower = true,
            timerStartedAt = 0,
            iterationCompleted = 0.0,
            iterationCount = "",
            timerRunning = false,
            isNotification = false,
            targetLength = 0
        ))


        selectedTabId=id.toInt()
        selectedTab=intentions.first{it.id==selectedTabId}
        intention=selectedTab.intention;
        sliderPosition=selectedTab.multiplier.toFloat();
        isKeepAwakeEnabled=selectedTab.awakeDevice;
        selectedFrequency=selectedTab.frequency;
        isBoostEnabled=selectedTab.boostPower;
        timerRunning=selectedTab.timerRunning;
        targetLength=selectedTab.targetLength;

        formattedIterations=context.getString(R.string.iterations_zero_hz)
        time = "00:00:00"

        if(intentionViewModel.getStartIntentions().isNotEmpty()){
            val intent = Intent(context, TimerForegroundService::class.java)
            intent.putExtra("selectedTabId",selectedTabId);
            context.startService(intent)
        }
    }

    val onDeleteClick={
        intentionViewModel.deleteIntention(selectedTabId)
        selectedTabId=intentions.first().id
        selectedTab=intentions.first()

        intention=selectedTab.intention;
        sliderPosition=selectedTab.multiplier.toFloat();
        isKeepAwakeEnabled=selectedTab.awakeDevice;
        selectedFrequency=selectedTab.frequency;
        isBoostEnabled=selectedTab.boostPower;
        timerRunning=selectedTab.timerRunning;
        targetLength=selectedTab.targetLength;

        formattedIterations=context.getString(R.string.iterations_zero_hz)
        time = "00:00:00"

        if(intentionViewModel.getStartIntentions().isNotEmpty()){
            val intent = Intent(context, TimerForegroundService::class.java)
            intent.putExtra("selectedTabId",selectedTabId);
            context.startService(intent)
        }
    }

    val onNotificationClick={
        intentionViewModel.changeNotificationIntention(selectedTabId)
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
            tabList = intentions,
            selectedFrequency = selectedFrequency,
            intention = intention,
            onIntentionChange = {
                intention = it
                selectedTab.intention=intention;
                intentionViewModel.updateIntention(selectedTab);
                                },
            timerRunning = timerRunning,
            isBoostEnabled = isBoostEnabled,
            onBoostChange = {
                isBoostEnabled = it
                selectedTab.boostPower=isBoostEnabled;
                intentionViewModel.updateIntention(selectedTab);
                            },
            onFrequencyChange = { newFrequency ->
                selectedFrequency = newFrequency
                selectedTab.frequency=selectedFrequency;
                intentionViewModel.updateIntention(selectedTab);
            },
            sliderPosition = sliderPosition,
            onSliderPositionChange = { newValue ->
                sliderPosition = newValue.roundToLong().toFloat()
//                sharedPref.edit().putFloat("sliderPosition", sliderPosition).apply()
                selectedTab.multiplier=sliderPosition.toDouble();
                intentionViewModel.updateIntention(selectedTab);
            },
            time = time,
            formattedIterations = formattedIterations,
            buttonText = if (timerRunning) stringResource(R.string.str_stop) else stringResource(R.string.str_start),
            onStartStopButtonClick = {
                focusManager.clearFocus()

                Log.d("TERS","Start stop clicked");
                if (selectedTab.timerRunning) {
                    timerRunning=false;

                    selectedTab.timerRunning=false;
                    selectedTab.timerStartedAt=0;
                    selectedTab.iterationCompleted=0.0;
                    selectedTab.targetLength=0;
                    selectedTab.iterationCount="";
                    intentionViewModel.updateIntention(selectedTab);
                    Log.d("TERS","Timer stopped "+selectedTab);

                    if(intentionViewModel.getStartIntentions().isEmpty()){
                        val intent = Intent(context, TimerForegroundService::class.java)
                        context.stopService(intent)
                    }

                    formattedIterations = context.getString(R.string.finished,formattedIterationsCount)
                } else {
                    formattedIterations = loadingText
                    time = "00:00:00"
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
//                    sharedPref.edit().putString("intention", intention).apply()
//                    sharedPref.edit().putString("frequency", selectedFrequency).apply()
//                    sharedPref.edit().putBoolean("boost_enabled", isBoostEnabled).apply()
//                    sharedPref.edit().putBoolean("keep_awake_enabled", isKeepAwakeEnabled).apply() // Ensure it saves state properly

                    selectedTab.boostPower=isBoostEnabled;
                    selectedTab.awakeDevice=isKeepAwakeEnabled;
                    selectedTab.timerRunning=true;
                    selectedTab.targetLength=targetLength;
                    selectedTab.timerStartedAt=System.nanoTime()
                    selectedTab.lastSecond=System.nanoTime()
                    selectedTab.targetLength=targetLength;

                    if(intentionViewModel.getStartIntentions().isEmpty()){
                        selectedTab.isNotification=true;
                        intentionViewModel.setNotificationIntention(selectedTab.id);
                    }

                    intentionViewModel.updateIntention(selectedTab);

                    timerRunning=true;
                    isIntentionProcessed = true

                    Log.d("TERS","Timer started "+selectedTab.timerRunning);

                    val intent = Intent(context, TimerForegroundService::class.java)
                    intent.putExtra("isInit",true);
                    intent.putExtra("selectedTabId",selectedTabId);
                    context.startService(intent)
                }
            },
            onResetButtonClick = {
                focusManager.clearFocus()
                formattedIterations = zeroIteration
                time = "00:00:00"
                selectedTab.timerStartedAt=0;
                selectedTab.timerRunning=false;
                intentionViewModel.updateIntention(selectedTab);
            },
            onInsertFileClick = handleInsertFileClick, // Pass the file selection logic
            onRecordVoiceIntentionClicked= onRecordVoiceIntentionClicked,
            onAddClick = onAddClick,
            onDeleteClick = onDeleteClick,
            onNotificationClick = onNotificationClick,
            scrollState = scrollState,
            expanded = expanded,
            onExpandChange = {
                expanded = !expanded
            },
            isKeepAwakeEnabled = isKeepAwakeEnabled,
            onKeepAwakeChange = { newValue ->
                isKeepAwakeEnabled = newValue
//                sharedPref.edit().putBoolean("keep_awake_enabled", newValue).apply()
                selectedTab.awakeDevice=newValue;
                intentionViewModel.updateIntention(selectedTab);
            },
            selectedTabId = selectedTabId,
            onTabSelected = {
                selectedTabId=it
                selectedTab=intentions.first{it.id==selectedTabId}
                intention=selectedTab.intention;
                sliderPosition=selectedTab.multiplier.toFloat();
                isKeepAwakeEnabled=selectedTab.awakeDevice;
                selectedFrequency=selectedTab.frequency;
                isBoostEnabled=selectedTab.boostPower;
                timerRunning=selectedTab.timerRunning;
                targetLength=selectedTab.targetLength;

                formattedIterations=context.getString(R.string.iterations_zero_hz)
                time = "00:00:00"

                if(intentionViewModel.getStartIntentions().isNotEmpty()){
                    val intent = Intent(context, TimerForegroundService::class.java)
                    intent.putExtra("selectedTabId",selectedTabId);
                    context.startService(intent)
                }
            }
        )
    }

    val mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            // Get extra data included in the Intent

            if(intent.hasExtra("stopTimer")){
                timerRunning=false;
//                val intents = Intent(context, TimerForegroundService::class.java)
//                context!!.stopService(intents)

                selectedTab.timerRunning=false;
                selectedTab.timerStartedAt=0;
                selectedTab.iterationCount="";
                selectedTab.iterationCompleted=0.0;
                intentionViewModel.updateIntention(selectedTab);

                formattedIterations = context!!.getString(R.string.finished,formattedIterationsCount)

                intentionViewModel.loadTabs();
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
        val buffer = ByteArray(8192) // 8KB buffer
        val digest = SHA3.Digest512()
        var bytesRead: Int

        inputStream?.let { stream ->
            while (stream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }

        digest.digest().joinToString("") { "%02x".format(it) }.uppercase()
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    } finally {
        inputStream?.close()
    }
}


@Composable
fun SimpleTabRow(
    tabs: List<Intention>,
    selectedTabId: Int,
    onTabSelected: (Int) -> Unit
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = {
                coroutineScope.launch {
                    val targetIndex = (listState.firstVisibleItemIndex - 1).coerceAtLeast(0)
                    listState.animateScrollToItem(targetIndex)
                }
            },
            modifier = Modifier.size(18.dp),
            enabled = listState.canScrollBackward
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Scroll Left", tint = Color.White)
        }

        LazyRow(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
            state = listState
        ) {
            itemsIndexed(tabs) { index, tab ->
                val isSelected = tab.id == selectedTabId
                val backgroundColor = if (isSelected) Color(0xFFD0E8FF) else Color.White
                val textColor = if (isSelected) Color.Black else Color.DarkGray


                Card(
                    modifier = Modifier
                        .width(60.dp)
                        .padding(2.dp)
                        .clickable { onTabSelected(tab.id) },
                    colors = CardDefaults.cardColors(containerColor = backgroundColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = (index+1).toString(),
                            fontSize = 12.sp,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (tab.isNotification) {
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Selected Tab",
                                tint = Color.Black,
                                modifier = Modifier.size(12.dp)
                            )
                        }

                        if (tab.timerRunning) {
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.FiberManualRecord,
                                contentDescription = "Timer Running",
                                tint = Color(0xFF4CAF50), // Green color
                                modifier = Modifier.size(8.dp)
                            )
                        }
                    }
                }
            }
        }

        IconButton(
            onClick = {
                coroutineScope.launch {
                    val targetIndex = (listState.firstVisibleItemIndex + 1).coerceAtMost(tabs.lastIndex)
                    listState.animateScrollToItem(targetIndex)
                }
            },
            modifier = Modifier.size(18.dp),
            enabled = listState.canScrollForward
        ) {
            Icon(Icons.Default.ArrowForward, contentDescription = "Scroll Right", tint = Color.White)
        }
    }
}


@Composable
    private fun MainContent(
    tabList: List<Intention>,
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
    onRecordVoiceIntentionClicked: ()->Unit,
    onAddClick: ()->Unit,
    onDeleteClick: () -> Unit,
    onNotificationClick: () -> Unit,
    scrollState: ScrollState,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    isKeepAwakeEnabled: Boolean, // Add parameter for keep awake state
    onKeepAwakeChange: (Boolean) -> Unit, // Callback for changing the keep awake state
    selectedTabId : Int,
    onTabSelected: (Int) -> Unit
) {
    val context = LocalContext.current // Correctly obtain the context in Compose
    val selectedTab=tabList.first { it.id == selectedTabId };

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppTitle()
        SimpleTabRow(
            tabs = tabList,
            selectedTabId = selectedTabId,
            onTabSelected = onTabSelected
        )
        IntentionTextField(
            intention = intention,
            onIntentionChange = onIntentionChange,
            timerRunning = timerRunning
        )
        FileVoiceSelector(timerRunning=timerRunning,onInsertFileClick = onInsertFileClick, onRecordVoiceIntentionClicked = onRecordVoiceIntentionClicked, onAddClick = onAddClick,onDeleteClick=onDeleteClick,onNotificationClick=onNotificationClick,selectedTab=selectedTab)
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
        readOnly = timerRunning,
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
private fun FileVoiceSelector(
    timerRunning: Boolean,
    // Add onClick handlers for the new icons
    // Keep existing handlers
    onInsertFileClick: () -> Unit,
    onRecordVoiceIntentionClicked: () -> Unit,
    onAddClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onNotificationClick: () -> Unit,
    selectedTab:Intention
) {
    val viewModel: IntentionViewModel = viewModel();

    Row(
        modifier = Modifier.fillMaxWidth(),
        // Remove Arrangement.End, Spacer will handle positioning
        // Add verticalAlignment for consistency
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- Left Icons ---
        IconButton(
            onClick = onAddClick,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = "Add",
                // Apply tint logic consistently
                tint = Color.White
            )
        }
        if(viewModel.tabs.size>1){
            IconButton(
                onClick = onDeleteClick,
                enabled = !timerRunning,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint = if (!timerRunning) Color.White else Color.Gray
                )
            }
        }

        if(!selectedTab.isNotification){
            IconButton(
                onClick = onNotificationClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Filled.Notifications,
                    contentDescription = "Notifications",
                    tint = Color.White
                )
            }
        }

        // --- Spacer to push next items to the end ---
        Spacer(modifier = Modifier.weight(1f))

        // --- Right Icons (Existing) ---
        IconButton(
            onClick = onInsertFileClick,
            enabled = !timerRunning,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                Icons.Filled.UploadFile,
                contentDescription = "Load File",
                tint = if (!timerRunning) Color.White else Color.Gray
            )
        }
        IconButton(
            onClick = onRecordVoiceIntentionClicked,
            enabled = !timerRunning,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                Icons.Filled.Mic,
                contentDescription = "Record Voice", // Updated content description slightly
                tint = if (!timerRunning) Color.White else Color.Gray
            )
        }
    }
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
        Option(stringResource(R.string.str_maximum_frequency),"0"),
        Option(stringResource(R.string.once_per_hour),"1")
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
                value = if(selectedFrequency=="3") options[0].title else if(selectedFrequency=="7.83") options[1].title else if(selectedFrequency=="0") options[2].title else options[3].title,
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
    val isCheckboxEnabled = (selectedFrequency == "3" || selectedFrequency == "7.83" || selectedFrequency == "1") && !timerRunning

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
        textAlign = TextAlign.Center,
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
//    Row(
//        modifier = Modifier.fillMaxWidth(),
//        horizontalArrangement = Arrangement.Center
//    ) {
//        IconButton(
//            onClick = onInsertFileClick,
//            modifier = Modifier.size(52.dp)
//        ) {
//            Icon(Icons.Filled.UploadFile,  contentDescription = "Load File", tint = Color.White)
//        }
//        IconButton(
//            onClick = onRecordVoiceIntentionClicked,
//            modifier = Modifier.size(52.dp)
//        ) {
//            Icon(Icons.Filled.Mic,  contentDescription = "Load File", tint = Color.White)
//        }
//    }
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
    private lateinit var  dbHelper: IntentionDBHelper

    override fun onCreate() {
        super.onCreate()
        dbHelper=IntentionDBHelper(applicationContext)
        sharedPreferences=getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val savedLanguage = sharedPreferences.getString("Language", "en") ?: "en"
        context=this;

//        val filter = IntentFilter("com.example.SEND_TO_SERVICE")
//        ContextCompat.registerReceiver(
//            context,
//            dataReceiver,
//            filter,
//            ContextCompat.RECEIVER_NOT_EXPORTED
//        )

        setLocale(context, savedLanguage)
    }


    private val dataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val data = intent?.getStringExtra("message_key")
            Log.d("MyService", "Received via Broadcast: $data")
        }
    }

    fun updateLocale(locale: Locale) {
        Locale.setDefault(locale)
        val config = context.resources.configuration
        config.setLocale(locale)
        config.setLayoutDirection(locale) // Important for RTL languages
        context= context.createConfigurationContext(config)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }

    private var intentionList = mutableListOf<Intention>();

    private var timerRunning = false
    private var durationSec=0L
    private var selectedTabId:Int=-1;
    private val listLock = Any()


    @SuppressLint("WakelockTimeout")
    @OptIn(DelicateCoroutinesApi::class)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)

        Log.d("TESTR","IN onstart commm");
        // Use safe call operator and provide default values
        intent?.let { safeIntent ->

                selectedTabId=safeIntent.getIntExtra("selectedTabId",-1);
                val notification = createNotification(context.getString(R.string.intention_repeater_header)+" 00:00:00", context.getString(R.string.loading_intention),true)
                startForeground(NOTIFICATION_ID, notification)

                // Acquire a partial wake lock
                val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "TimerForegroundService::WakeLock"
                )

                durationSec=sharedPreferences.getLong("Duration",0L) * 1000

            synchronized(listLock) {
                intentionList.removeAll { !it.timerRunning }

                val fetchedIntentions = dbHelper.getStartIntentions()
                val existingIds = intentionList.map { it.id }.toSet()
                val newIntentions = fetchedIntentions.filter { it.id !in existingIds }

                for (item in newIntentions) {
                    val intentionBuilder = StringBuilder()
                    var localMultiplier = 0L

                    if (item.targetLength > 0) {
                        while (intentionBuilder.length < item.targetLength) {
                            intentionBuilder.append(item.intention)
                            localMultiplier++
                        }
                    } else {
                        localMultiplier = 1
                        intentionBuilder.append(item.intention)
                    }

                    item.newIntention = intentionBuilder.toString()
                    item.newMultiplier = localMultiplier
                }

                intentionList.addAll(newIntentions)
            }

//                intentionList.removeAll { it.timerRunning == false }
//
//                val fetchedIntentions = dbHelper.getStartIntentions()
//
//                val existingIds = intentionList.map { it.id }.toSet()
//                val newIntentions = fetchedIntentions.filter { it.id !in existingIds }
//
//                for (item in newIntentions) {
//                    val intentionBuilder = StringBuilder()
//                    var localMultiplier = 0L
//
//                    if (item.targetLength > 0) {
//                        while (intentionBuilder.length < item.targetLength) {
//                            intentionBuilder.append(item.intention)
//                            localMultiplier++
//                        }
//                    } else {
//                        localMultiplier = 1
//                        intentionBuilder.append(item.intention)
//                    }
//
//                    item.newIntention = intentionBuilder.toString()
//                    item.newMultiplier = localMultiplier
//                }
//
//                intentionList.addAll(newIntentions)

//                intentionList=dbHelper.getStartIntentions().toMutableStateList();
//
//                for (item in intentionList) {
//                    val intentionBuilder = StringBuilder()
//                    var localMultiplier = 0L
//
//                    if (item.targetLength > 0) {
//                        while (intentionBuilder.length < item.targetLength) {
//                            intentionBuilder.append(item.intention)
//                            localMultiplier++
//                        }
//                    } else {
//                        localMultiplier = 1
//                        intentionBuilder.append(item.intention)
//                    }
//
//                    item.newIntention = intentionBuilder.toString()
//                    item.newMultiplier = localMultiplier
//                }


                if(isWakeEnabled()){
                    wakeLock.acquire();
                }else{
                    wakeLock.acquire(10 * 60 * 1000L /* 10 minutes */)
                }

                val intentUpdate = Intent("IterationUpdate")

                timerRunning=dbHelper.getStartIntentions().isNotEmpty();
                Log.d("TERS","Timer in command "+timerRunning);
                if (dbHelper.getStartIntentions().size==1 && safeIntent.hasExtra("isInit")) {
                    Log.d("TERR","Started intention");
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

    private fun isWakeEnabled(): Boolean {
        return dbHelper.getStartIntentions().any { it.awakeDevice }
    }

//    suspend fun startTimer(
//        onTimeUpdate: (String) -> Unit,
//        onIterationsUpdate: (String, String) -> Unit,
//        onTimerStop: (Boolean) -> Unit
//    ) {
//        Log.d("TESTF","Timer start");
//        while (timerRunning) {
//            val savedLanguage = sharedPreferences.getString("Language", "en") ?: "en"
//            if (savedLanguage != context.resources.configuration.locale.toString()) {
//                updateLocale(Locale(savedLanguage))
//            }
//
//            val now = System.nanoTime()
//
//            val snapshotList = synchronized(listLock) { intentionList.toList() }
//            for (intention in snapshotList) {
//                Log.d("TESTF","Intention loop");
//                Log.d("TESTF",intention.intention);
//                if (!intention.timerRunning) continue
//
//                val loopStart = System.nanoTime()
//
//                if (intention.boostPower) {
//                    intention.mutableIntention =
//                        sha512("${intention.mutableIntention}: ${intention.newIntention}")
//                }
//
//                val freq = intention.frequency.toDoubleOrNull() ?: continue
//                val shouldDelay = freq in listOf(1.0, 3.0, 7.83)
//
//                if (!shouldDelay) {
//                    intention.iterationsInLastSecond++
//                } else {
//                    val timeTakenNs = System.nanoTime() - loopStart
//                    val delayMs = (1.0 / freq) * 1000.0
//                    val remainingDelayMs = delayMs - (timeTakenNs / 1_000_000.0)
//                    preciseDelay(remainingDelayMs)
//                }
//
//                if (intention.frequency == "1" && !intention.isFirstIterationSet) {
//                    intention.iterationsInLastSecond = 1.0
//                    intention.iterations += 1.0 * intention.multiplier
//                    intention.isFirstIterationSet = true
//                }
//
//                if (now - intention.lastSecond >= 1_000_000_000L) {
//                    val ss=dbHelper.getIntentionById(intention.id);
//                    intention.timerRunning=ss.timerRunning;
//                    if (!intention.timerRunning) continue
//
//                    Log.d("TESTF","Inside one second loop");
//
//                    intention.lastSecond=now;
//
//                    val freq = intention.frequency.toDoubleOrNull() ?: continue
//
//                    when (freq) {
//                        3.0 -> intention.iterationsInLastSecond = 3.0
//                        7.83 -> intention.iterationsInLastSecond = 7.83
//                        1.0 -> {
//                            val elapsedSec =
//                                (System.nanoTime() - intention.timerStartedAt) / 1_000_000_000L
//                            intention.iterationsInLastSecond =
//                                if ((elapsedSec % 3600).toInt() == 0) 1.0 else 0.0
//                        }
//                    }
//
//                    intention.elapsedTime =
//                        (System.nanoTime() - intention.timerStartedAt) / 1_000_000L
//                    intention.iterations += intention.iterationsInLastSecond * intention.multiplier
//
//                    val hours = intention.elapsedTime / 3600000
//                    val minutes = (intention.elapsedTime / 60000) % 60
//                    val seconds = (intention.elapsedTime / 1000) % 60
//
//                    val updatedTime =
//                        String.format(Locale.ENGLISH, "%02d:%02d:%02d", hours, minutes, seconds)
//                    intention.lastTime = updatedTime
//
//                    Log.d("TESTF",updatedTime);
//
//                    val iterationString = context.getString(
//                        R.string.str_iterations,
//                        formatLargeNumber(
//                            context,
//                            BigInteger.valueOf(intention.iterations.toLong())
//                        ),
//                        when (freq) {
//                            7.83 -> formatDecimalNumber(
//                                context,
//                                7.83f * intention.multiplier.toFloat()
//                            )
//
//                            3.0 -> formatLargeFreq(context, 3f * intention.multiplier.toFloat())
//                            1.0 -> formatLargeNumber(
//                                context,
//                                BigInteger.valueOf((1f * intention.multiplier.toFloat()).toLong())
//                            ) + "/hr"
//
//                            else -> formatLargeFreq(
//                                context,
//                                (intention.iterationsInLastSecond * intention.multiplier).toFloat()
//                            )
//                        }
//                    )
//
//                    Log.d("TESTF",iterationString);
//
//                    intention.updatedIterationCount = formatLargeNumber(
//                        context,
//                        BigInteger.valueOf(intention.iterations.toLong())
//                    )
//
//                    // Stop condition check
//                    if ( durationSec>0&&intention.elapsedTime-1000>=durationSec) {
//                        intention.timerRunning = false
//                        withContext(Dispatchers.Main) {
//                            onTimerStop(true)
//                        }
//                        continue
//                    }
//
//                    dbHelper.updateIntention(intention);
//                    Log.d("TESTF","Updated intentions");
//                    withContext(Dispatchers.Main) {
//                        Log.d("TESTF","Send updated");
//
//                        if(selectedTabId==intention.id && intention.timerRunning){
//                            onTimeUpdate(updatedTime)
//                            onIterationsUpdate(iterationString, intention.updatedIterationCount)
//                        }
//
//                        if (intention.isNotification && intention.timerRunning) {
//                            val notification = createNotification(
//                                context.getString(R.string.intention_repeater_header) + " " + updatedTime,
//                                iterationString,
//                                true
//                            )
//                            val notificationManager =
//                                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
//                            notificationManager.notify(intention.id, notification)
//                        }
//                    }
//
//                    // Reset for next second
//                    intention.iterationsInLastSecond = 0.0
//                    intention.lastSecond = now
//
//                    timerRunning=dbHelper.getStartIntentions().isNotEmpty();
//                    Log.d("TERS","Timer running "+timerRunning);
//                    Log.d("TERS",dbHelper.getStartIntentions().toString());
//                }
//            }
//        }
//    }



    suspend fun startTimer(
        onTimeUpdate: (String) -> Unit,
        onIterationsUpdate: (String, String) -> Unit,
        onTimerStop: (Boolean) -> Unit
    ) {
        Log.d("TIMER", "Starting Timer")

        var lastTickTime = System.nanoTime()

        while (timerRunning) {
            val currentTime = System.nanoTime()
            val deltaNs = currentTime - lastTickTime

            // Ensure roughly 1s loop accuracy
            if (deltaNs < 1_000_000_000L) {
                preciseDelaySafe((1_000_000_000.0 - deltaNs) / 1_000_000.0)
                continue
            }
            lastTickTime = currentTime

            val savedLanguage = sharedPreferences.getString("Language", "en") ?: "en"
            if (savedLanguage != context.resources.configuration.locale.toString()) {
                updateLocale(Locale(savedLanguage))
            }

            val snapshotList = synchronized(listLock) { intentionList.toList() }

            for (intention in snapshotList) {

                val ss=dbHelper.getIntentionById(intention.id);
                intention.timerRunning=ss.timerRunning;
                intention.isNotification = ss.isNotification

                if(!intention.timerRunning && intention.isNotification){
                    val ll=dbHelper.getStartIntentions();
                    if(ll.isNotEmpty()){
                        dbHelper.setNotificationIntention(ll.first().id)
                    }
                }

                if (!intention.timerRunning) continue

                if (intention.boostPower) {
                    intention.mutableIntention = sha512("${intention.mutableIntention}: ${intention.newIntention}")
                }

                val freq = intention.frequency.toDoubleOrNull() ?: continue

                // One-time setup for 1/hr intentions
//                if (freq == 1.0 && !intention.isFirstIterationSet) {
//                    intention.iterationsInLastSecond = 1.0
//                    intention.iterations += 1.0 * intention.multiplier
//                    intention.isFirstIterationSet = true
//                }

                val timeNow = System.nanoTime()
//                val elapsedSec = (timeNow - intention.timerStartedAt) / 1_000_000_000L

                // Frequency-based updates
                when (freq) {
                    3.0, 7.83 -> intention.iterationsInLastSecond = freq
                    1.0 -> {
                        val elapsedSec = (System.nanoTime() - intention.timerStartedAt) / 1_000_000_000L
                        val currentHour = elapsedSec / 3600

                        if (!intention.isFirstIterationSet) {
                            intention.iterationsInLastSecond = 1.0
                            intention.isFirstIterationSet = true
                            intention.lastHourMark = currentHour
                        } else if (currentHour > intention.lastHourMark) {
                            intention.iterationsInLastSecond = 1.0
                            intention.lastHourMark = currentHour
                        } else {
                            // NO increment this tick
                            intention.iterationsInLastSecond = 0.0
                        }
                    }
                    else -> intention.iterationsInLastSecond++
                }

                intention.elapsedTime = (timeNow - intention.timerStartedAt) / 1_000_000L
                intention.iterations += intention.iterationsInLastSecond * intention.multiplier

                val hours = intention.elapsedTime / 3600000
                val minutes = (intention.elapsedTime / 60000) % 60
                val seconds = (intention.elapsedTime / 1000) % 60

                val updatedTime = String.format(Locale.ENGLISH, "%02d:%02d:%02d", hours, minutes, seconds)
                intention.lastTime = updatedTime

                val iterationString = context.getString(
                    R.string.str_iterations,
                    formatLargeNumber(context, BigInteger.valueOf(intention.iterations.toLong())),
                    when (freq) {
                        7.83 -> formatDecimalNumber(context, 7.83f * intention.multiplier.toFloat())
                        3.0 -> formatLargeFreq(context, 3f * intention.multiplier.toFloat())
                        1.0 -> formatLargeNumber(context, BigInteger.valueOf((1f * intention.multiplier).toLong())) + "/hr"
                        else -> formatLargeFreq(context, (intention.iterationsInLastSecond * intention.multiplier).toFloat())
                    }
                )

                intention.updatedIterationCount = formatLargeNumber(
                    context,
                    BigInteger.valueOf(intention.iterations.toLong())
                )

                // Check for stop condition
                if (durationSec > 0 && intention.elapsedTime >= durationSec) {
                    intention.timerRunning = false
                    withContext(Dispatchers.Main) {
                        onTimerStop(true)
                    }
                    continue
                }

                // DB Update
                withContext(Dispatchers.IO) {
                    dbHelper.updateIntention(intention)
                }

                // UI Update
                withContext(Dispatchers.Main) {
                    if (selectedTabId == intention.id && intention.timerRunning) {
                        onTimeUpdate(updatedTime)
                        onIterationsUpdate(iterationString, intention.updatedIterationCount)
                    }

                    if (intention.isNotification && intention.timerRunning) {
                        val notification = createNotification(
                            context.getString(R.string.intention_repeater_header) + " " + updatedTime,
                            iterationString,
                            true
                        )
                        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.notify(NOTIFICATION_ID, notification)
                    }
                }

                intention.iterationsInLastSecond = 0.0
            }

            // Refresh timerRunning check
            timerRunning = withContext(Dispatchers.IO) { dbHelper.getStartIntentions().isNotEmpty() }
        }
    }


    suspend fun preciseDelaySafe(milliseconds: Double) {
        val wholeMilliseconds = milliseconds.toLong()
        val fractionalMilliseconds = milliseconds - wholeMilliseconds
        val nanoseconds = (fractionalMilliseconds * 1_000_000).toInt().coerceIn(0, 999999)

        if (wholeMilliseconds > 0) {
            delay(wholeMilliseconds)
        }
        if (nanoseconds > 0) {
            delay((nanoseconds / 1_000_000).toLong())
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
        if (::wakeLock.isInitialized && wakeLock.isHeld) {
            wakeLock.release()
        }
        timerRunning=false
        stopForeground(true)
        stopSelf()
//        val notification=createNotification(context.getString(R.string.intention_repeater_finished),context.getString(R.string.str_iterations,updatedIterationCount,lastTime),false)
//
//        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//
//        notificationManager.notify(100,notification)
    }

    private fun createNotification(title:String,text:String,isSticky:Boolean): Notification {
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
            .setOngoing(isSticky)
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