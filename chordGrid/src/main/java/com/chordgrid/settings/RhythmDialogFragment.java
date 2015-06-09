package com.chordgrid.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.chordgrid.R;
import com.chordgrid.model.Rhythm;

import java.util.ArrayList;
import java.util.Set;

/**
 * Created by sylvain.surcin@gmail.com on 03/11/2014.
 */
public class RhythmDialogFragment extends DialogFragment {

    private final ArrayList<String> mKnownRhythmNames = new ArrayList<String>();
    private final TextWatcher mNameTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            String name = s.toString();
            if (mKnownRhythmNames.contains(name)) {
                mFailureImageView.setVisibility(View.VISIBLE);
                mNameErrorMessageTextView.setText(R.string.rhythm_name_already_exists);
            } else {
                mFailureImageView.setVisibility(View.INVISIBLE);
                mNameErrorMessageTextView.setText("");
            }
        }
    };

    /**
     * The currently edited rhythm.
     */
    private Rhythm mEditedRhythm;
    /**
     * Flag set if we are creating a new rhythm .
     */
    private boolean mIsNewRhythm;
    /**
     * The edited rhythm's original name (if any).
     */
    private String mOriginName;

    private EditText mNameEditText;
    private ImageView mFailureImageView;
    private TextView mNameErrorMessageTextView;
    private EditText mNumeratorEditText;
    private EditText mDenominatorEditText;
    private EditText mBeatsPerBarEditText;
    private RhythmDialogResultHandler mResultHandler;

    public static RhythmDialogFragment newInstance(RhythmDialogResultHandler resultHandler, Rhythm rhythm) {
        RhythmDialogFragment fragment = new RhythmDialogFragment();
        fragment.setResultHandler(resultHandler);
        Bundle args = new Bundle();
        args.putParcelable("rhythm", rhythm);
        fragment.setArguments(args);
        return fragment;
    }

    public static RhythmDialogFragment newInstance(RhythmDialogResultHandler resultHandler) {
        RhythmDialogFragment fragment = new RhythmDialogFragment();
        fragment.setResultHandler(resultHandler);
        return fragment;
    }

    private void setResultHandler(RhythmDialogResultHandler resultHandler) {
        mResultHandler = resultHandler;
    }

    private String getName() {
        return mNameEditText.getText().toString();
    }

    private String getSignature() {
        return mNumeratorEditText.getText() + "/" + mDenominatorEditText.getText();
    }

    private int getBeatsPerBar() {
        return Integer.parseInt(mBeatsPerBarEditText.getText().toString());
    }

    public boolean isNewRhythm() {
        return mIsNewRhythm;
    }

    public String getOriginalName() {
        return mOriginName;
    }

    public Rhythm getRhythm() {
        return new Rhythm(getName(), getSignature(), getBeatsPerBar());
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (getArguments() != null) {
            mEditedRhythm = getArguments().getParcelable("rhythm");
            mIsNewRhythm = (mEditedRhythm != null);
        }

        SharedPreferences sharedPreferences = getActivity().getSharedPreferences(Rhythm.PREFS_NAME_CUSTOM, Context.MODE_PRIVATE);
        String serializedRhythms = sharedPreferences.getString(Rhythm.PREFS_KEY_RHYTHMS, "[]");
        Set<Rhythm> set = Rhythm.parseLines(serializedRhythms);
        for (Rhythm rhythm : set) {
            mKnownRhythmNames.add(rhythm.getName().toLowerCase());
        }

        View view = getActivity().getLayoutInflater().inflate(R.layout.rhythm_dialog_preference, null);

        final RhythmDialogFragment fragment = this;

        Dialog dialog = new AlertDialog.Builder(getActivity())
                .setView(view)
                .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (mResultHandler != null)
                                    mResultHandler.OnRhythmDialogOk(fragment);
                            }
                        })
                .setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (mResultHandler != null)
                                    mResultHandler.OnRhythmDialogCancel(fragment);
                            }
                        })
                .create();

        mNameEditText = (EditText) view.findViewById(R.id.nameEditText);
        mFailureImageView = (ImageView) view.findViewById(R.id.nameFailImageView);
        mNameErrorMessageTextView = (TextView) view.findViewById(R.id.errorMessageTextView);
        mNumeratorEditText = (EditText) view.findViewById(R.id.numeratorEditText);
        mDenominatorEditText = (EditText) view.findViewById(R.id.denominatorEditText);
        mBeatsPerBarEditText = (EditText) view.findViewById(R.id.bpbEditText);

        mNameEditText.addTextChangedListener(mNameTextWatcher);

        if (mEditedRhythm != null) {
            mOriginName = mEditedRhythm.getName();
            mNameEditText.setText(mEditedRhythm.getName());
            String[] items = mEditedRhythm.getSignature().split("/");
            mNumeratorEditText.setText(items[0]);
            mDenominatorEditText.setText(items[1]);
            mBeatsPerBarEditText.setText(Integer.toString(mEditedRhythm.getBeatsPerBar()));
        }

        return dialog;
    }

    public interface RhythmDialogResultHandler {
        void OnRhythmDialogOk(RhythmDialogFragment rhythmDialogFragment);

        void OnRhythmDialogCancel(RhythmDialogFragment rhythmDialogFragment);
    }

}
