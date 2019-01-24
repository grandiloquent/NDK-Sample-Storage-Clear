package euphoria.psycho.calculatedirectories;

import android.Manifest;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.text.format.Formatter;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
        Arrays.sort(dirList, (o1, o2) -> collator.compare(o1.getName(), o2.getName()));
        final TextView textView = findViewById(R.id.text);
        final List<FileItem> fileItems = new ArrayList<>();

        ThreadUtils.postOnBackgroundThread(() -> {
            final StringBuilder sb = new StringBuilder();


            int count = 0;
            for (File dir : dirList) {
                count++;
                long size = NativeMethods.calculateDirectory(dir.getAbsolutePath());
                String description = Formatter.formatFileSize(MainActivity.this, size);

                FileItem fileItem = new FileItem();
                fileItem.name = dir.getName();
                fileItem.size = size;
                fileItem.description = description;
                fileItems.add(fileItem);
                ThreadUtils.postOnMainThread(() -> textView.setText("计算中...\n" + fileItem.name + " | " + fileItem.description));
                SystemClock.sleep(100);
            }
            ThreadUtils.postOnMainThread(() -> textView.setText(sb.toString()));
            Collections.sort(fileItems, (o1, o2) -> o1.size >= o2.size ? -1 : 1);
            sb.setLength(0);
            sb.append("总共计算手机内部储存中 ").append(count).append(" 个目录").append('\n');
            sb.append("从大到小依次排列: \n\n");
            sb.append("目录名").append(" | ").append("大小").append("\n\n");
            for (FileItem f : fileItems) {
                sb.append(f.name).append(" | ").append(f.description).append("\n");
            }

            ThreadUtils.postOnMainThread(() -> textView.setText(sb.toString()));
        });
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
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
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
        File externalStorageDirectory = Environment.getExternalStorageDirectory();
        if (externalStorageDirectory == null) return;
        String dir = externalStorageDirectory.getAbsolutePath();
        String[] directories = new String[]{
                dir + "/tencent",
                dir + "/.dir_com.qihoo360.feichuan",
                dir + "/DCIM/.thumbnails",
                dir + "/.thumbnails",

        };
        ThreadUtils.postOnBackgroundThread(() -> {
            NativeMethods.deleteDirectories(directories);
            ThreadUtils.postOnMainThread(() -> {
                Toast.makeText(this, "已完成清理", Toast.LENGTH_LONG).show();
            });
        });
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
