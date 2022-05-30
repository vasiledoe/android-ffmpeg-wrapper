package com.floodin.videoeditor.base.view

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.recyclerview.widget.RecyclerView
import com.floodin.videoeditor.R
import com.floodin.videoeditor.base.data.VideoItem

class VideosAdapter(
    private var items: MutableList<VideoItem>,
    private val onEventClickedCallback: (VideoItem) -> Unit
) : RecyclerView.Adapter<VideosAdapter.ViewHolder>() {

    @SuppressLint("NotifyDataSetChanged")
    fun updateMedias(medias: List<VideoItem>) {
        items = medias.toMutableList()
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.custom_video_item,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val data = items[position]
        holder.icon.loadFromMediaItem(data)
        holder.view.setOnClickListener {
            onEventClickedCallback.invoke(data)
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val view: RelativeLayout = view.findViewById(R.id.whole_item)
        val icon: ImageView = view.findViewById(R.id.iv_thumb)
    }
}