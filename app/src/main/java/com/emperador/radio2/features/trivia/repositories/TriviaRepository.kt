package com.emperador.radio2.features.trivia.repositories

import android.util.Log
import com.emperador.radio2.BuildConfig
import com.emperador.radio2.core.networking.NetWorkListener
import com.emperador.radio2.core.utils.Either
import com.emperador.radio2.core.networking.Network
import com.emperador.radio2.core.error.Error
import org.json.JSONObject


class TriviaRepository(private val net: Network) : TriviaRepositoryContract() {


    override fun loadFromServer(listener: OnLoadFromServerListener) {

        net.get("${BuildConfig.host}/api/getTrivia", object : NetWorkListener {
            override fun onNetworkListener(result: Either<Error, JSONObject>) {
                when (result) {
                    is Either.Left -> {
                        listener.onLoad(Either.Left(result.l))
                    }
                    is Either.Right -> {
                        listener.onLoad(Either.Right(result.r))
                    }
                }
            }
        })
    }

    override fun notifySelection(
        triviaId: Int,
        optionId: Int,
        name: String,
        image: String,
        listener: NetWorkListener
    ) {
        val json = JSONObject()
        json.put("triviaId", triviaId)
        json.put("optionId", optionId)
        json.put("name", name)
        json.put("image", image)



        net.post("${BuildConfig.host}/api/postTrivia", json, listener)
    }


}