package it.gmariotti.recyclerview.itemanimator.demo;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;

public class CustomLinearLayoutManager extends LinearLayoutManager {
    public CustomLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);

    }

    @Override
    public boolean canScrollVertically() {
        return false;
    }
}
