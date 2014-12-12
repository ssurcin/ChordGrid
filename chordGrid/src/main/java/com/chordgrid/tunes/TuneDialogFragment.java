package com.chordgrid.tunes;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.chordgrid.R;
import com.chordgrid.model.Rhythm;
import com.chordgrid.model.Tune;

import java.util.Set;

public class TuneDialogFragment extends DialogFragment {
    /**
     * The tag identifying messages from this class.
     */
    private static final String TAG = "TuneDialogFragment";

    /**
     * The fragment initialization parameter for the parcelable Tune.
     */
    private static final String ARG_TUNE = "tune";

    /**
     * The click listener in charge of the Select Rhythm button.
     * Opens a rhythm selection dialog.
     */
    private final View.OnClickListener mOnClickSelectRhythmListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final Set<Rhythm> knownRhythms = Rhythm.getKnownRhythms(getActivity());
            SelectRhythmDialogFragment rhythmDialog = SelectRhythmDialogFragment.newInstance(mTune.getRhythm());
            rhythmDialog.setResultHandler(new SelectRhythmDialogFragment.SelectRhythmDialogResultHandler() {
                @Override
                public void onSelectRhythmDialogOk(SelectRhythmDialogFragment dialogFragment) {
                    String selection = dialogFragment.getSelectedRhythmName();
                    Log.d(TAG, String.format("Selected rhythm name %s", selection));
                    for (Rhythm rhythm : knownRhythms) {
                        if (rhythm.getName().equalsIgnoreCase(selection)) {
                            mButtonRhythm.setText(selection);
                            mTune.setRhythm(rhythm);
                            return;
                        }
                    }
                    Log.d(TAG, "Unknown rhythm!");
                }

                @Override
                public void onSelectRhythmDialogCancel(SelectRhythmDialogFragment dialogFragment) {
                    Log.d(TAG, "Cancel rhythm selection");
                }
            });
            rhythmDialog.show(getFragmentManager(), getString(R.string.select_rhythm));
        }
    };

    private Tune mTune;
    private boolean mIsNewTune;
    private TuneDialogResultHandler mResultHandler;
    private EditText mEditTextTuneTitle;
    private Button mButtonRhythm;

    public TuneDialogFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment to edit of an existing tune.
     *
     * @param tune The edited tune.
     * @return A new instance of fragment TuneDialogFragment.
     */
    public static TuneDialogFragment newInstance(Tune tune) {
        TuneDialogFragment fragment = new TuneDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_TUNE, tune);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment to create a new tune.
     *
     * @return A new instance of fragment TuneDialogFragment.
     */
    public static TuneDialogFragment newInstance() {
        return new TuneDialogFragment();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (getArguments() != null) {
            mTune = getArguments().getParcelable(ARG_TUNE);
            mIsNewTune = false;
        } else {
            mTune = new Tune();
            mIsNewTune = true;
        }

        View view = getActivity().getLayoutInflater().inflate(R.layout.fragment_tune_dialog, null);

        final TuneDialogFragment fragment = this;

        Dialog tuneDialog = new AlertDialog.Builder(getActivity())
                .setView(view)
                .setPositiveButton(getString(R.string.ok),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (mResultHandler != null)
                                    mResultHandler.onTuneDialogOk(fragment);
                            }
                        })
                .setNegativeButton(getString(R.string.cancel),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (mResultHandler != null)
                                    mResultHandler.onTuneDialogCancel(fragment);
                            }
                        })
                .create();

        mEditTextTuneTitle = (EditText) view.findViewById(R.id.editTextTuneTitle);
        mButtonRhythm = (Button) view.findViewById(R.id.buttonRhythm);
        mButtonRhythm.setOnClickListener(mOnClickSelectRhythmListener);

        return tuneDialog;
    }

    /**
     * Gets the result handler (handling OK and CANCEL result).
     */
    public TuneDialogResultHandler getResultHandler() {
        return mResultHandler;
    }

    /**
     * Sets the result handler (handling OK and CANCEL result).
     */
    public void setResultHandler(TuneDialogResultHandler resultHandler) {
        mResultHandler = resultHandler;
    }

    /**
     * This interface defines 2 methods to handle both possible results of the dialog: OK and CANCEL.
     */
    public interface TuneDialogResultHandler {
        public void onTuneDialogOk(TuneDialogFragment dialogFragment);

        public void onTuneDialogCancel(TuneDialogFragment dialogFragment);
    }
}
