package com.chordgrid.settings;

import android.app.Activity;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

import com.chordgrid.R;
import com.chordgrid.model.Rhythm;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

/**
 * This class is the controller for the user settings activity.
 *
 * @author sylvain.surcin@gmail.com
 */
public class UserSettingsActivity extends Activity implements RhythmDialogFragment.RhythmDialogResultHandler {

    private final static String TAG = "UserSettingsActivity";

    private UserSettingsFragment mUserSettingsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content
        mUserSettingsFragment = new UserSettingsFragment();
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, mUserSettingsFragment)
                .commit();
    }

    public static class UserSettingsFragment extends PreferenceFragment {

        private final static String TAG = "UserSettingsFragment";

        private final static String RHYTHM_ADD_KEY = "rhythm_add";
        private final static String RHYTHM_DISCARD_KEY = "rhythm_discard";
        private final static String RHYTHM_LIST_KEY = "rhythms_list";

        private PreferenceCategory mRhythmsListCategory;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.user_settings);

            PreferenceScreen rhythmsPreferenceScreen = (PreferenceScreen) findPreference("settings_rhythms");
            rhythmsPreferenceScreen.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    updateRhythmListPreference();
                    return true;
                }
            });

            UserSettingsActivity activity = (UserSettingsActivity) getActivity();

            Preference addRhythmButton = findPreference(RHYTHM_ADD_KEY);
            addRhythmButton.setOnPreferenceClickListener(new OnRhythmPreferenceClickListener(activity));

            mRhythmsListCategory = (PreferenceCategory) findPreference(RHYTHM_LIST_KEY);
        }

        /**
         * Updates the rhythm preference list to reflect known rhtyhms.
         */
        private void updateRhythmListPreference() {
            UserSettingsActivity activity = (UserSettingsActivity) getActivity();

            Set<Rhythm> knownRhythms = Rhythm.getKnownRhythms();
            ArrayList<String> rhythmNames = new ArrayList<String>(knownRhythms.size());

            for (Rhythm rhythm : knownRhythms) {
                rhythmNames.add(rhythm.getName());
                Preference pref = findPreference(rhythm.getName());
                if (pref == null) {
                    pref = new Preference(getActivity(), null);
                    pref.setKey(rhythm.getName());
                    pref.setTitle(rhythm.getName());
                    pref.setSummary(String.format("%s - %d beats per bar", rhythm.getSignature(), rhythm.getBeatsPerBar()));
                    pref.setOnPreferenceClickListener(new OnRhythmPreferenceClickListener(activity, rhythm));
                    mRhythmsListCategory.addPreference(pref);
                }
            }

            ArrayList<Preference> obsoletePreferences = new ArrayList<Preference>();
            for (int i = 0; i < mRhythmsListCategory.getPreferenceCount(); i++) {
                Preference pref = mRhythmsListCategory.getPreference(i);
                if (!rhythmNames.contains(pref.getKey()))
                    obsoletePreferences.add(pref);
            }
            for (Preference pref : obsoletePreferences) {
                mRhythmsListCategory.removePreference(pref);
            }
        }
    }

    private static class OnRhythmPreferenceClickListener implements Preference.OnPreferenceClickListener {
        private final UserSettingsActivity mActivity;
        private final Rhythm mRhythm;

        public OnRhythmPreferenceClickListener(UserSettingsActivity activity, Rhythm rhythm) {
            mActivity = activity;
            mRhythm = rhythm;
        }

        public OnRhythmPreferenceClickListener(UserSettingsActivity activity) {
            mActivity = activity;
            mRhythm = null;
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            RhythmDialogFragment dialogFragment = (mRhythm != null) ? RhythmDialogFragment.newInstance(mActivity, mRhythm) : RhythmDialogFragment.newInstance(mActivity);
            dialogFragment.show(mActivity.getFragmentManager(), "Rhythm");
            return true;
        }
    }

    public void OnRhythmDialogOk(RhythmDialogFragment rhythmDialogFragment) {
        Set<Rhythm> rhythms = Rhythm.getKnownRhythms();
        if (!rhythmDialogFragment.isNewRhythm()) {
            Iterator<Rhythm> iterator = rhythms.iterator();
            while (iterator.hasNext()) {
                Rhythm rhythm = iterator.next();
                if (rhythm.getName().equals(rhythmDialogFragment.getOriginalName())) {
                    iterator.remove();
                    break;
                }
            }
        }
        rhythms.add(rhythmDialogFragment.getRhythm());
        Rhythm.saveKnownRhythms(rhythms, getApplicationContext());
        mUserSettingsFragment.updateRhythmListPreference();
    }

    public void OnRhythmDialogCancel(RhythmDialogFragment rhythmDialogFragment) {

    }

}
