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
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

/**
 * @test
 * @bug 4427890
 * @run main/othervm TickLength
 * @summary Sequencer.getTickLength() and Sequence.getTickLength() report the
 *          wrong length
 */
public class TickLength implements MetaEventListener {
    private Sequence          theSequence;
    private Sequencer         theSequencer;

    public TickLength() {
     this.initMidiCompoments();
     System.out.println("Got Sequencer "+theSequencer);
     theSequence = this.generateSequence();
     try {
       theSequencer.setSequence(theSequence);
     }
     catch(Exception e) {
       System.out.println(this.getClass()+"\tCannot set sequence to sequencer ("+e+")");
       return;
     }
    }

    public  void start() {
     theSequencer.start();
    }

    /*
     instantiate the necessary midi components
    */
    private boolean initMidiCompoments() {


     try {
       theSequencer = MidiSystem.getSequencer();
     }
     catch(Exception e) {
       System.out.println(this.getClass()+"\tSequencer Device not supported"+e+")");
       return false;
     }

     try {
       theSequencer.open();
     }
     catch(Exception e) {
       System.out.println(this.getClass()+"Cannot open Sequencer Device");
       return false;
     }
     if(!theSequencer.addMetaEventListener(this)) {
       System.out.println(this.getClass()+"\tCould not register MetaEventListener - there will be problems with scrolling! ");
       return false;
     }
     return true;
    }

    static int lastTick = 0;

    private Sequence generateSequence() {
     MidiEvent dummyMidiEvent;
     ShortMessage dummyShortMessage;
     Sequence dummySequence        = null;
     Track[] allTracks ;
     Track theTrack;

     try {
       dummySequence = new Sequence(Sequence.PPQ,1500);
     }
     catch(InvalidMidiDataException e) {
       System.out.println("O o "+e);
     }

     dummySequence.createTrack();
     allTracks = dummySequence.getTracks();
     theTrack = allTracks[0];
    lastTick = 0;
     for(int i=0;i<20; i++) {
       theTrack.add(this.createShortMidiEvent(ShortMessage.NOTE_ON, 2, 30+i, 100,100+1000*i));
       theTrack.add(this.createMetaMidiEvent(1,"start",100+1000*i));
       lastTick = (1000*i)+600;
       theTrack.add(this.createShortMidiEvent(ShortMessage.NOTE_OFF, 2, 30+i, 100, lastTick));
       theTrack.add(this.createMetaMidiEvent(1,"end",lastTick));
     }

     return dummySequence;
    }

    /*
     A method to create a short midi event (sound)
    */

    public MidiEvent createShortMidiEvent(int theCommand, int theChannel, int theData1, int theData2, long theTime) {
     ShortMessage dummyShortMessage;
     MidiEvent    dummyMidiEvent;

     try {
       dummyShortMessage = new ShortMessage();
       dummyShortMessage.setMessage(theCommand, theChannel, theData1, theData2);
       dummyMidiEvent    = new MidiEvent(dummyShortMessage,theTime);
     }
     catch (Exception e) {
       System.out.println(this.getClass()+"\t"+e);
       return null;
     }

     return dummyMidiEvent;
    }

    /*
     A method to create a meta midi event (used in  meta() method)
    */
    public MidiEvent createMetaMidiEvent(int theType, String theData1, long theTime) {
     MetaMessage  dummyMetaMessage;
     MidiEvent    dummyMidiEvent;

     try {
       dummyMetaMessage = new MetaMessage();
       dummyMetaMessage.setMessage(theType, theData1.getBytes(), theData1.length());
       dummyMidiEvent    = new MidiEvent(dummyMetaMessage,theTime);
     }
     catch (Exception e) {
       System.out.println(e);
       return null;
     }

     return dummyMidiEvent;
    }

    /*
     the method is activated by each meta midi event
     it puts out the actual tick position, as well as the WRONG total tick length and the RIGHT
     tick length using the work around by dividing the total length by 64
    */
    public void meta(MetaMessage p1) {
     if(p1.getType() ==47) {
       return;
     }
     System.out.println("getTickPosition:\t"+theSequencer.getTickPosition()
         +"\t Sequencer.getTickLength:\t"+theSequencer.getTickLength()
         +"\tReal Length:\t"+lastTick
         +"\t Sequence.getTickLength:\t"+theSequence.getTickLength()
         //(theSequencer.getTickLength()/64));
         );
    }

    public void checkLengths() throws Exception {
        System.out.println("Sequencer.getTickLength() = "+theSequencer.getTickLength());
        System.out.println("Sequence.getTickLength() = "+theSequence.getTickLength());
        long diff = theSequencer.getTickLength() - theSequence.getTickLength();
        if (diff > 100 || diff < -100) {
                throw new Exception("Difference too large! Failed.");
        }
        System.out.println("Passed");
    }

    public static void main(String[] args) throws Exception {
        if (!hasSequencer()) {
            return;
        }
         TickLength tlt = new TickLength();
         //tlt.start();
         tlt.checkLengths();
    }

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
