package com.example.google.whererunner;

import android.content.Context;
import android.os.Bundle;
import android.app.Fragment;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.google.whererunner.model.Workout;
import com.example.google.whererunner.persistence.WorkoutDbHelper;

import java.util.ArrayList;


public class HistoryFragment extends Fragment {

    @SuppressWarnings("unused")
    private static final String LOG_TAG = SettingsFragment.class.getSimpleName();

    private WearableListViewAdapter mListViewAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        WorkoutDbHelper mDbHelper = new WorkoutDbHelper(getContext());
        ArrayList<Workout> listItems = mDbHelper.readLastFiveWorkouts();

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
            Log.i(LOG_TAG, "Clicked on " + tag);
        }

        @Override
        public void onTopEmptyRegionClick() {}
    }

    private static final class WearableListViewAdapter extends WearableListView.Adapter {
        private ArrayList<Workout> mDataset;
        private final LayoutInflater mInflater;
        private final Context mContext;

        public WearableListViewAdapter(Context context, ArrayList<Workout> dataset) {
            mInflater = LayoutInflater.from(context);
            mDataset = dataset;
            mContext = context;
        }

        public static class ItemViewHolder extends WearableListView.ViewHolder {
            private TextView textView;
            private ImageView imageView;

            public ItemViewHolder(View itemView) {
                super(itemView);
                textView = (TextView) itemView.findViewById(R.id.name);
                // imageView = (ImageView) itemView.findViewById(R.id.circle);
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
            // ImageView imageView = itemHolder.imageView;

            Workout workout = mDataset.get(position);

            String text = new java.util.Date(workout.getStartTime()).toString();
            view.setText(text);

            // imageView.setImageDrawable(mContext.getDrawable(R.drawable.ic_running_white));

            holder.itemView.setTag(position);
        }

        @Override
        public int getItemCount() {
            return mDataset.size();
        }
    }

}
