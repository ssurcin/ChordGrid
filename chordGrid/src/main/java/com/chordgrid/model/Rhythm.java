package com.chordgrid.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import com.chordgrid.R;
import com.chordgrid.util.StaticObserver;
import com.chordgrid.util.StorageUtil;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

public class Rhythm implements Comparable<Rhythm>, Parcelable {

    /**
     * The shared preference name for custom preferences.
     */
    public static final String PREFS_NAME_CUSTOM = "CustomPrefs";
    /**
     * The shared preference key for rhythms.
     */
    public static final String PREFS_KEY_RHYTHMS = "Rhythms";
    /**
     * A factory to create Rhythm instances and instance arrays from a parcel.
     */
    public static final Creator<Rhythm> CREATOR = new Creator<Rhythm>() {
        @Override
        public Rhythm createFromParcel(Parcel source) {
            return new Rhythm(source);
        }

        @Override
        public Rhythm[] newArray(int size) {
            return new Rhythm[size];
        }
    };
    private static final String TAG = "Rhythm";
    /**
     * The set of static observers.
     */
    private static final HashSet<StaticObserver> OBSERVERS = new HashSet<StaticObserver>();

    /**
     * A cache to avoid parsing the known rhythms too often.
     * Just update it each time the preferences are modified.
     */
    private static Set<Rhythm> KNOWN_RHYTHMS;

    /**
     * The rhythm's name.
     */
    private String mName;
    /**
     * The time signature expressed as x/y.
     */
    private String mSignature;
    /**
     * The signature's numerator.
     */
    private volatile int mNumerator;
    /**
     * The signature's denominator.
     */
    private volatile int mDenominator;
    /**
     * The number of beats per bar.
     */
    private int mBeatsPerBar;

    ////////////////////////////////////////////////////////////////////////////////////////////
    // Construction
    ////////////////////////////////////////////////////////////////////////////////////////////

    public Rhythm(String name, String signature, int beatsPerBar) {
        mName = name;
        mSignature = signature;
        mBeatsPerBar = beatsPerBar;
        analyzeSignature();
    }

    /**
     * Builds a new instance of Rhythm from a data serialized in a parcel.
     *
     * @param sourceParcel The source parcel.
     */
    public Rhythm(Parcel sourceParcel) {
        readFromParcel(sourceParcel);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////
    // Properties
    ////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Initializes the set of known rhythms from the shared preferences.
     *
     * @param context The activity context allowing to access the shared preferences.
     */
    public static void initializeKnownRhythms(Context context) {
        Log.d(TAG, "Initializing known rhythms");

        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME_CUSTOM, Context.MODE_PRIVATE);
        String serializedRhythms = sharedPreferences.getString(PREFS_KEY_RHYTHMS, "");
        if (TextUtils.isEmpty(serializedRhythms)) {
            Log.d(TAG, "Defaulting rhythms");
            try {
                serializedRhythms = StorageUtil.convertStreamToString(context.getResources()
                        .openRawResource(R.raw.default_rhythms));
            } catch (IOException e) {
                Log.e(TAG, "Cannot load default rhythms: " + e.getMessage());
                serializedRhythms = "";
            }
        }

        KNOWN_RHYTHMS = parseLines(serializedRhythms);

        for (Rhythm rhythm : KNOWN_RHYTHMS)
            Log.d(TAG, "  " + rhythm);
    }

    /**
     * Parses a string containing a collection of serialized Rhythms (one per line).
     *
     * @param serializedRhythms The string containing the serizalized Rhythms.
     * @return An ordered set of Rhythm instances.
     */
    public static Set<Rhythm> parseLines(String serializedRhythms) {
        Set<Rhythm> rhythms = new TreeSet<Rhythm>(new Comparator<Rhythm>() {
            @Override
            public int compare(Rhythm lhs, Rhythm rhs) {
                return lhs.getName().compareTo(rhs.getName());
            }
        });

        String[] lines = serializedRhythms.split("\\s*\\n");
        for (String line : lines) {
            try {
                rhythms.add(Rhythm.parse(line));
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Cannot parse serialized rhythm: " + e.getMessage());
            }
        }

        return rhythms;
    }

    /**
     * Gets the set of known rhythms from the shared preferences.
     */
    public static Set<Rhythm> getKnownRhythms() {
        return KNOWN_RHYTHMS;
    }

    public static Rhythm getKnownRhythm(String name) {
        for (Rhythm rhythm : getKnownRhythms()) {
            if (rhythm.getName().equalsIgnoreCase(name))
                return rhythm;
        }
        throw new IllegalArgumentException("Unknown rhythm " + name);
    }

    public static void addKnownRhythm(Rhythm rhythm) {
        KNOWN_RHYTHMS.add(rhythm);
    }

    public static void addKnownRhythms(Collection<Rhythm> rhythms) {
        KNOWN_RHYTHMS.addAll(rhythms);
    }

    public static void saveKnownRhyhms(Context context) {
        saveKnownRhythms(KNOWN_RHYTHMS, context);
    }

    public static void saveKnownRhythms(Set<Rhythm> rhythms, Context context) {
        Log.d(TAG, "Serializing custom preferences");

        SharedPreferences sharedPreferences = context.getSharedPreferences(Rhythm.PREFS_NAME_CUSTOM, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        TreeSet<Rhythm> sortedRhythms = new TreeSet<Rhythm>(new Comparator<Rhythm>() {
            @Override
            public int compare(Rhythm lhs, Rhythm rhs) {
                return lhs.getName().compareTo(rhs.getName());
            }
        });
        sortedRhythms.addAll(rhythms);

        KNOWN_RHYTHMS = sortedRhythms;

        StringBuilder sb = new StringBuilder();
        for (Rhythm rhythm : sortedRhythms) {
            sb.append(rhythm.toString()).append("\n");
        }
        editor.putString(Rhythm.PREFS_KEY_RHYTHMS, sb.toString());
        Log.d(TAG, "Rhythms = \n" + sb.toString());

        editor.commit();
    }

    private void analyzeSignature() {
        if (mSignature == null || mSignature.isEmpty())
            throw new IllegalArgumentException("Rhythm signature cannot be null or empty!");
        if ("C".equals(mSignature))
            mNumerator = mDenominator = 4;
        int sep = mSignature.indexOf('/');
        if (sep < 0)
            throw new IllegalArgumentException("Rhythm signature must contain '/'!");
        mNumerator = Integer.parseInt(mSignature.substring(0, sep));
        mDenominator = Integer.parseInt(mSignature.substring(sep + 1));
    }

    public String getName() {
        return mName;
    }

    public String getSignature() {
        return mSignature;
    }

    public int getBeatsPerBar() {
        return mBeatsPerBar;
    }

    @Override
    public int compareTo(Rhythm another) {
        return getName().compareTo(another.getName());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Rhythm))
            return false;
        if (o == this)
            return true;

        Rhythm rhythm = (Rhythm) o;
        if (mName == null && rhythm.mName != null)
            return false;
        if (mName != null && !mName.equalsIgnoreCase(rhythm.mName))
            return false;

        if (mSignature == null && rhythm.mSignature != null)
            return false;
        if (mSignature != null && !mSignature.equalsIgnoreCase(rhythm.mSignature))
            return false;

        if (mBeatsPerBar != rhythm.mBeatsPerBar)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int hash = 1;
        hash = hash * 31 + (mName == null ? 0 : mName.hashCode());
        hash = hash * 31 + (mSignature == null ? 0 : mSignature.hashCode());
        hash = hash * 31 + mBeatsPerBar;
        return hash;
    }

    /**
     * Reads the data serialized in a parcel.
     *
     * @param sourceParcel The source parcel.
     */
    public void readFromParcel(Parcel sourceParcel) {
        mName = sourceParcel.readString();
        mSignature = sourceParcel.readString();
        mBeatsPerBar = sourceParcel.readInt();
        analyzeSignature();
    }

    /**
     * Saves serialized data to a parcel.
     *
     * @param destParcel The destination parcel.
     * @param flags      Some flags.
     */
    @Override
    public void writeToParcel(Parcel destParcel, int flags) {
        destParcel.writeString(mName);
        destParcel.writeString(mSignature);
        destParcel.writeInt(mBeatsPerBar);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Serializes this instance as a JSON string.
     */
    public String jsonSerialize() {
        return new Gson().toJson(this);
    }

    public static class RhythmSet extends TreeSet<Rhythm> {
    }

    @Override
    public String toString() {
        return java.lang.String.format("[%s, %s, %d bpb]", getName(), getSignature(), getBeatsPerBar());
    }

    public static Rhythm parse(String string) throws IllegalArgumentException {
        if (string == null) throw new IllegalArgumentException("Cannot parse null string!");
        if (string.isEmpty()) throw new IllegalArgumentException("Cannot parse empty string!");
        if (string.startsWith("[") && string.endsWith("]")) {
            String s = string.substring(1, string.length() - 1);
            String[] items = s.split(",\\s*");
            if (items.length == 3) {
                String name = items[0];
                String signature = items[1];
                int indexBpb = items[2].indexOf(" bpb");
                if (indexBpb > 0) {
                    try {
                        int bpb = Integer.parseInt(items[2].substring(0, indexBpb));
                        return new Rhythm(name, signature, bpb);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, e.getMessage());
                    }
                }
            }
        }
        throw new IllegalArgumentException("Ill-formed Rhythm string: " + string);
    }
}
