package com.kite.joco.kitnyomp1;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.kite.joco.kitnyomp1.adapter.NyomtAdapter;
import com.kite.joco.kitnyomp1.db.Dolgozok;
import com.kite.joco.kitnyomp1.db.Klbazis1516;
import com.kite.joco.kitnyomp1.db.Klragt1516;
import com.kite.joco.kitnyomp1.db.Klsajat1516;
import com.kite.joco.kitnyomp1.db.Nyomtatvany;
import com.kite.joco.kitnyomp1.db.Partner;
import com.kite.joco.kitnyomp1.db.Sorszamok;
import com.kite.joco.kitnyomp1.interfaces.MainFragComm;
import com.kite.joco.kitnyomp1.nyomtatvany.KLBazis1516Activity;
import com.kite.joco.kitnyomp1.nyomtatvany.KLRagt1516Activity;
import com.kite.joco.kitnyomp1.nyomtatvany.KLSajat1516Activity;
import com.kite.joco.kitnyomp1.rest.ServiceGenerator;
import com.kite.joco.kitnyomp1.util.DividerItemDecoration;
import com.kite.joco.kitnyomp1.util.SqlHelper;
import com.orm.SugarRecord;
import com.orm.query.Condition;
import com.orm.query.Select;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class NyomtMainActivity extends AppCompatActivity implements MainFragComm {

    TextView tvNyomtLeiras, tvRagtdb, tvSajatdb, tvBazisdb;
    RecyclerView rcvNyomtList;
    Button btnKitolt;
    String nyomtkod = "";
    SharedPreferences nyomtkituserPrefs, nyomtkitaltprefs;
    SharedPreferences updateprefs, userprefs;
    public final static String NYOMTKIT_USER_PREFS = "USER_PREFS";
    public final static String NYOMTKIT_ALT_PREFS = "ALTALANOS_PREFS";
    public final static String NYOMTKIT_UPD_PREFS = "UPDATE_PREFS";
    public final static String NYOMTKIT_FIRST_START = "FIRST_START";
    public final static String KEY_USER_NEV = "KEY_USER_NEV";
    public final static String KEY_USER_TOSZ = "KEY_USER_TOSZ";
    public final static String KEY_USER_EMAIL = "KEY_USER_EMAIL";
    public final static String KEY_USER_SZAT = "KEY_USER_SZAT";
    public final static String KEY_USER_ALK = "KEY_USER_ALK";
    public final static String KEY_ALK_SEL_ID = "KEY_ALK_SEL_ID";

    public final static String KEY_UPD_PARTNER_TMSP = "KEY_UPD_PARTNER_TMSP";
    public final static String KEY_UPD_DOLGOZOK_TMSP = "KEY_UPD_DOLGOZOK_TMSP";
    public final static String KEY_UPD_NYOMTATVANYOK_TMSP = "KEY_UPD_NYOMTATVANY_TMSP";
    public final static String KEY_UPD_SORSZAM_TMSP = "KEY_UPD_SORSZAM_TMSP";
    public final static String KEY_UPD_PS_DB = "KEY_UPD_PS_DB";

    public static final String AUTHORITY = "com.kite.joco.kitnyomp1.SyncPackage.provider";
    public static final String ACCOUNT_TYPE = "com.kite.joco.kitnyomp1.SyncPackage";
    public static final String ACCOUNT = "dummyaccount";
    public static final String ver = "ver.1.4";

    Account myaccount;
    ContentResolver mResolver;

    Handler progHandler = new Handler();
    ProgressDialog pbDialog;
    String tosz;

    public final String LOGTAG = "NyomtMainActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_nyomt_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        nyomtkitaltprefs = getSharedPreferences(NYOMTKIT_ALT_PREFS, MODE_PRIVATE);
        updateprefs = getSharedPreferences(NYOMTKIT_UPD_PREFS, MODE_PRIVATE);
        TextView tvVersion = (TextView) findViewById(R.id.tvVersion);
        tvVersion.setText(ver);
        if (!nyomtkitaltprefs.getBoolean(NYOMTKIT_FIRST_START, false)) {
            Log.i(LOGTAG, "Ez egy első indítás!");
            pbDialog = new ProgressDialog(this);
            pbDialog.setCancelable(false);
            pbDialog.setMessage("Adatok letöltése az első indulásnál (kb 10-11 perc) ...");
            pbDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            pbDialog.setProgress(0);
            pbDialog.setMax(83000);
            pbDialog.show();

            //FirstAsyncTask firstAsyncTask = new FirstAsyncTask(updateprefs);
            //firstAsyncTask.execute("valami");
            SharedPreferences.Editor editor = nyomtkitaltprefs.edit();
            editor.putBoolean(NYOMTKIT_FIRST_START, true);
            editor.commit();
            try {
                new LetoltAsyncTask().execute();
            } catch (Exception ex) {
                Toast.makeText(this, "Valószínűleg nem kapcsolódik a KITE hálózathoz!!", Toast.LENGTH_LONG).show();
                ex.printStackTrace();
                finish();
            }

            Log.i(LOGTAG, "Mostmár true");
        } else {
            Log.i(LOGTAG, "true volt nem futott a vizsgálat");
        }
        userprefs = getSharedPreferences(NYOMTKIT_USER_PREFS, MODE_PRIVATE);
        if (userprefs.getString(KEY_USER_NEV, "").isEmpty() && SqlHelper.getPsdb()>80000) {
            userBeallit();
        }
        tosz = userprefs.getString(KEY_USER_TOSZ, "");

        // Szinkronizáció beállítása
        myaccount = new Account(ACCOUNT, ACCOUNT_TYPE);
        AccountManager accountManager = (AccountManager) this.getSystemService(ACCOUNT_SERVICE);

        if (accountManager.addAccountExplicitly(myaccount, null, null)) {
            Log.i(LOGTAG, "Account created");
        } else {
            Log.i(LOGTAG, "Account creation error");
        }

        mResolver = getContentResolver();

        ContentResolver.setSyncAutomatically(myaccount, AUTHORITY, true);

        // ebben van a frissítés sűrűségének beállítása 2-3 óra 2 óra azt eredményezte, hogy sosem frissített fél órára állítom.

        long ora = 60 * 60000;
        long felora = ora/2;
        ContentResolver.addPeriodicSync(myaccount, AUTHORITY, Bundle.EMPTY, felora);

        RecyclerView.ItemDecoration nyomtitemdecor = new DividerItemDecoration(this,DividerItemDecoration.VERTICAL_LIST);

        rcvNyomtList = (RecyclerView) findViewById(R.id.rcvNyomtList);
        rcvNyomtList.addItemDecoration(nyomtitemdecor);
        List<Nyomtatvany> nyomtatvanies = Select.from(Nyomtatvany.class).where(Condition.prop("status").eq("A")).list();
        if (nyomtatvanies == null) {
            Nyomtatvany ny = new Nyomtatvany();
            nyomtatvanies.add(ny);
        }
        NyomtAdapter adapter = new NyomtAdapter(this, nyomtatvanies);
        rcvNyomtList.setAdapter(adapter);
        rcvNyomtList.setLayoutManager(new LinearLayoutManager(this));

        tvNyomtLeiras = (TextView) findViewById(R.id.tvNyomtLeiras);
        btnKitolt = (Button) findViewById(R.id.btnKitolt);

        tvBazisdb = (TextView) findViewById(R.id.tvBazisdb);
        tvRagtdb = (TextView) findViewById(R.id.tvRagtdb);
        tvSajatdb = (TextView) findViewById(R.id.tvSajatDb);

        long ragt1db = Klragt1516.count(Klragt1516.class);
        long sajat1db = Klsajat1516.count(Klsajat1516.class);
        long bazis1db = Klbazis1516.count(Klbazis1516.class);

        tvBazisdb.setText("Bázismag kedvezménylap: " + bazis1db);
        tvSajatdb.setText("KITE kedvezménylap: " + sajat1db);
        tvRagtdb.setText("RAGT kedvezménylap: " + ragt1db);

        Log.i(LOGTAG, "Mentett RAGT nyomtatvány: " + ragt1db);
        Log.i(LOGTAG, "Mentett MV nyomtatvány: " + bazis1db);
        Log.i(LOGTAG, "Mentett SAJÁT nyomtatvány: " + sajat1db);
        Log.i(LOGTAG, "Betöltött partner adat: " + SqlHelper.getPsdb());
    }

    public void userBeallit() {
        // User beállító intent
        Intent szatIntent = new Intent(getApplicationContext(), User_beallit.class);
        startActivity(szatIntent);
        finish();
    }

    public void handleDialogProgress(int p) {
        pbDialog.setProgress(p);
    }

    @Override
    protected void onResume() {
        super.onResume();
        userprefs = getSharedPreferences(NYOMTKIT_USER_PREFS, MODE_PRIVATE);
        if (userprefs.getString(KEY_USER_NEV, "").isEmpty() && SqlHelper.getPsdb()>80000) {
            userBeallit();
        }
        //long nyomtdb = Klragt1516.count(Klragt1516.class)+Klbazis1516.count(Klbazis1516.class)+Klsajat1516.count(Klsajat1516.class);
        long ragtdb = Klragt1516.count(Klragt1516.class);
        long sajatdb = Klsajat1516.count(Klsajat1516.class);
        long bazisdb = Klbazis1516.count(Klbazis1516.class);

        tvBazisdb.setText("Bázismag kedvezménylap: " + bazisdb);
        tvSajatdb.setText("KITE kedvezménylap: " + sajatdb);
        tvRagtdb.setText("RAGT kedvezménylap: " + ragtdb);

        Log.i(LOGTAG, "Mentett RAGT nyomtatvány: " + ragtdb);
        Log.i(LOGTAG, "Mentett MV nyomtatvány: " + bazisdb);
        Log.i(LOGTAG, "Mentett SAJÁT nyomtatvány: " + sajatdb);


        List<Sorszamok> sorszamokList = Select.from(Sorszamok.class).list();

        for (Sorszamok s : sorszamokList) {
            Log.i(LOGTAG, " tosz: " + s.getTosz() + " nyomt-kod: " + s.getNyomtkod() + " utolsó használt sorszám: " + s.getSorszam());
        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_nyomt_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_beallitas) {
            Intent beallitIntent = new Intent(this, User_beallit.class);
            startActivity(beallitIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void selectednyomt(Nyomtatvany nyomt) {
        Log.i(LOGTAG, "Nyomtatvány kód: " + nyomt.getNyomtKod());
        nyomtkod = nyomt.getNyomtKod();
        tvNyomtLeiras.setText(nyomt.getNyomtLeiras());
        btnKitolt.setEnabled(true);
    }

    public void startSync(View v) {
        Bundle b = new Bundle();
        b.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        b.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        ContentResolver.requestSync(myaccount, AUTHORITY, b);

        long ragtdb = Klragt1516.count(Klragt1516.class);
        long sajatdb = Klsajat1516.count(Klsajat1516.class);
        long bazisdb = Klbazis1516.count(Klbazis1516.class);

        tvBazisdb.setText("Bázismag kedvezménylap: " + bazisdb);
        tvSajatdb.setText("KITE kedvezménylap: " + sajatdb);
        tvRagtdb.setText("RAGT kedvezménylap: " + ragtdb);
    }

    public void onClick(View v) {
        switch (nyomtkod) {
            case ("ELP2015"):
                elp2015kitolt();
                break;
            case (KLBazis1516Activity.NYOMT_KOD):
                baziskitolt();
                break;
            case (KLRagt1516Activity.NYOMT_KOD):
                ragtkitolt();
                break;
            case (KLSajat1516Activity.NYOMT_KOD):
                sajatkitolt();
                break;
            default:
                Toast t = Toast.makeText(this, "A kiválasztott nyomtatványhoz nem tartozik kitöltő képernyő", Toast.LENGTH_LONG);
                t.setGravity(Gravity.CENTER | Gravity.CENTER_HORIZONTAL, 0, 0);
                t.show();
                break;
        }
    }

    private void sajatkitolt() {
        Intent sajatintent = new Intent(this, KLSajat1516Activity.class);
        startActivity(sajatintent);
    }

    private void ragtkitolt() {
        Intent ragtintent = new Intent(this, KLRagt1516Activity.class);
        startActivity(ragtintent);
    }

    private void baziskitolt() {
        Intent bazisintent = new Intent(this, KLBazis1516Activity.class);
        startActivity(bazisintent);
    }

    public void elp2015kitolt() {
        Toast.makeText(this, "Ez lesz a Proba kitöltő", Toast.LENGTH_LONG).show();
    }

    void saveActualTimestamp(String key) {
        SharedPreferences.Editor updateeditor = updateprefs.edit();
        updateeditor.putLong(key, getActualTimestamp());
        updateeditor.commit();
    }

    void saveActualPsdb(){
        SharedPreferences.Editor updateeditor = updateprefs.edit();
        updateeditor.putLong(KEY_UPD_PS_DB, SqlHelper.getPsdb());
        updateeditor.commit();
    }

    private Long getActualTimestamp() {
        Calendar c = GregorianCalendar.getInstance(new Locale("HU"));
        long timestamp = c.getTimeInMillis();
        return timestamp;
    }

    class LetoltAsyncTask extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {
            final Thread t = new Thread() {
                @Override
                public void run() {
                    int jumptime = 0;
                    int totalProgressTime = 83000;
                    while (jumptime < totalProgressTime) {
                        try {
                            sleep(530);
                            jumptime += 100;
                            publishProgress(jumptime);
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            };

            t.start();

            ServiceGenerator.get().getAsyncListofDolgozok(new Callback<List<Dolgozok>>() {
                @Override
                public void success(List<Dolgozok> dolgozoks, Response response) {
                    SugarRecord.saveInTx(dolgozoks);
                    Log.i(LOGTAG, "Dolgozok letöltve");
                }

                @Override
                public void failure(RetrofitError error) {
                    Log.e(LOGTAG, "Dolgozo adatok letöltése sikertlen : " + error.getMessage());
                }
            });

            ServiceGenerator.get().getAsyncListofNyomtatvany(new Callback<List<Nyomtatvany>>() {
                @Override
                public void success(List<Nyomtatvany> nyomtatvanies, Response response) {
                    SugarRecord.saveInTx(nyomtatvanies);
                    Log.i(LOGTAG, "Nyomtaványok letöltve");
                }

                @Override
                public void failure(RetrofitError error) {
                    Log.e(LOGTAG, "Nyomtatványok letöltése sikertelen: " + error.getMessage());
                }
            });

            // Szinkron letöltés. Ekkor jó sokáig nem lehet a tablethez nyúlni, de a végén ott van minden partner.

            try {
                List<Partner> partners = ServiceGenerator.get().getListofPartner();
                SugarRecord.saveInTx(partners);
            } catch (Exception ex) {
                cancel(true);
            }


            // Aszinkron letöltés. A háttérben szedi le a partnereket de akkor amikor felraktad akkor nincs ott "rögtön" a partner törzs. Mint Itware-k.
            // Csak itt nincs külön gomb a partner letöltésre hanem itt és most elkezdi a letöltést és kb. 10 perc múlva ott is lesz a teljes partnertörsz

            /*ServiceGenerator.get().getAsyncListofPartner(new Callback<List<Partner>>() {
                @Override
                public void success(List<Partner> partnerList, Response response) {
                    SugarRecord.saveInTx(partnerList);
                }

                @Override
                public void failure(RetrofitError error) {

                }
            });*/

            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            handleDialogProgress(values[0]);
        }

        @Override
        protected void onPostExecute(String s) {
            pbDialog.dismiss();
            saveActualTimestamp(NyomtMainActivity.KEY_UPD_NYOMTATVANYOK_TMSP);
            saveActualTimestamp(NyomtMainActivity.KEY_UPD_DOLGOZOK_TMSP);
            saveActualTimestamp(NyomtMainActivity.KEY_UPD_PARTNER_TMSP);
            saveActualPsdb();
            userBeallit();
        }
    }
}
