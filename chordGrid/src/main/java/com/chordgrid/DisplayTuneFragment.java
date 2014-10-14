package com.chordgrid;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.chordgrid.model.Tune;

public class DisplayTuneFragment extends Fragment {

    public static DisplayTuneFragment newInstance(Tune tune, int tuneIndex, int countTunes) {
        DisplayTuneFragment fragment = new DisplayTuneFragment();

        Bundle args = new Bundle();
        args.putParcelable("tune", tune);
        args.putInt("index", tuneIndex);
        args.putInt("count", countTunes);
        fragment.setArguments(args);

        return fragment;
    }

    public Tune getTune() {
        return (Tune) getArguments().getParcelable("tune");
    }

    public int getTuneIndex() {
        return getArguments().getInt("index");
    }

    public int getCountTunes() {
        return getArguments().getInt("count");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_display_tune_set, container, false);
        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        Tune tune = getTune();
        setTitle(tune.getTitle());
        setRythm(tune.getRhythm().getName());
        setKey(tune.getKey());
        setContents(tune.getChordGrid());
        setTuneIndex();
    }

    public void setTitle(String title) {
        TextView titleTextView = (TextView) getView().findViewById(R.id.textViewTitle);
        titleTextView.setText(title);
    }

    public void setRythm(String rythm) {
        TextView rythmTextView = (TextView) getView().findViewById(R.id.textViewRythm);
        rythmTextView.setText(rythm);
    }

    public void setKey(String key) {
        TextView keyTextView = (TextView) getView().findViewById(R.id.textViewKey);
        keyTextView.setText(key);
    }

    public void setContents(String contents) {
        TextView contentsTextView = (TextView) getView().findViewById(R.id.textViewContents);
        contentsTextView.setText(contents);
    }

    public void setTuneIndex() {
        TextView indexTextView = (TextView) getView().findViewById(R.id.textViewTuneIndex);
        indexTextView.setText(String.format("%d / %d", getTuneIndex() + 1, getCountTunes()));
    }
}
