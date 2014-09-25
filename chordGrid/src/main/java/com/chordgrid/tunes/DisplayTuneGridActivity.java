package com.chordgrid.tunes;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
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
     * The displayed tune.
     */
    private Tune tune;

    /**
     * The set in which the tune is displayed (may be null).
     */
    private TuneSet tuneset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_tune_grid);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            tune = (Tune) extras.getParcelable(BUNDLE_KEY_TUNE);
            tuneset = (TuneSet) extras.getParcelable(BUNDLE_KEY_TUNESET);
            setTitle(tune.getTitle());
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, TuneGridFragment.newInstance(tune, tuneset)).commit();
        }
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
}
