package com.devostrum.docloud.models;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.devostrum.docloud.DatabaseHelper;

public class GroupModel {
    public static String KEY_ROWID = "_id";
    public static String KEY_GROUPID = "groupid";
    public static String KEY_GROUPNAME = "groupname";

    public String DATABASE_TABLE;
    private Context context;
    private SQLiteDatabase database;
    private DatabaseHelper dbHelper;

    public GroupModel(Context context, String uid) {
        this.context = context;
        this.DATABASE_TABLE = "'" + uid + "Groups'";
    }

    public GroupModel open() throws SQLException {
        dbHelper = new DatabaseHelper(context);
        database = dbHelper.getWritableDatabase();

        return this;
    }

    public void close() {
        dbHelper.close();
        database.close();
    }

    /**
     * Used to add a new group to the local database.
     *
     * @param groupid		ID of the group to add
     * @param groupname		Name of the group to add
     * @return
     */
    public long addGroup(String groupid, String groupname) {
        ContentValues initialValues = createContentValues(groupid, groupname);

        return database.insertOrThrow(DATABASE_TABLE, null, initialValues);
    }

    /**
     * Used when a group's name changes or the user's privilege in the group is changed
     *
     * @param groupid		ID of the group to add
     * @param groupname		Name of the group
     * @return				Success of update operation
     */
    public boolean updateGroupData(String groupid, String groupname) {
        ContentValues updateValues = createContentValues(groupid, groupname);

        return database.update(DATABASE_TABLE, updateValues, KEY_GROUPID + "=?", new String[] { groupid }) > 0;
    }

    /**
     * Used to remove a group from the local database
     *
     * @param groupid		ID of the group to remove
     * @return				Success of delete operation
     */
    public boolean deleteGroup(String groupid) {
        return database.delete(DATABASE_TABLE, KEY_GROUPID + "=?", new String[] { groupid }) > 0;
    }

    /**
     * Used to fetch all groups' data from the database.
     *
     * @return				A <object>Cursor</object> object with all the data
     */
    public Cursor fetchAllGroups() {
        return database.query(DATABASE_TABLE, new String[] { KEY_ROWID, KEY_GROUPID, KEY_GROUPNAME }, null, null,
                null, null, null);
    }

    public Cursor fetchGroup(String groupid) {
        Cursor groupCursor = database.query(DATABASE_TABLE, new String[] { KEY_ROWID, KEY_GROUPID, KEY_GROUPNAME}, KEY_GROUPID + "=?", new String[] { groupid }, null, null, null, null);
        groupCursor.moveToFirst();

        return groupCursor;
    }

    private ContentValues createContentValues(String groupid, String groupname) {
        ContentValues values = new ContentValues();

        values.put("groupid", groupid);
        values.put("groupname", groupname);

        return values;
    }
}
