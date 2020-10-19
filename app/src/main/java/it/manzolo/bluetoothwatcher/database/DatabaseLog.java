package it.manzolo.bluetoothwatcher.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class DatabaseLog {
    public static final String KEY_ID = "_id";
    public static final String KEY_DATA = "data";
    public static final String KEY_TYPE = "type";
    public static final String KEY_MESSAGE = "message";
    private static final String LOG_TAG = DatabaseLog.class.getSimpleName();
    // Database fields
    private static final String DATABASE_TABLE = "log";
    private final Context context;
    private SQLiteDatabase database;
    private DatabaseHelper dbHelper;

    public DatabaseLog(Context context) {
        this.context = context;
    }

    public DatabaseLog open() throws SQLException {
        dbHelper = new DatabaseHelper(context);
        database = dbHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        dbHelper.close();
    }

    private ContentValues createContentValues(String data, String message, String type) {
        ContentValues values = new ContentValues();
        values.put(KEY_MESSAGE, message);
        values.put(KEY_DATA, data);
        values.put(KEY_TYPE, type);

        return values;
    }

    // create a contact
    public long createRow(String data, String message, String type) {
        ContentValues initialValues = createContentValues(data, message, type);
        return database.insertOrThrow(DATABASE_TABLE, null, initialValues);
    }

    public void clear() {
        String deleteQuery = "delete from log";
        Log.d("TAG", deleteQuery);
        Cursor c = database.rawQuery(deleteQuery, null);
        c.moveToFirst();
        c.close();
    }
}