package com.devostrum.docloud;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class ServerConnector {
    public static JSONArray sendJSONCommand(Context context, String urlPart, JSONArray commandData) {
        String url = urlPart + "/scripts/Main.php";

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();

        if(activeNetworkInfo == null || !activeNetworkInfo.isConnected()) {
            JSONObject object = new JSONObject();
            JSONArray response = new JSONArray();

            try {
                object.put("success", false);
                object.put("message", "You are not connected to the internet");
                response.put(object);

                return response;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        ArrayList<NameValuePair> jsonData = new ArrayList<NameValuePair>();
        jsonData.add(new BasicNameValuePair("json", commandData.toString()));
        InputStream inputStream = null;
        String result = null;
        JSONArray jsonResult = null;
        try {
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(url);
            httpPost.setEntity(new UrlEncodedFormEntity(jsonData));
            HttpResponse response = httpClient.execute(httpPost);
            HttpEntity entity = response.getEntity();
            inputStream = entity.getContent();
        }
        catch(Exception e) {
            Log.e("ServerConnection", "Error in http connection " + e.toString());
            JSONObject object = new JSONObject();
            JSONArray response = new JSONArray();

            try {
                object.put("success", false);
                object.put("message", "You are not connected to the internet");
                response.put(object);

                return response;
            } catch (JSONException e1) {
                e1.printStackTrace();
            }
        }

        try{
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream,"iso-8859-1"));
            StringBuilder sb = new StringBuilder();
            String line = null;

            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }

            inputStream.close();

            result = sb.toString();
        }
        catch(Exception e){
            Log.e("ServerConnection", "Error converting result " + e.toString());
            JSONObject object = new JSONObject();
            JSONArray response = new JSONArray();

            try {
                object.put("success", false);
                object.put("message", "You are not connected to the internet");
                response.put(object);

                return response;
            } catch (JSONException e1) {
                e1.printStackTrace();
            }
        }

        try{
            jsonResult = new JSONArray(result);
        }
        catch(JSONException e) {
            Log.e("ServerConnection", "Error parsing data " + result);
            JSONObject object = new JSONObject();
            JSONArray response = new JSONArray();

            try {
                object.put("success", false);
                object.put("message", "You are not connected to the internet");
                response.put(object);

                return response;
            } catch (JSONException e1) {
                e1.printStackTrace();
            }
        }

        return jsonResult;
    }
}
