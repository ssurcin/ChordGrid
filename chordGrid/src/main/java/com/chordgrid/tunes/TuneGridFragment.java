package com.chordgrid.tunes;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.chordgrid.R;
import com.chordgrid.model.Tune;
import com.chordgrid.model.TuneSet;

public class TuneGridFragment extends Fragment {

    /**
     * The key to retrieve a Tune in a bundle.
     */
    private static final String BUNDLE_KEY_TUNE = "tune";

    /**
     * The key to retrieve a TuneSet in a bundle.
     */
    private static final String BUNDLE_KEY_TUNESET = "tuneset";

    /**
     * The displayed tune.
     */
    private Tune tune;

    /**
     * The set in which the tune is displayed (may be null).
     */
    private TuneSet tuneset;

    /**
     * Gets a new instance of {@code TuneGridFragment} initialized with data in
     * a bundle.
     *
     * @param tune    The tune to be displayed.
     * @param tuneset The set in which this tune is displayed.
     * @return A new initialized instance of {@code TuneGridFragment}.
     */
    public static TuneGridFragment newInstance(Tune tune, TuneSet tuneset) {
        TuneGridFragment fragment = new TuneGridFragment();

        Bundle args = new Bundle();
        args.putParcelable(BUNDLE_KEY_TUNE, tune);
        if (tuneset != null)
            args.putParcelable(BUNDLE_KEY_TUNESET, tuneset);
        fragment.setArguments(args);

        return fragment;
    }

    /**
     * Getter for the displayed tune.
     */
    public Tune getTune() {
        if (tune == null)
            tune = (Tune) getArguments().getParcelable(BUNDLE_KEY_TUNE);
        return tune;
    }

    /**
     * Getter for the context tune set.
     */
    public TuneSet getTuneSet() {
        if (tuneset == null)
            tuneset = (TuneSet) getArguments()
                    .getParcelable(BUNDLE_KEY_TUNESET);
        return tuneset;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_display_tune_grid, container,
                false);
    }

    /**
     * Updates the view contents once it has been created.
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        Tune tune = getTune();

        setTitle(tune.getTitle());
        setRythm(tune.getRhythm().getName());
        setKey(tune.getKey());
        setTuneIndex();

        TuneGrid grid = (TuneGrid) getView().findViewById(R.id.viewGrid);
        grid.setTune(tune);
    }

    /**
     * Sets the tune's title in the view top header.
     */
    public void setTitle(String title) {
        TextView titleTextView = (TextView) getView().findViewById(
                R.id.textViewTitle);
        titleTextView.setText(title);
    }

    /**
     * Sets the tune's rythm in the view top header.
     */
    public void setRythm(String rythm) {
        TextView rythmTextView = (TextView) getView().findViewById(
                R.id.textViewRythm);
        rythmTextView.setText(rythm);
    }

    /**
     * Sets the tune's key in the view top header.
     */
    public void setKey(String key) {
        TextView keyTextView = (TextView) getView().findViewById(
                R.id.textViewKey);
        keyTextView.setText(key);
    }

    /**
     * Sets the tune's title in the view top header (only if we are in a set
     * context).
     */
    public void setTuneIndex() {
        TextView indexTextView = (TextView) getView().findViewById(
                R.id.textViewTuneIndex);

        TuneSet tuneset = getTuneSet();
        if (tuneset != null) {
            Tune tune = getTune();
            indexTextView.setText(String.format("%d / %d",
                    tuneset.getTuneIndex(tune) + 1, tuneset.size()));
        } else
            indexTextView.setText("");
    }
}
