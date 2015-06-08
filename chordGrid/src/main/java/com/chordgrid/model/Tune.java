package com.chordgrid.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import abc.notation.BarLine;
import abc.notation.RepeatBarLine;
import abc.parser.PositionableNote;

public class Tune extends TunebookItem implements Parcelable {

    public static class TuneBookParcelableCreator implements Creator<Tune> {

        @Override
        public Tune[] newArray(int size) {
            return new Tune[size];
        }

        @Override
        public Tune createFromParcel(Parcel source) {
            return new Tune(source);
        }
    }

    public static final TuneBookParcelableCreator CREATOR = new TuneBookParcelableCreator();

    /**
     * Tag for LogCat console debugging.
     */
    private final String TAG = "Tune";
    private final Pattern partPattern = Pattern.compile("^\\s*P:(.+)$");
    private abc.notation.Tune abcTune;
    private String id;
    private int index;
    private String name;
    private Rhythm mRhythm;
    private String chordGrid;
    private String key;
    private List<TunePart> parts = new ArrayList<TunePart>();
    private Character mNextPartLabel = 'A';

    ////////////////////////////////////////////////////////////////////////////////////////////
    // Construction
    ////////////////////////////////////////////////////////////////////////////////////////////

    public Tune() {
    }

    public Tune(Parcel in) {
        readFromParcel(in);
    }


    public Tune(String name, Rhythm rhythm, String key) {
        this.name = name;
        mRhythm = rhythm;
        this.key = key;
    }

    public Tune(String name, Rhythm rhythm, String key, String chordGrid) {
        this.name = name;
        mRhythm = rhythm;
        this.key = key;
        this.chordGrid = chordGrid;
    }

    /**
     * Builds a new instance of Tune from a text representation.
     *
     * @param text The serialized tune text.
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
                setRhythm(Rhythm.getKnownRhythm(lines[currentLine].substring(2)));
                Log.v(TAG, String.format("Rhythm = %s", getRhythm()));
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
        //abc.notation.Tune.Music music = abcTune.getMusic();
        Log.d(TAG, getChordString());
    }

    ////////////////////////////////////////////////////////////////////////////////////////////
    // Part labels
    ////////////////////////////////////////////////////////////////////////////////////////////

    public String getNextPartLabel() {
        String label = new StringBuilder().append(mNextPartLabel).toString();
        mNextPartLabel++;
        return label;
    }

    /**
     * ***********************************************************************
     * Parcelable implementation
     * ***********************************************************************
     */

//    public static Tune parseAbcTune(final String abcFragment) {
//        abc.parser.TuneParser tuneParser = new abc.parser.TuneParser();
//        return new Tune(tuneParser.parse(abcFragment));
//    }
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
        xmlSerializer.attribute("", "rhythm", getRhythm().getName());
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

    private void addPartFromLines(List<String> partLines) {
        if (partLines.size() > 0) {
            parts.add(new TunePart(this, partLines));
            partLines.clear();
        }
    }

    @Override
    public String toString() {
        return "X:" + getIndex() + "\n" + "I:" + getId() + "\n" + "T:" + getName() + "\n" + "R:" + getRhythm().getName() + "\n" + "K:" + getKey() + "\n" + getChordsString();
    }

    private String getChordsString() {
        StringBuilder sb = new StringBuilder();
        for (TunePart part : getParts()) {
            sb.append(part.toString());
        }
        return sb.toString();
    }

    public String getId() {
        if (TextUtils.isEmpty(id))
            id = generateId(getName());
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

    /**
     * Getter for rhythm.
     */
    @Override
    public Rhythm getRhythm() {
        return mRhythm;
    }

    /**
     * Setter for rhythm.
     *
     * @param rhythm The rythm.
     */
    public void setRhythm(Rhythm rhythm) {
        mRhythm = rhythm;
        notifyObservers(mRhythm);
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

    public TunePart getPart(String label) {
        for (TunePart p : parts) {
            if (p.getLabel().equalsIgnoreCase(label))
                return p;
        }
        return null;
    }

    public TunePart getPart(int index) {
        return parts.get(index);
    }

    public void addPart(TunePart newPart) {
        parts.add(newPart);
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
        dest.writeStringArray(new String[]{id, name, getRhythm().getName(), key});
        dest.writeTypedList(parts);
    }

    /**
     * Deserializes a Tune instance from a parcel.
     *
     * @param in The input parcel.
     */
    private void readFromParcel(Parcel in) {
        String[] data = new String[4];
        in.readStringArray(data);
        id = data[0];
        name = data[1];
        setRhythm(Rhythm.getKnownRhythm(data[2]));
        key = data[3];
        if (parts == null)
            parts = new ArrayList<TunePart>();
        in.readTypedList(parts, TunePart.CREATOR);
        for (TunePart part : parts) {
            part.setTune(this);
        }
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

        // Check rhythm
        if (mRhythm == null && tune.mRhythm != null)
            return false;
        if (mRhythm != null && !mRhythm.equals(tune.mRhythm))
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
        hash = hash * 31 + (getRhythm() == null ? 0 : getRhythm().hashCode());
        hash = hash * 31 + getKey().hashCode();
        hash = hash * 31 + getName().hashCode();
        hash = hash * 31 + getChordsString().hashCode();
        return hash;
    }

    public Measure getMeasure(String partLabel, int lineIndex, int measureIndex) {
        for (TunePart part : parts) {
            if (part.getLabel().equalsIgnoreCase(partLabel)) {
                if (lineIndex < part.getLines().size()) {
                    Line line = part.getLine(lineIndex);
                    if (measureIndex < line.getMeasures().size()) {
                        return line.getMeasure(measureIndex);
                    }
                }
            }
        }
        return null;
    }

    public Measure getMeasure(int partIndex, int lineIndex, int measureIndex) {
        if (partIndex < parts.size()) {
            TunePart part = parts.get(partIndex);
            if (lineIndex < part.getLines().size()) {
                Line line = part.getLine(lineIndex);
                if (measureIndex < line.getMeasures().size()) {
                    return line.getMeasure(measureIndex);
                }
            }
        }
        return null;
    }

}
