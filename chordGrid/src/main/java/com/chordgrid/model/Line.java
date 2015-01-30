package com.chordgrid.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import com.chordgrid.MainActivity;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a line of a chord grid (i.e. there is a line feed at the end).
 *
 * @author Sylvain Surcin (sylvain.surcin@gmail.com)
 */
public class Line implements Parcelable {

    /**
     * The XML tag expected for a line.
     */
    public static final String XML_TAG = "Line";
    /**
     * The name of the repeat attribute.
     */
    public static final String XML_ATTR_REPEAT = "repeat";
    /**
     * Required to create new instances of Chord (single and arrays).
     */
    public static final Parcelable.Creator<Line> CREATOR = new Creator<Line>() {

        /**
         * Creates an array of lines.
         *
         * @param size
         *            The size of the array.
         */
        @Override
        public Line[] newArray(int size) {
            return new Line[size];
        }

        /**
         * Creates a new instance of a line from a parcel of data.
         */
        @Override
        public Line createFromParcel(Parcel source) {
            return new Line(source);
        }
    };
    /**
     * Tag for LogCat console debugging.
     */
    private static final String TAG = "com.chordgrid.model.Line";
    private boolean mRepetition;
    private List<Measure> mMeasures = new ArrayList<Measure>();

    /**************************************************************************
     * XML parsing
     *************************************************************************/

    /**
     * Reads a new instance of Line from an XML parser.
     */
    public Line(XmlPullParser parser) throws XmlPullParserException,
            IOException {
        Log.v(TAG, "Parsing an XML tag " + parser.getName());
        parser.require(XmlPullParser.START_TAG, "", XML_TAG);

        int attrCount = parser.getAttributeCount();
        for (int i = 0; i < attrCount; i++) {
            String attrName = parser.getAttributeName(i);
            String attrValue = parser.getAttributeValue(i);
            parseXmlAttribute(attrName, attrValue);
        }

        while (parser.nextTag() == XmlPullParser.START_TAG) {
            String name = parser.getName();
            if (Measure.XML_TAG.equalsIgnoreCase(name)) {
                mMeasures.add(new Measure(parser));
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

    public Line(String text) {
        int endLabel = TextUtils.indexOf(text, ')');
        if (endLabel >= 0)
            text = TextUtils.substring(text, endLabel + 1, text.length()).trim();

        if (text.startsWith("|:")) {
            mRepetition = true;
            text = text.substring(2);
            int endRepeat = text.lastIndexOf(":|");
            if (endRepeat > -1)
                text = text.substring(0, endRepeat);
        } else if (text.startsWith("|")) {
            text = text.substring(1);
        }

        String[] items = text.split(":?\\|");
        for (String item : items) {
            mMeasures.add(new Measure(item));
        }
    }

    /**
     * Empty constructor for Parcelable construction.
     */
    public Line() {
    }

    public Line(int barsPerLine) {
        for (int i = 0; i < barsPerLine; i++) {
            mMeasures.add(new Measure());
        }
    }

    /**
     * Constructor from an Android parcel of data.
     */
    protected Line(Parcel source) {
        readFromParcel(source);
    }

    public boolean hasRepetition() {
        return mRepetition;
    }

    public int countMeasures() {
        return mMeasures.size();
    }

    public List<Measure> getMeasures() {
        return mMeasures;
    }

    public Measure getMeasure(int index) {
        return mMeasures.get(index);
    }

    public int getIndex(TunePart part) {
        for (int i = 0; i < part.getLines().size(); i++) {
            Line l = part.getLine(i);
            if (l.equals(this))
                return i;
        }
        return -1;
    }

    /**************************************************************************
     * Parcelable implementation
     *************************************************************************/

    /**
     * Parsing one of the Line attributes.
     *
     * @param attrName  The attribute name.
     * @param attrValue The attribute value.
     */
    private void parseXmlAttribute(String attrName, String attrValue) {
        Log.d(MainActivity.TAG, String.format("parse Tune attribute %s=\"%s\"",
                attrName, attrValue));
        if (XML_ATTR_REPEAT.equalsIgnoreCase(attrName)) {
            mRepetition = (!TextUtils.isEmpty(attrValue) && !"no"
                    .equalsIgnoreCase(attrValue));
        }
    }

    /**
     * ***********************************************************************
     * Serialization
     * ***********************************************************************
     */

    public void xmlSerialize(XmlSerializer xmlSerializer) throws IllegalArgumentException, IllegalStateException, IOException {
        xmlSerializer.startTag("", XML_TAG);
        xmlSerializer.attribute("", XML_ATTR_REPEAT, hasRepetition() ? "yes" : "no");
        for (Measure measure : mMeasures) {
            measure.xmlSerialize(xmlSerializer);
        }
        xmlSerializer.endTag("", XML_TAG);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(hasRepetition() ? "|: " : "| ");
        int count = mMeasures.size();
        for (int i = 0; i < count; i++) {
            sb.append(mMeasures.get(i).toString()).append(" ");
            if (i == count - 1 && hasRepetition())
                sb.append(":|");
            else
                sb.append("| ");
        }
        return sb.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Writes the line's contents into a parcel of data.
     *
     * @param dest  The destination parcel of data.
     * @param flags Optional flags.
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (mRepetition ? 1 : 0));
        dest.writeTypedList(mMeasures);
    }

    /**
     * Reads the line's contents from a parcel of data.
     *
     * @param source The source parcel of data.
     */
    public void readFromParcel(Parcel source) {
        mRepetition = source.readByte() != 0;
        source.readTypedList(mMeasures, Measure.CREATOR);
    }
}
