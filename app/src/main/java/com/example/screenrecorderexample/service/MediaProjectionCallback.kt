package com.example.screenrecorderexample.service

import android.media.projection.MediaProjection

class MediaProjectionCallback: MediaProjection.Callback() {

    override fun onCapturedContentVisibilityChanged(isVisible: Boolean) {
        super.onCapturedContentVisibilityChanged(isVisible)
    }

    override fun onCapturedContentResize(width: Int, height: Int) {
        super.onCapturedContentResize(width, height)
    }

    override fun onStop() {
        super.onStop()
    }
}