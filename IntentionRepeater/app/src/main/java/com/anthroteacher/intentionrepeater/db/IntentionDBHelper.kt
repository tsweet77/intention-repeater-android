package com.anthroteacher.intentionrepeater.db

import Intention
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class IntentionDBHelper(context: Context) :
    SQLiteOpenHelper(context, "intentions.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
                CREATE TABLE intentions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    intention TEXT,
                    multiplier REAL,
                    frequency TEXT,
                    awake_device INTEGER,
                    boost_power INTEGER,
                    timer_started_at INTEGER,
                    iteration_completed REAL,
                    iteration_count TEXT,
                    timer_running INTEGER DEFAULT 0,
                    is_notification INTEGER DEFAULT 0,
                    target_length INTEGER
                )
            """.trimIndent())
        db.execSQL("""
                INSERT INTO intentions (title, intention, multiplier, frequency, awake_device,boost_power, timer_started_at, iteration_completed,iteration_count,timer_running, is_notification,target_length)
            VALUES ('', '', 1, '1',1, 0, 0, 0,'',0, 1,0)
            """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS intentions")
        onCreate(db)
    }

    fun insertIntention(intention: Intention): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("title", intention.title)
            put("intention", intention.intention)
            put("multiplier", intention.multiplier)
            put("frequency", intention.frequency)
            put("awake_device", if (intention.awakeDevice) 1 else 0)
            put("boost_power", if (intention.boostPower) 1 else 0)
            put("timer_started_at", intention.timerStartedAt)
            put("iteration_completed", intention.iterationCompleted)
            put("iteration_count", intention.iterationCount)
            put("timer_running",intention.timerRunning)
            put("is_notification", if (intention.isNotification) 1 else 0)
            put("target_length",intention.targetLength)
        }
        return db.insert("intentions", null, values)
    }

    fun updateIntention(intention: Intention): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("title", intention.title)
            put("intention", intention.intention)
            put("multiplier", intention.multiplier)
            put("frequency", intention.frequency)
            put("awake_device", if (intention.awakeDevice) 1 else 0)
            put("boost_power", if (intention.boostPower) 1 else 0)
            put("timer_started_at", intention.timerStartedAt)
            put("iteration_completed", intention.iterationCompleted)
            put("iteration_count", intention.iterationCount)
            put("timer_running",intention.timerRunning)
            put("is_notification", if (intention.isNotification) 1 else 0)
            put("target_length",intention.targetLength)
        }
        return db.update("intentions", values, "id = ?", arrayOf(intention.id.toString()))
    }

    fun deleteIntention(id: Int): Int {
        val db = writableDatabase
        val deleted = db.delete("intentions", "id = ?", arrayOf(id.toString()))

        val countCursor = db.rawQuery("SELECT COUNT(*) FROM intentions", null)
        countCursor.moveToFirst()
        val count = countCursor.getInt(0)
        countCursor.close()

        if (count == 0) {
            db.execSQL("""
            INSERT INTO intentions (title, intention, multiplier, frequency, awake_device,boost_power, timer_started_at, iteration_completed,iteration_count,timer_running, is_notification,target_length)
            VALUES ('', '', 1, '1',1, 0, 0, 0,'',0, 1,0)
        """.trimIndent())
        }

        return deleted
    }

    fun getAllIntentions(): List<Intention> {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM intentions", null)
        val list = mutableListOf<Intention>()

        if (cursor.moveToFirst()) {
            do {
                list.add(
                    Intention(
                        id = cursor.getInt(0),
                        title = cursor.getString(1),
                        intention = cursor.getString(2),
                        multiplier = cursor.getDouble(3),
                        frequency = cursor.getString(4),
                        awakeDevice = cursor.getInt(5) == 1,
                        boostPower = cursor.getInt(6)==1,
                        timerStartedAt = cursor.getLong(7),
                        iterationCompleted = cursor.getDouble(8),
                        iterationCount = cursor.getString(9),
                        timerRunning = cursor.getInt(10)==1,
                        isNotification = cursor.getInt(11) == 1,
                        targetLength = cursor.getLong(12)
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }


    fun getStartIntentions(): List<Intention> {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM intentions WHERE timer_running=1", null)
        val list = mutableListOf<Intention>()

        if (cursor.moveToFirst()) {
            do {
                list.add(
                    Intention(
                        id = cursor.getInt(0),
                        title = cursor.getString(1),
                        intention = cursor.getString(2),
                        multiplier = cursor.getDouble(3),
                        frequency = cursor.getString(4),
                        awakeDevice = cursor.getInt(5) == 1,
                        boostPower = cursor.getInt(6)==1,
                        timerStartedAt = cursor.getLong(7),
                        iterationCompleted = cursor.getDouble(8),
                        iterationCount = cursor.getString(9),
                        timerRunning = cursor.getInt(10)==1,
                        isNotification = cursor.getInt(11) == 1,
                        targetLength = cursor.getLong(12)
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()

        return list
    }


    fun getNotificationEnabledIntention(): Intention? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM intentions WHERE is_notification = 1 LIMIT 1", null)
        val intention = if (cursor.moveToFirst()) {
            Intention(
                id = cursor.getInt(0),
                title = cursor.getString(1),
                intention = cursor.getString(2),
                multiplier = cursor.getDouble(3),
                frequency = cursor.getString(4),
                awakeDevice = cursor.getInt(5) == 1,
                boostPower = cursor.getInt(6)==1,
                timerStartedAt = cursor.getLong(7),
                iterationCompleted = cursor.getDouble(8),
                iterationCount = cursor.getString(9),
                timerRunning = cursor.getInt(10)==1,
                isNotification = cursor.getInt(11) == 1,
                targetLength = cursor.getLong(12)
            )
        } else null
        cursor.close()
        return intention
    }

    fun setNotificationIntention(id: Int) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            // Clear all
            db.execSQL("UPDATE intentions SET is_notification = 0")
            // Set selected
            val values = ContentValues().apply { put("is_notification", 1) }
            db.update("intentions", values, "id = ?", arrayOf(id.toString()))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun ensureNotificationExists() {
        val db = writableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM intentions WHERE is_notification = 1", null)
        cursor.moveToFirst()
        val hasNotification = cursor.getInt(0) > 0
        cursor.close()

        if (!hasNotification) {
            // Set notification on the first available intention
            db.execSQL("UPDATE intentions SET is_notification = 1 WHERE id = (SELECT id FROM intentions LIMIT 1)")
        }
    }

    fun stopAllIntention() {
        val db = writableDatabase
        db.execSQL("UPDATE intentions SET timer_running = 0,timer_started_at=0,iteration_completed=0.0,target_length=0,iteration_count=''")
    }


    fun getIntentionById(id: Int):Intention {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM intentions WHERE id=?", arrayOf(id.toString()))
        cursor.moveToFirst();

        val intention = Intention(
            id = cursor.getInt(0),
            title = cursor.getString(1),
            intention = cursor.getString(2),
            multiplier = cursor.getDouble(3),
            frequency = cursor.getString(4),
            awakeDevice = cursor.getInt(5) == 1,
            boostPower = cursor.getInt(6)==1,
            timerStartedAt = cursor.getLong(7),
            iterationCompleted = cursor.getDouble(8),
            iterationCount = cursor.getString(9),
            timerRunning = cursor.getInt(10)==1,
            isNotification = cursor.getInt(11) == 1,
            targetLength = cursor.getLong(12)
        )
        cursor.close()

        return intention;
    }
}
