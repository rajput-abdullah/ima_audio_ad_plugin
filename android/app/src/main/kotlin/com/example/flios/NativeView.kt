package com.example.flios
import com.google.ads.interactivemedia.v3.api.AdsRequest;
import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.View
import com.google.ads.interactivemedia.v3.api.ImaSdkFactory
import com.google.ads.interactivemedia.v3.api.ImaSdkSettings
import com.google.ads.interactivemedia.v3.api.player.ContentProgressProvider
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.MediaItem.AdsConfiguration
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.ima.ImaAdsLoader
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.MediaSourceFactory
import com.google.android.exoplayer2.source.ads.AdPlaybackState
import com.google.android.exoplayer2.source.ads.AdsLoader
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView


internal class NativeView(context: Context, id: Int, creationParams: Map<String?, Any?>?, messenger: BinaryMessenger,
                          mainActivity: MainActivity) : PlatformView,
    MethodChannel.MethodCallHandler {
    private val playerView: PlayerView
    private var adsLoader: ImaAdsLoader? = null
    private var eventListener : AdsLoader.EventListener? = null
    var player: ExoPlayer? = null
    var contentUri : String? = null
    private val methodChannel: MethodChannel
    override fun getView(): View {
        return playerView
    }
    private var sdkFactory: ImaSdkFactory? = null


    override fun dispose() {
        adsLoader!!.setPlayer(null)
        playerView.player = null
        player!!.release()
        player = null
        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }



    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "loadUrl" -> {
                contentUri = call.arguments.toString()

            }
            "pauseVideo" -> {
                player!!.pause()
            }
            "resumeVideo" -> {
            }
            else -> result.notImplemented()
        }
    }



    init {
        methodChannel = MethodChannel(messenger, "bms_video_player")
        methodChannel.setMethodCallHandler(this)
        playerView = PlayerView(context)
        adsLoader = ImaAdsLoader.Builder( /* context = */context).build()
        if (Util.SDK_INT > 23) {
            initializePlayer(id,mainActivity,creationParams,methodChannel)
        }

    }


    private fun initializePlayer(
        id: Int,
        mainActivity: MainActivity,
        creationParams: Map<String?, Any?>?,
        methodChannel: MethodChannel
    ) {

        sdkFactory = ImaSdkFactory.getInstance();
        // Set up the factory for media sources, passing the ads loader and ad view providers.
        val dataSourceFactory: DataSource.Factory =
            DefaultDataSourceFactory(view.context, Util.getUserAgent(playerView.context, "flios"))
        val mediaSourceFactory: MediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
            .setAdsLoaderProvider { unusedAdTagUri: AdsConfiguration? -> adsLoader }
            .setAdViewProvider(playerView)

        val imaSdkSettings: ImaSdkSettings? = sdkFactory?.createImaSdkSettings()
//        imaSdkSettings.language = language
        // Tell the SDK you want to control ad break playback.
        // Tell the SDK you want to control ad break playback.
        imaSdkSettings?.autoPlayAdBreaks = true

        player = ExoPlayer.Builder(view.context).setMediaSourceFactory(mediaSourceFactory).build()
        player!!.preparePlayer(playerView, true,mainActivity,methodChannel)
        playerView.player = player
        adsLoader!!.setPlayer(player)
        playerView.isControllerVisible
        playerView.setShowNextButton(false)
        playerView.setShowPreviousButton(false)
        playerView.showController()
        playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        playerView.controllerHideOnTouch=false


        // Create the MediaItem to play, specifying the content URI and ad tag URI.
       // val contentUri = Uri.parse("https://storage.googleapis.com/gvabox/media/samples/stock.mp4")
        val url = creationParams as Map<String?, Any?>?
        val contentUri = Uri.parse(url?.get("videoURL") as String?)
        val adTagUri = Uri.parse("https://pubads.g.doubleclick.net/gampad/ads?iu=/21775744923/external/"
                + "audio-preroll&ad_type=audio&sz=1x1&ciu_szs=300x250&gdfp_req=1&output=vast&"
                + "unviewed_position_start=1&env=vp&impl=s&correlator=")

        var adPlaybackState = AdPlaybackState(0, 500 * C.MICROS_PER_SECOND)
        adPlaybackState = adPlaybackState.withAdUri(0, 0, adTagUri)

        eventListener?.onAdPlaybackState(adPlaybackState);


        val mediaItem = MediaItem.Builder().setUri(contentUri).build()


        val contentStart = MediaItem.Builder().setUri(contentUri)
            .setAdsConfiguration(
                AdsConfiguration.Builder(adTagUri).build()).build()

        player!!.addMediaItem(contentStart)

        player!!.repeatMode = Player.REPEAT_MODE_ALL
        player!!.prepare()


        player!!.setPlayWhenReady(false)


    }



}