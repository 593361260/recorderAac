package com.doflamingo.recorderlib

interface IAudioRecorderListener {

    fun onRecordReady()

    fun onRecordStart(var1: String?)

    fun onRecordSuccess(var1: String?, var2: Long)

    fun onRecordFail(result: String? = null)

    fun onRecordCancel()

    fun onDecibel(value: Double)
}