package com.chordgrid.tunes;

import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.TextView;

import com.chordgrid.EditableExpandableListAdapter;
import com.chordgrid.R;
import com.chordgrid.model.Rythm;
import com.chordgrid.model.Tune;
import com.chordgrid.model.TuneBook;
import com.chordgrid.model.TuneSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class TunesListAdapter extends EditableExpandableListAdapter implements
        Observer {

    /**
     * The tag for log console filters.
     */
    private static final String TAG = "TunesListAdapter";

    public static final int MODE_CREATE_SET = 2;

    private TuneSet currentTuneSet;

    public TunesListAdapter(TuneBook tunebook) {
        super(tunebook);
    }

    @Override
    public void setTuneBook(TuneBook tuneBook) {
        super.setTuneBook(tuneBook);
        clear();
        List<Rythm> rythms = tuneBook.getAllTuneRythms();
        if (rythms.size() == 0) {
            addGroup("Empty", new ArrayList<Tune>());
        } else {
            for (Rythm rythm : rythms) {
                addGroup(rythm.getName(), tuneBook.getAllTunesWithRythm(rythm));
            }
        }
    }

    @Override
    protected int getGroupRowLayoutId() {
        return R.layout.tune_group_row;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {

        if (isSelectableMode())
            return super.getChildView(groupPosition, childPosition,
                    isLastChild, convertView, parent);

        if (convertView == null)
            convertView = getInflater().inflate(R.layout.tune_child_row, null);

        CheckedTextView titleView = (CheckedTextView) convertView
                .findViewById(R.id.checkedTextViewTitle);
        TextView positionView = (TextView) convertView
                .findViewById(R.id.textViewPosition);

        // If we are coming from another mode
        if (titleView == null) {
            convertView = getInflater().inflate(R.layout.tune_child_row, null);
            titleView = (CheckedTextView) convertView
                    .findViewById(R.id.checkedTextViewTitle);
            positionView = (TextView) convertView
                    .findViewById(R.id.textViewPosition);
        }

        if (groupPosition < groups.size()) {
            ArrayList<SelectableItem> group = (ArrayList<SelectableItem>) groups
                    .get(groupPosition);
            final Tune selectedTune = (Tune) group.get(childPosition).item;
            titleView.setText(String.format("%s (%s)", selectedTune.getTitle(),
                    selectedTune.getKey()));

            int positionInSet = getCurrentTuneSet().getTuneIndex(selectedTune);
            if (positionInSet >= 0) {
                positionView.setText(String.format("%d", positionInSet + 1));
                titleView.setChecked(true);
            } else {
                positionView.setText("");
                positionView.setVisibility(View.INVISIBLE);
                titleView.setChecked(false);
            }

            OnClickListener onClickListener = null;
            switch (getMode()) {
                case MODE_NONE:
                    onClickListener = new DisplayTuneOnClickListener(selectedTune);
                    break;
                case MODE_CREATE_SET:
                    onClickListener = new CreateSetOnClickListener(selectedTune);
                    break;
            }
            convertView.setOnClickListener(onClickListener);
        }

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    public TuneSet getCurrentTuneSet() {
        if (currentTuneSet == null)
            resetCurrentTuneSet();
        return currentTuneSet;
    }

    /**
     * Set the current tuneset to a brand new one.
     */
    public void resetCurrentTuneSet() {
        currentTuneSet = new TuneSet(getTuneBook());
    }

    public void setMode(int mode) {
        if (mode != MODE_CREATE_SET)
            resetCurrentTuneSet();
        super.setMode(mode);
    }

    private class DisplayTuneOnClickListener implements OnClickListener {

        private final Tune selectedTune;

        public DisplayTuneOnClickListener(Tune selectedTune) {
            this.selectedTune = selectedTune;
        }

        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getActivity(),
                    DisplayTuneGridActivity.class);
            intent.putExtra(DisplayTuneGridActivity.BUNDLE_KEY_TUNE,
                    selectedTune);
            getActivity().startActivity(intent);
        }
    }

    private class CreateSetOnClickListener implements OnClickListener {

        private final Tune selectedTune;

        public CreateSetOnClickListener(Tune selectedTune) {
            this.selectedTune = selectedTune;
        }

        @Override
        public void onClick(View v) {
            if (getCurrentTuneSet().getTuneIndex(selectedTune) < 0) {
                getCurrentTuneSet().add(selectedTune);
                TextView positionView = (TextView) v
                        .findViewById(R.id.textViewPosition);
                positionView.setVisibility(View.VISIBLE);
                positionView.setText(String.format("%d", getCurrentTuneSet()
                        .size()));
            }
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
            Log.d(TAG, "The observed tune book has changed.");
        }
    }
}
