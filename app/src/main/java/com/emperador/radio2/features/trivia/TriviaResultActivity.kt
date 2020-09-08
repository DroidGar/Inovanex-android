package com.emperador.radio2.features.trivia

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.emperador.radio2.MainActivity
import com.emperador.radio2.R
import com.emperador.radio2.core.utils.Utilities
import kotlinx.android.synthetic.main.activity_trivia_result.*

class TriviaResultActivity : AppCompatActivity() {
    private lateinit var util: Utilities

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trivia_result)

        util = Utilities(this, null)
        Glide.with(this).load(util.getLogo()).into(logo)

        btn.setBackgroundColor(util.getPrimaryColor())

        close2.setOnClickListener {
            onBackPressed()
        }


        val tl = intent.getStringExtra("title")
        val correct = intent.getIntExtra("correct", 0)
        val wrong = intent.getIntExtra("wrong", 0)

        mtitle.text = tl
        success.text = "$correct ${getString(R.string.correctas)}"
        fail.text = "$wrong ${getString(R.string.incorrectas)}"
    }


}
