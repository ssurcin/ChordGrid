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

    /**
     * Gets the set of known rhythms from the shared preferences.
     *
     * @param context The activity context allowing to access the shared preferences.
     * @return A set of rhythms.
     */
    public static Set<Rhythm> getKnownRhythms(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME_CUSTOM, Context.MODE_PRIVATE);
        String serializedRhythms = sharedPreferences.getString(PREFS_KEY_RHYTHMS, "");
        if (TextUtils.isEmpty(serializedRhythms)) {
            Log.d(TAG, "Defaulting rhythms");
            try {
                serializedRhythms = StorageUtil.convertStreamToString(context.getResources()
                        .openRawResource(R.raw.default_rhythms));
            } catch (IOException e) {
                Log.e(TAG, "Cannot load default rhythms: " + e.getMessage());
                serializedRhythms = "[]";
            }
        }
        return new Gson().fromJson(serializedRhythms, Rhythm.RhythmSet.class);
    }

    public static Rhythm parse(String name, Set<Rhythm> knownRhythms) {
        for (Rhythm rhythm : knownRhythms) {
            if (rhythm.getName().equalsIgnoreCase(name))
                return rhythm;
        }
        throw new IllegalArgumentException("Unknown rhythm " + name);
    }

    /**
     * Deserializes a Rhythm instance from a JSON string.
     *
     * @param jsonString A JSON string.
     * @return A Rhythm instance.
     */
    static public Rhythm jsonDeserialize(String jsonString) {
        return new Gson().fromJson(jsonString, Rhythm.class);
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
}
