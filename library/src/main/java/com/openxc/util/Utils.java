package com.openxc.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class Utils {

	private Context context;
	private SharedPreferences sharedPref;

	private static final String KEY_SHARED_PREF = "Java_Mob";
	private static final int KEY_MODE_PRIVATE = 0;
	private static final String KEY_SESSION_ID = "sessionId",
			FLAG_MESSAGE = "message";
	private static final String TAG = "Utils";



	public Utils(Context context) {
		this.context = context;
		sharedPref = this.context.getSharedPreferences(KEY_SHARED_PREF,
				KEY_MODE_PRIVATE);
	}

	public void storeSessionId(String sessionId) {
		Editor editor = sharedPref.edit();
		editor.putString(KEY_SESSION_ID, sessionId);
		editor.commit();
	}

	public String getSessionId() {
		return sharedPref.getString(KEY_SESSION_ID, null);
	}

	public String getSendMessageJSON(String message,String name) {
		String json = null;


		try {
			JSONObject jObj = new JSONObject();
			jObj.put("flag", FLAG_MESSAGE);
			jObj.put("sessionId", getSessionId());
			jObj.put("message", message);
			jObj.put("name","device");
			jObj.put("pid",WsConfig.pid);
			jObj.put("multipleResponses","false");
			jObj.put("decodeType","OBD2");
			jObj.put("success","true");
			jObj.put("frequency","0");
			jObj.put("securityToken","");
			jObj.put("userID",name);
			jObj.put("timeStamp",(System.currentTimeMillis()/1000));
			jObj.put("requestID","");
			jObj.put("ecuProtocol","");
			jObj.put("type","CAN");
			jObj.put("latitude","");

			jObj.put("longitude","");



			json = jObj.toString();
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return json;
	}
	public String getSendRegisterationJSON(String message,String name,String securtiyToken) {
		String json = null;

		try {
			JSONObject jObj = new JSONObject();
			jObj.put("flag", "requestRegistration");
			jObj.put("sessionId", getSessionId());
			jObj.put("idVin", "VIN ID TEST BERKAY");
			jObj.put("idDevice","device ID TEST BERKAY");
			//jObj.put("pid",WsConfig.pid);
			jObj.put("latitude","21.11");
			jObj.put("altitude","31.11");
			jObj.put("securityToken", securtiyToken);




			json = jObj.toString();
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return json;
	}
	public String getSendAcceptRequestJSON(String message,String name,String securtiyToken) {
		String json = null;

		try {
			JSONObject jObj = new JSONObject();
			jObj.put("flag", "acceptanceRegisteration");
			jObj.put("sessionId", getSessionId());
			jObj.put("validation", "accept");
			jObj.put("timeStamp",(System.currentTimeMillis()/1000));
			jObj.put("pid",WsConfig.pid);
			jObj.put("id",WsConfig.local);
			jObj.put("securityToken", "Pm23abce4f354dH");

			json = jObj.toString();
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return json;
	}
	public String getSendValueJSON(String value,String eventName,String securtiyToken) {
		String json = null;

		try {
			JSONObject jObj = new JSONObject();
			jObj.put("flag", "resultValue");
			jObj.put("sessionId", getSessionId());
			jObj.put("value", value);
			jObj.put("evenName", eventName);
			jObj.put("timeStamp",(System.currentTimeMillis()/1000));
			jObj.put("pid",WsConfig.pid);
			jObj.put("id",WsConfig.local);
			jObj.put("securityToken", "Pm23abce4f354dH");

			json = jObj.toString();
			Log.i("SendJSON",json);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return json;
	}
}
