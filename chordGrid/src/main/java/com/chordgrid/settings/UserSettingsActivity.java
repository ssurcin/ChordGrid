package com.chordgrid.settings;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.chordgrid.R;

/**
 * This class is the controller for the user settings activity.
 *
 * @author sylvain.surcin@gmail.com
 */
public class UserSettingsActivity extends Activity {

    private final static String TAG = "com.chordgrid.settings.UserSettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new UserSettingsFragment())
                .commit();
    }

    public static class UserSettingsFragment extends PreferenceFragment {

        private final static String TAG = "com.chordgrid.settings.UserSettingsActivity.UserSettingsFragment";

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.user_settings);
        }
    }
}
