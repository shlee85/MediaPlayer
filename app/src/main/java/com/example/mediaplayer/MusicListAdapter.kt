package com.example.mediaplayer

import android.graphics.Bitmap
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mediaplayer.databinding.ItemMusicListBinding

data class MusicFile(
    val path: String,
    val title: String,
    val duration: String,
    val bitmap: Bitmap? = null,
    var isSelected: Boolean = false // 선택 여부를 표시
)

class MusicListAdapter(
    private val files: List<MusicFile>,
    private val onFileSelected: (MusicFile) -> Unit
) : RecyclerView.Adapter<MusicListAdapter.MusicFileViewHolder>() {

    lateinit var binding: ItemMusicListBinding

    //음악 리스트에서 선택했을 경우의 콜백
    interface onItemClickListener {
        fun onItemClick(click: MusicFile)
    }

    private lateinit var mClickListener: onItemClickListener
    fun setOnItemClickListener(listener: onItemClickListener) {
        mClickListener = listener
    }

    inner class MusicFileViewHolder(private val binding: ItemMusicListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MusicFile) {
            binding.fileTitle.text = item.title
            binding.fileDuration.text = item.duration
            binding.fileIcon.setImageBitmap(item.bitmap)

            Log.i(TAG, "title = ${item.title}")
            Log.i(TAG, "duration = ${item.duration}")
            Log.i(TAG, "isSelected = ${item.isSelected}")

            if(item.isSelected) {
                binding.fileContainer.setBackgroundResource(R.color.selectedBackground)
                Log.i(TAG, "selected title = ${item.title}")
                mClickListener.onItemClick(item)
            } else {
                binding.fileContainer.setBackgroundResource(android.R.color.transparent)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicFileViewHolder {
        binding = ItemMusicListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MusicFileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MusicFileViewHolder, position: Int) {
        val file = files[position]
        holder.bind(file)

        Log.i(TAG, "onBindViewHolder!")
        holder.itemView.setOnClickListener {
            onFileSelected(file)
        }
    }

    override fun getItemCount(): Int = files.size

    companion object {
        val TAG = MusicListAdapter::class.java.simpleName

    }
}