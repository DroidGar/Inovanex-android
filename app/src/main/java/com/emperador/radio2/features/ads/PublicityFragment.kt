package com.emperador.radio2.features.ads

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import kotlinx.android.synthetic.main.frag_menu_publicity.view.*
import com.emperador.radio2.R
import com.emperador.radio2.core.Utilities
import com.emperador.radio2.core.openLink
import com.emperador.radio2.features.menu.onClose


class PublicityFragment : Fragment() {

    private lateinit var util: Utilities

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.frag_menu_publicity, container, false)
        onClose(view)
        util = Utilities(context!!, null)
        val sponsors = util.radio.getJSONArray("sponsors")
        view.list.adapter = PublicityAdapter(
            context!!,
            sponsors
        )

        view.list.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                val url = sponsors.getJSONObject(position).getString("link")
                context?.openLink(url)
            }

        return view
    }


}
