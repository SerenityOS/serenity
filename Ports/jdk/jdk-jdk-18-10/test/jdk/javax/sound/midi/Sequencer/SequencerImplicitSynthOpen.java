/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 6660470
 * @summary Tests that sequencer correctly opens/closes (implicitly) devices
 * @author Alex Menkov
 */

import java.util.List;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiDeviceReceiver;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Transmitter;

public class SequencerImplicitSynthOpen {

    static int TEST_COUNT   = 5;

    public static void main(String[] args) {
        try {
            log("getting sequencer...");
            Sequencer sequencer = MidiSystem.getSequencer();
            log("  - got " + getDeviceStr(sequencer));

            // obtain connected device (usually synthesizer)
            MidiDevice synth = getConnectedDevice(sequencer);
            if (synth == null) {
                log("could not get connected device, returning");
                return;
            }

            log("connected device: " + getDeviceStr(synth));

            int success = 0;
            for (int i=0; i<TEST_COUNT; i++) {
                if (test(sequencer)) {
                    success++;
                }
            }

            if (success != TEST_COUNT) {
                throw new RuntimeException("test FAILS");
            }
        } catch (MidiUnavailableException ex) {
            // this is not a failure
            log("Could not get Sequencer");
        }
        log("test PASSED.");
    }

    static boolean test(Sequencer sequencer) throws MidiUnavailableException {
        log("");
        log("opening sequencer...");
        sequencer.open();   // opens connected synthesizer implicitly
        MidiDevice synth = getConnectedDevice(sequencer);
        log("  connected device: " + getDeviceStr(synth));

        log("closing sequencer...");
        sequencer.close();  // closes the synth implicitly
        log("  synth is " + getDeviceStr(synth));
        MidiDevice synth2 = getConnectedDevice(sequencer);
        log("  currently connected device: " + getDeviceStr(synth2));

        if (synth != null && synth.isOpen()) {
            log("FAIL.");
            return false;
        }
        log("OK.");
        return true;
    }

    static MidiDevice getConnectedDevice(Sequencer sequencer) {
        List<Transmitter> trans = sequencer.getTransmitters();
        log("  sequencer has " + trans.size() + " opened transmitters:");
        for (Transmitter tr: trans) {
            Receiver r = tr.getReceiver();
            log("    " + getClassStr(tr) + " connected to " + getClassStr(r));
            if (r instanceof MidiDeviceReceiver) {
                MidiDeviceReceiver recv = (MidiDeviceReceiver)r;
                MidiDevice dev = recv.getMidiDevice();
                log("      - receiver of " + getClassStr(dev));
                return dev;
            } else {
                log("      - does NOT implement MidiDeviceReceiver");
            }
        }
        return null;
    }

    static String getClassStr(Object o) {
        if (o == null) {
            return "<null>";
        }
        return o.getClass().getName();
    }

    static String getDeviceStr(MidiDevice dev) {
        if (dev == null) {
            return "NULL";
        }
        return getClassStr(dev) + ", " + (dev.isOpen() ? "OPENED" : "CLOSED");
    }

    static void log(String s) {
        System.out.println(s);
    }

}
