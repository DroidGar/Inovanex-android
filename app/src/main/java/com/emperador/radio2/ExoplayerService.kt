package com.emperador.radio2

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.*
import android.graphics.Bitmap
import android.net.Uri
import android.os.*
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import android.widget.Toast
import androidx.media.session.MediaButtonReceiver
import com.emperador.radio2.core.networking.Statistics
import com.emperador.radio2.core.utils.Default
import com.emperador.radio2.core.utils.Utilities
import com.emperador.radio2.features.programation.OnProgramationListener
import com.emperador.radio2.features.programation.ProgramationHelper
import com.emperador.radio2.features.programation.models.Program
import com.emperador.radio2.features.programation.models.ProgramDay
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.metadata.MetadataOutput
import com.google.android.exoplayer2.metadata.icy.IcyInfo
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.Util
import java.io.BufferedReader
import java.net.URLConnection

@Suppress("DEPRECATION")
class ExoplayerService : Service(), Utilities.ArtworkListener, OnProgramationListener {

    private lateinit var mediaSessionConnector: MediaSessionConnector
    private lateinit var player: SimpleExoPlayer

    private lateinit var notificationManager: PlayerNotificationManager
    private lateinit var mMediaControllerCompat: MediaControllerCompat
    private lateinit var mMediaSessionCompat: MediaSessionCompat
    private lateinit var mediaSources: ConcatenatingMediaSource

    private lateinit var trackSelector: DefaultTrackSelector

    private val USER_AGENT = "Inovanex"


    private var currentSourceType = SourceType.AUDIO
    private var currentPlayerType = PlayerType.LOCAL

    private var lastSelectedAudioQuality = 0
    private var lastSelectedVideoQuality = 0


    private lateinit var util: Utilities

    private var selectedMediaSourceIndex = 0
    private var mediaSourcesAudio = HashMap<Int, MediaSource>()
    private var mediaSourcesVideo = HashMap<Int, MediaSource>()
    private var allUrlsList = mutableListOf<String>()

    private var sonName = ""
    private var artistName = ""
    private var artworkBitmap: Bitmap? = null

    private var currentProgram: Program? = null
    private lateinit var programmHelper: ProgramationHelper

    companion object {

        const val DESTROY = "destroy"
        const val SWITCH_VIDEO_QUALITY = "quality"
        const val SWITCH_TYPE = "type"
        const val SWITCH_SOURCE = "source"
        const val SWITCH_PLAYER = "player"

    }

    override fun onBind(intent: Intent?): IBinder? {

        return ExoServiceBinder()
    }


    lateinit var statistics: Statistics

    val handler = Handler()
    var runnable = handler.postDelayed(object : Runnable {
        override fun run() {
            statistics.update(5)
            handler.postDelayed(this, 5000)//1 sec delay
        }
    }, 0)


    override fun onCreate() {
        super.onCreate()



        statistics = Statistics(this)
        statistics.initSession()



        util = Utilities(this, this)


        trackSelector = DefaultTrackSelector(this)


        player = SimpleExoPlayer.Builder(this).setTrackSelector(trackSelector).build()

        initMediaSources()

        initMediaSession()

        buildNotification()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.CONTENT_TYPE_MUSIC)
            .build()


        player.setAudioAttributes(audioAttributes,true)
        player.addListener(playerEventListener)
        player.addMetadataOutput(metadataOutput)
        player.prepare(mediaSources)
        player.playWhenReady = true


        lastSelectedVideoQuality = util.getSelectedVideoQuality() + mediaSourcesAudio.size



        programmHelper = ProgramationHelper(this, this)
        programmHelper.refresh()

        registerReceiver(onSwitchType, IntentFilter(SWITCH_TYPE))
        registerReceiver(onSwitchPlayer, IntentFilter(SWITCH_PLAYER))
        registerReceiver(onSwitchVideoQuality, IntentFilter(SWITCH_VIDEO_QUALITY))


    }


    inner class ExoServiceBinder : Binder() {

        /**
         * This method should be used only for setting the exoplayer instance.
         * If exoplayer's internal are altered or accessed we can not guarantee
         * things will work correctly.
         */
        fun getExoPlayerInstance() = player
    }

    private val onSwitchType = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            currentSourceType =
                if (currentSourceType == SourceType.AUDIO) SourceType.VIDEO else SourceType.AUDIO



            Log.e("TAG", currentSourceType.toString())
            switchSourceType()

        }
    }

    private val onSwitchPlayer = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            currentPlayerType =
                if (intent.getBooleanExtra("cast", false)) PlayerType.CAST else PlayerType.LOCAL

            player.playWhenReady = currentPlayerType == PlayerType.LOCAL

            Log.e("tag", "press play ${player.playWhenReady}")

        }
    }

    private val onSwitchVideoQuality = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            val index = intent!!.extras!!.getInt("quality") + mediaSourcesAudio.size
            onVideoQualityChange(index)
        }

    }

    private fun buildNotification() {

        notificationManager = PlayerNotificationManager
            .createWithNotificationChannel(
                this, "channel", R.string.app_name, R.string.app_name, 123,
                mediaDescriptionAdapter, notificationListener
            )

        // omit skip previous and next actions
        notificationManager.setUseNavigationActions(false)
        // omit fast forward action by setting the increment to zero
        notificationManager.setFastForwardIncrementMs(0)
        // omit rewind action by setting the increment to zero
        notificationManager.setRewindIncrementMs(0)
        notificationManager.setUseStopAction(true)

        notificationManager.setMediaSessionToken(mMediaSessionCompat.sessionToken)

        notificationManager.setPlayer(player)
    }

    private val notificationListener = object : PlayerNotificationManager.NotificationListener {

        override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
            stopForeground(true)
            this@ExoplayerService.onDestroy()
        }

        override fun onNotificationStarted(notificationId: Int, notification: Notification) {
            startForeground(notificationId, notification)
        }

        override fun onNotificationPosted(
            notificationId: Int,
            notification: Notification,
            ongoing: Boolean
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(notificationId, notification, foregroundServiceType)
            }
        }

    }

    override fun stopService(name: Intent?): Boolean {
        unregisterReceiver(onSwitchType)
        unregisterReceiver(onSwitchVideoQuality)
        return super.stopService(name)
    }

    override fun onDestroy() {

        handler.removeCallbacksAndMessages(null)
        notificationManager.setPlayer(null)
        player.stop()

        Log.e("TAG", "onDestroyService")
        super.onDestroy()
    }

    private fun initMediaSession() {
        val mediaButtonReceiver = ComponentName(applicationContext, MediaButtonReceiver::class.java)
        mMediaSessionCompat = MediaSessionCompat(
            applicationContext, "MyMediasession", mediaButtonReceiver, null
        )
        mMediaSessionCompat.isActive = true


        // Controla los botones de la pantalla de bloqueo

        val timeLineQueueNavigator = object : TimelineQueueNavigator(mMediaSessionCompat) {


            override fun getMediaDescription(
                player: Player,
                windowIndex: Int
            ): MediaDescriptionCompat {

                val extras = Bundle()
                extras.putString(
                    MediaMetadataCompat.METADATA_KEY_ARTIST,
                    mediaDescriptionAdapter.getCurrentContentTitle(player).toString()
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

        }

        mediaSessionConnector = MediaSessionConnector(mMediaSessionCompat)
        mediaSessionConnector.setQueueNavigator(timeLineQueueNavigator)
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
            //  mediaCastItems.add(MediaQueueItem.Builder(buildMediaInfo(mimeAudio, url)).build())

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
            //    mediaCastItems.add(MediaQueueItem.Builder(buildMediaInfo(mimeVideo, url)).build())
        }

        mediaSources = concatenatedSource

    }

    private fun buildMediaSource(url: String, tag: String): MediaSource? {
        val data = url.split(".")
        val createdMediaSource: MediaSource
        val mimeType = data[data.size - 1]
        val uri = Uri.parse(url)


        val name = Util.getUserAgent(this, USER_AGENT)
        val dsf: DataSource.Factory = DefaultDataSourceFactory(this, name)

        createdMediaSource = when (mimeType) {
            "m3u8" -> HlsMediaSource.Factory(dsf).setTag(tag).createMediaSource(uri)
            "mp4" -> ProgressiveMediaSource.Factory(dsf).setTag(tag).createMediaSource(uri)
            else -> ProgressiveMediaSource.Factory(dsf).setTag(tag).createMediaSource(uri)
        }
        return createdMediaSource
    }

    private val mediaDescriptionAdapter =
        object : PlayerNotificationManager.MediaDescriptionAdapter {
            override fun createCurrentContentIntent(player: Player): PendingIntent? {
                val intent = Intent(this@ExoplayerService, MainActivity::class.java)
                return PendingIntent.getActivity(this@ExoplayerService, 0, intent, 0)
            }

            override fun getCurrentContentText(player: Player): CharSequence? {
                return artistName
            }

            override fun getCurrentContentTitle(player: Player): CharSequence {
                return sonName
            }

            override fun getCurrentLargeIcon(
                player: Player,
                callback: PlayerNotificationManager.BitmapCallback
            ): Bitmap? {
                return artworkBitmap
            }

        }

    private val metadataOutput = MetadataOutput {

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

                updateNotification()

            }
        }
        updateNotification()
    }

    private val playerEventListener = object : Player.EventListener {
        override fun onPlayerError(error: ExoPlaybackException) {
            Log.e("TAG_EVENT", error.message.toString())

            if (error.message.toString() == "com.google.android.exoplayer2.source.BehindLiveWindowException") {
                player.prepare(mediaSources)
                if (currentSourceType == SourceType.VIDEO) {
                    if (selectedMediaSourceIndex > 0) {
                        selectedMediaSourceIndex -= 1
                        lastSelectedVideoQuality = selectedMediaSourceIndex
                        util.setSelectedVideoQuality(lastSelectedVideoQuality - mediaSourcesAudio.size)
                    }
                    Toast.makeText(
                        this@ExoplayerService,
                        "Se bajo la calidad automaticamente ya que tu conexión esta teniendo problemas",
                        Toast.LENGTH_LONG
                    ).show()
                    switchSourceType()
                }
            }

            if (error.message.toString() == "com.google.android.exoplayer2.upstream.HttpDataSource\$HttpDataSourceException: Unable to connect") {
                Log.e("TAG_EVENT", "Sin internet")
            }
        }

        override fun onPositionDiscontinuity(reason: Int) {
//            Log.e("TAG", player.currentTag.toString())

        }

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {

        }
    }

    private fun updateNotification() {
        notificationManager.invalidate()
    }


    private fun switchSourceType() {

        Log.e("tag", currentPlayerType.toString())

        if (currentSourceType == SourceType.VIDEO) {
            player.seekTo(lastSelectedVideoQuality, C.TIME_UNSET)
        } else if (currentSourceType == SourceType.AUDIO) {
            player.seekTo(lastSelectedAudioQuality, C.TIME_UNSET)
        }


    }


    override fun onArtworkChange(bitmap: Bitmap) {
        artworkBitmap = bitmap
        updateNotification()
    }


    private fun onVideoQualityChange(index: Int) {

        if (lastSelectedVideoQuality == index) return

        lastSelectedVideoQuality = index
        selectedMediaSourceIndex = index
        util.setSelectedVideoQuality(lastSelectedVideoQuality - mediaSourcesAudio.size)
        switchSourceType()
    }

    private fun onAudioQualityChange(index: Int) {
        lastSelectedAudioQuality = index
        switchSourceType()
    }


    override fun onProgramChange(day: ProgramDay?, program: Program?) {
        currentProgram = program
    }
}