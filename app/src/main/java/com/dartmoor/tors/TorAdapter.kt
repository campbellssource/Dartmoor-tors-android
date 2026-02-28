package com.dartmoor.tors

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TorAdapter(
    private val tors: List<Tor>,
    private val onTorClick: (Tor) -> Unit
) : RecyclerView.Adapter<TorAdapter.TorViewHolder>() {

    inner class TorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.tor_name)
        private val heightTextView: TextView = itemView.findViewById(R.id.tor_height)
        private val gridRefTextView: TextView = itemView.findViewById(R.id.tor_grid_ref)

        fun bind(tor: Tor) {
            nameTextView.text = tor.name
            heightTextView.text = "${tor.heightMeters}m (${tor.heightFeet}ft)"
            gridRefTextView.text = tor.gridReference
            
            itemView.setOnClickListener {
                onTorClick(tor)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tor, parent, false)
        return TorViewHolder(view)
    }

    override fun onBindViewHolder(holder: TorViewHolder, position: Int) {
        holder.bind(tors[position])
    }

    override fun getItemCount(): Int = tors.size
}
