package com.chordgrid.model;

import java.util.ArrayList;

public class Rythm implements Comparable<Rythm> {

    public static final Rythm Jig = new Rythm("Jig", "6/8", 2);
    public static final Rythm Reel = new Rythm("Reel", "4/4", 4);
    public static final Rythm Hornpipe = new Rythm("Hornpipe", "4/4", 4);
    public static final Rythm Polka = new Rythm("Polka", "2/4", 2);
    public static final Rythm SlipJig = new Rythm("Slip Jig", "9/8", 3);
    public static final Rythm Waltz = new Rythm("Waltz", "3/4", 3);
    public static final Rythm Mazurka = new Rythm("Mazurka", "3/4", 3);

    public static final ArrayList<Rythm> KnownRythms;

    static {
        KnownRythms = new ArrayList<Rythm>();
        KnownRythms.add(Jig);
        KnownRythms.add(Reel);
        KnownRythms.add(Hornpipe);
        KnownRythms.add(Polka);
        KnownRythms.add(SlipJig);
        KnownRythms.add(Waltz);
        KnownRythms.add(Mazurka);
    }

    public static Rythm parse(String name) {
        for (Rythm rythm : KnownRythms) {
            if (rythm.getName().equalsIgnoreCase(name))
                return rythm;
        }
        throw new IllegalArgumentException("Unknown rythm " + name);
    }

    private String name;
    private String signature;
    private int numerator;
    private int denominator;
    private int beatsPerBar;

    public Rythm(String name, String signature, int beatsPerBar) {
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
    public int compareTo(Rythm another) {
        return getName().compareTo(another.getName());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Rythm))
            return false;
        if (o == this)
            return true;

        Rythm rythm = (Rythm) o;
        if (name == null && rythm.name != null)
            return false;
        if (name != null && !name.equalsIgnoreCase(rythm.name))
            return false;

        if (signature == null && rythm.signature != null)
            return false;
        if (signature != null && !signature.equalsIgnoreCase(rythm.signature))
            return false;

        if (beatsPerBar != rythm.beatsPerBar)
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
