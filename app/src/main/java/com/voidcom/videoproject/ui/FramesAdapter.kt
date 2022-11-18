package com.voidcom.videoproject.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.voidcom.v_base.utils.dp2px
import com.voidcom.videoproject.R


/**
 *
 * @Description: java类作用描述
 * @Author: Void
 * @CreateDate: 2022/11/13 21:27
 * @UpdateDate: 2022/11/13 21:27
 */
class FramesAdapter : RecyclerView.Adapter<FramesAdapter.ViewHolder?>() {
    private val items: MutableList<String> = ArrayList()
    private var mWidth = dp2px(35f)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.frames_item_layout, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Glide.with(holder.mIv.context).load(items[position]).into(holder.mIv)
        holder.mIv.layoutParams = holder.mIv.layoutParams.apply {
            width = mWidth.toInt()
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(list: List<String>) {
        items.clear()
        items.addAll(list)
        for (i in 0 until itemCount) {
            notifyItemChanged(i)
        }
    }

    fun updateItem(position: Int, outfile: String) {
        items[position] = outfile
        notifyItemChanged(position)
    }

    fun setItemWidth(width: Float) {
        this.mWidth = width
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val mIv: ImageView = itemView.findViewById(R.id.mIv)
    }
}
