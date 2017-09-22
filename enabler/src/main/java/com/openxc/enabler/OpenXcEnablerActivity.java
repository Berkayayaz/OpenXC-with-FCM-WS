package com.openxc.enabler;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.bugsnag.android.Bugsnag;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;
import com.openxc.VehicleManager;
import com.openxc.enabler.preferences.PreferenceManagerService;
import com.openxcplatform.enabler.BuildConfig;
import com.openxcplatform.enabler.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.HttpsURLConnection;

/** The OpenXC Enabler app is primarily for convenience, but it also increases
 * the reliability of OpenXC by handling background tasks on behalf of client
 * applications.
 *
 * The Enabler provides a common location to control which data sources and
 * sinks are active, e.g. if the a trace file should be played back or recorded.
 * It's preferable to be able to change the data source on the fly, and not have
 * to programmatically load a trace file in any application under test.
 *
 * With the Enabler installed, the {@link com.openxc.remote.VehicleService} is
 * also started automatically when the Android device boots up. A simple data
 * sink like a trace file uploader can start immediately without any user
 * interaction.
 *
 * As a developer, you can also appreciate that because the Enabler takes care
 * of starting the {@link com.openxc.remote.VehicleService}, you don't need to
 * add much to your application's AndroidManifest.xml - just the
 * {@link com.openxc.VehicleManager} service.
*/
public class OpenXcEnablerActivity extends FragmentActivity {
    private static String TAG = "OpenXcEnablerActivity";
    private AlertDialog dialog;
    private String ipString, deviceID;
    private EnablerFragmentAdapter mAdapter;
    private ViewPager mPager;
    private EditText txtIP, txtName;
    private Button btnSend;
    private ProgressDialog pDialog;
    private static final String TAG_SUCCESS = "success";
    private static final String TAG_DEVICE_ID = "deviceId";
    private static final String TAG_VIN = "vin";
    private static final String TAG_IP = "ip";
    private static final String TAG_SESSION_ID = "sessionId",TAG_IP_ADDRESS= "ipAddress";
    private String android_id;
    private VehicleManager mVehicleManager;
    private String valueEngineSpeed;
    private static final String TAG_USER_TOKEN = "userToken";
    private static final String TAG_ACCESS_TOKEN = "accessToken";
    private static final String FCM_PROJECT_SENDER_ID = "432412342123";
    private static final String FCM_SERVER_CONNECTION = "@gcm.googleapis.com";

    private String DOMAIN = "http://10.0.3.2:8081/api/v1/vecu/mobile/register";

    public String eventName="engine_speed";
    String ipAddress;
    public NotificationID c;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_button, null);
        txtIP = (EditText) view.findViewById(R.id.ipAddress);
        txtName = (EditText) view.findViewById(R.id.device_id_edit_text);
        android_id = Settings.Secure.getString(getApplicationContext().getContentResolver(),
                Settings.Secure.ANDROID_ID);
//        Intent intent_o = getIntent();
//         eventName = intent_o.getStringExtra("event");
//        Log.i(TAG,"TEST EVENT NAME:"+eventName);
            c = new NotificationID();
        btnSend = (Button) view.findViewById(R.id.button);
        WifiManager wifiMgr = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
        int ip = wifiInfo.getIpAddress();
         ipAddress = Formatter.formatIpAddress(ip);
       // txtIP.setText(ipAddress);
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!txtIP.getText().toString().isEmpty() && !txtName.getText().toString().isEmpty()) {

                    String token = FirebaseInstanceId.getInstance().getToken();
                    Log.d(TAG, "Firebase Token: " + token);
                    //Toast.makeText(OpenXcEnablerActivity.this, token, Toast.LENGTH_SHORT).show();
                    sendMessageFCM("");

                    SharedPreferences pref = getSharedPreferences(TAG_IP_ADDRESS, Context.MODE_PRIVATE);

                    SharedPreferences.Editor editor = pref.edit();
                    editor.putString(TAG_IP, txtIP.getText().toString());
                    editor.putString(TAG_DEVICE_ID, txtName.getText().toString());
                    editor.putString(TAG_USER_TOKEN, token);
                    editor.commit();

                    ipString = txtIP.getText().toString();
                    deviceID = txtName.getText().toString();
                    new HandShake().execute(DOMAIN, token, deviceID, ipString);

                    dialog.dismiss();

                }

            }
        });

        if (getIntent().getExtras() != null){
            for (String key: getIntent().getExtras().keySet()){
                if (key.equals("event")){
                    eventName = getIntent().getExtras().getString(key);
                    Log.d(TAG, "Event Name: " + eventName);


                }
            }

        }
        builder.setView(view);
        dialog = builder.create();
        dialog.show();

        String bugsnagToken = BuildConfig.BUGSNAG_TOKEN;
        if (bugsnagToken != null && !bugsnagToken.isEmpty()) {
            try {
                Bugsnag.init(this, bugsnagToken);
            } catch (NoClassDefFoundError e) {
                Log.w(TAG, "Busgnag is unsupported when building from Eclipse", e);
            }
        } else {
            Log.i(TAG, "No Bugsnag token found in AndroidManifest, not enabling Bugsnag");
        }

        Log.i(TAG, "OpenXC Enabler created");
        setContentView(R.layout.main);
        mAdapter = new EnablerFragmentAdapter(getSupportFragmentManager());
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);

        if (savedInstanceState != null) {
            mPager.setCurrentItem(savedInstanceState.getInt("tab", 0));
        }


        startService(new Intent(this, VehicleManager.class));
        startService(new Intent(this, PreferenceManagerService.class));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("tab", mPager.getCurrentItem());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }



    public static class EnablerFragmentAdapter extends FragmentPagerAdapter {
        private static final String[] mTitles = {"Status", "Dashboard",
                "Selector", "Diagnostic", "Send CAN", "CAN"};

        public EnablerFragmentAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return mTitles.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mTitles[position];
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 1) {
                return new VehicleDashboardFragment();
            } else if (position == 2) {
                return new SelectorFragment();
            } else if (position == 3) {
                return new DiagnosticRequestFragment();
            } else if (position == 4) {
                return new SendCanMessageFragment();
            } else if (position == 5) {
                return new CanMessageViewFragment();
            }

            // For position 0 or anything unrecognized, go to Status
            return new StatusFragment();
        }
    }

    static String getBugsnagToken(Context context) {
        String key = null;
        try {
            Context appContext = context.getApplicationContext();
            ApplicationInfo appInfo = appContext.getPackageManager().getApplicationInfo(
                    appContext.getPackageName(), PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                key = appInfo.metaData.getString("com.bugsnag.token");
            }
        } catch (NameNotFoundException e) {
            // Should not happen since the name was determined dynamically from the app context.
            Log.e(TAG, "Unexpected NameNotFound.", e);
        }
        return key;
    }


    class HandShake extends AsyncTask<String, String, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(OpenXcEnablerActivity.this);
            pDialog.setMessage("Connecting Server. Please wait...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(true);
            pDialog.show();
        }

        @Override
        protected String doInBackground(String... params) {


            HashMap<String, String> map = new HashMap<String, String>();
//            map.put(TAG_USER_TOKEN,params[1]);
//            map.put(TAG_DEVICE_ID,params[2]);
//            map.put(TAG_IP,params[3]);
            map.put(TAG_USER_TOKEN, TAG_USER_TOKEN);
            map.put(TAG_DEVICE_ID, random());
            String vinID = random();
            SharedPreferences pref = getSharedPreferences("ipAddress", Context.MODE_PRIVATE);

            SharedPreferences.Editor editor = pref.edit();
            editor.putString(TAG_VIN, vinID);
            editor.commit();
            map.put(TAG_IP, ipString);
            map.put(TAG_VIN, vinID);
            map.put("placeHolder1", "placeHolder1");
            map.put("placeHolder2", "placeHolder2");


            String result = performPostCall(params[0], map);
            try {
                JSONObject jsonObject = new JSONObject(result);

                String accessToken = jsonObject.getString(TAG_ACCESS_TOKEN);
                String sessionID = jsonObject.getString(TAG_SESSION_ID);

                return accessToken;

            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }


        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            pDialog.dismiss();
            if (s != null) {

                Log.i(TAG, "Access Token from Clement's server: " + s);
            }
        }
    }

    private String getPostDataString(HashMap<String, String> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }

        return result.toString();
    }

    public String performPostCall(String requestURL,
                                  HashMap<String, String> postDataParams) {

        URL url;
        String response = "";
        try {
            url = new URL(requestURL);
            DataOutputStream printout;
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(15000);
            conn.setConnectTimeout(15000);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            //conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.connect();


            JSONObject jsonObject = new JSONObject(postDataParams);
            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));
            //writer.write(getPostDataString(postDataParams));

            writer.write(jsonObject.toString());
            writer.flush();
            writer.close();
            os.close();
            int responseCode = conn.getResponseCode();

            if (responseCode == HttpsURLConnection.HTTP_CREATED) {
                String line;
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line = br.readLine()) != null) {
                    response += line;
                }
            } else if (responseCode == HttpURLConnection.HTTP_CONFLICT) {
                Log.w(TAG, "HTTP CONFLICT. DEVICE ID IS ALREADY REGISTERED");
                response = "DEVICE ID IS ALREADY REGISTERED";

            } else {
                response = "";

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return response;
    }
    public static String random() {
        Random generator = new Random();
        StringBuilder randomStringBuilder = new StringBuilder();
        int randomLength = generator.nextInt(6);
        char tempChar;
        for (int i = 0; i < randomLength; i++){
            tempChar = (char) (generator.nextInt(96) + 32);
            randomStringBuilder.append(tempChar);
        }
        return randomStringBuilder.toString();
    }

    public void sendMessageFCM(String message){
        FirebaseMessaging fm = FirebaseMessaging.getInstance();
        fm.send(new RemoteMessage.Builder(FCM_PROJECT_SENDER_ID + "@gcm.googleapis.com")
                .setMessageId(Integer.toString(c.getID()))
                .addData("my_message", "Hello World")
                .addData("my_action","SAY_HELLO")
                .build());
    }
    public int randomNumber(){
        Random r = new Random();
        int i1 = r.nextInt(80 - 65) + 65;
        return i1;
    }
    public class NotificationID {
        private final  AtomicInteger c = new AtomicInteger(0);
        public  int getID() {
            return c.incrementAndGet();
        }
    }
}

