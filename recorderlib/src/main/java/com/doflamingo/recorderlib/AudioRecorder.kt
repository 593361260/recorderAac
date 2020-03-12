package com.doflamingo.recorderlib

import android.content.Context
import android.media.*
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import java.io.File
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class AudioRecorder constructor(
    private val context: Context,
    length: Int,
    private val listener: IAudioRecorderListener?
) {
    //解码PCM
    //录制
    private var audioRecord: AudioRecord? = null
    private var audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val isRecording = AtomicBoolean(false)
    private var pcmPath: String? = null
    private var recordThread: Thread? = null
    private var codecManager: MediaCodeCManager? = null
    private val mEventHandler = Handler(Looper.getMainLooper())//用于回调到主线程
    private val maxLength = length * 1000L
    private var startTime = 0L

    /**
     * 缓冲区大小
     */
    private var bufferSize = 0

    private fun init() {
        bufferSize = AudioRecord.getMinBufferSize(
            44100,
            AudioFormat.CHANNEL_IN_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            44100,
            AudioFormat.CHANNEL_IN_STEREO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )
    }

    fun start() {
        if (isRecording.get()) {
            print("current state is isRecording ,please stop record")
            return
        }
        init()
        val rootFile = File(context.getExternalFilesDir(""), ".cache/audio")
        if (!rootFile.exists()) {
            rootFile.mkdirs()
        }
        pcmPath = File(rootFile, UUID.randomUUID().toString() + ".aac").absolutePath
        codecManager = MediaCodeCManager(pcmPath!!)
        codecManager!!.init(MediaFormat.MIMETYPE_AUDIO_AAC, 44100, 2)
        codecManager!!.startEncode()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.requestAudioFocus(
                AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).build()
            )
        } else {
            audioManager.requestAudioFocus(null as AudioManager.OnAudioFocusChangeListener?, 0, 2)
        }
        listener?.onRecordReady()
        if (!isRecording.get() && audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
            println("初始化成功, 开始录制")
            audioRecord?.startRecording()
            isRecording.set(true)
            recordThread = Thread(RecordThread())
            recordThread?.start()
        } else {
            listener?.onRecordFail("AudioRecord state " + audioRecord?.state)
        }
    }

    /*
     *是否正在录制
     */
    fun isRecording(): Boolean {
        return isRecording.get()
    }

    /**
     * @param isComplete true 取消录制,otherwise record finish
     */
    fun complete(isComplete: Boolean) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            audioManager.abandonAudioFocusRequest(AudioFocusRequest.Builder())
//        } else
        audioManager.abandonAudioFocus(null)//释放焦点
        stop()
        if (isComplete) {
            //删除本地文件
            listener?.onRecordCancel()
            File(pcmPath).let {
                if (it.exists()) {
                    it.delete()
                }
            }
        } else {
            mEventHandler.post {
                listener?.onRecordSuccess(pcmPath, (System.currentTimeMillis() - startTime) / 1000)
            }
        }
    }

    /*
     * 停止录制
     */
    private fun stop() {
        isRecording.set(false)
        recordThread?.interrupt()
        //释放资源
        recordRelease()
        codecManager!!.stopEncode()
    }

    private fun recordRelease() {
        audioRecord?.let {
            if (it.state == AudioRecord.STATE_INITIALIZED) {
                it.stop()
            }
            it.release()
        }
        audioRecord = null
    }

    private fun execute(decibel: Double) {
        mEventHandler.post {
            listener?.onDecibel(decibel)
        }
    }

    private fun handlerMsg(type: Int) {
        if (type == 1)
            mEventHandler.post {
                listener?.onRecordStart(pcmPath)
            }
    }

    private inner class RecordThread : Runnable {
        override fun run() {
            handlerMsg(1)
            val buffer = ByteArray(bufferSize)
            startTime = System.currentTimeMillis()
            while (isRecording.get()) {
                val audioSampleSize = audioRecord?.read(buffer, 0, bufferSize) ?: 0
                if (audioSampleSize > 0) {
                    codecManager?.setPcmData(buffer, audioSampleSize)
                    execute(ByteUtils.calcDecibelValue(buffer, audioSampleSize))
                }
                //延迟写入 SystemClock  --  Android专用
                if (System.currentTimeMillis() - startTime > maxLength) {
                    stop()
                }
                SystemClock.sleep(10)
            }
        }
    }

    //移除所有资源
    fun destroyAudioRecorder() {
        mEventHandler.removeCallbacksAndMessages(null)
    }
}