package uz.star.starmusicplayer.adapter

import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import uz.star.starmusicplayer.R
import uz.star.starmusicplayer.data.MusicData
import uz.star.starmusicplayer.databinding.MusicItemBinding
import uz.star.starmusicplayer.helpers.CursorAdapter
import uz.star.starmusicplayer.songArt

/**
 * Created by Farhod Tohirov on 24-September-2021, 11-21
 **/

class MusicListAdapter : CursorAdapter<MusicListAdapter.ViewHolder>() {

    private var listenerMusicClick: DoubleBlock<MusicData, Int>? = null

    inner class ViewHolder(private val binding: MusicItemBinding) : RecyclerView.ViewHolder(binding.root) {
        init {

        }

        fun bind() {
            val data = cursor.getMusicData(binding.root.context)
            binding.musicName.text = data.title
            binding.musicAuthor.text = data.artist
            binding.musicImage.loadImage(data.imageUri)
            binding.root.setOnClickListener { listenerMusicClick?.invoke(data, bindingAdapterPosition) }
        }
    }


    fun setOnMusicChooseListener(f: DoubleBlock<MusicData, Int>) {
        listenerMusicClick = f
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(MusicItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(viewHolder: ViewHolder, cursor: Cursor?, position: Int) {
        viewHolder.bind()
    }
}

typealias DoubleBlock <T, F> = (T, F) -> Unit

fun ImageView.loadImage(data: Bitmap) {
    Glide.with(this).load(data).centerCrop().into(this)
}

fun ImageView.loadImage(data: Uri?) {
    Glide.with(this).load(data).centerCrop().placeholder(R.drawable.ic_music).error(R.drawable.ic_music).into(this)
}

const val ID = 0
const val ARTIST = 1
const val TITLE = 2
const val DATA = 3
const val DISPLAY_NAME = 4
const val DURATION = 5
const val ALBUM_ID = 6

fun Cursor.getMusicData(context: Context): MusicData {
    return MusicData(
        id = getLong(ID),
        artist = getString(ARTIST),
        title = getString(TITLE),
        data = getString(DATA),
        displayName = getString(DISPLAY_NAME),
        duration = getLong(DURATION),
        imageUri = context.songArt(getLong(ALBUM_ID))
    )
}
