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

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.sound.midi.VoiceStatus;

/**
 * Software synthesizer voice class.
 *
 * @author Karl Helgason
 */
public final class SoftVoice extends VoiceStatus {

    public int exclusiveClass = 0;
    public boolean releaseTriggered = false;
    private int noteOn_noteNumber = 0;
    private int noteOn_velocity = 0;
    private int noteOff_velocity = 0;
    private int delay = 0;
    ModelChannelMixer channelmixer = null;
    double tunedKey = 0;
    SoftTuning tuning = null;
    SoftChannel stealer_channel = null;
    ModelConnectionBlock[] stealer_extendedConnectionBlocks = null;
    SoftPerformer stealer_performer = null;
    ModelChannelMixer stealer_channelmixer = null;
    int stealer_voiceID = -1;
    int stealer_noteNumber = 0;
    int stealer_velocity = 0;
    boolean stealer_releaseTriggered = false;
    int voiceID = -1;
    boolean sustain = false;
    boolean sostenuto = false;
    boolean portamento = false;
    private final SoftFilter filter_left;
    private final SoftFilter filter_right;
    private final SoftProcess eg = new SoftEnvelopeGenerator();
    private final SoftProcess lfo = new SoftLowFrequencyOscillator();
    final Map<String, SoftControl> objects = new HashMap<>();
    SoftSynthesizer synthesizer;
    SoftInstrument instrument;
    SoftPerformer performer;
    SoftChannel softchannel = null;
    boolean on = false;
    private boolean audiostarted = false;
    private boolean started = false;
    private boolean stopping = false;
    private float osc_attenuation = 0.0f;
    private ModelOscillatorStream osc_stream;
    private int osc_stream_nrofchannels;
    private float[][] osc_buff = new float[2][];
    private boolean osc_stream_off_transmitted = false;
    private boolean out_mixer_end = false;
    private float out_mixer_left = 0;
    private float out_mixer_right = 0;
    private float out_mixer_effect1 = 0;
    private float out_mixer_effect2 = 0;
    private float last_out_mixer_left = 0;
    private float last_out_mixer_right = 0;
    private float last_out_mixer_effect1 = 0;
    private float last_out_mixer_effect2 = 0;
    ModelConnectionBlock[] extendedConnectionBlocks = null;
    private ModelConnectionBlock[] connections;
    // Last value added to destination
    private double[] connections_last = new double[50];
    // Pointer to source value
    private double[][][] connections_src = new double[50][3][];
    // Key-based override (if any)
    private int[][] connections_src_kc = new int[50][3];
    // Pointer to destination value
    private double[][] connections_dst = new double[50][];
    private boolean soundoff = false;
    private float lastMuteValue = 0;
    private float lastSoloMuteValue = 0;
    double[] co_noteon_keynumber = new double[1];
    double[] co_noteon_velocity = new double[1];
    double[] co_noteon_on = new double[1];
    private final SoftControl co_noteon = new SoftControl() {
        double[] keynumber = co_noteon_keynumber;
        double[] velocity = co_noteon_velocity;
        double[] on = co_noteon_on;
        @Override
        public double[] get(int instance, String name) {
            if (name == null)
                return null;
            if (name.equals("keynumber"))
                return keynumber;
            if (name.equals("velocity"))
                return velocity;
            if (name.equals("on"))
                return on;
            return null;
        }
    };
    private final double[] co_mixer_active = new double[1];
    private final double[] co_mixer_gain = new double[1];
    private final double[] co_mixer_pan = new double[1];
    private final double[] co_mixer_balance = new double[1];
    private final double[] co_mixer_reverb = new double[1];
    private final double[] co_mixer_chorus = new double[1];
    private final SoftControl co_mixer = new SoftControl() {
        final double[] active = co_mixer_active;
        final double[] gain = co_mixer_gain;
        final double[] pan = co_mixer_pan;
        final double[] balance = co_mixer_balance;
        final double[] reverb = co_mixer_reverb;
        final double[] chorus = co_mixer_chorus;
        @Override
        public double[] get(int instance, String name) {
            if (name == null)
                return null;
            if (name.equals("active"))
                return active;
            if (name.equals("gain"))
                return gain;
            if (name.equals("pan"))
                return pan;
            if (name.equals("balance"))
                return balance;
            if (name.equals("reverb"))
                return reverb;
            if (name.equals("chorus"))
                return chorus;
            return null;
        }
    };
    private final double[] co_osc_pitch = new double[1];
    private final SoftControl co_osc = new SoftControl() {
        final double[] pitch = co_osc_pitch;
        @Override
        public double[] get(int instance, String name) {
            if (name == null)
                return null;
            if (name.equals("pitch"))
                return pitch;
            return null;
        }
    };
    private final double[] co_filter_freq = new double[1];
    private final double[] co_filter_type = new double[1];
    private final double[] co_filter_q = new double[1];
    private final SoftControl co_filter = new SoftControl() {
        final double[] freq = co_filter_freq;
        final double[] ftype = co_filter_type;
        final double[] q = co_filter_q;
        @Override
        public double[] get(int instance, String name) {
            if (name == null)
                return null;
            if (name.equals("freq"))
                return freq;
            if (name.equals("type"))
                return ftype;
            if (name.equals("q"))
                return q;
            return null;
        }
    };
    SoftResamplerStreamer resampler;
    private final int nrofchannels;

    public SoftVoice(SoftSynthesizer synth) {
        synthesizer = synth;
        filter_left = new SoftFilter(synth.getFormat().getSampleRate());
        filter_right = new SoftFilter(synth.getFormat().getSampleRate());
        nrofchannels = synth.getFormat().getChannels();
    }

    private int getValueKC(ModelIdentifier id) {
        if (id.getObject().equals("midi_cc")) {
            int ic = Integer.parseInt(id.getVariable());
            if (ic != 0 && ic != 32) {
                if (ic < 120)
                    return ic;
            }
        } else if (id.getObject().equals("midi_rpn")) {
            if (id.getVariable().equals("1"))
                return 120; // Fine tuning
            if (id.getVariable().equals("2"))
                return 121; // Coarse tuning
        }
        return -1;
    }

    private double[] getValue(ModelIdentifier id) {
        SoftControl o = objects.get(id.getObject());
        if (o == null)
            return null;
        return o.get(id.getInstance(), id.getVariable());
    }

    private double transformValue(double value, ModelSource src) {
        if (src.getTransform() != null)
            return src.getTransform().transform(value);
        else
            return value;
    }

    private double transformValue(double value, ModelDestination dst) {
        if (dst.getTransform() != null)
            return dst.getTransform().transform(value);
        else
            return value;
    }

    private double processKeyBasedController(double value, int keycontrol) {
        if (keycontrol == -1)
            return value;
        if (softchannel.keybasedcontroller_active != null)
            if (softchannel.keybasedcontroller_active[note] != null)
                if (softchannel.keybasedcontroller_active[note][keycontrol]) {
                    double key_controlvalue =
                            softchannel.keybasedcontroller_value[note][keycontrol];
                    if (keycontrol == 10 || keycontrol == 91 || keycontrol == 93)
                        return key_controlvalue;
                    value += key_controlvalue * 2.0 - 1.0;
                    if (value > 1)
                        value = 1;
                    else if (value < 0)
                        value = 0;
                }
        return value;
    }

    private void processConnection(int ix) {
        ModelConnectionBlock conn = connections[ix];
        double[][] src = connections_src[ix];
        double[] dst = connections_dst[ix];
        if (dst == null || Double.isInfinite(dst[0]))
            return;

        double value = conn.getScale();
        if (softchannel.keybasedcontroller_active == null) {
            ModelSource[] srcs = conn.getSources();
            for (int i = 0; i < srcs.length; i++) {
                value *= transformValue(src[i][0], srcs[i]);
                if (value == 0)
                    break;
            }
        } else {
            ModelSource[] srcs = conn.getSources();
            int[] src_kc = connections_src_kc[ix];
            for (int i = 0; i < srcs.length; i++) {
                value *= transformValue(processKeyBasedController(src[i][0],
                        src_kc[i]), srcs[i]);
                if (value == 0)
                    break;
            }
        }

        value = transformValue(value, conn.getDestination());
        dst[0] = dst[0] - connections_last[ix] + value;
        connections_last[ix] = value;
        // co_mixer_gain[0] = 0;
    }

    void updateTuning(SoftTuning newtuning) {
        tuning = newtuning;
        tunedKey = tuning.getTuning(note) / 100.0;
        if (!portamento) {
            co_noteon_keynumber[0] = tunedKey * (1.0 / 128.0);
            if(performer == null)
                return;
            int[] c = performer.midi_connections[4];
            if (c == null)
                return;
            for (int i = 0; i < c.length; i++)
                processConnection(c[i]);
        }
    }

    void setNote(int noteNumber) {
        note = noteNumber;
        tunedKey = tuning.getTuning(noteNumber) / 100.0;
    }

    void noteOn(int noteNumber, int velocity, int delay) {

        sustain = false;
        sostenuto = false;
        portamento = false;

        soundoff = false;
        on = true;
        active = true;
        started = true;
        // volume = velocity;

        noteOn_noteNumber = noteNumber;
        noteOn_velocity = velocity;
        this.delay = delay;

        lastMuteValue = 0;
        lastSoloMuteValue = 0;

        setNote(noteNumber);

        if (performer.forcedKeynumber)
            co_noteon_keynumber[0] = 0;
        else
            co_noteon_keynumber[0] = tunedKey * (1f / 128f);
        if (performer.forcedVelocity)
            co_noteon_velocity[0] = 0;
        else
            co_noteon_velocity[0] = velocity * (1f / 128f);
        co_mixer_active[0] = 0;
        co_mixer_gain[0] = 0;
        co_mixer_pan[0] = 0;
        co_mixer_balance[0] = 0;
        co_mixer_reverb[0] = 0;
        co_mixer_chorus[0] = 0;
        co_osc_pitch[0] = 0;
        co_filter_freq[0] = 0;
        co_filter_q[0] = 0;
        co_filter_type[0] = 0;
        co_noteon_on[0] = 1;

        eg.reset();
        lfo.reset();
        filter_left.reset();
        filter_right.reset();

        objects.put("master", synthesizer.getMainMixer().co_master);
        objects.put("eg", eg);
        objects.put("lfo", lfo);
        objects.put("noteon", co_noteon);
        objects.put("osc", co_osc);
        objects.put("mixer", co_mixer);
        objects.put("filter", co_filter);

        connections = performer.connections;

        if (connections_last == null
                || connections_last.length < connections.length) {
            connections_last = new double[connections.length];
        }
        if (connections_src == null
                || connections_src.length < connections.length) {
            connections_src = new double[connections.length][][];
            connections_src_kc = new int[connections.length][];
        }
        if (connections_dst == null
                || connections_dst.length < connections.length) {
            connections_dst = new double[connections.length][];
        }
        for (int i = 0; i < connections.length; i++) {
            ModelConnectionBlock conn = connections[i];
            connections_last[i] = 0;
            if (conn.getSources() != null) {
                ModelSource[] srcs = conn.getSources();
                if (connections_src[i] == null
                        || connections_src[i].length < srcs.length) {
                    connections_src[i] = new double[srcs.length][];
                    connections_src_kc[i] = new int[srcs.length];
                }
                double[][] src = connections_src[i];
                int[] src_kc = connections_src_kc[i];
                connections_src[i] = src;
                for (int j = 0; j < srcs.length; j++) {
                    src_kc[j] = getValueKC(srcs[j].getIdentifier());
                    src[j] = getValue(srcs[j].getIdentifier());
                }
            }

            if (conn.getDestination() != null)
                connections_dst[i] = getValue(conn.getDestination()
                        .getIdentifier());
            else
                connections_dst[i] = null;
        }

        for (int i = 0; i < connections.length; i++)
            processConnection(i);

        if (extendedConnectionBlocks != null) {
            for (ModelConnectionBlock connection: extendedConnectionBlocks) {
                double value = 0;

                if (softchannel.keybasedcontroller_active == null) {
                    for (ModelSource src: connection.getSources()) {
                        double x = getValue(src.getIdentifier())[0];
                        ModelTransform t = src.getTransform();
                        if (t == null)
                            value += x;
                        else
                            value += t.transform(x);
                    }
                } else {
                    for (ModelSource src: connection.getSources()) {
                        double x = getValue(src.getIdentifier())[0];
                        x = processKeyBasedController(x,
                                getValueKC(src.getIdentifier()));
                        ModelTransform t = src.getTransform();
                        if (t == null)
                            value += x;
                        else
                            value += t.transform(x);
                    }
                }

                ModelDestination dest = connection.getDestination();
                ModelTransform t = dest.getTransform();
                if (t != null)
                    value = t.transform(value);
                getValue(dest.getIdentifier())[0] += value;
            }
        }

        eg.init(synthesizer);
        lfo.init(synthesizer);

    }

    void setPolyPressure(int pressure) {
        if(performer == null)
            return;
        int[] c = performer.midi_connections[2];
        if (c == null)
            return;
        for (int i = 0; i < c.length; i++)
            processConnection(c[i]);
    }

    void setChannelPressure(int pressure) {
        if(performer == null)
            return;
        int[] c = performer.midi_connections[1];
        if (c == null)
            return;
        for (int i = 0; i < c.length; i++)
            processConnection(c[i]);
    }

    void controlChange(int controller, int value) {
        if(performer == null)
            return;
        int[] c = performer.midi_ctrl_connections[controller];
        if (c == null)
            return;
        for (int i = 0; i < c.length; i++)
            processConnection(c[i]);
    }

    void nrpnChange(int controller, int value) {
        if(performer == null)
            return;
        int[] c = performer.midi_nrpn_connections.get(controller);
        if (c == null)
            return;
        for (int i = 0; i < c.length; i++)
            processConnection(c[i]);
    }

    void rpnChange(int controller, int value) {
        if(performer == null)
            return;
        int[] c = performer.midi_rpn_connections.get(controller);
        if (c == null)
            return;
        for (int i = 0; i < c.length; i++)
            processConnection(c[i]);
    }

    void setPitchBend(int bend) {
        if(performer == null)
            return;
        int[] c = performer.midi_connections[0];
        if (c == null)
            return;
        for (int i = 0; i < c.length; i++)
            processConnection(c[i]);
    }

    void setMute(boolean mute) {
        co_mixer_gain[0] -= lastMuteValue;
        lastMuteValue = mute ? -960 : 0;
        co_mixer_gain[0] += lastMuteValue;
    }

    void setSoloMute(boolean mute) {
        co_mixer_gain[0] -= lastSoloMuteValue;
        lastSoloMuteValue = mute ? -960 : 0;
        co_mixer_gain[0] += lastSoloMuteValue;
    }

    void shutdown() {
        if (co_noteon_on[0] < -0.5)
            return;
        on = false;

        co_noteon_on[0] = -1;

        if(performer == null)
            return;
        int[] c = performer.midi_connections[3];
        if (c == null)
            return;
        for (int i = 0; i < c.length; i++)
            processConnection(c[i]);
    }

    void soundOff() {
        on = false;
        soundoff = true;
    }

    void noteOff(int velocity) {
        if (!on)
            return;
        on = false;

        noteOff_velocity = velocity;

        if (softchannel.sustain) {
            sustain = true;
            return;
        }
        if (sostenuto)
            return;

        co_noteon_on[0] = 0;

        if(performer == null)
            return;
        int[] c = performer.midi_connections[3];
        if (c == null)
            return;
        for (int i = 0; i < c.length; i++)
            processConnection(c[i]);
    }

    void redamp() {
        if (co_noteon_on[0] > 0.5)
            return;
        if (co_noteon_on[0] < -0.5)
            return; // don't redamp notes in shutdown stage

        sustain = true;
        co_noteon_on[0] = 1;

        if(performer == null)
            return;
        int[] c = performer.midi_connections[3];
        if (c == null)
            return;
        for (int i = 0; i < c.length; i++)
            processConnection(c[i]);
    }

    void processControlLogic() {
        if (stopping) {
            active = false;
            stopping = false;
            audiostarted = false;
            instrument = null;
            performer = null;
            connections = null;
            extendedConnectionBlocks = null;
            channelmixer = null;
            if (osc_stream != null)
                try {
                    osc_stream.close();
                } catch (IOException e) {
                    //e.printStackTrace();
                }

            if (stealer_channel != null) {
                stealer_channel.initVoice(this, stealer_performer,
                        stealer_voiceID, stealer_noteNumber, stealer_velocity, 0,
                        stealer_extendedConnectionBlocks, stealer_channelmixer,
                        stealer_releaseTriggered);
                stealer_releaseTriggered = false;
                stealer_channel = null;
                stealer_performer = null;
                stealer_voiceID = -1;
                stealer_noteNumber = 0;
                stealer_velocity = 0;
                stealer_extendedConnectionBlocks = null;
                stealer_channelmixer = null;
            }
        }
        if (started) {
            audiostarted = true;

            ModelOscillator osc = performer.oscillators[0];

            osc_stream_off_transmitted = false;
            if (osc instanceof ModelWavetable) {
                try {
                    resampler.open((ModelWavetable)osc,
                            synthesizer.getFormat().getSampleRate());
                    osc_stream = resampler;
                } catch (IOException e) {
                    //e.printStackTrace();
                }
            } else {
                osc_stream = osc.open(synthesizer.getFormat().getSampleRate());
            }
            osc_attenuation = osc.getAttenuation();
            osc_stream_nrofchannels = osc.getChannels();
            if (osc_buff == null || osc_buff.length < osc_stream_nrofchannels)
                osc_buff = new float[osc_stream_nrofchannels][];

            if (osc_stream != null)
                osc_stream.noteOn(softchannel, this, noteOn_noteNumber,
                        noteOn_velocity);


        }
        if (audiostarted) {
            if (portamento) {
                double note_delta = tunedKey - (co_noteon_keynumber[0] * 128);
                double note_delta_a = Math.abs(note_delta);
                if (note_delta_a < 0.0000000001) {
                    co_noteon_keynumber[0] = tunedKey * (1.0 / 128.0);
                    portamento = false;
                } else {
                    if (note_delta_a > softchannel.portamento_time)
                        note_delta = Math.signum(note_delta)
                                * softchannel.portamento_time;
                    co_noteon_keynumber[0] += note_delta * (1.0 / 128.0);
                }

                int[] c = performer.midi_connections[4];
                if (c == null)
                    return;
                for (int i = 0; i < c.length; i++)
                    processConnection(c[i]);
            }

            eg.processControlLogic();
            lfo.processControlLogic();

            for (int i = 0; i < performer.ctrl_connections.length; i++)
                processConnection(performer.ctrl_connections[i]);

            osc_stream.setPitch((float)co_osc_pitch[0]);

            int filter_type = (int)co_filter_type[0];
            double filter_freq;

            if (co_filter_freq[0] == 13500.0)
                filter_freq = 19912.126958213175;
            else
                filter_freq = 440.0 * Math.exp(
                        ((co_filter_freq[0]) - 6900.0) *
                        (Math.log(2.0) / 1200.0));
            /*
            filter_freq = 440.0 * Math.pow(2.0,
            ((co_filter_freq[0]) - 6900.0) / 1200.0);*/
            /*
             * double velocity = co_noteon_velocity[0]; if(velocity < 0.5)
             * filter_freq *= ((velocity * 2)*0.75 + 0.25);
             */

            double q = co_filter_q[0] / 10.0;
            filter_left.setFilterType(filter_type);
            filter_left.setFrequency(filter_freq);
            filter_left.setResonance(q);
            filter_right.setFilterType(filter_type);
            filter_right.setFrequency(filter_freq);
            filter_right.setResonance(q);
            /*
            float gain = (float) Math.pow(10,
            (-osc_attenuation + co_mixer_gain[0]) / 200.0);
             */
            float gain = (float)Math.exp(
                    (-osc_attenuation + co_mixer_gain[0])*(Math.log(10) / 200.0));

            if (co_mixer_gain[0] <= -960)
                gain = 0;

            if (soundoff) {
                stopping = true;
                gain = 0;
                /*
                 * if(co_mixer_gain[0] > -960)
                 *   co_mixer_gain[0] -= 960;
                 */
            }

            volume = (int)(Math.sqrt(gain) * 128);

            // gain *= 0.2;

            double pan = co_mixer_pan[0] * (1.0 / 1000.0);
            // System.out.println("pan = " + pan);
            if (pan < 0)
                pan = 0;
            else if (pan > 1)
                pan = 1;

            if (pan == 0.5) {
                out_mixer_left = gain * 0.7071067811865476f;
                out_mixer_right = out_mixer_left;
            } else {
                out_mixer_left = gain * (float)Math.cos(pan * Math.PI * 0.5);
                out_mixer_right = gain * (float)Math.sin(pan * Math.PI * 0.5);
            }

            double balance = co_mixer_balance[0] * (1.0 / 1000.0);
            if (balance != 0.5) {
                if (balance > 0.5)
                    out_mixer_left *= (1 - balance) * 2;
                else
                    out_mixer_right *= balance * 2;
            }

            if (synthesizer.reverb_on) {
                out_mixer_effect1 = (float)(co_mixer_reverb[0] * (1.0 / 1000.0));
                out_mixer_effect1 *= gain;
            } else
                out_mixer_effect1 = 0;
            if (synthesizer.chorus_on) {
                out_mixer_effect2 = (float)(co_mixer_chorus[0] * (1.0 / 1000.0));
                out_mixer_effect2 *= gain;
            } else
                out_mixer_effect2 = 0;
            out_mixer_end = co_mixer_active[0] < 0.5;

            if (!on)
                if (!osc_stream_off_transmitted) {
                    osc_stream_off_transmitted = true;
                    if (osc_stream != null)
                        osc_stream.noteOff(noteOff_velocity);
                }

        }
        if (started) {
            last_out_mixer_left = out_mixer_left;
            last_out_mixer_right = out_mixer_right;
            last_out_mixer_effect1 = out_mixer_effect1;
            last_out_mixer_effect2 = out_mixer_effect2;
            started = false;
        }

    }

    void mixAudioStream(SoftAudioBuffer in, SoftAudioBuffer out,
                                SoftAudioBuffer dout, float amp_from,
                                float amp_to) {
        int bufferlen = in.getSize();
        if (amp_from < 0.000000001 && amp_to < 0.000000001)
            return;
        if(dout != null && delay != 0)
        {
            if (amp_from == amp_to) {
                float[] fout = out.array();
                float[] fin = in.array();
                int j = 0;
                for (int i = delay; i < bufferlen; i++)
                    fout[i] += fin[j++] * amp_to;
                fout = dout.array();
                for (int i = 0; i < delay; i++)
                    fout[i] += fin[j++] * amp_to;
            } else {
                float amp = amp_from;
                float amp_delta = (amp_to - amp_from) / bufferlen;
                float[] fout = out.array();
                float[] fin = in.array();
                int j = 0;
                for (int i = delay; i < bufferlen; i++) {
                    amp += amp_delta;
                    fout[i] += fin[j++] * amp;
                }
                fout = dout.array();
                for (int i = 0; i < delay; i++) {
                    amp += amp_delta;
                    fout[i] += fin[j++] * amp;
                }
            }
        }
        else
        {
            if (amp_from == amp_to) {
                float[] fout = out.array();
                float[] fin = in.array();
                for (int i = 0; i < bufferlen; i++)
                    fout[i] += fin[i] * amp_to;
            } else {
                float amp = amp_from;
                float amp_delta = (amp_to - amp_from) / bufferlen;
                float[] fout = out.array();
                float[] fin = in.array();
                for (int i = 0; i < bufferlen; i++) {
                    amp += amp_delta;
                    fout[i] += fin[i] * amp;
                }
            }
        }

    }

    void processAudioLogic(SoftAudioBuffer[] buffer) {
        if (!audiostarted)
            return;

        int bufferlen = buffer[0].getSize();

        try {
            osc_buff[0] = buffer[SoftMainMixer.CHANNEL_LEFT_DRY].array();
            if (nrofchannels != 1)
                osc_buff[1] = buffer[SoftMainMixer.CHANNEL_RIGHT_DRY].array();
            int ret = osc_stream.read(osc_buff, 0, bufferlen);
            if (ret == -1) {
                stopping = true;
                return;
            }
            if (ret != bufferlen) {
                Arrays.fill(osc_buff[0], ret, bufferlen, 0f);
                if (nrofchannels != 1)
                    Arrays.fill(osc_buff[1], ret, bufferlen, 0f);
            }

        } catch (IOException e) {
            //e.printStackTrace();
        }

        SoftAudioBuffer left = buffer[SoftMainMixer.CHANNEL_LEFT];
        SoftAudioBuffer right = buffer[SoftMainMixer.CHANNEL_RIGHT];
        SoftAudioBuffer mono = buffer[SoftMainMixer.CHANNEL_MONO];
        SoftAudioBuffer eff1 = buffer[SoftMainMixer.CHANNEL_EFFECT1];
        SoftAudioBuffer eff2 = buffer[SoftMainMixer.CHANNEL_EFFECT2];

        SoftAudioBuffer dleft = buffer[SoftMainMixer.CHANNEL_DELAY_LEFT];
        SoftAudioBuffer dright = buffer[SoftMainMixer.CHANNEL_DELAY_RIGHT];
        SoftAudioBuffer dmono = buffer[SoftMainMixer.CHANNEL_DELAY_MONO];
        SoftAudioBuffer deff1 = buffer[SoftMainMixer.CHANNEL_DELAY_EFFECT1];
        SoftAudioBuffer deff2 = buffer[SoftMainMixer.CHANNEL_DELAY_EFFECT2];

        SoftAudioBuffer leftdry = buffer[SoftMainMixer.CHANNEL_LEFT_DRY];
        SoftAudioBuffer rightdry = buffer[SoftMainMixer.CHANNEL_RIGHT_DRY];

        if (osc_stream_nrofchannels == 1)
            rightdry = null;

        if (!Double.isInfinite(co_filter_freq[0])) {
            filter_left.processAudio(leftdry);
            if (rightdry != null)
                filter_right.processAudio(rightdry);
        }

        if (nrofchannels == 1) {
            out_mixer_left = (out_mixer_left + out_mixer_right) / 2;
            mixAudioStream(leftdry, left, dleft, last_out_mixer_left, out_mixer_left);
            if (rightdry != null)
                mixAudioStream(rightdry, left, dleft, last_out_mixer_left,
                        out_mixer_left);
        } else {
            if(rightdry == null &&
                    last_out_mixer_left == last_out_mixer_right &&
                    out_mixer_left == out_mixer_right)
            {
                mixAudioStream(leftdry, mono, dmono, last_out_mixer_left, out_mixer_left);
            }
            else
            {
                mixAudioStream(leftdry, left, dleft, last_out_mixer_left, out_mixer_left);
                if (rightdry != null)
                    mixAudioStream(rightdry, right, dright, last_out_mixer_right,
                        out_mixer_right);
                else
                    mixAudioStream(leftdry, right, dright, last_out_mixer_right,
                        out_mixer_right);
            }
        }

        if (rightdry == null) {
            mixAudioStream(leftdry, eff1, deff1, last_out_mixer_effect1,
                    out_mixer_effect1);
            mixAudioStream(leftdry, eff2, deff2, last_out_mixer_effect2,
                    out_mixer_effect2);
        } else {
            mixAudioStream(leftdry, eff1, deff1, last_out_mixer_effect1 * 0.5f,
                    out_mixer_effect1 * 0.5f);
            mixAudioStream(leftdry, eff2, deff2, last_out_mixer_effect2 * 0.5f,
                    out_mixer_effect2 * 0.5f);
            mixAudioStream(rightdry, eff1, deff1, last_out_mixer_effect1 * 0.5f,
                    out_mixer_effect1 * 0.5f);
            mixAudioStream(rightdry, eff2, deff2, last_out_mixer_effect2 * 0.5f,
                    out_mixer_effect2 * 0.5f);
        }

        last_out_mixer_left = out_mixer_left;
        last_out_mixer_right = out_mixer_right;
        last_out_mixer_effect1 = out_mixer_effect1;
        last_out_mixer_effect2 = out_mixer_effect2;

        if (out_mixer_end) {
            stopping = true;
        }
    }
}
