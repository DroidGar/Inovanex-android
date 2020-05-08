package com.emperador.radio2

import android.content.*
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.IBinder
import android.transition.Scene
import android.transition.Slide
import android.transition.TransitionManager
import android.util.Log
import android.view.Display
import android.view.Gravity
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.ImageView
import android.widget.SimpleAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeTransition
import com.bumptech.glide.request.transition.Transition
import com.bumptech.glide.request.transition.TransitionFactory
import com.emperador.inovanex.features.ads.OnAdsListener
import com.emperador.radio2.core.utils.Default
import com.emperador.radio2.core.utils.Utilities
import com.emperador.radio2.features.ads.AdFragment
import com.emperador.radio2.features.ads.PublicityFragment
import com.emperador.radio2.features.auth.Login
import com.emperador.radio2.features.config.ConfigActivity
import com.emperador.radio2.features.history.HistoryFragment
import com.emperador.radio2.features.menu.MenuFragment
import com.emperador.radio2.features.programation.OnProgramationListener
import com.emperador.radio2.features.programation.ProgramationFragment
import com.emperador.radio2.features.programation.ProgramationHelper
import com.emperador.radio2.features.programation.models.Program
import com.emperador.radio2.features.programation.models.ProgramDay
import com.emperador.radio2.features.trivia.TriviaActivity
import com.facebook.login.LoginManager
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.cast.CastPlayer
import com.google.android.exoplayer2.metadata.MetadataOutput
import com.google.android.exoplayer2.metadata.icy.IcyInfo
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.SessionManager
import com.google.firebase.auth.FirebaseAuth
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar.*
import kotlinx.android.synthetic.main.scene1.*
import org.json.JSONObject

enum class SourceType {
    AUDIO, VIDEO
}

enum class PlayerType {
    LOCAL, CAST
}

class MainActivity : AppCompatActivity(), MenuFragment.OnMenuListener, OnAdsListener,
    Utilities.ArtworkListener, OnProgramationListener {

    private var currentProgram: Program? = null
    private lateinit var programmHelper: ProgramationHelper

    private var playImage: Drawable? = null
    private var pauseImage: Drawable? = null

    private val CONFIG_CODE: Int = 23794
    private val LOGIN_CODE: Int = 23662

    private lateinit var util: Utilities

    private var currentSourceType = SourceType.AUDIO
    private var currentPlayerType = PlayerType.LOCAL

    private var sonName = ""
    private var artistName = ""
    private var artworkBitmap: Bitmap? = null

    private lateinit var scene1: Scene

    private lateinit var player: SimpleExoPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        util = Utilities(this, this)

        scene1 = Scene.getSceneForLayout(sceneRoot, R.layout.scene1, this)

        scene1.enter()

        changeMediaSource.setOnClickListener {
            (it as TextView)

            currentSourceType =
                if (currentSourceType == SourceType.AUDIO) SourceType.VIDEO else SourceType.AUDIO

            val intent = Intent().apply {
                action = ExoplayerService.SWITCH_TYPE
                putExtra("switchType", true)
            }
            sendBroadcast(intent)

            updateHomeScreen()
        }




        playPauseButton.setOnClickListener {

            player.playWhenReady = !player.playWhenReady

        }

        CastButtonFactory.setUpMediaRouteButton(applicationContext, mediaButton)



        menu.setOnClickListener {
            onMenuItemSelected(10)
        }

//        AdsHandler(this)

        programmHelper = ProgramationHelper(this, this)
        programmHelper.refresh()

        playImage = ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_play)
        pauseImage = ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_pause)

    }

    private val playerEventListener = object : Player.EventListener {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            Log.e("state", playbackState.toString())
            if (playbackState == Player.STATE_BUFFERING) {

            }
            playPauseButton.setImageDrawable(if (player.isPlaying) pauseImage else playImage)
        }
    }


    private fun bindService() {
        Intent(this, ExoplayerService::class.java).also { intent ->
            bindService(intent, onServiceConnectionListener, Context.BIND_AUTO_CREATE)
//            bindService(intent, onServiceConnectionListener, Context.BIND_AUTO_CREATE)
        }
    }

    /**
     * Used to unbind and stop our service class
     */
    private fun unbindService() {
        Intent(this, ExoplayerService::class.java).also { intent ->
            unbindService(onServiceConnectionListener)
        }
    }

    override fun onStart() {
        super.onStart()
        bindService()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        unbindService()
        super.onDestroy()
    }


    private val onServiceConnectionListener = object : ServiceConnection {

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.e("TAG", "Service disconected")
            finish()
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is ExoplayerService.ExoServiceBinder) {
                player = service.getExoPlayerInstance()
                playerView.player = player
                player.addMetadataOutput(metadataOutput)
                player.addListener(playerEventListener)
            }
        }
    }

    private val metadataOutput = MetadataOutput {

        Log.e("TAG", it.toString())
        programmHelper.refresh()

        for (i in 0 until it.length()) {
            val entry = it.get(i)
            if (entry is IcyInfo) {

                val splitted = entry.title!!.split("-")

                sonName = util.getDefault(Default.SONG_NAME)
                artistName = util.getDefault(Default.ARTIST_NAME)

                if (splitted.size == 2) {
                    sonName = splitted[1].trim()
                    artistName = splitted[0].trim()
                }


                if (util.isPortadaFija()) {
                    util.downloadImage(util.getDefault(Default.ARTWORK))
                } else {

                    if (util.showProgramImages() && currentProgram != null)
                        util.downloadImage(currentProgram!!.image)
                    else
                        util.getArtwork(sonName, artistName)
                }

                if (currentProgram != null) {
                    sonName = currentProgram!!.title
                    artistName = currentProgram!!.locutor
                }

                updateHomeScreen()

            }
        }
        updateHomeScreen()
    }

    private fun updateHomeScreen() {

        changeMediaSource.text = if (currentSourceType == SourceType.VIDEO) {
            "AUDIO"
        } else {
            "VIDEO"
        }

        if (!util.roundedArtwork()) {
            artCont.radius = 0f
        }

        if (currentPlayerType == PlayerType.LOCAL) {


            if (currentSourceType == SourceType.AUDIO) {

                playerView.visibility = GONE
                audioView.visibility = VISIBLE

                song.text = sonName
                artist.text = artistName

            } else if (currentSourceType == SourceType.VIDEO) {

                playerView.visibility = VISIBLE
                audioView.visibility = GONE
            }

        } else if (currentPlayerType == PlayerType.CAST) {
            playerView.visibility = GONE
            audioView.visibility = GONE
            // Mostrar cast view


        }



        Glide.with(this).load(util.getLogo()).into(logo)


        song.text = song.text.split(' ').joinToString(" ") { it.capitalize() }
        artist.text = artist.text.split(' ').joinToString(" ") { it.capitalize() }

        song.isSelected = true
        artist.isSelected = true

        artwork.setImageBitmap(artworkBitmap)
        glassImage(artworkBitmap, backgronudImage)

    }

    private fun glassImage(bitmap: Bitmap?, view: ImageView) {

        if (bitmap == null) return

        val multi = MultiTransformation(BlurTransformation(20))

        Glide.with(this)
            .load(bitmap)
            .transition(DrawableTransitionOptions.with(DrawableAlwaysCrossFadeFactory()))
            .apply(RequestOptions.bitmapTransform(multi))
            .into(view)

    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        val orientation = resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
        } else {
            // In portrait
            playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
        }
    }

    override fun onMenuItemSelected(position: Int) {

        val ft = supportFragmentManager.beginTransaction()
        ft.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)

        when (position) {
            0 -> ft.replace(R.id.container, ProgramationFragment(), "pro")
            1 -> ft.replace(R.id.container, HistoryFragment(), "his")
            2 -> ft.replace(R.id.container, PublicityFragment(), "pub")
            3 -> {
                logout()
                return
            }
            4 -> {
                startActivityForResult(Intent(this, ConfigActivity::class.java), CONFIG_CODE)
                return
            }
            5 -> {
                startActivity(Intent(this, TriviaActivity::class.java))
                return
            }
            6 -> {
                login()
            }

            10 -> ft.replace(R.id.container, MenuFragment(), "menu")
        }

        ft.addToBackStack("pro")
        ft.commitAllowingStateLoss()
    }

    private fun logout() {
        LoginManager.getInstance().logOut()
        FirebaseAuth.getInstance().signOut()
        Toast.makeText(this, "Saliste correctamente", Toast.LENGTH_SHORT).show()
    }

    private fun login() {
        startActivityForResult(Intent(this, Login::class.java), LOGIN_CODE)
    }

    override fun onAdsShow(ads: JSONObject) {

        val ft = supportFragmentManager.beginTransaction()
        ft.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)

        val fragment = AdFragment()
        val bundle = Bundle()
        bundle.putString("ad", ads.toString())
        fragment.arguments = bundle
        ft.replace(R.id.container, fragment, "ads")
        ft.commitAllowingStateLoss()

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == LOGIN_CODE) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(
                    this, "Hola ${FirebaseAuth.getInstance().currentUser?.displayName}",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(this, "No se logro ingresar, reintente", Toast.LENGTH_SHORT).show()
            }
        }

        if (requestCode == CONFIG_CODE) {
            if (resultCode == RESULT_OK) {

                if (currentSourceType == SourceType.VIDEO) {
                    val index = util.getSelectedVideoQuality()
                    val intent = Intent().apply {
                        action = ExoplayerService.SWITCH_VIDEO_QUALITY
                        putExtra("quality", index)
                    }
                    sendBroadcast(intent)
                }
            }
        }

    }

    override fun onArtworkChange(bitmap: Bitmap) {
        artworkBitmap = bitmap
        updateHomeScreen()
    }

    override fun onProgramChange(day: ProgramDay?, program: Program?) {
        currentProgram = program
    }


}

class DrawableAlwaysCrossFadeFactory : TransitionFactory<Drawable> {
    private val resourceTransition: DrawableCrossFadeTransition = DrawableCrossFadeTransition(
        800,
        true
    ) //customize to your own needs or apply a builder pattern

    override fun build(
        dataSource: com.bumptech.glide.load.DataSource?,
        isFirstResource: Boolean
    ): Transition<Drawable> {
        return resourceTransition
    }
}


























