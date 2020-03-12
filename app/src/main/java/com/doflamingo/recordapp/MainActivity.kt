package com.doflamingo.recordapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Message
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.doflamingo.recordapp.utils.ProgressHelper
import com.doflamingo.recorderlib.AudioRecorder
import com.doflamingo.recorderlib.IAudioRecorderListener
import kotlinx.android.synthetic.main.activity_main.*

private const val PERMISSION_AUDIO = 0x00

class MainActivity : AppCompatActivity(), IAudioRecorderListener {
    private var startTime = 0L
    private lateinit var audioRecorder: AudioRecorder
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    PERMISSION_AUDIO
                )
            }
        }
        audioRecorder = AudioRecorder(this, 120, this)
        btnStart.setOnClickListener {
            audioRecorder.start()
        }
        btnEnd.setOnClickListener {
            audioRecorder.complete(false)
        }
    }

    private val helper = @SuppressLint("HandlerLeak")
    object : ProgressHelper() {
        override fun handleMessage(msg: Message) {
            tvTimes.text = "${((System.currentTimeMillis() - startTime) / 1000).toInt()} S"
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_AUDIO) {
            if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "请打开录音权限", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRecordReady() {

    }

    override fun onRecordStart(var1: String?) {
        startTime = System.currentTimeMillis()
        helper.start()
    }

    override fun onRecordSuccess(var1: String?, var2: Long) {
        helper.stop()
        tvResult.text = var1
    }

    override fun onRecordFail(msg: String?) {
        helper.stop()
        Toast.makeText(this, "请检查权限是否开启", Toast.LENGTH_SHORT).show()
    }

    override fun onRecordCancel() {
        helper.stop()
    }

    override fun onDecibel(value: Double) {
        //当前录制的分贝
    }

    override fun onDestroy() {
        super.onDestroy()
        audioRecorder.destroyAudioRecorder()
    }
}
