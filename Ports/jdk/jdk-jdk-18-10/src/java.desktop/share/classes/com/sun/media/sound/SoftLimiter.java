/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.media.sound;

/**
 * A simple look-ahead volume limiter with very fast attack and fast release.
 * This filter is used for preventing clipping.
 *
 * @author Karl Helgason
 */
public final class SoftLimiter implements SoftAudioProcessor {

    float lastmax = 0;
    float gain = 1;
    float[] temp_bufferL;
    float[] temp_bufferR;
    boolean mix = false;
    SoftAudioBuffer bufferL;
    SoftAudioBuffer bufferR;
    SoftAudioBuffer bufferLout;
    SoftAudioBuffer bufferRout;
    float controlrate;

    @Override
    public void init(float samplerate, float controlrate) {
        this.controlrate = controlrate;
    }

    @Override
    public void setInput(int pin, SoftAudioBuffer input) {
        if (pin == 0)
            bufferL = input;
        if (pin == 1)
            bufferR = input;
    }

    @Override
    public void setOutput(int pin, SoftAudioBuffer output) {
        if (pin == 0)
            bufferLout = output;
        if (pin == 1)
            bufferRout = output;
    }

    @Override
    public void setMixMode(boolean mix) {
        this.mix = mix;
    }

    @Override
    public void globalParameterControlChange(int[] slothpath, long param,
                                             long value) {
    }

    double silentcounter = 0;

    @Override
    public void processAudio() {
        if (this.bufferL.isSilent()
                && (this.bufferR == null || this.bufferR.isSilent())) {
            silentcounter += 1 / controlrate;

            if (silentcounter > 60) {
                if (!mix) {
                    bufferLout.clear();
                    if (bufferRout != null) bufferRout.clear();
                }
                return;
            }
        } else
            silentcounter = 0;

        float[] bufferL = this.bufferL.array();
        float[] bufferR = this.bufferR == null ? null : this.bufferR.array();
        float[] bufferLout = this.bufferLout.array();
        float[] bufferRout = this.bufferRout == null
                                ? null : this.bufferRout.array();

        if (temp_bufferL == null || temp_bufferL.length < bufferL.length)
            temp_bufferL = new float[bufferL.length];
        if (bufferR != null)
            if (temp_bufferR == null || temp_bufferR.length < bufferR.length)
                temp_bufferR = new float[bufferR.length];

        float max = 0;
        int len = bufferL.length;

        if (bufferR == null) {
            for (int i = 0; i < len; i++) {
                if (bufferL[i] > max)
                    max = bufferL[i];
                if (-bufferL[i] > max)
                    max = -bufferL[i];
            }
        } else {
            for (int i = 0; i < len; i++) {
                if (bufferL[i] > max)
                    max = bufferL[i];
                if (bufferR[i] > max)
                    max = bufferR[i];
                if (-bufferL[i] > max)
                    max = -bufferL[i];
                if (-bufferR[i] > max)
                    max = -bufferR[i];
            }
        }

        float lmax = lastmax;
        lastmax = max;
        if (lmax > max)
            max = lmax;

        float newgain = 1;
        if (max > 0.99f)
            newgain = 0.99f / max;
        else
            newgain = 1;

        if (newgain > gain)
            newgain = (newgain + gain * 9) / 10f;

        float gaindelta = (newgain - gain) / len;
        if (mix) {
            if (bufferR == null) {
                for (int i = 0; i < len; i++) {
                    gain += gaindelta;
                    float bL = bufferL[i];
                    float tL = temp_bufferL[i];
                    temp_bufferL[i] = bL;
                    bufferLout[i] += tL * gain;
                }
            } else {
                for (int i = 0; i < len; i++) {
                    gain += gaindelta;
                    float bL = bufferL[i];
                    float bR = bufferR[i];
                    float tL = temp_bufferL[i];
                    float tR = temp_bufferR[i];
                    temp_bufferL[i] = bL;
                    temp_bufferR[i] = bR;
                    bufferLout[i] += tL * gain;
                    bufferRout[i] += tR * gain;
                }
            }

        } else {
            if (bufferR == null) {
                for (int i = 0; i < len; i++) {
                    gain += gaindelta;
                    float bL = bufferL[i];
                    float tL = temp_bufferL[i];
                    temp_bufferL[i] = bL;
                    bufferLout[i] = tL * gain;
                }
            } else {
                for (int i = 0; i < len; i++) {
                    gain += gaindelta;
                    float bL = bufferL[i];
                    float bR = bufferR[i];
                    float tL = temp_bufferL[i];
                    float tR = temp_bufferR[i];
                    temp_bufferL[i] = bL;
                    temp_bufferR[i] = bR;
                    bufferLout[i] = tL * gain;
                    bufferRout[i] = tR * gain;
                }
            }

        }
        gain = newgain;
    }

    @Override
    public void processControlLogic() {
    }
}
