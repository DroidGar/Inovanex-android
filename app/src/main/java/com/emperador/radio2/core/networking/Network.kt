package com.emperador.radio2.core.networking

import android.content.Context
import android.util.Log
import com.android.volley.*
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.emperador.radio2.BuildConfig
import org.json.JSONObject
import com.emperador.radio2.core.error.Error
import com.emperador.radio2.core.utils.Either
import com.google.firebase.auth.FirebaseUser

class Network(private val context: Context, private val user: FirebaseUser) : NetworkContract() {


    override fun get(url: String, listener: NetWorkListener) {

        val queue = Volley.newRequestQueue(context)

        val stringRequest = object : JsonObjectRequest(Method.GET, url, null,
            Response.Listener<JSONObject> { response ->
                listener.onNetworkListener(Either.Right(response))
            },
            Response.ErrorListener {
                errorHandling(it, listener)
            }) {
            override fun getHeaders(): Map<String, String> {
                return buildHeaders()
            }
        }

        queue.add(stringRequest)


    }

    override fun post(url: String, params: JSONObject, listener: NetWorkListener) {
        val queue = Volley.newRequestQueue(context)

        val stringRequest = object : JsonObjectRequest(Method.POST,
            url + "?triviaId=${params.getInt("triviaId")}&optionId=${params.getInt("optionId")}&name=${params.getString("name")}&image=${params.getString("image")}",
            null,
            Response.Listener<JSONObject> { response ->
                listener.onNetworkListener(Either.Right(response))
            },
            Response.ErrorListener {
                errorHandling(it, listener)
            }) {
            override fun getHeaders(): Map<String, String> {
                return buildHeaders()
            }
        }

        queue.add(stringRequest)
    }

    private fun buildHeaders(): Map<String, String> {
        val header = HashMap<String, String>()
        header["userId"] = user.uid
        header["radioId"] = BuildConfig.id.toString()
        return header
    }

    private fun errorHandling(error: VolleyError, listener: NetWorkListener) {
        if (error.networkResponse == null) {
            error.printStackTrace()
            listener.onNetworkListener(Either.Left(Error.NETWORK_ERROR_NULL))
        } else
            when (error.networkResponse.statusCode) {
                404 -> listener.onNetworkListener(Either.Left(Error.SERVER_404))
                500 -> listener.onNetworkListener(Either.Left(Error.SERVER_500))
                else -> {
                    error.printStackTrace()
                    listener.onNetworkListener(Either.Left(Error.UNIMPLEMENTED_NETWORK_ERROR))
                }

            }
    }
}