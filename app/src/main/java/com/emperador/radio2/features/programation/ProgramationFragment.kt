package com.emperador.radio2.features.programation

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.emperador.radio2.R
import com.emperador.radio2.core.Utilities
import com.emperador.radio2.features.menu.onClose
import com.emperador.radio2.features.programation.models.Program
import com.emperador.radio2.features.programation.models.ProgramDay
import com.emperador.radio2.features.programation.repositories.ProgramDayRepository
import kotlinx.android.synthetic.main.frag_menu_programation.view.*


class ProgramationFragment : Fragment(), OnProgramationListener {
    private lateinit var programming: List<ProgramDay>
    private lateinit var adapter: ProgrammingAdapter
    private lateinit var util: Utilities



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.frag_menu_programation, container, false)
        onClose(view)

        util = Utilities(context!!, null)

        val preferences = context?.getSharedPreferences("preferences_emperador", 0)
        programming = ProgramDayRepository(preferences!!).getLocalProgramDay()
        val color = util.getPrimaryColor()

        adapter = ProgrammingAdapter(context!!, programming, color!!, view.listView)
        view.listView.setAdapter(adapter)

        val helper = ProgramationHelper(context!!, this)

        helper.refresh()

        view.listView.setOnChildClickListener { parent, v, groupPosition, childPosition, id ->
            val program = programming[groupPosition].programms[childPosition].toJson()

            val intent = Intent(context, ProgramDetail::class.java)
            intent.putExtra("program", program.toString())
            startActivity(intent)

            true
        }

        return view
    }

    override fun onProgramChange(day: ProgramDay?, program: Program?) {
        adapter.setCurrentProgram(day, program)
    }


}
