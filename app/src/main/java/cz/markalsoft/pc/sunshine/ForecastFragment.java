package cz.markalsoft.pc.sunshine;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by PC on 16.7.2014.
 */

public class ForecastFragment extends Fragment {
    private ArrayAdapter<String> mForecastAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //this fragment handles menu events
        setHasOptionsMenu(true);
        //call helper method to get new weather data using city id
        updateWeather();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            updateWeather();
            return true;
        }
        if (id == R.id.action_map) {
            Intent intent = new Intent(getActivity(),GoogleMapActivity.class);
            startActivityForResult(intent, 1);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateWeather() {

        FetchWeatherTask weatherTask = new FetchWeatherTask();
        weatherTask.execute(getActivity().getPreferences(Context.MODE_PRIVATE)
                .getString(getString(R.string.pref_location_key), getString(R.string.pref_location_default)));

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {
                Log.v(this.getTag(), String.valueOf(requestCode));
                FetchWeatherTask weatherTask = new FetchWeatherTask();
                double latitude = data.getDoubleExtra("latitude", 0);
                double longitude = data.getDoubleExtra("longitude", 0);
                Log.v(this.getTag(), String.valueOf(latitude) + " " + String.valueOf(longitude));
                weatherTask.execute(String.valueOf(latitude), String.valueOf(longitude));

            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        //ArrayAdapter for forecast list view, it process raw data and
        //creates list item layouts based on that data which then it sends to
        //ListView listViewForecast

        //initialization of ArrayAdapter
        mForecastAdapter = new ArrayAdapter<String>(
                //get context which is parent class which we can provide with getActivity()
                getActivity(),
                //layout for list item for listView
                R.layout.list_item_forecast,
                //id of list item
                R.id.list_item_forecast_textview,
                //raw data to display in ListView, no data to display on start
                //sending empty arraylist
                new ArrayList<String>());
        //finding reference to ListView using findViewById, try to call to the closest
        //container in hierarchy in order to reduce the search space, in this example it is
        //root view, this is the view of this Fragment, where listView is defined
        ListView listViewForecast = (ListView) rootView.findViewById(R.id.listview_forecast);
        //set adapter for listViewForecast, this connects listView with adapter
        listViewForecast.setAdapter(mForecastAdapter);
        listViewForecast.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String weather = mForecastAdapter.getItem(i);

                Intent intent = new Intent(getActivity(),DetailActivity.class)
                        .putExtra(Intent.EXTRA_TEXT, weather);
                startActivity(intent);
            }
        });

        return rootView;
    }

    public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        /* The date/time conversion code is going to be moved outside the asynctask later,
         * so for convenience we're breaking it out into its own method now.
        */
        private String getReadableDateString(long time) {
            // Because the API returns a unix timestamp (measured in seconds),
            // it must be converted to milliseconds in order to be converted to valid date.
            Date date = new Date(time * 1000);
            SimpleDateFormat format = new SimpleDateFormat("E, MMM d");
            return format.format(date).toString();
        }

        /**
         * Prepare the weather high/lows for presentation.
         */
        private String formatHighLows(double high, double low) {
            // For presentation, assume the user doesn't care about tenths of a degree.
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String units = preferences.getString(getString(R.string.pref_units_key),
                            getString(R.string.pref_units_default));

            if (units.equals("imperial")) {
                high = (high*9)/5 + 32;
                low = (low*9)/5 + 32;
            }
            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);

            String highLowStr;
            highLowStr = roundedHigh + "/" + roundedLow;

            return highLowStr;
        }

        /**
         * Take the String representing the complete forecast in JSON Format and
         * pull out the data we need to construct the Strings needed for the wireframes.
         * <p/>
         * Fortunately parsing is easy:  constructor takes the JSON string and converts it
         * into an Object hierarchy for us.
         */
        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_TEMPERATURE = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";
            final String OWM_DATETIME = "dt";
            final String OWM_DESCRIPTION = "main";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            String[] resultStrs = new String[numDays];
            for (int i = 0; i < weatherArray.length(); i++) {
                // For now, using the format "Day, description, hi/low"
                String day;
                String description;
                String highAndLow;

                // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);

                // The date/time is returned as a long.  We need to convert that
                // into something human-readable, since most people won't read "1400356800" as
                // "this saturday".
                long dateTime = dayForecast.getLong(OWM_DATETIME);
                day = getReadableDateString(dateTime);

                // description is in a child array called "weather", which is 1 element long.
                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);

                // Temperatures are in a child object called "temp".  Try not to name variables
                // "temp" when working with temperature.  It confuses everybody.
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                double high = temperatureObject.getDouble(OWM_MAX);
                double low = temperatureObject.getDouble(OWM_MIN);

                highAndLow = formatHighLows(high, low);
                resultStrs[i] = day + " - " + description + " - " + highAndLow;
            }


            return resultStrs;
        }

        @Override
        protected String[] doInBackground(String... params) {
            if (params.length == 0) {
                return null;
            }



            //-------OPEN WEATHER MAP URL QUERY BLOCK--------
            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;

            int numDays = 7;

            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are avaiable at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast
                Uri builtUri = null;
                if (params.length == 1) {
                    //Creates url based on postcode user defined
                    builtUri = Uri.parse("http://api.openweathermap.org/data/2.5/forecast/daily?").buildUpon()
                            .appendQueryParameter("id", params[0])
                            .appendQueryParameter("mode", "json")
                            .appendQueryParameter("units", "metric")
                            .appendQueryParameter("cnt", "7")
                            .build();
                } else if (params.length == 2) {
                    //Creates url based on postcode user defined
                    builtUri = Uri.parse("http://api.openweathermap.org/data/2.5/forecast/daily?").buildUpon()
                            .appendQueryParameter("lat", params[0])
                            .appendQueryParameter("lon",params[1])
                            .appendQueryParameter("mode", "json")
                            .appendQueryParameter("units", "metric")
                            .appendQueryParameter("cnt", "7")
                            .build();
                }
                if (builtUri == null)
                    return null;

                URL url = new URL(builtUri.toString());
                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    forecastJsonStr = null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    forecastJsonStr = null;
                }
                forecastJsonStr = buffer.toString();

            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
                forecastJsonStr = null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }
            //-------END OF OPEN WEATHER MAP URL QUERY BLOCK--------
            try {
                return getWeatherDataFromJson(forecastJsonStr, numDays);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String[] result) {
            if (result != null) {
                mForecastAdapter.clear();
                for (String dayForecast : result) {
                    mForecastAdapter.add(dayForecast);
                }
                // Data from server is updated.
            }
        }
    }
}

