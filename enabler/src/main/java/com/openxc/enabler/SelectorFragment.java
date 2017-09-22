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
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.codebutler.android_websockets.WebSocketClient;
import com.openxc.VehicleManager;
import com.openxc.messages.EventedSimpleVehicleMessage;
import com.openxc.messages.SimpleVehicleMessage;
import com.openxc.messages.VehicleMessage;
import com.openxc.util.Utils;
import com.openxc.util.WsConfig;
import com.openxcplatform.enabler.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static com.openxc.enabler.DiagnosticRequestFragment.hexArray;

public class SelectorFragment extends Fragment {
    private static String TAG = "SelectorFragment",TAG_EVENT_LISTENER = "SelectorFragmentListener";

    private VehicleManager mVehicleManager;
    private String valueOfEvent;
    private TextView mEngineSpeedTextView,eventNameTextView;
    private Button engineButton;
    private static final String STOP = "Stop Engine Speed Meter";
    private static final String START = "Start Engine Speed Meter";

    private Utils utils;
    private WebSocketClient client;
    String val,name,vinID,deviceID;
    boolean alertRequest,isSendingValue;
    String token;
    private static final String TAG_DEVICE_ID = "deviceId",TAG_IP_ADDRESS= "ipAddress";
    private static final String TAG_VIN = "vin";
    private static final String TAG_IP = "ip";
    private static final String TAG_SESSION_ID = "sessionId";
    private static final String TAG_REQUESTID = "requestID";
    private static final String TAG_USER_TOKEN = "userToken";
    private static final String TAG_ACCESS_TOKEN = "accessToken";
    private String eventName="engine_speed";
    Map<String, String> eventMap = new HashMap<String, String>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.selector_fragment, container, false);


        mEngineSpeedTextView = (TextView) v.findViewById(R.id.engine_speed_value_text_view_selector);
        eventNameTextView = (TextView) v.findViewById(R.id.event_name_textview);


        engineButton = (Button) v.findViewById(R.id.check_engine_speed_btn);
        utils = new Utils(getActivity().getApplicationContext());
        name="enabler";
        SharedPreferences pref = getActivity().getSharedPreferences(TAG_IP_ADDRESS,Context.MODE_PRIVATE);
                        vinID = pref.getString(TAG_VIN,"");
                        deviceID = pref.getString(TAG_DEVICE_ID,"");
                        token = pref.getString(TAG_USER_TOKEN,"");

       if (client==null){callConnection();}


        //engineButton.setText("Start Engine Speed Meter");

        engineButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        if (engineButton.getText().equals("Start Engine Speed Meter")) {
//                                Log.i(TAG_EVENT_LISTENER,printMap(eventMap,eventName));

                                String selectedValue =printMap(eventMap,"engine_speed");
                            Log.i("acceptanceRegisteration",eventName+":"+selectedValue);
                            client.send(utils.getSendValueJSON(selectedValue,eventName,token));
                        }


                    }
                });
        return v;
    }


    private VehicleMessage.Listener mListener = new VehicleMessage.Listener() {
        @Override
        public void receive(final VehicleMessage message) {
            Activity activity = getActivity();
            if (activity != null) {
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        if (message instanceof EventedSimpleVehicleMessage) {
                            SimpleVehicleMessage convertedMsg = new SimpleVehicleMessage(message.getTimestamp(),
                                    ((EventedSimpleVehicleMessage) message).getName(),
                                    ((EventedSimpleVehicleMessage) message).getValue() +
                                            ": " + ((EventedSimpleVehicleMessage) message).getEvent());
                            String name = ((EventedSimpleVehicleMessage) message).getName();
                            String value = ((EventedSimpleVehicleMessage) message).getValue().toString();
                            eventMap.put(name, value);
                            if (name.equals(eventName)) {
                                valueOfEvent = value;

                            }


                        } else {
                            String value = message.asSimpleMessage().getValue().toString();
                            String name = message.asSimpleMessage().getName();
                            eventMap.put(name, value);
                            if (message.asSimpleMessage().getName().equals(eventName)) {
                                valueOfEvent = value;
                                  Log.i(TAG_EVENT_LISTENER, name + value );


                            }
                        }
                    }
                });
            }
        }
    };


    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            Log.i(TAG, "Bound to VehicleManager");
            mVehicleManager = ((VehicleManager.VehicleBinder) service
            ).getService();

                  mVehicleManager.addListener(SimpleVehicleMessage.class, mListener);
                  mVehicleManager.addListener(EventedSimpleVehicleMessage.class, mListener);
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.w(TAG, "VehicleService disconnected unexpectedly");
            mVehicleManager = null;
        }
    };

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (getActivity() == null) {
            return;
        }

        if (isVisibleToUser) {
            getActivity().bindService(
                    new Intent(getActivity(), VehicleManager.class),
                    mConnection, Context.BIND_AUTO_CREATE);
        } else {
            if (mVehicleManager != null) {
                Log.i(TAG, "Unbinding from vehicle service");
                mVehicleManager.removeListener(SimpleVehicleMessage.class, mListener);
                mVehicleManager.removeListener(EventedSimpleVehicleMessage.class, mListener);
                getActivity().unbindService(mConnection);
                mVehicleManager = null;
            }
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


            }else if (flag.equalsIgnoreCase("acceptanceRegisteration")) {
              String eventNameJson = jObj.getString("evenName");
                eventName = eventNameJson;


                String selectedValue =printMap(eventMap,eventName);
                Log.i("acceptanceRegisteration",eventName+":"+selectedValue);

                client.send(utils.getSendValueJSON(selectedValue,eventName,token));
                isSendingValue = true;



            }else if (flag.equalsIgnoreCase("message")) {
                String securityToken = jObj.getString("securityToken");
                String pid = jObj.getString("pid");
                String sessionId = jObj.getString("sessionId");
                String ip = jObj.getString("id");

                SharedPreferences pref = getActivity().getSharedPreferences("getValue",Context.MODE_PRIVATE);
                val = pref.getString("value","5");
                client.send(utils.getSendMessageJSON(val,name));


            }

        } catch (JSONException e) {
            e.printStackTrace();
        }


    }

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }


    private void callConnection() {

        SharedPreferences shar = getActivity().getSharedPreferences("ipAddress",Context.MODE_PRIVATE);
        String strrr=shar.getString("ip","");

        client = new WebSocketClient(URI.create("ws://"+strrr+":"+WsConfig.pid+ "/ProjectECU/com?name=" + name), new WebSocketClient.Listener(){



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
                                    Log.i("WebSocket","Accepted from server");
                                }
                            });
                        }
                    }.start();
                    alertRequest=false;
                }
                if (isSendingValue){
                    new Thread()
                    {
                        public void run()
                        {
                            getActivity().runOnUiThread(new Runnable()
                            {
                                public void run()
                                {
                                    eventNameTextView.setText(eventName);
                                    mEngineSpeedTextView.setText(valueOfEvent);
                                    Toast.makeText(getActivity(), eventName+":"+valueOfEvent, Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }.start();
                    isSendingValue=false;

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

                    }
                }).show();

    }
    public static String printMap(Map mp,String eventName) {
        Iterator it = mp.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            System.out.println(pair.getKey() + " = " + pair.getValue());
            if (pair.getKey().equals(eventName)){
                return pair.getValue().toString();
            }
            it.remove(); // avoids a ConcurrentModificationException

        }
        return "Event Not Found";
    }
}
