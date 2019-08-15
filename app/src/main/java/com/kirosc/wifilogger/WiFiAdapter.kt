package com.kirosc.wifilogger

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.kirosc.wifilogger.databinding.ListItemBinding
import com.kirosc.wifilogger.helper.WiFi


class WiFiAdapter(_wifiList: ArrayList<WiFi>) :
    androidx.recyclerview.widget.RecyclerView.Adapter<WiFiAdapter.ViewHolder>() {
    private val wifiList = _wifiList

    class ViewHolder(_binding: ListItemBinding) : androidx.recyclerview.widget.RecyclerView.ViewHolder(_binding.root) {
        var binding: ListItemBinding = _binding

        init {
            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding: ListItemBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.list_item, parent, false
        )

        return ViewHolder(binding)
    }

    override fun getItemCount() = wifiList.size

    override fun onBindViewHolder(holder: ViewHolder, pos: Int) {
        holder.binding.ssid = wifiList[pos].SSID
        holder.binding.bssid = wifiList[pos].BSSID
        holder.binding.level = wifiList[pos].level
        holder.binding.encryption = wifiList[pos].encryption
    }
}