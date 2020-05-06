package com.emperador.radio2.programation.domain

import com.emperador.radio2.programation.models.Program

abstract class ProgramDayContract {

    var day: String = ""
    var dayNumber: Int = 0
    var programms = ArrayList<Program>()

}