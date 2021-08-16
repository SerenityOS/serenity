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

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

/**
 * @test
 * @bug 5048381
 * @summary Sequencer doesn't create distinct messages when recording events.
 */
public class SeqRecordDoesNotCopy {

    public static void main(String argv[]) {
        Sequencer s = null;
        try {
            s = MidiSystem.getSequencer();
            s.open();
        } catch (final MidiUnavailableException ignored) {
            // the test is not applicable
            return;
        }
        try {
            Sequence seq = new Sequence(Sequence.PPQ, 384, 2);
            s.setSequence(seq);
            Track t = seq.getTracks()[0];
            ShortMessage msg = new ShortMessage();
            msg.setMessage(0x90, 0x40, 0x7F);
            t.add(new MidiEvent(msg, 11000));
            msg.setMessage(0x90, 0x40, 0x00);
            t.add(new MidiEvent(msg, 12000));
            t = seq.getTracks()[1];
            s.recordEnable(t, -1);
            System.out.println("Started recording...");
            s.startRecording();
            Receiver r = s.getReceiver();
            Thread.sleep(100);
            // send a normal message
            System.out.println("Recording a normal NOTE ON message...");
            msg.setMessage(0x90, 0x40, 0x6F);
            r.send(msg, -1);
            Thread.sleep(100);
            // send a normal message
            System.out.println("Recording a normal NOTE OFF message...");
            msg.setMessage(0x90, 0x40, 0x00);
            r.send(msg, -1);
            Thread.sleep(100);
            s.stop();
            // now see if the messages were recorded
            System.out.println("Recorded messages:");
            int sameMessage = 0;
            for (int i = 0; i < t.size(); i++) {
                System.out.print(" "+(i+1)+". ");
                printEvent(t.get(i));
                if (t.get(i).getMessage() == msg) {
                    System.out.println("## Failed: Same Message reference!");
                    sameMessage++;
                }
            }
            if (sameMessage > 0) {
                System.out.println("## Failed: The same instance was recorded!");
                throw new Exception("Test FAILED!");
            }
            System.out.println("Did not detect any duplicate messages.");
            System.out.println("Test passed.");
        } catch (Exception e) {
            System.out.println("Unexpected Exception: "+e);
            //e.printStackTrace();
            throw new RuntimeException("Test FAILED!");
        } finally {
            s.close();
        }
    }
    public static void printEvent(MidiEvent event)
    {
        MidiMessage message = event.getMessage();
        long tick = event.getTick();
        byte[] data = message.getMessage();

        StringBuffer sb = new StringBuffer((data.length * 3) - 1);

        for (int i = 0; i < data.length; i++)
        {
                sb.append(toHexByteString(data[i]));
                if (i < data.length - 1) sb.append(' ');
        }
        System.out.printf("%5d: %s%n", tick, sb);
    }

    private static String toHexByteString(int n)
    {
        if (n < 0) n &= 0xFF;
        String s = Integer.toHexString(n).toUpperCase();
        if (s.length() == 1) s = '0' + s;
        return s;
    }
}
