package com.devostrum.docloud;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import fr.tkeunebr.gravatar.Gravatar;

public class RegistrationActivity extends ActionBarActivity implements View.OnFocusChangeListener {
    EditText edtName, edtEmail, edtPassword, edtRetypePassword;
    ImageView imgGravatar, imgEmailStatus, imgPasswordStatus;
    TextView txtEmailStatus, txtPasswordStatus;
    ProgressBar prgGravatar, prgEmailStatus, prgPasswordStatus;
    MainApplication m;

    GoogleCloudMessaging gcm;
    String registrationId;

    private static final String PROPERTY_APP_VERSION = "appVersion";
    private static final String PROPERTY_REG_ID = "registrationId";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Get handle to application object
        m = (MainApplication)getApplicationContext();

        // Get handles to UI elements
        edtName = (EditText)findViewById(R.id.edtName);
        edtEmail = (EditText)findViewById(R.id.edtEmail);
        edtPassword = (EditText)findViewById(R.id.edtPassword);
        edtRetypePassword = (EditText)findViewById(R.id.edtRetypePassword);

        imgGravatar = (ImageView) findViewById(R.id.imgGravatar);
        imgEmailStatus = (ImageView) findViewById(R.id.imgEmailStatus);
        imgPasswordStatus = (ImageView) findViewById(R.id.imgPasswordStatus);

        txtEmailStatus = (TextView) findViewById(R.id.txtEmailStatus);
        txtPasswordStatus = (TextView) findViewById(R.id.txtPasswordStatus);

        prgGravatar = (ProgressBar) findViewById(R.id.prgGravatar);
        prgEmailStatus = (ProgressBar) findViewById(R.id.prgEmailStatus);
        prgPasswordStatus = (ProgressBar) findViewById(R.id.prgPasswordStatus);

        edtPassword.addTextChangedListener(new PasswordTextWatcher());
        edtRetypePassword.addTextChangedListener(new PasswordTextWatcher());

        edtEmail.setOnFocusChangeListener(this);

        // Set email and password values from previous login page
        Intent registrationIntent = getIntent();
        edtEmail.setText(registrationIntent.getStringExtra("LOGIN_EMAIL"));
        edtPassword.setText(registrationIntent.getStringExtra("LOGIN_PASSWORD"));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.registration, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_done) {
            final ProgressDialog registrationDialog = ProgressDialog.show(this, getString(R.string.RegistrationDialogTitle), getString(R.string.RegistrationDialogText), true, false);

            if(!checkPassword()) {
                registrationDialog.cancel();
            }
            
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if(!checkEmail()) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                registrationDialog.cancel();
                            }
                        });
                        return;
                    }
                    if(edtName.getText().toString().isEmpty()) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                registrationDialog.cancel();
                                Toast.makeText(getBaseContext(), "Please enter your name", Toast.LENGTH_SHORT).show();
                            }
                        });
                        return;
                    }
                    Log.d("Registration Activity", "Checks passed");

                    final JSONArray jsonData = new JSONArray();
                    JSONObject registerObject = new JSONObject();

                    String password = edtPassword.getText().toString().trim();
                    String passwordConfirm = edtRetypePassword.getText().toString().trim();

                    gcm = GoogleCloudMessaging.getInstance(getBaseContext());
                    registrationId = getRegistrationId(getBaseContext());
                    Log.d("Registration Activity", "GCM initialized");
                    if(registrationId.isEmpty()) {
                        try {
                            registrationId = gcm.register(m.SENDER_ID);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        storeRegistrationId(getBaseContext(), registrationId);
                    }
                    try {
                        registerObject.put("command", "register");
                        registerObject.put("name", edtName.getText().toString());
                        registerObject.put("email", edtEmail.getText().toString());
                        registerObject.put("hashedPassword", Utils.hashPassword(password));
                        registerObject.put("hashedPasswordConfirm", Utils.hashPassword(passwordConfirm));
                        registerObject.put("gcmid", registrationId);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    Log.d("Registration Activity", "Data prepped");
                    jsonData.put(registerObject);

                    JSONArray jsonResponse = ServerConnector.sendJSONCommand(getBaseContext(), m.serverURL, jsonData);
                    Log.d("Registration Activity", "Data received");
                    try {
                        final JSONObject registerResponse = jsonResponse.getJSONObject(0);

                        Log.d("Registration Activity", "Response: " + registerResponse.toString());
                        registrationDialog.cancel();
                        if(!registerResponse.getBoolean("success")) {
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
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public boolean checkPassword() {
        imgPasswordStatus.setVisibility(View.INVISIBLE);
        txtPasswordStatus.setVisibility(View.INVISIBLE);
        prgPasswordStatus.setVisibility(View.VISIBLE);

        if(edtPassword.getText().toString().isEmpty()) {
            imgPasswordStatus.setVisibility(View.VISIBLE);
            txtPasswordStatus.setVisibility(View.VISIBLE);
            prgPasswordStatus.setVisibility(View.GONE);

            //imgPasswordStatus.setImageDrawable(R.drawable.cross);
            txtPasswordStatus.setText(R.string.EnterPassword);
            txtPasswordStatus.setTextColor(Color.RED);

            return false;
        }
        else if(!edtPassword.getText().toString().equals(edtRetypePassword.getText().toString())) {
            imgPasswordStatus.setVisibility(View.VISIBLE);
            txtPasswordStatus.setVisibility(View.VISIBLE);
            prgPasswordStatus.setVisibility(View.GONE);

            //imgPasswordStatus.setImageDrawable(R.drawable.cross);
            txtPasswordStatus.setText(R.string.PasswordsNoMatch);
            txtPasswordStatus.setTextColor(Color.RED);

            return false;
        }
        else {
            imgPasswordStatus.setVisibility(View.VISIBLE);
            txtPasswordStatus.setVisibility(View.VISIBLE);
            prgPasswordStatus.setVisibility(View.GONE);

            //imgPasswordStatus.setImageDrawable(R.drawable.tick);
            txtPasswordStatus.setText(R.string.PasswordsMatch);
            txtPasswordStatus.setTextColor(Color.GREEN);

            return true;
        }
    }

    public boolean checkEmail() {
        // Perform emptiness check
        if(edtEmail.getText().toString().isEmpty()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    imgEmailStatus.setVisibility(View.VISIBLE);
                    txtEmailStatus.setVisibility(View.VISIBLE);
                    imgGravatar.setVisibility(View.VISIBLE);
                    prgEmailStatus.setVisibility(View.GONE);

                    //imgEmailStatus.setImageDrawable(R.drawable.cross);
                    txtEmailStatus.setText(R.string.EnterEmail);
                    txtEmailStatus.setTextColor(Color.RED);
                }
            });

            return false;
        }

        // Perform validity check
        Pattern emailPattern;
        Matcher emailMatcher;
        emailPattern = Pattern.compile("^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$");
        emailMatcher = emailPattern.matcher(edtEmail.getText().toString());
        if(!emailMatcher.matches()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    imgEmailStatus.setVisibility(View.VISIBLE);
                    txtEmailStatus.setVisibility(View.VISIBLE);
                    imgGravatar.setVisibility(View.VISIBLE);
                    prgEmailStatus.setVisibility(View.GONE);

                    //imgEmailStatus.setImageDrawable(R.drawable.cross);
                    txtEmailStatus.setText(R.string.InvalidEmail);
                    txtEmailStatus.setTextColor(Color.RED);
                }
            });

            return false;
        }

        // Perform availability check
        final JSONArray jsonData = new JSONArray();
        JSONObject availabilityObject = new JSONObject();

        try {
            availabilityObject.put("command", "emailAvailability");
            availabilityObject.put("email", edtEmail.getText().toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        jsonData.put(availabilityObject);

        JSONArray jsonResponse = ServerConnector.sendJSONCommand(getBaseContext(), m.serverURL, jsonData);
        try {
            JSONObject availabilityResponse = jsonResponse.getJSONObject(0);

            if(!availabilityResponse.getBoolean("success")) {
                final String message = availabilityResponse.getString("message");

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imgEmailStatus.setVisibility(View.VISIBLE);
                        txtEmailStatus.setVisibility(View.VISIBLE);
                        prgEmailStatus.setVisibility(View.GONE);
                        imgGravatar.setVisibility(View.VISIBLE);

                        //imgEmailStatus.setImageDrawable(R.drawable.cross);
                        txtEmailStatus.setText(message);
                        txtEmailStatus.setTextColor(Color.RED);
                    }
                });

                return false;
            }
            else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imgEmailStatus.setVisibility(View.VISIBLE);
                        txtEmailStatus.setVisibility(View.VISIBLE);
                        prgEmailStatus.setVisibility(View.GONE);

                        //imgEmailStatus.setImageDrawable(R.drawable.tick);
                        txtEmailStatus.setText(R.string.ValidEmail);
                        txtEmailStatus.setTextColor(Color.GREEN);

                        prgGravatar.setVisibility(View.VISIBLE);
                        imgGravatar.setVisibility(View.INVISIBLE);
                    }
                });
                try {
                    String gravatarURL = Gravatar.init().with(edtEmail.getText().toString()).defaultImage(Gravatar.DefaultImage.IDENTICON).size(Gravatar.MAX_IMAGE_SIZE_PIXEL).build();
                    URL url = new URL(gravatarURL);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    connection.connect();
                    InputStream input = connection.getInputStream();
                    final Bitmap myBitmap = BitmapFactory.decodeStream(input);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            prgGravatar.setVisibility(View.GONE);
                            imgGravatar.setVisibility(View.VISIBLE);
                            imgGravatar.setImageBitmap(myBitmap);
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return true;
    }

    public void onClick(View v) {
        /*if(v == btnRegister) {

            final ProgressDialog registrationDialog = ProgressDialog.show(this, getString(R.string.RegistrationDialogTitle), getString(R.string.RegistrationDialogText), true, false);

            new Thread(new Runnable() {
                public void run() {


                    jsonData.put(registerObject);

                    JSONArray jsonResponse = ServerConnector.sendJSONCommand(getBaseContext(), m.serverURL, jsonData);

                    try {
                        final JSONObject registerResponse = jsonResponse.getJSONObject(0);

                        registrationDialog.cancel();
                        if(!registerResponse.getBoolean("success")) {
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
        }*/
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
        editor.apply();
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        if(view == edtEmail && !hasFocus) {
            imgEmailStatus.setVisibility(View.INVISIBLE);
            txtEmailStatus.setVisibility(View.INVISIBLE);
            prgEmailStatus.setVisibility(View.VISIBLE);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    checkEmail();
                }
            }).start();
        }
    }

    class PasswordTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            checkPassword();
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    }
}
