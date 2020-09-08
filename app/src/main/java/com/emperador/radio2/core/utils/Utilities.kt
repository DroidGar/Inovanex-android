package com.emperador.radio2.core.utils

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.net.Uri
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.ImageRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.emperador.radio2.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.lang.Exception

enum class Default {
    SONG_NAME,
    ARTIST_NAME,
    ARTWORK,
    CAST_BACK,
    CAST_LOGO,
}

class Utilities(var context: Context, var artWorkListener: ArtworkListener?) {

    val config: JSONObject
    val radio: JSONObject
    private val prefs: SharedPreferences = context.getSharedPreferences("preferences_emperador", 0)

    init {
        val confString = prefs.getString("configuration", "")
        config = JSONObject(confString!!)
        radio = config.getJSONObject("radio")

    }

    interface ArtworkListener {
        fun onArtworkChange(bitmap: Bitmap)
    }

    fun getDefaultLaunchType(): Boolean {
        return config.getBoolean("start_on_video")
    }

    fun getDefault(default: Default): String {
        val defaults = radio.getJSONObject("defaults")
        return when (default) {
            Default.SONG_NAME -> defaults.getString("song_name")
            Default.ARTIST_NAME -> defaults.getString("artist_name")
            Default.ARTWORK -> defaults.getString("artwork")
            Default.CAST_BACK -> if (defaults.has("chcast_background")) defaults.getString("chcast_background") else "no existe"
            Default.CAST_LOGO -> if (defaults.has("chcast_logo")) defaults.getString("chcast_logo") else "no existe"
        }

    }

    fun getLogo(): String {
        return radio.getString("logo")
    }

    fun getAudioSources(): Map<String, String> {
        val audio = radio.getJSONArray("audio")
        val audioSources = mutableMapOf<String, String>()
        for (i in 0 until audio.length()) {
            audioSources["audio-$i"] = audio.getJSONObject(i).getString("url")
        }
        return audioSources
    }

    fun getVideoSources(): Map<String, String> {
        val videos = radio.getJSONArray("video")
        val videoSources = mutableMapOf<String, String>()
        for (i in 0 until videos.length()) {
            val data = videos.getJSONObject(i)
            videoSources["video-$i"] = data.getString("url")
        }
        return videoSources

    }

    /** ARTWORK */
    fun getArtwork(song: String, artist: String) {

        val host = "https://spotify.instream.audio"


        val path = "$host/inovanex.com/api/v1/cover/get?id=${BuildConfig.id}"
        Log.i("Utilities", "getArtwork -> $path")
        val queue = Volley.newRequestQueue(context)
        // Request a string response from the provided URL.
        val stringRequest = JsonObjectRequest(
            Request.Method.GET, path, null,
            Response.Listener { response ->

                Log.e("TAG", response.toString())


                if (!response.has("image")) {
                    downloadImage(getDefault(Default.ARTWORK))
                    return@Listener
                }


                val image = response.getString("image")


                downloadImage(image)

            },
            Response.ErrorListener {
                downloadImage(getDefault(Default.ARTWORK))
                Log.i("Utilities", "getArtwork -> fallo en obetener caratula")
            })

        // Add the request to the RequestQueue.
        stringRequest.retryPolicy = DefaultRetryPolicy(
            5000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
        queue.add(stringRequest)


    }

    fun downloadImage(path: String) {
        Log.e("IMAGE", "downloadImage -> $path");
        val queue = Volley.newRequestQueue(context)

        val stringRequest = ImageRequest(path,
            Response.Listener { bitmap ->

                artWorkListener?.onArtworkChange(bitmap)

            }, 300, 300, ImageView.ScaleType.CENTER_CROP, Bitmap.Config.RGB_565,
            Response.ErrorListener {
                it.printStackTrace()
            })

        queue.add(stringRequest)
    }

    fun roundedArtwork(): Boolean {
        return config.getBoolean("portada_redondeada")
    }

    fun getPrimaryColor(): Int {
        return Color.parseColor(radio.getString("color"))
    }

    fun getAds(): JSONArray? {
        return radio.getJSONArray("ads")
    }

    fun getVideoQualities(): JSONArray {
        return radio.getJSONArray("video")
    }

    fun getSelectedVideoQuality(): Int {
        return prefs.getInt("video-quality-selected", 0)
    }

    fun setSelectedVideoQuality(index: Int) {
        prefs.edit().putInt("video-quality-selected", index).apply()
    }

    fun isPortadaFija(): Boolean {
        return config.getBoolean("portada_fija")
    }

    fun showProgramImages(): Boolean {
        return config.getBoolean("show_programs_image")
    }
}

fun TextView.setDrawableColor(color: Int) {
    compoundDrawablesRelative.filterNotNull().forEach {
        it.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
    }
}

fun Context.openLink(link: String) {
    if (link.isEmpty()) return
    if (link.isBlank()) return

    try {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
        startActivity(browserIntent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}