package com.example.mediaplayer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetMediaMetadata(private val context: Context) {
    suspend fun retrieveMetadataFromRawResource(rawResId: Int): MediaMetadata? {
        val retriever = MediaMetadataRetriever()
        val afd = context.resources.openRawResourceFd(rawResId)
        return try {
            retriever.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)

            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: "Unknown Title"
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown Artist"
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "Unknown Album"

            withContext(Dispatchers.IO) {
                // Retrieve album art
                val albumArt = retriever.embeddedPicture
                var bitmap: Bitmap? = null
                while(albumArt != null) {
                    bitmap = BitmapFactory.decodeByteArray(albumArt, 0, albumArt.size)  //메타데이터에서 가져온 image부분을 bitmap으로 변환
                    break
                }

                bitmap?.let {
                    Log.i(TAG, "bitmap = $bitmap")
                    MediaMetadata(title, artist, album, bitmap)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving metadata: ${e.message}", e)
            null
        } finally {
            retriever.release()
            afd.close()
        }
    }

    fun retrieveMetadataFromFilePath(filePath: String): MediaMetadata? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(filePath)

            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: "Unknown Title"
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown Artist"
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "Unknown Album"

            // Retrieve album art
            val albumArt = retriever.embeddedPicture
            val bitmap = if (albumArt != null) {
                BitmapFactory.decodeByteArray(albumArt, 0, albumArt.size)
            } else {
                null
            }

            MediaMetadata(title, artist, album, bitmap)
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving metadata: ${e.message}", e)
            null
        } finally {
            retriever.release()
        }
    }

    data class MediaMetadata(
        val title: String,
        val artist: String,
        val album: String,
        val albumArt: Bitmap?   //앨범 비트맵
    )

    companion object {
        val TAG = GetMediaMetadata::class.java.simpleName
    }
}