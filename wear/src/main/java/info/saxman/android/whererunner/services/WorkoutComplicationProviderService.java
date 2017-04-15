package info.saxman.android.whererunner.services;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationManager;
import android.support.wearable.complications.ComplicationProviderService;

import info.saxman.android.whererunner.MainActivity;
import info.saxman.android.whererunner.R;

public class WorkoutComplicationProviderService extends ComplicationProviderService {
    @Override
    public void onComplicationUpdate(int complicationId, int dataType, ComplicationManager complicationManager) {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.setAction(MainActivity.ACTION_SHOW_WORKOUT);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(), complicationId, intent, 0);

        ComplicationData complicationData = new ComplicationData.Builder(ComplicationData.TYPE_ICON)
                .setIcon(Icon.createWithResource(getApplicationContext(), R.drawable.ic_running_white))
                .setTapAction(pendingIntent)
                .build();

        complicationManager.updateComplicationData(complicationId, complicationData);
    }
}
