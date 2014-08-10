/*
 * Copyright (C) 2014 Lucas Rocha
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lucasr.twowayview.widget;

import android.content.Context;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.widget.RecyclerView.Recycler;
import android.support.v7.widget.RecyclerView.State;
import android.util.AttributeSet;
import android.view.View;

public class StaggeredGridLayoutManager extends GridLayoutManager {
    private static final String LOGTAG = "StaggeredGridLayoutManager";

    private static final int DEFAULT_NUM_COLS = 2;
    private static final int DEFAULT_NUM_ROWS = 2;

    protected static class StaggeredItemEntry extends BaseLayoutManager.ItemEntry {
        private final int width;
        private final int height;

        public StaggeredItemEntry(int lane, int width, int height) {
            super(lane);
            this.width = width;
            this.height = height;
        }

        public StaggeredItemEntry(Parcel in) {
            super(in);
            this.width = in.readInt();
            this.height = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(width);
            out.writeInt(height);
        }

        public static final Parcelable.Creator<StaggeredItemEntry> CREATOR
                = new Parcelable.Creator<StaggeredItemEntry>() {
            @Override
            public StaggeredItemEntry createFromParcel(Parcel in) {
                return new StaggeredItemEntry(in);
            }

            @Override
            public StaggeredItemEntry[] newArray(int size) {
                return new StaggeredItemEntry[size];
            }
        };
    }

    public StaggeredGridLayoutManager(Context context) {
        this(context, null);
    }

    public StaggeredGridLayoutManager(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StaggeredGridLayoutManager(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle, DEFAULT_NUM_COLS, DEFAULT_NUM_ROWS);
    }

    public StaggeredGridLayoutManager(Context context, Orientation orientation,
                                      int numColumns, int numRows) {
        super(context, orientation, numColumns, numRows);
    }

    @Override
    int getLaneForPosition(int position, Direction direction) {
        int lane = Lanes.NO_LANE;

        final StaggeredItemEntry entry = (StaggeredItemEntry) getItemEntryForPosition(position);
        if (entry != null) {
            lane = entry.lane;
        }

        if (lane == Lanes.NO_LANE) {
            lane = getLanes().findLane(direction);
        }

        return lane;
    }

    @Override
    void moveLayoutToPosition(int position, int offset, Recycler recycler, State state) {
        final boolean isVertical = isVertical();
        final Lanes lanes = getLanes();
        final Rect childFrame = new Rect();

        lanes.reset(0);

        for (int i = 0; i < position; i++) {
            StaggeredItemEntry entry = (StaggeredItemEntry) getItemEntryForPosition(i);

            if (entry != null) {
                lanes.getChildFrame(entry.width, entry.height, entry.lane,
                        Direction.END, childFrame);
            } else {
                final View child = recycler.getViewForPosition(i);

                // Temporarily add view as some item decoration might assume
                // the while being measured is attached.
                addView(child);

                // XXX: This might potentially cause stalls in the main
                // thread if the layout ends up having to measure tons of
                // child views. We might need to add different policies based
                // on known assumptions regarding certain layouts e.g. child
                // views have stable aspect ratio, lane size is fixed, etc.
                measureChild(child);

                final int lane = lanes.findLane(Direction.END);
                lanes.getChildFrame(getDecoratedMeasuredWidth(child),
                        getDecoratedMeasuredHeight(child), lane, Direction.END, childFrame);

                entry = (StaggeredItemEntry) cacheItemEntry(child, i, lane, childFrame);

                // Done, now recycle view.
                removeAndRecycleView(child, recycler);
            }

            lanes.pushChildFrame(childFrame, entry.lane, Direction.END);
        }

        lanes.reset(Direction.END);
        lanes.getLane(getLaneForPosition(position, Direction.END), mTempRect);
        lanes.offset(offset - (isVertical ? mTempRect.bottom : mTempRect.right));
    }

    @Override
    ItemEntry cacheItemEntry(View child, int position, int lane, Rect childFrame) {
        StaggeredItemEntry entry = (StaggeredItemEntry) getItemEntryForPosition(position);
        if (entry == null) {
            final int width = childFrame.right - childFrame.left;
            final int height = childFrame.bottom - childFrame.top;

            entry = new StaggeredItemEntry(lane, width, height);
            setItemEntryForPosition(position, entry);
        }

        return entry;
    }
}