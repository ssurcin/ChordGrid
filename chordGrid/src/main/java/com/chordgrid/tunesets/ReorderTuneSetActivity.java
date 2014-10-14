package com.chordgrid.tunesets;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;

import com.chordgrid.R;
import com.chordgrid.model.Tune;
import com.chordgrid.model.TuneSet;
import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;

import java.util.ArrayList;

public class ReorderTuneSetActivity extends ActionBarActivity {

    private final static String TAG = "ReorderTuneSetActivity";

    /**
     * The displayed tune set.
     */
    private TuneSet mTuneset;

    /**
     * The array of tune wrappers for the dynamic list view.
     */
    private ArrayList<TuneReference> mTuneReferences;

    private ArrayAdapter<TuneReference> mAdapter;

    private ActionMode mActionMode;

    private DragSortListView.DropListener onDrop = new DragSortListView.DropListener() {
        @Override
        public void drop(int from, int to) {
            if (from != to) {
                TuneReference tuneRef = mAdapter.getItem(from);
                mAdapter.remove(tuneRef);
                mAdapter.insert(tuneRef, to);
            }
        }
    };

    private DragSortListView.RemoveListener onRemove = new DragSortListView.RemoveListener() {
        @Override
        public void remove(int which) {
            mAdapter.remove(mAdapter.getItem(which));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reorder_tuneset);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            setTuneset((TuneSet) extras.getParcelable(TuneSet.class.getSimpleName()));
        }

        if (mActionMode == null) {
            mActionMode = startActionMode(mActionModeCallback);
        }

        DragSortListView listView = (DragSortListView) findViewById(R.id.listview);
        mAdapter = new ArrayAdapter<TuneReference>(this, R.layout.tuneset_reorder_child_row, R.id.text, mTuneReferences);
        listView.setAdapter(mAdapter);
        listView.setDropListener(onDrop);
        listView.setRemoveListener(onRemove);

        DragSortController dragSortController = new DragSortController(listView);
        dragSortController.setDragHandleId(R.id.drag_handle);
        dragSortController.setRemoveEnabled(true);
        dragSortController.setSortEnabled(true);
        dragSortController.setDragInitMode(DragSortController.ON_DOWN);
        dragSortController.setRemoveMode(DragSortController.FLING_REMOVE);

        listView.setFloatViewManager(dragSortController);
        listView.setOnTouchListener(dragSortController);
        listView.setDragEnabled(true);
    }

    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        // Called when the action mode is created; startActionMode() was called
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.reorder_tuneset_emptymenu, menu);
            mode.setTitle(R.string.reorder_tunes);
            return true;
        }

        // Called each time the action mode is shown. Always called after onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false; // Return false if nothing is done
        }

        // Called when the user selects a contextual menu item
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                //case R.id.menu_share:
                //    shareCurrentItem();
                //    mode.finish(); // Action picked, so close the CAB
                //    return true;
                default:
                    return false;
            }
        }

        // Called when the user exits the action mode
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            Log.d(TAG, "End of tuneset reordering action mode");
            ArrayList<Tune> reorderedTunes = new ArrayList<Tune>(mAdapter.getCount());
            for (int i = 0; i < mAdapter.getCount(); i++) {
                reorderedTunes.add(mAdapter.getItem(i).getTune());
            }
            mTuneset.setTunes(reorderedTunes);

            // Puts the data into a parcel to get it back to the calling activity
            Intent data = new Intent();
            data.putExtra("tuneset", mTuneset);
            setResult(Activity.RESULT_OK, data);

            mActionMode = null;
            finish();
        }
    };

    private void setTuneset(TuneSet tuneset) {
        mTuneset = tuneset;
        setTitle(mTuneset.toString());
        mTuneReferences = new ArrayList<TuneReference>(mTuneset.size());
        for (Tune tune : mTuneset.getTunes()) {
            mTuneReferences.add(new TuneReference(tune));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.reorder_tune_set, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A wrapper class to display the tune's title in the dynamic list view through toString() method.
     */
    private class TuneReference {
        private Tune mTune;

        public TuneReference(Tune tune) {
            mTune = tune;
        }

        public Tune getTune() {
            return mTune;
        }

        @Override
        public String toString() {
            return mTune.getTitle();
        }
    }
}
