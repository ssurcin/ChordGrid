package com.chordgrid.tunes;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

import com.chordgrid.R;
import com.chordgrid.model.Tune;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_tune_grid);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mTune = extras.getParcelable(BUNDLE_KEY_TUNE);
            mTuneset = extras.getParcelable(BUNDLE_KEY_TUNESET);
            mEditMode = extras.getBoolean(BUNDLE_KEY_EDIT, false);
            setTitle(mTune.getTitle());
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, TuneGridFragment.newInstance(mTune, mTuneset, mEditMode)).commit();
            if (mEditMode)
                startActionMode(mEditTuneActionModeCallback);
        }
    }

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
}
