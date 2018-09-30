package com.mentalmachines.ttime.services;

import android.app.IntentService;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;

import com.mentalmachines.ttime.data.DBHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by emezias on 2/5/16.
 */
public class CopyDBService extends IntentService {

    public static final String TAG = "CopyDBService";

    public CopyDBService( ) {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "starting service");
        FileInputStream inStream = null;
        try {
            inStream = new FileInputStream("/data/data/com.mentalmachines.ttime/databases/" +
                    DBHelper.DBNAME);
            final File outFile;
            if (Environment.getExternalStorageState().equals(
                    Environment.MEDIA_MOUNTED)) {
                outFile = new File(Environment.getExternalStorageDirectory(), "copy" + DBHelper.DBNAME);

            } else {
                outFile = new File("copy " + DBHelper.DBNAME);
            }
            //outFile.setReadOnly(false);
            //this line is failing on permissions
            if(outFile.exists()) outFile.delete();
            FileOutputStream outStream = new FileOutputStream(outFile);
            int tmp = inStream.read(); //read one byte
            while(tmp != -1) {
                outStream.write(tmp); //write that byte
                tmp = inStream.read();
            }
            Log.d(TAG, "mDB file: " + outFile.getName());
            //Toast.makeText(this, "database copy to sdcard complete", Toast.LENGTH_SHORT).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
