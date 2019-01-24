package euphoria.psycho.calculatedirectories;

import android.Manifest;
import android.app.Activity;
import android.app.admin.NetworkEvent;
import android.os.Build;
import android.os.Environment;
import android.os.SystemClock;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.io.File;
import java.io.FileFilter;
import java.nio.file.Files;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String TAG = "TAG/" + MainActivity.class.getCanonicalName();

    private void calculate() {
        final File[] dirList = Environment.getExternalStorageDirectory().listFiles(pathname -> {
            if (pathname.isDirectory()) return true;
            return false;
        });
        final Collator collator = Collator.getInstance(Locale.CHINA);
        Arrays.sort(dirList, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return collator.compare(o1.getName(), o2.getName());
            }
        });
        final TextView textView = findViewById(R.id.text);
        final List<FileItem> fileItems = new ArrayList<>();

        new Thread(new Runnable() {
            @Override
            public void run() {
                final StringBuilder sb = new StringBuilder();


                int count = 0;
                for (File dir : dirList) {
                    count++;
                    long size = dirSize(dir);
                    String description = Formatter.formatFileSize(MainActivity.this, size);
                    sb.append(dir.getName() + " 目录大小为: " + description).append('\n');
                    FileItem fileItem = new FileItem();
                    fileItem.name = dir.getName();
                    fileItem.size = size;
                    fileItem.description = description;
                    fileItems.add(fileItem);
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textView.setText(sb.toString());
                        }
                    });
                    SystemClock.sleep(500);
                }
                sb.append("总共 " + count + " 目录");
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textView.setText(sb.toString());
                    }
                });

                Collections.sort(fileItems, new Comparator<FileItem>() {
                    @Override
                    public int compare(FileItem o1, FileItem o2) {
                        return o1.size >= o2.size ? -1 : 1;
                    }
                });
                sb.setLength(0);
                sb.append("所有目录，从大到小依次排列: \n\n");
                for (FileItem f : fileItems) {
                    sb.append(f.name + "  <==> " + f.description).append("\n");
                }
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textView.setText(sb.toString());
                    }
                });
            }
        }).start();
    }

    private static long dirSize(File dir) {

        if (dir.exists()) {
            long result = 0;
            File[] fileList = dir.listFiles();
            for (int i = 0; i < fileList.length; i++) {
                // Recursive call if it's a directory
                if (fileList[i].isDirectory()) {
                    result += dirSize(fileList[i]);
                } else {
                    // Sum the file size in bytes
                    result += fileList[i].length();
                }
            }
            return result; // return the file size
        }
        return 0;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
        } else
            calculate();


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_clear:
                actionClear();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void actionClear() {
        String dir = new File(Environment.getExternalStorageDirectory(), "Android").getAbsolutePath();
        Log.e(TAG, NativeMethods.calculateDirectory(dir) + " ");
        //deleteDir(new File(Environment.getExternalStorageDirectory(),""));
    }

    void deleteDir(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                deleteDir(f);
            }
        }
        file.delete();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        calculate();
    }

    private static class FileItem {
        public String description;
        public String name;
        public long size;
    }
}
