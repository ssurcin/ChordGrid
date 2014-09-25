package com.chordgrid.tunesets;

import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.chordgrid.R;
import com.chordgrid.TuneSlidePagerAdapter;
import com.chordgrid.model.Tune;
import com.chordgrid.model.TuneSet;
import com.chordgrid.tunes.TuneGrid;

public class DisplayTuneSetActivity extends ActionBarActivity {

    /**
     * The tag used to identify log messages from this class.
     */
    private static final String TAG = "DisplayTuneSetActivity";

    /**
     * The key for the "Paged Tunesets" user settings.
     */
    private static final String SETTING_PAGED_TUNESETS = "pagedTunesets";

    /**
     * The displayed tune set.
     */
    private TuneSet tuneSet;

    /**
     * Is the display paged (or continuous)?
     */
    private boolean pagedDisplay;

    /**
     * The pager widget, which handles animation and allows swiping horizontally
     * to access previous and next wizard steps.
     */
    private ViewPager viewPager;

    private TuneSlidePagerAdapter tuneSlidePagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "Creating DisplayTuneSetActivity");

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            tuneSet = (TuneSet) extras.getParcelable(TuneSet.class
                    .getSimpleName());
            setTitle(tuneSet.toString());
        }

        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this);
        pagedDisplay = sharedPreferences.getBoolean(SETTING_PAGED_TUNESETS,
                false);
        if (pagedDisplay) {
            Log.d(TAG, "Tuneset display is paged");
            setContentView(R.layout.activity_display_tune_set);

            viewPager = (ViewPager) findViewById(R.id.pager);
            tuneSlidePagerAdapter = new TuneSlidePagerAdapter(
                    getSupportFragmentManager(), tuneSet);
            viewPager.setAdapter(tuneSlidePagerAdapter);
        } else {
            Log.d(TAG, "Tuneset display is continuous");
            setContentView(R.layout.activity_display_tuneset_continous);

            final LinearLayout container = (LinearLayout) findViewById(R.id.container);
            for (int i = 0; i < tuneSet.size(); i++) {
                Tune tune = tuneSet.get(i);

                TextView text = new TextView(getApplicationContext());
                text.setLayoutParams(new LayoutParams(
                        LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
                text.setGravity(Gravity.CENTER_HORIZONTAL);
                text.setPadding(5, 5, 5, 10);
                text.setTextSize(18.0f);
                text.setTypeface(Typeface.DEFAULT_BOLD);
                text.setTextColor(getResources().getColor(R.color.tune_title_in_tuneset));
                text.setText(String.format("%d. %s", i + 1, tune.getTitle()));
                container.addView(text);

                TuneGrid grid = new TuneGrid(getApplicationContext());
                grid.setLayoutParams(new LayoutParams(
                        LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
                grid.setPadding(5, 5, 5, 15);
                grid.setTune(tune);
                container.addView(grid);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (viewPager.getCurrentItem() == 0) {
            // If the user is currently looking at the first step, allow the
            // system to handle the
            // Back button. This calls finish() on this activity and pops the
            // back stack.
            super.onBackPressed();
        } else {
            // Otherwise, select the previous step.
            viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.display_tune_set, menu);
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
