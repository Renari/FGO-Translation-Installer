package com.arimil.fgo.fategotranslation;

import android.content.res.AssetManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.Runtime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView)findViewById(R.id.textView);
    }

    protected String sudoForResult(String...strings) {
        String res = "";
        DataOutputStream outputStream = null;
        InputStream response = null;
        try{
            Process su = Runtime.getRuntime().exec("su");
            outputStream = new DataOutputStream(su.getOutputStream());
            response = su.getInputStream();

            for (String s : strings) {
                outputStream.writeBytes(s+"\n");
                outputStream.flush();
            }

            outputStream.writeBytes("exit\n");
            outputStream.flush();
            try {
                su.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            res = readFully(response);
        } catch (IOException e){
            textView.setText(textView.getText() + "\nAn error has occurred, you probably don't have root.");
        } finally {
            Closer.closeSilently(outputStream, response);
        }
        return res;
    }
    public static String readFully(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length = 0;
        while ((length = is.read(buffer)) != -1) {
            baos.write(buffer, 0, length);
        }
        return baos.toString("UTF-8");
    }

    public void install(View view)
    {
        try {
            textView.setText("Copying patch to: " + getFilesDir());
            String result = sudoForResult("ls -l -n /data/data/com.aniplex.fategrandorder/files");
            Pattern p = Pattern.compile("(?:\\s*)?[-rwx]*\\s*(\\d+)\\s*\\d+\\s*\\d+\\s*[\\d-]*\\s*[\\d:]*\\s(?:.*)\\.dat");
            Matcher m = p.matcher(result);
            String id;
            if(m.find()) {
                id = m.group(1);
                textView.setText(textView.getText() + "\nGetting guid and uid: " + id);
            }
            else {
                textView.setText(textView.getText() + "\nUnable to get guid/uid.");
                return;
            }
            textView.setText(textView.getText() + "\nCopying assets...");
            copyAssets();
            textView.setText(textView.getText() + " done");
            File folder = new File("/data/data/com.arimil.fgo.fategotranslation/files");
            File[] list = folder.listFiles();
            for (File f : list) {
                sudoForResult("mv /data/data/com.arimil.fgo.fategotranslation/files/" + f.getName() + " " +
                        "/data/data/com.aniplex.fategrandorder/files/test/" + f.getName());
                sudoForResult("chown " + id + "." + id + " " + "/data/data/com.aniplex.fategrandorder/files/" + f.getName());
                textView.setText(textView.getText() + "\nProcessed file: " + f.getName());
            }
            textView.setText(textView.getText() + "\npatch complete");
        }
        catch (Exception e)
        {
            textView.setText(textView.getText() + "\nSomething bad happened and we weren't able to complete the patch. \n"+e.toString());
        }

    }

    private void copyAssets() {
        AssetManager assetManager = getAssets();
        String[] files = null;
        try {
            files = assetManager.list("");
        } catch (IOException e) {
            //NOOP
        }
        for(String filename : files) {
            InputStream in = null;
            OutputStream out = null;
            try {
                in = assetManager.open(filename);
                File outFile = new File(getFilesDir(), filename);
                out = new FileOutputStream(outFile);
                copyFile(in, out);
            } catch(IOException e) {
                //NOOP
            }
            finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        // NOOP
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        // NOOP
                    }
                }
            }
        }
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}