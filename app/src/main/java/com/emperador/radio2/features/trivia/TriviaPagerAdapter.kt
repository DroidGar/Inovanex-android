package com.emperador.radio2.features.trivia

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.emperador.radio2.features.trivia.TriviaFragment
import org.json.JSONObject


class TriviaPagerAdapter(
    fragmentManager: FragmentManager,
    private val group: JSONObject,
    private val listener: TriviaFragment.OnOptionSelected
) : FragmentStatePagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItem(position: Int): Fragment {
        return TriviaFragment(group, position, listener)
    }

    override fun getCount(): Int {
        return group.getJSONArray("trivias").length()
    }


}