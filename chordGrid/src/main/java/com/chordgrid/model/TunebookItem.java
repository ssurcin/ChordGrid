package com.chordgrid.model;

import java.util.Observable;

/**
 * An abstract class subsuming all tunebook items on which the user can operate.
 * <p/>
 * Created by sylvain.surcin@gmail.com on 14/10/2014.
 */
public abstract class TunebookItem extends Observable {

    protected TunebookItem() {

    }

    /**
     * Getter for rhythm.
     */
    public abstract Rhythm getRhythm();

    /**
     * Getter for the item's title.
     */
    public abstract String getTitle();
}
