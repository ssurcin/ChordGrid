package com.chordgrid.util;

/**
 * A helper class to represent a position in a list view (with groups).
 * Created by sylvain.surcin@gmail.com on 14/10/2014.
 */
public class ListViewItemPosition {

    private int mGroupPosition;
    private int mChildPosition;

    public ListViewItemPosition(int groupPosition, int childPosition) {
        mGroupPosition = groupPosition;
        mChildPosition = childPosition;
    }

    public int getGroupPosition() {
        return mGroupPosition;
    }

    public int getChildPosition() {
        return mChildPosition;
    }
}
