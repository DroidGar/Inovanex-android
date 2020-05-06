package com.emperador.radio2

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.frag_menu_history.view.*


class HistoryFragment : Fragment() {
    private lateinit var util: Utilities

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.frag_menu_history, container, false)
        onClose(view)
        util = Utilities(context!!, null)
        view.text.text = util.config.getJSONObject("radio").getString("history")
        return view
    }


}
