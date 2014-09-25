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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import abc.notation.BarLine;
import abc.notation.RepeatBarLine;
import abc.parser.PositionableNote;

public class Tune implements ITuneItem, Parcelable {

    /**
     * ***********************************************************************
     * XML Serialization
     * ***********************************************************************
     */

    public final static String XML_TAG = "Tune";
    public static final Parcelable.Creator<Tune> CREATOR = new Creator<Tune>() {

        @Override
        public Tune[] newArray(int size) {
            return new Tune[size];
        }

        @Override
        public Tune createFromParcel(Parcel source) {
            return new Tune(source);
        }
    };
    /**
     * Tag for LogCat console debugging.
     */
    private final String TAG = "Tune";
    private final Pattern partPattern = Pattern.compile("^\\s*P:(.+)$");
    private abc.notation.Tune abcTune;
    private String id;
    private int index;
    private String name;
    private String chordGrid;
    private String key;
    private Rythm rythm;
    private List<TunePart> parts = new ArrayList<TunePart>();

    public Tune() {
    }

    public Tune(String name, Rythm rythm, String key, String chordGrid) {
        this.name = name;
        this.rythm = rythm;
        this.key = key;
        this.chordGrid = chordGrid;
    }

    public Tune(XmlPullParser parser) throws XmlPullParserException,
            IOException {
        Log.d(MainActivity.TAG, "parse Tune tag " + parser.getName());
        parser.require(XmlPullParser.START_TAG, "", XML_TAG);

        int attrCount = parser.getAttributeCount();
        for (int i = 0; i < attrCount; i++) {
            String attrName = parser.getAttributeName(i);
            String attrValue = parser.getAttributeValue(i);
            parseXmlAttribute(attrName, attrValue);
        }

        while (parser.nextTag() == XmlPullParser.START_TAG) {
            if (parser.getName().equalsIgnoreCase(TunePart.XML_TAG)) {
                TunePart part = new TunePart(this, parser);
                parts.add(part);
            }
        }
        updateChordGridFromParts();

        parser.require(XmlPullParser.END_TAG, "", XML_TAG);
    }

    /**
     * Builds a new instance of Tune from a text representation.
     *
     * @throws Exception
     */
    public Tune(String text) throws Exception {
        String[] lines = text.split("\n");
        if (lines.length < 5) {
            Log.e(TAG, "Not enough lines in Tune text definition!");
            throw new Exception("Not enough lines in Tune text definition!");
        }

        if (!lines[0].startsWith("X:")) {
            String error = String
                    .format("First line should start with X: instead of '%s'",
                            lines[0]);
            Log.e(TAG, error);
            throw new Exception(error);
        }

        index = Integer.parseInt(TextUtils.substring(lines[0], 2,
                lines[0].length()));

        int currentLine = 1;
        while (key == null) {
            String prefix = lines[currentLine].substring(0, 2);
            if ("T:".equalsIgnoreCase(prefix)) {
                name = lines[currentLine].substring(2);
                Log.v(TAG, String.format("Title = %s", name));
            } else if ("I:".equalsIgnoreCase(prefix)) {
                id = lines[currentLine].substring(2).trim();
            } else if ("R:".equalsIgnoreCase(prefix)) {
                rythm = Rythm.parse(lines[currentLine].substring(2));
                Log.v(TAG, String.format("Rythm = %s", rythm));
            } else if ("K:".equalsIgnoreCase(prefix)) {
                key = lines[currentLine].substring(2);
                Log.v(TAG, String.format("Key = %s", key));
            } else
                throw new Exception(String.format("Unexpected line '%s'",
                        lines[currentLine]));
            currentLine++;
        }

        if (TextUtils.isEmpty(id)) {
            id = generateId(name);
            Log.v(TAG, String.format("Generated id = %s", id));
        }

        TunePart.resetNextLabel();

        ArrayList<String> partLines = new ArrayList<String>();
        boolean inRepetition = false, previousLineInRepetition = false;
        while (currentLine < lines.length) {
            String line = lines[currentLine].trim();
            if (line.isEmpty())
                break;
            if (line.startsWith("|:")) {
                addPartFromLines(partLines);
                inRepetition = previousLineInRepetition = true;
                do {
                    partLines.add(line);
                    if (line.endsWith(":|"))
                        inRepetition = false;
                    else {
                        currentLine++;
                        line = lines[currentLine].trim();
                    }
                } while (inRepetition && currentLine < lines.length);
            } else if ((line.startsWith("|") && previousLineInRepetition && !inRepetition)
                    || TunePart.parseLabel(line) != null) {
                addPartFromLines(partLines);
                previousLineInRepetition = false;
                boolean endPart = false;
                while (!endPart) {
                    partLines.add(line);
                    if (line.endsWith("||") || line.endsWith(":|") || currentLine == lines.length - 1)
                        endPart = true;
                    else {
                        currentLine++;
                        line = lines[currentLine].trim();
                    }
                }
            } else {
                partLines.add(line);
                previousLineInRepetition = false;
                if (line.endsWith("||") || line.endsWith(":|") || currentLine == lines.length - 1)
                    addPartFromLines(partLines);
            }
            currentLine++;
        }
        addPartFromLines(partLines);
        Log.d(TAG, String.format("Added %d parts to this tune", parts.size()));
    }

    public Tune(final abc.notation.Tune abcTune) {
        this.abcTune = abcTune;
        abc.notation.Tune.Music music = abcTune.getMusic();
        Log.d(TAG, getChordString());
    }

    /**************************************************************************
     * Text parsing
     *************************************************************************/

    /**
     * ***********************************************************************
     * Parcelable implementation
     * ***********************************************************************
     */

    public Tune(Parcel in) {
        readFromParcel(in);
    }

    public static Tune parseAbcTune(final String abcFragment) {
        abc.parser.TuneParser tuneParser = new abc.parser.TuneParser();
        return new Tune(tuneParser.parse(abcFragment));
    }

    public static String generateId(String text) {
        if (TextUtils.isEmpty(text))
            throw new IllegalArgumentException(
                    "Argument cannot be a null or empty string!");

        String id = text.toLowerCase().trim();
        return id.replaceAll("[^a-z0-9]+", "_");
    }

    public void xmlSerialize(XmlSerializer xmlSerializer)
            throws IllegalArgumentException, IllegalStateException, IOException {
        xmlSerializer.startTag("", "Tune");
        xmlSerializer.attribute("", "id", getId());
        xmlSerializer.attribute("", "name", getName());
        xmlSerializer.attribute("", "rythm", getRythm().getName());
        xmlSerializer.attribute("", "key", getKey());
        for (TunePart part : parts) {
            part.xmlSerialize(xmlSerializer);
        }
        xmlSerializer.endTag("", "Tune");
    }

    private void updateChordGridFromParts() {
        StringBuilder sb = new StringBuilder();
        int countParts = countParts();
        for (int i = 0; i < countParts; i++) {
            sb.append(parts.get(i).getChordGrid());
            if (i < countParts - 1)
                sb.append('\n');
        }
        chordGrid = sb.toString();
    }

    private void parseXmlAttribute(String attrName, String attrValue) {
        Log.d(MainActivity.TAG, String.format("parse Tune attribute %s=\"%s\"",
                attrName, attrValue));
        if ("id".equals(attrName))
            id = attrValue;
        else if ("name".equals(attrName))
            setName(attrValue);
        else if ("rythm".equals(attrName)) {
            rythm = Rythm.parse(attrValue);
        } else if ("key".equals(attrName))
            key = attrValue;
    }

    private void addPartFromLines(List<String> partLines) {
        if (partLines.size() > 0) {
            parts.add(new TunePart(this, partLines));
            partLines.clear();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("X:").append(getIndex()).append("\n");
        sb.append("I:").append(getId()).append("\n");
        sb.append("T:").append(getName()).append("\n");
        sb.append("R:").append(getRythm().getName()).append("\n");
        sb.append("K:").append(getKey()).append("\n");
        sb.append(getChordsString());
        return sb.toString();
    }

    private String getChordsString() {
        StringBuilder sb = new StringBuilder();
        for (TunePart part : getParts()) {
            sb.append(part.toString());
        }
        return sb.toString();
    }

    public String getId() {
        return id;
    }

    public void setId(String newId) {
        id = newId;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int newIndex) {
        index = newIndex;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        if (abcTune != null) {
            String[] titles = abcTune.getTitles();
            if (titles == null || titles.length == 0)
                abcTune.addTitle(name);
            else {
                for (String t : titles)
                    abcTune.removeTitle(t);
                abcTune.addTitle(name);
                for (int i = 1; i < titles.length; i++)
                    abcTune.addTitle(titles[i]);
            }
        }
    }

    public String getTitle() {
        int comma = name.indexOf(',');
        if (comma < 0)
            return name;
        return name.substring(comma + 1).trim() + " "
                + name.substring(0, comma);
    }

    public int countParts() {
        return parts.size();
    }

    /**
     * Getter for the tune parts.
     */
    public List<TunePart> getParts() {
        return parts;
    }

    public int countTotalLines() {
        int result = 0;
        for (TunePart part : parts) {
            result += part.getLines().size();
        }
        return result;
    }

    public List<String> getPartLabels() {
        ArrayList<String> labels = new ArrayList<String>();
        Matcher m = partPattern.matcher(abcTune.toString());
        while (m.find()) {
            labels.add(m.group(1).trim());
        }
        return labels;
    }

    public String getChordString() {
        StringBuilder sb = new StringBuilder();
        ArrayList<ArrayList<String>> parts = new ArrayList<ArrayList<String>>();
        ArrayList<String> currentPart = new ArrayList<String>();
        int indexFirstRepetition = -1;
        ArrayList<ArrayList<String>> currentPartRepetitions = new ArrayList<ArrayList<String>>();
        ArrayList<String> currentRepetition = new ArrayList<String>();
        int currentRepetitionNumber = 0;
        boolean hasRepeatBar = false;

        for (Object o : abcTune.getMusic()) {
            if (o instanceof BarLine) {
                BarLine barLine = (BarLine) o;
                switch (barLine.getType()) {
                    case BarLine.REPEAT_OPEN:
                        parts.add(currentPart);
                        currentPart.clear();
                        hasRepeatBar = true;
                        break;
                    case BarLine.REPEAT_CLOSE:
                        if (!hasRepeatBar) {
                            currentPart.add(0, "|:");
                            hasRepeatBar = true;
                        }
                        break;
                }
                currentPart.add(barLine.toString());
                if (currentRepetitionNumber > 0)
                    currentRepetition.add(barLine.toString());
            } else if (o instanceof PositionableNote) {
                PositionableNote note = (PositionableNote) o;
                String chordName = note.getChordName();
                if (chordName != null && !chordName.isEmpty()) {
                    currentPart.add(chordName);
                    if (currentRepetitionNumber > 0)
                        currentRepetition.add(chordName);
                }
            } else if (o instanceof RepeatBarLine) {
                RepeatBarLine repeatBarLine = (RepeatBarLine) o;
                if (currentRepetitionNumber == 0) {
                    indexFirstRepetition = currentPart.size();
                    currentRepetitionNumber = repeatBarLine.getRepeatNumber();
                }
            }
        }

        int countParts = parts.size();
        for (int i = 0; i < countParts; i++) {
            ArrayList<String> part = parts.get(i);
            int partSize = part.size();
            for (int j = 0; j < partSize; j++) {
                sb.append(part.get(j));
                if (j < partSize - 1)
                    sb.append(" ");
            }
            if (i < countParts - 1)
                sb.append("\n");
        }
        return sb.toString();
    }

    public Rythm getRythm() {
        return rythm;
    }

    public String getKey() {
        return key;
    }

    public String getChordGrid() {
        return chordGrid;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringArray(new String[]{id, name, rythm.getName(), key});
        dest.writeTypedList(parts);
    }

    public void readFromParcel(Parcel in) {
        String[] data = new String[4];
        in.readStringArray(data);
        id = data[0];
        name = data[1];
        rythm = Rythm.parse(data[2]);
        key = data[3];
        if (parts == null)
            parts = new ArrayList<TunePart>();
        in.readTypedList(parts, TunePart.CREATOR);
        updateChordGridFromParts();

    }

    /**
     * Gets the greatest number of measures per line (i.e. the largest line).
     */
    public int getMaxMeasuresPerLine() {
        int max = 0;
        for (TunePart part : parts) {
            int count = part.getMaxMeasuresPerLine();
            if (count > max)
                max = count;
        }
        return max;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Tune))
            return false;
        if (o == this)
            return true;

        Tune tune = (Tune) o;

        // Check rythm
        if (rythm == null && tune.rythm != null)
            return false;
        if (rythm != null && !rythm.equals(tune.rythm))
            return false;

        // Check key
        if (key == null && tune.key != null)
            return false;
        if (key != null && !key.equalsIgnoreCase(tune.key))
            return false;

        // Check text representation
        String rep1 = getChordsString();
        String rep2 = tune.getChordsString();
        if (!rep1.equalsIgnoreCase(rep2))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int hash = 1;
        hash = hash * 31 + getId().hashCode();
        hash = hash * 31 + getIndex();
        hash = hash * 31 + getRythm().hashCode();
        hash = hash * 31 + getKey().hashCode();
        hash = hash * 31 + getName().hashCode();
        hash = hash * 31 + getChordsString().hashCode();
        return hash;
    }


}
