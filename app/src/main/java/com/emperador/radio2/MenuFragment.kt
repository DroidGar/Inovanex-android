package com.emperador.radio2

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.frag_menu.view.*
import org.json.JSONObject


class MenuFragment : Fragment() {

    private var listener: OnMenuListener? = null
    private var color: Int? = null
    private lateinit var util: Utilities

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.frag_menu, container, false)
        onClose(view)
        view.pro.setOnClickListener { onItemSelected(0) }
        view.his.setOnClickListener { onItemSelected(1) }
        view.pub.setOnClickListener { onItemSelected(2) }
        view.acc.setOnClickListener { onItemSelected(3) }
        view.options.setOnClickListener { onItemSelected(4) }
        view.trivia.setOnClickListener { onItemSelected(5) }

//        if (FirebaseAuth.getInstance().currentUser == null) {
//            view.acc.visibility = GONE
//        }

        val prefs = context!!.getSharedPreferences("preferences_emperador", 0)
        val config = prefs.getString("configuration", "{}")

        val radio = JSONObject(config!!).getJSONObject("radio")

        val programs = radio.getJSONArray("programming")
        val sponsors = radio.getJSONArray("sponsors")

        // if(programs.length() == 0) view.pro.visibility = GONE
        if (sponsors.length() == 0) view.pub.visibility = GONE

        util = Utilities(context!!, null)

        color = util.getPrimaryColor()

        setLogo(view, radio.getString("logo"))
        setColors(view)

        return view
    }



    private fun setColors(mainView: View) {

        mainView.pro.setDrawableColor(color!!)
        mainView.his.setDrawableColor(color!!)
        mainView.pub.setDrawableColor(color!!)
        mainView.trivia.setDrawableColor(color!!)

        mainView.acc.setBackgroundColor(color!!)

    }

    private fun setLogo(mainView: View, image: String) {
        Glide.with(this).load(image).into(mainView.logo)
    }


    private fun onItemSelected(position: Int) {
        activity?.onBackPressed()
        listener!!.onMenuItemSelected(position)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnMenuListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }


    interface OnMenuListener {
        fun onMenuItemSelected(position: Int)
    }


}

fun Fragment.onClose(view: View) {
    view.close.setOnClickListener {
        activity?.onBackPressed()
    }
}
