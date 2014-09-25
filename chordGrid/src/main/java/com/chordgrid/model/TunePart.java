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
     * Tag for LogCat console debugging.
     */
    private static final String TAG = "com.chordgrid.model.TunePart";

    private Tune tune;
    private String label;
    private String chordGrid;
    private List<Line> lines = new ArrayList<Line>();

    public TunePart() {
    }

    public Tune getTune() {
        return tune;
    }

    public String getLabel() {
        return label;
    }

    public String getChordGrid() {
        return chordGrid;
    }

    /**
     * Getter for the part lines.
     */
    public List<Line> getLines() {
        return lines;
    }

    /**************************************************************************
     * XML parsing
     *************************************************************************/

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

    /**
     * Reads a new instance of TunePart from an XML parser.
     */
    public TunePart(Tune tune, XmlPullParser parser)
            throws XmlPullParserException, IOException {
        this.tune = tune;
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
                lines.add(new Line(parser));
            }
        }
        chordGrid = sb.toString();

        parser.require(XmlPullParser.END_TAG, "", XML_TAG);
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
            label = attrValue;
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
        for (Line line : lines) {
            line.xmlSerialize(xmlSerializer);
        }
        xmlSerializer.endTag("", XML_TAG);
    }

    /**
     * ***********************************************************************
     * Text parsing
     * ***********************************************************************
     */

    private static Character currentLabel = 'A';

    public static String nextLabel() {
        String label = new StringBuilder().append(currentLabel).toString();
        currentLabel++;
        return label;
    }

    public static void resetNextLabel() {
        currentLabel = 'A';
    }

    /**
     * Builds a new instance of Tune from a text representation.
     */
    public TunePart(Tune tune, List<String> textLines) {
        this.tune = tune;

        label = parseLabel(textLines.get(0));
        if (label == null)
            label = nextLabel();

        Log.d(TAG, String.format("Reading part %s with %d lines", getLabel(),
                textLines.size()));
        for (String textLine : textLines) {
            lines.add(new Line(textLine));
        }
        Log.d(TAG, String.format("Added %d lines to this part", lines.size()));
    }

    private static final Pattern labelPattern = Pattern
            .compile("([A-Za-z0-9]+)\\)");

    public static String parseLabel(String firstLine) {
        Matcher m = labelPattern.matcher(firstLine);
        if (m.find())
            return m.group(1);
        return null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getLabel()).append(") ");
        for (Line line : lines) {
            sb.append(line.toString()).append("\n");
        }
        return sb.toString();
    }

    /**
     * ***********************************************************************
     * Parcelable implementation
     * ***********************************************************************
     */

    public TunePart(Parcel in) {
        readFromParcel(in);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringArray(new String[]{tune.getId(), label, chordGrid});
        dest.writeTypedList(lines);
    }

    public void readFromParcel(Parcel in) {
        String[] data = new String[3];
        in.readStringArray(data);
        label = data[1];
        chordGrid = data[2];
        in.readTypedList(lines, Line.CREATOR);
    }

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
     * Gets the greatest number of measures per line (i.e. the largest line).
     */
    public int getMaxMeasuresPerLine() {
        int max = 0;
        for (Line line : lines) {
            if (line.countMeasures() > max)
                max = line.countMeasures();
        }
        return max;
    }
}
