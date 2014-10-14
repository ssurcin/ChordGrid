package com.chordgrid.tunes;

import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ListView;

import com.chordgrid.EditableExpandableListFragment;
import com.chordgrid.R;
import com.chordgrid.model.TuneBook;
import com.chordgrid.model.TuneSet;

public class ExpandableTunesListFragment extends EditableExpandableListFragment {

    private final String TAG = "ExpandableTunesListFragment";

    /**
     * Factory-type constructor.
     */
    public static ExpandableTunesListFragment newInstance(
            final TuneBook tunebook) {
        ExpandableTunesListFragment fragment = new ExpandableTunesListFragment();
        fragment.setTuneBook(tunebook);
        return fragment;
    }

    private TunesListAdapter getTunesListAdapter() {
        return (TunesListAdapter) getAdapter();
    }

    private void setTunesListAdapter(TunesListAdapter adapter) {
        setAdapter(adapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setInflater(inflater);
        View view = inflater.inflate(R.layout.activity_tunes_layout, null);
        return view;
    }

    ;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        TuneBook tunebook = getTuneBook();
        if (tunebook != null) {
            ExpandableListView elv = (ExpandableListView) getListView();
            setTunesListAdapter(new TunesListAdapter(tunebook));
            getTunesListAdapter().setInflater(getInflater(), getActivity());
            elv.setAdapter(getTunesListAdapter());
        }
    }

    ;

    public void enterNewSet() {
        setActionMode(getActivity().startActionMode(new ModalTuneSelector()));
        getAdapter().setMode(TunesListAdapter.MODE_CREATE_SET);
    }

    public void leaveNewSet() {
        ListView listView = getListView();
        listView.clearChoices();
        listView.setChoiceMode(ListView.CHOICE_MODE_NONE);
        getAdapter().setMode(TunesListAdapter.MODE_NONE);
    }

    /**
     * ***********************************************************************
     * MultiChoiceModeListener for tune selection
     * ************************************************************************
     */

    private class ModalTuneSelector implements ListView.MultiChoiceModeListener {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            //MenuInflater inflater = getActivity().getMenuInflater();
            //inflater.inflate(R.menu.tune_select_menu, menu);
            mode.setTitle(R.string.select_tunes);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            Log.d(TAG, "Selected item = " + item.getTitle());
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            Log.d(TAG, "Closing action mode Create Set");
            TuneSet tuneset = getTunesListAdapter().getCurrentTuneSet();
            if (tuneset.size() == 0) {
                Log.d(TAG, "The current tuneset is empty, do not add it");
            } else {
                getTuneBook().add(tuneset);
                Log.d(TAG, String.format("Added tuneset \"%s\" (%s)", tuneset.toString(), tuneset.getRhythm()));
            }
            leaveNewSet();
        }

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position,
                                              long id, boolean checked) {
            final int checkedCount = getListView().getCheckedItemCount();
            switch (checkedCount) {
                case 0:
                    mode.setSubtitle(null);
                    break;
                case 1:
                    mode.setSubtitle("One item selected");
                    break;
                default:
                    mode.setSubtitle("" + checkedCount + " items selected");
                    break;
            }
        }

    }
}
