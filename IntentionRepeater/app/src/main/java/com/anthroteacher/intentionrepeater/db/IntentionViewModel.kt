import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.AndroidViewModel
import com.anthroteacher.intentionrepeater.db.IntentionDBHelper
import Intention
import android.util.Log

class IntentionViewModel(application: Application) : AndroidViewModel(application) {
    private val dbHelper = IntentionDBHelper(application)

    private val _tabs = mutableStateListOf<Intention>()
    val tabs: SnapshotStateList<Intention> get() = _tabs

    init {
        ensureDefaultIntentionExists()
        loadTabs()
    }

    private fun ensureDefaultIntentionExists() {
        if (dbHelper.getAllIntentions().isEmpty()) {
            dbHelper.insertIntention(
                Intention(
                    title = "",
                    intention = "",
                    multiplier = 1.1,
                    frequency = "1",
                    awakeDevice = false,
                    boostPower = true,
                    timerStartedAt = 0L,
                    iterationCompleted = 0.0,
                    iterationCount = "",
                    timerRunning = false,
                    isNotification = true,
                    targetLength = 0
                )
            )
        } else {
            dbHelper.ensureNotificationExists() // optional safety check
        }
    }

    fun loadTabs() {
        val allIntentions = dbHelper.getAllIntentions()
        _tabs.clear()
        _tabs.addAll(allIntentions.map { it })
    }

    fun addIntention(intention: Intention):Long {
        val id = dbHelper.insertIntention(intention)
        loadTabs()
        return id;
    }

    fun updateIntention(intention: Intention) {
        dbHelper.updateIntention(intention)
        Log.d("TERS",dbHelper.getStartIntentions().toString())
        loadTabs()
    }

    fun deleteIntention(id: Int) {
        dbHelper.deleteIntention(id)
        ensureDefaultIntentionExists()
        loadTabs()
    }

    fun getNotificationIntention(): Intention? {
        return dbHelper.getNotificationEnabledIntention()
    }

    fun changeNotificationIntention(id: Int) {
        dbHelper.setNotificationIntention(id)
        loadTabs()
    }

    fun stopAllIntentions() {
        dbHelper.stopAllIntention()
    }

    fun setNotificationIntention(id: Int){
        dbHelper.setNotificationIntention(id);
        loadTabs();
    }

    fun getStartIntentions(): List<Intention> {
        return dbHelper.getStartIntentions()
    }
}
