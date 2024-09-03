package com.anthroteacher.intentionrepeater

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TimerViewModel : ViewModel() {

    private val _timerRunning = MutableLiveData<Boolean>()
    val timerRunning: LiveData<Boolean> get() = _timerRunning

    fun setTimerRunning(value: Boolean) {
        _timerRunning.value = value
    }
}