package com.chordgrid;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.chordgrid.model.TuneSet;

public class TuneSlidePagerAdapter extends FragmentStatePagerAdapter {

    private final TuneSet tuneset;

    public TuneSlidePagerAdapter(FragmentManager fm, TuneSet tuneset) {
        super(fm);
        this.tuneset = tuneset;
    }

    @Override
    public Fragment getItem(int index) {
        return DisplayTuneFragment.newInstance(tuneset.get(index), index, tuneset.size());
    }

    @Override
    public int getCount() {
        return tuneset.size();
    }

}
