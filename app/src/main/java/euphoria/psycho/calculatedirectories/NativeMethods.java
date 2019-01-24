package euphoria.psycho.calculatedirectories;

public class NativeMethods {
    static {
        System.loadLibrary("main");
    }

    public static native long calculateDirectory(String dir);
    public static native int deleteDirectories(String[] directories);
}
