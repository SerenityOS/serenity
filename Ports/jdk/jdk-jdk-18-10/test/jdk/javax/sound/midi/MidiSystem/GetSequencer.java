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

import java.util.List;

import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Transmitter;

/**
 * @test
 * @bug 4931400
 * @summary Clarify default connections in default sequencer
 */
public class GetSequencer {

    static boolean failed = false;

    public static void main(String args[]) throws Exception {
        doTest(1);
        doTest(2);
        doTest(3);

        if (failed) throw new Exception("Test FAILED!");
        out("test OK");
    }

    public static void doTest(int mode) {
        Sequencer seq = null;
        boolean connected = false;

        try {
            switch (mode) {
            case 1:
                seq = MidiSystem.getSequencer();
                connected = true;
                break;
            case 2:
                seq = MidiSystem.getSequencer(false);
                connected = false;
                break;
            case 3:
                seq = MidiSystem.getSequencer(true);
                connected = true;
                break;
            }
            out("Testing Sequencer "+seq);
            if (connected) {
                out("  opened in connected mode.");
            } else {
                out("  opened in non-connected mode.");
            }
            System.out.println("  opening...");
            seq.open();
        } catch (MidiUnavailableException mue) {
            System.err.println("MidiUnavailableException was thrown: " + mue);
            System.err.println("  could not test this sequencer.");
            return;
        }

        try {
            List<Transmitter> transmitters = seq.getTransmitters();
            int size = transmitters.size();
            out("  transmitters.size()="+size);
            if (size != 1 && connected) {
                out("  should have 1 connection! Failed.");
                failed = true;
            }
            if (size != 0 && !connected) {
                out("  should have 0 connections! Failed.");
                failed = true;
            }
            out("  closing...");
            seq.close();
            transmitters = seq.getTransmitters();
            size = transmitters.size();
            out("  transmitters.size()="+size);
            if (size != 0) {
                out("  should have 0 connections! Failed.");
                failed = true;
            }

            out("  opening again...");
            seq.open();
            transmitters = seq.getTransmitters();
            size = transmitters.size();
            out("  transmitters.size()="+size);
            if (size != 1 && connected) {
                out("  should have 1 connection! Failed.");
                failed = true;
            }
            if (size != 0 && !connected) {
                out("  should have 0 connections! Failed.");
                failed = true;
            }
        } catch (Exception e) {
            System.err.println("  unexpectedException was thrown: " + e);
            System.err.println("  causes this test to FAIL.");
            failed = true;
        }
        seq.close();
    }

    static void out(String s) {
        System.out.println(s);
    }
}
