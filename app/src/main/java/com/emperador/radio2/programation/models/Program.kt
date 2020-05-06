package com.emperador.radio2.programation.models

import com.emperador.radio2.programation.domain.ProgramContract
import org.json.JSONObject

class Program : ProgramContract() {


    fun fromJson(json: JSONObject): Program {

        id = json.getInt("id")
        title = json.getString("title")
        image = json.getString("image")
        locutor = json.getString("locutor")
        about = if (json.has("about")) json.getString("about") else ""
        wsp = if (json.has("wsp")) json.getString("wsp") else ""
        startTime = json.getString("start_time")
        endTime = json.getString("end_time")

        return this
    }

    fun toJson(): JSONObject {

        val data = mapOf(
            "title" to title,
            "image" to image,
            "locutor" to locutor,
            "about" to about,
            "wsp" to wsp,
            "start_time" to startTime,
            "end_time" to endTime,
            "id" to id

        )


        return JSONObject(data)
    }

}