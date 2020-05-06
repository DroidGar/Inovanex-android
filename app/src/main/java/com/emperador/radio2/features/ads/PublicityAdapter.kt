package com.emperador.radio2.features.ads

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.emperador.radio2.R
import kotlinx.android.synthetic.main.row_publicity.view.*
import org.json.JSONArray
import org.json.JSONObject

class PublicityAdapter(val context: Context, private val data: JSONArray) : BaseAdapter() {

    @SuppressLint("ViewHolder", "InflateParams")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val json = getItem(position)
        val view = LayoutInflater.from(context).inflate(R.layout.row_publicity, null)
        Glide.with(context).load(json.getString("image"))
            .transition(withCrossFade())
            .into(view.img)
        return view
    }

    override fun getItem(position: Int): JSONObject {
        return data.getJSONObject(position)
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    override fun getCount(): Int {
        return data.length()
    }

}