package com.chordgrid.model;

import java.util.Observable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An abstract class subsuming all tunebook items on which the user can operate.
 * <p/>
 * Created by sylvain.surcin@gmail.com on 14/10/2014.
 */
public abstract class TunebookItem extends Observable {

    /**
     * The ID of the next instance to be created.
     */
    private static volatile AtomicInteger sNextId = new AtomicInteger(1);

    /**
     * This instance's ID.
     */
    private final int mInstanceId;

    protected TunebookItem() {
        mInstanceId = sNextId.getAndIncrement();
    }

    /**
     * Gets this instance's ID.
     */
    public int getInstanceId() {
        return mInstanceId;
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
