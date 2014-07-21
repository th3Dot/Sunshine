package cz.markalsoft.pc.sunshine;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new ForecastFragment())
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(MainActivity.this,SettingsActivity.class);
            startActivity(intent);
        }
        if (id == R.id.action_map) {
            //do nothing ForecastFragment will send intent to GoogleMapActivity
            //doing it here also would start GoogleMapActivity two times
        }
        if (id == R.id.action_map_show) {
            //start implicit intent to show user's default location on google map
            String location;
            location = PreferenceManager.getDefaultSharedPreferences(this)
                    .getString(getString(R.string.pref_location_key),getString(R.string.pref_location_default));
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = Uri.parse("geo:0.0?").buildUpon().appendQueryParameter("q",location).build();
            intent.setData(uri);
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            }
        }
        return super.onOptionsItemSelected(item);
    }

}
