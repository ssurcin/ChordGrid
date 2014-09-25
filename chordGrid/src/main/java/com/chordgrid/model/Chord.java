package com.chordgrid.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;

/**
 * This class represents a chord from a chord grid.
 *
 * @author Sylvain Surcin (sylvain.surcin@gmail.com)
 */
public class Chord implements Parcelable {

    /**
     * Tag for LogCat console debugging.
     */
    private static final String TAG = "Chord";

    /**
     * The chord's value.
     */
    private String value;

    /**
     * Empty constructor for Parcelable construction.
     */
    public Chord() {
        value = "";
    }

    /**
     * Regular constructor.
     */
    public Chord(String chord) {
        value = chord.trim();
    }

    @Override
    public String toString() {
        return value;
    }

    /**
     * Value's getter.
     */
    public String getValue() {
        return value;
    }

    /**************************************************************************
     * XML parsing
     *************************************************************************/

    /**
     * The XML tag expected for a chord.
     */
    public static final String XML_TAG = "Chord";

    /**
     * Reads a new instance of Chord from an XML parser.
     */
    public Chord(XmlPullParser parser) throws XmlPullParserException, IOException {
        Log.v(TAG, "Parsing an XML tag " + parser.getName());
        parser.require(XmlPullParser.START_TAG, "", XML_TAG);

        value = parser.nextText().trim();
        Log.v(TAG, String.format("Read chord \"%s\"", value));

        // Work around nextText() bug that may not advance as expected
        if (parser.getEventType() != XmlPullParser.END_TAG)
            parser.nextTag();

        // Advance past closing tag
        parser.require(XmlPullParser.END_TAG, "", XML_TAG);
    }

    /**
     * ***********************************************************************
     * Serialization
     * ***********************************************************************
     */

    public void xmlSerialize(XmlSerializer xmlSerializer) throws IllegalArgumentException, IllegalStateException, IOException {
        xmlSerializer.startTag("", XML_TAG);
        xmlSerializer.text(value);
        xmlSerializer.endTag("", XML_TAG);
    }

    /**************************************************************************
     * Parcelable implementation
     *************************************************************************/

    /**
     * Constructor from an Android parcel of data.
     *
     * @param source A parcel of data.
     */
    protected Chord(Parcel source) {
        readFromParcel(source);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Writes the chord's contents into a parcel of data.
     *
     * @param dest  The destination parcel of data.
     * @param flags Optional flags.
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(value);
    }

    /**
     * Reads the chord's contents from a parcel of data.
     *
     * @param source The source parcel of data.
     */
    public void readFromParcel(Parcel source) {
        value = source.readString();
    }

    /**
     * Required to create new instances of Chord (single and arrays).
     */
    public static final Parcelable.Creator<Chord> CREATOR = new Creator<Chord>() {

        /**
         * Creates an array of chords.
         *
         * @param size
         *            The size of the array.
         */
        @Override
        public Chord[] newArray(int size) {
            return new Chord[size];
        }

        /**
         * Creates a new instance of a chord from a parcel of data.
         */
        @Override
        public Chord createFromParcel(Parcel source) {
            return new Chord(source);
        }
    };
}
