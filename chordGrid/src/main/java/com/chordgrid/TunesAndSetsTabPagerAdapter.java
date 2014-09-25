package com.chordgrid;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.chordgrid.model.TuneBook;
import com.chordgrid.tunes.ExpandableTunesListFragment;
import com.chordgrid.tunesets.ExpandableTuneSetsListFragment;

public class TunesAndSetsTabPagerAdapter extends FragmentPagerAdapter {

    private TuneBook tuneBook;
    private ExpandableTunesListFragment tunesListFragment;
    private ExpandableTuneSetsListFragment setsListFragment;
    private boolean createdFragment;

    /**
     * Designated constructor.
     */
    public TunesAndSetsTabPagerAdapter(FragmentManager fragmentManager, TuneBook tunebook) {
        super(fragmentManager);
        setTuneBook(tunebook);
    }

    public TuneBook getTuneBook() {
        return tuneBook;
    }

    public void setTuneBook(TuneBook tuneBook) {
        this.tuneBook = tuneBook;
    }

    public boolean wasFragmentCreated() {
        return createdFragment;
    }

    @Override
    public Fragment getItem(int index) {
        switch (index) {
            case 0:
                if (tunesListFragment == null) {
                    tunesListFragment = ExpandableTunesListFragment
                            .newInstance(getTuneBook());
                    createdFragment = true;
                } else
                    createdFragment = false;
                return tunesListFragment;
            case 1:
                if (setsListFragment == null) {
                    setsListFragment = ExpandableTuneSetsListFragment
                            .newInstance(getTuneBook());
                    createdFragment = true;
                } else
                    createdFragment = false;
                return setsListFragment;
        }

        return null;
    }

    @Override
    public int getCount() {
        return 2;
    }

}
