package com.example.locationpicker.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.locationpicker.R;
import com.example.locationpicker.model.NearbyItem;

import java.util.ArrayList;

public class NearbyListAdapter extends ArrayAdapter<NearbyItem> {
    private ArrayList<NearbyItem> data;
    private Context mContext;
    private int lastPosition = -1;


    public NearbyListAdapter(Context context, ArrayList<NearbyItem> data) {
        super(context, R.layout.nearby_list_item, data);
        this.data = data;
        this.mContext = context;

    }

    public ArrayList<NearbyItem> getData() {
        return data;
    }

    public void setData(ArrayList<NearbyItem> data) {
        this.data = data;
    }

    // View lookup cache
    private static class ViewHolder {
        TextView name;
        TextView address;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        NearbyItem dataModel = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        ViewHolder viewHolder; // view lookup cache stored in tag

        final View result;

        if (convertView == null) {

            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.nearby_list_item, parent, false);
            viewHolder.name = convertView.findViewById(R.id.nearby_name_text);
            viewHolder.address = convertView.findViewById(R.id.nearby_address_text);

            result = convertView;

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
            result = convertView;
        }
        lastPosition = position;

        viewHolder.name.setText(dataModel.getName());
        viewHolder.address.setText(dataModel.getAddress());
        // Return the completed view to render on screen
        return convertView;
    }
}
