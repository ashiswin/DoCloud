package com.devostrum.docloud;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class LoginActivity extends ActionBarActivity implements View.OnClickListener {
    MainApplication m;

    EditText edtEmail, edtPassword;
    Button btnLogin, btnRegister;

    public static int REGISTRATION_INTENT = 0;
    public static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    private static final String PROPERTY_APP_VERSION = "appVersion";
    private static final String PROPERTY_REG_ID = "registrationId";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        m = (MainApplication) getApplicationContext();

        edtEmail = (EditText) findViewById(R.id.txtLoginEmail);
        edtPassword = (EditText) findViewById(R.id.txtLoginPassword);

        btnLogin = (Button) findViewById(R.id.btnLoginLetMeIn);
        btnRegister = (Button) findViewById(R.id.btnLoginRegister);

        btnLogin.setOnClickListener(this);
        btnRegister.setOnClickListener(this);

        if(!checkPlayService()) {
            Toast.makeText(this, "Invalid Google Play Services APK detected", Toast.LENGTH_LONG).show();
            finish();
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if(prefs.contains("persistentlogin")) {
            m.uid = prefs.getString("uid", "");
            m.email = prefs.getString("email", "");
            m.token = prefs.getString("token", "");

            if(prefs.contains("incompleteupdate") && !prefs.contains("completeupdate")) {
                Intent updateIntent = new Intent(this, ProfileUpdateActivity.class);
                startActivity(updateIntent);

                finish();
            }
            else {
                Intent homeIntent = new Intent(this, DoCloudActivity.class);
                startActivity(homeIntent);

                finish();
            }
        }
    }

    public boolean checkPlayService() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if(resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i("LoginActivity", "This device is not supported.");
                finish();
            }
            return false;
        }

        return true;
    }

    @Override
    public void onClick(View view) {
        if(view == btnRegister) {
            Intent registrationIntent = new Intent(this, RegistrationActivity.class);
            registrationIntent.putExtra("LOGIN_EMAIL", edtEmail.getText().toString());
            registrationIntent.putExtra("LOGIN_PASSWORD", edtPassword.getText().toString());

            startActivityForResult(registrationIntent, REGISTRATION_INTENT);
        }
        else if(view == btnLogin) {
            login();
        }
    }

    private void login() {
        final ProgressDialog loginDialog = ProgressDialog.show(this, getString(R.string.LoginDialogTitle), getString(R.string.LoginDialogText), true, false);

        new Thread(new Runnable() {
            @Override
            public void run() {
                String email = edtEmail.getText().toString();
                String password = edtPassword.getText().toString();

                JSONArray jsonData = new JSONArray();
                JSONObject loginObject = new JSONObject();

                try {
                    loginObject.put("command", "authenticate");
                    loginObject.put("email", email);

                    MessageDigest md = MessageDigest.getInstance("SHA-512");
                    md.update(password.getBytes());
                    byte[] mb = md.digest();
                    String hashedPassword = "";
                    for (int i = 0; i < mb.length; i++) {
                        byte temp = mb[i];
                        String s = Integer.toHexString(new Byte(temp));
                        while (s.length() < 2) {
                            s = "0" + s;
                        }
                        s = s.substring(s.length() - 2);
                        hashedPassword += s;
                    }

                    loginObject.put("hashedPassword", hashedPassword);

                    GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(getBaseContext());
                    String registrationId = getRegistrationId(getBaseContext());

                    if(registrationId.isEmpty()) {
                        try {
                            registrationId = gcm.register(m.SENDER_ID);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        storeRegistrationId(getBaseContext(), registrationId);
                    }

                    loginObject.put("gcmid", registrationId);
                } catch(JSONException e) {
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }

                jsonData.put(loginObject);

                JSONArray response = ServerConnector.sendJSONCommand(getBaseContext(), m.serverURL, jsonData);
                loginDialog.cancel();
                try {
                    JSONObject loginResponse = response.getJSONObject(0);

                    if(!loginResponse.getBoolean("success")) {
                        final String message = loginResponse.getString("message");

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getBaseContext(), message, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    else {
                        m.uid = loginResponse.getString("uid");
                        m.token = loginResponse.getString("token");
                        m.email = edtEmail.getText().toString();

                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putInt("persistentlogin", 1);
                        editor.putString("uid", m.uid);
                        editor.putString("token", m.token);
                        editor.putString("email", m.email);
                        editor.commit();

                        DatabaseHelper databaseHelper = new DatabaseHelper(getBaseContext());
                        databaseHelper.createUserTables(databaseHelper.getWritableDatabase(), m.uid);
                        databaseHelper.close();

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getBaseContext(), "Successfully logged in", Toast.LENGTH_SHORT).show();

                                Intent homeIntent = new Intent(getBaseContext(), DoCloudActivity.class);
                                startActivity(homeIntent);

                                finish();
                            }
                        });
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private String getRegistrationId(Context context) {
        final SharedPreferences prefs = getSharedPreferences(getClass().getSimpleName(), Context.MODE_PRIVATE);

        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            return "";
        }

        return registrationId;
    }

    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    private void storeRegistrationId(Context context, String registrationId) {
        final SharedPreferences prefs = getSharedPreferences(getClass().getSimpleName(), Context.MODE_PRIVATE);
        int appVersion = getAppVersion(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, registrationId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.commit();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REGISTRATION_INTENT && resultCode == RESULT_OK) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("persistentlogin", 1);
            editor.putString("uid", m.uid);
            editor.putString("email", m.email);
            editor.putString("token", m.token);
            editor.commit();

            Intent profileIntent = new Intent(this, ProfileUpdateActivity.class);
            startActivity(profileIntent);

            finish();
        }
    }
}