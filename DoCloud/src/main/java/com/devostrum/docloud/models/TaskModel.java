package com.devostrum.docloud.models;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.devostrum.docloud.DatabaseHelper;

/**
 * Created by ashiswin on 25/9/14.
 */
public class TaskModel {
    public static String KEY_ROWID = "_id";
    public static String KEY_GROUPID = "groupid";
    public static String KEY_TODOID = "todoid";
    public static String KEY_NAME = "name";
    public static String KEY_DESCRIPTION = "description";
    public static String KEY_DUEDATE = "duedate";
    public static String KEY_COMPLETION = "completion";
    public static String KEY_ASSIGNEES = "assignees";
    public static String KEY_COMPLETED = "completed";

    public String DATABASE_TABLE;
    private Context context;
    private SQLiteDatabase database;
    private DatabaseHelper dbHelper;

    public TaskModel(Context context, String uid) {
        this.context = context;
        this.DATABASE_TABLE = "'" + uid + "Todos'";
    }

    public TaskModel open() throws SQLException {
        dbHelper = new DatabaseHelper(context);
        database = dbHelper.getWritableDatabase();

        return this;
    }

    public void close() {
        dbHelper.close();
        database.close();
    }

    public long addTodo(String todoid, String groupid, String name, String description, String duedate, int completion, String assignees, String completed) {
        ContentValues initialValues = createContentValues(todoid, groupid, name, description, duedate, completion, assignees, completed);

        return database.insertOrThrow(DATABASE_TABLE, null, initialValues);
    }

    public boolean updateTodo(String todoid, String groupid, String name, String description, String duedate, int completion, String assignees, String completed) {
        ContentValues updateValues = createContentValues(todoid, groupid, name, description, duedate, completion, assignees, completed);

        return database.update(DATABASE_TABLE, updateValues, KEY_TODOID + "=?", new String[] { todoid }) > 0;
    }

    public boolean deleteTodo(String todoid) {
        return database.delete(DATABASE_TABLE, KEY_TODOID + "=?", new String[] { todoid }) > 0;
    }

    public Cursor fetchAllTodos(String groupid) {
        Cursor todoCursor = database.query(DATABASE_TABLE, new String[] { KEY_TODOID, KEY_NAME }, KEY_GROUPID + "=?", new String[] { groupid }, null, null, null, null);
        todoCursor.moveToFirst();

        return todoCursor;
    }

    public Cursor fetchEveryTodo() {
        Cursor todoCursor = database.query(DATABASE_TABLE, new String[] { KEY_TODOID, KEY_NAME, KEY_DUEDATE }, null, null, null, null, null, null);
        todoCursor.moveToFirst();

        return todoCursor;
    }
    public Cursor fetchTodo(String todoid) {
        Cursor groupCursor = database.query(DATABASE_TABLE, null, KEY_TODOID + "=?", new String[] { todoid }, null, null, null, null);
        groupCursor.moveToFirst();

        return groupCursor;
    }

    private ContentValues createContentValues(String todoid, String groupid, String name, String description, String duedate, int completion, String assignees, String completed) {
        ContentValues values = new ContentValues();

        values.put(KEY_GROUPID, groupid);
        values.put(KEY_TODOID, todoid);
        values.put(KEY_NAME, name);
        values.put(KEY_DESCRIPTION, description);
        if(duedate != null) {
            values.put(KEY_DUEDATE, duedate);
        }
        values.put(KEY_COMPLETION, completion);
        values.put(KEY_ASSIGNEES, assignees);
        values.put(KEY_COMPLETED, completed);

        return values;
    }
}
