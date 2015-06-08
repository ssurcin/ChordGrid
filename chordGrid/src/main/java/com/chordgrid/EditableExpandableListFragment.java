package com.chordgrid;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import com.chordgrid.model.TuneBook;
import com.chordgrid.util.LogUtils;

/**
 * Base class for Tunes / Tune sets expandable list fragments. Provides edition
 * features (select, discard...).
 *
 * @author sylvain.surcin@gmail.com
 */
public abstract class EditableExpandableListFragment extends ListFragment {

    /**
     * The TAG identifying log messages issued from this class.
     */
    private final String TAG = "EditableExpandableListFragment";

    /**
     * The inflater used to display the resources.
     */
    private LayoutInflater inflater;

    /**
     * The current contextual action mode.
     */
    private ActionMode actionMode;

    /**
     * The list adapter.
     */
    private EditableExpandableListAdapter adapter;

    /**
     * Read accessor for the inflater.
     */
    protected LayoutInflater getInflater() {
        return inflater;
    }

    /**
     * Write accessor for the inflater.
     *
     * @param inflater The new inflater.
     */
    protected void setInflater(final LayoutInflater inflater) {
        this.inflater = inflater;
    }

    /**
     * Read accessor for the action mode.
     */
    protected ActionMode getActionMode() {
        return actionMode;
    }

    /**
     * Write accessor for the action mode.
     *
     * @param actionMode The new action mode.
     */
    protected void setActionMode(final ActionMode actionMode) {
        this.actionMode = actionMode;
    }

    /**
     * Read accessor for the associated tunebook (passed in the arguments
     * bundle).
     */
    public TuneBook getTuneBook() {
        return (TuneBook) getArguments().getParcelable("tunebook");
    }

    /**
     * Write accessor for the associated tunebook (passed in the arguments
     * bundle).
     *
     * @param tunebook The tunebook to be bundled in the arguments.
     */
    public void setTuneBook(final TuneBook tunebook) {
        Bundle args = new Bundle();
        args.putParcelable("tunebook", tunebook);
        setArguments(args);
    }

    public void setAdapter(EditableExpandableListAdapter adapter) {
        this.adapter = adapter;
    }

    public EditableExpandableListAdapter getAdapter() {
        return adapter;
    }

    /**
     * ***********************************************************************
     * Operations
     * ************************************************************************
     */

    public void enterDiscardMode() {
        Log.d(TAG, "Entering Discard action mode");
        setActionMode(getActivity().startActionMode(new DiscardSelector()));
        getAdapter().setMode(EditableExpandableListAdapter.MODE_DISCARD);
    }

    /**
     * ***********************************************************************
     * MultiChoiceModeListener for item discarding
     * ************************************************************************
     */

    private class DiscardSelector implements ListView.MultiChoiceModeListener {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            //MenuInflater inflater = getActivity().getMenuInflater();
            //inflater.inflate(R.menu.discard_menu, menu);
            mode.setTitle(R.string.discard);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            Log.d(TAG, "Selected item = " + item.getTitle());
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            Log.d(LogUtils.getTag(), "Leaving Discard action mode");

            final EditableExpandableListAdapter adapter = getAdapter();
            final TuneBook tunebook = getTuneBook();

            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    getAdapter().setMode(EditableExpandableListAdapter.MODE_NONE);
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            tunebook.remove(adapter.getSelectedItems());
                            break;

                        case DialogInterface.BUTTON_NEGATIVE:
                            Log.d(LogUtils.getTag(), "Do not discard");
                            break;
                    }
                }
            };

            int countSelectedItems = adapter.countSelectedItems();
            if (countSelectedItems > 0) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getView().getContext());
                builder.setTitle(R.string.discard);
                builder.setMessage(getResources().getString(R.string.msg_confirm_discard, countSelectedItems));
                builder.setPositiveButton(R.string.yes, dialogClickListener);
                builder.setNegativeButton(R.string.no, dialogClickListener);
                AlertDialog alert = builder.create();
                alert.show();
            } else {
                getAdapter().setMode(EditableExpandableListAdapter.MODE_NONE);
            }
        }

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position,
                                              long id, boolean checked) {
            final int checkedCount = getListView().getCheckedItemCount();
            switch (checkedCount) {
                case 0:
                    mode.setSubtitle(null);
                    break;
                case 1:
                    mode.setSubtitle("One item selected");
                    break;
                default:
                    mode.setSubtitle("" + checkedCount + " items selected");
                    break;
            }
        }
    }
}
