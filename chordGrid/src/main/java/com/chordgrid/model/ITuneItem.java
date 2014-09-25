package com.chordgrid.model;

/**
 * The basic interface for all items behaving like a tune (tunes, tune sets).
 *
 * @author sylvain.surcin@gmail.com
 */
public interface ITuneItem {

    /**
     * Gets a title for this item.
     */
    public String getTitle();

    /**
     * Gets the (relevant) rythm for this item.
     */
    public Rythm getRythm();
}
