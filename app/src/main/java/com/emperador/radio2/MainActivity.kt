package com.emperador.radio2

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.transition.Scene
import android.transition.Slide
import android.transition.TransitionManager
import android.util.Log
import android.view.Display
import android.view.Gravity
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media.session.MediaButtonReceiver
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeTransition
import com.bumptech.glide.request.transition.Transition
import com.bumptech.glide.request.transition.TransitionFactory
import com.emperador.radio2.programation.ProgramationFragment
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.cast.CastPlayer
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.metadata.MetadataOutput
import com.google.android.exoplayer2.metadata.icy.IcyInfo
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.Util
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaQueueItem
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.SessionManager
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.common.images.WebImage
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar.*
import kotlinx.android.synthetic.main.scene1.*
import kotlinx.android.synthetic.main.scene1.artCont
import kotlinx.android.synthetic.main.scene1.audioView
import kotlinx.android.synthetic.main.scene2.*

enum class SourceType {
    AUDIO, VIDEO
}

enum class PlayerType {
    LOCAL, CAST
}

class MainActivity : AppCompatActivity(), Utilities.ArtworkListener,
    CastPlayer.SessionAvailabilityListener, MenuFragment.OnMenuListener {

    //https://59db7e671a1ad.streamlock.net:443/1280demo/mp4:1280demo_160p/playlist.m3u8
    //https://cdn2.instream.audio/:8007/stream


    private var playerMinimized: Boolean = false
    private var mediaCastItems = mutableListOf<MediaQueueItem>()
    private var mediaCastInfo = mutableListOf<MediaInfo>()
    private lateinit var notificationManager: PlayerNotificationManager
    private lateinit var mMediaControllerCompat: MediaControllerCompat
    private lateinit var mMediaSessionCompat: MediaSessionCompat
    private lateinit var mediaSources: ConcatenatingMediaSource
    private lateinit var util: Utilities
    private val USER_AGENT = "Inovanex"
    private lateinit var player: SimpleExoPlayer
    private lateinit var castPlayer: CastPlayer
    private lateinit var trackSelector: DefaultTrackSelector


    private var selectedMediaSourceIndex = 0
    private var mediaSourcesAudio = HashMap<Int, MediaSource>()
    private var mediaSourcesVideo = HashMap<Int, MediaSource>()
    private var allUrlsList = mutableListOf<String>()

    private var sonName = ""
    private var artistName = ""
    private var artworkBitmap: Bitmap? = null

    private var qualityOptionsArray: ArrayList<String> = ArrayList()


    // CAST
    private lateinit var mSessionManager: SessionManager
    private lateinit var mCastContext: CastContext
    private lateinit var remoteMediaClient: RemoteMediaClient


    private var currentSourceType = SourceType.AUDIO
    private var currentPlayerType = PlayerType.LOCAL

    private var lastSelectedAudioQuality = 0
    private var lastSelectedVideoQuality = 0


    private lateinit var scene1: Scene
    private lateinit var scene2: Scene

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        scene1 = Scene.getSceneForLayout(sceneRoot, R.layout.scene1, this)
        scene2 = Scene.getSceneForLayout(sceneRoot, R.layout.scene2, this)

        scene1.enter()

        util = Utilities(this, this)

        trackSelector = DefaultTrackSelector(this)

        player = SimpleExoPlayer.Builder(this).setTrackSelector(trackSelector).build()
        playerView.player = player


        initMediaSources()

        initMediaSession()

        buildNotification()

        player.addListener(errorListener)
        player.addMetadataOutput(metadataOutput)
        player.prepare(mediaSources)
        player.playWhenReady = true



        changeMediaSource.setOnClickListener {
            (it as TextView)
            it.text = if (currentSourceType == SourceType.VIDEO) {
                currentSourceType = SourceType.AUDIO
                "VIDEO"
            } else {
                currentSourceType = SourceType.VIDEO
                "AUDIO"
            }

            switchSourceType()
        }

//        quality.setOnItemClickListener { _, _, position, _ ->
//            var index = position
//
//            if (currentSourceType == SourceType.VIDEO) {
//                index = position + mediaSourcesAudio.size
//                onVideoQualityChange(index)
//            } else if (currentSourceType == SourceType.AUDIO) {
//                onAudioQualityChange(index)
//            }
//
//            selectedMediaSourceIndex = index
//
//        }

        CastButtonFactory.setUpMediaRouteButton(applicationContext, mediaButton)
        mCastContext = CastContext.getSharedInstance(this)
        mSessionManager = CastContext.getSharedInstance(this).sessionManager
        castPlayer = CastPlayer(mCastContext)
        castPlayer.setSessionAvailabilityListener(this)

        lastSelectedVideoQuality = mediaSourcesAudio.size

        switchSourceType()

        menu.setOnClickListener {
            onMenuItemSelected(10)
        }

    }


    private fun onVideoQualityChange(index: Int) {
        lastSelectedVideoQuality = index
        switchSourceType()
    }


    private fun onAudioQualityChange(index: Int) {
        lastSelectedAudioQuality = index
        switchSourceType()
    }

    private fun updateHomeScreen() {

        if (!util.roundedArtwork()) {
            artCont.radius = 0f
        }

        if (currentPlayerType == PlayerType.LOCAL) {


            if (currentSourceType == SourceType.AUDIO) {
                qualityOptionsArray = mediaSourcesAudio.map { it.value.tag } as ArrayList<String>

                playerView.visibility = GONE
                audioView.visibility = VISIBLE

                song.text = sonName
                artist.text = artistName

            } else if (currentSourceType == SourceType.VIDEO) {
                qualityOptionsArray = mediaSourcesVideo.map { it.value.tag } as ArrayList<String>

                playerView.visibility = VISIBLE
                audioView.visibility = GONE
            }

        } else if (currentPlayerType == PlayerType.CAST) {
            playerView.visibility = GONE
            audioView.visibility = GONE
            // Mostrar cast view

            if (currentSourceType == SourceType.VIDEO) {
                qualityOptionsArray = mediaSourcesVideo.map { it.value.tag } as ArrayList<String>

            } else if (currentSourceType == SourceType.AUDIO) {
                qualityOptionsArray = mediaSourcesAudio.map { it.value.tag } as ArrayList<String>

            }
        }

//        val adapter = ArrayAdapter(this, R.layout.quality_row, optionsArray)
//        quality.adapter = adapter

        glassImage(artworkBitmap, backgronudImage)

        Glide.with(this).load(util.getLogo()).into(logo)


        if (playerMinimized) {

            artwork2.setImageBitmap(artworkBitmap)

            song2.text = song.text.split(' ').joinToString(" ") { it.capitalize() }
            artist2.text = artist.text.split(' ').joinToString(" ") { it.capitalize() }

            song2.isSelected = true
            artist2.isSelected = true

        } else {

            artwork.setImageBitmap(artworkBitmap)

            song.text = song.text.split(' ').joinToString(" ") { it.capitalize() }
            artist.text = artist.text.split(' ').joinToString(" ") { it.capitalize() }

            song.isSelected = true
            artist.isSelected = true
        }


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

    private fun updateNotification() {
        notificationManager.invalidate()
    }

    private fun buildNotification() {

        notificationManager = PlayerNotificationManager
            .createWithNotificationChannel(
                this, "channel", R.string.app_name, R.string.app_name, 123,
                mediaDescriptionAdapter
            )

        // omit skip previous and next actions
        notificationManager.setUseNavigationActions(false)
        // omit fast forward action by setting the increment to zero
        notificationManager.setFastForwardIncrementMs(0)
        // omit rewind action by setting the increment to zero
        notificationManager.setRewindIncrementMs(0)

        notificationManager.setMediaSessionToken(mMediaSessionCompat.sessionToken)

        notificationManager.setPlayer(player)
    }

    private fun initMediaSession() {
        val mediaButtonReceiver = ComponentName(applicationContext, MediaButtonReceiver::class.java)
        mMediaSessionCompat =
            MediaSessionCompat(applicationContext, "MyMediasession", mediaButtonReceiver, null)
        mMediaSessionCompat.isActive = true

        val mediaSessionConnector = MediaSessionConnector(mMediaSessionCompat)
        mediaSessionConnector.setQueueNavigator(object :
            TimelineQueueNavigator(mMediaSessionCompat) {


            override fun getMediaDescription(
                player: Player?,
                windowIndex: Int
            ): MediaDescriptionCompat {

                val extras = Bundle()
                extras.putString(
                    MediaMetadataCompat.METADATA_KEY_ARTIST,
                    mediaDescriptionAdapter.getCurrentContentTitle(player!!).toString()
                )
                extras.putString(
                    MediaMetadataCompat.METADATA_KEY_TITLE,
                    mediaDescriptionAdapter.getCurrentContentText(player).toString()
                )



                return MediaDescriptionCompat.Builder()
                    .setTitle(mediaDescriptionAdapter.getCurrentContentText(player))
                    .setDescription(mediaDescriptionAdapter.getCurrentContentTitle(player))
                    .setIconBitmap(artworkBitmap)
                    .setExtras(extras)
                    .build()
            }


        })
        mediaSessionConnector.setPlayer(player)
        mMediaControllerCompat = mMediaSessionCompat.controller

    }

    private fun initMediaSources() {

        val concatenatedSource = ConcatenatingMediaSource()
        val mimeAudio = MimeTypes.AUDIO_UNKNOWN
        val mimeVideo = MimeTypes.VIDEO_UNKNOWN

        val audioSources = util.getAudioSources().toList()

        audioSources.forEachIndexed { i, item ->

            val url = item.second

            allUrlsList.add(url)

            val audio = buildMediaSource(url, item.first)!!
            concatenatedSource.addMediaSource(i, audio)
            mediaSourcesAudio[i] = audio
            mediaCastItems.add(MediaQueueItem.Builder(buildMediaInfo(mimeAudio, url)).build())

        }


        val audioLength = audioSources.size

        val videoSources = util.getVideoSources().toList()

        videoSources.forEachIndexed { i, item ->

            val url = item.second
            val index = audioLength + i

            allUrlsList.add(url)

            val video = buildMediaSource(url, item.first)!!
            concatenatedSource.addMediaSource(index, video)
            mediaSourcesVideo[index] = video
            mediaCastItems.add(MediaQueueItem.Builder(buildMediaInfo(mimeVideo, url)).build())
        }

        mediaSources = concatenatedSource

    }

    private fun buildMediaInfo(mimeType: String, url: String): MediaInfo {

        val mediaData = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE)
        mediaData.putString(MediaMetadata.KEY_TITLE, sonName)
        mediaData.putString(MediaMetadata.KEY_ALBUM_ARTIST, artistName)
        mediaData.addImage(WebImage(Uri.parse(util.getDefault(Default.ARTWORK))))

        val mediaInfo = MediaInfo.Builder(url)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType(mimeType)
            .setMetadata(mediaData).build()

        mediaCastInfo.add(mediaInfo)

        return mediaInfo
    }


    private fun switchSourceType() {

        if (currentPlayerType == PlayerType.LOCAL) {

            // Volver a mostar la notificacion si se saco por el cast
            notificationManager.setPlayer(player)

            if (currentSourceType == SourceType.VIDEO) {
                player.seekTo(lastSelectedVideoQuality, C.TIME_UNSET)
            } else if (currentSourceType == SourceType.AUDIO) {
                player.seekTo(lastSelectedAudioQuality, C.TIME_UNSET)
            }

            player.playWhenReady = true


        } else if (currentPlayerType == PlayerType.CAST) {
            player.playWhenReady = false
            // ocultar notificacion del player para mostrar la del cast
            notificationManager.setPlayer(null)

            if (currentSourceType == SourceType.VIDEO) {
                remoteMediaClient.load(mediaCastInfo[lastSelectedAudioQuality + mediaSourcesVideo.size])
            } else if (currentSourceType == SourceType.AUDIO) {
                remoteMediaClient.load(mediaCastInfo[lastSelectedAudioQuality])
            }

        }

        updateHomeScreen()

    }

    private fun getNextReason(reason: Int): String {
        return when (reason) {
            Player.DISCONTINUITY_REASON_PERIOD_TRANSITION -> "This happens when playback automatically transitions from one item to the next."
            Player.DISCONTINUITY_REASON_SEEK -> "This happens when the current playback item changes as part of a seek operation, for example when calling Player.next"
            Player.TIMELINE_CHANGE_REASON_DYNAMIC -> "This happens when the playlist changes, e.g. if items are added, moved, or removed."
            else -> "Unknown change mediasourse reason"
        }
    }

    private fun buildMediaSource(url: String, tag: String): MediaSource? {
        val data = url.split(".")
        val createdMediaSource: MediaSource
        val mimeType = data[data.size - 1]
        val uri = Uri.parse(url)

//        Log.e("TAG", "Mime Type -> $mimeType")

        val name = Util.getUserAgent(this, USER_AGENT)
        val dsf: DataSource.Factory = DefaultDataSourceFactory(this, name)

        createdMediaSource = when (mimeType) {
            "m3u8" -> HlsMediaSource.Factory(dsf).setTag(tag).createMediaSource(uri)
            "mp4" -> ProgressiveMediaSource.Factory(dsf).setTag(tag).createMediaSource(uri)
            else -> ProgressiveMediaSource.Factory(dsf).setTag(tag).createMediaSource(uri)
        }
        return createdMediaSource
    }

    override fun onArtworkChange(bitmap: Bitmap) {
        artworkBitmap = bitmap
        updateHomeScreen()
        updateNotification()
    }


    private val mediaDescriptionAdapter =
        object : PlayerNotificationManager.MediaDescriptionAdapter {
            override fun createCurrentContentIntent(player: Player): PendingIntent? {
                val intent = Intent(this@MainActivity, MainActivity::class.java)
                return PendingIntent.getActivity(this@MainActivity, 0, intent, 0)
            }

            override fun getCurrentContentText(player: Player): CharSequence? {
                return sonName
            }

            override fun getCurrentContentTitle(player: Player): CharSequence {
                return artistName
            }

            override fun getCurrentLargeIcon(
                player: Player,
                callback: PlayerNotificationManager.BitmapCallback
            ): Bitmap? {
                return artworkBitmap
            }

        }


    private val metadataOutput = MetadataOutput {

        Log.e("TAG", it.toString())

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

                updateHomeScreen()

                util.getArtwork(sonName, artistName)

            }
        }
        updateHomeScreen()
        updateNotification()
    }


    private val errorListener = object : Player.EventListener {
        override fun onPlayerError(error: ExoPlaybackException) {
            Log.e("TAG", error.message.toString())

            if (error.message.toString() == "com.google.android.exoplayer2.source.BehindLiveWindowException") {
                player.prepare(mediaSources)
                if (currentSourceType == SourceType.VIDEO) {
                    if (selectedMediaSourceIndex > 0) {
                        selectedMediaSourceIndex -= 1
                        lastSelectedVideoQuality = selectedMediaSourceIndex
                    }
                    Toast.makeText(
                        this@MainActivity,
                        "Se bajo la calidad automaticamente ya que tu conexión esta teniendo problemas",
                        Toast.LENGTH_LONG
                    ).show()
                    player.seekTo(selectedMediaSourceIndex, C.TIME_UNSET)
                }
            }

        }

        override fun onPositionDiscontinuity(reason: Int) {
//            Log.e("TAG", player.currentTag.toString())

        }
    }


    override fun onCastSessionAvailable() {
        currentPlayerType = PlayerType.CAST
        castPlayer.loadItems(mediaCastItems.toTypedArray(), 0, 0, Player.REPEAT_MODE_OFF)
        remoteMediaClient = mSessionManager.currentCastSession.remoteMediaClient
        switchSourceType()

    }

    override fun onCastSessionUnavailable() {
        currentPlayerType = PlayerType.LOCAL
        switchSourceType()
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

    private val transitionListener = object : android.transition.Transition.TransitionListener {
        override fun onTransitionEnd(transition: android.transition.Transition?) {
            updateHomeScreen()
        }

        override fun onTransitionResume(transition: android.transition.Transition?) {
        }

        override fun onTransitionPause(transition: android.transition.Transition?) {
        }

        override fun onTransitionCancel(transition: android.transition.Transition?) {
        }

        override fun onTransitionStart(transition: android.transition.Transition?) {
        }
    }

    private fun minimizePlayer(minimize: Boolean) {
        playerMinimized = minimize

        val display: Display = windowManager.defaultDisplay
        val point = Point()
        display.getSize(point)
        val width: Int = point.x
        val height: Int = point.y


        if (minimize) {
            val explode = Slide(Gravity.START)
            explode.addListener(transitionListener)
            TransitionManager.go(scene2, explode)
        } else {
            val explode = Slide(Gravity.START)
            explode.addListener(transitionListener)
            TransitionManager.go(scene1, explode)
        }

    }

    override fun onMenuItemSelected(position: Int) {

        val ft = supportFragmentManager.beginTransaction()
        ft.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)

        when (position) {
            0 -> ft.replace(R.id.container, ProgramationFragment(), "pro")
            1 -> ft.replace(R.id.container, HistoryFragment(), "his")
            10 -> ft.replace(R.id.container, MenuFragment(), "menu")
        }

        ft.addToBackStack("pro")
        ft.commitAllowingStateLoss()
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


























