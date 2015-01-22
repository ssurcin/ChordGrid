package com.chordgrid.model;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;

import com.chordgrid.ParcelableUtils;
import com.chordgrid.R;
import com.chordgrid.util.MyTextUtils;

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
import java.util.Observer;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class TuneBook extends Observable implements Parcelable, Observer {

    /**
     * ***********************************************************************
     * XML parsing
     * ***********************************************************************
     */

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
    private Handler progressDialogHandler;

    public TuneBook() {
    }

    /**************************************************************************
     * Text parsing
     *************************************************************************/

    /**
     * Builds a new instance of TuneBook from a text representation.
     *
     * @param text    The serialized tune book text.
     * @param context The activity context (allowing to get access to shared preferences).
     * @throws Exception
     */
    public TuneBook(String text, Context context) throws Exception {
        // Split the text into tunes (the first item will be empty or
        // irrelevant).
        String[] tuneSources = MyTextUtils
                .splitWithDelimiter(text, "(X:|SET:)");

        Set<Rhythm> knownRhythms = Rhythm.getKnownRhythms(context);

        for (int i = 1; i < tuneSources.length; i++) {
            String source = tuneSources[i];
            if (source.startsWith("X:")) {
                try {
                    Tune tune = new Tune(tuneSources[i], knownRhythms);
                    tunes.put(tune.getId(), tune);
                } catch (Exception e) {
                    Log.w(TAG, e.getMessage());
                }
            } else if (source.startsWith("SET:")) {
                try {
                    TuneSet set = new TuneSet(this, source);
                    add(set);
                } catch (Exception e) {
                    Log.w(TAG, e.getMessage());
                }
            }
        }
    }

    public TuneBook(Parcel source) {
        readFromParcel(source);
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

    public List<Rhythm> getAllTuneSetRythms() {
        TreeSet<Rhythm> rhythms = new TreeSet<Rhythm>();
        for (TuneSet tuneSet : tuneSets)
            rhythms.add(tuneSet.getRhythm());
        ArrayList<Rhythm> list = new ArrayList<Rhythm>(rhythms);
        Collections.sort(list, new Comparator<Rhythm>() {

            @Override
            public int compare(Rhythm lhs, Rhythm rhs) {
                return lhs.getName().compareTo(rhs.getName());
            }

        });
        return list;
    }

    public ArrayList<TuneSet> getAllSetsWithRythm(Rhythm rhythm) {
        ArrayList<TuneSet> result = new ArrayList<TuneSet>();
        for (TuneSet set : tuneSets) {
            if (rhythm.equals(set.getRhythm()))
                result.add(set);
        }
        return result;
    }

    /**
     * Gets all rythms expressed in the tune collection.
     */
    public List<Rhythm> getAllTuneRythms() {
        TreeSet<Rhythm> rhythms = new TreeSet<Rhythm>();
        for (Tune tune : tunes.values())
            rhythms.add(tune.getRhythm());
        ArrayList<Rhythm> list = new ArrayList<Rhythm>(rhythms);
        Collections.sort(list, new Comparator<Rhythm>() {

            @Override
            public int compare(Rhythm lhs, Rhythm rhs) {
                return lhs.getName().compareTo(rhs.getName());
            }

        });
        return list;
    }

    /**
     * Retrieve all tunes with a given rhythm.
     */
    public ArrayList<Tune> getAllTunesWithRythm(Rhythm rhythm) {
        ArrayList<Tune> result = new ArrayList<Tune>();
        for (Tune tune : tunes.values()) {
            if (rhythm.equals(tune.getRhythm()))
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

    public void remove(List<? extends TunebookItem> discardedItems) {
        int count = 0;
        for (TunebookItem item : discardedItems) {
            if (item instanceof Tune) {
                tunes.remove(((Tune) item).getId());
                count++;
            } else if (item instanceof TuneSet) {
                tuneSets.remove(item);
                count++;
            }
            item.deleteObserver(this);
        }
        if (count > 0) {
            // Notify observers that the tune set collection has changed
            setChanged();
            notifyObservers(ChangedProperty.TuneSets);
        }
    }

    /**
     * Adds a tuneset to the tunebook.
     *
     * @param tuneset The new tuneset.
     */
    public void add(TuneSet tuneset) {
        String setName = tuneset.getName();
        for (TuneSet set : tuneSets) {
            if (TextUtils.equals(set.getName(), setName)) {
                Log.w(TAG, "Set with name " + setName + " already exists! Skip add");
                return;
            }
        }
        tuneSets.add(tuneset);

        // Observe this tune set
        tuneset.addObserver(this);

        // Notify observers that the tune set collection has changed
        setChanged();
        notifyObservers(ChangedProperty.TuneSets);
    }

    public void replaceTuneSet(TuneSet oldTuneSet, TuneSet newTuneSet) {
        int index = tuneSets.indexOf(oldTuneSet);
        if (index >= 0) {
            oldTuneSet.deleteObserver(this);
            tuneSets.remove(index);
            tuneSets.add(index, newTuneSet);
            newTuneSet.addObserver(this);

            // Notify observers that the tune set collection has changed
            setChanged();
            notifyObservers(ChangedProperty.TuneSets);
        }
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
        mergeTunes(other, null);
        mergeSets(other, null);
        Log.v(TAG, "Merging complete");
    }

    public void mergeAsync(Context context, final TuneBook other) {
        final ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setTitle(R.string.merging_tunebook);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.show();

        progressDialogHandler = new Handler();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Log.v(TAG, "Merging tunebooks (async)");
                mergeTunes(other, progressDialog);
                mergeSets(other, progressDialog);
                Log.v(TAG, "Merging complete (async)");
                progressDialog.dismiss();
                notifyObservers(ChangedStatus.MergeComplete);
            }
        }).start();
    }

    private void mergeTunes(TuneBook other, final ProgressDialog progressDialog) {
        Log.v(TAG, "Merging tunes from another tunebook");

        HashSet<Tune> originalTunes = new HashSet<Tune>(tunes.values());

        Set<Map.Entry<String, Tune>> entries = other.tunes.entrySet();
        final int nbEntries = entries.size();
        if (progressDialog != null) {
            progressDialogHandler.post(new Runnable() {
                @Override
                public void run() {
                    progressDialog.setMessage(progressDialog.getContext().getResources().getString(R.string.merging_tunebook_tunes));
                    progressDialog.setProgress(0);
                    progressDialog.setMax(nbEntries);
                }
            });
        }

        for (Map.Entry<String, Tune> entry : entries) {
            Tune tune = entry.getValue();
            String id = entry.getKey();

            if (progressDialog != null) {
                progressDialogHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.incrementProgressBy(1);
                    }
                });
            }

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

    private void mergeSets(final TuneBook other, final ProgressDialog progressDialog) {
        Log.v(TAG, "Merging sets from another tunebook");

        final int nbSets = other.tuneSets.size();
        if (progressDialog != null) {
            progressDialogHandler.post(new Runnable() {
                @Override
                public void run() {
                    progressDialog.setMax(nbSets);
                    progressDialog.setProgress(0);
                    progressDialog.setMessage(progressDialog.getContext().getString(R.string.merging_tunebook_sets));
                }
            });
        }

        for (TuneSet set : other.tuneSets) {
            if (progressDialog != null) {
                progressDialogHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.incrementProgressBy(1);
                    }
                });
            }
            Log.d(TAG, String.format("Inserting set '%s'.", set.getTitle()));
            tuneSets.add(set);
        }

        Log.v(TAG, "Merging sets complete");
    }

    /**
     * Reacts to a notification from one of the observed objects.
     *
     * @param observable The observed object.
     * @param data       Additional data to specify what changed in the object.
     */
    @Override
    public void update(Observable observable, Object data) {
        if (observable instanceof TuneSet) {
            setChanged();
            notifyObservers(observable);
        }
    }

    /**
     * ***********************************************************************
     * Operations
     * ***********************************************************************
     */

    public enum ChangedProperty {
        Tunes, TuneSets
    }

    public enum ChangedStatus {
        MergeComplete
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
