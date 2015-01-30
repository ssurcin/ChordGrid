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
     * The XML tag expected for a measure.
     */
    public static final String XML_TAG = "Measure";
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
    /**
     * Tag for LogCat console debugging.
     */
    private static final String TAG = "Measure";
    private List<Chord> mChords = new ArrayList<Chord>();

    /**
     * Empty constructor for Parcelable construction.
     */
    public Measure() {
    }

    /**
     * Reads a new instance of Measure from an XML parser.
     */
    public Measure(XmlPullParser parser) throws XmlPullParserException,
            IOException {
        Log.v(TAG, "Parsing an XML tag " + parser.getName());
        parser.require(XmlPullParser.START_TAG, "", XML_TAG);

        while (parser.nextTag() == XmlPullParser.START_TAG) {
            if (Chord.XML_TAG.equalsIgnoreCase(parser.getName())) {
                mChords.add(new Chord(parser));
            }
        }

        // Get rid of closing tag
        parser.require(XmlPullParser.END_TAG, "", XML_TAG);
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
            mChords.add(new Chord(item));
        }
    }

    /**************************************************************************
     * XML parsing
     *************************************************************************/

    /**
     * Constructor from an Android parcel of data.
     *
     * @param source A source parcel of data.
     */
    public Measure(Parcel source) {
        readFromParcel(source);
    }

    /**
     * Counts the chords in this measure.
     */
    public int countChords() {
        return mChords.size();
    }

    /**
     * Getter for the list of chords.
     */
    public List<Chord> getChords() {
        return mChords;
    }

    public void setChords(List<String> chords) {
        mChords = new ArrayList<Chord>();
        for (String chord : chords) {
            mChords.add(new Chord(chord));
        }
    }

    public Chord getChord(int index) {
        return mChords.get(index);
    }

    /**
     * ***********************************************************************
     * Serialization
     * ***********************************************************************
     */

    public void xmlSerialize(XmlSerializer xmlSerializer) throws IllegalArgumentException, IllegalStateException, IOException {
        xmlSerializer.startTag("", XML_TAG);
        for (Chord chord : mChords) {
            chord.xmlSerialize(xmlSerializer);
        }
        xmlSerializer.endTag("", XML_TAG);
    }

    @Override
    public String toString() {
        return TextUtils.join(" ", mChords);
    }

    /**************************************************************************
     * Parcelable implementation
     *************************************************************************/

    /**
     * ***********************************************************************
     * Properties
     * ***********************************************************************
     */

    public String getLargestChordText() {
        if (mChords.size() == 0)
            return "";
        String largest = mChords.get(0).getValue();
        for (int i = 1; i < mChords.size(); i++) {
            String chord = mChords.get(i).getValue();
            if (chord.length() > largest.length())
                largest = chord;
        }
        return largest;
    }

    public int getIndexInLine(Line line) {
        for (int i = 0; i < line.getMeasures().size(); i++) {
            Measure m = line.getMeasure(i);
            if (m.equals(this))
                return i;
        }
        return -1;
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
        dest.writeTypedList(mChords);
    }

    /**
     * Reads the measure's contents from a parcel of data.
     *
     * @param source The source parcel of data.
     */
    public void readFromParcel(Parcel source) {
        source.readTypedList(mChords, Chord.CREATOR);
    }
}
