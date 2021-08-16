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

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

/**
 * @test
 * @bug 4493775
 * @summary Sequncer method, setTickPosition(long) doesnot set the Tick position
 */
public class SetTickPosition {
        private static boolean testPassed = true;

        public void runTest()
        {
            Sequencer theSequencer = null;
            try
            {
                System.out.print("Getting Sequencer...");
                theSequencer = MidiSystem.getSequencer();
                System.out.println("got "+theSequencer);

                if(!(theSequencer.isOpen()))
                {
                    System.out.println("Opening Sequencer...");
                    theSequencer.open();

                    if(!(theSequencer.isOpen()))
                    {
                        System.out.println("Unable to open the Sequencer. Test NOT FAILED.");
                        return;
                    }
                }

                System.out.println("theSequencer is open!\n");

                System.out.println("Creating New Sequence...");
                Sequence theSequence = new Sequence(Sequence.PPQ, 120);

                System.out.println("Adding Track To Sequence...");
                Track theTrack = theSequence.createTrack();

                int theChannel = 0;

                int theNote = 60;
                int theVelocity = 100;
                ShortMessage theShortMessage = new ShortMessage();

                for (int tick=0; tick<2000; tick+=120) {
                    //System.out.println("Adding NOTE_ON To Track At Tick: " + tick + "...\n");
                    theShortMessage.setMessage(ShortMessage.NOTE_ON, theChannel, theNote, theVelocity);
                    MidiEvent theMidiEvent = new MidiEvent(theShortMessage, tick);
                    theTrack.add(theMidiEvent);

                    //System.out.println("Adding NOTE_OFF To Track At Tick: " + (tick+60) + "...\n");
                    theShortMessage.setMessage(ShortMessage.NOTE_OFF, theChannel, theNote, theVelocity);
                    theMidiEvent = new MidiEvent(theShortMessage, tick+60);
                    theTrack.add(theMidiEvent);
                }
                theSequencer.setSequence(theSequence);

                float theTempoInBPM = 120;
                theSequencer.setTempoInBPM(theTempoInBPM);
                long theTickLengthOfSequence = theSequencer.getTickLength();
                System.out.println("Length Of Sequence In Ticks: " + theTickLengthOfSequence);
                System.out.println("Sequence resolution: " + theSequencer.getSequence().getResolution());

                theSequencer.start();
                for(long theTickPosition = 0; theTickPosition < theTickLengthOfSequence; theTickPosition += (theTickLengthOfSequence / 10))
                {
                    System.out.println("Now Setting Tick Position To: " + theTickPosition);
                    theSequencer.setTickPosition(theTickPosition);

                    long theCurrentTickPosition = theSequencer.getTickPosition();
                    long theCurrentMsPosition = (long) (theSequencer.getMicrosecondPosition()/1000);
                    System.out.println("IsRunning()=" + theSequencer.isRunning());
                    System.out.println("Now Current Tick Position Is: " + theCurrentTickPosition);
                    //System.out.println("Now Current micro Position Is: " + theCurrentMsPosition);
                    System.out.println("");

                    try {
                        Thread.sleep(800);
                    } catch (InterruptedException ie) {}

                    // last time, set tick pos to 0
                    if (theTickPosition>0 && theTickPosition<(theTickLengthOfSequence / 10)) {
                        theTickPosition=(theTickLengthOfSequence / 10);
                    }

                    // 30 = 1/4 * 120, the resolution of the sequence
                    if(Math.abs(theCurrentTickPosition - theTickPosition) > 30) {
                        System.out.println("theCurrentTickPosition != theTickPosition!");
                        testPassed = false;
                    }
                }

            }
            catch (Exception ex) { ex.printStackTrace(); }
            if (theSequencer != null) {
                theSequencer.close();
            }
            if (testPassed) {
                System.out.println("Test Passed.");
            }
        }

    public static void main(String[] args) throws Exception {
        SetTickPosition thisTest = new SetTickPosition();
        thisTest.runTest();
        if (!testPassed) {
            throw new Exception("Test FAILED");
        }
    }
}
