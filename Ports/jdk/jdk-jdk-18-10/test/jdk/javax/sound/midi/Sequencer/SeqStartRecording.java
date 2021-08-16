/*
 * Copyright (c) 2004, 2017, Oracle and/or its affiliates. All rights reserved.
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
import javax.sound.midi.Sequencer;

/**
 * @test
 * @bug 5001943
 * @summary Sequencer.startRecording throws unexpected NPE
 */
public class SeqStartRecording {

    public static void main(String argv[]) {
        Sequencer seq = null;
        try {
            seq = MidiSystem.getSequencer();
            seq.open();
        } catch (final MidiUnavailableException ignored) {
            // the test is not applicable
            return;
        }
        try {
            seq.startRecording();
            System.out.println("Test passed.");
        } catch (NullPointerException npe) {
            System.out.println("Caught NPE: "+npe);
            npe.printStackTrace();
            throw new RuntimeException("Test FAILED!");
        } catch (Exception e) {
            System.out.println("Unexpected Exception: "+e);
            e.printStackTrace();
            System.out.println("Test NOT failed.");
        } finally {
            seq.close();
        }
    }
}
