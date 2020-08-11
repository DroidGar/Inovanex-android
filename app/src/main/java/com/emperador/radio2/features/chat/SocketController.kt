package com.emperador.radio2.features.chat

import android.app.Activity
import android.util.Log
import com.github.nkzawa.emitter.Emitter
import com.github.nkzawa.socketio.client.IO
import com.github.nkzawa.socketio.client.Socket
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import org.json.JSONObject


interface OnSocketListener {

    fun onNewMessage(json: JSONObject)
    fun onConnecting()
    fun onConnected()
    fun onDisconnect()
    fun onUnauthorized()
}


class SocketController(
    private val chatConfig: JSONObject,

    private val listener: OnSocketListener,
    private val activity: Activity
) {

    private var user: FirebaseUser? = null
    private var mSocket: Socket? = null
    private var room = ""

    fun configure() {
        disconnect()
        listener.onConnecting()
        val url = chatConfig.getString("url")
        room = chatConfig.getString("room")

        configureSocket(url)
        registerListeners()

    }

    fun connect() {
        mSocket?.connect()
    }

    fun disconnect() {
        if (mSocket == null) return
        mSocket?.off("message")
        mSocket?.off("total-users")
        mSocket?.off("unauthorized")
        mSocket?.off("disconnect")
        mSocket?.off("connect")
        mSocket?.disconnect()

    }


    private fun configureSocket(url: String) {
        mSocket = IO.socket(url)
    }

    private fun registerListeners() {
        mSocket?.on("message", onNewMessage)
        mSocket?.on("total-users", onTotalUsers)
        mSocket?.on("unauthorized", onUnauthorized)
        mSocket?.on("disconnect", onDisconnect)
        mSocket?.on("connect", onConnected)
    }

    fun sendMessage(json: JSONObject) {
        mSocket?.emit("message", json)
    }

    fun sendFile(json: JSONObject) {
        mSocket?.emit("file", json)
    }

    fun joinRoom() {

        user = FirebaseAuth.getInstance().currentUser

        val _user = JSONObject()
        val data = JSONObject()


        val name = user?.displayName
        val photoUrl = user?.photoUrl
        val uid = user?.uid

        data.put("hash", uid)
        data.put("name", name)
        data.put("image", photoUrl)
        data.put("channel", room)
        _user.put("user", data)


        mSocket?.emit("auth", _user)
    }

    private val onNewMessage = Emitter.Listener { args ->
//        Log.e("socket", args[0].toString())

        val data = args[0] as JSONObject
        activity.runOnUiThread { listener.onNewMessage(data) }

    }

    private val onTotalUsers = Emitter.Listener { args ->

        //        activity.runOnUiThread { listener.onTotalUsers(args[0] as Int) }

    }

    private val onUnauthorized = Emitter.Listener { args ->

        activity.runOnUiThread { listener.onUnauthorized() }

    }

    private val onDisconnect = Emitter.Listener { args ->
        Log.e("socket", "onDisconnect")
        activity.runOnUiThread { listener.onDisconnect() }

    }

    private val onConnected = Emitter.Listener { args ->
        Log.e("socket", "onConnected")
        activity.runOnUiThread { listener.onConnected() }
        joinRoom()
    }


}