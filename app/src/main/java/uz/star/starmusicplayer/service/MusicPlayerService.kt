package uz.star.starmusicplayer.service

import android.annotation.SuppressLint
import android.app.*
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import uz.star.starmusicplayer.EventBus
import uz.star.starmusicplayer.R
import uz.star.starmusicplayer.adapter.getMusicData
import uz.star.starmusicplayer.data.MusicData
import uz.star.starmusicplayer.getCursor
import uz.star.starmusicplayer.helpers.MusicPlayState
import java.io.File

/**
 * Created by Farhod Tohirov on 24-September-2021, 12-01
 **/

class MusicPlayerService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var mediaPlayer: MediaPlayer? = null
    private var cursor: Cursor? = null
    private var currentPosition = 0

    override fun onCreate() {
        super.onCreate()
        mediaPlayer = MediaPlayer()
        createNotificationChannel()
        mediaPlayer?.setOnCompletionListener {
            loadNextMusic()
        }
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Audio Player"
            val descriptionText = "STAR MUSIC"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(getString(R.string.default_notification_channel_id), name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }


    private fun createView(data: MusicData?): RemoteViews {
        val remote = RemoteViews(packageName, R.layout.notification_view)
        remote.setTextViewText(R.id.remoteViewName, data?.title)
        remote.setTextViewText(R.id.remoteViewAuthor, data?.artist)
        data?.imageUri?.let {
            remote.setImageViewUri(R.id.remoteViewImage, it)
        }
        if (data?.isPlaying == true) {
            remote.setImageViewResource(R.id.remotePlay, R.drawable.ic_baseline_pause_circle_filled_24)
        } else {
            remote.setImageViewResource(R.id.remotePlay, R.drawable.ic_baseline_play_circle_filled_24)
        }
        remote.setOnClickPendingIntent(R.id.remotePrevious, createActionPendingIntent(MusicPlayState.PREV))
        remote.setOnClickPendingIntent(R.id.remoteNext, createActionPendingIntent(MusicPlayState.NEXT))
        remote.setOnClickPendingIntent(R.id.remotePlay, createActionPendingIntent(MusicPlayState.PLAY))
        return remote
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun createActionPendingIntent(action: MusicPlayState): PendingIntent {
        val intent = Intent(this, MusicPlayerService::class.java)
        when (action) {
            MusicPlayState.PLAY -> {
                intent.putExtra(MusicPlayState.PLAY.name, true)
            }
            MusicPlayState.PREV -> {
            }
            MusicPlayState.NEXT -> {
                intent.putExtra(MusicPlayState.NEXT.name, true)
            }
            else -> {
            }
        }
        return PendingIntent.getService(this, action.ordinal, intent, FLAG_UPDATE_CURRENT)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (cursor == null) getCursor().onEach { cursor = it }.launchIn(serviceScope)
        val data = intent?.extras?.getParcelable("MUSIC") as? MusicData
        if (data != null) {
            val position = intent.extras?.getInt("POSITION", 0)
            currentPosition = position ?: 0
            makeMusic(data)
        }

        val isNext = intent?.getBooleanExtra(MusicPlayState.NEXT.name, false)
        if (isNext == true) loadNextMusic()


        val isPrev = intent?.getBooleanExtra(MusicPlayState.PREV.name, false)
        if (isPrev == true) loadPreviousMusic()

        val isPlay = intent?.getBooleanExtra(MusicPlayState.PLAY.name, false)
        if (isPlay == true) startStopMusic()

        return START_NOT_STICKY
    }

    private fun makeMusic(data: MusicData?) {
        data?.let {
            if (!it.isPlaying) {
                loadMusicSound(it.data ?: return@let)
                it.isPlaying = true
                updateUI(it, false)
            }
        }
    }

    private fun updateUI(data: MusicData, shouldStopAfterUpdate: Boolean) {
        EventBus.currentMusic.postValue(data)
        val notification by lazy {
            NotificationCompat.Builder(this, getString(R.string.default_notification_channel_id))
                .setSmallIcon(R.drawable.ic_music)
                .setDefaults(Notification.DEFAULT_LIGHTS or Notification.DEFAULT_VIBRATE)//Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE
                .setVibrate(LongArray(0))
                .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_music))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCustomContentView(createView(data))
                .setAutoCancel(false)
                .build()
        }
        startForeground(1, notification)
        if (shouldStopAfterUpdate) stopForeground(false)
    }

    private fun loadMusicSound(path: String) {
        mediaPlayer?.stop()
        mediaPlayer = MediaPlayer.create(this, Uri.fromFile(File(path)))
        mediaPlayer?.start()
    }

    private fun loadNextMusic() {
        currentPosition++
        val isValid = cursor?.moveToPosition(currentPosition)
        if (isValid == true) {
            getMusicAndLoad()
        } else {
            val canGo = cursor?.moveToFirst()
            if (canGo == true) {
                currentPosition = cursor?.position ?: 0
                getMusicAndLoad()
            }
        }
    }

    private fun loadPreviousMusic() {
        currentPosition--
        val isValid = cursor?.moveToPosition(currentPosition)
        if (isValid == true) {
            getMusicAndLoad()
        } else {
            if (cursor?.moveToFirst() == true) {
                currentPosition = cursor?.position ?: return
                getMusicAndLoad()
            }
        }
    }

    private fun getMusicAndLoad() {
        val music = cursor?.getMusicData(this)
        makeMusic(music)
    }

    private fun startStopMusic() {
        val isValid = cursor?.moveToPosition(currentPosition)
        if (isValid == true) {
            var shouldStopAfterUpdate = false
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                shouldStopAfterUpdate = true
            } else {
                mediaPlayer?.start()
            }
            val music = cursor?.getMusicData(this)
            music?.isPlaying = mediaPlayer?.isPlaying == true
            updateUI(music ?: return, shouldStopAfterUpdate)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(p0: Intent?): IBinder? = null
}