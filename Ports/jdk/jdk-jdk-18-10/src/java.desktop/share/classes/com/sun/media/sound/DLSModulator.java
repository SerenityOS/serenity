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
 * This class is used to store modulator/artiuclation data.
 * A modulator connects one synthesizer source to
 * a destination. For example a note on velocity
 * can be mapped to the gain of the synthesized voice.
 * It is stored as a "art1" or "art2" chunk inside DLS files.
 *
 * @author Karl Helgason
 */
public final class DLSModulator {

    // DLS1 Destinations
    public static final int CONN_DST_NONE = 0x000; // 0
    public static final int CONN_DST_GAIN = 0x001; // cB
    public static final int CONN_DST_PITCH = 0x003; // cent
    public static final int CONN_DST_PAN = 0x004; // 0.1%
    public static final int CONN_DST_LFO_FREQUENCY = 0x104; // cent (default 5 Hz)
    public static final int CONN_DST_LFO_STARTDELAY = 0x105; // timecent
    public static final int CONN_DST_EG1_ATTACKTIME = 0x206; // timecent
    public static final int CONN_DST_EG1_DECAYTIME = 0x207; // timecent
    public static final int CONN_DST_EG1_RELEASETIME = 0x209; // timecent
    public static final int CONN_DST_EG1_SUSTAINLEVEL = 0x20A; // 0.1%
    public static final int CONN_DST_EG2_ATTACKTIME = 0x30A; // timecent
    public static final int CONN_DST_EG2_DECAYTIME = 0x30B; // timecent
    public static final int CONN_DST_EG2_RELEASETIME = 0x30D; // timecent
    public static final int CONN_DST_EG2_SUSTAINLEVEL = 0x30E; // 0.1%
    // DLS2 Destinations
    public static final int CONN_DST_KEYNUMBER = 0x005;
    public static final int CONN_DST_LEFT = 0x010; // 0.1%
    public static final int CONN_DST_RIGHT = 0x011; // 0.1%
    public static final int CONN_DST_CENTER = 0x012; // 0.1%
    public static final int CONN_DST_LEFTREAR = 0x013; // 0.1%
    public static final int CONN_DST_RIGHTREAR = 0x014; // 0.1%
    public static final int CONN_DST_LFE_CHANNEL = 0x015; // 0.1%
    public static final int CONN_DST_CHORUS = 0x080; // 0.1%
    public static final int CONN_DST_REVERB = 0x081; // 0.1%
    public static final int CONN_DST_VIB_FREQUENCY = 0x114; // cent
    public static final int CONN_DST_VIB_STARTDELAY = 0x115; // dB
    public static final int CONN_DST_EG1_DELAYTIME = 0x20B; // timecent
    public static final int CONN_DST_EG1_HOLDTIME = 0x20C; // timecent
    public static final int CONN_DST_EG1_SHUTDOWNTIME = 0x20D; // timecent
    public static final int CONN_DST_EG2_DELAYTIME = 0x30F; // timecent
    public static final int CONN_DST_EG2_HOLDTIME = 0x310; // timecent
    public static final int CONN_DST_FILTER_CUTOFF = 0x500; // cent
    public static final int CONN_DST_FILTER_Q = 0x501; // dB

    // DLS1 Sources
    public static final int CONN_SRC_NONE = 0x000; // 1
    public static final int CONN_SRC_LFO = 0x001; // linear (sine wave)
    public static final int CONN_SRC_KEYONVELOCITY = 0x002; // ??db or velocity??
    public static final int CONN_SRC_KEYNUMBER = 0x003; // ??cent or keynumber??
    public static final int CONN_SRC_EG1 = 0x004; // linear direct from eg
    public static final int CONN_SRC_EG2 = 0x005; // linear direct from eg
    public static final int CONN_SRC_PITCHWHEEL = 0x006; // linear -1..1
    public static final int CONN_SRC_CC1 = 0x081; // linear 0..1
    public static final int CONN_SRC_CC7 = 0x087; // linear 0..1
    public static final int CONN_SRC_CC10 = 0x08A; // linear 0..1
    public static final int CONN_SRC_CC11 = 0x08B; // linear 0..1
    public static final int CONN_SRC_RPN0 = 0x100; // ?? // Pitch Bend Range
    public static final int CONN_SRC_RPN1 = 0x101; // ?? // Fine Tune
    public static final int CONN_SRC_RPN2 = 0x102; // ?? // Course Tune
    // DLS2 Sources
    public static final int CONN_SRC_POLYPRESSURE = 0x007; // linear 0..1
    public static final int CONN_SRC_CHANNELPRESSURE = 0x008; // linear 0..1
    public static final int CONN_SRC_VIBRATO = 0x009; // linear 0..1
    public static final int CONN_SRC_MONOPRESSURE = 0x00A; // linear 0..1
    public static final int CONN_SRC_CC91 = 0x0DB; // linear 0..1
    public static final int CONN_SRC_CC93 = 0x0DD; // linear 0..1
    // DLS1 Transforms
    public static final int CONN_TRN_NONE = 0x000;
    public static final int CONN_TRN_CONCAVE = 0x001;
    // DLS2 Transforms
    public static final int CONN_TRN_CONVEX = 0x002;
    public static final int CONN_TRN_SWITCH = 0x003;
    public static final int DST_FORMAT_CB = 1;
    public static final int DST_FORMAT_CENT = 1;
    public static final int DST_FORMAT_TIMECENT = 2;
    public static final int DST_FORMAT_PERCENT = 3;
    int source;
    int control;
    int destination;
    int transform;
    int scale;
    int version = 1;

    public int getControl() {
        return control;
    }

    public void setControl(int control) {
        this.control = control;
    }

    public static int getDestinationFormat(int destination) {

        if (destination == CONN_DST_GAIN)
            return DST_FORMAT_CB;
        if (destination == CONN_DST_PITCH)
            return DST_FORMAT_CENT;
        if (destination == CONN_DST_PAN)
            return DST_FORMAT_PERCENT;

        if (destination == CONN_DST_LFO_FREQUENCY)
            return DST_FORMAT_CENT;
        if (destination == CONN_DST_LFO_STARTDELAY)
            return DST_FORMAT_TIMECENT;

        if (destination == CONN_DST_EG1_ATTACKTIME)
            return DST_FORMAT_TIMECENT;
        if (destination == CONN_DST_EG1_DECAYTIME)
            return DST_FORMAT_TIMECENT;
        if (destination == CONN_DST_EG1_RELEASETIME)
            return DST_FORMAT_TIMECENT;
        if (destination == CONN_DST_EG1_SUSTAINLEVEL)
            return DST_FORMAT_PERCENT;

        if (destination == CONN_DST_EG2_ATTACKTIME)
            return DST_FORMAT_TIMECENT;
        if (destination == CONN_DST_EG2_DECAYTIME)
            return DST_FORMAT_TIMECENT;
        if (destination == CONN_DST_EG2_RELEASETIME)
            return DST_FORMAT_TIMECENT;
        if (destination == CONN_DST_EG2_SUSTAINLEVEL)
            return DST_FORMAT_PERCENT;

        if (destination == CONN_DST_KEYNUMBER)
            return DST_FORMAT_CENT; // NOT SURE WITHOUT DLS 2 SPEC
        if (destination == CONN_DST_LEFT)
            return DST_FORMAT_CB;
        if (destination == CONN_DST_RIGHT)
            return DST_FORMAT_CB;
        if (destination == CONN_DST_CENTER)
            return DST_FORMAT_CB;
        if (destination == CONN_DST_LEFTREAR)
            return DST_FORMAT_CB;
        if (destination == CONN_DST_RIGHTREAR)
            return DST_FORMAT_CB;
        if (destination == CONN_DST_LFE_CHANNEL)
            return DST_FORMAT_CB;
        if (destination == CONN_DST_CHORUS)
            return DST_FORMAT_PERCENT;
        if (destination == CONN_DST_REVERB)
            return DST_FORMAT_PERCENT;

        if (destination == CONN_DST_VIB_FREQUENCY)
            return DST_FORMAT_CENT;
        if (destination == CONN_DST_VIB_STARTDELAY)
            return DST_FORMAT_TIMECENT;

        if (destination == CONN_DST_EG1_DELAYTIME)
            return DST_FORMAT_TIMECENT;
        if (destination == CONN_DST_EG1_HOLDTIME)
            return DST_FORMAT_TIMECENT;
        if (destination == CONN_DST_EG1_SHUTDOWNTIME)
            return DST_FORMAT_TIMECENT;

        if (destination == CONN_DST_EG2_DELAYTIME)
            return DST_FORMAT_TIMECENT;
        if (destination == CONN_DST_EG2_HOLDTIME)
            return DST_FORMAT_TIMECENT;

        if (destination == CONN_DST_FILTER_CUTOFF)
            return DST_FORMAT_CENT;
        if (destination == CONN_DST_FILTER_Q)
            return DST_FORMAT_CB;

        return -1;
    }

    public static String getDestinationName(int destination) {

        if (destination == CONN_DST_GAIN)
            return "gain";
        if (destination == CONN_DST_PITCH)
            return "pitch";
        if (destination == CONN_DST_PAN)
            return "pan";

        if (destination == CONN_DST_LFO_FREQUENCY)
            return "lfo1.freq";
        if (destination == CONN_DST_LFO_STARTDELAY)
            return "lfo1.delay";

        if (destination == CONN_DST_EG1_ATTACKTIME)
            return "eg1.attack";
        if (destination == CONN_DST_EG1_DECAYTIME)
            return "eg1.decay";
        if (destination == CONN_DST_EG1_RELEASETIME)
            return "eg1.release";
        if (destination == CONN_DST_EG1_SUSTAINLEVEL)
            return "eg1.sustain";

        if (destination == CONN_DST_EG2_ATTACKTIME)
            return "eg2.attack";
        if (destination == CONN_DST_EG2_DECAYTIME)
            return "eg2.decay";
        if (destination == CONN_DST_EG2_RELEASETIME)
            return "eg2.release";
        if (destination == CONN_DST_EG2_SUSTAINLEVEL)
            return "eg2.sustain";

        if (destination == CONN_DST_KEYNUMBER)
            return "keynumber";
        if (destination == CONN_DST_LEFT)
            return "left";
        if (destination == CONN_DST_RIGHT)
            return "right";
        if (destination == CONN_DST_CENTER)
            return "center";
        if (destination == CONN_DST_LEFTREAR)
            return "leftrear";
        if (destination == CONN_DST_RIGHTREAR)
            return "rightrear";
        if (destination == CONN_DST_LFE_CHANNEL)
            return "lfe_channel";
        if (destination == CONN_DST_CHORUS)
            return "chorus";
        if (destination == CONN_DST_REVERB)
            return "reverb";

        if (destination == CONN_DST_VIB_FREQUENCY)
            return "vib.freq";
        if (destination == CONN_DST_VIB_STARTDELAY)
            return "vib.delay";

        if (destination == CONN_DST_EG1_DELAYTIME)
            return "eg1.delay";
        if (destination == CONN_DST_EG1_HOLDTIME)
            return "eg1.hold";
        if (destination == CONN_DST_EG1_SHUTDOWNTIME)
            return "eg1.shutdown";

        if (destination == CONN_DST_EG2_DELAYTIME)
            return "eg2.delay";
        if (destination == CONN_DST_EG2_HOLDTIME)
            return "eg.2hold";

        if (destination == CONN_DST_FILTER_CUTOFF)
            return "filter.cutoff"; // NOT SURE WITHOUT DLS 2 SPEC
        if (destination == CONN_DST_FILTER_Q)
            return "filter.q"; // NOT SURE WITHOUT DLS 2 SPEC

        return null;
    }

    public static String getSourceName(int source) {

        if (source == CONN_SRC_NONE)
            return "none";
        if (source == CONN_SRC_LFO)
            return "lfo";
        if (source == CONN_SRC_KEYONVELOCITY)
            return "keyonvelocity";
        if (source == CONN_SRC_KEYNUMBER)
            return "keynumber";
        if (source == CONN_SRC_EG1)
            return "eg1";
        if (source == CONN_SRC_EG2)
            return "eg2";
        if (source == CONN_SRC_PITCHWHEEL)
            return "pitchweel";
        if (source == CONN_SRC_CC1)
            return "cc1";
        if (source == CONN_SRC_CC7)
            return "cc7";
        if (source == CONN_SRC_CC10)
            return "c10";
        if (source == CONN_SRC_CC11)
            return "cc11";

        if (source == CONN_SRC_POLYPRESSURE)
            return "polypressure";
        if (source == CONN_SRC_CHANNELPRESSURE)
            return "channelpressure";
        if (source == CONN_SRC_VIBRATO)
            return "vibrato";
        if (source == CONN_SRC_MONOPRESSURE)
            return "monopressure";
        if (source == CONN_SRC_CC91)
            return "cc91";
        if (source == CONN_SRC_CC93)
            return "cc93";
        return null;
    }

    public int getDestination() {
        return destination;
    }

    public void setDestination(int destination) {
        this.destination = destination;
    }

    public int getScale() {
        return scale;
    }

    public void setScale(int scale) {
        this.scale = scale;
    }

    public int getSource() {
        return source;
    }

    public void setSource(int source) {
        this.source = source;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public int getTransform() {
        return transform;
    }

    public void setTransform(int transform) {
        this.transform = transform;
    }
}
