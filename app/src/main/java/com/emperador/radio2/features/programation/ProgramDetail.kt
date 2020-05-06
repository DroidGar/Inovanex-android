package com.emperador.radio2.features.programation

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.bumptech.glide.Glide
import com.emperador.radio2.R
import com.emperador.radio2.core.utils.Utilities
import com.emperador.radio2.core.utils.openLink
import com.emperador.radio2.features.programation.models.Program
import kotlinx.android.synthetic.main.activity_program_detail.*
import org.json.JSONObject

class ProgramDetail : AppCompatActivity() {
    private lateinit var util: Utilities
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_program_detail)
        util = Utilities(this, null)
        val program = Program().fromJson(JSONObject(intent.getStringExtra("program")!!))
        Glide.with(this).load(program.image).into(image)

        mtitle.text = program.title
        locutor.text = program.locutor
        time.text = "DESDE ${program.startTime} A ${program.endTime}"
        about.text = program.about

        wsp.setOnClickListener {
            openLink("whatsapp://send?phone=${program.wsp}")
        }


        val color = util.getPrimaryColor()!!

        color1.setTextColor(color)
        color2.setTextColor(color)
        color3.setTextColor(color)

        close.setOnClickListener {
            onBackPressed()
        }

    }
}
