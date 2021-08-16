/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;

/**
 * @test
 * @bug 4914667
 * @summary Closing and reopening MIDI IN device on Linux throws
 *          MidiUnavailableException
 */
public class Reopen {

    private static boolean isTestExecuted;
    private static boolean isTestPassed;

    /*
     * run manually:
     * java Reopen 100 in      for 100 iterations on the MIDI IN device
     * java Reopen 16 out      for 16 iterations on the MIDI OUT device
     */
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            doAllTests();
        } else if (args.length == 2) {
            int numIterations = Integer.parseInt(args[0]);
            if (args[1].equals("in")) {
                doTest(numIterations, true);
            } else {
                doTest(numIterations, false);
            }
        } else {
            out("usage: java Reopen <iterations> in|out");
        }
    }

    private static void doAllTests() throws Exception {
        out("#4914667: Closing and reopening MIDI IN device on Linux throws MidiUnavailableException");
        boolean success = true;
        try {
            success &= doTest(20, true); // MIDI IN
            success &= doTest(20, false); // MIDI OUT
            isTestExecuted = true;
        } catch (Exception e) {
            out(e);
            isTestExecuted = false;
        }
        isTestPassed = success;
        if (isTestExecuted) {
            if (isTestPassed) {
                out("Test PASSED.");
            } else {
                throw new Exception("Test FAILED.");
            }
        } else {
            out("Test NOT FAILED");
        }
    }

    private static boolean doTest(int numIterations, boolean input) throws Exception {
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        MidiDevice outDevice = null;
        MidiDevice inDevice = null;
        for (int i = 0; i < infos.length; i++) {
            MidiDevice device = MidiSystem.getMidiDevice(infos[i]);
            if (! (device instanceof Sequencer) &&
                ! (device instanceof Synthesizer)) {
                if (device.getMaxReceivers() != 0) {
                    outDevice = device;
                }
                if (device.getMaxTransmitters() != 0) {
                    inDevice = device;
                }
            }
        }
        MidiDevice testDevice = null;
        if (input) {
            testDevice = inDevice;
        } else {
            testDevice = outDevice;
        }
        if (testDevice == null) {
            out("Cannot test: device not available.");
            return true;
        }
        out("Using Device: " + testDevice);

        for (int i = 0; i < numIterations; i++) {
            out("@@@ ITERATION: " + i);
            testDevice.open();
            // This sleep ensures that the thread of MidiInDevice is started.
            sleep(50);
            testDevice.close();
        }
        return true;
    }

    private static void sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
        }
    }

    private static void out(Throwable t) {
        t.printStackTrace(System.out);
    }

    private static void out(String message) {
        System.out.println(message);
    }
}
