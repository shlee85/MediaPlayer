package com.example.mediaplayer

import android.graphics.Bitmap

data class MusicFilesInfo (
    val title: String,
    val duration: String,
    val idx: Int,
    val albumsBitmap: Bitmap? = null,
    val path: String,
    var isSelected: Boolean = false
)

object MusicPlayInfo {
    private var musicPlayList: MutableList<MusicFilesInfo> = mutableListOf()
    private var musicCurrentPlayInfo: MusicFilesInfo? = null

    @Synchronized
    fun setMusicPlayInfo(title: String, duration: String, idx: Int, albumsBitmap: Bitmap?, path: String, isSelected: Boolean) {
        musicPlayList.add(MusicFilesInfo(title, duration, idx, albumsBitmap, path, isSelected))
    }

    fun getMusicPlayInfo() : MutableList<MusicFilesInfo> {
        return musicPlayList
    }

    @Synchronized
    fun setCurrentPlayInfo(title: String, duration: String, idx: Int, albumsBitmap: Bitmap?, path: String) {
        musicCurrentPlayInfo = MusicFilesInfo(title, duration, idx, albumsBitmap, path)
    }

    fun getCurrentPlayInfo() : MusicFilesInfo? {
        return musicCurrentPlayInfo
    }
}