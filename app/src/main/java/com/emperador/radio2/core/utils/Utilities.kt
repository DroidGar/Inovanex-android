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
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import kotlinx.android.synthetic.main.scene1.*
import org.json.JSONArray
import org.json.JSONObject
import java.lang.Exception

enum class Default {
    SONG_NAME,
    ARTIST_NAME,
    ARTWORK,
}

class Utilities(var context: Context, var artWorkListener: ArtworkListener?) {

    val config: JSONObject
    val radio: JSONObject
    val prefs: SharedPreferences

    init {
        prefs = context.getSharedPreferences("preferences_emperador", 0)
        val confString = prefs.getString("configuration", "")
        config = JSONObject(confString!!)
        radio = config.getJSONObject("radio")

    }

    interface ArtworkListener {
        fun onArtworkChange(bitmap: Bitmap)
    }


    fun getDefault(default: Default): String {
        val defaults = radio.getJSONObject("defaults")

        return when (default) {
            Default.SONG_NAME -> defaults.getString("song_name")
            Default.ARTIST_NAME -> defaults.getString("artist_name")
            Default.ARTWORK -> defaults.getString("artwork")
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
            videoSources[data.getString("name")] = data.getString("url")
        }
        return videoSources

    }

    /** ARTWORK */
    fun getArtwork(song: String, artist: String) {



        val host = "https://spotify.instream.audio"

        val query = "$song $artist"

        val path = "$host/api/function/c/track.search?limit=1&text=$query"
        Log.i("EmperadorMusicservice", "getArtwork -> $path")
        val queue = Volley.newRequestQueue(context)
        // Request a string response from the provided URL.
        val stringRequest = JsonArrayRequest(
            Request.Method.GET, path, null,
            Response.Listener { response ->

                if (response.toString() == "[]") {
                    downloadImage(getDefault(Default.ARTWORK))
                    return@Listener
                }

                val image = response.getJSONObject(0)
                    .getJSONArray("album")
                    .getJSONObject(0)
                    .getJSONArray("images")
                    .getJSONObject(1).getString("url")


                downloadImage(image)

            },
            Response.ErrorListener {
                downloadImage(getDefault(Default.ARTWORK))
                it.printStackTrace()
            })

        // Add the request to the RequestQueue.
        stringRequest.retryPolicy = DefaultRetryPolicy(
            5000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
        queue.add(stringRequest)


    }

    public fun downloadImage(path: String) {
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

    fun getVideoQualities() : JSONArray {
        return radio.getJSONArray("video")
    }
    fun getSelectedVideoQuality(): Int {
        return prefs.getInt("video-quality-selected",0)
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