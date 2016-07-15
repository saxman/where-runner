package com.example.google.whererunner.framework;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.wearable.view.GridViewPager;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.example.google.whererunner.R;

public class VerticalDotsPageIndicator extends LinearLayout {

    @SuppressWarnings("unused")
    private static final String LOG_TAG = VerticalDotsPageIndicator.class.getSimpleName();

    private GridViewPager mGridViewPager;
    private ImageView[] mPipImageViews;

    // XML attributes
    private int mDotSpacing;
    private int mDotDrawableId;
    private int mDotDrawableCurrentId;

    public VerticalDotsPageIndicator(Context context) {
        super(context);
    }

    public VerticalDotsPageIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.VerticalDotsPageIndicator, 0, 0);

        try {
            mDotSpacing = a.getDimensionPixelOffset(R.styleable.VerticalDotsPageIndicator_dotSpacing, 0);
            mDotDrawableId = a.getResourceId(R.styleable.VerticalDotsPageIndicator_dotDrawable, 0);
            mDotDrawableCurrentId = a.getResourceId(R.styleable.VerticalDotsPageIndicator_dotDrawableCurrent, 0);
        } finally {
            a.recycle();
        }
    }

    public void setPager(GridViewPager pager) {
        mGridViewPager = pager;
        mGridViewPager.setOnPageChangeListener(new GridViewPagerChangeListener());

        // Don't draw dots unless there is more than one page
        if (mGridViewPager.getAdapter().getRowCount() <= 1) {
            return;
        }

        mPipImageViews = new ImageView[mGridViewPager.getAdapter().getRowCount()];

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, mDotSpacing, 0, mDotSpacing);

        for (int i = 0; i < mGridViewPager.getAdapter().getRowCount(); i++) {
            ImageView view = new ImageView(getContext());
            view.setLayoutParams(layoutParams);
            view.setImageResource(mDotDrawableId);

            addView(view, layoutParams);
            mPipImageViews[i] = view;
        }

        if (mPipImageViews.length > 0) {
            mPipImageViews[0].setImageResource(R.drawable.ic_page_indicator_dot_current);
        }

        invalidate();
    }

    private class GridViewPagerChangeListener implements GridViewPager.OnPageChangeListener {
        @Override
        public void onPageSelected(int row, int col) {
            for (int j = 0; j < mPipImageViews.length; j++) {
                if (row != j) {
                    mPipImageViews[j].setImageResource(mDotDrawableId);
                } else {
                    mPipImageViews[j].setImageResource(mDotDrawableCurrentId);
                }
            }
        }

        @Override
        public void onPageScrolled(int i, int i1, float v, float v1, int i2, int i3) {}

        @Override
        public void onPageScrollStateChanged(int i) {}
    }
}
