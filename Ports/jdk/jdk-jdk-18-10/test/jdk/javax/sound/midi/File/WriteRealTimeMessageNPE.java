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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

/**
 * @test
 * @bug 5048381
 * @summary NPE when writing a sequence with a realtime MIDI message
 */
public class WriteRealTimeMessageNPE {

    public static void main(String args[]) throws Exception {
        System.out.println("5048381: NullPointerException when saving a MIDI sequence");
        boolean npeThrown = false;
        boolean noEx = false;

        Sequence seq = new Sequence(Sequence.PPQ, 384, 1);
        Track t = seq.getTracks()[0];
        ShortMessage msg = new ShortMessage();
        msg.setMessage(0xF8, 0, 0);
        t.add(new MidiEvent(msg, 0));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            MidiSystem.write(seq, 0, out);
            noEx = true;
        } catch (NullPointerException npe) {
            npeThrown = true;
            System.out.println("## Failed: Threw unexpected NPE: "+npe);
            throw new Exception("Test FAILED!");
        } catch (Exception e) {
            System.out.println("Threw unexpected Exception: "+e);
            System.out.println("But at least did not throw NPE...");
        }
        if (noEx) {
            InputStream is = new ByteArrayInputStream(out.toByteArray());
            seq = MidiSystem.getSequence(is);
            System.out.println("Sequence has "+seq.getTracks().length+" tracks.");
            if (seq.getTracks().length > 0) {
                System.out.println("Track 0 has "+seq.getTracks()[0].size()+" events.");
            }
        }
        System.out.println("Test passed.");
    }
}
