package uz.star.starmusicplayer

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import uz.star.starmusicplayer.adapter.MusicListAdapter
import uz.star.starmusicplayer.adapter.loadImage
import uz.star.starmusicplayer.data.MusicData
import uz.star.starmusicplayer.databinding.ActivityMainBinding
import uz.star.starmusicplayer.helpers.MusicPlayState
import uz.star.starmusicplayer.service.MusicPlayerService

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private val binding: ActivityMainBinding by viewBinding(ActivityMainBinding::bind)
    private val musicAdapter = MusicListAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        loadViews()
    }

    private fun loadViews() {
        binding.currentMusicName.isSelected = true
        binding.currentMusicAuthor.isSelected = true
        binding.musicList.adapter = musicAdapter

        checkPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            getCursor().onEach { musicAdapter.submitCursor(it) }.launchIn(lifecycleScope)
        }

        EventBus.currentMusic.observe(this, { loadMusicData(it) })

        musicAdapter.setOnMusicChooseListener { music, position ->
            val intent = Intent(this, MusicPlayerService::class.java)
            intent.putExtra("MUSIC", music)
            intent.putExtra("POSITION", position)
            startMusicService(intent)
        }

        binding.next.setOnClickListener {
            val intent = Intent(this, MusicPlayerService::class.java)
            intent.putExtra(MusicPlayState.NEXT.name, true)
            startMusicService(intent)
        }

        binding.previous.setOnClickListener {
            val intent = Intent(this, MusicPlayerService::class.java)
            intent.putExtra(MusicPlayState.PREV.name, true)
            startMusicService(intent)
        }

        binding.play.setOnClickListener {
            val intent = Intent(this, MusicPlayerService::class.java)
            intent.putExtra(MusicPlayState.PLAY.name, true)
            startMusicService(intent)
        }
    }

    private fun startMusicService(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun loadMusicData(music: MusicData) {
        binding.controlPanel.visibility = View.VISIBLE
        binding.musicPanel.visibility = View.VISIBLE
        binding.currentMusicName.text = music.title
        binding.currentMusicAuthor.text = music.artist
        binding.currentMusicImage.loadImage(music.imageUri, R.drawable.ic_music_black)
        if (music.isPlaying) {
            binding.play.setImageResource(R.drawable.ic_baseline_pause_circle_filled_24)
        } else {
            binding.play.setImageResource(R.drawable.ic_baseline_play_circle_filled_24)
        }
    }
}