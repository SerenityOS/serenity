/*
 * Copyright (c) 2009, 2015, Oracle and/or its affiliates. All rights reserved.
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
 @summary Test SoftLowFrequencyOscillator processControlLogic method
 @modules java.desktop/com.sun.media.sound
*/

import com.sun.media.sound.AudioSynthesizerPropertyInfo;
import com.sun.media.sound.SoftLowFrequencyOscillator;
import com.sun.media.sound.SoftSynthesizer;

public class TestProcessControlLogic {

    private static float control_rate = 147f;
    private static SoftSynthesizer synth = new SoftSynthesizer();
    private static SoftLowFrequencyOscillator lfo = new SoftLowFrequencyOscillator();

    private static void testLFO(boolean shared, int instance, float freq, float delay,
            float delay2) throws Exception {
        SoftLowFrequencyOscillator lfo =
            shared?TestProcessControlLogic.lfo:new SoftLowFrequencyOscillator();
        lfo.reset();
        double[] lfo_freq = lfo.get(instance, "freq");
        double[] lfo_delay = lfo.get(instance, "delay");
        double[] lfo_delay2 = lfo.get(instance, "delay2");
        double[] lfo_output = lfo.get(instance, null);
        lfo_freq[0] = freq;
        lfo_delay[0] = delay;
        lfo_delay2[0] = delay2;
        lfo.init(synth);

        // For delayCount amount time, the output LFO should be 0.5
        int delayCount = (int) ((Math.pow(2, delay / 1200.0) * control_rate));
        delayCount += (int) ((delay2 * control_rate) / 1000.0);
        for (int i = 0; i < delayCount; i++) {
            if (Math.abs(0.5 - lfo_output[0]) > 0.000001)
                throw new Exception("Incorrect LFO output ("
                        +"0.5 != "+lfo_output[0]+")!");
            lfo.processControlLogic();
        }

        // After the delay the LFO should start oscillate
        // Let make sure output is accurate enough
        double p_step = (440.0 / control_rate)
        * Math.exp((freq - 6900.0) * (Math.log(2) / 1200.0));
        double p = 0;
        for (int i = 0; i < 30; i++) {
            p += p_step;
            double predicted_output = 0.5 + Math.sin(p * 2 * Math.PI) * 0.5;
            if (Math.abs(predicted_output - lfo_output[0]) > 0.001)
                throw new Exception("Incorrect LFO output ("
                        +predicted_output+" != "+lfo_output[0]+")!");
            lfo.processControlLogic();
        }

    }

    public static void main(String[] args) throws Exception {

        // Get default control rate from synthesizer
        AudioSynthesizerPropertyInfo[] p = synth.getPropertyInfo(null);
        for (int i = 0; i < p.length; i++) {
            if (p[i].name.equals("control rate")) {
                control_rate = ((Float) p[i].value).floatValue();
                break;
            }
        }

        // Test LFO under various configurations
        for (int instance = 0; instance < 3; instance++)
            for (int d1 = -3000; d1 < 0; d1 += 1000)
                for (int d2 = 0; d2 < 5000; d2 += 1000)
                    for (int fr = -1000; fr < 1000; fr += 100) {
                        testLFO(true, instance,
                                (fr == -1000) ? Float.NEGATIVE_INFINITY : fr,
                                (d1 == -3000) ? Float.NEGATIVE_INFINITY : d1,
                                d2);
                        testLFO(false, instance,
                                (fr == -1000) ? Float.NEGATIVE_INFINITY : fr,
                                (d1 == -3000) ? Float.NEGATIVE_INFINITY : d1,
                                d2);
                    }

    }
}
