package uz.star.starmusicplayer.data

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize


@Parcelize
data class MusicData(
    var id: Long? = null,
    var artist: String? = null,
    var title: String? = null,
    var data: String? = null,
    var displayName: String? = null,
    var duration: Long? = null,
    var imageUri: Uri? = null,
    var isPlaying: Boolean = false
) : Parcelable