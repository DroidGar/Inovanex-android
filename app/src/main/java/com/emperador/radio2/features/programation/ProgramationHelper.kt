package com.emperador.radio2.features.programation

import android.content.Context
import android.util.Log
import com.emperador.radio2.features.programation.models.Program
import com.emperador.radio2.features.programation.models.ProgramDay
import com.emperador.radio2.features.programation.repositories.ProgramDayRepository
import java.text.SimpleDateFormat
import java.util.*


interface OnProgramationListener {
    fun onProgramChange(day: ProgramDay?, program: Program?)
}

class ProgramationHelper(val context: Context, val listener: OnProgramationListener) {


    fun refresh() {

        val pref = context.getSharedPreferences("preferences_emperador", 0)
        updateCurrentProgram(ProgramDayRepository(pref).getLocalProgramDay())

    }

    private fun updateCurrentProgram(programs: List<ProgramDay>) {

        val c = Calendar.getInstance()
        val numberDay = c.get(Calendar.DAY_OF_WEEK)
        val nowTime = stringToDate("${c.get(Calendar.HOUR_OF_DAY)}:${c.get(Calendar.MINUTE)}")

        programs.map {
            if (it.dayNumber == numberDay) {
                it.programms.map { it2 ->
                    val start = stringToDate(it2.startTime)
                    val end = stringToDate(it2.endTime)
                    if (start.before(nowTime) && (end.after(nowTime) || end.before(start))) {
                        listener.onProgramChange(it, it2)
                    }
                }
            }
        }
    }

    private fun stringToDate(time: String): Date {
        val c = Calendar.getInstance()
        val parser = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
        val fullDate =
            "${c.get(Calendar.DAY_OF_MONTH)}/${c.get(Calendar.MONTH)}/${c.get(Calendar.YEAR)} $time"
        return parser.parse(fullDate)!!
    }

}