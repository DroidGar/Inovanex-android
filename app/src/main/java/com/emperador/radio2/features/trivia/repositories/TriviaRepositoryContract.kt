package com.emperador.radio2.features.trivia.repositories

import com.emperador.radio2.core.networking.NetWorkListener
import com.emperador.radio2.core.error.Error
import com.emperador.radio2.core.utils.Either
import org.json.JSONObject

interface OnLoadFromServerListener {
    fun onLoad(result: Either<Error, JSONObject>)
}

abstract class TriviaRepositoryContract {

    abstract fun loadFromServer(listener: OnLoadFromServerListener)

    abstract fun notifySelection(
        triviaId: Int, optionId: Int, name: String,
        image: String, listener: NetWorkListener
    )


}