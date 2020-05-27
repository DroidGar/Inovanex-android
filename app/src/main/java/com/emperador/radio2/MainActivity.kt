package com.emperador.radio2

import android.app.Activity
import android.content.*
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.transition.Scene
import android.util.Base64
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeTransition
import com.bumptech.glide.request.transition.Transition
import com.bumptech.glide.request.transition.TransitionFactory
import com.emperador.button.AudioRecordView
import com.emperador.inovanex.features.ads.OnAdsListener
import com.emperador.radio2.core.utils.Default
import com.emperador.radio2.core.utils.PermissionHandler
import com.emperador.radio2.core.utils.RealPathUtil
import com.emperador.radio2.core.utils.Utilities
import com.emperador.radio2.features.ads.AdFragment
import com.emperador.radio2.features.ads.PublicityFragment
import com.emperador.radio2.features.auth.Login
import com.emperador.radio2.features.chat.ChatAdapter
import com.emperador.radio2.features.chat.OnSocketListener
import com.emperador.radio2.features.chat.SocketController
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
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.cast.CastPlayer
import com.google.android.exoplayer2.metadata.MetadataOutput
import com.google.android.exoplayer2.metadata.icy.IcyInfo
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.firebase.auth.FirebaseAuth
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar.*
import kotlinx.android.synthetic.main.app_bar.logo
import kotlinx.android.synthetic.main.fragment_trivia.*
import kotlinx.android.synthetic.main.scene1.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


enum class SourceType {
    AUDIO, VIDEO
}

enum class PlayerType {
    LOCAL, CAST
}

class MainActivity : PermissionHandler(), MenuFragment.OnMenuListener, OnAdsListener,
    Utilities.ArtworkListener, OnProgramationListener, OnSocketListener,
    AudioManager.OnAudioFocusChangeListener {

    private lateinit var chatAdapter: ChatAdapter
    private lateinit var socket: SocketController
    private var currentProgram: Program? = null
    private lateinit var programmHelper: ProgramationHelper

    private var playImage: Drawable? = null
    private var pauseImage: Drawable? = null

    private val CONFIG_CODE: Int = 23794
    val LOGIN_CODE: Int = 23662

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


        am = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(this)
                .build()
        }

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

        loginButton.setOnClickListener {
            onMenuItemSelected(6)
        }


        playPauseButton.setOnClickListener {

            player.playWhenReady = !player.playWhenReady

        }


        CastButtonFactory.setUpMediaRouteButton(applicationContext, mediaButton)



        menu.setOnClickListener {
            Log.e("atag", "menu")
            onMenuItemSelected(10)
        }

//        AdsHandler(this)

        programmHelper = ProgramationHelper(this, this)
        programmHelper.refresh()

        playImage = ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_play)
        pauseImage = ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_pause)

        setUpWspButton()

        initChat()

        panel.addPanelSlideListener(panelStateListener)

        screenSize()

        panel.panelHeight = height / 3
        setChatHeight(height / 3)

        loginButton.visibility = GONE

        artwork.layoutParams.height = height / 3 - 50
        artwork.layoutParams.width = height / 3 - 50

    }

    var height: Int = 0
    var width: Int = 0


    private val onCurrentPlayerTypeListener = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            currentPlayerType = if(intent!!.getBooleanExtra("source", false))
                PlayerType.LOCAL
            else PlayerType.CAST

            updateHomeScreen()

        }

    }

    private fun setChatHeight(height: Int) {
        val params: ViewGroup.LayoutParams = chatView.layoutParams
        params.height = height
        chatView.layoutParams = params

        if (chatAdapter.itemCount > 0) {
            val handler = Handler()
            handler.postDelayed({
                rv.smoothScrollToPosition(chatAdapter.itemCount - 1)
            }, 100)
            rv.smoothScrollToPosition(chatAdapter.itemCount - 1)
        }
    }

    private fun screenSize() {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        height = displayMetrics.heightPixels
        width = displayMetrics.widthPixels
    }

    private fun setUpWspButton() {
        recordingView.messageView.setHint(R.string.comenta)
        recordingView.setButtonRecordColor(util.getPrimaryColor())

    }

    private val playerEventListener = object : Player.EventListener {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    Log.e("state", "STATE_BUFFERING")

                }
                Player.STATE_IDLE -> {
                    Log.e("state", "STATE_IDLE")

                }
                Player.STATE_READY -> {
                    Log.e("state", "STATE_READY")

                }
                else -> {
                    Log.e("state", playbackState.toString())

                }
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
        socket.connect()

        registerReceiver(onCurrentPlayerTypeListener, IntentFilter(ExoplayerService.SWITCH_SOURCE))
    }

    var canSendChat = true
    private fun waitChat() {
        canSendChat = false
        val handler = Handler()
        handler.postDelayed({
            canSendChat = true
        }, 5000)
    }

    override fun onDestroy() {
        unbindService()
        socket.disconnect()

        unregisterReceiver(onCurrentPlayerTypeListener)

        super.onDestroy()
    }


    private val panelStateListener = object : SlidingUpPanelLayout.PanelSlideListener {
        override fun onPanelSlide(p: View?, slideOffset: Float) {

        }

        override fun onPanelStateChanged(
            panel: View?,
            previousState: SlidingUpPanelLayout.PanelState?,
            newState: SlidingUpPanelLayout.PanelState?
        ) {

            if (newState == SlidingUpPanelLayout.PanelState.EXPANDED) {
                setChatHeight(height - 50)


            }

            if (newState == SlidingUpPanelLayout.PanelState.COLLAPSED) {
                setChatHeight(height / 3)


            }


        }

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
        //  (supportFragmentManager.findFragmentById(R.id.fragmentChat) as ChatFragment).logout()
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
                if (FirebaseAuth.getInstance().currentUser != null) {
                    loginButton.visibility = VISIBLE
                    recordingView.visibility = GONE

                } else {
                    loginButton.visibility = GONE
                    recordingView.visibility = VISIBLE
                }
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

        if (requestCode == 303 && resultCode == Activity.RESULT_OK) {
            try {
                val selectedImage = data!!.data

                val pth = RealPathUtil.getRealPathFromURI_API11to18(this, selectedImage)
                sendImage(pth)

            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }

        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == Activity.RESULT_OK) {
            sendImage(currentPhotoPath)
        }

    }

    private var fileName = ""
    private var startRecordTime = 0L

    private lateinit var am: AudioManager
    private lateinit var audioFocusRequest: AudioFocusRequest
    private var recorder: MediaRecorder? = null

    private fun startRecording() {

        if (!canRecordAudio) {
            requestPermissions()
            return
        }

        val res = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            am.requestAudioFocus(audioFocusRequest)
        } else {
            TODO("VERSION.SDK_INT < O")
        }


        fileName = "${externalCacheDir!!.absolutePath}/audiorecordtest.3gp"
        File(fileName)

        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(fileName)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

            try {
                prepare()
            } catch (e: IOException) {
                Log.e("tag", "prepare() failed")
            }

            start()
            startRecordTime = System.nanoTime()
        }


    }


    private fun cancelRecording() {

        val res = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            am.abandonAudioFocusRequest(audioFocusRequest)
        } else {
            TODO("VERSION.SDK_INT < O")
        }

       val elapsedMillis = SystemClock.elapsedRealtime()

        if (elapsedMillis < 500) {
            Thread.sleep(500)
        }

        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
        }
        recorder = null
    }


    private fun sendAudio(path: String) {


        val user = FirebaseAuth.getInstance().currentUser;
        val data = JSONObject()
        data.put("type", 2)
        data.put("name", user?.displayName)
        data.put("image", user?.photoUrl)
        data.put("message", path)
        data.put("local", true)
        data.put("duration", (recordingView.timeElapsed - 1).toString() + " sec ")
        chatAdapter.add(data)


        val message = JSONObject()
        message.put("type", 2)
        message.put("message", encoder(path))
        message.put("duration", startRecordTime)
        socket.sendFile(message)

        rv.smoothScrollToPosition(chatAdapter.itemCount - 1)


    }


    private fun sendImage(path: String) {

        val user = FirebaseAuth.getInstance().currentUser;

        val data = JSONObject()
        data.put("type", 3)
        data.put("name", user?.displayName)
        data.put("image", user?.photoUrl)
        data.put("message", path)
        data.put("local", true)
        chatAdapter.add(data)

        val message = JSONObject()
        message.put("type", 3)
        message.put("message", encoder(path))
        socket.sendFile(message)

        rv.smoothScrollToPosition(chatAdapter.itemCount - 1)

    }

    private fun encoder(filePath: String): String {
        val bytes = File(filePath).readBytes()
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }

    override fun onArtworkChange(bitmap: Bitmap) {
        artworkBitmap = bitmap
        updateHomeScreen()
    }

    override fun onProgramChange(day: ProgramDay?, program: Program?) {
        currentProgram = program
    }


    /**
     *          SOCKET
     *
     * */

    private fun initChat() {


        val viewManager = LinearLayoutManager(this).apply {
            stackFromEnd = false
            reverseLayout = false
        }

        chatAdapter = ChatAdapter(JSONArray(), this)

        rv.apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = chatAdapter
        }

        recordingView.sendView.setOnClickListener {

            if (!canSendChat) {
                Toast.makeText(
                    this,
                    "Solo puede enviar un mensaje cada 5 segundos",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            waitChat()

            val ms = recordingView.messageView.text!!.trim()
            if (ms.length < 2) {
                Toast.makeText(this, "Minimo 2 caracteres", LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val msg1 = JSONObject()
            msg1.put("type", 1)
            msg1.put("message", ms)
            socket.sendMessage(msg1)

            recordingView.messageView.setText("")

        }

        recordingView.attachmentView.setOnClickListener {
            if (!canSelectImage || !canTakePicture) {
                requestPermissions()
                return@setOnClickListener
            }

            dialogGetImage()
        }

        val chatConfig = util.radio.getJSONObject("chat")
        socket = SocketController(chatConfig, this, this)
        socket.configure()

        recordingView.recordingListener = object : AudioRecordView.RecordingListener {
            override fun onRecordingCanceled() {
                cancelRecording()
            }

            override fun onRecordingStarted() {
                startRecording()
            }

            override fun onRecordingLocked() {

            }

            override fun onRecordingCompleted() {

                if (recordingView.timeElapsed < 2) {
                    cancelRecording()
                    Toast.makeText(
                        this@MainActivity,
                        "Minimo 2 segundos de audio",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }

                cancelRecording()
                sendAudio(fileName)
            }

        }
    }

    override fun onNewMessage(json: JSONObject) {
        chatAdapter.add(json)
        rv.smoothScrollToPosition(chatAdapter.itemCount - 1)
    }


    override fun onConnecting() {
    }

    override fun onConnected() {
        if (FirebaseAuth.getInstance().currentUser == null) {
            loginButton.visibility = VISIBLE
            recordingView.visibility = GONE
        } else {
            loginButton.visibility = GONE
            recordingView.visibility = VISIBLE
        }
    }

    override fun onDisconnect() {
    }

    override fun onUnauthorized() {
    }

    fun dialogGetImage() {

        val items: Array<CharSequence> =
            arrayOf(
                getString(R.string.sacar_foto),
                getString(R.string.elegir_galeria),
                getString(R.string.cancelar)
            )

        val title = TextView(this)
        title.text = getString(R.string.seleccionar_imagen)
        title.setBackgroundColor(Color.BLACK)
        title.setPadding(10, 15, 15, 10)
        title.gravity = Gravity.CENTER
        title.setTextColor(Color.WHITE)
        title.textSize = 20f

        val builder = AlertDialog.Builder(this)



        builder.setCustomTitle(title)

        // builder.setTitle("Add Photo!");
        val listener = DialogInterface.OnClickListener { dialog, which ->

            when {
                items[which] == getString(R.string.sacar_foto) -> {
                    takePhoto()
                }
                items[which] == getString(R.string.elegir_galeria) -> {
                    getImageFromGallery()
                }
                else -> {

                }
            }

        }
        builder.setItems(items, listener)
        builder.show()
    }

    private fun getImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, 303)

    }

    var currentPhotoPath = ""
    private fun takePhoto() {

        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    Toast.makeText(this, "No se pudo acceder a la camara", LENGTH_SHORT).show()
                    return
                }
                // Continue only if the File was successfully created
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "com.emperador.inovanex.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, PermissionHandler.REQUEST_TAKE_PHOTO)
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    override fun onAudioFocusChange(focusChange: Int) {
        TODO("Not yet implemented")
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


























