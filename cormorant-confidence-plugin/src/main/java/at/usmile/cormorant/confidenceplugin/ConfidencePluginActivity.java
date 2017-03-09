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
package at.usmile.cormorant.confidenceplugin;

import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import at.usmile.cormorant.api.AbstractPluginActivity;
import at.usmile.cormorant.api.model.StatusDataConfidence;

public class ConfidencePluginActivity extends AbstractPluginActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demoplugin);
    }

    @Override
    protected void onResume() {
        // mock confidence UI - textview and slider
        final TextView textviewConfidence = (TextView) findViewById(R.id.textViewConfidence);
        textviewConfidence.setText(getConfidenceString());
        SeekBar seekbarConfidence = (SeekBar) findViewById(R.id.seekBarConfidence);
        seekbarConfidence.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar _seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar _seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar _seekBar, int _progress, boolean _fromUser) {
                textviewConfidence.setText(getConfidenceString());
            }

        });
        super.onResume();
    }

    private double getConfidence() {
        SeekBar seekbarConfidence = (SeekBar) findViewById(R.id.seekBarConfidence);
        int progress = seekbarConfidence.getProgress();
        return progress / 100.0d;
    }

    private String getConfidenceString() {
        return "confidence: " + getConfidence();
    }

    public void sendConfidence(View view) {
        publishConfidenceData(new StatusDataConfidence()
                .status(StatusDataConfidence.Status.OPERATIONAL)
                .confidence(getConfidence()));
        finish();
    }

}
