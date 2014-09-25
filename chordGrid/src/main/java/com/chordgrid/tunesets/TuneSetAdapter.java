package com.chordgrid.tunesets;

import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.chordgrid.EditableExpandableListAdapter;
import com.chordgrid.R;
import com.chordgrid.model.Rythm;
import com.chordgrid.model.TuneBook;
import com.chordgrid.model.TuneSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class TuneSetAdapter extends EditableExpandableListAdapter implements
        Observer {

    /**
     * The tag for log console filters.
     */
    private static final String TAG = "TuneSetAdapter";
    /**
     * The currently selection.
     */
    private SelectableItem selection;

    public TuneSetAdapter(TuneBook tuneBook) {
        super(tuneBook);
    }

    @Override
    public void setTuneBook(TuneBook tuneBook) {
        super.setTuneBook(tuneBook);
        getTuneBook().addObserver(this);
        updateTunebookContents();
    }

    private void updateTunebookContents() {
        clear();
        List<Rythm> rythms = getTuneBook().getAllTuneSetRythms();
        if (rythms.size() == 0) {
            addGroup("Empty", new ArrayList<TuneSet>());
        } else {
            for (Rythm rythm : getTuneBook().getAllTuneSetRythms()) {
                addGroup(rythm.getName(), getTuneBook().getAllSetsWithRythm(rythm));
            }
        }
        notifyDataSetChanged();
    }

    @Override
    protected int getGroupRowLayoutId() {
        return R.layout.tuneset_group_row;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {

        setSelection(groupPosition, childPosition);

        if (getMode() == MODE_DISCARD)
            return super.getChildView(groupPosition, childPosition,
                    isLastChild, convertView, parent);

        TextView textView = null;
        if (convertView == null)
            convertView = getInflater().inflate(R.layout.tuneset_child_row, null);
        textView = (TextView) convertView.findViewById(R.id.textViewTitle);

        if (textView == null) {
            convertView = getInflater().inflate(R.layout.tuneset_child_row, null);
            textView = (TextView) convertView.findViewById(R.id.textViewTitle);
        }

        TuneSet selectedTuneSet = getSelectedItem();
        if (selectedTuneSet != null)
            textView.setText(selectedTuneSet.toString());
        else
            textView.setText(R.string.nothing);

        ImageView reorderImageView = (ImageView) convertView.findViewById(R.id.imageViewReorder);
        reorderImageView.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                TuneSet selectedTuneSet = getSelectedItem();
                if (selectedTuneSet != null) {

                }
            }
        });

        convertView.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                TuneSet selectedTuneSet = getSelectedItem();
                if (selectedTuneSet != null) {
                    Intent intent = new Intent(getActivity(), DisplayTuneSetActivity.class);
                    intent.putExtra(TuneSet.class.getSimpleName(), selectedTuneSet);
                    getActivity().startActivity(intent);
                }
            }
        });

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    /**************************************************************************
     * Properties
     *************************************************************************/

    /**
     * Gets the current selection.
     *
     * @return A SelectableItem or null if no selection.
     */
    public SelectableItem getSelection() {
        return selection;
    }

    /**
     * Gets (directly) the selected tune set.
     *
     * @return A TuneSet or null if no selection.
     */
    public TuneSet getSelectedItem() {
        return (selection != null) ? (TuneSet) selection.item : null;
    }

    /**
     * Sets the selection to the given tune set, identified by its position within a group.
     *
     * @param groupPosition The group index.
     * @param childPosition The child index within the group.
     */
    public void setSelection(int groupPosition, int childPosition) {
        selection = null;
        if (groupPosition < groups.size()) {
            ArrayList<SelectableItem> group = groups.get(groupPosition);
            if (childPosition < group.size()) {
                selection = group.get(childPosition);
            } else {
                Log.w(TAG, String.format("setSelection(%d, %d) - child position overflow!", groupPosition, childPosition));
            }
        } else {
            Log.w(TAG, String.format("setSelection(%d, %d) - group position overflow!", groupPosition, childPosition));
        }
    }

    /**************************************************************************
     * Observer implementation
     *************************************************************************/

    /**
     * Called when one of the observable objects has been updated.
     *
     * @param observable The updated observable object.
     * @param data       Additional data to specify the change (optional).
     */
    @Override
    public void update(Observable observable, Object data) {
        if (observable == getTuneBook()) {
            if (data instanceof TuneBook.ChangedProperty) {
                TuneBook.ChangedProperty changedProperty = (TuneBook.ChangedProperty) data;
                if (changedProperty == TuneBook.ChangedProperty.TuneSets) {
                    Log.d(TAG, "The observed tunebook's sets have changed");
                    updateTunebookContents();
                }
            }
        }
    }
}
