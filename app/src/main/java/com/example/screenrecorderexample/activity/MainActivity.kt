package com.example.screenrecorderexample.activity

import android.Manifest
import android.content.Context
import android.content.Intent
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
import android.util.SparseIntArray
import android.view.Surface
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.screenrecorderexample.R
import com.example.screenrecorderexample.service.MediaProjectionService
import java.io.File

class MainActivity: AppCompatActivity() {
    private val REQUEST_CODE = 1000
    private val REQUEST_PERMISSION = 1001
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private lateinit var mediaProjectionCallback: MediaProjectionCallBack

    private var mScreenDensity: Int? = null
    private val DISPLAY_WIDTH = 1080
    private val DISPLAY_HEIGHT = 1920

    private var mediaRecorder: MediaRecorder? = null
    private lateinit var toggleBtn: Button

    var isChecked = false

    private var videoUri: String = ""
    private var ORIENTATIONS = SparseIntArray()

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            toggleBtn.setOnClickListener {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) +
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) +
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) +
                    ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                    isChecked = false
                    ActivityCompat.requestPermissions(
                        this, arrayOf(
                            Manifest.permission.READ_MEDIA_VIDEO,
                            Manifest.permission.READ_MEDIA_AUDIO,
                            Manifest.permission.READ_MEDIA_IMAGES,
                            Manifest.permission.RECORD_AUDIO
                        ), REQUEST_CODE
                    )
                } else {
                    toggleScreenRecord(toggleBtn)
                }
            }
        } else {
            toggleBtn.setOnClickListener {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) +
                    ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                    isChecked = false
                    ActivityCompat.requestPermissions(
                        this, arrayOf(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.RECORD_AUDIO
                        ), REQUEST_CODE
                    )
                } else {
                    toggleScreenRecord(toggleBtn)
                }
            }
        }

    }

    private fun toggleScreenRecord(toggleBtn: Button?) {
        if (!isChecked) {
            try {
                initRecorder()
                recordScreen()
                isChecked = true
                toggleBtn?.text = "Stop"
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            try {
                mediaRecorder!!.stop()
                mediaRecorder!!.reset()
                stopRecordingScreen()
                toggleBtn?.text = "Start"
            } catch (e: Exception) {
                e.printStackTrace()
            }
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
            mediaRecorder!!.setVideoSize(DISPLAY_WIDTH, DISPLAY_HEIGHT)
            mediaRecorder!!.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            mediaRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.HE_AAC)
            mediaRecorder!!.setVideoEncodingBitRate(512*1000)
            mediaRecorder!!.setVideoFrameRate(30)

            var rotation = windowManager.defaultDisplay.rotation
            var orientation = ORIENTATIONS.get(rotation + 90)

            mediaRecorder!!.setOrientationHint(orientation)
            mediaRecorder!!.prepare()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun recordScreen() {
        if (mediaProjection == null) {
            val intent = Intent(this, MediaProjectionService::class.java)
            startService(intent)
            startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE)
        }
        virtualDisplay = createVirtualDisplay()
//        try {
//            mediaRecorder!!.start()
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
    }

    private fun createVirtualDisplay(): VirtualDisplay? {
        return mediaProjection?.createVirtualDisplay(
            "MainActivity", DISPLAY_WIDTH, DISPLAY_HEIGHT,
            mScreenDensity!!,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mediaRecorder!!.surface, null, null
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != REQUEST_CODE) {
            Toast.makeText(this, "Unknown error", Toast.LENGTH_LONG).show()
            return
        }
        if (resultCode != RESULT_OK) {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_LONG).show()
            isChecked = false
            return
        }
        mediaProjectionCallback = MediaProjectionCallBack(
            mediaRecorder!!, mediaProjection
        )
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data!!)

        mediaProjection!!.registerCallback(mediaProjectionCallback, null)
        virtualDisplay = createVirtualDisplay()
        try {
            mediaRecorder!!.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopRecordingScreen() {
        if (virtualDisplay == null)
            return
        virtualDisplay!!.release()
        destroyMediaProjection()
    }

    private fun destroyMediaProjection() {
        if (mediaProjection != null) {
            mediaProjection!!.unregisterCallback(mediaProjectionCallback)
            mediaProjection!!.stop()
            mediaProjection = null
        }
    }

    inner class MediaProjectionCallBack(
        var mediaRecorder: MediaRecorder,
        var mediaProjection: MediaProjection?
    ): MediaProjection.Callback() {
        override fun onStop() {
            if (isChecked) {
                mediaRecorder.stop()
                mediaRecorder.reset()
            }
            mediaProjection = null
            stopRecordingScreen()
            super.onStop()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode) {
            REQUEST_PERMISSION -> {

                val permissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    grantResults[0] + grantResults[1] + grantResults[2] + grantResults[3] == PackageManager.PERMISSION_GRANTED
                } else {
                    grantResults[0] + grantResults[1] == PackageManager.PERMISSION_GRANTED
                }
                if (grantResults.size > 0 && permissionGranted) {
                    toggleScreenRecord(toggleBtn)
                } else {
                    isChecked = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        ActivityCompat.requestPermissions(
                            this, arrayOf(
                                Manifest.permission.READ_MEDIA_VIDEO,
                                Manifest.permission.READ_MEDIA_AUDIO,
                                Manifest.permission.READ_MEDIA_IMAGES,
                                Manifest.permission.RECORD_AUDIO
                            ), REQUEST_CODE
                        )
                    } else {
                        ActivityCompat.requestPermissions(
                            this, arrayOf(
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.RECORD_AUDIO
                            ), REQUEST_CODE
                        )
                    }
                }
            }
        }
    }
}