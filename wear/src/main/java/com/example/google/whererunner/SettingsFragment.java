package com.example.google.whererunner;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class SettingsFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        String[] listItems = view.getResources().getStringArray(R.array.settings_list);
        ArrayAdapter listAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, listItems);

        ListView listView = (ListView) view.findViewById(R.id.settings_list);
        listView.setAdapter(listAdapter);

        return view;
    }
}
