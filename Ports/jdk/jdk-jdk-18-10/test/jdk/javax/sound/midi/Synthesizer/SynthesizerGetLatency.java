/*
 * Copyright (c) 2004, 2016, Oracle and/or its affiliates. All rights reserved.
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

import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;

/**
 * @test
 * @bug 5029790
 * @summary Synthesizer.getLatency returns wrong value
 */
public class SynthesizerGetLatency {

    public static void main(String args[]) throws Exception {
        Synthesizer synth = null;
        boolean failed = false;
        boolean notexec = false;
        try {
            synth = MidiSystem.getSynthesizer();
            System.out.println("Got synth: "+synth);
            synth.open();

            int latency = (int) synth.getLatency();
            System.out.println("  -> latency: "
                               +latency
                               +" microseconds");
            if (latency < 5000 && latency > 0) {
                System.out.println("## This latency is VERY small, probably due to this bug.");
                System.out.println("## This causes failure of this test.");
                failed = true;
            }
        } catch (MidiUnavailableException mue) {
            System.err.println("MidiUnavailableException was "
                               +"thrown: " + mue);
            System.out.println("could not test.");
            notexec = true;
        } catch(SecurityException se) {
            se.printStackTrace();
            System.err.println("Sound access is not denied but "
            + "SecurityException was thrown!");
            notexec = true;
        } finally {
            if (synth != null) synth.close();
        }


        if (failed) {
            throw new Exception("Test FAILED!");
        }
        if (notexec) {
            System.out.println("Test not failed.");
        } else {
            System.out.println("Test Passed.");
        }
    }
}
