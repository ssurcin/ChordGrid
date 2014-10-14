package com.chordgrid.model;

import java.util.ArrayList;

public class Rhythm implements Comparable<Rhythm> {

    public static final Rhythm Jig = new Rhythm("Jig", "6/8", 2);
    public static final Rhythm Reel = new Rhythm("Reel", "4/4", 4);
    public static final Rhythm Hornpipe = new Rhythm("Hornpipe", "4/4", 4);
    public static final Rhythm Polka = new Rhythm("Polka", "2/4", 2);
    public static final Rhythm SlipJig = new Rhythm("Slip Jig", "9/8", 3);
    public static final Rhythm Waltz = new Rhythm("Waltz", "3/4", 3);
    public static final Rhythm Mazurka = new Rhythm("Mazurka", "3/4", 3);

    public static final ArrayList<Rhythm> KNOWN_RHYTHMs;

    static {
        KNOWN_RHYTHMs = new ArrayList<Rhythm>();
        KNOWN_RHYTHMs.add(Jig);
        KNOWN_RHYTHMs.add(Reel);
        KNOWN_RHYTHMs.add(Hornpipe);
        KNOWN_RHYTHMs.add(Polka);
        KNOWN_RHYTHMs.add(SlipJig);
        KNOWN_RHYTHMs.add(Waltz);
        KNOWN_RHYTHMs.add(Mazurka);
    }

    public static Rhythm parse(String name) {
        for (Rhythm rhythm : KNOWN_RHYTHMs) {
            if (rhythm.getName().equalsIgnoreCase(name))
                return rhythm;
        }
        throw new IllegalArgumentException("Unknown rythm " + name);
    }

    private String name;
    private String signature;
    private int numerator;
    private int denominator;
    private int beatsPerBar;

    public Rhythm(String name, String signature, int beatsPerBar) {
        this.name = name;
        this.signature = signature;
        this.beatsPerBar = beatsPerBar;
        analyzeSignature();
    }

    private void analyzeSignature() {
        if (signature == null || signature.isEmpty())
            throw new IllegalArgumentException("Rhythm signature cannot be null or empty!");
        if ("C".equals(signature))
            numerator = denominator = 4;
        int sep = signature.indexOf('/');
        if (sep < 0)
            throw new IllegalArgumentException("Rhythm signature must contain '/'!");
        numerator = Integer.parseInt(signature.substring(0, sep));
        denominator = Integer.parseInt(signature.substring(sep + 1));
    }

    public String getName() {
        return name;
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
        if (name == null && rhythm.name != null)
            return false;
        if (name != null && !name.equalsIgnoreCase(rhythm.name))
            return false;

        if (signature == null && rhythm.signature != null)
            return false;
        if (signature != null && !signature.equalsIgnoreCase(rhythm.signature))
            return false;

        if (beatsPerBar != rhythm.beatsPerBar)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int hash = 1;
        hash = hash * 31 + (name == null ? 0 : name.hashCode());
        hash = hash * 31 + (signature == null ? 0 : signature.hashCode());
        hash = hash * 31 + beatsPerBar;
        return hash;
    }
}
