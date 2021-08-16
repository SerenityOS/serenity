/*
 * Copyright (c) 2007, 2015, Oracle and/or its affiliates. All rights reserved.
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
   @summary Test SoftLimiter processAudio method
   @modules java.desktop/com.sun.media.sound
*/

import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Patch;
import javax.sound.sampled.*;

import com.sun.media.sound.*;

public class ProcessAudio_replace_normal {

    private static void assertEquals(Object a, Object b) throws Exception
    {
        if(!a.equals(b))
            throw new RuntimeException("assertEquals fails!");
    }

    private static void assertTrue(boolean value) throws Exception
    {
        if(!value)
            throw new RuntimeException("assertTrue fails!");
    }

    public static void main(String[] args) throws Exception {
        SoftSynthesizer synth = new SoftSynthesizer();
        synth.openStream(null, null);

        SoftAudioBuffer in1 = new SoftAudioBuffer(250, synth.getFormat());
        SoftAudioBuffer in2 = new SoftAudioBuffer(250, synth.getFormat());
        SoftAudioBuffer out1 = new SoftAudioBuffer(250, synth.getFormat());
        SoftAudioBuffer out2 = new SoftAudioBuffer(250, synth.getFormat());

        float[] testdata1 = new float[in1.getSize()];
        float[] testdata2 = new float[in2.getSize()];
        float[] n1a = in1.array();
        float[] n2a = in2.array();
        float[] out1a = out1.array();
        float[] out2a = out2.array();
        for (int i = 0; i < n1a.length; i++) {
            testdata1[i] = (float)Math.sin(i*0.3)*0.9f;
            testdata2[i] = (float)Math.sin(i*0.4)*0.9f;
            n1a[i] = testdata1[i];
            n2a[i] = testdata2[i];
            out1a[i] = 1;
            out2a[i] = 1;
        }

        SoftLimiter limiter = new SoftLimiter();
        limiter.init(44100, 147);
        limiter.setMixMode(false);
        limiter.setInput(0, in1);
        limiter.setInput(1, in2);
        limiter.setOutput(0, out1);
        limiter.setOutput(1, out2);
        limiter.processControlLogic();
        limiter.processAudio();
        limiter.processControlLogic();
        limiter.processAudio();
        // Limiter should delay audio by one buffer,
        // and there should almost no different in output v.s. input
        for (int i = 0; i < n1a.length; i++) {
            if(Math.abs(out1a[i] - testdata1[i]) > 0.00001)
                throw new Exception("input != output");
            if(Math.abs(out2a[i] - testdata2[i]) > 0.00001)
                throw new Exception("input != output");
        }

        synth.close();
    }
}
