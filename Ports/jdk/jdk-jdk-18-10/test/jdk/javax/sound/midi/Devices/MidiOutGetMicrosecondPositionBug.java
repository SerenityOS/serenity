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
 * @bug 4903786
 * @summary MIDI OUT does not implement getMicrosecondPosition() consistently
 */
public class MidiOutGetMicrosecondPositionBug {
    static int successfulTests = 0;

    private static void testDevice(MidiDevice device) throws Exception {
        boolean timestampsAvailable = false;
        boolean timestampPrecisionOk = false;
        try {
            // expected behaviour if not opened?
            device.open();
            /* First, we're testing if timestamps are provided at all.
               Returning -1 (unsupported), while allowed by the API
               specification, is not sufficient to pass this test. */
            long timestamp = device.getMicrosecondPosition();
            timestampsAvailable = (timestamp != -1);

            /* Then, we're testing the precision. Note that the system time
               is measured in milliseconds, while the device time is measured
               in microseconds. */

            long systemTime1 = System.currentTimeMillis();
            long deviceTime1 = device.getMicrosecondPosition();
            // rest for 5 seconds
            Thread.sleep(5000);
            long systemTime2 = System.currentTimeMillis();
            long deviceTime2 = device.getMicrosecondPosition();

            // now both period measurements are calculated in milliseconds.
            long systemDuration = systemTime2 - systemTime1;
            long deviceDuration = (deviceTime2 - deviceTime1) / 1000;
            long delta = Math.abs(systemDuration - deviceDuration);
            // a deviation of 0.5 seconds (= 500 ms) is allowed.
            timestampPrecisionOk = (delta <= 500);
        } catch (Throwable t) {
            System.out.println("  - Caught exception. Not failed.");
            System.out.println("  - " + t.toString());
            return;
        } finally {
            device.close();
        }
        if (! timestampsAvailable) {
            throw new Exception("timestamps are not supported");
        }
        if (! timestampPrecisionOk) {
            throw new Exception("device timer not precise enough");
        }
        successfulTests++;
    }

    private static void doAll() throws Exception {
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        for (int i=0; i < infos.length; i++) {
            MidiDevice device = MidiSystem.getMidiDevice(infos[i]);
            if ((! (device instanceof Sequencer)) &&
                (! (device instanceof Synthesizer)) &&
                (device.getMaxReceivers() > 0 || device.getMaxReceivers() == -1)) {

                System.out.println("--------------");
                System.out.println("Testing MIDI device: " + infos[i]);
                testDevice(device);
            }
            if (infos.length==0) {
                System.out.println("No MIDI devices available!");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (!isMidiInstalled()) {
            return;
        }
        doAll();
        if (successfulTests==0) {
            System.out.println("Could not execute any of the tests. Test NOT failed.");
        } else {
            System.out.println("Test PASSED.");
        }
    }

    /**
     * Returns true if at least one MIDI (port) device is correctly installed on
     * the system.
     */
    public static boolean isMidiInstalled() {
        boolean result = false;
        MidiDevice.Info[] devices = MidiSystem.getMidiDeviceInfo();
        for (int i = 0; i < devices.length; i++) {
            try {
                MidiDevice device = MidiSystem.getMidiDevice(devices[i]);
                result = ! (device instanceof Sequencer) && ! (device instanceof Synthesizer);
            } catch (Exception e1) {
                System.err.println(e1);
            }
            if (result)
                break;
        }
        if (!result) {
            System.err.println("Soundcard does not exist or sound drivers not installed!");
            System.err.println("This test requires sound drivers for execution.");
        }
        return result;
    }
}
