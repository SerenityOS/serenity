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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;

/**
 * @test
 * @bug 4713900
 * @summary default Sequencer allows to set Mute for invalid track
 */
public class SequencerSetMuteSolo {

    public static void main(String args[]) throws Exception {
        if (!hasSequencer()) {
            return;
        }

        //printMidiFile(args[0]);

        boolean failed = false;
        Sequencer seq = null;
        Sequence midiData = getSequence();
        int numTracks = midiData.getTracks().length;
        int totalNumberOfSequencers = 0;
        int totalNumberOfTestedSequencers = 0;

        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        for (int device=0; device<infos.length; device++) {
            //seq = MidiSystem.getSequencer();
            MidiDevice dev = MidiSystem.getMidiDevice(infos[device]);
            if (dev instanceof Sequencer) {
                seq = (Sequencer) dev;
                totalNumberOfSequencers++;
                System.out.println("Opening sequencer "+infos[device]);
                try {
                    seq.open();
                    try {
                        seq.setSequence(midiData);
                        System.err.println("Number of tracks: " + numTracks);
                        System.err.println("TrackMute["+numTracks+"] state was: " + seq.getTrackMute(numTracks));
                        System.err.println("  setting to muted.");
                        seq.setTrackMute(numTracks, true);
                        System.err.println("  TrackMute["+numTracks+"] is now: " + seq.getTrackMute(numTracks));
                        if (seq.getTrackMute(numTracks)) {
                            failed = true;
                        }
                        System.err.println("TrackSolo["+numTracks+"] state was: " + seq.getTrackSolo(numTracks));
                        System.err.println("  setting to solo.");
                        seq.setTrackSolo(numTracks, true);
                        System.err.println("  TrackSolo["+numTracks+"] is now: " + seq.getTrackSolo(numTracks));
                        if (seq.getTrackSolo(numTracks)) {
                            failed = true;
                        }
                        totalNumberOfTestedSequencers++;
                    } finally {
                        if (seq != null) {
                            seq.close();
                            seq = null;
                        }
                    }
                } catch (MidiUnavailableException mue) {
                    System.err.println("MidiUnavailableException was thrown: " + mue);
                    System.err.println("could not test this sequencer.");
                    return;
                }
            }
        }
        if (totalNumberOfSequencers == 0) {
            System.out.println("No sequencers installed!");
            failed = true;
        }
        if (totalNumberOfTestedSequencers == 0) {
            System.out.println("Could not test any sequencers!");
            failed = true;
        }
        if( failed ) {
            throw new Exception("FAILED");
        } else {
            System.out.println("test OK");
        }
    }

    public static String getString(byte b) {
        //String res = Integer.toHexString(b & 0xFF).toUpperCase();
        //while (res.length()<2) res="0"+res;
        //return res;
        return String.valueOf(b);
    }


    public static void printMidiFile(String filename) throws Exception {
        File file = new File(filename);
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        String s = "";
        for (int i=0; i<data.length; i++) {
                s+=getString(data[i])+", ";
                if (s.length()>72) {
                        System.out.println(s);
                        s="";
                }
        }
        System.out.println(s);
    }

    public static Sequence getSequence() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(pitchbend);
        Sequence seq = MidiSystem.getSequence(bais);
        return seq;
    }

    public static byte[] pitchbend = {
        77, 84, 104, 100, 0, 0, 0, 6, 0, 1, 0, 2, 0, 120, 77, 84, 114, 107, 0, 0,
        0, 27, 0, -1, 3, 19, 77, 73, 68, 73, 32, 116, 101, 115, 116, 32, 45, 32,
        116, 114, 97, 99, 107, 32, 48, 0, -1, 47, 0, 77, 84, 114, 107, 0, 0, 0, -44,
        0, -1, 3, 19, 77, 73, 68, 73, 32, 116, 101, 115, 116, 32, 45, 32, 116, 114,
        97, 99, 107, 32, 49, 0, -64, 30, 0, -112, 68, 126, 0, -32, 6, 67, 0, 14,
        71, 0, 20, 74, 0, 26, 77, 0, 32, 80, 0, 42, 85, 6, 50, 89, 6, 56, 92, 5,
        66, 97, 6, 74, 101, 6, 80, 104, 11, 84, 106, 20, 76, 102, 6, 70, 99, 5, 60,
        94, 6, 52, 90, 5, 44, 86, 4, 34, 81, 5, 26, 77, 5, 20, 74, 6, 10, 69, 5,
        2, 65, 7, 0, 64, 42, -112, 66, 123, 11, 68, 0, 72, 63, 126, 4, 66, 0, 43,
        -32, 0, 63, 6, 0, 60, 7, 0, 56, 6, 0, 53, 5, 0, 49, 5, 0, 43, 4, 0, 37, 3,
        0, 30, 3, 0, 25, 3, 0, 19, 3, 0, 13, 4, 0, 8, 4, 0, 2, 4, 0, 0, 70, 0, 3,
        5, 0, 9, 3, 0, 14, 7, 0, 16, 25, 0, 21, 5, 0, 25, 7, 0, 28, 5, 0, 32, 5,
        0, 36, 5, 0, 41, 6, 0, 46, 5, 0, 50, 5, 0, 53, 4, 0, 58, 7, 0, 61, 7, 0,
        64, 117, -112, 63, 0, 0, -1, 47, 0
    };

    static boolean hasSequencer() {
        try {
            Sequencer seq = MidiSystem.getSequencer();
            if (seq != null) {
                seq.open();
                seq.close();
                return true;
            }
        } catch (Exception e) {}
        System.out.println("No sequencer available! Cannot execute test.");
        return false;
    }
}
