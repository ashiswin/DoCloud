package com.devostrum.docloud;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import fr.tkeunebr.gravatar.Gravatar;


public class ProfileUpdateActivity extends ActionBarActivity {
    EditText edtTagline;
    ImageView imgProfilePic;
    TextView txtNumberOfChars;
    ProgressBar prgImageLoad;

    MainApplication m;

    int freeChars = 20;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_update);

        m = (MainApplication) getApplicationContext();

        edtTagline = (EditText) findViewById(R.id.edtTagline);
        imgProfilePic = (ImageView) findViewById(R.id.imgProfilePic);
        txtNumberOfChars = (TextView) findViewById(R.id.txtNumberOfChars);
        prgImageLoad = (ProgressBar) findViewById(R.id.prgImageLoad);

        String gravatarURL = Gravatar.init().with(m.email).defaultImage(Gravatar.DefaultImage.IDENTICON).size(Gravatar.MAX_IMAGE_SIZE_PIXEL).build();

        loadProfilePic(gravatarURL);

        txtNumberOfChars.setText(getResources().getQuantityString(R.plurals.characters, freeChars, freeChars));

        edtTagline.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                String text = charSequence.toString();
                freeChars = 20 - text.length();
                txtNumberOfChars.setText(getResources().getQuantityString(R.plurals.characters, freeChars, freeChars));
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("incompleteupdate", 1);
        editor.commit();
    }
    public void loadProfilePic(final String gravatarURL) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(gravatarURL);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    connection.connect();
                    InputStream input = connection.getInputStream();
                    final Bitmap myBitmap = BitmapFactory.decodeStream(input);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            prgImageLoad.setVisibility(View.GONE);
                            imgProfilePic.setImageBitmap(myBitmap);
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.profile_update, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_done) {
            String tagline = edtTagline.getText().toString();

            submitTagline(tagline);

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void submitTagline(final String tagline) {
        final ProgressDialog updateDialog = ProgressDialog.show(this, "Updating...", "Updating your profile", true, false);
        new Thread(new Runnable() {
            @Override
            public void run() {
                JSONArray jsonData = new JSONArray();
                JSONObject updateObject = new JSONObject();

                try {
                    updateObject.put("command", "inserttagline");
                    updateObject.put("uid", m.uid);
                    updateObject.put("tagline", tagline);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                jsonData.put(updateObject);

                JSONArray response = ServerConnector.sendJSONCommand(getBaseContext(), m.serverURL, jsonData);

                updateDialog.cancel();
                try {
                    JSONObject updateResponse = response.getJSONObject(0);

                    if(!updateResponse.getBoolean("success")) {
                        final String message = updateResponse.getString("message");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getBaseContext(), message, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getBaseContext(), "Successfully updated your profile", Toast.LENGTH_SHORT).show();
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
}
