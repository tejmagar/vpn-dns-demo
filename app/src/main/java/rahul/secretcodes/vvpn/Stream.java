package rahul.secretcodes.vvpn;

import android.util.Log;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Stream {
    private final InputStream inputStream;
    private final OutputStream outputStream;

    public Stream(FileDescriptor fileDescriptor) {
        this.inputStream = new FileInputStream(fileDescriptor);
        this.outputStream = new FileOutputStream(fileDescriptor);
    }

    public int read(byte[] buffer) throws IOException {
        return inputStream.read(buffer);
    }

    public void write(byte[] bytes) throws IOException {
        outputStream.write(bytes);
    }
}
