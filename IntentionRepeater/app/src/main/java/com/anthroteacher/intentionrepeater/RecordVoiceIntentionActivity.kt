package com.anthroteacher.intentionrepeater

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract.Colors
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.anthroteacher.intentionrepeater.ui.theme.BlueLight
import com.anthroteacher.intentionrepeater.ui.theme.DarkColorScheme
import com.anthroteacher.intentionrepeater.ui.theme.FireRed
import com.anthroteacher.intentionrepeater.ui.theme.IntentionRepeaterTheme
import com.anthroteacher.intentionrepeater.ui.theme.LightColorScheme
import com.anthroteacher.intentionrepeater.ui.theme.SafetyOrange
import kotlinx.coroutines.delay
import org.bouncycastle.jcajce.provider.digest.SHA3
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.security.Security
import java.util.Locale


class RecordVoiceIntentionActivity : ComponentActivity() {
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    var outputFile: String = ""

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        outputFile = "${externalCacheDir?.absolutePath}/recorded_audio.3gp"
        Security.addProvider(BouncyCastleProvider())

        loadLocale()

        setContent {
            IntentionRepeaterTheme {
                RecordVoiceScreen(
                    onRecord = { startRecording() },
                    onStop = { stopRecording() },
                    onPlay = { isPaused, onPlaySet, onAudioComplete ->
                        startPlaying(isPaused, onPlaySet, onAudioComplete)
                    },
                    onPause = { pausePlaying() },
                    onStopPlayback = { stopPlayback() },
                    onSave = { saveHashAndClose() },
                    onCancel = {
                        finish()
                    } // Finish activity on cancel
                )
            }
        }
    }


    private fun loadLocale() {
        val savedLanguage = sharedPreferences.getString("Language", "en") ?: "en"
        setLocale(this, savedLanguage) // Apply the saved or default locale
    }

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

    private fun startRecording() {
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44100)
            setOutputFile(outputFile)
            try {
                prepare()
                start()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
    }

    private fun startPlaying(isPaused:Boolean,onPlaySet:() -> Unit ,onAudioComplete: () -> Unit) {

        if(isPaused){
            mediaPlayer!!.seekTo(mediaPlayer!!.currentPosition);
            mediaPlayer!!.start();
            onPlaySet()
        }else{
            mediaPlayer = MediaPlayer().apply {
                setOnCompletionListener {
                    onAudioComplete()
                }
                try {
                    setDataSource(outputFile)
                    prepare()
                    start()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

    }

    private fun pausePlaying() {
        mediaPlayer?.apply {
            if (isPlaying) {
                pause()
            }
        }
    }

    private fun stopPlayback() {
        mediaPlayer?.apply {
            stop()
            reset()
            release()
        }
        mediaPlayer = null
    }

    fun hashFile(filePath: String): String {
        val file = File(filePath)
        val digest = SHA3.Digest512()

        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }

        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun saveHashAndClose() {
        val hashValue = hashFile(outputFile)
        sharedPreferences.edit().putString("audioHash", hashValue).apply()

        val file=File(outputFile);
        file.deleteOnExit();
        setResult(RESULT_OK)
        finish()
    }
}

@Composable
fun RecordVoiceScreen(
    onRecord: () -> Unit,
    onStop: () -> Unit,
    onPlay: (isPaused:Boolean,onPlaySet:()->Unit,onAudioComplete: () -> Unit) -> Unit,
    onPause: () -> Unit,
    onStopPlayback: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    var isRecording by remember { mutableStateOf(false) }
    var isRecorded by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var timeInSeconds by remember { mutableStateOf(0) }
    var recordedTimeInSeconds by remember { mutableStateOf(0) }
    var audioPlayTimeInSeconds by remember { mutableStateOf(0) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var hashValue by remember { mutableStateOf("") }

    val context = LocalContext.current
    val activity = context as RecordVoiceIntentionActivity

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                onRecord()
                isRecording = true
                isTimerRunning = true
            } else {
                showPermissionDialog = true
            }
        }
    )

    LaunchedEffect(isTimerRunning) {
        if (isTimerRunning) {
            while (timeInSeconds < 60 && isRecording) {
                delay(1000L)
                timeInSeconds++
            }
            if (timeInSeconds >= 60) {
                onStop()
                isRecording = false
                isTimerRunning = false
                isRecorded = true
                recordedTimeInSeconds=timeInSeconds;
                timeInSeconds = 0
            }
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isPlaying) {
                delay(1000L)
                audioPlayTimeInSeconds++
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.record_voice_intention),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Justify,
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.record_voice_description),
                color = Color.White
            )
        }

        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (isRecording) stringResource(R.string.recording)
                else if (isPlaying) stringResource(R.string.playing)
                else if (isPaused) stringResource(R.string.paused)
                else if (isRecorded) stringResource(R.string.tap_play_listen)
                else stringResource(R.string.tap_mic_record),
                color =  Color.White,
                fontSize = 20.sp,
                fontFamily = FontFamily.Serif,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = if (isRecording) "${timeInSeconds / 60}:${(timeInSeconds % 60).toString().padStart(2, '0')}"
                    else if (isPlaying || isPaused) "${audioPlayTimeInSeconds / 60}:${(audioPlayTimeInSeconds % 60).toString().padStart(2, '0')}"
                    else "${timeInSeconds / 60}:${(timeInSeconds % 60).toString().padStart(2, '0')}",
                    color = if(isRecording && timeInSeconds>=30) FireRed else Color.White,
                    fontSize = 48.sp,
                    fontFamily = FontFamily.Serif,
                )
                if(isRecorded){
                    Text(text ="/${recordedTimeInSeconds / 60}:${(recordedTimeInSeconds % 60).toString().padStart(2, '0')}", color = Color.White, modifier = Modifier.padding(bottom = 10.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!isRecording && !isRecorded) {
                IconButton(
                    onClick = {
                        when {
                            ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED -> {
                                onRecord()
                                isRecording = true
                                isTimerRunning = true
                            }
                            else -> {
                                permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    },
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(Icons.Filled.Mic, modifier = Modifier.size(120.dp), contentDescription = stringResource(
                        R.string.record
                    ), tint = Color.Blue)
                }
            }

            if (isRecording) {
                IconButton(
                    onClick = {
                        onStop()
                        isRecording = false
                        isTimerRunning = false
                        isRecorded = true
                        recordedTimeInSeconds=timeInSeconds;
                        timeInSeconds = 0
                    },
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = stringResource(R.string.stop), tint = Color.Red,modifier = Modifier.size(100.dp))
                }
            }

            if (isRecorded && !isRecording) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (isPlaying) {
                                onPause()
                                isPaused=true;
                                isPlaying = false
                            } else {
                                onPlay(
                                   isPaused, { isPaused = false },
                                    { isPlaying = false; isPaused=false; audioPlayTimeInSeconds = 0 }
                                )
                                isPlaying = true
                            }
                        },
                        modifier = Modifier.size(72.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) stringResource(R.string.pause) else stringResource(R.string.play),
                            tint = Color.White,
                            modifier = Modifier.size(100.dp)
                        )
                    }

                    if (isPlaying) {
                        IconButton(
                            onClick = {
                                onStopPlayback()
                                audioPlayTimeInSeconds = 0
                                isPlaying = false
                                isPaused = false;
                            },
                            modifier = Modifier.size(72.dp)
                        ) {
                            Icon(Icons.Filled.Stop, contentDescription = stringResource(R.string.stop_playback), tint = Color.Red,modifier = Modifier.size(100.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = onSave,
                    colors = ButtonDefaults.buttonColors(
                        contentColor = Color.White,
                        containerColor = Color.Blue
                    ),
                    enabled = !isPlaying,
                    modifier = Modifier
                        .width(220.dp)
                        .height(48.dp)) {
                    Text(stringResource(R.string.save), color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                if(isRecorded){
                    isRecording=false;
                    isRecorded=false;
                    isPlaying=false;
                    isPaused=false;
                    isTimerRunning=false;
                    recordedTimeInSeconds=0;
                    timeInSeconds=0;
                    audioPlayTimeInSeconds=0;
                }else{
                    onCancel();
                } },
                enabled = !isPlaying&&!isRecording,
                colors = ButtonDefaults.buttonColors(
                    contentColor = Color.White,
                    containerColor = if(isRecorded) Color.Red else BlueLight
                ),
                modifier = Modifier
                    .width(220.dp)
                    .height(52.dp)) {
                Text( if(isRecorded) stringResource(R.string.cancel) else stringResource(R.string.back), color = Color.White)
            }
        }


        Spacer(modifier = Modifier.size(0.dp))
    }

    if (showPermissionDialog) {
        AlertDialog(
            containerColor = Color.Black,
            onDismissRequest = { showPermissionDialog = false },
            title = { Text(stringResource(R.string.mic_permission_required), color = Color.White) },
            text = { Text(stringResource(R.string.mic_permission_description), color = Color.White) },
            confirmButton = {
                Button(onClick = {
                    showPermissionDialog = false
                    openAppSettings(context)
                }) {
                    Text(stringResource(R.string.open_settings))
                }
            },
            dismissButton = {
                Button(onClick = { showPermissionDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:${context.packageName}")
    }
    context.startActivity(intent)
}
