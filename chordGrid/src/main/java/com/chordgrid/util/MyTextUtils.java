package com.chordgrid.util;

import android.text.TextUtils;

public class MyTextUtils {

    private MyTextUtils() {
    }

    public static String[] splitWithDelimiter(String text, String expression) {
        return TextUtils.split(text, String.format("(?=%1$s)", expression));
    }
}
