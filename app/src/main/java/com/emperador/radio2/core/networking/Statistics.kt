package com.emperador.radio2.core.networking

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import android.provider.Settings.Secure.getString
import android.util.Log
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.emperador.radio2.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject


class Statistics(val context: Context) {

    var sessiom_id = ""
    var user_id = ""


    @SuppressLint("HardwareIds")
    fun initSession() {

        val androidId: String = getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )
        user_id = "${FirebaseAuth.getInstance().currentUser}"

        Log.e("error", "{'id':'${BuildConfig.id}','client_id':'${androidId}'," +
                "'browser':{'name':'App de Radios','fullname':'${BuildConfig.name}','version':'${BuildConfig.VERSION_NAME}'," +
                "'versionNumber':${BuildConfig.VERSION_CODE},'mobile':true,'os':'Android'}}");

        val queue = Volley.newRequestQueue(context)

        val stringRequest = object : JsonObjectRequest(
            Method.PUT,
            "https://streamingradioplayer.inovanex.com/coolcast/api/function/c/analytics/session",
            JSONObject(
                "{'id':'${BuildConfig.id}','client_id':'${androidId}'," +
                        "'browser':{'name':'App de Radios','fullname':'${BuildConfig.name}','version':'${BuildConfig.VERSION_NAME}'," +
                        "'versionNumber':${BuildConfig.VERSION_CODE},'mobile':true,'os':'Android'}}"
            ),
            Response.Listener { it ->
                sessiom_id = it.getString("id")
            },
            Response.ErrorListener {
            }) {}

        queue.add(stringRequest)
    }

    fun update(time: Int) {
        user_id = "${FirebaseAuth.getInstance().currentUser}"

        val queue = Volley.newRequestQueue(context)

        val stringRequest = object : StringRequest(
            Method.PUT,
            "https://streamingradioplayer.inovanex.com/coolcast/api/function/c/analytics/update",
            Response.Listener { response ->
            },
            Response.ErrorListener {
                it.printStackTrace()
            }) {


            override fun getParams(): MutableMap<String, String> {
                val pm = mutableMapOf<String, String>()
                pm["id"] = sessiom_id
                pm["listentime"] = time.toString()
                return pm
            }
        }

        queue.add(stringRequest)
    }

}