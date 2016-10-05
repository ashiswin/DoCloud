package com.devostrum.docloud;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.devostrum.docloud.models.GroupModel;
import com.devostrum.docloud.objects.Group;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class DoCloudActivity extends ActionBarActivity {
    ListView lstGroupsList;
    SharedPreferences prefs;
    GroupModel groupModel;
    Group[] groups;
    GroupListAdapter adapter;

    MainApplication m;

    public static final int NEW_INTENT = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_docloud);

        m = (MainApplication) getApplicationContext();

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putInt("completeupdate", 0).apply();

        lstGroupsList = (ListView) findViewById(R.id.lstGroupsList);

        groupModel = new GroupModel(this, m.uid);
        groupModel.open();
        Cursor groupCursor = groupModel.fetchAllGroups();

        prepareGroups(groupCursor);

        groupCursor.close();

        checkForSynchronizeData();
        downloadGroupPics();
        downloadGroupNumbers();

        adapter = new GroupListAdapter();
        lstGroupsList.setAdapter(adapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        groupModel.close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.do_cloud, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.mntHomeNew) {
            Intent newIntent = new Intent(this, NewGroupActivity.class);
            startActivityForResult(newIntent, NEW_INTENT);

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void prepareGroups(Cursor groupCursor) {
        groups = new Group[groupCursor.getCount()];

        for(int i = 0; i < groupCursor.getCount(); i++) {
            groupCursor.moveToPosition(i);

            groups[i].setGroupId(groupCursor.getString(groupCursor.getColumnIndex(GroupModel.KEY_GROUPID)));
            groups[i].setGroupName(groupCursor.getString(groupCursor.getColumnIndex(GroupModel.KEY_GROUPNAME)));
        }
    }

    private void downloadGroupPics() {
        for(final Group currentGroup : groups) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        URL url = new URL(m.serverURL + "/grouppics/" + currentGroup.getGroupId() + ".png");
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setDoInput(true);
                        connection.connect();
                        InputStream input = connection.getInputStream();
                        currentGroup.setGroupPic(BitmapFactory.decodeStream(input));
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                        currentGroup.setGroupPic(BitmapFactory.decodeResource(getResources(), R.drawable.sampleprofile));
                    } catch (IOException e) {
                        e.printStackTrace();
                        currentGroup.setGroupPic(BitmapFactory.decodeResource(getResources(), R.drawable.sampleprofile));
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter.notifyDataSetChanged();
                        }
                    });
                }
            }).start();
        }
    }

    private void downloadGroupNumbers() {
        JSONArray groupIdJSON = new JSONArray();
        for(Group group : groups) {
            groupIdJSON.put(group.getGroupId());
        }
        JSONObject sendObject = new JSONObject();
        try {
            sendObject.put("command", "groupNumbers");
            sendObject.put("data", groupIdJSON);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        final JSONArray numberData = new JSONArray();
        numberData.put(sendObject);

        new Thread(new Runnable() {
            @Override
            public void run() {
                JSONArray numberResponse = ServerConnector.sendJSONCommand(getBaseContext(), m.serverURL, numberData);
                try {
                    final JSONObject numberObject = numberResponse.getJSONObject(0);
                    if(numberObject.getBoolean("success")) {
                        JSONArray numberArray = numberObject.getJSONArray("numbers");

                        for (int i = 0; i < groups.length; i++) {
                            groups[i].setGroupId(numberArray.getString(i));
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                adapter.notifyDataSetChanged();
                            }
                        });
                    }
                    else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Toast.makeText(getBaseContext(), numberObject.getString("message"), Toast.LENGTH_SHORT).show();
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void checkForSynchronizeData() {
        setSupportProgressBarIndeterminateVisibility(true);

        new Thread(new Runnable() {
            @Override
            public void run() {
                if(!prefs.contains("lastChange")) {
                    performRedownload();
                }
                else {
                    performSync();
                }
            }
        }).start();
    }

    private void performSync() {
        JSONArray jsonData = new JSONArray();
        JSONObject checkObject = new JSONObject();

        try {
            checkObject.put("command", "getLastChange");
            checkObject.put("uid", m.uid);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        jsonData.put(checkObject);

        JSONArray responseData = ServerConnector.sendJSONCommand(getBaseContext(), m.serverURL, jsonData);
        try {
            final JSONObject checkResponse = responseData.getJSONObject(0);
            if(!checkResponse.getBoolean("success")) {
                Toast.makeText(getBaseContext(), checkResponse.getString("message"), Toast.LENGTH_SHORT).show();
            }
            else if(prefs.getInt("lastChange", 0) < checkResponse.getInt("firstChange")) {
                performRedownload();
            }
            else if(prefs.getInt("lastChange", 0) != checkResponse.getInt("lastChange")) {
                JSONArray jsonData2 = new JSONArray();
                JSONObject syncObject = new JSONObject();

                syncObject.put("command", "getChanges");
                syncObject.put("uid", m.uid);

                jsonData2.put(syncObject);

                JSONArray responseData2 = ServerConnector.sendJSONCommand(getBaseContext(), m.serverURL, jsonData2);
                JSONObject syncResponse = responseData2.getJSONObject(0);

                if(!syncResponse.getBoolean("success")) {
                    Toast.makeText(getBaseContext(), syncResponse.getString("message"), Toast.LENGTH_SHORT).show();
                }
                else {
                    JSONArray changeArray = syncResponse.getJSONArray("changeArray");
                    DatabaseHelper databaseHelper = new DatabaseHelper(getBaseContext());
                    SQLiteDatabase database = databaseHelper.getWritableDatabase();

                    for(int i = 0; i < changeArray.length(); i++) {
                        database.execSQL(changeArray.getJSONObject(i).getString("sql"));
                    }

                    database.close();

                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putInt("lastChange", changeArray.getJSONObject(changeArray.length() - 1).getInt("id"));
                    editor.apply();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter.notifyDataSetChanged();

                            setSupportProgressBarIndeterminateVisibility(false);
                        }
                    });
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void performRedownload() {
        JSONArray jsonData = new JSONArray();
        JSONObject groupObject = new JSONObject();
        JSONObject taskObject = new JSONObject();

        try {
            groupObject.put("command", "getAllGroups");
            groupObject.put("uid", m.uid);

            taskObject.put("command", "getAllTodos");
            taskObject.put("uid", m.uid);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        jsonData.put(groupObject);
        jsonData.put(taskObject);

        JSONArray jsonResponse = ServerConnector.sendJSONCommand(getBaseContext(), m.serverURL, jsonData);

        try {
            JSONObject groupResponse = jsonResponse.getJSONObject(0);
            JSONObject taskResponse = jsonResponse.getJSONObject(1);

            if(!groupResponse.getBoolean("success")) {
                Toast.makeText(getBaseContext(), groupResponse.getString("message"), Toast.LENGTH_SHORT).show();
            }
            else {
                JSONArray groups = groupResponse.getJSONArray("groups");

                for(int i = 0; i < groups.length(); i++) {
                    groupModel.addGroup(groups.getJSONObject(i).getString("groupid"), groups.getJSONObject(i).getString("groupname"));
                }
            }

            if(!taskResponse.getBoolean("success")) {
                Toast.makeText(getBaseContext(), taskResponse.getString("message"), Toast.LENGTH_SHORT).show();
            }
            else {
                JSONArray tasks = taskResponse.getJSONArray("todos");

                // TODO: Create TaskModel and add all tasks in
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter.notifyDataSetChanged();

                    setSupportProgressBarIndeterminateVisibility(false);
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    private class GroupListAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return groups.length;
        }

        @Override
        public Object getItem(int position) {
            return groups[position];
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Group group = (Group) getItem(position);
            View itemView;

            if(convertView == null) {
                LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                itemView = inflater.inflate(R.layout.listrow_groups, parent, false);
            }
            else {
                itemView = convertView;
            }

            ((TextView) itemView.findViewById(R.id.txtGroupName)).setText(group.getGroupName());
            if(group.getGroupPic() != null) {
                (itemView.findViewById(R.id.prgGroupPicLoad)).setVisibility(View.GONE);
                ((ImageView) itemView.findViewById(R.id.imgGroupPic)).setImageBitmap(group.getGroupPic());
            }

            if(group.getNumberOfItems() >= 0) {
                (itemView.findViewById(R.id.prgGroupNumberLoad)).setVisibility(View.GONE);
                ((TextView) itemView.findViewById(R.id.txtGroupItems)).setText(group.getNumberOfItems() + "");
            }
            return itemView;
        }
    }
}
