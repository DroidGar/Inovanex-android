package com.emperador.radio2

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.upstream.AssetDataSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
import kotlinx.android.synthetic.main.activity_splash.*
import org.json.JSONObject

class SplashActivity : AppCompatActivity(), Player.EventListener {

    private var configReady = false
    private var id: Int = 0
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        setContentView(R.layout.activity_splash)


        id = BuildConfig.id
        val splash_type = BuildConfig.splash

        prefs = getSharedPreferences("preferences_emperador", 0)
        configReady = prefs.contains("configuration")
        downloadConfig()

        when (splash_type) {
            0 -> {

            }
            1 -> {
                flagVideoEnd = false
                setUpVideo()
            }
            2 -> {
                setUpFullImage()
            }
        }

    }

    private fun setUpFullImage() {
        fullImage.visibility = View.VISIBLE
    }

    private fun setUpVideo() {
        video.visibility = View.VISIBLE

        val dataSpec = DataSpec(Uri.parse("asset:///splash.mp4"))

        val exoPlayer = ExoPlayerFactory.newSimpleInstance(this)

        val assetDataSource = AssetDataSource(this)
        assetDataSource.open(dataSpec)

        val factory = DataSource.Factory {
            assetDataSource
        }

        val source = ExtractorMediaSource(
            assetDataSource.uri,
            factory, DefaultExtractorsFactory(), null, null
        )

        video.player = exoPlayer
        exoPlayer.prepare(source)
        video.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        exoPlayer.playWhenReady = true
        exoPlayer.addListener(this)
    }

    private var flagConfigReady = false
    private fun downloadConfig() {

        val queue = Volley.newRequestQueue(this)
        val url = "https://apps.instream.audio/api/appConfig/$id"
        Log.e("config", url)

        val stringRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            Response.Listener<JSONObject> { response ->
                prefs.edit().putString("configuration", response.toString()).apply()
                flagConfigReady = true
                next()
            },
            Response.ErrorListener {
                it.printStackTrace()
                Toast.makeText(this, "Falló en bajar la configuración", Toast.LENGTH_SHORT).show()
            })

        stringRequest.setShouldCache(false)
        queue.add(stringRequest)


    }

    private var flagVideoEnd = true
    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        if (playbackState == ExoPlayer.STATE_ENDED) {
            flagVideoEnd = true
            next()
        }
    }

    private fun next() {
        if (flagConfigReady && flagVideoEnd) {
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }
}
