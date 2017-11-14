/**
 * Copyright 2016 - 2017
 *
 * Daniel Hintze <daniel.hintze@fhdw.de>
 * Sebastian Scholz <sebastian.scholz@fhdw.de>
 * Rainhard D. Findling <rainhard.findling@fh-hagenberg.at>
 * Muhammad Muaaz <muhammad.muaaz@usmile.at>
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
package at.usmile.gaitmodule.extras;

import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import Jama.Matrix;

public class DataStorageLocation {
    //Folder Location
    public static final String TRAIN_RAW_DATA_PATH = Environment.getExternalStorageDirectory() + "/gaitDataRecording" + "/rawTrainData" + "/acc/";
    public static final String TEMPLATE_PATH = Environment.getExternalStorageDirectory() + "/gaitDataRecording" + "/gaitTemplates" + "/acc/";
    public static final String TEST_RAW_DATA_PATH = Environment.getExternalStorageDirectory() + "/gaitDataRecording" + "/rawTestingData" + "/acc/";
    public static final String META_DATA_FILE = Environment.getExternalStorageDirectory() + "/gaitDataRecording" + "/metaData" + "/" + "metaFile.txt";
    public static final String ALL_TEMPLATES_PATH = Environment.getExternalStorageDirectory() + "/gaitDataRecording" + "/gaitTemplates" + "/acc/";
    public static final String TRAIN_PROCESSED_DATA_PATH = Environment.getExternalStorageDirectory() + "/gaitDataRecording" + "/ProcessedTrainData" + "/acc/";
    public static final String GAIT_DATA_BASE_PATH = Environment.getExternalStorageState() + "/gaitDataRecording" + "/gaitDataBase/";
    //File Names
    public static final String FILE_NAME = "gaitTestData";


    /**
     * In case is template creation is failed then we have to delete user data files
     */
    static public void deleteDirectory(File path) {
        if (path == null)
            return;
        if (path.exists()) {
            for (File f : path.listFiles()) {
                if (f.isDirectory()) {
                    deleteDirectory(f);
                    f.delete();
                } else {
                    f.delete();
                }
            }
            path.delete();
        }
    }


    public static Matrix LoadDataMatrix(File mFile) throws IOException {

        if (!mFile.exists()) {
            return null;
        } else {


            BufferedReader br = new BufferedReader(new FileReader(mFile.getAbsolutePath()));
            Matrix dataMatrix = null;
            try {
                dataMatrix = Matrix.read(br);

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                br.close();
            }
            return dataMatrix;
        }
    }

}
