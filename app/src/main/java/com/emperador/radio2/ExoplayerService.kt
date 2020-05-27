package com.emperador.radio2

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.*
import android.graphics.Bitmap
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import android.view.View.GONE
import android.widget.Toast
import androidx.media.session.MediaButtonReceiver
import androidx.mediarouter.media.MediaItemMetadata.KEY_TITLE
import com.emperador.radio2.core.utils.Default
import com.emperador.radio2.core.utils.Utilities
import com.emperador.radio2.features.programation.OnProgramationListener
import com.emperador.radio2.features.programation.ProgramationHelper
import com.emperador.radio2.features.programation.models.Program
import com.emperador.radio2.features.programation.models.ProgramDay
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ext.cast.CastPlayer
import com.google.android.exoplayer2.ext.cast.SessionAvailabilityListener
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
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaQueueItem
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.SessionManager
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.common.images.WebImage

@Suppress("DEPRECATION")
class ExoplayerService : Service(), Utilities.ArtworkListener, OnProgramationListener,
    SessionAvailabilityListener {

    private lateinit var mediaSessionConnector: MediaSessionConnector
    private lateinit var player: SimpleExoPlayer

    private lateinit var notificationManager: PlayerNotificationManager
    private lateinit var mMediaControllerCompat: MediaControllerCompat
    private lateinit var mMediaSessionCompat: MediaSessionCompat
    private lateinit var mediaSources: ConcatenatingMediaSource

    private lateinit var trackSelector: DefaultTrackSelector

    private val USER_AGENT = "Inovanex"


    private lateinit var remoteMediaClient: RemoteMediaClient


    private var currentSourceType = SourceType.AUDIO
    private var currentPlayerType = PlayerType.LOCAL

    private var lastSelectedAudioQuality = 0
    private var lastSelectedVideoQuality = 0


    private var mediaCastItems = mutableListOf<MediaQueueItem>()
    private var mediaCastInfo = mutableListOf<MediaInfo>()

    private lateinit var util: Utilities


    private var selectedMediaSourceIndex = 0
    private var mediaSourcesAudio = HashMap<Int, MediaSource>()
    private var mediaSourcesVideo = HashMap<Int, MediaSource>()
    private var allUrlsList = mutableListOf<String>()

    private var sonName = ""
    private var artistName = ""
    private var artworkBitmap: Bitmap? = null

    private lateinit var mSessionManager: SessionManager
    private lateinit var mCastContext: CastContext

    private lateinit var castPlayer: CastPlayer

    private var currentProgram: Program? = null
    private lateinit var programmHelper: ProgramationHelper

    companion object {

        const val DESTROY = "destroy"
        const val SWITCH_VIDEO_QUALITY = "quality"
        const val SWITCH_TYPE = "type"
        const val SWITCH_SOURCE = "source"

    }

    override fun onBind(intent: Intent?): IBinder? {

        return ExoServiceBinder()
    }

    override fun onCreate() {
        super.onCreate()

        util = Utilities(this, this)


        trackSelector = DefaultTrackSelector(this)


        player = SimpleExoPlayer.Builder(this).setTrackSelector(trackSelector).build()

        initMediaSources()

        initMediaSession()

        buildNotification()

        player.addListener(playerEventListener)
        player.addMetadataOutput(metadataOutput)
        player.prepare(mediaSources)
        player.playWhenReady = true


        lastSelectedVideoQuality = util.getSelectedVideoQuality() + mediaSourcesAudio.size

        mCastContext = CastContext.getSharedInstance(this)
        mSessionManager = CastContext.getSharedInstance(this).sessionManager
        castPlayer = CastPlayer(mCastContext)
        castPlayer.setSessionAvailabilityListener(this)

        programmHelper = ProgramationHelper(this, this)
        programmHelper.refresh()

        registerReceiver(onSwitchType, IntentFilter(SWITCH_TYPE))
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

            switchSourceType()

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
            startForeground(notificationId, notification, foregroundServiceType)
        }

    }

    override fun stopService(name: Intent?): Boolean {
        unregisterReceiver(onSwitchType)
        unregisterReceiver(onSwitchVideoQuality)
        return super.stopService(name)
    }

    override fun onDestroy() {


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
            Log.e("TAG", error.message.toString())

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

    private fun buildMediaInfo(mimeType: String, url: String): MediaInfo {

        val mediaData = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE)
        mediaData.putString(MediaMetadataCompat.METADATA_KEY_TITLE, sonName)
        mediaData.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artistName)
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

            mediaSessionConnector.setPlayer(player)

            // Volver a mostar la notificacion si se saco por el cast

            if (currentSourceType == SourceType.VIDEO) {
                player.seekTo(lastSelectedVideoQuality, C.TIME_UNSET)
            } else if (currentSourceType == SourceType.AUDIO) {
                player.seekTo(lastSelectedAudioQuality, C.TIME_UNSET)
            }

            player.playWhenReady = true

            val intent = Intent().apply {
                action = SWITCH_SOURCE
                putExtra("source", true)
            }
            sendBroadcast(intent)

        } else if (currentPlayerType == PlayerType.CAST) {

            mediaSessionConnector.setPlayer(castPlayer)

            val intent = Intent().apply {
                action = SWITCH_SOURCE
                putExtra("source", false)
            }
            sendBroadcast(intent)

            player.playWhenReady = false
            // ocultar notificacion del player para mostrar la del cast


            if (currentSourceType == SourceType.VIDEO) {
                remoteMediaClient.load(mediaCastInfo[lastSelectedAudioQuality + mediaSourcesVideo.size])
            } else if (currentSourceType == SourceType.AUDIO) {

                remoteMediaClient.load(mediaCastInfo[lastSelectedAudioQuality])
            }

        }

//        Log.e("tag", "selected video index $lastSelectedVideoQuality")
//        Log.e("tag", "selected audio index $lastSelectedAudioQuality")

    }

    private fun getNextReason(reason: Int): String {
        return when (reason) {
            Player.DISCONTINUITY_REASON_PERIOD_TRANSITION -> "This happens when playback automatically transitions from one item to the next."
            Player.DISCONTINUITY_REASON_SEEK -> "This happens when the current playback item changes as part of a seek operation, for example when calling Player.next"
            Player.TIMELINE_CHANGE_REASON_DYNAMIC -> "This happens when the playlist changes, e.g. if items are added, moved, or removed."
            else -> "Unknown change mediasourse reason"
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

    override fun onProgramChange(day: ProgramDay?, program: Program?) {
        currentProgram = program
    }
}