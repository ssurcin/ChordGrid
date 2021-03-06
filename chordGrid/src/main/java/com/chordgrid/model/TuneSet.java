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
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * A set of tunes (i.e. references to existing tunes).
 *
 * @author sylvain.surcin@gmail.com
 */
public class TuneSet extends TunebookItem implements Parcelable {

    /**
     * ***********************************************************************
     * XML parsing
     * ***********************************************************************
     */

    public static final String XML_TAG = "TuneSet";
    public static final Parcelable.Creator<TuneSet> CREATOR = new Creator<TuneSet>() {

        @Override
        public TuneSet[] newArray(int size) {
            return new TuneSet[size];
        }

        @Override
        public TuneSet createFromParcel(Parcel source) {
            return new TuneSet(source);
        }
    };
    private static final String TAG = "TuneSet";
    private static final String XML_TAG_TUNEREF = "TuneRef";
    private static final String XML_TAG_ATTR_ID = "id";
    private final ArrayList<Tune> tunes = new ArrayList<Tune>();
    private TuneBook tuneBook;
    private String name;

    public TuneSet() {
        tuneBook = null;
    }

    public TuneSet(TuneBook tuneBook) {
        this.tuneBook = tuneBook;
    }

    /**
     * ***********************************************************************
     * Text parsing
     * <p/>
     * ***********************************************************************
     */

    public TuneSet(TuneBook tunebook, String tuneSource) throws Exception {
        tuneBook = tunebook;
        String[] lines = TextUtils.split(tuneSource, "\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (i == 0) {
                if (!line.startsWith("SET:"))
                    throw new Exception("Tuneset must start with 'SET:'!");
                name = TextUtils.substring(line, 4, line.length()).trim();
                Log.d(TAG, String.format("Parsing tune set '%s'", name));
            } else {
                if (line.startsWith("SET:"))
                    throw new Exception(
                            "Tuneset reference line must not start with 'SET:'!");
                String[] refs = TextUtils.split(line, "\\s");
                for (int j = 0; j < refs.length; j++) {
                    String ref = refs[j];
                    try {
                        int index = Integer.parseInt(ref);
                        Tune tune = tuneBook.getTuneFromIndex(index);
                        if (tune != null)
                            tunes.add(tune);
                        else
                            Log.w(TAG, String.format("Unknown tune index %d",
                                    index));
                    } catch (NumberFormatException e) {
                        Tune tune = tuneBook.getTuneFromId(ref);
                        if (tune != null)
                            tunes.add(tune);
                        else
                            Log.w(TAG,
                                    String.format("Unknown tune id '%s'", ref));
                    }
                }
            }
        }
        if (TextUtils.isEmpty(name) || "null".equalsIgnoreCase(name)) {
            name = generateSetName();
            Log.d(TAG, "Generating set name " + name);
        }
    }

    public TuneSet(TuneBook tuneBook, XmlPullParser parser)
            throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, "", XML_TAG);

        this.tuneBook = tuneBook;
        while (parser.nextTag() == XmlPullParser.START_TAG
                && parser.getName().equalsIgnoreCase(XML_TAG_TUNEREF)) {
            int countAttr = parser.getAttributeCount();
            for (int i = 0; i < countAttr; i++) {
                String attrName = parser.getAttributeName(i);
                String attrValue = parser.getAttributeValue(i);
                if (XML_TAG_ATTR_ID.equalsIgnoreCase(attrName)) {
                    Tune tune = tuneBook.getTuneFromId(attrValue);
                    if (tune == null)
                        Log.w(MainActivity.TAG, "Unknown tune with id "
                                + attrValue);
                    else
                        tunes.add(tune);
                }
            }
            // closing tag
            parser.nextTag();
            parser.require(XmlPullParser.END_TAG, "", XML_TAG_TUNEREF);
        }
    }

    /**
     * ***********************************************************************
     * Parcelable implementation
     * ***********************************************************************
     */

    public TuneSet(Parcel source) {
        readFromParcel(source);
    }

    public String toText() {
        StringBuilder sb = new StringBuilder();
        sb.append("SET:").append(getName()).append("\n");
        int count = tunes.size();
        for (int i = 0; i < count; i++) {
            sb.append(tunes.get(i).getId());
            if (i < count - 1)
                sb.append(" ");
        }
        sb.append("\n");
        return sb.toString();
    }

    public void serialize(OutputStream outputStream) {
        try {
            outputStream.write(toText().getBytes());
        } catch (Exception e) {
            Log.e(TAG, String.format("Cannot serialize to text tune set '%s'",
                    getName()), e);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        int size = tunes.size();
        for (int i = 0; i < size; i++) {
            sb.append(tunes.get(i).getTitle());
            if (i < size - 1)
                sb.append(" / ");
        }
        return sb.toString();
    }

    public int size() {
        return tunes.size();
    }

    public Tune get(int index) {
        return tunes.get(index);
    }

    public String getName() {
        return name;
    }

    /**
     * ***********************************************************************
     * ITuneItem
     * ***********************************************************************
     */

    @Override
    public String getTitle() {
        StringBuilder sb = new StringBuilder();
        int size = tunes.size();
        for (int i = 0; i < size; i++) {
            sb.append(tunes.get(i).getTitle());
            if (i < size - 1)
                sb.append(" / ");
        }
        return sb.toString();
    }

    /**
     * ***********************************************************************
     * Operations
     * ***********************************************************************
     */

    @Override
    public Rhythm getRhythm() {
        HashMap<Rhythm, Integer> rythmCount = new HashMap<Rhythm, Integer>();
        for (Tune tune : tunes) {
            Rhythm rhythm = tune.getRhythm();
            Integer count = rythmCount.get(rhythm);
            if (count == null)
                count = 0;
            count++;
            rythmCount.put(rhythm, count);
        }
        int maxCount = 0;
        Rhythm r = null;
        for (Rhythm currentRhythm : rythmCount.keySet()) {
            int count = rythmCount.get(currentRhythm);
            if (count > maxCount) {
                maxCount = count;
                r = currentRhythm;
            }
        }
        return r;
    }

    /**
     * Generates an automatic set name from the tunes indexes.
     *
     * @return A string.
     */
    private String generateSetName() {
        String[] tuneIndexes = new String[tunes.size()];
        for (int i = 0; i < tunes.size(); i++) {
            tuneIndexes[i] = tunes.get(i).getId();
        }
        return TextUtils.join("_", tuneIndexes);
    }

    /**
     * ***********************************************************************
     * Serialization
     * ***********************************************************************
     */

    public void xmlSerialize(XmlSerializer xmlSerializer)
            throws IllegalArgumentException, IllegalStateException, IOException {
        xmlSerializer.startTag("", XML_TAG);
        for (Tune tune : tunes) {
            xmlSerializer.startTag("", XML_TAG_TUNEREF);
            xmlSerializer.attribute("", XML_TAG_ATTR_ID, tune.getId());
            xmlSerializer.endTag("", XML_TAG_TUNEREF);
        }
        xmlSerializer.endTag("", XML_TAG);
    }

    /**
     * Adds a new tune to this set.
     *
     * @param tune A Tune.
     */
    public void add(Tune tune) {
        tunes.add(tune);

        // Notify observers of a change
        setChanged();
        notifyObservers(tunes);
    }

    /**
     * Removes all tunes from this set.
     */
    public void clear() {
        tunes.clear();

        // Notify observers of a change
        setChanged();
        notifyObservers(tunes);
    }

    /**
     * Gets the zero-based index of a tune in the set.
     *
     * @param tune A Tune.
     * @return The tune index (-1 if not found).
     */
    public int getTuneIndex(Tune tune) {
        return tunes.indexOf(tune);
    }

    public ArrayList<Tune> getTunes() {
        return tunes;
    }

    public void setTunes(Collection<? extends Tune> newTunes) {
        tunes.clear();
        for (Tune tune : newTunes)
            tunes.add(tune);

        // Notify observers of a change
        setChanged();
        notifyObservers(tunes);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(tunes);
    }

    public void readFromParcel(Parcel in) {
        in.readTypedList(tunes, Tune.CREATOR);
    }

}
