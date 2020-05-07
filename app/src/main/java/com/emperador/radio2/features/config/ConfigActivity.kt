package com.emperador.radio2.features.config

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.emperador.radio2.R
import com.emperador.radio2.core.utils.Utilities
import kotlinx.android.synthetic.main.activity_config.*
import org.json.JSONObject


class ConfigActivity : AppCompatActivity() {
    private lateinit var util: Utilities

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)

        util = Utilities(this, null)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        title = ""

        val videos = util.getVideoQualities()
        val lastSelection = util.getSelectedVideoQuality()


        val videoQualityArray = mutableListOf<String>()

        for (i in 0 until videos.length()) {
            videoQualityArray.add(videos.getJSONObject(i).getString("name"))
        }

        val spinnerArrayAdapter: ArrayAdapter<String> = ArrayAdapter(
            this, android.R.layout.simple_spinner_item,
            videoQualityArray
        ) //selected item will look like a spinner set from XML

        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        videoQualitySpinner.adapter = spinnerArrayAdapter

        videoQualitySpinner.setSelection(lastSelection)

        videoQualitySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                util.setSelectedVideoQuality(position)
            }

        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                val returnIntent = Intent()
                returnIntent.putExtra("result", 23662)
                setResult(Activity.RESULT_OK, returnIntent)
                finish()
                return false
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
