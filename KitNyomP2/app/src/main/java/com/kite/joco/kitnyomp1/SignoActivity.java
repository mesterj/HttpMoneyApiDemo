package com.kite.joco.kitnyomp1;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.kite.joco.kitnyomp1.signature.SignatureView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class SignoActivity extends AppCompatActivity {


    public static final String ALAIRO_NEVE = "ALAIRO_NEVE";
    public static final String LOGTAG = "SIGNOACTI";
    public static final String NYOMT_SORSZAM = "NYOMT_SORSZAM";
    SignatureView signature;
    EditText etOlvashatoAlairas;
    String filename;

    public static final int KEY_PARTNER_SIGNO_REQUEST_CODE = 1;
    public static final int KEY_SZAT_SIGNO_REQUEST_CODE=2;
    public static final int SIGNO_OK_KOD = 1;
    public static final int SIGNO_BAD_KOD= 2;
    public final static String NYOMTKIT_USER_PREFS = "USER_PREFS";
    public final static String NYOMTKIT_ALT_PREFS = "ALTALANOS_PREFS";
    public static final String KEY_SIGNO_READABLE = "KEY_SIGNO_READABLE";
    public static final String KEY_SIGNO_FILE_NAME = "FILENAME";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signo);
        signature = (SignatureView) findViewById(R.id.signatureView);
        etOlvashatoAlairas = (EditText) findViewById(R.id.etOlvashatoNev);
        filename = getIntent().getStringExtra(KEY_SIGNO_FILE_NAME);

    }

    public void saveSignature(View view) {

        String message = "";

        Bitmap image = signature.getImage();
        if (etOlvashatoAlairas.getText().toString().equals("") || etOlvashatoAlairas.getText() == null || (!signature.isSigned())) {
            if (signature.isSigned()) {
                message = "Az olvasható aláírást is ki kell tölteni !";
            }
            else {
                message = "Alá kell írni a lapot!";
            }
            Toast talair = Toast.makeText(this, message, Toast.LENGTH_LONG);
            talair.setGravity(Gravity.CENTER|Gravity.CENTER_HORIZONTAL,0,0);
            talair.show();
        }
        else {
            /*String olvashatoalairas = etOlvashatoAlairas.getText().toString();
            SharedPreferences userprefs = getSharedPreferences(NYOMTKIT_USER_PREFS, MODE_PRIVATE);
            SharedPreferences.Editor editor = userprefs.edit();
            editor.putString(KEY_SIGNO_READABLE, olvashatoalairas);
            //Toast.makeText(this, "Mentettem az aláírót !", Toast.LENGTH_LONG).show();
            editor.commit();*/

            String nyomtsorszam = getIntent().getExtras().getString(NYOMT_SORSZAM).toUpperCase();
            Log.i(LOGTAG,"file neve legyen: " +nyomtsorszam);

            File sd = Environment.getExternalStorageDirectory();

            File signo = new File(sd, nyomtsorszam+".PNG");
            Log.i(LOGTAG,signo.getAbsolutePath());

            try {
                if (sd.canWrite()) {
                    signo.createNewFile();
                    OutputStream os = new FileOutputStream(signo);
                    image.compress(Bitmap.CompressFormat.PNG, 90, os);
                    os.close();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            Intent returnIntent = new Intent();
            returnIntent.putExtra(ALAIRO_NEVE, etOlvashatoAlairas.getText().toString());
            returnIntent.putExtra(KEY_SIGNO_FILE_NAME,signo.getAbsolutePath());
            setResult(RESULT_OK, returnIntent);
            finish();
        }
    }

    public void ClearSigno(View v){
        signature.clearSignature();
        etOlvashatoAlairas.setText("");
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
