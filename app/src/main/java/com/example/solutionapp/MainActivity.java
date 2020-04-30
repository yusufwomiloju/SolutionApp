package com.example.solutionapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    TextView textView;
    Button button;

    ConnectivityManager cm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button = findViewById(R.id.button);

        button.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               // Check network connection status
               boolean isConnected = connected();
               String path = "https://capi.stage.9c9media.com/destinations/tsn_ios/platforms/iPad/contents/69585";
               if (isConnected) {
                   // make a network call in the background
                   new DownloadJsonTask().execute(path);

                   // TODO The AsyncTask class should be static or leaks might occcur
                   //new DownloadJsonTask((MainActivity) getApplicationContext()).execute(path);
               } else {
                   Toast.makeText(getApplicationContext(), "Network unavailable", Toast.LENGTH_SHORT).show();
               }
           }
        });
    }

    // Check network connection status
    private boolean connected() {
        cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        boolean isConnected = activeNetwork != null && activeNetwork.isConnected();
        return isConnected;
    }

    // Send HTTPS Get request in the background
    private class DownloadJsonTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {
            return downloadJson(urls[0]);
        }

        @Override
        protected void onPostExecute(String json) {
            // Parse JSON
            String dateUnformmated = getLastModifiedDate(json);

            // Display the date time
            String lastModifiedDate = formatDate(dateUnformmated);

            Toast.makeText(getApplicationContext(), lastModifiedDate, Toast.LENGTH_SHORT).show();

        }
    }

    /**
     * Makes an HTTP Get request using the passed path, returns the JSON response.
     *
     * @param path The url of the resource to download
     * @return the JSON response
     */
    private static String downloadJson(String path) {
        final String TAG = "Download Json";

        String json = null;


        try {
            URL url = new URL(path);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);  // Timeout at 5 seconds
            conn.setReadTimeout(2500);     // Timeout at 2.5 seconds
            conn.setRequestMethod("GET");
            conn.setDoInput(true);

            // Perform network operation
            conn.connect();

            json = new BufferedReader(new InputStreamReader(conn.getInputStream()))
                    .lines().collect(Collectors.joining("\n"));
            System.out.println("Json: " + json);
        } catch (MalformedURLException e) {
            Log.e(TAG, "URL error : " + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, "Download failed : " + e.getMessage());
        }

        return json;
    }

    /**
     *  Parses the passed JSON and returns the lastModifiedDateTime value. In Production code,
     *  I would use a Json library (the one used by other Bell Media developers).
     */
    private static String getLastModifiedDate(String json) {
        // "LastModifiedDateTime":"2020-02-10T15:49:05Z"
        String pattern = "LastModifiedDateTime\":\"(.+?)\"";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(json);

        String lastModifiedDate = "";
        if (m.find()) {
            lastModifiedDate = m.group(1);
        }

        System.out.println("Found value: " + lastModifiedDate);
        return lastModifiedDate;
    }

    // Converts the date in ISO 8601 format to RFC 1123 format (i.e. 'Tue, 3 Jun 2008 11:05:30 GMT')
    private static String formatDate(String lastModifiedDateTime) {
        if (lastModifiedDateTime.equals("")) { return ""; }

        Instant timestamp = Instant.parse(lastModifiedDateTime);

        LocalDateTime ldt = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault());
        StringBuilder builder = (new StringBuilder(ldt.getMonth().toString())).append(" ")
                .append(ldt.getDayOfMonth()).append(" ")
                .append(ldt.getYear()).append(" ")
                .append(ldt.getHour()).append(":").append(ldt.getMinute());

        return builder.toString();
    }
}
