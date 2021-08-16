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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;

/**
 * @test
 * @bug 4913027
 * @summary several Sequencer methods should specify behaviour on closed Sequencer
 */
public class SequencerState {

    private static boolean hasSequencer() {
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


    public static void main(String[] args) throws Exception {
        out("4913027: several Sequencer methods should should specify behaviour on closed Sequencer");
        if (hasSequencer()) {
            boolean passed = testAll();
            if (passed) {
                out("Test PASSED.");
            } else {
                throw new Exception("Test FAILED.");
            }
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
    private static boolean testSequencer(Sequencer seq) throws Exception {
        boolean result = true;

        out("testing: " + seq);
        /* test calls in closed state.
         */
        if (seq.isOpen()) {
            out("Sequencer is already open, cannot test!");
            return result;
        }

        try {
            seq.start();
            out("closed state: start() does not throw IllegalStateException!");
            result = false;
        } catch (IllegalStateException e) {
        }

        try {
            seq.stop();
            out("closed state: stop() does not throw IllegalStateException!");
            result = false;
        } catch (IllegalStateException e) {
        }

        try {
            seq.startRecording();
            out("closed state: startRecording() does not throw IllegalStateException!");
            result = false;
        } catch (IllegalStateException e) {
        }

        try {
            seq.stopRecording();
            out("closed state: stopRecording() does not throw IllegalStateException!");
            result = false;
        } catch (IllegalStateException e) {
        }

        Sequence sequence = createSequence();
        if (sequence == null) {
            out("created Sequence is null, cannot test!");
            return result;
        }
        try {
            seq.setSequence(sequence);
        } catch (IllegalStateException e) {
            out("closed state: setSequence(Sequence) throws IllegalStateException!");
            result = false;
        }

        InputStream inputStream = createSequenceInputStream();
        if (inputStream == null) {
            out("created InputStream is null, cannot test!");
            return result;
        }
        try {
            seq.setSequence(inputStream);
        } catch (IllegalStateException e) {
            out("closed state: setSequence(InputStream) throws IllegalStateException!");
            result = false;
        }

        try {
            seq.getSequence();
        } catch (IllegalStateException e) {
            out("closed state: getSequence() throws IllegalStateException!");
            result = false;
        }

        /* test calls in open state.
         */
        seq.open();
        if (! seq.isOpen()) {
            out("Sequencer is not open, cannot test!");
            return result;
        }

        try {
            seq.start();
        } catch (IllegalStateException e) {
            out("open state: start() throws IllegalStateException!");
            result = false;
        }

        try {
            seq.stop();
        } catch (IllegalStateException e) {
            out("open state: stop() throws IllegalStateException!");
            result = false;
        }

        try {
            seq.startRecording();
        } catch (IllegalStateException e) {
            out("open state: startRecording() throws IllegalStateException!");
            result = false;
        }

        try {
            seq.stopRecording();
        } catch (IllegalStateException e) {
            out("open state: stopRecording() throws IllegalStateException!");
            result = false;
        }

        sequence = createSequence();
        if (sequence == null) {
            out("created Sequence is null, cannot test!");
            return result;
        }
        try {
            seq.setSequence(sequence);
        } catch (IllegalStateException e) {
            out("open state: setSequence(Sequence) throws IllegalStateException!");
            result = false;
        }

        inputStream = createSequenceInputStream();
        if (inputStream == null) {
            out("created InputStream is null, cannot test!");
            return result;
        }
        try {
            seq.setSequence(inputStream);
        } catch (IllegalStateException e) {
            out("open state: setSequence(InputStream) throws IllegalStateException!");
            result = false;
        }

        try {
            seq.getSequence();
        } catch (IllegalStateException e) {
            out("open state: getSequence() throws IllegalStateException!");
            result = false;
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
        try {
            sequence = new Sequence(Sequence.PPQ, 480, 1);
        } catch (InvalidMidiDataException e) {
            // DO NOTHING
        }
        return sequence;
    }

    /**
     * Create a new InputStream containing a Sequence for testing.
     *
     * @return an InputStream containing a dummy Sequence, or null, if a problem
     *         occured while creating the InputStream
     */
    private static InputStream createSequenceInputStream() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Sequence sequence = createSequence();
        if (sequence == null) {
            return null;
        }
        try {
            MidiSystem.write(sequence, 0, baos);
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            return bais;
        } catch (IOException e) {
            return null;
        }
    }


    private static void out(String message) {
        System.out.println(message);
    }
}
