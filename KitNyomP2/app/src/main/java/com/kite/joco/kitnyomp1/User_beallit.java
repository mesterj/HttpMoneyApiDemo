package com.kite.joco.kitnyomp1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import com.kite.joco.kitnyomp1.adapter.DolgozoAdapter;
import com.kite.joco.kitnyomp1.db.Dolgozok;
import com.kite.joco.kitnyomp1.db.Sorszamok;
import com.kite.joco.kitnyomp1.rest.ServiceGenerator;
import com.kite.joco.kitnyomp1.util.SqlHelper;
import com.orm.query.Select;

import java.util.List;

import retrofit.RetrofitError;
import retrofit.client.Response;

public class User_beallit extends AppCompatActivity {

    private static final String LOGTAG = "UserBeallit";
    AutoCompleteTextView textView;
    Dolgozok selectedDolgozo;
    String textViewCaption = "";
    final int[] psdb = {0};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_beallit);
        SharedPreferences userprefs = getApplicationContext().getSharedPreferences(NyomtMainActivity.NYOMTKIT_USER_PREFS, MODE_PRIVATE);

        List<Dolgozok> dolgozokList = Select.from(Dolgozok.class).list();
        if (dolgozokList.size() == 0 || dolgozokList == null) {
           recreate();
        }

        String aktTosz = userprefs.getString(NyomtMainActivity.KEY_USER_TOSZ, "");
        if (aktTosz.isEmpty()) {

        } else {
            selectedDolgozo = SqlHelper.getByTosz(userprefs.getString(NyomtMainActivity.KEY_USER_TOSZ, ""));
            textViewCaption = selectedDolgozo.getNev();
        }

        ArrayAdapter<Dolgozok> adapter = new DolgozoAdapter(this,
                R.layout.dolgozoitem, dolgozokList);
        textView = (AutoCompleteTextView)
                findViewById(R.id.autotextNev);
        textView.setAdapter(adapter);
        textView.setText(textViewCaption);


        textView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedDolgozo = (Dolgozok) parent.getAdapter().getItem(position);
            }
        });
    }

      /*@Override
    public void onBackPressed() {
        Toast t = Toast.makeText(this,"A nevet be kell írni, és a mentés gommbal lehet kilépni!",Toast.LENGTH_LONG);
        t.setGravity(Gravity.CENTER|Gravity.CENTER_HORIZONTAL,0,0);
        t.show();
    }*/

    public void onSave(View v) {
        SharedPreferences sharedPreferences = getSharedPreferences(NyomtMainActivity.NYOMTKIT_USER_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor usereditor = sharedPreferences.edit();
        usereditor.putString(NyomtMainActivity.KEY_USER_NEV, selectedDolgozo.getNev());
        usereditor.putString(NyomtMainActivity.KEY_USER_TOSZ, selectedDolgozo.getTosz());
        usereditor.putString(NyomtMainActivity.KEY_USER_ALK, selectedDolgozo.getAlkrovidkod());
        usereditor.putString(NyomtMainActivity.KEY_USER_SZAT, selectedDolgozo.getUzletkotokod());
        usereditor.putString(NyomtMainActivity.KEY_USER_EMAIL, selectedDolgozo.getEmail());

        int myalknuminspinner = 0;
        String[] alkrovidkod = getResources().getStringArray(R.array.alkozpontrovidkod);
        for (int alksorszam = 0; alksorszam < alkrovidkod.length; alksorszam++) {
            if (selectedDolgozo.getAlkrovidkod().equals(alkrovidkod[alksorszam])) {
                myalknuminspinner = alksorszam;
            }
        }
        usereditor.putInt(NyomtMainActivity.KEY_ALK_SEL_ID, myalknuminspinner);

        usereditor.commit();
        if (!SqlHelper.isInSorszamDb(selectedDolgozo.getTosz())) {
            ServiceGenerator.get().getAsyncSorszamok(selectedDolgozo.getTosz(), new retrofit.Callback<List<Sorszamok>>() {
                @Override
                public void success(List<Sorszamok> sorszamoks, Response response) {
                    for (Sorszamok sorszamok: sorszamoks){
                        sorszamok.setServerId(sorszamok.getId());
                        Log.i(LOGTAG,"id (sima) : "+ sorszamok.getId() + " tosz "+ sorszamok.getTosz() + " nyomtkod : "+ sorszamok.getNyomtkod() + " server id: "+ sorszamok.getServerId());
                        sorszamok.save();
                    }
                    Log.i(LOGTAG, "A sorszámokat letöltöttem");
                }

                @Override
                public void failure(RetrofitError error) {
                    Log.i(LOGTAG, "A sorszámok letöltése sikertelen" + error.getMessage());
                }
            });
        }
        // Ide inkább kilépés kell. Vagy még egy gomb, hogy tovább a kitöltésre.
        Intent mainIntent = new Intent(this, NyomtMainActivity.class);
        startActivity(mainIntent);
    }

}
