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
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

/**
 * @test
 * @bug 4929955
 * @summary Sequence.getMicrosecondLength() returns wrong value
 */
public class GetMicrosecondLength {

    public static boolean failed = false;
    //private static Sequencer seq = null;

    public static void main(String[] args) throws Exception {
        /*
          try {
          seq = MidiSystem.getSequencer();
          } catch (Exception e) {
          e.printStackTrace();
          }
        */
        for (int sec = 1; sec < 10; sec += 4) {
            for (int tempo=0; tempo < 1000; tempo+=120) {
                for (int resolution=1; resolution < 480; ) {
                    testSequence(sec, tempo, resolution);
                    if (resolution == 1) {
                        resolution = 120;
                    } else {
                        resolution += 120;
                    }
                }
            }
        }
        if (failed) throw new Exception("Test FAILED!");
        out("Test Passed.");
    }

    /**
     * Create a new Sequence for testing.
     */
    private static void testSequence(int lengthInSeconds, int tempoInBPM, int resolution) {
        Sequence sequence = null;
        long lengthInMicroseconds = lengthInSeconds * 1000000;
        boolean createTempoEvent = true;
        if (tempoInBPM == 0) {
            tempoInBPM = 120;
            createTempoEvent = false;
            System.out.print("Creating sequence: "+lengthInSeconds+"sec, "
                             +"resolution="+resolution+" ticks/beat...");
        } else {
            System.out.print("Creating sequence: "+lengthInSeconds+"sec, "
                             +tempoInBPM+" beats/min, "
                             +"resolution="+resolution+" ticks/beat...");
        }
        //long lengthInTicks = (lengthInMicroseconds * resolution) / tempoInBPM;
        long lengthInTicks = (lengthInMicroseconds * tempoInBPM * resolution) / 60000000l;
        //out("expected length in ticks: " + lengthInTicks);
        try {
            sequence = new Sequence(Sequence.PPQ, resolution);
            Track track = sequence.createTrack();
            if (createTempoEvent) {
                int tempoInMPQ = (int) (60000000l / tempoInBPM);
                MetaMessage tm = new MetaMessage();
                byte[] msg = new byte[3];
                msg[0] = (byte) (tempoInMPQ >> 16);
                msg[1] = (byte) ((tempoInMPQ >> 8) & 0xFF);
                msg[2] = (byte) (tempoInMPQ & 0xFF);

                tm.setMessage(0x51 /* Meta Tempo */, msg, msg.length);
                track.add(new MidiEvent(tm, 0));
                //out("regtest: tempoInMPQ="+tempoInMPQ);
                //out("Added tempo event: new size="+track.size());
            }
            ShortMessage mm = new ShortMessage();
            mm.setMessage(0xF6, 0, 0);
            MidiEvent me = new MidiEvent(mm, lengthInTicks);
            track.add(me);
            //out("Added realtime event: new size="+track.size());
        } catch (InvalidMidiDataException e) {
            out(e);
        }
        boolean thisFailed = false;
        long actualLengthInTicks = sequence.getTickLength();
        // allow +/- 5%
        if (Math.abs(actualLengthInTicks - lengthInTicks) > lengthInTicks / 20) {
            out("FAILED:");
            out("  expected length in ticks: " + lengthInTicks);
            out("  actual length in ticks  : " + actualLengthInTicks);
            thisFailed = true;
        }
        long actualLengthInUs = sequence.getMicrosecondLength();
        // allow +/- 5%
        if (Math.abs(actualLengthInUs - lengthInMicroseconds) > lengthInMicroseconds / 20) {
            if (!thisFailed) {
                out("FAILED:");
            }
            out("  expected length in microsecs: " + lengthInMicroseconds);
            out("  actual length in microsecs  : " + actualLengthInUs);
            thisFailed = true;
        }
        if (!thisFailed) {
            out("OK");
        }
        /*if (seq != null) {
          try {
          seq.setSequence(sequence);
          out("Sequencer tempo="+seq.getTempoInBPM());
          } catch (Exception e) {
          e.printStackTrace();
          }
          }
        */
        failed |= thisFailed;
    }


    private static void out(Throwable t) {
        t.printStackTrace(System.out);
    }

    private static void out(String message) {
        System.out.println(message);
    }
}
