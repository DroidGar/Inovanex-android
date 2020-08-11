package com.emperador.radio2.features.chat

import android.annotation.SuppressLint
import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.emperador.radio2.R
import com.emperador.radio2.core.utils.Utilities
import com.emperador.radio2.core.utils.openLink
import kotlinx.android.synthetic.main.row_message_text.view.*
import kotlinx.android.synthetic.main.row_message_text.view.image
import kotlinx.android.synthetic.main.row_message_text.view.name
import org.json.JSONArray
import org.json.JSONObject


class ChatAdapter(private var messages: JSONArray, val context: Activity) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TYPE_TEXT = 1
    private val TYPE_AUDIO = 2
    private val TYPE_IMAGE = 3
    private val TYPE_RADIO = 4
    private val TYPE_ADS = 5

    private lateinit var util: Utilities

    fun addAll(messages: JSONArray) {
        this.messages = messages
        notifyDataSetChanged()
    }

    fun add(message: JSONObject) {
        this.messages.put(message)
        notifyDataSetChanged()

    }

    fun removeAll() {
        messages = JSONArray()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        util = Utilities(context, null)

        return when (viewType) {
            TYPE_TEXT -> {
                val rowMessage = inflater.inflate(R.layout.row_message_text, parent, false)
                TextViewHolder(rowMessage)
            }
            TYPE_AUDIO -> {
                val rowMessage = inflater.inflate(R.layout.row_message_audio, parent, false)
                AudioViewHolder(rowMessage)
            }
            TYPE_IMAGE -> {
                val rowMessage = inflater.inflate(R.layout.row_message_image, parent, false)
                ImageViewHolder(rowMessage)
            }
            TYPE_RADIO -> {
                val rowMessage = inflater.inflate(R.layout.row_message_text, parent, false)
                TextViewHolder(rowMessage)
            }
            TYPE_ADS -> {
                val rowMessage = inflater.inflate(R.layout.row_message_ads, parent, false)
                AdsViewHolder(rowMessage)
            }
            else -> {
                val rowMessage = inflater.inflate(R.layout.row_message_text, parent, false)
                TextViewHolder(rowMessage)
            }
        }

    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages.getJSONObject(position)

        return when (message.getInt("type")) {
            TYPE_TEXT -> (holder as TextViewHolder).bind(message)
            TYPE_RADIO -> (holder as TextViewHolder).bind(message)
            TYPE_AUDIO -> (holder as AudioViewHolder).bind(message)
            TYPE_IMAGE -> (holder as ImageViewHolder).bind(message)
            TYPE_ADS -> (holder as AdsViewHolder).bind(message)
            else -> (holder as TextViewHolder).bind(message)
        }

    }


    override fun getItemCount() = messages.length()


    override fun getItemViewType(position: Int): Int {
        return messages.getJSONObject(position).getInt("type")
    }


    inner class TextViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(message: JSONObject) {

            itemView.name.text = message.getString("name")
            itemView.message.text = message.getString("message")
                .trimIndent().trim().replace("  ", " ")
                .replace("\n", "")

//            itemView.message.text = "\uD83D\uDE00"

            if (message.has("image"))
                Glide.with(context)
                    .load(message.getString("image"))
                    .apply(RequestOptions.circleCropTransform())
                    .into(itemView.image)

            if (message.getInt("type") == 4) {
                val color = util.getPrimaryColor()
                itemView.name.setTextColor(color)

            }
        }
    }

    inner class AudioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        @SuppressLint("SetTextI18n")
        fun bind(message: JSONObject) {
            itemView.name.text = message.getString("name")

            itemView.message.text = "Audio enviado ${message.getString("duration")}"

            Glide.with(context)
                .load(message.getString("audio"))
                .apply(RequestOptions.circleCropTransform())
                .into(itemView.image)
        }
    }

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(message: JSONObject) {
            itemView.name.text = message.getString("name")

            itemView.message.text = "Imagen enviada."

            Glide.with(context)
                .load(message.getString("image"))
                .apply(RequestOptions.circleCropTransform())
                .into(itemView.image)
        }
    }

    inner class AdsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(message: JSONObject) {

            Glide.with(context)
                .load(message.getString("image"))
                .into(itemView.image)

            itemView.image.setOnClickListener {
                if (message.has("message") && message.getString("message").isNotEmpty())
                    context.openLink(message.getString("message"))
            }

        }
    }
}



























