package com.emperador.radio2.features.ads


import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.emperador.radio2.R
import com.emperador.radio2.core.utils.Utilities
import com.emperador.radio2.core.utils.openLink
import kotlinx.android.synthetic.main.fragment_ad.view.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit


/**
 * A simple [Fragment] subclass.
 */
class AdFragment : Fragment() {
    private lateinit var util: Utilities

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_ad, container, false)
        view.dismiss.visibility = GONE

        util = Utilities(context!!, null)

        val json = JSONObject(arguments!!.getString("ad")!!)
        val url = json.getString("url")
        val canDismiss = json.getLong("canDismiss")
        val type = json.getInt("type")

        view.countdown.text = TimeUnit.MILLISECONDS.toSeconds(canDismiss).toString()

        val timer = object : CountDownTimer(canDismiss, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                view.countdown.text =
                    TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished).toString()
            }

            override fun onFinish() {
                view.countdown.visibility = GONE
                view.dismiss.visibility = VISIBLE
            }
        }
        timer.start()


        if (type == 0) {
            view.image.visibility = VISIBLE
            view.video.visibility = GONE
            Glide.with(this).load(url).into(view.image)
        } else {
            view.image.visibility = GONE
            view.video.visibility = VISIBLE
            view.video.setVideoURI(Uri.parse(url))
            view.video.requestFocus()
            view.video.start()
        }

        view.dismiss.setOnClickListener {

            activity!!.supportFragmentManager.beginTransaction()
                .remove(this).commitAllowingStateLoss()
        }


        view.progressbar.progress = 0
        view.progressbar.max = json.getInt("duration")

        val timerCount = object : CountDownTimer(json.getLong("duration"), 100) {
            override fun onTick(millisUntilFinished: Long) {
                view.progressbar.progress = millisUntilFinished.toInt()
            }

            override fun onFinish() {
                view.progressbar.progress = 100
                try {
                    view.dismiss.performClick()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        timerCount.start()

        // fixes pre-Lollipop progressBar indeterminateDrawable tinting
        // fixes pre-Lollipop progressBar indeterminateDrawable tinting

        view.progressbar.progressDrawable.colorFilter =
            BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                util.getPrimaryColor(),
                BlendModeCompat.SRC_ATOP
            )



        view.setOnClickListener {
            context?.openLink(json.getString("link"))
        }
        return view
    }


}
