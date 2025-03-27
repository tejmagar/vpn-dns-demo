package rahul.secretcodes.vvpn;

import java.io.FileDescriptor;

public class RustBridge {
    static {
        System.loadLibrary("tunnel");
    }

    public static native void process(Stream stream);
}