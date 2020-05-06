package com.emperador.radio2.programation.models

import com.emperador.radio2.programation.domain.ProgramDayContract
import org.json.JSONObject

class ProgramDay : ProgramDayContract() {


    fun fromJson(json: JSONObject): ProgramDay {

        day = json.getString("day")
        dayNumber = json.getInt("daynum")
        val optionsJson = json.getJSONArray("programs")
        for (i in 0 until optionsJson.length()) {
            val optionJsn = optionsJson.getJSONObject(i)
            programms.add(Program().fromJson(optionJsn))
        }
        return this
    }




}