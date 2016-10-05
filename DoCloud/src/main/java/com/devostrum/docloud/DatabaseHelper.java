/*
 * Copyright (C) 2011 Isaac Ashiwn Ravindran
 * Copyright (C) 2011 Nathanael See
 * 
 * Database Helper
 * Facilitates the creation and upgrading of a database
 */
package com.devostrum.docloud;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String databaseName = "applicationdata";
    private static final int databaseVersion = 3;

    /* Synchronization Service Query Table allows any activity to enter a notice consisting of a URL and data string
     * for the Synchronization Service to connect to and send the data to without the activity doing anything.
     * Speeds up activity's performance as it need not wait for the HTTP request to complete
     */
    private static final String SSQTCreateString =
            "create table ssqt (" +
                    "_id integer primary key autoincrement, " +
                    "address text not null, " +
                    "data text not null);";

    public DatabaseHelper(Context context) {
        super(context, databaseName, null, databaseVersion);
        // Constructor
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        // Create all tables with sql strings specified earlier
        database.execSQL(SSQTCreateString);
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int arg1, int arg2) {
        // Drop all tables
        database.execSQL("DROP TABLE IF EXISTS ssqt");
        onCreate(database);
    }

    public void createUserTables(SQLiteDatabase database, String uid) {
        String groupTableString = "create table if not exists '" + uid + "Groups' (" +
                "_id integer primary key autoincrement, " +
                "groupid text not null, " +
                "groupname text not null);";
        String tasksTableString = "create table if not exists '" + uid + "Todos' (" +
                "_id integer primary key autoincrement, " +
                "todoid text not null, " +
                "groupid text not null, " +
                "name text not null, " +
                "description text, " +
                "duedate text, " +
                "completion integer not null, " +
                "assignees text, " +
                "completed text);";
        String friendTableString = "create table if not exists '" + uid + "Friends' (" +
                "_id integer primary key autoincrement, " +
                "frienduid text not null, " +
                "friendname text not null, " +
                "friendemail text not null);";
        String remindersTableString = "create table if not exists '" + uid + "Reminders' (" +
                "_id integer primary key autoincrement, " +
                "groupid text not null, " +
                "todoid text not null, " +
                "duedate text not null);";

        database.execSQL(groupTableString);
        database.execSQL(tasksTableString);
        database.execSQL(friendTableString);
        database.execSQL(remindersTableString);
    }
}
