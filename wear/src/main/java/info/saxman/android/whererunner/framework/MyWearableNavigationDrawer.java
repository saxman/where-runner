package info.saxman.android.whererunner.framework;

import android.content.Context;
import android.support.wearable.internal.view.drawer.WearableNavigationDrawerPresenter;
import android.support.wearable.view.drawer.WearableDrawerLayout;
import android.support.wearable.view.drawer.WearableDrawerView;
import android.support.wearable.view.drawer.WearableNavigationDrawer;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;

public class MyWearableNavigationDrawer extends WearableNavigationDrawer {
    @SuppressWarnings("unused")
    private String LOG_TAG = MyWearableNavigationDrawer.class.getSimpleName();

    private boolean mIsInitialPeek = true;

    public MyWearableNavigationDrawer(Context context) {
        super(context);
    }

    public MyWearableNavigationDrawer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyWearableNavigationDrawer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onDrawerStateChanged(int state) {
        super.onDrawerStateChanged(state);

        // Allow the drawer to peek once, when it is peeked during the activity creation, but
        // prevent it from peeking automatically when the user attempts to close it.
        if (state == WearableDrawerView.DrawerState.STATE_IDLE && isPeeking()) {
            if (mIsInitialPeek) {
                mIsInitialPeek = false;
            } else {
                ((WearableDrawerLayout) getParent()).closeDrawer(Gravity.TOP);
            }
        }
    }
}
