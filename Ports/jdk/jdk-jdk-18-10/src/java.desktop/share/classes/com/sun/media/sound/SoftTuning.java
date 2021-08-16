/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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

import javax.sound.midi.Patch;

import static java.nio.charset.StandardCharsets.US_ASCII;

/**
 * A tuning program container, for use with MIDI Tuning.
 * See: http://www.midi.org
 *
 * @author Karl Helgason
 */
public final class SoftTuning {

    private String name = null;
    private final double[] tuning = new double[128];
    private Patch patch = null;

    public SoftTuning() {
        name = "12-TET";
        for (int i = 0; i < tuning.length; i++)
            tuning[i] = i * 100;
    }

    public SoftTuning(byte[] data) {
        for (int i = 0; i < tuning.length; i++)
            tuning[i] = i * 100;
        load(data);
    }

    public SoftTuning(Patch patch) {
        this.patch = patch;
        name = "12-TET";
        for (int i = 0; i < tuning.length; i++)
            tuning[i] = i * 100;
    }

    public SoftTuning(Patch patch, byte[] data) {
        this.patch = patch;
        for (int i = 0; i < tuning.length; i++)
            tuning[i] = i * 100;
        load(data);
    }

    private boolean checksumOK(byte[] data) {
        int x = data[1] & 0xFF;
        for (int i = 2; i < data.length - 2; i++)
            x = x ^ (data[i] & 0xFF);
        return (data[data.length - 2] & 0xFF) == (x & 127);
    }

    /*
    private boolean checksumOK2(byte[] data) {
        int x = data[1] & 0xFF; // 7E
        x = x ^ (data[2] & 0xFF); // <device ID>
        x = x ^ (data[4] & 0xFF); // nn
        x = x ^ (data[5] & 0xFF); // tt
        for (int i = 22; i < data.length - 2; i++)
            x = x ^ (data[i] & 0xFF);
        return (data[data.length - 2] & 0xFF) == (x & 127);
    }
     */
    public void load(byte[] data) {
        // Universal Non-Real-Time / Real-Time SysEx
        if ((data[1] & 0xFF) == 0x7E || (data[1] & 0xFF) == 0x7F) {
            int subid1 = data[3] & 0xFF;
            switch (subid1) {
            case 0x08: // MIDI Tuning Standard
                int subid2 = data[4] & 0xFF;
                switch (subid2) {
                case 0x01: // BULK TUNING DUMP (NON-REAL-TIME)
                {
                    // http://www.midi.org/about-midi/tuning.shtml
                    //if (!checksumOK2(data))
                    //    break;
                    name = new String(data, 6, 16, US_ASCII);
                    int r = 22;
                    for (int i = 0; i < 128; i++) {
                        int xx = data[r++] & 0xFF;
                        int yy = data[r++] & 0xFF;
                        int zz = data[r++] & 0xFF;
                        if (!(xx == 127 && yy == 127 && zz == 127))
                            tuning[i] = 100.0 *
                                    (((xx * 16384) + (yy * 128) + zz) / 16384.0);
                    }
                    break;
                }
                case 0x02: // SINGLE NOTE TUNING CHANGE (REAL-TIME)
                {
                    // http://www.midi.org/about-midi/tuning.shtml
                    int ll = data[6] & 0xFF;
                    int r = 7;
                    for (int i = 0; i < ll; i++) {
                        int kk = data[r++] & 0xFF;
                        int xx = data[r++] & 0xFF;
                        int yy = data[r++] & 0xFF;
                        int zz = data[r++] & 0xFF;
                        if (!(xx == 127 && yy == 127 && zz == 127))
                            tuning[kk] = 100.0*(((xx*16384) + (yy*128) + zz)/16384.0);
                    }
                    break;
                }
                case 0x04: // KEY-BASED TUNING DUMP (NON-REAL-TIME)
                {
                    // http://www.midi.org/about-midi/tuning_extens.shtml
                    if (!checksumOK(data))
                        break;
                    name = new String(data, 7, 16, US_ASCII);
                    int r = 23;
                    for (int i = 0; i < 128; i++) {
                        int xx = data[r++] & 0xFF;
                        int yy = data[r++] & 0xFF;
                        int zz = data[r++] & 0xFF;
                        if (!(xx == 127 && yy == 127 && zz == 127))
                            tuning[i] = 100.0*(((xx*16384) + (yy*128) + zz)/16384.0);
                    }
                    break;
                }
                case 0x05: // SCALE/OCTAVE TUNING DUMP, 1 byte format
                           // (NON-REAL-TIME)
                {
                    // http://www.midi.org/about-midi/tuning_extens.shtml
                    if (!checksumOK(data))
                        break;
                    name = new String(data, 7, 16, US_ASCII);
                    int[] octave_tuning = new int[12];
                    for (int i = 0; i < 12; i++)
                        octave_tuning[i] = (data[i + 23] & 0xFF) - 64;
                    for (int i = 0; i < tuning.length; i++)
                        tuning[i] = i * 100 + octave_tuning[i % 12];
                    break;
                }
                case 0x06: // SCALE/OCTAVE TUNING DUMP, 2 byte format
                           // (NON-REAL-TIME)
                {
                    // http://www.midi.org/about-midi/tuning_extens.shtml
                    if (!checksumOK(data))
                        break;
                    name = new String(data, 7, 16, US_ASCII);
                    double[] octave_tuning = new double[12];
                    for (int i = 0; i < 12; i++) {
                        int v = (data[i * 2 + 23] & 0xFF) * 128
                                + (data[i * 2 + 24] & 0xFF);
                        octave_tuning[i] = (v / 8192.0 - 1) * 100.0;
                    }
                    for (int i = 0; i < tuning.length; i++)
                        tuning[i] = i * 100 + octave_tuning[i % 12];
                    break;
                }
                case 0x07: // SINGLE NOTE TUNING CHANGE (NON
                           // REAL-TIME/REAL-TIME) (BANK)
                    // http://www.midi.org/about-midi/tuning_extens.shtml
                    int ll = data[7] & 0xFF;
                    int r = 8;
                    for (int i = 0; i < ll; i++) {
                        int kk = data[r++] & 0xFF;
                        int xx = data[r++] & 0xFF;
                        int yy = data[r++] & 0xFF;
                        int zz = data[r++] & 0xFF;
                        if (!(xx == 127 && yy == 127 && zz == 127))
                            tuning[kk] = 100.0
                                    * (((xx*16384) + (yy*128) + zz) / 16384.0);
                    }
                    break;
                case 0x08: // scale/octave tuning 1-byte form (Non
                           // Real-Time/REAL-TIME)
                {
                    // http://www.midi.org/about-midi/tuning-scale.shtml
                    int[] octave_tuning = new int[12];
                    for (int i = 0; i < 12; i++)
                        octave_tuning[i] = (data[i + 8] & 0xFF) - 64;
                    for (int i = 0; i < tuning.length; i++)
                        tuning[i] = i * 100 + octave_tuning[i % 12];
                    break;
                }
                case 0x09: // scale/octave tuning 2-byte form (Non
                           // Real-Time/REAL-TIME)
                {
                    // http://www.midi.org/about-midi/tuning-scale.shtml
                    double[] octave_tuning = new double[12];
                    for (int i = 0; i < 12; i++) {
                        int v = (data[i * 2 + 8] & 0xFF) * 128
                                + (data[i * 2 + 9] & 0xFF);
                        octave_tuning[i] = (v / 8192.0 - 1) * 100.0;
                    }
                    for (int i = 0; i < tuning.length; i++)
                        tuning[i] = i * 100 + octave_tuning[i % 12];
                    break;
                }
                default:
                    break;
                }
            }
        }
    }

    // am: getTuning(int) is more effective.
    // currently getTuning() is used only by tests
    public double[] getTuning() {
        return Arrays.copyOf(tuning, tuning.length);
    }

    public double getTuning(int noteNumber) {
        return tuning[noteNumber];
    }

    public Patch getPatch() {
        return patch;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
