/**
 * Copyright 2016 - Daniel Hintze <daniel.hintze@fhdw.de>
 * 				 Sebastian Scholz <sebastian.scholz@fhdw.de>
 * 				 Rainhard D. Findling <rainhard.findling@fh-hagenberg.at>
 * 				 Muhammad Muaaz <muhammad.muaaz@usmile.at>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package at.usmile.gaitmodule.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

import Jama.Matrix;
import at.usmile.gaitmodule.extras.LogMessage;
import edu.umbc.cs.maple.utils.JamaUtils;

public class DataBaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "gait_Authentication.db";
    public static final String GAIT_DATA_BASE_PATH = Environment.getExternalStorageState() + "/gaitDataRecording" + "/gaitDataBase/";
    public static final String USERS_TABLE_NAME = "users";
    public static final String USERS_COLUMN_ID = "_userId";
    public static final String USERS_COLUMN_NAME = "userName";
    public static final String USERS_COLUMN_BESTGAITCYCLES = "bestGaitCycles";
    private static final String[] userData = new String[]{USERS_COLUMN_NAME, USERS_COLUMN_BESTGAITCYCLES};
    //public static final String USERS_COLUMN_REMAINEDGAITCYCLES = "remainedGaitCycles";
    private static final String CREATE_USERS_TABLE =
            "create table " + USERS_TABLE_NAME
                    + "("
                    + USERS_COLUMN_ID + " INTEGER primery key,"
                    + USERS_COLUMN_NAME + " TEXT,"
                    + USERS_COLUMN_BESTGAITCYCLES + " TEXT" + ");";
    private static int dataBaseVersion = 3;
    //	+ USERS_COLUMN_REMAINEDGAITCYCLES + " TEXT" + ");";

    public DataBaseHelper(Context context) {
        super(context, DATABASE_NAME, null, dataBaseVersion);

    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(CREATE_USERS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        db.execSQL("DROP TABLE IF EXISTS contacts");
        onCreate(db);
    }

    public Cursor getData(int userId) {

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from users where id=" + userId + "", null);
        db.close();
        return res;
    }


    public boolean insertUser(String name, String bestGaitCycles) {

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put("userName", name);
        contentValues.put("bestGaitCycles", bestGaitCycles);

        db.insert(USERS_TABLE_NAME, null, contentValues);
        db.close();
        return true;
    }


    public int numberOfRows() {

        SQLiteDatabase db = this.getReadableDatabase();

        int numRows = (int) DatabaseUtils.queryNumEntries(db, USERS_TABLE_NAME);
        db.close();
        return numRows;
    }

    public boolean updateUserInfo(String name, String bestGaitCycles) {

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("userName", name);
        contentValues.put("bestGaitCycles", bestGaitCycles);
        db.update(USERS_TABLE_NAME, contentValues, "userName = ?", new String[]{name});
        db.close();
        return true;
    }

    public Integer deleteUser(Integer userId) {

        SQLiteDatabase db = this.getWritableDatabase();
        int a = db.delete(USERS_TABLE_NAME, "userId = ? ", new String[]{Integer.toString(userId)});
        db.close();
        return a;
    }

    public ArrayList getAllUsers() {

        ArrayList array_list = new ArrayList();
        SQLiteDatabase db = this.getReadableDatabase();
        try {
            Cursor res = db.rawQuery("select * from users", null);

            res.moveToFirst();
            while (res.isAfterLast() == false) {
                array_list.add(res.getString(res.getColumnIndex(USERS_COLUMN_NAME)));
                res.moveToNext();
            }

            res.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        db.close();
        return array_list;
    }

    public boolean deleteSingleRow(long rowId) {

        SQLiteDatabase db = this.getWritableDatabase();

        return db.delete(USERS_TABLE_NAME, USERS_COLUMN_ID + "=" + rowId, null) > 0;
    }


    public void emptyDataBaseTable2() {

        SQLiteDatabase db = this.getWritableDatabase();

        db.delete(USERS_TABLE_NAME, null, null);
        db.execSQL("vacuum");
    }


    public String[] getUserNames() {

        String selectQuery = "SELECT  * FROM " + USERS_TABLE_NAME;
        SQLiteDatabase db = this.getReadableDatabase();
        String[] data = null;

        try {

            Cursor cursor = db.rawQuery(selectQuery, null);
            if (cursor.moveToFirst()) {
                do {

                } while (cursor.moveToNext());
            }

            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        db.close();


        return data;
    }

    public void deleteUser(String userName) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.delete(USERS_TABLE_NAME, "username = ?", new String[]{userName});
        } catch (Exception e) {
            e.printStackTrace();
        }

        db.close();

    }

    public String getUserData(String userName) {

        SQLiteDatabase db = this.getWritableDatabase();
        String str = null;
        String WHERE = USERS_COLUMN_NAME + "=?";
        try {
            // Cursor cursor = db.query(USERS_TABLE_NAME,userData, WHERE , new String[] { userName },null, null, null);
            Cursor cursor = db.query(USERS_TABLE_NAME, userData, "username=?", new String[]{userName}, null, null, null);
            int userNameCol = cursor.getColumnIndex(USERS_COLUMN_NAME);
            int userTemplateCol = cursor.getColumnIndex(USERS_COLUMN_BESTGAITCYCLES);

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                str = cursor.getString(userTemplateCol);
            }

            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }


        db.close();
        return str;
    }


    public int getUserID(String userName) {
        LogMessage.setStatus("eDB", "accessing database");
        SQLiteDatabase db = this.getWritableDatabase();
        int rowID = -1;
        String WHERE = USERS_COLUMN_NAME + "=?";
        try {
            // Cursor cursor = db.query(USERS_TABLE_NAME,userData, WHERE , new String[] { userName },null, null, null);
            Cursor cursor = db.query(USERS_TABLE_NAME, new String[]{USERS_COLUMN_ID, USERS_COLUMN_NAME, USERS_COLUMN_BESTGAITCYCLES}, "username=?", new String[]{userName}, null, null, null);
            int userIDCol = cursor.getColumnIndex(USERS_COLUMN_ID);
            int userNameCol = cursor.getColumnIndex(USERS_COLUMN_NAME);
            int userTemplateCol = cursor.getColumnIndex(USERS_COLUMN_BESTGAITCYCLES);

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                rowID = cursor.getInt(userIDCol);
            }

            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        db.close();

        return rowID;
    }


    // Fucntion to convert matrix data to strings
    public String matrix2StringRepresentation(Matrix _mat) {
        StringBuilder sb = new StringBuilder();

        int num_Cols = _mat.getColumnDimension();
        if (num_Cols > 0) {
            for (int i = 0; i < num_Cols; i++) {

                double[] singleColumn = JamaUtils.getcol(_mat, i).getColumnPackedCopy();
                String str = Arrays.toString(singleColumn);
                sb = sb.append(str).append("\t");
            }
            Log.i("TAG", sb.toString());

        }
        return sb.toString();
    }

    // Function to convert string data back to matrix representation
    public Matrix String2MatrixRepresentation(String _str) {
        String[] s_array = _str.split("\t");
        Matrix[] mtr = new Matrix[s_array.length];
        for (int i = 0; i < s_array.length; i++) {
            // cut off the square brackets at the beginning and at the end
            String s_arr = s_array[i].substring(1, s_array[i].length() - 1);
            String[] strArray = s_arr.split(", ");
            double[] vals = new double[strArray.length];
            for (int j = 0; j < strArray.length; j++) {
                vals[j] = Double.parseDouble(strArray[j]);
            }
            mtr[i] = new Matrix(vals, 1).transpose();
        }
        int p = 1;
        Matrix fullMatrix = mtr[0];
        while (p < mtr.length) {
            fullMatrix = JamaUtils.columnAppend(fullMatrix, mtr[p]);
            p = p + 1;
        }
        return fullMatrix;
    }
}
