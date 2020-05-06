package com.emperador.inovanex.features.ads


import android.app.Activity
import android.os.Handler
import org.json.JSONArray
import org.json.JSONObject


interface OnAdsListener {

    fun onAdsShow(ads: JSONObject)
}


class AdsHandler(val listener: OnAdsListener) {

    var ads: JSONArray = JSONArray()
    var delay = 300000L
    var handler: Handler? = null
    var index = 0

    init {

        val prefs = (listener as Activity).getSharedPreferences("preferences_emperador", 0)
        val configuration = JSONObject(prefs.getString("configuration", "{}")!!)

        delay = configuration.getJSONObject("radio").getLong("ads_timer")
        ads  = configuration.getJSONObject("radio").getJSONArray("ads")


        index = prefs.getInt("lastAdIndex", 0)

        if (handler == null) handler = Handler()

        if (ads.length() > 0) {

            handler!!.postDelayed(object : Runnable {
                override fun run() { //do something
                    listener.onAdsShow(ads.getJSONObject(index))
                    handler!!.postDelayed(this, delay)

                    if (index < ads.length() - 1) index += 1 else index = 0

                    prefs.edit().putInt("lastAdIndex", index).apply()
                }
            }, 5000)
        }
    }


}

