package com.kirosc.wifilogger;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Updated by Kiros Choi on 2018/04/27.
 */
class WiFiAdapter extends RecyclerView.Adapter<WiFiAdapter.ViewHolder> {

    private Context context;
    private ArrayList<WiFi> wiFiList;

    // Provide a reference to the views for each data item
    static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        TextView ssid_tv, bssid_tv, level_tv, protocol_tv;
        ViewHolder(View v) {
            super(v);
            ssid_tv = v.findViewById(R.id.ssid);
            bssid_tv = v.findViewById(R.id.bssid);
            level_tv = v.findViewById(R.id.level);
            protocol_tv = v.findViewById(R.id.protocol);
        }
    }
    /**
     * Constructor for WiFiAdapter that accepts a number of items to display and the context
     * of the activity.
     *
     * @param context of the attached Activity
     * @param wiFiArrayList contains the scanned WiFi data
     */
    public WiFiAdapter(Context context, ArrayList<WiFi> wiFiArrayList) {
        this.context = context;
        wiFiList = wiFiArrayList;
    }

    /**
     * This gets called when each new ViewHolder is created. This happens when the RecyclerView
     * is laid out. Enough ViewHolders will be created to fill the screen and allow for scrolling.
     *
     * @param parent The ViewGroup that these ViewHolders are contained within.
     * @param viewType  If the RecyclerView has more than one type of item (which mine doesn't) it
     *                  can be used to provide a different layout.
     * @return A new ViewHolder that holds the View for each list item
     */
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Create a new item
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    /**
     * OnBindViewHolder is called by the RecyclerView to display the data at the specified
     * position. In this method, it updates the contents of the ViewHolder to display the correct
     * indices in the list for this particular position, using the "position" argument that is conveniently
     * passed into it.
     *
     * @param holder   The ViewHolder which should be updated to represent the contents of the
     *                 item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // Replace the contents of the view with the WiFi scanned data
        WiFi wifi = wiFiList.get(position);
        holder.ssid_tv.setText(wifi.getsSID());
        holder.bssid_tv.setText(wifi.getbSSID());
        holder.protocol_tv.setText(wifi.getProtocol());

        int level = wifi.getLevel();
        // Set the level TextView color according to the strength
        holder.level_tv.setText(level + " dBm");
        if (level >= -67) {
            holder.level_tv.setTextColor(context.getResources().getColor(R.color.md_green_400));
        } else if (level > -80) {
            holder.level_tv.setTextColor(context.getResources().getColor(R.color.md_amber_400));
        } else {
            holder.level_tv.setTextColor(context.getResources().getColor(R.color.md_red_A400));
        }
    }

    /**
     * This method simply returns the number of items to display. It is used behind the scenes
     * to help layout our Views and for animations.
     *
     * @return The number of items available
     */
    @Override
    public int getItemCount() {
        return wiFiList.size();
    }

    /**
     * This method is used to update the RecyclerView with new data.
     *
     * @param newList The new WiFi data set
     */
    public void updateList(ArrayList<WiFi> newList) {
        wiFiList.clear();
        wiFiList.addAll(newList);
        notifyDataSetChanged();
    }
}
