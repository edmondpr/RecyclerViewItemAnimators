/*
 * ******************************************************************************
 *   Copyright (c) 2014 Gabriele Mariotti.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *  *****************************************************************************
 */
package it.gmariotti.recyclerview.itemanimator.demo.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import it.gmariotti.recyclerview.itemanimator.demo.R;
import it.gmariotti.recyclerview.itemanimator.demo.models.UpdateListEvent;

/**
 * @author Gabriele Mariotti (gabri.mariotti@gmail.com)
 */
public class SimpleAdapter extends RecyclerView.Adapter<SimpleAdapter.SimpleViewHolder> {

    private final Context mContext;
    private List<String> mData;

    public void add(String s, int position) {
        mData.add(getItemCount(), s);
        notifyItemInserted(position);
        EventBus.getDefault().post(new UpdateListEvent(10f, null, true));
    }

    public void remove(int position){
        mData.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, getItemCount());
    }

    public static class SimpleViewHolder extends RecyclerView.ViewHolder {
        public final RelativeLayout containerRelativeLayout;
        public final TextView title;
        public final TextView actionRemove;

        public SimpleViewHolder(View view) {
            super(view);
            containerRelativeLayout = (RelativeLayout) view.findViewById(R.id.containerRelativeLayout);
            title = (TextView) view.findViewById(R.id.simple_text);
            actionRemove = (TextView) view.findViewById(R.id.actionRemove);
        }
    }

    public SimpleAdapter(Context context, String[] data) {
        mContext = context;
        if (data != null)
            mData = new ArrayList<String>(Arrays.asList(data));
        else mData = new ArrayList<String>();
    }

    public SimpleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(mContext).inflate(R.layout.simple_item, parent, false);
        return new SimpleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(SimpleViewHolder holder, final int position) {
        holder.title.setText(mData.get(position));
        holder.actionRemove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                remove(position);
                EventBus.getDefault().post(new UpdateListEvent(null, position, false));
            }
        });

        final RelativeLayout containerRelativeLayout = holder.containerRelativeLayout;
        ViewTreeObserver viewTreeObserver = containerRelativeLayout.getViewTreeObserver();
        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @SuppressLint("NewApi")
            @SuppressWarnings("deprecation")
            @Override
            public void onGlobalLayout() {
                float itemHeight = containerRelativeLayout.getMeasuredHeight();
                EventBus.getDefault().post(new UpdateListEvent(itemHeight, null, false));

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    containerRelativeLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    containerRelativeLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
            }

        });
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }


}
