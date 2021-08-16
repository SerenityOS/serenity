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
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

/**
 * @test
 * @bug 4204105
 * @summary RFE: add loop() method(s) to Sequencer
 * @key intermittent
 */
public class Looping {

    public static void main(String[] args) throws Exception {
        out("4204105: RFE: add loop() method(s) to Sequencer");
        boolean passed = testAll();
        if (passed) {
            out("Test PASSED.");
        } else {
            throw new Exception("Test FAILED.");
        }
    }

    /**
     * Execute the test on all available Sequencers.
     *
     * @return true if the test passed for all Sequencers, false otherwise
     */
    private static boolean testAll() throws Exception {
        boolean result = true;
        MidiDevice.Info[] devices = MidiSystem.getMidiDeviceInfo();
        for (int i = 0; i < devices.length; i++) {
            MidiDevice device = MidiSystem.getMidiDevice(devices[i]);
            if (device instanceof Sequencer) {
                result &= testSequencer((Sequencer) device);
            }
        }
        return result;
    }

    /**
     * Execute the test on the passed Sequencer.
     *
     * @return true if the test is passed this Sequencer, false otherwise
     */
    private static boolean testSequencer(Sequencer seq) throws Exception{
        boolean result = true;
        out("testing: " + seq);

        result &= testGetSet(seq);

        seq.setSequence(createSequence());

        result &= testGetSet(seq);

        result &= testPlay(seq);

        return result;
    }

    private static boolean testGetSet(Sequencer seq) {
        boolean result = true;
        Sequence sequence = seq.getSequence();
        boolean isSequenceLoaded = (sequence != null);

        out("TestGetSet");

        try {
            if (seq.getLoopStartPoint() != 0) {
                out("start point", isSequenceLoaded,
                    "isn't 0!");
                result = false;
            }
        } catch (IllegalArgumentException iae) {
            if (!isSequenceLoaded) {
                out("Caught permissable IllegalArgumentException:");
            } else {
                out("Threw unacceptable IllegalArgumentException! FAILED");
                result = false;
            }
            out(iae.toString());
        }

        if (seq.getLoopEndPoint() != -1) {
            out("end point", isSequenceLoaded,
                "isn't -1!");
            result = false;
        }

        try {
            seq.setLoopStartPoint(25);
            if (seq.getLoopStartPoint() != 25) {
                out("setLoopStartPoint()", isSequenceLoaded,
                    "doesn't set the start point correctly!");
                result = false;
            }
        } catch (IllegalArgumentException iae) {
            if (!isSequenceLoaded) {
                out("Caught permissable IllegalArgumentException:");
            } else {
                out("Threw unacceptable IllegalArgumentException! FAILED");
                result = false;
            }
            out(iae.toString());
        }

        try {
            seq.setLoopEndPoint(26);
            if (seq.getLoopEndPoint() != 26) {
                out("setLoopEndPoint()", isSequenceLoaded,
                    "doesn't set the end point correctly!");
                result = false;
            }
        } catch (IllegalArgumentException iae) {
            if (!isSequenceLoaded) {
                out("Caught permissable IllegalArgumentException:");
            } else {
                out("Threw unacceptable IllegalArgumentException! FAILED");
                result = false;
            }
            out(iae.toString());
        }

        try {
            seq.setLoopStartPoint(0);
            if (seq.getLoopStartPoint() != 0) {
                out("setLoopStartPoint()", isSequenceLoaded,
                    "doesn't set the start point correctly!");
                result = false;
            }
        } catch (IllegalArgumentException iae) {
            if (!isSequenceLoaded) {
                out("Caught permissable IllegalArgumentException:");
            } else {
                out("Threw unacceptable IllegalArgumentException! FAILED");
                result = false;
            }
            out(iae.toString());
        }

        if (isSequenceLoaded) {
            seq.setLoopEndPoint(sequence.getTickLength());
            if (seq.getLoopEndPoint() != sequence.getTickLength()) {
                out("setLoopEndPoint()", isSequenceLoaded,
                    "doesn't set the end point correctly!");
                result = false;
            }
        } else {
            // fails
            seq.setLoopEndPoint(-1);
            if (seq.getLoopEndPoint() != -1) {
                out("setLoopEndPoint()", isSequenceLoaded,
                    "doesn't set the end point correctly!");
                result = false;
            }
        }

        if (seq.getLoopCount() != 0) {
            out("loop count", isSequenceLoaded,
                "isn't 0!");
            result = false;
        }

        seq.setLoopCount(1001);
        if (seq.getLoopCount() != 1001) {
            out("setLoopCount()", isSequenceLoaded,
                "doesn't set the loop count correctly!");
            result = false;
        }

        seq.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
        if (seq.getLoopCount() != Sequencer.LOOP_CONTINUOUSLY) {
            out("setLoopCount(Sequencer.LOOP_CONTINUOUSLY)", isSequenceLoaded,
                "doesn't set the loop count correctly!");
            result = false;
        }

        try {
            seq.setLoopCount(-55);
            out("setLoopCount()", isSequenceLoaded,
                "doesn't throw IllegalArgumentException on illegal value!");
            result = false;
        } catch (IllegalArgumentException e) {
            // EXCEPTION IS EXPECTED
            out("Caught permissable IAE");
        }

        seq.setLoopCount(0);
        if (seq.getLoopCount() != 0) {
            out("setLoopCount()", isSequenceLoaded,
                "doesn't set the loop count correctly!");
            result = false;
        }

        return result;
    }

    private static boolean testPlay(Sequencer seq) {
        boolean result = true;
        long stopTime;

        out("TestPlay");

        TestMetaEventListener listener = new TestMetaEventListener();
        seq.addMetaEventListener(listener);
        long startTime = System.currentTimeMillis();
        try {
            seq.open();
            out("Playing sequence, length="+(seq.getMicrosecondLength()/1000)+"millis");
            seq.start();
            while (true) {
                stopTime = listener.getStopTime();
                if (stopTime != 0) {
                    break;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
            }
            long measuredDuration = stopTime - startTime;
            out("play duration (us): " + measuredDuration);
        } catch (Exception e) {
            out("test not executed; exception:");
            e.printStackTrace();
        }
        seq.close();
        return result;
    }

    /**
     * Create a new Sequence for testing.
     *
     * @return a dummy Sequence, or null, if a problem occured while creating
     *         the Sequence
     */
    private static Sequence createSequence() {
        Sequence sequence = null;
        int lengthInSeconds = 2;
        long lengthInMicroseconds = lengthInSeconds * 1000000;
        int resolution = 480;
        long lengthInTicks = (lengthInMicroseconds * 120 * resolution) / 60000000l;
        out("length in ticks: " + lengthInTicks);
        try {
            sequence = new Sequence(Sequence.PPQ, resolution, 1);
            Track track = sequence.createTrack();
            ShortMessage mm = new ShortMessage();
            mm.setMessage(0xF6, 0, 0);
            MidiEvent me = new MidiEvent(mm, lengthInTicks);
            track.add(me);
        } catch (InvalidMidiDataException e) {
            // DO NOTHING
        }
        out("sequence length (ticks): " + sequence.getTickLength());
        out("sequence length (us): " + sequence.getMicrosecondLength());
        return sequence;
    }


    private static void out(String m1, boolean isSequenceLoaded, String m2) {
        out(m1 + (isSequenceLoaded ? " with Sequence " : " without Sequence ") + m2);
    }

    private static void out(String message) {
        System.out.println(message);
    }

    private static class TestMetaEventListener implements MetaEventListener {
        private long stopTime;


        public void meta(MetaMessage m) {
            System.out.print("  Got MetaMessage: ");
            if (m.getType() == 47) {
                stopTime = System.currentTimeMillis();
                System.out.println(" End Of Track -- OK");
            } else {
                System.out.println(" unknown. Ignored.");
            }
        }

        public long getStopTime() {
            return stopTime;
        }
    }
}
