package com.chordgrid.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.chordgrid.MainActivity;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TunePart implements Parcelable {

    /**
     * The XML tag expected for a tune part.
     */
    public static final String XML_TAG = "Part";
    /**
     * The XML tag expected for a line's text.
     */
    public static final String XML_TEXT_TAG = "Text";
    /**
     * The name of the label attribute.
     */
    public static final String XML_ATTR_LABEL = "label";
    public static final Parcelable.Creator<TunePart> CREATOR = new Creator<TunePart>() {

        @Override
        public TunePart[] newArray(int size) {
            return new TunePart[size];
        }

        @Override
        public TunePart createFromParcel(Parcel source) {
            return new TunePart(source);
        }
    };
    /**
     * Tag for LogCat console debugging.
     */
    private static final String TAG = "TunePart";
    private static final Pattern labelPattern = Pattern
            .compile("([A-Za-z0-9]+)\\)");
    /**
     * ***********************************************************************
     * Text parsing
     * ***********************************************************************
     */

    private static Character currentLabel = 'A';
    private final List<Line> mLines = new ArrayList<Line>();
    private Tune mTune;
    private String mLabel;
    private String mChordGrid;

    /**
     * ***********************************************************************
     * XML parsing
     * ***********************************************************************
     */

    public TunePart() {
    }

    /**
     * Reads a new instance of TunePart from an XML parser.
     */
    public TunePart(Tune tune, XmlPullParser parser)
            throws XmlPullParserException, IOException {
        this.mTune = tune;
        Log.d(MainActivity.TAG, "parse TunePart tag " + parser.getName());
        parser.require(XmlPullParser.START_TAG, "", XML_TAG);

        int attrCount = parser.getAttributeCount();
        for (int i = 0; i < attrCount; i++) {
            String attrName = parser.getAttributeName(i);
            String attrValue = parser.getAttributeValue(i);
            parseXmlAttribute(attrName, attrValue);
        }

        StringBuilder sb = new StringBuilder();
        while (parser.nextTag() == XmlPullParser.START_TAG) {
            String name = parser.getName();
            if (XML_TEXT_TAG.equalsIgnoreCase(name)) {
                sb.append(parseTextTag(parser));
            } else if (Line.XML_TAG.equalsIgnoreCase(name)) {
                mLines.add(new Line(parser));
            }
        }
        mChordGrid = sb.toString();

        parser.require(XmlPullParser.END_TAG, "", XML_TAG);
    }

    /**
     * Builds a new instance of Tune from a text representation.
     */
    public TunePart(Tune tune, List<String> textLines) {
        this.mTune = tune;

        mLabel = parseLabel(textLines.get(0));
        if (mLabel == null)
            mLabel = nextLabel();

        Log.d(TAG, String.format("Reading part %s with %d lines", getLabel(),
                textLines.size()));
        for (String textLine : textLines) {
            mLines.add(new Line(textLine));
        }
        Log.d(TAG, String.format("Added %d lines to this part", mLines.size()));
    }

    public TunePart(Tune tune, int barsPerLine) {
        mLabel = nextLabel();
        mLines.add(new Line(barsPerLine));
        Log.d(TAG, String.format("Added a new line of %d bars for tune %s", barsPerLine, tune.getTitle()));
    }

    /**
     * ***********************************************************************
     * Parcelable implementation
     * ***********************************************************************
     */

    public TunePart(Parcel in) {
        readFromParcel(in);
    }

    public static String nextLabel() {
        String label = new StringBuilder().append(currentLabel).toString();
        currentLabel++;
        return label;
    }

    public static void resetNextLabel() {
        currentLabel = 'A';
    }

    public static String parseLabel(String firstLine) {
        Matcher m = labelPattern.matcher(firstLine);
        if (m.find())
            return m.group(1);
        return null;
    }

    public Tune getTune() {
        return mTune;
    }

    public void setTune(Tune tune) {
        mTune = tune;
    }

    public String getLabel() {
        return mLabel;
    }

    public void setLabel(String label) {
        mLabel = label;
    }

    public String getChordGrid() {
        return mChordGrid;
    }

    /**
     * Getter for the part lines.
     */
    public List<Line> getLines() {
        return mLines;
    }

    public Line getLine(int index) {
        return mLines.get(index);
    }

    /**
     * Parsing one of the TunePart attributes.
     *
     * @param attrName  The attribute name.
     * @param attrValue The attribute value.
     */
    private void parseXmlAttribute(String attrName, String attrValue) {
        Log.d(TAG, String.format("parse Tune attribute %s=\"%s\"", attrName,
                attrValue));
        if (XML_ATTR_LABEL.equalsIgnoreCase(attrName)) {
            Log.d(TAG, "parse TunePart label " + attrValue);
            mLabel = attrValue;
        }
    }

    /**
     * Reads the next Text tag in the Tune Part.
     *
     * @param parser The current XML parser.
     * @return The string contained in the tag, with each subline trimmed.
     * @throws XmlPullParserException
     * @throws IOException
     */
    private String parseTextTag(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        Log.v(TAG, "Parsing Text tag");
        StringBuilder sb = new StringBuilder();
        String[] textLines = parser.nextText().trim().split("\n");
        for (int i = 0; i < textLines.length; i++) {
            sb.append(textLines[i].trim());
            if (i < textLines.length - 1)
                sb.append("\n");
        }
        parser.next();
        return sb.toString();
    }

    /**
     * ***********************************************************************
     * Serialization
     * ***********************************************************************
     */

    public void xmlSerialize(XmlSerializer xmlSerializer)
            throws IllegalArgumentException, IllegalStateException, IOException {
        xmlSerializer.startTag("", XML_TAG);
        xmlSerializer.attribute("", XML_ATTR_LABEL, getLabel());
        for (Line line : mLines) {
            line.xmlSerialize(xmlSerializer);
        }
        xmlSerializer.endTag("", XML_TAG);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getLabel()).append(") ");
        for (Line line : mLines) {
            sb.append(line.toString()).append("\n");
        }
        return sb.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        try {
            dest.writeStringArray(new String[]{mTune.getId(), mLabel, mChordGrid});
            dest.writeTypedList(mLines);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void readFromParcel(Parcel in) {
        String[] data = new String[3];
        in.readStringArray(data);
        mLabel = data[1];
        mChordGrid = data[2];
        in.readTypedList(mLines, Line.CREATOR);
    }

    /**
     * Gets the greatest number of measures per line (i.e. the largest line).
     */
    public int getMaxMeasuresPerLine() {
        int max = 0;
        for (Line line : mLines) {
            if (line.countMeasures() > max)
                max = line.countMeasures();
        }
        return max;
    }
}
