package com.devostrum.docloud;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
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

import com.devostrum.docloud.models.GroupModel;
import com.devostrum.docloud.objects.Group;

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
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_docloud);

        m = (MainApplication) getApplicationContext();

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putInt("completeupdate", 0).commit();

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
        for(int i = 0; i < groups.length; i++) {
            final Group currentGroup = groups[i];
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        URL url = new URL(m.serverURL + "/grouppics/" + m.uid + ".png");
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

    }

    private void checkForSynchronizeData() {
        setProgressBarIndeterminateVisibility(true);

        new Thread(new Runnable() {
            @Override
            public void run() {
                if(!prefs.contains("tablechecksum")) {
                    performSync();
                }
            }
        }).start();
    }

    private void performSync() {

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
