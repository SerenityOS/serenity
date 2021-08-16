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
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Transmitter;

/**
 * @test
 * @bug 4616517
 * @summary Receiver.send() does not work properly
 */
public class ReceiverTransmitterAvailable {

    private static boolean isTestExecuted;
    private static boolean isTestPassed;

    public static void main(String[] args) throws Exception {
        out("#4616517: Receiver.send() does not work properly");
        doAllTests();
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

    private static void doAllTests() {
        boolean problemOccured = false;
        boolean succeeded = true;
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        for (int i = 0; i < infos.length; i++) {
            MidiDevice device = null;
            try {
                device = MidiSystem.getMidiDevice(infos[i]);
                succeeded &= doTest(device);
            } catch (MidiUnavailableException e) {
                out("exception occured; cannot test");
                problemOccured = true;
            }
        }
        if (infos.length == 0) {
            out("Soundcard does not exist or sound drivers not installed!");
            out("This test requires sound drivers for execution.");
        }
        isTestExecuted = !problemOccured;
        isTestPassed = succeeded;
    }

    private static boolean doTest(MidiDevice device) {
        boolean succeeded = true;
        out("Testing: " + device);
        boolean expectingReceivers = (device.getMaxReceivers() != 0);
        boolean expectingTransmitters = (device.getMaxTransmitters() != 0);
        try {
            Receiver rec = device.getReceiver();
            rec.close();
            if (! expectingReceivers) {
                out("no exception on getting Receiver");
                succeeded = false;
            }
        } catch (MidiUnavailableException e) {
            if (expectingReceivers) {
                out("Exception on getting Receiver: " + e);
                succeeded = false;
            }
        }
        try {
            Transmitter trans = device.getTransmitter();
            trans.close();
            if (! expectingTransmitters) {
                out("no exception on getting Transmitter");
                succeeded = false;
            }
        } catch (MidiUnavailableException e) {
            if (expectingTransmitters) {
                out("Exception on getting Transmitter: " + e);
                succeeded = false;
            }
        }
        return succeeded;
    }

    private static void out(String message) {
        System.out.println(message);
    }
}
