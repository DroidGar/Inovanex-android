package com.emperador.radio2.features.programation

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.ExpandableListView
import com.emperador.radio2.R
import com.emperador.radio2.features.programation.models.Program
import com.emperador.radio2.features.programation.models.ProgramDay
import kotlinx.android.synthetic.main.row_program.view.*
import kotlinx.android.synthetic.main.row_title.view.*


class ProgrammingAdapter(
    private val context: Context,
    private val days: List<ProgramDay>,
    val color: Int,
    val list: ExpandableListView
) :
    BaseExpandableListAdapter() {

    override fun getChild(groupPosition: Int, childPosititon: Int): Program {
        return days[groupPosition].programms[childPosititon]
    }

    override fun getChildId(groupPosition: Int, childPosition: Int): Long {
        return 0
    }


    override fun getChildrenCount(groupPosition: Int): Int {
        return days[groupPosition].programms.size
    }

    override fun getGroup(groupPosition: Int): ProgramDay {
        return days[groupPosition]
    }

    override fun getGroupCount(): Int {
        return days.size
    }

    override fun getGroupId(groupPosition: Int): Long {
        return 0
    }

    override fun getGroupView(grp: Int, isExpanded: Boolean, cv: View?, parent: ViewGroup): View {
        val day = getGroup(grp)
        val view = LayoutInflater.from(context).inflate(R.layout.row_title, null)
        view.title.text = day.day
        return view
    }

    @SuppressLint("SetTextI18n")
    override fun getChildView(
        gp: Int,
        cp: Int,
        isLastChild: Boolean,
        cv: View?,
        parent: ViewGroup
    ): View {
        val program = getChild(gp, cp)
        val view = LayoutInflater.from(context).inflate(R.layout.row_program, null)
        view.name.text = program.title
        view.locutor.text = program.locutor
        view.time.apply {
            setTextColor(color)
            text = "${program.startTime} A ${program.endTime}"
        }

        if (program.active) {
            view.live.visibility = VISIBLE
        } else {
            view.live.visibility = GONE
        }

        return view
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
        return true
    }

    fun setCurrentProgram(day: ProgramDay?, program: Program?) {


        var pos = 0
        days.map {

            if (day?.dayNumber == it.dayNumber) {
                list.expandGroup(pos)

                it.programms.map { it2 ->
                    it2.active = it2.id == program?.id
                    notifyDataSetChanged()
                }
            }

            pos += 1
        }

    }
}