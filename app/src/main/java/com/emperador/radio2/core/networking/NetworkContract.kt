package com.emperador.radio2.core.networking

import com.emperador.radio2.core.utils.Either
import com.emperador.radio2.core.error.Error
import org.json.JSONObject

interface NetWorkListener {
    fun onNetworkListener(result: Either<Error, JSONObject>) {}
}

abstract class NetworkContract {


    abstract fun get(url: String, listener: NetWorkListener)

    abstract fun post(url: String, params: JSONObject, listener: NetWorkListener)

}