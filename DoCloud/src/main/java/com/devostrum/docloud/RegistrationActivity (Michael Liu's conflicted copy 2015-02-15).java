package com.devostrum.docloud;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.gcm.GoogleCloudMessaging;

public class RegistrationActivity extends ActionBarActivity implements OnClickListener {
    EditText edtName, edtEmail, edtPassword, edtRetype;
    Button btnRegister;
    MainApplication m;

    GoogleCloudMessaging gcm;
    String registrationId;

    private static final String PROPERTY_APP_VERSION = "appVersion";
    private static final String PROPERTY_REG_ID = "registrationId";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        // Get handle to application object
        m = (MainApplication)getApplicationContext();

        // Get handles to UI elements
        edtName = (EditText)findViewById(R.id.edtName);
        edtEmail = (EditText)findViewById(R.id.edtEmail);
        edtPassword = (EditText)findViewById(R.id.edtPassword);
        edtRetype = (EditText)findViewById(R.id.edtConfirmPassword);

        btnRegister = (Button)findViewById(R.id.btnRegistrationLetMeInNow);

        // Set up button listener
        btnRegister.setOnClickListener(this);

        // Set email and password values from previous login page
        Intent registrationIntent = getIntent();
        edtEmail.setText(registrationIntent.getStringExtra("LOGIN_EMAIL"));
        edtPassword.setText(registrationIntent.getStringExtra("LOGIN_PASSWORD"));
    }

    public void onClick(View v) {
        if(v == btnRegister) {
            if(edtName.getText().toString().trim().length() == 0 || edtName.getText() == null) {
                Toast.makeText(this, getString(R.string.EnterName), Toast.LENGTH_LONG).show();
                return;
            }

            // Check if email entered is valid
            Pattern emailPattern;
            Matcher emailMatcher;
            emailPattern = Pattern.compile("^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$");
            emailMatcher = emailPattern.matcher(edtEmail.getText().toString());
            if(!emailMatcher.matches()) {
                Toast.makeText(this, getString(R.string.InvalidEmail), Toast.LENGTH_LONG).show();
                return;
            }

            if(edtEmail.getText().toString().trim().length() == 0) {
                Toast.makeText(this, getString(R.string.EnterEmail), Toast.LENGTH_LONG).show();
                return;
            }

            if(edtPassword.getText().toString().trim().length() == 0) {
                Toast.makeText(this, getString(R.string.EnterPassword), Toast.LENGTH_LONG).show();
                return;
            }

            if(edtRetype.getText().toString().trim().length() == 0) {
                Toast.makeText(this, getString(R.string.ConfirmPassword), Toast.LENGTH_LONG).show();
                return;
            }

            if(!edtPassword.getText().toString().equals(edtRetype.getText().toString())) {
                Toast.makeText(this, getString(R.string.NonMatchingPasswords), Toast.LENGTH_LONG).show();
                return;
            }

            final ProgressDialog registrationDialog = ProgressDialog.show(this, getString(R.string.RegistrationDialogTitle), getString(R.string.RegistrationDialogText), true, false);

            new Thread(new Runnable() {
                public void run() {
                    final JSONArray jsonData = new JSONArray();
                    JSONObject registerObject = new JSONObject();

                    try {
                        String password = edtPassword.getText().toString().trim();
                        String passwordConfirm = edtRetype.getText().toString().trim();

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

                        md.update(passwordConfirm.getBytes());
                        mb = md.digest();
                        String hashedPasswordConfirm = "";
                        for (int i = 0; i < mb.length; i++) {
                            byte temp = mb[i];
                            String s = Integer.toHexString(new Byte(temp));
                            while (s.length() < 2) {
                                s = "0" + s;
                            }
                            s = s.substring(s.length() - 2);
                            hashedPasswordConfirm += s;
                        }

                        gcm = GoogleCloudMessaging.getInstance(getBaseContext());
                        registrationId = getRegistrationId(getBaseContext());

                        if(registrationId.isEmpty()) {
                            try {
                                registrationId = gcm.register(m.SENDER_ID);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            storeRegistrationId(getBaseContext(), registrationId);
                        }

                        registerObject.put("command", "register");
                        registerObject.put("name", edtName.getText().toString());
                        registerObject.put("email", edtEmail.getText().toString());
                        registerObject.put("hashedPassword", hashedPassword);
                        registerObject.put("hashedPasswordConfirm", hashedPasswordConfirm);
                        registerObject.put("gcmid", registrationId);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    }

                    jsonData.put(registerObject);
                    Log.d("RegistrationActivity", jsonData.toString());

                    JSONArray jsonResponse = ServerConnector.sendJSONCommand(getBaseContext(), m.serverURL, jsonData);

                    try {
                        final JSONObject registerResponse = jsonResponse.getJSONObject(0);

                        registrationDialog.cancel();
                        if(registerResponse.getBoolean("success") == false) {
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    try {
                                        Toast.makeText(getBaseContext(), registerResponse.getString("message"), Toast.LENGTH_LONG).show();
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }
                        else {
                            m.email = edtEmail.getText().toString();
                            try {
                                m.uid = registerResponse.getString("uid");
                                m.token = registerResponse.getString("longToken");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            runOnUiThread(new Runnable() {
                                public void run() {
                                    Toast.makeText(getBaseContext(), getString(R.string.RegistrationSuccessToast), Toast.LENGTH_LONG).show();

                                    DatabaseHelper dbHelper = new DatabaseHelper(getBaseContext());
                                    dbHelper.createUserTables(dbHelper.getWritableDatabase(), m.uid);
                                    dbHelper.close();

                                    setResult(RESULT_OK);
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
}
