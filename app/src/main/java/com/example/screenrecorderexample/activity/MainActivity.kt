package com.example.screenrecorderexample.activity

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.DisplayMetrics
import android.util.Log
import android.util.SparseIntArray
import android.view.Surface
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.screenrecorderexample.R
import com.example.screenrecorderexample.service.MediaProjectionCallback
import com.example.screenrecorderexample.service.MediaProjectionService
import com.example.screenrecorderexample.util.Constants
import java.io.File

class MainActivity: AppCompatActivity() {
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mScreenDensity: Int? = null

    private var mediaRecorder: MediaRecorder? = null
    private lateinit var toggleBtn: Button
    private var recordingOn = false

    private var videoUri: String = ""
    private var ORIENTATIONS = SparseIntArray()

    private val mediaProjectionCallback = MediaProjectionCallback()

    private val tag = "MainActivity"

    var resultCodeResult: Int = -1
    lateinit var dataResult: Intent

    init {
        ORIENTATIONS.append(Surface.ROTATION_0, 90)
        ORIENTATIONS.append(Surface.ROTATION_90, 0)
        ORIENTATIONS.append(Surface.ROTATION_180, 270)
        ORIENTATIONS.append(Surface.ROTATION_270, 180)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        mScreenDensity = metrics.densityDpi
        mediaRecorder = MediaRecorder()
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        toggleBtn = findViewById(R.id.toggleButton)
        toggleBtn.setOnClickListener {
            if ((!recordingOn && arePermissionsGranted()) || recordingOn) {
                toggleScreenRecord()
            } else {
                recordingOn = false
                requestPermissions()
            }
        }
        val filter = IntentFilter("com.example.YourServiceReady")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(serviceReadyReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(serviceReadyReceiver, filter)
        }
    }

    private fun arePermissionsGranted(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        } else {
            return (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) +
                    ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED)
        }
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    Manifest.permission.RECORD_AUDIO
                ), Constants.REQUEST_PERMISSION
            )
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.RECORD_AUDIO
                ), Constants.REQUEST_PERMISSION
            )
        }
    }

    private fun toggleScreenRecord() {
        try {
            if (!recordingOn) {
                Log.d(tag, "Start recording - enter")
                initRecorder()
                recordScreen()
                Log.d(tag, "Start recording - exit")
            } else {
                Log.d(tag, "Stop recording - enter")
                toggleBtn.text = getString(R.string.stopping)
                mediaRecorder!!.stop()
                mediaRecorder!!.reset()
                stopRecordingScreen()
                recordingOn = false
                toggleBtn.text = getString(R.string.start)
                Log.d(tag, "Stop recording - exit")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initRecorder() {
        try {
            var recordingFile = ("ScreenREC${System.currentTimeMillis()}.mp4")
            mediaRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder!!.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            mediaRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)

            val newPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val folder = File(newPath, "MyScreenRec/")
            if (!folder.exists()) {
                folder.mkdirs()
            }
            val file1 = File(folder, recordingFile)
            videoUri = file1.absolutePath

            mediaRecorder!!.setOutputFile(videoUri)
            mediaRecorder!!.setVideoSize(Constants.DISPLAY_WIDTH, Constants.DISPLAY_HEIGHT)
            mediaRecorder!!.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            mediaRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.HE_AAC)
            mediaRecorder!!.setVideoEncodingBitRate(512*1000)
            mediaRecorder!!.setVideoFrameRate(30)

            val rotation = windowManager.defaultDisplay.rotation
            val orientation = ORIENTATIONS.get(rotation + 90)

            mediaRecorder!!.setOrientationHint(orientation)
            mediaRecorder!!.prepare()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun recordScreen() {
        if (mediaProjection == null) {
            startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), Constants.REQUEST_CODE)
        }
    }

    private fun createVirtualDisplay(): VirtualDisplay? {
        mediaProjection?.registerCallback(mediaProjectionCallback, null)
        return mediaProjection?.createVirtualDisplay(
            "MainActivity", Constants.DISPLAY_WIDTH, Constants.DISPLAY_HEIGHT,
            mScreenDensity!!,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mediaRecorder!!.surface, null, null
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != Constants.REQUEST_CODE) {
            Toast.makeText(this, "Unknown error", Toast.LENGTH_LONG).show()
            return
        }
        if (resultCode != RESULT_OK) {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_LONG).show()
            recordingOn = false
            return
        }

        resultCodeResult = resultCode
        dataResult = data!!
        val intent = Intent(this, MediaProjectionService::class.java)
        intent.setAction(Constants.START_FOREGROUND_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private val serviceReadyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.example.YourServiceReady") {
                mediaProjection = mediaProjectionManager.getMediaProjection(resultCodeResult, dataResult)
                virtualDisplay = createVirtualDisplay()
                try {
                    mediaRecorder!!.start()
                    recordingOn = true
                    toggleBtn.text = getString(R.string.stop)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun stopRecordingScreen() {
        if (virtualDisplay == null)
            return
        virtualDisplay!!.release()
        mediaProjection?.unregisterCallback(mediaProjectionCallback)
        destroyMediaProjection()
    }

    private fun destroyMediaProjection() {
        if (mediaProjection != null) {
            mediaProjection!!.stop()
            mediaProjection = null
            val intent = Intent(this, MediaProjectionService::class.java)
            intent.setAction(Constants.STOP_FOREGROUND_ACTION)
            startService(intent)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode) {
            Constants.REQUEST_PERMISSION -> {
                val permissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                } else {
                    grantResults[0] + grantResults[1] == PackageManager.PERMISSION_GRANTED
                }
                if (permissionGranted) {
                    toggleScreenRecord()
                } else {
                    recordingOn = false
                    requestPermissions()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(serviceReadyReceiver)
    }
}