package com.kite.joco.selfupdateappp2;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import android.view.View;
import android.widget.Button;


public class MainActivity extends AppCompatActivity {

   public ProgressDialog pbDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnRefresh = (Button) findViewById(R.id.btnRefresh);
        pbDialog = new ProgressDialog(this);

    }

    public void onClick(View v){

        UpdateApp updateapp = new UpdateApp();
       // updateapp.setContext(getApplicationContext());

        //pbDialog = new ProgressDialog(this);
        pbDialog.setCancelable(false);
        pbDialog.setMessage("Frissítés file letöltése ...");
        pbDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        pbDialog.setProgress(0);
        pbDialog.setMax(1000);
        pbDialog.show();

        updateapp.execute("http://192.168.3.115/ugyvitel/apk/update.apk");
    }

       public void handleDialogProgress(int p) {        pbDialog.setProgress(p);
    }


/**
 * Created by Mester József on 2016.05.23..
 *
 * Meghívása:
 * updateapp = new UpdateApp();
 * updateapp.setContext(getApplicationContext();
 * updateapp.execute("http://ugyvitel.kite.hu/ugyvitel/apk/nyk5.apk");
 */
class UpdateApp extends AsyncTask<String,Integer, String> {
    /*private Context context;
    public void setContext(Context contextf){
        context = contextf;
    }*/

    @Override
    protected String doInBackground(String... arg0) {

        final Thread t = new Thread() {
            @Override
            public void run() {
                int jumptime = 0;
                int totalProgressTime = 1000;
                while (jumptime < totalProgressTime) {
                    try {
                        sleep(10000);
                        jumptime += 1;
                        publishProgress(jumptime);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        };

        t.start();


        try {
            URL url = new URL(arg0[0]);
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setRequestMethod("GET");
            c.setDoOutput(true);
            c.connect();

            String PATH = "/mnt/sdcard/Download/";
            File file = new File(PATH);
            file.mkdirs();
            File outputFile = new File(file, "update.apk");
            if(outputFile.exists()){
                outputFile.delete();
            }
            FileOutputStream fos = new FileOutputStream(outputFile);

            InputStream is = c.getInputStream();

            byte[] buffer = new byte[1024];
            int len1 = 0;
            while ((len1 = is.read(buffer)) != -1) {
                fos.write(buffer, 0, len1);
            }
            fos.close();
            is.close();

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(new File("/mnt/sdcard/Download/update.apk")), "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // without this flag android returned a intent error!
            startActivity(intent);


        } catch (Exception e) {
            Log.e("UpdateAPP", "Update error! " + e.getMessage());
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        handleDialogProgress(values[0]);
    }


    @Override
    protected void onPostExecute(String s) {
        pbDialog.dismiss();
    }
}

}