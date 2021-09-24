package uz.star.starmusicplayer

import androidx.lifecycle.MutableLiveData
import uz.star.starmusicplayer.data.MusicData

/**
 * Created by Farhod Tohirov on 24-September-2021, 12-05
 **/

object EventBus {
    val currentMusic = MutableLiveData<MusicData>()
}