package com.kirosc.wifilogger

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import com.kirosc.wifilogger.Helper.WiFi

class WiFiAdapter(_context: Context, _wifiList: ArrayList<WiFi>) :
    androidx.recyclerview.widget.RecyclerView.Adapter<WiFiAdapter.ViewHolder>() {
    private val context = _context
    private val wifiList = _wifiList

    class ViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
        init {
        }
    }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getItemCount(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onBindViewHolder(p0: ViewHolder, p1: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}