package com.chordgrid.tunes;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.chordgrid.R;
import com.chordgrid.model.Tune;
import com.chordgrid.model.TunePart;
import com.chordgrid.model.TuneSet;

public class DisplayTuneGridActivity extends ActionBarActivity {

    /**
     * The key to retrieve a Tune in a bundle.
     */
    public static final String BUNDLE_KEY_TUNE = "tune";
    /**
     * The key to retrieve a TuneSet in a bundle.
     */
    public static final String BUNDLE_KEY_TUNESET = "tuneset";
    /**
     * The key to retrieve the edit flag in a bundle.
     */
    public static final String BUNDLE_KEY_EDIT = "edit";
    /**
     * The key to retrieve the bars per line in a bundle.
     */
    public static final String BUNDLE_KEY_BARSPERLINE = "barsPerLine";
    private static final String TAG = "DisplayTuneGridActivity";

    /**
     * The ActionMode callback handling contextual commands for tune edition.
     */
    private final ActionMode.Callback mEditTuneActionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            getMenuInflater().inflate(R.menu.tune_edition_action_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_add_part:
                    addPart();
                    return true;
                case R.id.action_add_line:
                    return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {

        }
    };
    /**
     * The displayed tune.
     */
    private Tune mTune;
    /**
     * The set in which the tune is displayed (may be null).
     */
    private TuneSet mTuneset;
    /**
     * This flag is raised if we are in edition mode, and false in display mode.
     */
    private boolean mEditMode;
    /**
     * The default number of bars per line for a new tune.
     */
    private int mDefaultBarsPerLine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_tune_grid);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mTune = extras.getParcelable(BUNDLE_KEY_TUNE);
            mTuneset = extras.getParcelable(BUNDLE_KEY_TUNESET);
            mEditMode = extras.getBoolean(BUNDLE_KEY_EDIT, false);
            mDefaultBarsPerLine = extras.getInt(BUNDLE_KEY_BARSPERLINE, mTune.getMaxMeasuresPerLine());
            if (mDefaultBarsPerLine == 0)
                mDefaultBarsPerLine = 8;
            setTitle(mTune.getTitle());
        }

        if (savedInstanceState == null) {
            updateTuneFragment();
            if (mEditMode)
                startActionMode(mEditTuneActionModeCallback);
        }
    }

    private void updateTuneFragment() {
        TuneGridFragment tuneGridFragment = TuneGridFragment.newInstance(mTune, mTuneset, mEditMode);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.tunegrid_fragment, tuneGridFragment).commit();
    }

    private void addPart() {
        mTune.addPart(new TunePart(mTune, mDefaultBarsPerLine));
        updateTuneFragment();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.display_tune_grid, menu);
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

    public void onEditPartLabel(final String partLabel) {
        final DisplayTuneGridActivity activity = this;
        PartLabelDialogFragment dialog = PartLabelDialogFragment.newInstance(partLabel);
        dialog.setResultHandler(new PartLabelDialogFragment.PartLabelDialogResultHandler() {
            @Override
            public void onTuneDialogOk(PartLabelDialogFragment dialogFragment) {
                Log.d(TAG, "New part label = " + dialogFragment.getLabel());
                TunePart duplicate = mTune.getPart(dialogFragment.getLabel());
                if (duplicate != null) {
                    Toast.makeText(activity, "A part with this label already exists! Please use another label.", Toast.LENGTH_LONG);
                    return;
                }
                TunePart part = mTune.getPart(partLabel);
                if (part != null) {
                    part.setLabel(dialogFragment.getLabel());
                    Log.d(TAG, "Tune is now " + mTune.toString());
                    updateTuneFragment();
                }
            }

            @Override
            public void onTuneDialogCancel(PartLabelDialogFragment dialogFragment) {
                Log.d(TAG, "Cancelled part label edition");
            }
        });
        dialog.show(getFragmentManager(), "Edit Label");
    }
}
