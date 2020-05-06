package com.emperador.radio2.programation.repositories

import android.content.SharedPreferences
import com.emperador.radio2.programation.models.Program
import com.emperador.radio2.programation.models.ProgramDay
import org.json.JSONObject
import java.lang.Error

abstract class ProgramDayRepositoryContract {

    abstract fun getLocalProgramDay(): List<ProgramDay>

    abstract fun getRemoteProgramDay(): List<ProgramDay>
}


class ProgramDayRepository(val preferences: SharedPreferences) : ProgramDayRepositoryContract() {

    override fun getLocalProgramDay(): List<ProgramDay> {

        val configuration: String = preferences.getString("configuration", "{}")!!
        val jsonPrograms = JSONObject(configuration).getJSONObject("radio")
            .getJSONArray("programming")

        val programs = ArrayList<ProgramDay>()
        for (i in 0 until jsonPrograms.length()) {
            val jsn = jsonPrograms.getJSONObject(i)
            programs.add(ProgramDay().fromJson(jsn))
        }

        return programs
    }

    override fun getRemoteProgramDay(): List<ProgramDay> {
        throw Error("Unimplemented")
    }

    private fun  buildMock() : List<ProgramDay> {
        val programs = ArrayList<ProgramDay>()

        val lunesPrograms = ArrayList<Program>()

        val primero = Program()
        primero.id = 1
        primero.startTime = "15:00"
        primero.endTime = "16:00"
        primero.locutor = " Eduardo Holzmann"
        primero.title = "Buena musica programa"
        lunesPrograms.add(primero)

        val segundo = Program()
        primero.id = 2
        segundo.startTime = "16:00"
        segundo.endTime = "18:00"
        segundo.locutor = "Juan Perez"
        segundo.title = "Buena musica programa 2"
        lunesPrograms.add(segundo)

        val jueves = ProgramDay()
        jueves.day = "Jueves"
        jueves.dayNumber = 5
        jueves.programms = lunesPrograms

        val viernes = ProgramDay()
        viernes.day = "Viernes"
        viernes.dayNumber = 6
        viernes.programms = lunesPrograms


        programs.add(jueves)
        programs.add(viernes)

        return programs
    }
}