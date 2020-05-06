package com.emperador.inovanex.features.trivia

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.emperador.radio2.R
import com.emperador.radio2.core.utils.Utilities
import kotlinx.android.synthetic.main.trivia_option_row.view.*
import org.json.JSONArray
import org.json.JSONObject


class TriviasAdapter(private val options: JSONArray, val context: Fragment) : BaseAdapter() {

    private lateinit var util: Utilities

    var showAnswer: Boolean = false

    @SuppressLint("ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val layoutInflater = LayoutInflater.from(parent?.context)
        util = Utilities(context.context!!, null)
        val view = layoutInflater.inflate(R.layout.trivia_option_row, parent, false)
        val trivia = getItem(position)
        view.text.text = trivia.getString("text")

        view.line.setBackgroundColor(util.getPrimaryColor())


        if (showAnswer) {

            if (trivia.getBoolean("its_correct"))
                view.text.setBackgroundColor(
                    ContextCompat.getColor(
                        context.context!!,
                        R.color.green
                    )
                )
            else
                view.text.setBackgroundColor(ContextCompat.getColor(context.context!!, R.color.red))
        } else {

            view.text.setBackgroundColor(
                ContextCompat.getColor(
                    context.context!!,
                    android.R.color.transparent
                )
            )
        }

        return view
    }

    override fun getItem(position: Int): JSONObject {
        return options.getJSONObject(position)
    }

    override fun getItemId(position: Int): Long {
        return options.getJSONObject(position).getLong("id")
    }


    override fun getCount(): Int {
        return options.length()
    }


}

