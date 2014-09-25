package com.chordgrid.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.util.Xml;

import com.chordgrid.MainActivity;
import com.chordgrid.ParcelableUtils;
import com.chordgrid.util.MyTextUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.TreeMap;
import java.util.TreeSet;

public class TuneBook extends Observable implements Parcelable {

    /**
     * ***********************************************************************
     * XML parsing
     * ***********************************************************************
     */

    public static final String XML_TAG_TUNES = "Tunes";
    public static final String XML_TAG_TUNESETS = "TuneSets";
    public static final Parcelable.Creator<TuneBook> CREATOR = new Creator<TuneBook>() {

        @Override
        public TuneBook[] newArray(int size) {
            return new TuneBook[size];
        }

        @Override
        public TuneBook createFromParcel(Parcel source) {
            return new TuneBook(source);
        }
    };
    private final String TAG = "TuneBook";
    private final Map<String, Tune> tunes = new HashMap<String, Tune>();
    private final List<TuneSet> tuneSets = new ArrayList<TuneSet>();

    public TuneBook() {
    }

    public TuneBook(XmlPullParser parser) throws XmlPullParserException,
            IOException {
        if (parser.nextTag() == XmlPullParser.START_TAG
                && parser.getName().equalsIgnoreCase(XML_TAG_TUNES)) {
            for (Tune tune : parseTunes(parser)) {
                tunes.put(tune.getId(), tune);
            }
        }

        if (parser.nextTag() == XmlPullParser.START_TAG
                && parser.getName().equalsIgnoreCase(XML_TAG_TUNESETS)) {
            tuneSets.addAll(parseTuneSets(parser));
        }
    }

    /**
     * Builds a new instance of TuneBook from a text representation.
     *
     * @throws Exception
     */
    public TuneBook(String text) throws Exception {
        // Split the text into tunes (the first item will be empty or
        // irrelevant).
        String[] tuneSources = MyTextUtils
                .splitWithDelimiter(text, "(X:|SET:)");

        for (int i = 1; i < tuneSources.length; i++) {
            String source = tuneSources[i];
            if (source.startsWith("X:")) {
                try {
                    Tune tune = new Tune(tuneSources[i]);
                    tunes.put(tune.getId(), tune);
                } catch (Exception e) {
                    Log.w(TAG, e.getMessage());
                }
            } else if (source.startsWith("SET:")) {
                try {
                    TuneSet set = new TuneSet(this, source);
                    tuneSets.add(set);
                } catch (Exception e) {
                    Log.w(TAG, e.getMessage());
                }
            }
        }
    }

    /**************************************************************************
     * Text parsing
     *************************************************************************/

    /**
     * ***********************************************************************
     * Parcelable implementation
     * ***********************************************************************
     */

    public TuneBook(Parcel source) {
        readFromParcel(source);
    }

    private List<Tune> parseTunes(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        ArrayList<Tune> tunes = new ArrayList<Tune>();

        Log.d(MainActivity.TAG, "parseTunes tag " + parser.getName());

        while (parser.nextTag() == XmlPullParser.START_TAG
                && Tune.XML_TAG.equalsIgnoreCase(parser.getName())) {
            Log.d(MainActivity.TAG, "parsing tune");
            Tune tune = new Tune(parser);
            tunes.add(tune);
        }

        parser.require(XmlPullParser.END_TAG, "", XML_TAG_TUNES);

        return tunes;
    }

    private List<TuneSet> parseTuneSets(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        ArrayList<TuneSet> tunesets = new ArrayList<TuneSet>();
        Log.d(MainActivity.TAG, "parseTuneSets tag " + parser.getName());

        parser.require(XmlPullParser.START_TAG, "", XML_TAG_TUNESETS);

        while (parser.nextTag() == XmlPullParser.START_TAG
                && parser.getName().equalsIgnoreCase(TuneSet.XML_TAG)) {
            TuneSet tuneset = new TuneSet(this, parser);
            tunesets.add(tuneset);
        }

        parser.require(XmlPullParser.END_TAG, "", XML_TAG_TUNESETS);

        return tunesets;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        TreeMap<String, Tune> sortedTunes = new TreeMap<String, Tune>(new TuneIndexComparator(tunes));
        sortedTunes.putAll(tunes);

        for (Tune tune : sortedTunes.values()) {
            sb.append(tune.toString());
            sb.append("\n");
        }

        for (TuneSet set : tuneSets) {
            sb.append(set.toText());
            sb.append("\n");
        }

        return sb.toString();
    }

    public void serialize(OutputStream outputStream) throws IOException {
        outputStream.write(toString().getBytes());
    }

    /**
     * ***********************************************************************
     * Serialization
     * ***********************************************************************
     */

    public void xmlSerialize(FileOutputStream outputStream) {
        XmlSerializer xmlSerializer = Xml.newSerializer();
        StringWriter writer = new StringWriter();
        try {
            xmlSerializer.setOutput(writer);

            xmlSerializer.startDocument("UTF-8", true);
            xmlSerializer.startTag("", "TuneBook");
            xmlSerializer.startTag("", "Tunes");
            for (Tune tune : tunes.values()) {
                tune.xmlSerialize(xmlSerializer);
            }
            xmlSerializer.endTag("", "Tunes");
            xmlSerializer.startTag("", "TuneSets");
            for (TuneSet set : tuneSets) {
                set.xmlSerialize(xmlSerializer);
            }
            xmlSerializer.endTag("", "TuneSets");
            xmlSerializer.endTag("", "TuneBook");
            xmlSerializer.endDocument();

            outputStream.write(writer.toString().getBytes());
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * ***********************************************************************
     * Properties
     * ***********************************************************************
     */

    public int countTunes() {
        return tunes.size();
    }

    public int countTuneSets() {
        return tuneSets.size();
    }

    public List<Rythm> getAllTuneSetRythms() {
        TreeSet<Rythm> rythms = new TreeSet<Rythm>();
        for (TuneSet tuneSet : tuneSets)
            rythms.add(tuneSet.getRythm());
        ArrayList<Rythm> list = new ArrayList<Rythm>(rythms);
        Collections.sort(list, new Comparator<Rythm>() {

            @Override
            public int compare(Rythm lhs, Rythm rhs) {
                return lhs.getName().compareTo(rhs.getName());
            }

        });
        return list;
    }

    public ArrayList<TuneSet> getAllSetsWithRythm(Rythm rythm) {
        ArrayList<TuneSet> result = new ArrayList<TuneSet>();
        for (TuneSet set : tuneSets) {
            if (rythm.equals(set.getRythm()))
                result.add(set);
        }
        return result;
    }

    /**
     * Gets all rythms expressed in the tune collection.
     */
    public List<Rythm> getAllTuneRythms() {
        TreeSet<Rythm> rythms = new TreeSet<Rythm>();
        for (Tune tune : tunes.values())
            rythms.add(tune.getRythm());
        ArrayList<Rythm> list = new ArrayList<Rythm>(rythms);
        Collections.sort(list, new Comparator<Rythm>() {

            @Override
            public int compare(Rythm lhs, Rythm rhs) {
                return lhs.getName().compareTo(rhs.getName());
            }

        });
        return list;
    }

    /**
     * Retrieve all tunes with a given rythm.
     */
    public ArrayList<Tune> getAllTunesWithRythm(Rythm rythm) {
        ArrayList<Tune> result = new ArrayList<Tune>();
        for (Tune tune : tunes.values()) {
            if (rythm.equals(tune.getRythm()))
                result.add(tune);
        }
        return result;
    }

    public Tune getTuneFromId(String id) {
        return tunes.get(id);
    }

    public Tune getTuneFromIndex(int index) {
        for (Tune tune : tunes.values()) {
            if (tune.getIndex() == index)
                return tune;
        }
        return null;
    }

    public void remove(List<? extends ITuneItem> discardedItems) {
        int count = 0;
        for (ITuneItem item : discardedItems) {
            if (item instanceof Tune) {
                tunes.remove(((Tune) item).getId());
                count++;
            } else if (item instanceof TuneSet) {
                tuneSets.remove(item);
                count++;
            }
        }
        if (count > 0) {
            setChanged();
            Log.d(TAG, String.format("Notifying %d observers of new tuneset",
                    countObservers()));
            notifyObservers(ChangedProperty.TuneSets);
        }
    }

    /**
     * Adds a tuneset to the tunebook.
     *
     * @param tuneset The new tuneset.
     */
    public void add(TuneSet tuneset) {
        tuneSets.add(tuneset);
        setChanged();
        Log.d(TAG, String.format("Notifying %d observers of new tuneset",
                countObservers()));
        notifyObservers(ChangedProperty.TuneSets);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        ParcelableUtils.writeMap(tunes, dest);
        dest.writeTypedList(tuneSets);
    }

    public void readFromParcel(Parcel in) {
        tunes.putAll(ParcelableUtils.readMap(in, Tune.class));
        in.readTypedList(tuneSets, TuneSet.CREATOR);
    }

    /**
     * ***********************************************************************
     * Merge
     * ***********************************************************************
     */

    public void merge(TuneBook other) {
        Log.v(TAG, "Merging tunebooks");

        mergeTunes(other);

        mergeSets(other);

        Log.v(TAG, "Merging complete");
    }

    private void mergeTunes(TuneBook other) {
        Log.v(TAG, "Merging tunes from another tunebook");

        HashSet<Tune> originalTunes = new HashSet<Tune>(tunes.values());

        for (Map.Entry<String, Tune> entry : other.tunes.entrySet()) {
            Tune tune = entry.getValue();
            String id = entry.getKey();
            if (originalTunes.contains(tune)) {
                Log.d(TAG, String.format("Tune '%s' is already here, skip.", tune.getTitle()));
            } else {
                Log.d(TAG, String.format("Inserting tune '%s'.", tune.getTitle()));
                if (tunes.keySet().contains(id)) {
                    Log.d(TAG, String.format("Tune id '%s' already in use", id));
                    String newId = generateIdFrom(id);
                    Log.d(TAG, String.format("Reassigning id '%s'", newId));
                    tune.setId(newId);
                }
                tune.setIndex(tunes.size() + 1);
                tunes.put(tune.getId(), tune);
            }
        }

        Log.v(TAG, "Merging tunes complete");
    }

    private String generateIdFrom(String id) {
        String longestId = "";
        int highest = 0;
        for (String k : tunes.keySet()) {
            if (k.startsWith(id)) {
                if (k.length() > longestId.length())
                    longestId = k;
                int underscore = k.lastIndexOf('_');
                if (underscore > -1) {
                    try {
                        int n = Integer.parseInt(k.substring(underscore + 1));
                        if (n > highest)
                            highest = n;
                    } catch (Exception e) {
                        //
                    }
                }
            }
        }
        int underscore = longestId.lastIndexOf('_');
        return String.format("%s_%d", underscore == -1 ? longestId : longestId.substring(0, underscore - 1), highest + 1);
    }

    private void mergeSets(TuneBook other) {
        Log.v(TAG, "Merging sets from another tunebook");

        for (TuneSet set : other.tuneSets) {
            Log.d(TAG, String.format("Inserting set '%s'.", set.getTitle()));
            tuneSets.add(set);
        }

        Log.v(TAG, "Merging sets complete");
    }

    /**
     * ***********************************************************************
     * Operations
     * ***********************************************************************
     */

    public enum ChangedProperty {
        Tunes, TuneSets
    }

    private class TuneIndexComparator implements Comparator<String> {

        private Map<String, Tune> base;

        public TuneIndexComparator(Map<String, Tune> base) {
            this.base = base;
        }

        @Override
        public int compare(String lhs, String rhs) {
            if (base.get(lhs).getIndex() >= base.get(rhs).getIndex())
                return -1;
            else
                return 1;
        }


    }
}
