/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Patch;
import javax.sound.midi.ShortMessage;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/**
 * Software synthesizer main audio mixer.
 *
 * @author Karl Helgason
 */
public final class SoftMainMixer {

    // A private class thats contains a ModelChannelMixer and it's private buffers.
    // This becomes necessary when we want to have separate delay buffers for each channel mixer.
    private class SoftChannelMixerContainer
    {
        ModelChannelMixer mixer;
        SoftAudioBuffer[] buffers;
    }

    public static final int CHANNEL_LEFT = 0;
    public static final int CHANNEL_RIGHT = 1;
    public static final int CHANNEL_MONO = 2;
    public static final int CHANNEL_DELAY_LEFT = 3;
    public static final int CHANNEL_DELAY_RIGHT = 4;
    public static final int CHANNEL_DELAY_MONO = 5;
    public static final int CHANNEL_EFFECT1 = 6;
    public static final int CHANNEL_EFFECT2 = 7;
    public static final int CHANNEL_DELAY_EFFECT1 = 8;
    public static final int CHANNEL_DELAY_EFFECT2 = 9;
    public static final int CHANNEL_LEFT_DRY = 10;
    public static final int CHANNEL_RIGHT_DRY = 11;
    public static final int CHANNEL_SCRATCH1 = 12;
    public static final int CHANNEL_SCRATCH2 = 13;
    boolean active_sensing_on = false;
    private long msec_last_activity = -1;
    private boolean pusher_silent = false;
    private int pusher_silent_count = 0;
    private long sample_pos = 0;
    boolean readfully = true;
    private final Object control_mutex;
    private final SoftSynthesizer synth;
    private float samplerate = 44100;
    private int nrofchannels = 2;
    private SoftVoice[] voicestatus = null;
    private final SoftAudioBuffer[] buffers;
    private final SoftReverb reverb;
    private final SoftAudioProcessor chorus;
    private final SoftAudioProcessor agc;
    private long msec_buffer_len = 0;
    private int buffer_len = 0;
    TreeMap<Long, Object> midimessages = new TreeMap<>();
    private int delay_midievent = 0;
    private int max_delay_midievent = 0;
    double last_volume_left = 1.0;
    double last_volume_right = 1.0;
    private final double[] co_master_balance = new double[1];
    private final double[] co_master_volume = new double[1];
    private final double[] co_master_coarse_tuning = new double[1];
    private final double[] co_master_fine_tuning = new double[1];
    private final AudioInputStream ais;
    private Set<SoftChannelMixerContainer> registeredMixers = null;
    private Set<ModelChannelMixer> stoppedMixers = null;
    private SoftChannelMixerContainer[] cur_registeredMixers = null;
    SoftControl co_master = new SoftControl() {

        double[] balance = co_master_balance;
        double[] volume = co_master_volume;
        double[] coarse_tuning = co_master_coarse_tuning;
        double[] fine_tuning = co_master_fine_tuning;

        @Override
        public double[] get(int instance, String name) {
            if (name == null)
                return null;
            if (name.equals("balance"))
                return balance;
            if (name.equals("volume"))
                return volume;
            if (name.equals("coarse_tuning"))
                return coarse_tuning;
            if (name.equals("fine_tuning"))
                return fine_tuning;
            return null;
        }
    };

    private void processSystemExclusiveMessage(byte[] data) {
        synchronized (synth.control_mutex) {
            activity();

            // Universal Non-Real-Time SysEx
            if ((data[1] & 0xFF) == 0x7E) {
                int deviceID = data[2] & 0xFF;
                if (deviceID == 0x7F || deviceID == synth.getDeviceID()) {
                    int subid1 = data[3] & 0xFF;
                    int subid2;
                    switch (subid1) {
                    case 0x08:  // MIDI Tuning Standard
                        subid2 = data[4] & 0xFF;
                        switch (subid2) {
                        case 0x01:  // BULK TUNING DUMP
                        {
                            // http://www.midi.org/about-midi/tuning.shtml
                            SoftTuning tuning = synth.getTuning(new Patch(0,
                                    data[5] & 0xFF));
                            tuning.load(data);
                            break;
                        }
                        case 0x04:  // KEY-BASED TUNING DUMP
                        case 0x05:  // SCALE/OCTAVE TUNING DUMP, 1 byte format
                        case 0x06:  // SCALE/OCTAVE TUNING DUMP, 2 byte format
                        case 0x07:  // SINGLE NOTE TUNING CHANGE (NON REAL-TIME)
                                    // (BANK)
                        {
                            // http://www.midi.org/about-midi/tuning_extens.shtml
                            SoftTuning tuning = synth.getTuning(new Patch(
                                    data[5] & 0xFF, data[6] & 0xFF));
                            tuning.load(data);
                            break;
                        }
                        case 0x08:  // scale/octave tuning 1-byte form (Non
                                    // Real-Time)
                        case 0x09:  // scale/octave tuning 2-byte form (Non
                                    // Real-Time)
                        {
                            // http://www.midi.org/about-midi/tuning-scale.shtml
                            SoftTuning tuning = new SoftTuning(data);
                            int channelmask = (data[5] & 0xFF) * 16384
                                    + (data[6] & 0xFF) * 128 + (data[7] & 0xFF);
                            SoftChannel[] channels = synth.channels;
                            for (int i = 0; i < channels.length; i++)
                                if ((channelmask & (1 << i)) != 0)
                                    channels[i].tuning = tuning;
                            break;
                        }
                        default:
                            break;
                        }
                        break;
                    case 0x09:  // General Midi Message
                        subid2 = data[4] & 0xFF;
                        switch (subid2) {
                        case 0x01:  // General Midi 1 On
                            synth.setGeneralMidiMode(1);
                            reset();
                            break;
                        case 0x02:  // General Midi Off
                            synth.setGeneralMidiMode(0);
                            reset();
                            break;
                        case 0x03:  // General MidI Level 2 On
                            synth.setGeneralMidiMode(2);
                            reset();
                            break;
                        default:
                            break;
                        }
                        break;
                    case 0x0A: // DLS Message
                        subid2 = data[4] & 0xFF;
                        switch (subid2) {
                        case 0x01:  // DLS On
                            if (synth.getGeneralMidiMode() == 0)
                                synth.setGeneralMidiMode(1);
                            synth.voice_allocation_mode = 1;
                            reset();
                            break;
                        case 0x02:  // DLS Off
                            synth.setGeneralMidiMode(0);
                            synth.voice_allocation_mode = 0;
                            reset();
                            break;
                        case 0x03:  // DLS Static Voice Allocation Off
                            synth.voice_allocation_mode = 0;
                            break;
                        case 0x04:  // DLS Static Voice Allocation On
                            synth.voice_allocation_mode = 1;
                            break;
                        default:
                            break;
                        }
                        break;

                    default:
                        break;
                    }
                }
            }

            // Universal Real-Time SysEx
            if ((data[1] & 0xFF) == 0x7F) {
                int deviceID = data[2] & 0xFF;
                if (deviceID == 0x7F || deviceID == synth.getDeviceID()) {
                    int subid1 = data[3] & 0xFF;
                    int subid2;
                    switch (subid1) {
                    case 0x04: // Device Control

                        subid2 = data[4] & 0xFF;
                        switch (subid2) {
                        case 0x01: // Master Volume
                        case 0x02: // Master Balane
                        case 0x03: // Master fine tuning
                        case 0x04: // Master coarse tuning
                            int val = (data[5] & 0x7F)
                                    + ((data[6] & 0x7F) * 128);
                            if (subid2 == 0x01)
                                setVolume(val);
                            else if (subid2 == 0x02)
                                setBalance(val);
                            else if (subid2 == 0x03)
                                setFineTuning(val);
                            else if (subid2 == 0x04)
                                setCoarseTuning(val);
                            break;
                        case 0x05: // Global Parameter Control
                            int ix = 5;
                            int slotPathLen = (data[ix++] & 0xFF);
                            int paramWidth = (data[ix++] & 0xFF);
                            int valueWidth = (data[ix++] & 0xFF);
                            int[] slotPath = new int[slotPathLen];
                            for (int i = 0; i < slotPathLen; i++) {
                                int msb = (data[ix++] & 0xFF);
                                int lsb = (data[ix++] & 0xFF);
                                slotPath[i] = msb * 128 + lsb;
                            }
                            int paramCount = (data.length - 1 - ix)
                                    / (paramWidth + valueWidth);
                            long[] params = new long[paramCount];
                            long[] values = new long[paramCount];
                            for (int i = 0; i < paramCount; i++) {
                                values[i] = 0;
                                for (int j = 0; j < paramWidth; j++)
                                    params[i] = params[i] * 128
                                            + (data[ix++] & 0xFF);
                                for (int j = 0; j < valueWidth; j++)
                                    values[i] = values[i] * 128
                                            + (data[ix++] & 0xFF);

                            }
                            globalParameterControlChange(slotPath, params, values);
                            break;
                        default:
                            break;
                        }
                        break;

                    case 0x08:  // MIDI Tuning Standard
                        subid2 = data[4] & 0xFF;
                        switch (subid2) {
                        case 0x02:  // SINGLE NOTE TUNING CHANGE (REAL-TIME)
                        {
                            // http://www.midi.org/about-midi/tuning.shtml
                            SoftTuning tuning = synth.getTuning(new Patch(0,
                                    data[5] & 0xFF));
                            tuning.load(data);
                            SoftVoice[] voices = synth.getVoices();
                            for (int i = 0; i < voices.length; i++)
                                if (voices[i].active)
                                    if (voices[i].tuning == tuning)
                                        voices[i].updateTuning(tuning);
                            break;
                        }
                        case 0x07:  // SINGLE NOTE TUNING CHANGE (REAL-TIME)
                                    // (BANK)
                        {
                            // http://www.midi.org/about-midi/tuning_extens.shtml
                            SoftTuning tuning = synth.getTuning(new Patch(
                                    data[5] & 0xFF, data[6] & 0xFF));
                            tuning.load(data);
                            SoftVoice[] voices = synth.getVoices();
                            for (int i = 0; i < voices.length; i++)
                                if (voices[i].active)
                                    if (voices[i].tuning == tuning)
                                        voices[i].updateTuning(tuning);
                            break;
                        }
                        case 0x08:  // scale/octave tuning 1-byte form
                                    //(Real-Time)
                        case 0x09:  // scale/octave tuning 2-byte form
                                    // (Real-Time)
                        {
                            // http://www.midi.org/about-midi/tuning-scale.shtml
                            SoftTuning tuning = new SoftTuning(data);
                            int channelmask = (data[5] & 0xFF) * 16384
                                    + (data[6] & 0xFF) * 128 + (data[7] & 0xFF);
                            SoftChannel[] channels = synth.channels;
                            for (int i = 0; i < channels.length; i++)
                                if ((channelmask & (1 << i)) != 0)
                                    channels[i].tuning = tuning;
                            SoftVoice[] voices = synth.getVoices();
                            for (int i = 0; i < voices.length; i++)
                                if (voices[i].active)
                                    if ((channelmask & (1 << (voices[i].channel))) != 0)
                                        voices[i].updateTuning(tuning);
                            break;
                        }
                        default:
                            break;
                        }
                        break;
                    case 0x09:  // Control Destination Settings
                        subid2 = data[4] & 0xFF;
                        switch (subid2) {
                        case 0x01: // Channel Pressure
                        {
                            int[] destinations = new int[(data.length - 7) / 2];
                            int[] ranges = new int[(data.length - 7) / 2];
                            int ix = 0;
                            for (int j = 6; j < data.length - 1; j += 2) {
                                destinations[ix] = data[j] & 0xFF;
                                ranges[ix] = data[j + 1] & 0xFF;
                                ix++;
                            }
                            int channel = data[5] & 0xFF;
                            SoftChannel softchannel = synth.channels[channel];
                            softchannel.mapChannelPressureToDestination(
                                    destinations, ranges);
                            break;
                        }
                        case 0x02: // Poly Pressure
                        {
                            int[] destinations = new int[(data.length - 7) / 2];
                            int[] ranges = new int[(data.length - 7) / 2];
                            int ix = 0;
                            for (int j = 6; j < data.length - 1; j += 2) {
                                destinations[ix] = data[j] & 0xFF;
                                ranges[ix] = data[j + 1] & 0xFF;
                                ix++;
                            }
                            int channel = data[5] & 0xFF;
                            SoftChannel softchannel = synth.channels[channel];
                            softchannel.mapPolyPressureToDestination(
                                    destinations, ranges);
                            break;
                        }
                        case 0x03: // Control Change
                        {
                            int[] destinations = new int[(data.length - 7) / 2];
                            int[] ranges = new int[(data.length - 7) / 2];
                            int ix = 0;
                            for (int j = 7; j < data.length - 1; j += 2) {
                                destinations[ix] = data[j] & 0xFF;
                                ranges[ix] = data[j + 1] & 0xFF;
                                ix++;
                            }
                            int channel = data[5] & 0xFF;
                            SoftChannel softchannel = synth.channels[channel];
                            int control = data[6] & 0xFF;
                            softchannel.mapControlToDestination(control,
                                    destinations, ranges);
                            break;
                        }
                        default:
                            break;
                        }
                        break;

                    case 0x0A:  // Key Based Instrument Control
                    {
                        subid2 = data[4] & 0xFF;
                        switch (subid2) {
                        case 0x01: // Basic Message
                            int channel = data[5] & 0xFF;
                            int keynumber = data[6] & 0xFF;
                            SoftChannel softchannel = synth.channels[channel];
                            for (int j = 7; j < data.length - 1; j += 2) {
                                int controlnumber = data[j] & 0xFF;
                                int controlvalue = data[j + 1] & 0xFF;
                                softchannel.controlChangePerNote(keynumber,
                                        controlnumber, controlvalue);
                            }
                            break;
                        default:
                            break;
                        }
                        break;
                    }
                    default:
                        break;
                    }
                }
            }

        }
    }

    private void processMessages(long timeStamp) {
        Iterator<Entry<Long, Object>> iter = midimessages.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<Long, Object> entry = iter.next();
            if (entry.getKey() >= (timeStamp + msec_buffer_len))
                return;
            long msec_delay = entry.getKey() - timeStamp;
            delay_midievent = (int)(msec_delay * (samplerate / 1000000.0) + 0.5);
            if(delay_midievent > max_delay_midievent)
                delay_midievent = max_delay_midievent;
            if(delay_midievent < 0)
                delay_midievent = 0;
            processMessage(entry.getValue());
            iter.remove();
        }
        delay_midievent = 0;
    }

    void processAudioBuffers() {

        if(synth.weakstream != null && synth.weakstream.silent_samples != 0)
        {
            sample_pos += synth.weakstream.silent_samples;
            synth.weakstream.silent_samples = 0;
        }

        for (int i = 0; i < buffers.length; i++) {
            if(i != CHANNEL_DELAY_LEFT &&
                    i != CHANNEL_DELAY_RIGHT &&
                    i != CHANNEL_DELAY_MONO &&
                    i != CHANNEL_DELAY_EFFECT1 &&
                    i != CHANNEL_DELAY_EFFECT2)
                buffers[i].clear();
        }

        if(!buffers[CHANNEL_DELAY_LEFT].isSilent())
        {
            buffers[CHANNEL_LEFT].swap(buffers[CHANNEL_DELAY_LEFT]);
        }
        if(!buffers[CHANNEL_DELAY_RIGHT].isSilent())
        {
            buffers[CHANNEL_RIGHT].swap(buffers[CHANNEL_DELAY_RIGHT]);
        }
        if(!buffers[CHANNEL_DELAY_MONO].isSilent())
        {
            buffers[CHANNEL_MONO].swap(buffers[CHANNEL_DELAY_MONO]);
        }
        if(!buffers[CHANNEL_DELAY_EFFECT1].isSilent())
        {
            buffers[CHANNEL_EFFECT1].swap(buffers[CHANNEL_DELAY_EFFECT1]);
        }
        if(!buffers[CHANNEL_DELAY_EFFECT2].isSilent())
        {
            buffers[CHANNEL_EFFECT2].swap(buffers[CHANNEL_DELAY_EFFECT2]);
        }

        double volume_left;
        double volume_right;

        SoftChannelMixerContainer[] act_registeredMixers;

        // perform control logic
        synchronized (control_mutex) {

            long msec_pos = (long)(sample_pos * (1000000.0 / samplerate));

            processMessages(msec_pos);

            if (active_sensing_on) {
                // Active Sensing
                // if no message occurs for max 1000 ms
                // then do AllSoundOff on all channels
                if ((msec_pos - msec_last_activity) > 1000000) {
                    active_sensing_on = false;
                    for (SoftChannel c : synth.channels)
                        c.allSoundOff();
                }

            }

            for (int i = 0; i < voicestatus.length; i++)
                if (voicestatus[i].active)
                    voicestatus[i].processControlLogic();
            sample_pos += buffer_len;

            double volume = co_master_volume[0];
            volume_left = volume;
            volume_right = volume;

            double balance = co_master_balance[0];
            if (balance > 0.5)
                volume_left *= (1 - balance) * 2;
            else
                volume_right *= balance * 2;

            chorus.processControlLogic();
            reverb.processControlLogic();
            agc.processControlLogic();

            if (cur_registeredMixers == null) {
                if (registeredMixers != null) {
                    cur_registeredMixers =
                            new SoftChannelMixerContainer[registeredMixers.size()];
                    registeredMixers.toArray(cur_registeredMixers);
                }
            }

            act_registeredMixers = cur_registeredMixers;
            if (act_registeredMixers != null)
                if (act_registeredMixers.length == 0)
                    act_registeredMixers = null;

        }

        if (act_registeredMixers != null) {

            // Make backup of left,right,mono channels
            SoftAudioBuffer leftbak = buffers[CHANNEL_LEFT];
            SoftAudioBuffer rightbak = buffers[CHANNEL_RIGHT];
            SoftAudioBuffer monobak = buffers[CHANNEL_MONO];
            SoftAudioBuffer delayleftbak = buffers[CHANNEL_DELAY_LEFT];
            SoftAudioBuffer delayrightbak = buffers[CHANNEL_DELAY_RIGHT];
            SoftAudioBuffer delaymonobak = buffers[CHANNEL_DELAY_MONO];

            int bufferlen = buffers[CHANNEL_LEFT].getSize();

            float[][] cbuffer = new float[nrofchannels][];
            float[][] obuffer = new float[nrofchannels][];
            obuffer[0] = leftbak.array();
            if (nrofchannels != 1)
                obuffer[1] = rightbak.array();

            for (SoftChannelMixerContainer cmixer : act_registeredMixers) {

                // Reroute default left,right output
                // to channelmixer left,right input/output
                buffers[CHANNEL_LEFT] =  cmixer.buffers[CHANNEL_LEFT];
                buffers[CHANNEL_RIGHT] = cmixer.buffers[CHANNEL_RIGHT];
                buffers[CHANNEL_MONO] = cmixer.buffers[CHANNEL_MONO];
                buffers[CHANNEL_DELAY_LEFT] = cmixer.buffers[CHANNEL_DELAY_LEFT];
                buffers[CHANNEL_DELAY_RIGHT] = cmixer.buffers[CHANNEL_DELAY_RIGHT];
                buffers[CHANNEL_DELAY_MONO] = cmixer.buffers[CHANNEL_DELAY_MONO];

                buffers[CHANNEL_LEFT].clear();
                buffers[CHANNEL_RIGHT].clear();
                buffers[CHANNEL_MONO].clear();

                if(!buffers[CHANNEL_DELAY_LEFT].isSilent())
                {
                    buffers[CHANNEL_LEFT].swap(buffers[CHANNEL_DELAY_LEFT]);
                }
                if(!buffers[CHANNEL_DELAY_RIGHT].isSilent())
                {
                    buffers[CHANNEL_RIGHT].swap(buffers[CHANNEL_DELAY_RIGHT]);
                }
                if(!buffers[CHANNEL_DELAY_MONO].isSilent())
                {
                    buffers[CHANNEL_MONO].swap(buffers[CHANNEL_DELAY_MONO]);
                }

                cbuffer[0] = buffers[CHANNEL_LEFT].array();
                if (nrofchannels != 1)
                    cbuffer[1] = buffers[CHANNEL_RIGHT].array();

                boolean hasactivevoices = false;
                for (int i = 0; i < voicestatus.length; i++)
                    if (voicestatus[i].active)
                        if (voicestatus[i].channelmixer == cmixer.mixer) {
                            voicestatus[i].processAudioLogic(buffers);
                            hasactivevoices = true;
                        }

                if(!buffers[CHANNEL_MONO].isSilent())
                {
                    float[] mono = buffers[CHANNEL_MONO].array();
                    float[] left = buffers[CHANNEL_LEFT].array();
                    if (nrofchannels != 1) {
                        float[] right = buffers[CHANNEL_RIGHT].array();
                        for (int i = 0; i < bufferlen; i++) {
                            float v = mono[i];
                            left[i] += v;
                            right[i] += v;
                        }
                    }
                    else
                    {
                        for (int i = 0; i < bufferlen; i++) {
                            left[i] += mono[i];
                        }
                    }
                }

                if (!cmixer.mixer.process(cbuffer, 0, bufferlen)) {
                    synchronized (control_mutex) {
                        registeredMixers.remove(cmixer);
                        cur_registeredMixers = null;
                    }
                }

                for (int i = 0; i < cbuffer.length; i++) {
                    float[] cbuff = cbuffer[i];
                    float[] obuff = obuffer[i];
                    for (int j = 0; j < bufferlen; j++)
                        obuff[j] += cbuff[j];
                }

                if (!hasactivevoices) {
                    synchronized (control_mutex) {
                        if (stoppedMixers != null) {
                            if (stoppedMixers.contains(cmixer.mixer)) {
                                stoppedMixers.remove(cmixer.mixer);
                                cmixer.mixer.stop();
                            }
                        }
                    }
                }

            }

            buffers[CHANNEL_LEFT] = leftbak;
            buffers[CHANNEL_RIGHT] = rightbak;
            buffers[CHANNEL_MONO] = monobak;
            buffers[CHANNEL_DELAY_LEFT] = delayleftbak;
            buffers[CHANNEL_DELAY_RIGHT] = delayrightbak;
            buffers[CHANNEL_DELAY_MONO] = delaymonobak;

        }

        for (int i = 0; i < voicestatus.length; i++)
            if (voicestatus[i].active)
                if (voicestatus[i].channelmixer == null)
                    voicestatus[i].processAudioLogic(buffers);

        if(!buffers[CHANNEL_MONO].isSilent())
        {
            float[] mono = buffers[CHANNEL_MONO].array();
            float[] left = buffers[CHANNEL_LEFT].array();
            int bufferlen = buffers[CHANNEL_LEFT].getSize();
            if (nrofchannels != 1) {
                float[] right = buffers[CHANNEL_RIGHT].array();
                for (int i = 0; i < bufferlen; i++) {
                    float v = mono[i];
                    left[i] += v;
                    right[i] += v;
                }
            }
            else
            {
                for (int i = 0; i < bufferlen; i++) {
                    left[i] += mono[i];
                }
            }
        }

        // Run effects
        if (synth.chorus_on)
            chorus.processAudio();

        if (synth.reverb_on)
            reverb.processAudio();

        if (nrofchannels == 1)
            volume_left = (volume_left + volume_right) / 2;

        // Set Volume / Balance
        if (last_volume_left != volume_left || last_volume_right != volume_right) {
            float[] left = buffers[CHANNEL_LEFT].array();
            float[] right = buffers[CHANNEL_RIGHT].array();
            int bufferlen = buffers[CHANNEL_LEFT].getSize();

            float amp;
            float amp_delta;
            amp = (float)(last_volume_left * last_volume_left);
            amp_delta = (float)((volume_left * volume_left - amp) / bufferlen);
            for (int i = 0; i < bufferlen; i++) {
                amp += amp_delta;
                left[i] *= amp;
            }
            if (nrofchannels != 1) {
                amp = (float)(last_volume_right * last_volume_right);
                amp_delta = (float)((volume_right*volume_right - amp) / bufferlen);
                for (int i = 0; i < bufferlen; i++) {
                    amp += amp_delta;
                    right[i] *= volume_right;
                }
            }
            last_volume_left = volume_left;
            last_volume_right = volume_right;

        } else {
            if (volume_left != 1.0 || volume_right != 1.0) {
                float[] left = buffers[CHANNEL_LEFT].array();
                float[] right = buffers[CHANNEL_RIGHT].array();
                int bufferlen = buffers[CHANNEL_LEFT].getSize();
                float amp;
                amp = (float) (volume_left * volume_left);
                for (int i = 0; i < bufferlen; i++)
                    left[i] *= amp;
                if (nrofchannels != 1) {
                    amp = (float)(volume_right * volume_right);
                    for (int i = 0; i < bufferlen; i++)
                        right[i] *= amp;
                }

            }
        }

        if(buffers[CHANNEL_LEFT].isSilent()
            && buffers[CHANNEL_RIGHT].isSilent())
        {

            int midimessages_size;
            synchronized (control_mutex) {
                midimessages_size = midimessages.size();
            }

            if(midimessages_size == 0)
            {
                pusher_silent_count++;
                if(pusher_silent_count > 5)
                {
                    pusher_silent_count = 0;
                    synchronized (control_mutex) {
                        pusher_silent = true;
                        if(synth.weakstream != null)
                            synth.weakstream.setInputStream(null);
                    }
                }
            }
        }
        else
            pusher_silent_count = 0;

        if (synth.agc_on)
            agc.processAudio();

    }

    // Must only we called within control_mutex synchronization
    public void activity()
    {
        long silent_samples = 0;
        if(pusher_silent)
        {
            pusher_silent = false;
            if(synth.weakstream != null)
            {
                synth.weakstream.setInputStream(ais);
                silent_samples = synth.weakstream.silent_samples;
            }
        }
        msec_last_activity = (long)((sample_pos + silent_samples)
                * (1000000.0 / samplerate));
    }

    public void stopMixer(ModelChannelMixer mixer) {
        if (stoppedMixers == null)
            stoppedMixers = new HashSet<>();
        stoppedMixers.add(mixer);
    }

    public void registerMixer(ModelChannelMixer mixer) {
        if (registeredMixers == null)
            registeredMixers = new HashSet<>();
        SoftChannelMixerContainer mixercontainer = new SoftChannelMixerContainer();
        mixercontainer.buffers = new SoftAudioBuffer[6];
        for (int i = 0; i < mixercontainer.buffers.length; i++) {
            mixercontainer.buffers[i] =
                new SoftAudioBuffer(buffer_len, synth.getFormat());
        }
        mixercontainer.mixer = mixer;
        registeredMixers.add(mixercontainer);
        cur_registeredMixers = null;
    }

    public SoftMainMixer(SoftSynthesizer synth) {
        this.synth = synth;

        sample_pos = 0;

        co_master_balance[0] = 0.5;
        co_master_volume[0] = 1;
        co_master_coarse_tuning[0] = 0.5;
        co_master_fine_tuning[0] = 0.5;

        msec_buffer_len = (long) (1000000.0 / synth.getControlRate());
        samplerate = synth.getFormat().getSampleRate();
        nrofchannels = synth.getFormat().getChannels();

        int buffersize = (int) (synth.getFormat().getSampleRate()
                                / synth.getControlRate());

        buffer_len = buffersize;

        max_delay_midievent = buffersize;

        control_mutex = synth.control_mutex;
        buffers = new SoftAudioBuffer[14];
        for (int i = 0; i < buffers.length; i++) {
            buffers[i] = new SoftAudioBuffer(buffersize, synth.getFormat());
        }
        voicestatus = synth.getVoices();

        reverb = new SoftReverb();
        chorus = new SoftChorus();
        agc = new SoftLimiter();

        float samplerate = synth.getFormat().getSampleRate();
        float controlrate = synth.getControlRate();
        reverb.init(samplerate, controlrate);
        chorus.init(samplerate, controlrate);
        agc.init(samplerate, controlrate);

        reverb.setLightMode(synth.reverb_light);

        reverb.setMixMode(true);
        chorus.setMixMode(true);
        agc.setMixMode(false);

        chorus.setInput(0, buffers[CHANNEL_EFFECT2]);
        chorus.setOutput(0, buffers[CHANNEL_LEFT]);
        if (nrofchannels != 1)
            chorus.setOutput(1, buffers[CHANNEL_RIGHT]);
        chorus.setOutput(2, buffers[CHANNEL_EFFECT1]);

        reverb.setInput(0, buffers[CHANNEL_EFFECT1]);
        reverb.setOutput(0, buffers[CHANNEL_LEFT]);
        if (nrofchannels != 1)
            reverb.setOutput(1, buffers[CHANNEL_RIGHT]);

        agc.setInput(0, buffers[CHANNEL_LEFT]);
        if (nrofchannels != 1)
            agc.setInput(1, buffers[CHANNEL_RIGHT]);
        agc.setOutput(0, buffers[CHANNEL_LEFT]);
        if (nrofchannels != 1)
            agc.setOutput(1, buffers[CHANNEL_RIGHT]);

        InputStream in = new InputStream() {

            private final SoftAudioBuffer[] buffers = SoftMainMixer.this.buffers;
            private final int nrofchannels
                    = SoftMainMixer.this.synth.getFormat().getChannels();
            private final int buffersize = buffers[0].getSize();
            private final byte[] bbuffer = new byte[buffersize
                    * (SoftMainMixer.this.synth.getFormat()
                        .getSampleSizeInBits() / 8)
                    * nrofchannels];
            private int bbuffer_pos = 0;
            private final byte[] single = new byte[1];

            public void fillBuffer() {
                /*
                boolean pusher_silent2;
                synchronized (control_mutex) {
                    pusher_silent2 = pusher_silent;
                }
                if(!pusher_silent2)*/
                processAudioBuffers();
                for (int i = 0; i < nrofchannels; i++)
                    buffers[i].get(bbuffer, i);
                bbuffer_pos = 0;
            }

            @Override
            public int read(byte[] b, int off, int len) {
                int bbuffer_len = bbuffer.length;
                int offlen = off + len;
                int orgoff = off;
                byte[] bbuffer = this.bbuffer;
                while (off < offlen) {
                    if (available() == 0)
                        fillBuffer();
                    else {
                        int bbuffer_pos = this.bbuffer_pos;
                        while (off < offlen && bbuffer_pos < bbuffer_len)
                            b[off++] = bbuffer[bbuffer_pos++];
                        this.bbuffer_pos = bbuffer_pos;
                        if (!readfully)
                            return off - orgoff;
                    }
                }
                return len;
            }

            @Override
            public int read() throws IOException {
                int ret = read(single);
                if (ret == -1)
                    return -1;
                return single[0] & 0xFF;
            }

            @Override
            public int available() {
                return bbuffer.length - bbuffer_pos;
            }

            @Override
            public void close() {
                SoftMainMixer.this.synth.close();
            }
        };

        ais = new AudioInputStream(in, synth.getFormat(), AudioSystem.NOT_SPECIFIED);

    }

    public AudioInputStream getInputStream() {
        return ais;
    }

    public void reset() {

        SoftChannel[] channels = synth.channels;
        for (int i = 0; i < channels.length; i++) {
            channels[i].allSoundOff();
            channels[i].resetAllControllers(true);

            if (synth.getGeneralMidiMode() == 2) {
                if (i == 9)
                    channels[i].programChange(0, 0x78 * 128);
                else
                    channels[i].programChange(0, 0x79 * 128);
            } else
                channels[i].programChange(0, 0);
        }
        setVolume(0x7F * 128 + 0x7F);
        setBalance(0x40 * 128 + 0x00);
        setCoarseTuning(0x40 * 128 + 0x00);
        setFineTuning(0x40 * 128 + 0x00);
        // Reset Reverb
        globalParameterControlChange(
                new int[]{0x01 * 128 + 0x01}, new long[]{0}, new long[]{4});
        // Reset Chorus
        globalParameterControlChange(
                new int[]{0x01 * 128 + 0x02}, new long[]{0}, new long[]{2});
    }

    public void setVolume(int value) {
        synchronized (control_mutex) {
            co_master_volume[0] = value / 16384.0;
        }
    }

    public void setBalance(int value) {
        synchronized (control_mutex) {
            co_master_balance[0] = value / 16384.0;
        }
    }

    public void setFineTuning(int value) {
        synchronized (control_mutex) {
            co_master_fine_tuning[0] = value / 16384.0;
        }
    }

    public void setCoarseTuning(int value) {
        synchronized (control_mutex) {
            co_master_coarse_tuning[0] = value / 16384.0;
        }
    }

    public int getVolume() {
        synchronized (control_mutex) {
            return (int) (co_master_volume[0] * 16384.0);
        }
    }

    public int getBalance() {
        synchronized (control_mutex) {
            return (int) (co_master_balance[0] * 16384.0);
        }
    }

    public int getFineTuning() {
        synchronized (control_mutex) {
            return (int) (co_master_fine_tuning[0] * 16384.0);
        }
    }

    public int getCoarseTuning() {
        synchronized (control_mutex) {
            return (int) (co_master_coarse_tuning[0] * 16384.0);
        }
    }

    public void globalParameterControlChange(int[] slothpath, long[] params,
            long[] paramsvalue) {
        if (slothpath.length == 0)
            return;

        synchronized (control_mutex) {

            // slothpath: 01xx are reserved only for GM2

            if (slothpath[0] == 0x01 * 128 + 0x01) {
                for (int i = 0; i < paramsvalue.length; i++) {
                    reverb.globalParameterControlChange(slothpath, params[i],
                            paramsvalue[i]);
                }
            }
            if (slothpath[0] == 0x01 * 128 + 0x02) {
                for (int i = 0; i < paramsvalue.length; i++) {
                    chorus.globalParameterControlChange(slothpath, params[i],
                            paramsvalue[i]);
                }

            }

        }
    }

    public void processMessage(Object object) {
        if (object instanceof byte[])
            processMessage((byte[]) object);
        if (object instanceof MidiMessage)
            processMessage((MidiMessage)object);
    }

    public void processMessage(MidiMessage message) {
        if (message instanceof ShortMessage) {
            ShortMessage sms = (ShortMessage)message;
            processMessage(sms.getChannel(), sms.getCommand(),
                    sms.getData1(), sms.getData2());
            return;
        }
        processMessage(message.getMessage());
    }

    public void processMessage(byte[] data) {
        int status = 0;
        if (data.length > 0)
            status = data[0] & 0xFF;

        if (status == 0xF0) {
            processSystemExclusiveMessage(data);
            return;
        }

        int cmd = (status & 0xF0);
        int ch = (status & 0x0F);

        int data1;
        int data2;
        if (data.length > 1)
            data1 = data[1] & 0xFF;
        else
            data1 = 0;
        if (data.length > 2)
            data2 = data[2] & 0xFF;
        else
            data2 = 0;

        processMessage(ch, cmd, data1, data2);

    }

    public void processMessage(int ch, int cmd, int data1, int data2) {
        synchronized (synth.control_mutex) {
            activity();
        }

        if (cmd == 0xF0) {
            int status = cmd | ch;
            switch (status) {
            case ShortMessage.ACTIVE_SENSING:
                synchronized (synth.control_mutex) {
                    active_sensing_on = true;
                }
                break;
            default:
                break;
            }
            return;
        }

        SoftChannel[] channels = synth.channels;
        if (ch >= channels.length)
            return;
        SoftChannel softchannel = channels[ch];

        switch (cmd) {
        case ShortMessage.NOTE_ON:
            if(delay_midievent != 0)
                softchannel.noteOn(data1, data2, delay_midievent);
            else
                softchannel.noteOn(data1, data2);
            break;
        case ShortMessage.NOTE_OFF:
            softchannel.noteOff(data1, data2);
            break;
        case ShortMessage.POLY_PRESSURE:
            softchannel.setPolyPressure(data1, data2);
            break;
        case ShortMessage.CONTROL_CHANGE:
            softchannel.controlChange(data1, data2);
            break;
        case ShortMessage.PROGRAM_CHANGE:
            softchannel.programChange(data1);
            break;
        case ShortMessage.CHANNEL_PRESSURE:
            softchannel.setChannelPressure(data1);
            break;
        case ShortMessage.PITCH_BEND:
            softchannel.setPitchBend(data1 + data2 * 128);
            break;
        default:
            break;
        }

    }

    public long getMicrosecondPosition() {
        if(pusher_silent)
        {
            if(synth.weakstream != null)
            {
                return (long)((sample_pos  + synth.weakstream.silent_samples)
                        * (1000000.0 / samplerate));
            }
        }
        return (long)(sample_pos * (1000000.0 / samplerate));
    }

    public void close() {
    }
}
