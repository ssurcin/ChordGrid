package com.chordgrid.tunes;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.chordgrid.R;
import com.chordgrid.model.Rhythm;
import com.chordgrid.model.Tune;

import java.util.Set;

public class TuneMetadataDialogFragment extends DialogFragment {
    /**
     * The tag identifying messages from this class.
     */
    private static final String TAG = "TuneDialogFragment";
    /**
     * The fragment initialization parameter for the parcelable Tune.
     */
    private static final String ARG_TUNE = "tune";
    /**
     * A text watcher to monitor events concerning the edit text fields.
     */
    private final TextWatcher mEditTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            updateOkButton(null);
        }
    };
    private TuneDialogResultHandler mResultHandler;
    private Rhythm mRhythm;
    /**
     * The click listener in charge of the Select Rhythm button.
     * Opens a rhythm selection dialog.
     */
    private final View.OnClickListener mOnClickSelectRhythmListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final Set<Rhythm> knownRhythms = Rhythm.getKnownRhythms(getActivity());
            SelectRhythmDialogFragment rhythmDialog = SelectRhythmDialogFragment.newInstance(mRhythm);
            rhythmDialog.setResultHandler(new SelectRhythmDialogFragment.SelectRhythmDialogResultHandler() {
                @Override
                public void onSelectRhythmDialogOk(SelectRhythmDialogFragment dialogFragment) {
                    String selection = dialogFragment.getSelectedRhythmName();
                    Log.d(TAG, String.format("Selected rhythm name %s", selection));
                    for (Rhythm rhythm : knownRhythms) {
                        if (rhythm.getName().equalsIgnoreCase(selection)) {
                            mButtonRhythm.setText(selection);
                            mRhythm = rhythm;
                            updateOkButton(null);
                            return;
                        }
                    }
                    Log.d(TAG, "Unknown rhythm!");
                    updateOkButton(null);
                }

                @Override
                public void onSelectRhythmDialogCancel(SelectRhythmDialogFragment dialogFragment) {
                    Log.d(TAG, "Cancel rhythm selection");
                }
            });
            rhythmDialog.show(getFragmentManager(), getString(R.string.select_rhythm));
        }
    };
    private EditText mEditTextTuneTitle;
    private Button mButtonRhythm;
    private EditText mEditTextKey;
    private EditText mEditTextBarsPerLine;

    public TuneMetadataDialogFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment to edit of an existing tune.
     *
     * @param tune The edited tune.
     * @return A new instance of fragment TuneDialogFragment.
     */
    public static TuneMetadataDialogFragment newInstance(Tune tune) {
        TuneMetadataDialogFragment fragment = new TuneMetadataDialogFragment();
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
    public static TuneMetadataDialogFragment newInstance() {
        return new TuneMetadataDialogFragment();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Tune tune = null;
        if (getArguments() != null) {
            tune = (Tune) getArguments().getParcelable(ARG_TUNE);
            mRhythm = tune.getRhythm();
        }

        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_tune, null);

        final TuneMetadataDialogFragment fragment = this;

        AlertDialog tuneDialog = new AlertDialog.Builder(getActivity())
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
        tuneDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                updateOkButton((AlertDialog) dialog);
            }
        });

        mEditTextTuneTitle = (EditText) view.findViewById(R.id.editTextTuneTitle);
        mEditTextTuneTitle.addTextChangedListener(mEditTextWatcher);

        mButtonRhythm = (Button) view.findViewById(R.id.buttonRhythm);
        mButtonRhythm.setOnClickListener(mOnClickSelectRhythmListener);

        mEditTextKey = (EditText) view.findViewById(R.id.editTextKey);
        mEditTextKey.addTextChangedListener(mEditTextWatcher);

        mEditTextBarsPerLine = (EditText) view.findViewById(R.id.editTextBarsPerLine);
        mEditTextBarsPerLine.setText(tune == null ? "8" : String.format("%d", tune.getMaxMeasuresPerLine()));
        mEditTextBarsPerLine.addTextChangedListener(mEditTextWatcher);

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
     * Tests conditions are met to enable the OK button.
     */
    private boolean canEnableOkButton() {
        return !TextUtils.isEmpty(mEditTextTuneTitle.getText().toString()) && !TextUtils.isEmpty(mEditTextKey.getText().toString()) && (getRhythm() != null) && (getBarsPerLine() > 0);
    }

    private void updateOkButton(AlertDialog alertDialog) {
        if (alertDialog == null)
            alertDialog = (AlertDialog) getDialog();
        if (alertDialog != null)
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(canEnableOkButton());
    }

    /**
     * Gets the title entered by the user.
     */
    public String getTuneTitle() {
        return mEditTextTuneTitle.getText().toString();
    }

    public Rhythm getRhythm() {
        return mRhythm;
    }

    public String getKey() {
        return mEditTextKey.getText().toString();
    }

    public int getBarsPerLine() {
        try {
            Integer n = Integer.parseInt(mEditTextBarsPerLine.getText().toString());
            return n.intValue();
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * This interface defines 2 methods to handle both possible results of the dialog: OK and CANCEL.
     */
    public interface TuneDialogResultHandler {
        public void onTuneDialogOk(TuneMetadataDialogFragment dialogFragment);

        public void onTuneDialogCancel(TuneMetadataDialogFragment dialogFragment);
    }
}
