package com.emperador.radio2.core.utils

import android.Manifest.permission.*
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import androidx.fragment.app.Fragment

open class PermissionHandler : AppCompatActivity() {

    // Requesting permission to RECORD_AUDIO
    var canRecordAudio = false
    var canTakePicture = false
    var canSelectImage = false


    companion object {
        val REQUEST_TAKE_PHOTO = 0
        val REQUEST_SELECT_IMAGE = 1
    }


    override fun onStart() {
        super.onStart()

        // check permissions
        checkPermissionsAceppted()


    }

    fun requestPermissions() {
        val permissions: Array<String> = arrayOf(RECORD_AUDIO, CAMERA, WRITE_EXTERNAL_STORAGE)
        ActivityCompat.requestPermissions(this, permissions, 100)
    }

    public fun checkPermissionsAceppted() {
        val pg = PERMISSION_GRANTED
        canRecordAudio = ContextCompat.checkSelfPermission(this, RECORD_AUDIO) == pg
        canTakePicture = ContextCompat.checkSelfPermission(this, CAMERA) == pg
        canSelectImage = ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) == pg

        Log.d("Permission", "RECORD_AUDIO $canRecordAudio")
        Log.d("Permission", "CAMERA $canTakePicture")
        Log.d("Permission", "WRITE_EXTERNAL_STORAGE $canSelectImage")

        if (!canRecordAudio || !canTakePicture || !canSelectImage) {
            requestPermissions()
        }

    }


    override fun onRequestPermissionsResult(rq: Int, permissions: Array<String>, gr: IntArray) {
        super.onRequestPermissionsResult(rq, permissions, gr)

        Log.d("Permission", "permission result")


    }


}