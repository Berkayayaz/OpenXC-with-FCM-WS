package com.openxc.enabler;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.codebutler.android_websockets.WebSocketClient;
import com.google.firebase.iid.FirebaseInstanceId;
import com.openxc.VehicleManager;
import com.openxc.messages.DiagnosticRequest;
import com.openxc.messages.DiagnosticResponse;
import com.openxc.messages.VehicleMessage;
import com.openxc.util.Utils;
import com.openxc.util.WsConfig;
import com.openxcplatform.enabler.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;

public class DiagnosticRequestFragment extends ListFragment {
    private static String TAG = "DiagnosticRequestFragment";
    private WebSocketClient client;
    private Utils utils;
    private String val;
    private TextView txtIP;
    private  String name;
    private VehicleManager mVehicleManager;
    private DiagnosticResponseAdapter mAdapter;
    private View mLastRequestView;
    String token;
    boolean alertRequest;
    private VehicleMessage.Listener mListener = new VehicleMessage.Listener() {
        @Override
        public void receive(final VehicleMessage message) {
            Activity activity = getActivity();
            if(activity != null) {
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        mAdapter.add(message.asDiagnosticResponse());
                    }
                });
            }
        }
    };

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            Log.i(TAG, "Bound to VehicleManager");
            mVehicleManager = ((VehicleManager.VehicleBinder)service
                    ).getService();

            mVehicleManager.addListener(DiagnosticResponse.class, mListener);
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.w(TAG, "VehicleService disconnected unexpectedly");
            mVehicleManager = null;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAdapter = new DiagnosticResponseAdapter(getActivity());

        name="enabler";
         token = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Firebase Token: " + token);

        utils = new Utils(getActivity().getApplicationContext());

        //if (client==null){callConnection();}




    }

    private void callConnection() {

        SharedPreferences shar = getActivity().getSharedPreferences("ipAddress",Context.MODE_PRIVATE);
        String strrr=shar.getString("ip","");

        client = new WebSocketClient(URI.create("ws://"+strrr+":"+WsConfig.pid+ "/ProjectECU/com?name=" + name), new WebSocketClient.Listener(){

//            client = new WebSocketClient(URI.create("ws://"+strrr+":"+WsConfig.pid+"/JavaNetworking/com?name="
//                + URLEncoder.encode("enabler")), new WebSocketClient.Listener(){


            @Override
            public void onConnect() {

            }

            @Override
            public void onMessage(String message) {
                parseMessage(message);
                if (alertRequest){
                    new Thread()
                    {
                        public void run()
                        {
                            getActivity().runOnUiThread(new Runnable()
                            {
                                public void run()
                                {
                                alertView("Do you accept request from server?");
                                    //Do your UI operations like dialog opening or Toast here
                                }
                            });
                        }
                    }.start();
                   // Toast.makeText(getActivity(), "ALERT POP UP", Toast.LENGTH_LONG).show();
                    Log.i("WebSocket","Accepted from server");
                }
            }

            @Override
            public void onMessage(byte[] data) {
                parseMessage(bytesToHex(data));
            }

            @Override
            public void onDisconnect(int code, String reason) {
                utils.storeSessionId(null);
            }

            @Override
            public void onError(Exception error) {
                Log.e(TAG, "Error! : " + error);


            }
        },null);
        client.connect();


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.diagnostic_request_fragment,
                container, false);

        final Spinner spinner = (Spinner) v.findViewById(R.id.bus_spinner);
        // Create an ArrayAdapter using the string array and a default spinner
        // layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                getActivity(), R.array.buses_array
                , android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);

        Button btn = (Button) v.findViewById(R.id.send_request);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View buttonView) {
                onSendDiagnosticRequest(spinner,
                        (EditText) v.findViewById(R.id.diag_request_id),
                        (EditText) v.findViewById(R.id.diag_request_mode),
                        (EditText) v.findViewById(R.id.diag_request_pid));

            }
        });

        mLastRequestView = v.findViewById(R.id.last_request);
        return v;
    }
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private void onSendDiagnosticRequest(Spinner busSpinner,
            EditText idView, EditText modeView, EditText pidView) {
        boolean validInput = true;
        if(idView.getText().toString().isEmpty()) {
            idView.setError("ID is required");
            validInput = false;
        }

        if(modeView.getText().toString().isEmpty()) {
            modeView.setError("Mode is required");
            validInput = false;
        }

        if(validInput) {
            DiagnosticRequest request = new DiagnosticRequest(
                    Integer.valueOf(busSpinner.getSelectedItem().toString()),
                    Integer.valueOf(idView.getText().toString(), 16),
                    Integer.valueOf(modeView.getText().toString(), 16));
            // Make sure to update after sending so the timestamp is set by the
            // VehicleManager
            String pidString = pidView.getText().toString();
            if(!pidString.isEmpty()) {
                request.setPid(Integer.valueOf(pidString, 16));
            }
            mVehicleManager.send(request);
            // Make sure to update after sending so the timestamp is set by the
            // VehicleManager

            updateLastRequestView(request);
        } else {
            Log.i(TAG, "Form is invalid, not sending diagnostic request");
        }
    }

    private void parseMessage(final String msg) {
        System.out.println("WS Message: "+msg);
        Log.i("WebSocket","Parsing Message From WS:"+msg);
        try {
            JSONObject jObj = new JSONObject(msg);
            String flag = jObj.getString("flag");

            if (flag.equalsIgnoreCase("self")) {

                String sessionId = jObj.getString("sessionId");


                utils.storeSessionId(sessionId);
                client.send(utils.getSendRegisterationJSON(val,name,token));

                Log.e(TAG, "Your session id: " + utils.getSessionId());

            }else if (flag.equalsIgnoreCase("responseRequestRegistration")) {
                String securityToken = jObj.getString("securityToken");
                String pid = jObj.getString("pid");
                String sessionId = jObj.getString("sessionId");
                String ip = jObj.getString("id");
                String validation = jObj.getString("validation");
                if (validation.equalsIgnoreCase("accept")){

                   this.alertRequest = true;
                }


            }else if (flag.equalsIgnoreCase("message")) {
            String securityToken = jObj.getString("securityToken");
            String pid = jObj.getString("pid");
            String sessionId = jObj.getString("sessionId");
            String ip = jObj.getString("id");

            //        if(id.contains(WsConfig.local)) {
            SharedPreferences pref = getActivity().getSharedPreferences("getValue",Context.MODE_PRIVATE);
            val = pref.getString("value","5");
            client.send(utils.getSendMessageJSON(val,name));

            //      }

        }




        } catch (JSONException e) {
            e.printStackTrace();
        }


    }






    private void updateLastRequestView(final DiagnosticRequest request) {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                // TODO This is duplicated in DiagnosticResponseAdapter - figure
                // out the best way to share this rendering info
                TextView timestampView = (TextView)
                        mLastRequestView.findViewById(R.id.timestamp);
                timestampView.setText(VehicleMessageAdapter.formatTimestamp(
                            request));

                TextView busView = (TextView)
                        mLastRequestView.findViewById(R.id.bus);
                busView.setText("" + request.getBusId());

                TextView idView = (TextView)
                        mLastRequestView.findViewById(R.id.id);
                idView.setText("0x" + Integer.toHexString(request.getId()));

                TextView modeView = (TextView)
                        mLastRequestView.findViewById(R.id.mode);
                modeView.setText("0x" + Integer.toHexString(request.getMode()));

                if(request.hasPid()) {
                    TextView pidView = (TextView)
                            mLastRequestView.findViewById(R.id.pid);
                    pidView.setText("0x" + Integer.toHexString(
                                request.getPid()));
                }
            }
        });
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setListAdapter(mAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().bindService(
                new Intent(getActivity(), VehicleManager.class),
                mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onPause() {
        super.onPause();
        if(mVehicleManager != null) {
            Log.i(TAG, "Unbinding from vehicle service");
            mVehicleManager.removeListener(DiagnosticResponse.class, mListener);
            getActivity().unbindService(mConnection);
            mVehicleManager = null;
        }
    }
    private void alertView( String message ) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());

        dialog.setTitle( "Server Request" )
                .setIcon(R.drawable.open_xc_launcher_icon_black)
                .setMessage(message)
  .setNegativeButton("No", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialoginterface, int i) {
          dialoginterface.cancel();
          }})
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialoginterface, int i) {


                        client.send(utils.getSendAcceptRequestJSON("This is a validation message",name,token));
//                        SharedPreferences pref = getActivity().getSharedPreferences("getValue",Context.MODE_PRIVATE);
//                        val = pref.getString("value","");
//                        client.send(utils.getSendMessageJSON(val,name));
                    }
                }).show();

    }
}
