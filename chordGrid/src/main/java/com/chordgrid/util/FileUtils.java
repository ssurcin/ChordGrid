package com.chordgrid.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Sylvain on 22/01/2015.
 */
public class FileUtils {

    private static final String TAG = "FileUtils";

    public static void copy(File src, File dest) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dest);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    public static String getFilename(File file) {
        String path = file.getPath();
        int sep = path.lastIndexOf(File.separator);
        if (sep >= 0)
            return path.substring(sep + 1);
        return path;
    }
}
