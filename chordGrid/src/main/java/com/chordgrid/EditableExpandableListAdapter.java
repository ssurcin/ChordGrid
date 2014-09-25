package com.chordgrid;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.chordgrid.model.ITuneItem;
import com.chordgrid.model.TuneBook;

import java.util.ArrayList;
import java.util.List;
import java.util.Observer;

/**
 * Abstract class adapter providing editing features.
 *
 * @author sylvain.surcin@gmail.com
 */
public abstract class EditableExpandableListAdapter extends
        BaseExpandableListAdapter implements Observer {

    public static final int MODE_NONE = 0;
    public static final int MODE_DISCARD = 1;

    private int mode;

    /**
     * The inflater used to create the group and child views.
     */
    private LayoutInflater inflater;

    /**
     * The context activity.
     */
    private Activity activity;

    private TuneBook tuneBook;

    protected final ArrayList<String> groupNames = new ArrayList<String>();

    protected class SelectableItem {
        public ITuneItem item;
        public boolean selected;

        public SelectableItem(ITuneItem item) {
            this.item = item;
            this.selected = false;
        }
    }

    protected final ArrayList<ArrayList<SelectableItem>> groups = new ArrayList<ArrayList<SelectableItem>>();

    /**
     * ***********************************************************************
     * Constructor
     * ************************************************************************
     */

    public EditableExpandableListAdapter(TuneBook tuneBook) {
        setTuneBook(tuneBook);
        setMode(MODE_NONE);
    }

    /**
     * ***********************************************************************
     * Properties
     * ************************************************************************
     */

    public TuneBook getTuneBook() {
        return tuneBook;
    }

    public void setTuneBook(TuneBook tuneBook) {
        this.tuneBook = tuneBook;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
        notifyDataSetChanged();
    }

    public boolean isSelectableMode() {
        return mode == MODE_DISCARD;
    }

    public void setInflater(LayoutInflater inflater, Activity activity) {
        this.inflater = inflater;
        this.activity = activity;
    }

    protected LayoutInflater getInflater() {
        return inflater;
    }

    protected Activity getActivity() {
        return activity;
    }

    public int countSelectedItems() {
        int count = 0;
        for (ArrayList<SelectableItem> group : groups) {
            for (SelectableItem item : group) {
                if (item.selected)
                    count++;
            }
        }
        return count;
    }

    public List<ITuneItem> getSelectedItems() {
        ArrayList<ITuneItem> items = new ArrayList<ITuneItem>();
        for (ArrayList<SelectableItem> group : groups) {
            for (SelectableItem item : group) {
                if (item.selected)
                    items.add(item.item);
            }
        }
        return items;
    }

    /**
     * ***********************************************************************
     * Operations
     * ************************************************************************
     */

    public void clear() {
        groupNames.clear();
        groups.clear();
    }

    public void addGroup(String groupName, ArrayList<? extends ITuneItem> items) {
        groupNames.add(groupName);
        ArrayList<SelectableItem> array = new ArrayList<SelectableItem>();
        for (ITuneItem item : items)
            array.add(new SelectableItem(item));
        groups.add(array);
    }

    @Override
    public int getGroupCount() {
        return groups.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return groups.get(groupPosition).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return groups.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return groups.get(groupPosition).get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return 0;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return 0;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    protected abstract int getGroupRowLayoutId();

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {
        if (convertView == null)
            convertView = inflater.inflate(getGroupRowLayoutId(), null);

        CheckedTextView checkedTextView = (CheckedTextView) convertView;
        checkedTextView.setChecked(isExpanded);

        if (groupPosition < groupNames.size()) {
            checkedTextView.setText(groupNames.get(groupPosition));
        }

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {

        if (isSelectableMode()) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.child_row_selectable,
                        null);
            }

            CheckBox checkBox = (CheckBox) convertView
                    .findViewById(R.id.selectable_item_checkBox);

            // If the checkbox is null, we come from another mode, with another
            // child row layout
            if (checkBox == null) {
                convertView = inflater.inflate(R.layout.child_row_selectable,
                        null);
                checkBox = (CheckBox) convertView
                        .findViewById(R.id.selectable_item_checkBox);
            }

            if (groupPosition < groups.size()) {
                ArrayList<SelectableItem> group = (ArrayList<SelectableItem>) groups
                        .get(groupPosition);
                final SelectableItem selectedItem = group.get(childPosition);

                checkBox.setText(selectedItem.item.getTitle());
                checkBox.setChecked(selectedItem.selected);

                checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView,
                                                 boolean isChecked) {
                        selectedItem.selected = isChecked;
                    }
                });

                convertView.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        selectedItem.selected = !selectedItem.selected;
                        CheckBox checkBox = (CheckBox) v
                                .findViewById(R.id.selectable_item_checkBox);
                        checkBox.setChecked(selectedItem.selected);
                    }
                });
            }
        }

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        if (getMode() == MODE_DISCARD)
            return true;
        return false;
    }
}
