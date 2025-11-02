package com.example.plantpall

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PopupAdapter(private val popups: MutableList<NotificationModel>) :
    RecyclerView.Adapter<PopupAdapter.PopupViewHolder>() {

    inner class PopupViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMessage: TextView = view.findViewById(R.id.tvPopupMessage)
        val tvTime: TextView = view.findViewById(R.id.tvPopupTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PopupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_popup, parent, false)
        return PopupViewHolder(view)
    }

    override fun onBindViewHolder(holder: PopupViewHolder, position: Int) {
        val popup = popups[position]
        holder.tvMessage.text = popup.message
        holder.tvTime.text = popup.time
    }

    override fun getItemCount(): Int = popups.size

    fun removeAt(position: Int) {
        popups.removeAt(position)
        notifyItemRemoved(position)
    }

    fun clearAll() {
        popups.clear()
        notifyDataSetChanged()
    }
}
