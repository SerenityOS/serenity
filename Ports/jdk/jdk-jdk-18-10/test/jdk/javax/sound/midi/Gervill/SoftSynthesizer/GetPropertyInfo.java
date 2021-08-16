/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

/* @test
 @summary Test SoftSynthesizer getPropertyInfo method
 @modules java.desktop/com.sun.media.sound
*/

import java.util.HashMap;
import java.util.Map;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Patch;
import javax.sound.midi.Soundbank;
import javax.sound.sampled.*;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.midi.MidiDevice.Info;

import com.sun.media.sound.*;

public class GetPropertyInfo {

    private static void assertTrue(boolean value) throws Exception {
        if (!value)
            throw new RuntimeException("assertTrue fails!");
    }

    public static void main(String[] args) throws Exception {
        SoftSynthesizer synth = new SoftSynthesizer();
        Map<String, Object> p = new HashMap<String, Object>();
        p.put("format", "8000 HZ 24 BIT MONO UNSIGNED BIG-ENDIAN");
        p.put("control rate", 125);
        p.put("reverb", false);
        p.put("auto gain control", "false");
        AudioSynthesizerPropertyInfo[] ap = synth.getPropertyInfo(p);
        for (int i = 0; i < ap.length; i++) {
            if (ap[i].name.equals("control rate"))
                assertTrue(Math.abs((Float) ap[i].value - 125.0) < 0.001);
            if (ap[i].name.equals("reverb"))
                assertTrue((Boolean) ap[i].value == false);
            if (ap[i].name.equals("auto gain control"))
                assertTrue((Boolean) ap[i].value == false);
            if (ap[i].name.equals("format")) {
                AudioFormat format = (AudioFormat) ap[i].value;
                assertTrue(format.getChannels() == 1);
                assertTrue(format.getSampleSizeInBits() == 24);
                assertTrue(format.isBigEndian());
                assertTrue(Math.abs(format.getSampleRate() - 8000) < 0.001);
                assertTrue(format.getEncoding() == Encoding.PCM_UNSIGNED);
            }
        }
        p = new HashMap<String, Object>();
        p.put("format", "9000 Hz, 8 bit, 4 channels");
        ap = synth.getPropertyInfo(p);
        for (int i = 0; i < ap.length; i++) {
            if (ap[i].name.equals("format")) {
                AudioFormat format = (AudioFormat) ap[i].value;
                assertTrue(format.getChannels() == 4);
                assertTrue(format.getSampleSizeInBits() == 8);
                assertTrue(!format.isBigEndian());
                assertTrue(Math.abs(format.getSampleRate() - 9000) < 0.001);
                assertTrue(format.getEncoding() == Encoding.PCM_SIGNED);
            }
        }

        p = new HashMap<String, Object>();
        p.put("format", "PCM_UNSIGNED 44100.0 Hz, 16 bit, 3 channels, 6 bytes/frame, big-endian");
        ap = synth.getPropertyInfo(p);
        for (int i = 0; i < ap.length; i++) {
            if (ap[i].name.equals("format")) {
                AudioFormat format = (AudioFormat) ap[i].value;
                assertTrue(format.getChannels() == 3);
                assertTrue(format.getSampleSizeInBits() == 16);
                assertTrue(format.isBigEndian());
                assertTrue(Math.abs(format.getSampleRate() - 44100) < 0.001);
                assertTrue(format.getEncoding() == Encoding.PCM_UNSIGNED);
            }
        }


    }
}
