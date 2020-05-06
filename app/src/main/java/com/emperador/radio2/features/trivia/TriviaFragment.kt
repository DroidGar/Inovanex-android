package com.emperador.radio2.features.trivia


import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.emperador.inovanex.features.trivia.TriviasAdapter
import com.emperador.radio2.R
import com.emperador.radio2.core.utils.CountDownAnimation
import com.emperador.radio2.core.utils.Utilities
import kotlinx.android.synthetic.main.fragment_trivia.view.*
import kotlinx.android.synthetic.main.trivia_option_row.view.*
import org.json.JSONArray
import org.json.JSONObject


/**
 * A simple [Fragment] subclass.
 */
class TriviaFragment(val group: JSONObject, val position: Int, val listener: OnOptionSelected) :
    Fragment() {

    private lateinit var util: Utilities

    lateinit var madapter: TriviasAdapter
    lateinit var countDownAnimation: CountDownAnimation
    lateinit var miainView: View
    lateinit var trivia: JSONObject
    lateinit var next: Button
    private var primaryColor = 0
    private var selectedOption: JSONObject? = null
    private var flagGoNext = false

    interface OnOptionSelected {
        fun onOptionSelected(triviaId: Int, optionId: Long)
    }

    override fun onResume() {
        super.onResume()
        countDownAnimation.start()
        miainView.result.text = getString(R.string.segundos)
        madapter.showAnswer = false
        flagGoNext = false
        madapter.notifyDataSetChanged()
    }



    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        miainView = inflater.inflate(R.layout.fragment_trivia, container, false)
        util = Utilities(context!!, null)

        trivia = group.getJSONArray("trivias").getJSONObject(position)
        primaryColor = util.getPrimaryColor()

        val title = trivia.getString("question")
        val options: JSONArray = trivia.getJSONArray("options")

        miainView.title.text = title

        madapter = TriviasAdapter(options, this)
        miainView.lv.adapter = madapter

        miainView.lv.setOnItemClickListener { _, _view, position, id ->

            if (flagGoNext) return@setOnItemClickListener

            _view.text.setBackgroundColor(ContextCompat.getColor(context!!, R.color.gray))
            selectedOption = trivia.getJSONArray("options").getJSONObject(position)
            activeNext()
        }

        next = miainView.nextButton

        Glide.with(this).load(util.getLogo()).into(miainView.logo)

        val time = group.getInt("time")
        miainView.timer.text = time.toString() + "seg"

        next.setOnClickListener {


            madapter.showAnswer = true
            madapter.notifyDataSetChanged()

            when {
                selectedOption == null -> {
                    miainView.result.text = getString(R.string.incorrecto_sin_responder)
                }
                selectedOption!!.getBoolean("its_correct") -> {
                    miainView.result.text = getString(R.string.correcto)
                }
                else -> {
                    miainView.result.text = getString(R.string.incorrecto)
                }
            }

            next.text = getString(R.string.siguiente_pregunta)

            next.isEnabled = true
            next.setBackgroundColor(primaryColor)

            if (flagGoNext) {
                val optionId = selectedOption?.getLong("id") ?: 0L
                listener.onOptionSelected(trivia.getInt("id"), optionId)
            }

            flagGoNext = true
            countDownAnimation.cancel()

        }


        countDownAnimation = CountDownAnimation(miainView.timer, group.getInt("time") / 1000)

        val scaleAnimation: Animation = ScaleAnimation(
            1.0f, 0.5f, 1.0f, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f
        )
        countDownAnimation.animation = scaleAnimation

        countDownAnimation.start()
        countDownAnimation.setCountDownListener {
            next.performClick()
        }


        return miainView

    }

    private fun activeNext() {
        next.setBackgroundColor(primaryColor)
        next.isEnabled = true
    }


}
