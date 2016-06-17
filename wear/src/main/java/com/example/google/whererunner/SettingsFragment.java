package com.example.google.whererunner;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class SettingsFragment extends Fragment {

    private static final String LOG_TAG = SettingsFragment.class.getSimpleName();

    private WearableListViewAdapter mListViewAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        String[] listItems = view.getResources().getStringArray(R.array.settings_list);

        mListViewAdapter = new WearableListViewAdapter(getContext(), listItems);

        WearableListView listView = (WearableListView) view.findViewById(R.id.settings_list);
        listView.setAdapter(mListViewAdapter);
        listView.setClickListener(new WearableListViewClickListener());

        return view;
    }

    private class WearableListViewClickListener implements WearableListView.ClickListener {
        @Override
        public void onClick(WearableListView.ViewHolder viewHolder) {
            Integer tag = (Integer) viewHolder.itemView.getTag();
            String[] listItems = {};

            switch (tag) {
                case 0:
                    listItems = getResources().getStringArray(R.array.settings_map_list);
                    break;
                case 1:
                    listItems = getResources().getStringArray(R.array.settings_location_list);
                    break;
            }

            mListViewAdapter.mDataset = listItems;
            mListViewAdapter.notifyDataSetChanged();
        }

        @Override
        public void onTopEmptyRegionClick() {}
    }

    private static final class WearableListViewAdapter extends WearableListView.Adapter {
        private String[] mDataset;
        private final LayoutInflater mInflater;

        public WearableListViewAdapter(Context context, String[] dataset) {
            mInflater = LayoutInflater.from(context);
            mDataset = dataset;
        }

        public static class ItemViewHolder extends WearableListView.ViewHolder {
            private TextView textView;

            public ItemViewHolder(View itemView) {
                super(itemView);
                textView = (TextView) itemView.findViewById(R.id.name);
            }
        }

        @Override
        public WearableListView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ItemViewHolder(mInflater.inflate(R.layout.list_item_settings, null));
        }

        @Override
        public void onBindViewHolder(WearableListView.ViewHolder holder, int position) {
            ItemViewHolder itemHolder = (ItemViewHolder) holder;
            TextView view = itemHolder.textView;

            view.setText(mDataset[position]);
            holder.itemView.setTag(position);
        }

        @Override
        public int getItemCount() {
            return mDataset.length;
        }
    }
}
