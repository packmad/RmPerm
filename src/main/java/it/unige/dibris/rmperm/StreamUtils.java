package it.unige.dibris.rmperm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class StreamUtils {
    public static void copyAndClose(InputStream is, OutputStream os) throws IOException {
        copy(is, os);
        is.close();
        os.close();
    }

    public static void copy(InputStream is, OutputStream os) throws IOException {
        final byte[] buf = new byte[512];
        int nRead;
        while ((nRead = is.read(buf)) != -1) {
            os.write(buf, 0, nRead);
        }
    }
}
