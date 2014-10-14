package com.chordgrid.tunesets;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;

import com.chordgrid.EditableExpandableListFragment;
import com.chordgrid.R;
import com.chordgrid.model.TuneBook;
import com.chordgrid.model.TuneSet;
import com.chordgrid.util.ListViewItemPosition;

import java.util.List;

public class ExpandableTuneSetsListFragment extends EditableExpandableListFragment {

    private final String TAG = "ExpandableTuneSetsListFragment";

    /**
     * The expandable list view.
     */
    private ExpandableListView mListView;

    /**
     * The tune set adapter.
     */
    private TuneSetAdapter mAdapter;

    public static ExpandableTuneSetsListFragment newInstance(final TuneBook tunebook) {
        ExpandableTuneSetsListFragment fragment = new ExpandableTuneSetsListFragment();
        fragment.setTuneBook(tunebook);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setInflater(inflater);
        return inflater.inflate(R.layout.activity_tunesets_layout, null);
    }

    ;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mListView = (ExpandableListView) getListView();
        TuneBook tunebook = getTuneBook();
        if (tunebook != null) {
            mAdapter = new TuneSetAdapter(tunebook);
            setAdapter(mAdapter);
            getAdapter().setInflater(getInflater(), getActivity());
            mListView.setAdapter(mAdapter);
        }
    }

    ;

    @Override
    public void onDestroyView() {
        getTuneBook().deleteObserver(getAdapter());
        super.onDestroyView();
    }

    public void updateTuneSet(TuneSet changedTuneSet) {
        mAdapter.replaceTuneSet(mAdapter.getSelectedItem(), changedTuneSet);

        int firstVisiblePosition = mListView.getFirstVisiblePosition();
        int lastVisiblePosition = mListView.getLastVisiblePosition();

        List<ListViewItemPosition> listViewPositions = mAdapter.getListViewItemPositions(changedTuneSet);
        if (listViewPositions != null) {
            for (ListViewItemPosition itemPosition : listViewPositions) {
                int groupPosition = itemPosition.getGroupPosition();
                int childPosition = itemPosition.getChildPosition();

                int flatPosition = mListView.getFlatListPosition(ExpandableListView.getPackedPositionForChild(groupPosition, childPosition));
                if (flatPosition >= firstVisiblePosition && flatPosition <= lastVisiblePosition) {
                    View view = mListView.getChildAt(flatPosition);
                    mListView.getAdapter().getView(flatPosition, view, mListView);
                }
            }
        }
    }
}
