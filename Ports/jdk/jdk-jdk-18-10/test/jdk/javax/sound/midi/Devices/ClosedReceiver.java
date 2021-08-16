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

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;

/**
 * @test
 * @bug 4616517
 * @summary Receiver.send() does not work properly
 */
public class ClosedReceiver {

    public static void main(String[] args) throws Exception {
        out("#4616517: Receiver.send() does not work properly");
        if (!isMidiInstalled()) {
            out("Soundcard does not exist or sound drivers not installed!");
            out("This test requires sound drivers for execution.");
            return;
        }

        boolean passed = true;

        passed &= testReceiverSend();
        passed &= testClosedReceivers();
        if (passed) {
            out("Test PASSED.");
        } else {
            throw new Exception("Test FAILED.");
        }
    }

    /**
     * Execute Receiver.send() and expect that there is no exception.
     */
    private static boolean testReceiverSend() {
        boolean result = true;

        Receiver receiver;
        ShortMessage shMsg = new ShortMessage();

        try {
            receiver = MidiSystem.getReceiver();
            shMsg.setMessage(ShortMessage.NOTE_ON, 0,60, 93);
            try {
                receiver.send( shMsg, -1 );
            } catch(IllegalStateException ilEx) {
                ilEx.printStackTrace(System.out);
                out("IllegalStateException was thrown incorrectly!");
                result = false;
            }
            receiver.close();
        } catch(MidiUnavailableException e) {
            out("Midi unavailable, cannot test.");
        } catch(InvalidMidiDataException ine) {
            out("InvalidMidiDataException, cannot test.");
        }
        return result;
    }

    private static boolean testClosedReceivers() {
        boolean result = true;
        Receiver receiver;
        Synthesizer synt = null;

        // test Synthesizer's Receiver
        try {
            synt = MidiSystem.getSynthesizer();
            synt.open();
        } catch(MidiUnavailableException e) {
            out("Midi unavailable, cannot test.");
            return result;
        }
        try {
            receiver = synt.getReceiver();
        } catch (MidiUnavailableException e) {
            out("unable to get Receiver from synthesizer, cannot test.");
            return result;
        }
        result &= testClosedReceiver(receiver);
        synt.close();

        // test all MidiDevices' Receivers

        MidiDevice.Info[] devices = MidiSystem.getMidiDeviceInfo();
        for (int i = 0; i < devices.length; i++) {
            try {
                MidiDevice device = MidiSystem.getMidiDevice(devices[i]);
                if (device.getMaxReceivers() != 0) {
                    receiver = device.getReceiver();
                    result &= testClosedReceiver(receiver);
                }
            } catch (Exception e) {
                out(e);
                out("cannot test.");
                return result;
            }
        }
        return result;
    }

    /**
     * Execute send() on a closed Receivers and expect IllegalStateException.
     */
    private static boolean testClosedReceiver(Receiver receiver) {
        boolean result = true;
        out("testing Receiver: " + receiver);
        ShortMessage shMsg = new ShortMessage();
        try {
            shMsg.setMessage(ShortMessage.NOTE_ON, 0,60, 93);
        } catch(InvalidMidiDataException e) {
            out(e);
            out("unable to construct ShortMessage, cannot test.");
            return result;
        }

        // begin of test
        receiver.close();
        try {
            receiver.send( shMsg, -1 );
            out("IllegalStateException was not thrown "
                + "on Receiver.send()!");
            result = false;
        } catch(IllegalStateException e) {
            out("IllegalStateException was thrown. Ok.");
        }
        return result;
    }

    private static void out(Throwable t) {
        t.printStackTrace(System.out);
    }

    private static void out(String message) {
        System.out.println(message);
    }

    /**
     * Returns true if at least one MIDI (port) device is correctly installed on
     * the system.
     */
    private static boolean isMidiInstalled() {
        boolean result = false;
        MidiDevice.Info[] devices = MidiSystem.getMidiDeviceInfo();
        for (int i = 0; i < devices.length; i++) {
            try {
                MidiDevice device = MidiSystem.getMidiDevice(devices[i]);
                result = !(device instanceof Sequencer)
                        && !(device instanceof Synthesizer);
            } catch (Exception e1) {
                System.err.println(e1);
            }
            if (result)
                break;
        }
        return result;
    }
}
