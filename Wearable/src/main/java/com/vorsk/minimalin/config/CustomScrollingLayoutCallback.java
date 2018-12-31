package com.vorsk.minimalin.config;

import android.support.v7.widget.RecyclerView;
import android.support.wear.widget.WearableLinearLayoutManager;
import android.view.View;

public class CustomScrollingLayoutCallback extends WearableLinearLayoutManager.LayoutCallback {
    // https://developer.android.com/training/wearables/ui/lists#java
    // https://stackoverflow.com/questions/38453663/how-to-create-circular-view-on-android-wear
    /** How much should we scale the icon at most. */
    private static final float MAX_ICON_PROGRESS = 0.65f;

    private float mProgressToCenter;

    @Override
    public void onLayoutFinished(View child, RecyclerView parent) {

        // Figure out % progress from top to bottom
        float centerOffset = ((float) child.getHeight() / 2.0f) / (float) parent.getHeight();
        float yRelativeToCenterOffset = (child.getY() / parent.getHeight()) + centerOffset;
        float progresstoCenter = (float) Math.sin(yRelativeToCenterOffset * Math.PI);

        // Normalize for center
        mProgressToCenter = Math.abs(0.5f - yRelativeToCenterOffset);
        // Adjust to the maximum scale
        mProgressToCenter = Math.min(mProgressToCenter, MAX_ICON_PROGRESS);

        child.setScaleX(1 - mProgressToCenter);
        child.setScaleY(1 - mProgressToCenter);
        child.setX(+(1 - progresstoCenter) * 100);
    }
}
