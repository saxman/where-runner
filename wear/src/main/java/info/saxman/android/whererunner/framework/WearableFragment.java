package info.saxman.android.whererunner.framework;

import android.app.Fragment;
import android.os.Bundle;

/**
 * Base class for fragments embedded in WearableActivities, so that ambient mode change and UI
 * update messages can be passed from a WearableActivity to it child WearableFragments.
 *
 * Note:
 * These messages are not sent automatically; the parent WearableActivity class must call the
 * appropriate WearableFragment methods when a WearableFragment is in view.
 *
 * @see android.support.wearable.activity.WearableActivity
 */
public abstract class WearableFragment extends Fragment {
    private boolean mIsAmbient = false;

    public abstract void onUpdateAmbient();

    public void onEnterAmbient(Bundle ambientDetails) {
        mIsAmbient = true;
    }

    public void onExitAmbient() {
        mIsAmbient = false;
    }

    public boolean isAmbient() {
        return mIsAmbient;
    }
}
