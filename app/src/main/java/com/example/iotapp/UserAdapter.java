package com.example.iotapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.iotapp.models.Coordinates;

import java.util.List;

public class UserAdapter extends BaseAdapter {
    private final Context context;
    private final List<CompassView.UserLocation> users;

    public UserAdapter(Context context, List<CompassView.UserLocation> users) {
        this.context = context;
        this.users = users;
    }

    @Override
    public int getCount() {
        return users.size();
    }

    @Override
    public Object getItem(int position) {
        return users.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.user_list_item, parent, false);
        }

        TextView usernameTextView = convertView.findViewById(R.id.usernameTextView);
        TextView coordinatesTextView = convertView.findViewById(R.id.coordinatesTextView);

        CompassView.UserLocation user = users.get(position);
        usernameTextView.setText(user.getId());
        coordinatesTextView.setText(String.format("Lat: %.5f, Lon: %.5f", user.getLatitude(), user.getLongitude()));

        return convertView;
    }
}

