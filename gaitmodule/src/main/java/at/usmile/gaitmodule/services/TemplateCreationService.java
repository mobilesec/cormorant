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
package at.usmile.gaitmodule.services;

/**
 * This is background intent service this is responsible for processing gait template data.
 * It generates gait template and puts it to the files and database respectively
 */

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.JMathStudio.Exceptions.IllegalArgumentException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import Jama.Matrix;
import at.usmile.gaitmodule.MainGaitActivity.TemplateCreationResponseReceiver;
import at.usmile.gaitmodule.dataProcessing.GaitDataProcessingSteps;
import at.usmile.gaitmodule.dataProcessing.Template;
import at.usmile.gaitmodule.database.DataBaseHelper;
import at.usmile.gaitmodule.extras.DataStorageLocation;
import at.usmile.gaitmodule.extras.LogMessage;
import edu.umbc.cs.maple.utils.JamaUtils;

public class TemplateCreationService extends IntentService {

    public static final String PARAM_OUT_MSG = "omsg";
    public static final String PARAM_IN_MSG = "imsg";
    private static final String TAG = "TemplateCreationService";
    private static String userName;
    private static Boolean appendData;
    private static boolean b = false;
    private DataBaseHelper myDb;

    // Constructor
    public TemplateCreationService() {
        super(TemplateCreationService.class.getName());
        myDb = new DataBaseHelper(TemplateCreationService.this);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        LogMessage.setStatus(TAG, "Template Creation Service Started");
        userName = intent.getStringExtra(PARAM_IN_MSG);
        appendData = intent.getBooleanExtra("AppendData", false);

        try {

            //b = GaitDataProcessingSteps.gaitDataLoading(DataStorageLocation.TRAIN_RAW_DATA_PATH, userName, appendData);
            ArrayList <Template> template = GaitDataProcessingSteps.generateTemplate(DataStorageLocation.TRAIN_RAW_DATA_PATH, userName, appendData);
            if(template.size() >0){
                b = true;

                double[] gaitCycleLengths = GaitDataProcessingSteps.getCycleLengths();
                for (int i = 0; i < gaitCycleLengths.length; i++) {
                    LogMessage.setStatus(TAG, "Cycle Lengths are:" + gaitCycleLengths[i]);
                }

            }

            Log.i(TAG, "ExitCode :" + b);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        if (b && !appendData) {

            Matrix mtrRemained = GaitDataProcessingSteps.getAllRemainedGaitCycles();

            if (mtrRemained != null) {
                String str = myDb.matrix2StringRepresentation(mtrRemained);
                myDb.insertUser(userName, str);
            }
        }

        if (b && appendData) {
            LogMessage.setStatus(TAG, "Appending user Data for user: " + userName + ", DPEXIT=" + b + ", appendData=" + appendData);
            //Pick previous gait cycles
            Matrix mtrRemained = GaitDataProcessingSteps.getAllRemainedGaitCycles();
            Matrix mtrPreviousRemained = null;
            mtrPreviousRemained = myDb.String2MatrixRepresentation(myDb.getUserData(userName));

            Matrix updatedDataRemained = JamaUtils.columnAppend(mtrPreviousRemained, mtrRemained);
            String str = myDb.matrix2StringRepresentation(updatedDataRemained);
            LogMessage.setStatus(TAG, "User id is:" + userName);
            LogMessage.setStatus(TAG, str);
            myDb.updateUserInfo(userName, str);
            LogMessage.setStatus(TAG, "Information of: " + userName + " is updated");

            // We are just prinint test data only for testing point of view
            // In updated versions we need not to include this.
            PrintWriter ptrRemained;
            try {
                ptrRemained = new PrintWriter(new File(DataStorageLocation.ALL_TEMPLATES_PATH.concat("/RemainedGaitCycles/").concat(userName).concat(".txt")));
                updatedDataRemained.print(ptrRemained, 10, 5);
                ptrRemained.flush();
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

        final Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(TemplateCreationResponseReceiver.ACTION_RESP);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent.putExtra(PARAM_OUT_MSG, "Template Creation Finished");
        broadcastIntent.putExtra("ExitCode", b);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
    }

}
