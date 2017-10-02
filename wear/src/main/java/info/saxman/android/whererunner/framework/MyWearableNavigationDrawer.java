package info.saxman.android.whererunner.framework;

import android.content.Context;
import android.support.wear.widget.drawer.WearableDrawerView;
import android.support.wear.widget.drawer.WearableNavigationDrawerView;
import android.util.AttributeSet;

public class MyWearableNavigationDrawer extends WearableNavigationDrawerView {
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
        if (state == WearableDrawerView.STATE_IDLE && isPeeking()) {
            if (mIsInitialPeek) {
                mIsInitialPeek = false;
            } else {
                getController().closeDrawer();
            }
        }
    }
}
