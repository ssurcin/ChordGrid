package com.chordgrid.tunes;

import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.chordgrid.R;
import com.chordgrid.model.Measure;
import com.chordgrid.model.Tune;
import com.chordgrid.model.TuneSet;

import java.util.List;

public class TuneGridFragment extends Fragment {

    public static final String TAG = "TuneGridFragment";

    /**
     * The key to retrieve a Tune in a bundle.
     */
    private static final String BUNDLE_KEY_TUNE = "tune";

    /**
     * The key to retrieve a TuneSet in a bundle.
     */
    private static final String BUNDLE_KEY_TUNESET = "tuneset";

    private TuneProvider mTuneProvider;

    /**
     * The displayed tune.
     */
    private Tune mTune;

    /**
     * The set in which the tune is displayed (may be null).
     */
    private TuneSet mTuneSet;

    /**
     * This flag is raised if we are in edition mode, and false in display mode.
     */
    private boolean mEditMode;

    private TuneGrid mTuneGrid;

    /**
     * Gets a new instance of {@code TuneGridFragment} initialized with data in
     * a bundle.
     *
     * @param tune     The tune to be displayed.
     * @param tuneset  The set in which this tune is displayed.
     * @param editMode A flag raised if we are editing the tune.
     * @return A new initialized instance of {@code TuneGridFragment}.
     */
    public static TuneGridFragment newInstance(Tune tune, TuneSet tuneset, boolean editMode) {
        TuneGridFragment fragment = new TuneGridFragment();

        Bundle args = new Bundle();
        args.putBoolean(DisplayTuneGridActivity.BUNDLE_KEY_EDIT, editMode);
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
        return mTune;
    }

    /**
     * Getter for the context tune set.
     */
    public TuneSet getTuneSet() {
        return mTuneSet;
    }

    public boolean getEditMode() {
        return mEditMode;
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
        mTuneProvider = (TuneProvider) getActivity();
        mTune = mTuneProvider.getTune();

        setTitle(mTune.getTitle());
        setRythm(mTune.getRhythm().getName());
        setKey(mTune.getKey());
        setTuneIndex();

        mTuneGrid = (TuneGrid) getView().findViewById(R.id.viewGrid);
        mTuneGrid.setTune(mTune);
        mTuneGrid.setOnSelectMeasureHandler(mOnSelectMeasureHandler);
        mTuneGrid.setOnSelectPartHandler(mOnSelectPartHandler);

        registerForContextMenu(mTuneGrid);
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

    private final TuneGrid.OnSelectMeasureHandler mOnSelectMeasureHandler = new TuneGrid.OnSelectMeasureHandler() {
        @Override
        public void selectMeasure(String partLabel, int lineIndex, int measureIndex) {
            final Tune tune = getTune();
            final Measure measure = tune.getMeasure(partLabel, lineIndex, measureIndex);
            Log.d(TAG, String.format("Long press on measure box '%s'", measure));
            MeasureDialogFragment dialog = MeasureDialogFragment.newInstance(measure);
            dialog.setResultHandler(new MeasureDialogFragment.MeasureDialogResultHandler() {
                @Override
                public void onTuneDialogOk(MeasureDialogFragment dialogFragment) {
                    Log.d(TAG, "Complete measure edition dialog");
                    List<String> chords = dialogFragment.getChords();
                    measure.setChords(chords);
                    Log.d(TAG, "Tune is now " + tune.toString());
                }

                @Override
                public void onTuneDialogCancel(MeasureDialogFragment dialogFragment) {
                    Log.d(TAG, "Cancelled measure edition dialog");
                }
            });
            FragmentManager fm = getActivity().getFragmentManager();
            dialog.show(fm, getString(R.string.edit_measure));
        }
    };

    private String mSelectedPartLabel;

    private final TuneGrid.OnSelectPartHandler mOnSelectPartHandler = new TuneGrid.OnSelectPartHandler() {
        @Override
        public void selectPart(String partLabel) {
            mSelectedPartLabel = partLabel;
            getActivity().openContextMenu(mTuneGrid);
        }
    };

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        getActivity().getMenuInflater().inflate(R.menu.part_edition_context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.part_edit_label:
                Log.d(TAG, "Edit part label");
                ((DisplayTuneGridActivity) getActivity()).onEditPartLabel(mSelectedPartLabel);
                return true;
            case R.id.part_delete_part:
                Log.d(TAG, "Delete part");
                return true;
            case R.id.part_add_line:
                Log.d(TAG, "Add line to part");
                return true;
        }
        return super.onContextItemSelected(item);
    }
}
