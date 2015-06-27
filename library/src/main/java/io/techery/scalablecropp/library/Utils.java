package io.techery.scalablecropp.library;

import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author GT
 */
public class Utils {

    public static Uri getImageUri(String path) {
        return Uri.fromFile(new File(path));
    }


    public static void copyStream(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
    }


    public static void copy(InputStream inputStream, File toFIle) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(toFIle);
        Utils.copyStream(inputStream, fileOutputStream);
        fileOutputStream.close();
        inputStream.close();
    }
}
