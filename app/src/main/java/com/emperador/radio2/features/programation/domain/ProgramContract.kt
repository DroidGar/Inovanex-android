package com.emperador.radio2.features.programation.domain

abstract class ProgramContract {

    var id : Int = 0
    var title : String = ""
    var image : String = ""
    var locutor: String = ""
    var startTime : String = ""
    var endTime : String = ""
    var about : String = ""
    var wsp : String = ""
    var active : Boolean = false

}