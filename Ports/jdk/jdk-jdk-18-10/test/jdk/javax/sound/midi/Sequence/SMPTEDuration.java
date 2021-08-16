/*
 * Copyright (c) 2002, 2016, Oracle and/or its affiliates. All rights reserved.
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
import javax.sound.midi.MidiEvent;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

/**
 * @test
 * @bug 4702328
 * @summary Wrong time in sequence for SMPTE based types
 */
public class SMPTEDuration {

    public static void main(String args[]) throws Exception {
         int[][] dataMes =  { {ShortMessage.NOTE_ON, 10, 0x24, 0x50} ,
                 { ShortMessage.NOTE_OFF, 10, 0x24, 0x44 },
                 { ShortMessage.NOTE_ON, 10, 0x24, 0x50 },
                 { ShortMessage.NOTE_ON, 10, 0x26, 0x50 },
                 { ShortMessage.NOTE_OFF, 10, 0x26, 0x53 } };
         long[] ticks = { 0, 68, 240, 240, 286};
         int res = 240;
         ShortMessage msg;
         Sequence midiData = null;
         Track track;
         boolean failed = false;


         try {
             midiData = new Sequence(Sequence.SMPTE_24 , res);
         } catch (InvalidMidiDataException invMidiEx) {
             invMidiEx.printStackTrace(System.out);
             System.out.println("Unexpected InvalidMidiDataException: "
                     + invMidiEx.getMessage());
             failed = true;
         }
         track = midiData.createTrack();
         for (int i = 0; i < dataMes.length; i++) {
             msg = new ShortMessage();
             try {
                 msg.setMessage(dataMes[i][0], dataMes[i][1], dataMes[i][2],
                         dataMes[i][3]);
             } catch (InvalidMidiDataException invMidiEx) {
                 invMidiEx.printStackTrace(System.out);
                 System.out.println("Unexpected InvalidMidiDataException: "
                         + invMidiEx.getMessage());
                 failed = true;
             }
             track.add(new MidiEvent(msg, ticks[i]));
         }
         //   lengthInMs = (tickLength*1000000)/(divType*Res)
         long micros = (long) ((midiData.getTickLength() * 1000000) / (res * Sequence.SMPTE_24));
         if (midiData.getMicrosecondLength() != micros) {
             failed = true;
             System.out.println("getMicrosecondLength() returns wrong length: "
                     + midiData.getMicrosecondLength());
             System.out.println("getMicrosecondLength() must return length: "
                     + micros);
         }
         if (midiData.getTickLength() != 286) {
             failed = true;
             System.out.println("getTickLength() returns wrong length: "
                     + midiData.getTickLength());
         }

         if( failed == true ) {
             throw new Exception("test failed");
         } else {
             System.out.println("Passed.");
         }
    }
}
