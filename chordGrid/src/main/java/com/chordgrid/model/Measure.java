package com.chordgrid.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a measure (i.e. a cell) in a chord grid line.
 *
 * @author Sylvain Surcin (sylvain.surcin@gmail.com)
 */
public class Measure implements Parcelable {

    /**
     * Tag for LogCat console debugging.
     */
    private static final String TAG = "Measure";

    private List<Chord> chords = new ArrayList<Chord>();

    /**
     * Empty constructor for Parcelable construction.
     */
    public Measure() {
    }

    /**
     * Counts the chords in this measure.
     */
    public int countChords() {
        return chords.size();
    }

    /**
     * Getter for the list of chords.
     */
    public List<Chord> getChords() {
        return chords;
    }

    /**************************************************************************
     * XML parsing
     *************************************************************************/

    /**
     * The XML tag expected for a measure.
     */
    public static final String XML_TAG = "Measure";

    /**
     * Reads a new instance of Measure from an XML parser.
     */
    public Measure(XmlPullParser parser) throws XmlPullParserException,
            IOException {
        Log.v(TAG, "Parsing an XML tag " + parser.getName());
        parser.require(XmlPullParser.START_TAG, "", XML_TAG);

        while (parser.nextTag() == XmlPullParser.START_TAG) {
            if (Chord.XML_TAG.equalsIgnoreCase(parser.getName())) {
                chords.add(new Chord(parser));
            }
        }

        // Get rid of closing tag
        parser.require(XmlPullParser.END_TAG, "", XML_TAG);
    }

    /**
     * ***********************************************************************
     * Serialization
     * ***********************************************************************
     */

    public void xmlSerialize(XmlSerializer xmlSerializer) throws IllegalArgumentException, IllegalStateException, IOException {
        xmlSerializer.startTag("", XML_TAG);
        for (Chord chord : chords) {
            chord.xmlSerialize(xmlSerializer);
        }
        xmlSerializer.endTag("", XML_TAG);
    }

    /**
     * ***********************************************************************
     * Text parsing
     * ***********************************************************************
     */

    public Measure(String text) {
        text = text.trim();
        String[] items = text.split("\\s+");
        for (String item : items) {
            chords.add(new Chord(item));
        }
    }

    @Override
    public String toString() {
        return TextUtils.join(" ", chords);
    }

    /**
     * ***********************************************************************
     * Properties
     * ***********************************************************************
     */

    public String getLargestChordText() {
        if (chords.size() == 0)
            return "";
        String largest = chords.get(0).getValue();
        for (int i = 1; i < chords.size(); i++) {
            String chord = chords.get(i).getValue();
            if (chord.length() > largest.length())
                largest = chord;
        }
        return largest;
    }

    /**************************************************************************
     * Parcelable implementation
     *************************************************************************/

    /**
     * Constructor from an Android parcel of data.
     *
     * @param source A source parcel of data.
     */
    public Measure(Parcel source) {
        readFromParcel(source);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Writes the measure's contents into a parcel of data.
     *
     * @param dest  The destination parcel of data.
     * @param flags Optional flags.
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(chords);
    }

    /**
     * Reads the measure's contents from a parcel of data.
     *
     * @param source The source parcel of data.
     */
    public void readFromParcel(Parcel source) {
        source.readTypedList(chords, Chord.CREATOR);
    }

    /**
     * Required to create new instances of Chord (single and arrays).
     */
    public static final Parcelable.Creator<Measure> CREATOR = new Creator<Measure>() {

        /**
         * Creates an array of measures.
         *
         * @param size
         *            The size of the array.
         */
        @Override
        public Measure[] newArray(int size) {
            return new Measure[size];
        }

        /**
         * Creates a new instance of a measure from a parcel of data.
         */
        @Override
        public Measure createFromParcel(Parcel source) {
            return new Measure(source);
        }
    };
}
