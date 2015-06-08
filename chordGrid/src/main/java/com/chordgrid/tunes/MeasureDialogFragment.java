package com.chordgrid.tunes;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import com.chordgrid.R;
import com.chordgrid.model.Measure;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Sylvain on 29/01/2015.
 */
public class MeasureDialogFragment extends DialogFragment {

    public static final String TAG = "MeasureDialogFragment";

    private static final String BUNDLE_KEY_MEASURE = "measure";

    private MeasureDialogResultHandler mResultHandler;
    private Measure mMeasure;
    private EditText[] mEditTexts;
    private ImageButton[] mDiscardButtons;

    public static MeasureDialogFragment newInstance(Measure measure) {
        MeasureDialogFragment fragment = new MeasureDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(BUNDLE_KEY_MEASURE, measure);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mMeasure = getArguments().getParcelable(BUNDLE_KEY_MEASURE);

        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_measure, null);
        final MeasureDialogFragment fragment = this;

        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setView(view)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
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

        mEditTexts = new EditText[]{
                (EditText) view.findViewById(R.id.editTextChord1),
                (EditText) view.findViewById(R.id.editTextChord2),
                (EditText) view.findViewById(R.id.editTextChord3),
                (EditText) view.findViewById(R.id.editTextChord4)};
        mDiscardButtons = new ImageButton[]{
                (ImageButton) view.findViewById(R.id.discardButton1),
                (ImageButton) view.findViewById(R.id.discardButton2),
                (ImageButton) view.findViewById(R.id.discardButton3),
                (ImageButton) view.findViewById(R.id.discardButton4)};

        // Link discard buttons to edit boxes, to know which edit box to clear
        for (int i = 0; i < 4; i++) {
            mDiscardButtons[i].setOnClickListener(new OnDiscardButtonClickListener(mEditTexts[i]));
        }

        // Fill in the edit text boxes with the measure's chords
        for (int i = 0; i < Math.min(4, mMeasure.countChords()); i++) {
            mEditTexts[i].setText(mMeasure.getChord(i).getValue());
        }

        return dialog;
    }

    public void setResultHandler(MeasureDialogResultHandler resultHandler) {
        mResultHandler = resultHandler;
    }

    public List<String> getChords() {
        ArrayList<String> chords = new ArrayList<String>();
        chords.add(mEditTexts[0].getText().toString());
        for (int i = 1; i < 4; i++) {
            String chord = mEditTexts[i].getText().toString();
            if (!TextUtils.isEmpty(chord))
                chords.add(chord);
        }
        return chords;
    }

    /**
     * This interface defines 2 methods to handle both possible results of the dialog: OK and CANCEL.
     */
    public interface MeasureDialogResultHandler {
        void onTuneDialogOk(MeasureDialogFragment dialogFragment);

        void onTuneDialogCancel(MeasureDialogFragment dialogFragment);
    }

    private class OnDiscardButtonClickListener implements View.OnClickListener {

        private EditText mEditText;

        public OnDiscardButtonClickListener(EditText editText) {
            mEditText = editText;
        }

        @Override
        public void onClick(View v) {
            mEditText.setText("");
        }
    }
}
