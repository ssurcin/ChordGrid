package com.chordgrid.tunes;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.chordgrid.R;
import com.chordgrid.model.Rhythm;

import java.util.Set;

public class SelectRhythmDialogFragment extends DialogFragment {
    private static final String ARG_SELECTED_RHYTHM = "rhythm";
    private String[] mKnownRhythmNames;
    private SelectRhythmDialogResultHandler mResultHandler;
    private ListView mListView;

    public SelectRhythmDialogFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param rhythm The currently selected rhythm (may be null).
     * @return A new instance of fragment SelectRhythmDialogFragment.
     */
    public static SelectRhythmDialogFragment newInstance(Rhythm rhythm) {
        SelectRhythmDialogFragment fragment = new SelectRhythmDialogFragment();
        if (rhythm != null) {
            Bundle args = new Bundle();
            args.putString(ARG_SELECTED_RHYTHM, rhythm.getName());
            fragment.setArguments(args);
        }
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Set<Rhythm> knownRhythms = Rhythm.getKnownRhythms();
        mKnownRhythmNames = new String[knownRhythms.size()];
        int i = 0;
        for (Rhythm rhythm : knownRhythms) {
            mKnownRhythmNames[i++] = rhythm.getName();
        }

        int selectedItemIndex = -1;
        if (getArguments() != null) {
            String selectedRhythmName = getArguments().getString(ARG_SELECTED_RHYTHM);
            if (selectedRhythmName != null) {
                for (i = 0; i < mKnownRhythmNames.length; i++) {
                    if (selectedRhythmName.equalsIgnoreCase(mKnownRhythmNames[i])) {
                        selectedItemIndex = i;
                        break;
                    }
                }
            }
        }

        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_select_rhythm, null);

        final SelectRhythmDialogFragment fragment = this;

        Dialog dialog = new AlertDialog.Builder(getActivity())
                .setView(view)
                .setPositiveButton(getString(R.string.ok),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (mResultHandler != null)
                                    mResultHandler.onSelectRhythmDialogOk(fragment);
                            }
                        })
                .setNegativeButton(getString(R.string.cancel),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (mResultHandler != null)
                                    mResultHandler.onSelectRhythmDialogCancel(fragment);
                            }
                        })
                .create();

        mListView = (ListView) view.findViewById(R.id.listView);
        mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        if (selectedItemIndex >= 0)
            mListView.setItemChecked(selectedItemIndex, true);
        mListView.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_selectable_list_item, mKnownRhythmNames));

        return dialog;
    }

    /**
     * Gets the result handler (handling OK and CANCEL result).
     */
    public SelectRhythmDialogResultHandler getResultHandler() {
        return mResultHandler;
    }

    /**
     * Sets the result handler (handling OK and CANCEL result).
     */
    public void setResultHandler(SelectRhythmDialogResultHandler resultHandler) {
        mResultHandler = resultHandler;
    }

    /**
     * Gets the name of the selected rhythm.
     *
     * @return A rhythm name or null if no selection.
     */
    public String getSelectedRhythmName() {
        int index = mListView.getCheckedItemPosition();
        return index >= 0 ? mKnownRhythmNames[index] : null;
    }

    /**
     * This interface defines 2 methods to handle both possible results of the dialog: OK and CANCEL.
     */
    public interface SelectRhythmDialogResultHandler {
        public void onSelectRhythmDialogOk(SelectRhythmDialogFragment dialogFragment);

        public void onSelectRhythmDialogCancel(SelectRhythmDialogFragment dialogFragment);
    }
}
