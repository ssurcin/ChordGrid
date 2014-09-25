package com.chordgrid.tunesets;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;

import com.chordgrid.EditableExpandableListFragment;
import com.chordgrid.R;
import com.chordgrid.model.TuneBook;

public class ExpandableTuneSetsListFragment extends EditableExpandableListFragment {

    private final String TAG = "ExpandableTuneSetsListFragment";

    public static ExpandableTuneSetsListFragment newInstance(final TuneBook tunebook) {
        ExpandableTuneSetsListFragment fragment = new ExpandableTuneSetsListFragment();
        fragment.setTuneBook(tunebook);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setInflater(inflater);
        View view = inflater.inflate(R.layout.activity_tunesets_layout, null);
        return view;
    }

    ;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        TuneBook tunebook = getTuneBook();
        if (tunebook != null) {
            ExpandableListView elv = (ExpandableListView) getListView();
            setAdapter(new TuneSetAdapter(tunebook));
            getAdapter().setInflater(getInflater(), getActivity());
            elv.setAdapter(getAdapter());
        }
    }

    ;

    @Override
    public void onDestroyView() {
        getTuneBook().deleteObserver(getAdapter());
        super.onDestroyView();
    }
}
