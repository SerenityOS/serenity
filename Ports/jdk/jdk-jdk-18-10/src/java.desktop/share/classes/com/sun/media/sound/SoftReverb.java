/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;

/**
 * Reverb effect based on allpass/comb filters. First audio is send to 8
 * parelled comb filters and then mixed together and then finally send thru 3
 * different allpass filters.
 *
 * @author Karl Helgason
 */
public final class SoftReverb implements SoftAudioProcessor {

    private static final class Delay {

        private float[] delaybuffer;
        private int rovepos = 0;

        Delay() {
            delaybuffer = null;
        }

        public void setDelay(int delay) {
            if (delay == 0)
                delaybuffer = null;
            else
                delaybuffer = new float[delay];
            rovepos = 0;
        }

        public void processReplace(float[] inout) {
            if (delaybuffer == null)
                return;
            int len = inout.length;
            int rnlen = delaybuffer.length;
            int rovepos = this.rovepos;

            for (int i = 0; i < len; i++) {
                float x = inout[i];
                inout[i] = delaybuffer[rovepos];
                delaybuffer[rovepos] = x;
                if (++rovepos == rnlen)
                    rovepos = 0;
            }
            this.rovepos = rovepos;
        }
    }

    private static final class AllPass {

        private final float[] delaybuffer;
        private final int delaybuffersize;
        private int rovepos = 0;
        private float feedback;

        AllPass(int size) {
            delaybuffer = new float[size];
            delaybuffersize = size;
        }

        public void setFeedBack(float feedback) {
            this.feedback = feedback;
        }

        public void processReplace(float[] inout) {
            int len = inout.length;
            int delaybuffersize = this.delaybuffersize;
            int rovepos = this.rovepos;
            for (int i = 0; i < len; i++) {
                float delayout = delaybuffer[rovepos];
                float input = inout[i];
                inout[i] = delayout - input;
                delaybuffer[rovepos] = input + delayout * feedback;
                if (++rovepos == delaybuffersize)
                    rovepos = 0;
            }
            this.rovepos = rovepos;
        }

        public void processReplace(float[] in, float[] out) {
            int len = in.length;
            int delaybuffersize = this.delaybuffersize;
            int rovepos = this.rovepos;
            for (int i = 0; i < len; i++) {
                float delayout = delaybuffer[rovepos];
                float input = in[i];
                out[i] = delayout - input;
                delaybuffer[rovepos] = input + delayout * feedback;
                if (++rovepos == delaybuffersize)
                    rovepos = 0;
            }
            this.rovepos = rovepos;
        }
    }

    private static final class Comb {

        private final float[] delaybuffer;
        private final int delaybuffersize;
        private int rovepos = 0;
        private float feedback;
        private float filtertemp = 0;
        private float filtercoeff1 = 0;
        private float filtercoeff2 = 1;

        Comb(int size) {
            delaybuffer = new float[size];
            delaybuffersize = size;
        }

        public void setFeedBack(float feedback) {
            this.feedback = feedback;
            filtercoeff2 = (1 - filtercoeff1)* feedback;
        }

        public void processMix(float[] in, float[] out) {
            int len = in.length;
            int delaybuffersize = this.delaybuffersize;
            int rovepos = this.rovepos;
            float filtertemp = this.filtertemp;
            float filtercoeff1 = this.filtercoeff1;
            float filtercoeff2 = this.filtercoeff2;
            for (int i = 0; i < len; i++) {
                float delayout = delaybuffer[rovepos];
                // One Pole Lowpass Filter
                filtertemp = (delayout * filtercoeff2)
                        + (filtertemp * filtercoeff1);
                out[i] += delayout;
                delaybuffer[rovepos] = in[i] + filtertemp;
                if (++rovepos == delaybuffersize)
                    rovepos = 0;
            }
            this.filtertemp  = filtertemp;
            this.rovepos = rovepos;
        }

        public void processReplace(float[] in, float[] out) {
            int len = in.length;
            int delaybuffersize = this.delaybuffersize;
            int rovepos = this.rovepos;
            float filtertemp = this.filtertemp;
            float filtercoeff1 = this.filtercoeff1;
            float filtercoeff2 = this.filtercoeff2;
            for (int i = 0; i < len; i++) {
                float delayout = delaybuffer[rovepos];
                // One Pole Lowpass Filter
                filtertemp = (delayout * filtercoeff2)
                        + (filtertemp * filtercoeff1);
                out[i] = delayout;
                delaybuffer[rovepos] = in[i] + filtertemp;
                if (++rovepos == delaybuffersize)
                    rovepos = 0;
            }
            this.filtertemp  = filtertemp;
            this.rovepos = rovepos;
        }

        public void setDamp(float val) {
            filtercoeff1 = val;
            filtercoeff2 = (1 - filtercoeff1)* feedback;
        }
    }
    private float roomsize;
    private float damp;
    private float gain = 1;
    private Delay delay;
    private Comb[] combL;
    private Comb[] combR;
    private AllPass[] allpassL;
    private AllPass[] allpassR;
    private float[] input;
    private float[] out;
    private float[] pre1;
    private float[] pre2;
    private float[] pre3;
    private boolean denormal_flip = false;
    private boolean mix = true;
    private SoftAudioBuffer inputA;
    private SoftAudioBuffer left;
    private SoftAudioBuffer right;
    private boolean dirty = true;
    private float dirty_roomsize;
    private float dirty_damp;
    private float dirty_predelay;
    private float dirty_gain;
    private float samplerate;
    private boolean light = true;

    @Override
    public void init(float samplerate, float controlrate) {
        this.samplerate = samplerate;

        double freqscale = ((double) samplerate) / 44100.0;
        // freqscale = 1.0/ freqscale;

        int stereospread = 23;

        delay = new Delay();

        combL = new Comb[8];
        combR = new Comb[8];
        combL[0] = new Comb((int) (freqscale * (1116)));
        combR[0] = new Comb((int) (freqscale * (1116 + stereospread)));
        combL[1] = new Comb((int) (freqscale * (1188)));
        combR[1] = new Comb((int) (freqscale * (1188 + stereospread)));
        combL[2] = new Comb((int) (freqscale * (1277)));
        combR[2] = new Comb((int) (freqscale * (1277 + stereospread)));
        combL[3] = new Comb((int) (freqscale * (1356)));
        combR[3] = new Comb((int) (freqscale * (1356 + stereospread)));
        combL[4] = new Comb((int) (freqscale * (1422)));
        combR[4] = new Comb((int) (freqscale * (1422 + stereospread)));
        combL[5] = new Comb((int) (freqscale * (1491)));
        combR[5] = new Comb((int) (freqscale * (1491 + stereospread)));
        combL[6] = new Comb((int) (freqscale * (1557)));
        combR[6] = new Comb((int) (freqscale * (1557 + stereospread)));
        combL[7] = new Comb((int) (freqscale * (1617)));
        combR[7] = new Comb((int) (freqscale * (1617 + stereospread)));

        allpassL = new AllPass[4];
        allpassR = new AllPass[4];
        allpassL[0] = new AllPass((int) (freqscale * (556)));
        allpassR[0] = new AllPass((int) (freqscale * (556 + stereospread)));
        allpassL[1] = new AllPass((int) (freqscale * (441)));
        allpassR[1] = new AllPass((int) (freqscale * (441 + stereospread)));
        allpassL[2] = new AllPass((int) (freqscale * (341)));
        allpassR[2] = new AllPass((int) (freqscale * (341 + stereospread)));
        allpassL[3] = new AllPass((int) (freqscale * (225)));
        allpassR[3] = new AllPass((int) (freqscale * (225 + stereospread)));

        for (int i = 0; i < allpassL.length; i++) {
            allpassL[i].setFeedBack(0.5f);
            allpassR[i].setFeedBack(0.5f);
        }

        /* Init other settings */
        globalParameterControlChange(new int[]{0x01 * 128 + 0x01}, 0, 4);

    }

    @Override
    public void setInput(int pin, SoftAudioBuffer input) {
        if (pin == 0)
            inputA = input;
    }

    @Override
    public void setOutput(int pin, SoftAudioBuffer output) {
        if (pin == 0)
            left = output;
        if (pin == 1)
            right = output;
    }

    @Override
    public void setMixMode(boolean mix) {
        this.mix = mix;
    }

    private boolean silent = true;

    @Override
    public void processAudio() {
        boolean silent_input = this.inputA.isSilent();
        if(!silent_input)
            silent = false;
        if(silent)
        {
            if (!mix) {
                left.clear();
                right.clear();
            }
            return;
        }

        float[] inputA = this.inputA.array();
        float[] left = this.left.array();
        float[] right = this.right == null ? null : this.right.array();

        int numsamples = inputA.length;
        if (input == null || input.length < numsamples)
            input = new float[numsamples];

        float again = gain * 0.018f / 2;

        denormal_flip = !denormal_flip;
        if(denormal_flip)
            for (int i = 0; i < numsamples; i++)
                input[i] = inputA[i] * again + 1E-20f;
        else
            for (int i = 0; i < numsamples; i++)
                input[i] = inputA[i] * again - 1E-20f;

        delay.processReplace(input);

        if(light && (right != null))
        {
            if (pre1 == null || pre1.length < numsamples)
            {
                pre1 = new float[numsamples];
                pre2 = new float[numsamples];
                pre3 = new float[numsamples];
            }

            for (int i = 0; i < allpassL.length; i++)
                allpassL[i].processReplace(input);

            combL[0].processReplace(input, pre3);
            combL[1].processReplace(input, pre3);

            combL[2].processReplace(input, pre1);
            for (int i = 4; i < combL.length-2; i+=2)
                combL[i].processMix(input, pre1);

            combL[3].processReplace(input, pre2);
            for (int i = 5; i < combL.length-2; i+=2)
                combL[i].processMix(input, pre2);

            if (!mix)
            {
                Arrays.fill(right, 0);
                Arrays.fill(left, 0);
            }
            for (int i = combR.length-2; i < combR.length; i++)
                combR[i].processMix(input, right);
            for (int i = combL.length-2; i < combL.length; i++)
                combL[i].processMix(input, left);

            for (int i = 0; i < numsamples; i++)
            {
                float p = pre1[i] - pre2[i];
                float m = pre3[i];
                left[i] += m + p;
                right[i] += m - p;
            }
        }
        else
        {
            if (out == null || out.length < numsamples)
                out = new float[numsamples];

            if (right != null) {
                if (!mix)
                    Arrays.fill(right, 0);
                allpassR[0].processReplace(input, out);
                for (int i = 1; i < allpassR.length; i++)
                    allpassR[i].processReplace(out);
                for (int i = 0; i < combR.length; i++)
                    combR[i].processMix(out, right);
            }

            if (!mix)
                Arrays.fill(left, 0);
            allpassL[0].processReplace(input, out);
            for (int i = 1; i < allpassL.length; i++)
                allpassL[i].processReplace(out);
            for (int i = 0; i < combL.length; i++)
                combL[i].processMix(out, left);
        }

        if (silent_input) {
            silent = true;
            for (int i = 0; i < numsamples; i++)
            {
                float v = left[i];
                if(v > 1E-10 || v < -1E-10)
                {
                    silent = false;
                    break;
                }
            }
        }

    }

    @Override
    public void globalParameterControlChange(int[] slothpath, long param,
                                             long value) {
        if (slothpath.length == 1) {
            if (slothpath[0] == 0x01 * 128 + 0x01) {

                if (param == 0) {
                    if (value == 0) {
                        // Small Room A small size room with a length
                        // of 5m or so.
                        dirty_roomsize = (1.1f);
                        dirty_damp = (5000);
                        dirty_predelay = (0);
                        dirty_gain = (4);
                        dirty = true;
                    }
                    if (value == 1) {
                        // Medium Room A medium size room with a length
                        // of 10m or so.
                        dirty_roomsize = (1.3f);
                        dirty_damp = (5000);
                        dirty_predelay = (0);
                        dirty_gain = (3);
                        dirty = true;
                    }
                    if (value == 2) {
                        // Large Room A large size room suitable for
                        // live performances.
                        dirty_roomsize = (1.5f);
                        dirty_damp = (5000);
                        dirty_predelay = (0);
                        dirty_gain = (2);
                        dirty = true;
                    }
                    if (value == 3) {
                        // Medium Hall A medium size concert hall.
                        dirty_roomsize = (1.8f);
                        dirty_damp = (24000);
                        dirty_predelay = (0.02f);
                        dirty_gain = (1.5f);
                        dirty = true;
                    }
                    if (value == 4) {
                        // Large Hall A large size concert hall
                        // suitable for a full orchestra.
                        dirty_roomsize = (1.8f);
                        dirty_damp = (24000);
                        dirty_predelay = (0.03f);
                        dirty_gain = (1.5f);
                        dirty = true;
                    }
                    if (value == 8) {
                        // Plate A plate reverb simulation.
                        dirty_roomsize = (1.3f);
                        dirty_damp = (2500);
                        dirty_predelay = (0);
                        dirty_gain = (6);
                        dirty = true;
                    }
                } else if (param == 1) {
                    dirty_roomsize = ((float) (Math.exp((value - 40) * 0.025)));
                    dirty = true;
                }

            }
        }
    }

    @Override
    public void processControlLogic() {
        if (dirty) {
            dirty = false;
            setRoomSize(dirty_roomsize);
            setDamp(dirty_damp);
            setPreDelay(dirty_predelay);
            setGain(dirty_gain);
        }
    }

    public void setRoomSize(float value) {
        roomsize = 1 - (0.17f / value);

        for (int i = 0; i < combL.length; i++) {
            combL[i].feedback = roomsize;
            combR[i].feedback = roomsize;
        }
    }

    public void setPreDelay(float value) {
        delay.setDelay((int)(value * samplerate));
    }

    public void setGain(float gain) {
        this.gain = gain;
    }

    public void setDamp(float value) {
        double x = (value / samplerate) * (2 * Math.PI);
        double cx = 2 - Math.cos(x);
        damp = (float)(cx - Math.sqrt(cx * cx - 1));
        if (damp > 1)
            damp = 1;
        if (damp < 0)
            damp = 0;

        // damp = value * 0.4f;
        for (int i = 0; i < combL.length; i++) {
            combL[i].setDamp(damp);
            combR[i].setDamp(damp);
        }
    }

    public void setLightMode(boolean light)
    {
        this.light = light;
    }
}

