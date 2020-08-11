package com.emperador.radio2.features.trivia

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.emperador.radio2.core.networking.NetWorkListener
import com.emperador.radio2.core.networking.Network
import com.emperador.radio2.core.error.Error
import com.emperador.radio2.features.auth.Login
import com.emperador.radio2.features.trivia.repositories.OnLoadFromServerListener
import com.emperador.radio2.R
import com.emperador.radio2.core.utils.Either
import com.emperador.radio2.core.utils.Utilities
import com.emperador.radio2.features.trivia.repositories.TriviaRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.android.synthetic.main.activity_trivia.*
import org.json.JSONObject


/*
 crear la trivia y poner fecha en la que se va publicar, marcar enviar push al publicar automaticamente ,
 elegir si mostrar resultados al instante
 elegir si reenviar push cuando finalizo
* */


class TriviaActivity : AppCompatActivity(), TriviaFragment.OnOptionSelected {

    private lateinit var preferences: SharedPreferences
    private lateinit var triviaGroup: JSONObject
    private lateinit var repository: TriviaRepository
    private var user: FirebaseUser? = null
    private lateinit var util: Utilities

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trivia)

        util = Utilities(this, null)

        preferences = getSharedPreferences("preferences_emperador", 0)

        user = FirebaseAuth.getInstance().currentUser

        loadTrivias()

        start.setBackgroundColor(util.getPrimaryColor())

        backButton.setOnClickListener { onBackPressed() }
        close.setOnClickListener { onBackPressed() }
        close2.setOnClickListener { onBackPressed() }
    }


    private fun needLogin() {
        startActivityForResult(Intent(this, Login::class.java), 100)
    }

    private fun loadTrivias() {
        welcome.visibility = GONE
        empty.visibility = GONE
        trivia_view.visibility = GONE

        if (user == null) {
            needLogin()
            return
        }


        progress_view.visibility = VISIBLE
        repository = TriviaRepository(Network(this, user!!))

        repository.loadFromServer(object : OnLoadFromServerListener {
            override fun onLoad(result: Either<Error, JSONObject>) {

                when (result) {
                    is Either.Left -> {
                        loadViewNoTrivias()
                    }
                    is Either.Right -> {
                        loadTriviaWelcome(result.r)
                    }
                }
                progress_view.visibility = GONE
            }

        })

    }

    private fun loadViewNoTrivias() {
        backButton.setBackgroundColor(util.getPrimaryColor())
        empty.visibility = VISIBLE

    }

    private fun loadTriviaWelcome(triviaGroup: JSONObject) {
        this.triviaGroup = triviaGroup

        if (triviaGroup.getJSONArray("trivias").length() == 0) {
            val title = triviaGroup.getString("name")
            val correct = triviaGroup.getJSONObject("results").getInt("correct")
            val wrong = triviaGroup.getJSONObject("results").getInt("incorrect")
            showResults(title, correct, wrong)
            return
        }

        welcome.visibility = VISIBLE




        Glide.with(this).load(util.getLogo()).into(logo)
        upText.text = triviaGroup.getString("upText")
        mtitle.text = triviaGroup.getString("name")
        bottomText.text = triviaGroup.getString("bottomText")

        val startTime = triviaGroup.getString("start_time")
        val endTime = triviaGroup.getString("end_time")
        val validText =
            "${getString(R.string.valido)} $startTime ${getString(R.string.al)} $endTime"

        valid.text = validText

        start.setOnClickListener {
            welcome.visibility = GONE
            loadTriviasView(triviaGroup)
        }
    }

    private fun loadTriviasView(triviaGroup: JSONObject) {
        trivia_view.visibility = VISIBLE
        viewPager.adapter = TriviaPagerAdapter(supportFragmentManager, triviaGroup, this)
    }

    override fun onOptionSelected(triviaId: Int, optionId: Long, name: String, image: String) {
        val nextPosition = getNextPossibleItemIndex()

        // Notify about selection to server
        notifyServerAboutSelection(
            triviaId, optionId.toInt(), name,
            image
        )

        // check if last trivia, then go back or show results
        if (nextPosition == -1) {
            loadTrivias()
        }

        // Move on view pager to next trivia
        viewPager.setCurrentItem(nextPosition, true)

    }

    private fun notifyServerAboutSelection(
        triviaId: Int,
        optionId: Int,
        name: String,
        image: String
    ) {

        repository.notifySelection(triviaId, optionId, name, image, object : NetWorkListener {})

    }

    private fun getNextPossibleItemIndex(): Int {
        val currentIndex: Int = viewPager.currentItem
        val total: Int = viewPager.adapter!!.count - 1

        if (total == currentIndex) {
            return -1
        }

        return currentIndex + 1

    }

    private fun showResults(title: String, correct: Int, wrong: Int) {
        val intent = Intent(this, TriviaResultActivity::class.java)
        intent.putExtra("title", title)
        intent.putExtra("correct", correct)
        intent.putExtra("wrong", wrong)
        intent.flags = intent.flags or Intent.FLAG_ACTIVITY_NO_HISTORY
        startActivity(intent)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            user = FirebaseAuth.getInstance().currentUser
            loadTrivias()
        } else {
            onBackPressed()
        }

    }

}
