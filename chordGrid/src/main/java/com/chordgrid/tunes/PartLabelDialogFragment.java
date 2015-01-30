package com.chordgrid.tunes;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.chordgrid.R;

/**
 * Created by Sylvain on 30/01/2015.
 */
public class PartLabelDialogFragment extends DialogFragment {

    private static final String TAG = "PartLabelDialogFragment";

    private static final String BUNDLE_KEY_LABEL = "label";

    private PartLabelDialogResultHandler mResultHandler;
    private EditText mLabelEditText;

    public static PartLabelDialogFragment newInstance(String label) {
        PartLabelDialogFragment fragment = new PartLabelDialogFragment();
        Bundle args = new Bundle();
        args.putString(BUNDLE_KEY_LABEL, label);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_part_label, null);
        final PartLabelDialogFragment fragment = this;

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

        mLabelEditText = (EditText) view.findViewById(R.id.editTextLabel);

        String label = getArguments().getString(BUNDLE_KEY_LABEL);
        if (label != null)
            mLabelEditText.setText(label);

        return dialog;
    }

    public String getLabel() {
        return mLabelEditText.getText().toString();
    }

    public void setResultHandler(PartLabelDialogResultHandler resultHandler) {
        mResultHandler = resultHandler;
    }

    public interface PartLabelDialogResultHandler {
        public void onTuneDialogOk(PartLabelDialogFragment dialogFragment);

        public void onTuneDialogCancel(PartLabelDialogFragment dialogFragment);
    }
}
