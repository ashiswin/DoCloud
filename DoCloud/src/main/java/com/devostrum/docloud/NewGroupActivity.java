package com.devostrum.docloud;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.devostrum.docloud.objects.Person;

import java.util.ArrayList;

public class NewGroupActivity extends ActionBarActivity implements View.OnClickListener, AdapterView.OnItemClickListener {
    ImageView imgGroupPic;
    EditText edtGroupName;
    TextView txtGroupName, txtGroupCreator;
    ListView lstGroupMembers;

    public static final int IMAGE_INTENT = 0;

    Bitmap groupPic;
    ArrayList<Person> persons;
    MemberListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_info);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        imgGroupPic = (ImageView) findViewById(R.id.imgGroupPic);
        edtGroupName = (EditText) findViewById(R.id.edtGroupName);
        txtGroupName = (TextView) findViewById(R.id.txtGroupName);
        txtGroupCreator = (TextView) findViewById(R.id.txtGroupCreator);
        lstGroupMembers = (ListView) findViewById(R.id.lstGroupMembers);

        txtGroupName.setVisibility(View.INVISIBLE);
        edtGroupName.setHint("Group name");
        imgGroupPic.setImageResource(R.drawable.sampleprofile);

        imgGroupPic.setOnClickListener(this);

        adapter = new MemberListAdapter();
        lstGroupMembers.setAdapter(adapter);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.new_group, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_done) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        if(view == imgGroupPic) {
            Intent imagePickerIntent = new Intent(Intent.ACTION_PICK);
            imagePickerIntent.setType("image/*");
            startActivityForResult(imagePickerIntent, IMAGE_INTENT);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == IMAGE_INTENT) {
            Uri imageURI = data.getData();
            if(imageURI != null) {
                String[] filePathColumn = {MediaStore.Images.Media.DATA};
                Cursor cursor = getContentResolver().query(imageURI, filePathColumn, null, null, null);
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String filePath = cursor.getString(columnIndex);
                cursor.close();

                groupPic = BitmapFactory.decodeFile(filePath);
                groupPic = Bitmap.createScaledBitmap(groupPic, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 104, getResources().getDisplayMetrics()), (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 104, getResources().getDisplayMetrics()), false);

                imgGroupPic.setImageBitmap(groupPic);
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

    }

    private class MemberListAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            if(persons == null) {
                return 1;
            }
            return persons.size() + 1;
        }

        @Override
        public Object getItem(int i) {
            if(persons == null) {
                return null;
            }
            return persons.get(i);
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View convertView, ViewGroup parent) {
            View itemView;

            if(convertView == null) {
                LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                itemView = inflater.inflate(R.layout.listrow_members, parent, false);
            }
            else {
                itemView = convertView;
            }

            if(persons == null || i == persons.size()) {
                LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                itemView = inflater.inflate(R.layout.listrow_addmember, parent, false);
                ((ImageView) itemView.findViewById(R.id.imgProfilePic)).setImageResource(R.drawable.new_item);
                ((TextView) itemView.findViewById(R.id.txtName)).setText("Add members");
            }
            else {
                ((TextView) itemView.findViewById(R.id.txtName)).setText(persons.get(i).getName());
                if(!persons.get(i).getTagline().isEmpty()) {
                    ((TextView) itemView.findViewById(R.id.txtTagline)).setText(persons.get(i).getTagline());
                }
                if(persons.get(i).getProfilePic() != null) {
                    ((ImageView) itemView.findViewById(R.id.imgProfilePic)).setImageBitmap(persons.get(i).getProfilePic());
                }
            }
            return itemView;
        }
    }
}
