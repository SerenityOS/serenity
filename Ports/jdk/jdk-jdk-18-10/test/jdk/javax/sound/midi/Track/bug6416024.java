/*
 * Copyright (c) 2006, 2016, Oracle and/or its affiliates. All rights reserved.
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
import javax.sound.midi.Track;

/**
 * @test
 * @bug 6416024
 * @summary Tests that sequence correctly handle removing of EndOfTrack event
 * @run main bug6416024
 */
public class bug6416024 {

    boolean test() {
        Sequence sequence = null;
        Track track = null;
        MidiEvent event = null;

        log("creating sequence...");
        try {
            sequence = new Sequence(Sequence.PPQ, 10);
            log("  - OK: " + sequence);
        } catch(InvalidMidiDataException e ) {
            log("  - FAILED: got exception");
            e.printStackTrace(System.out);
            return false;
        }

        log("creating track...");
        track = sequence.createTrack();
        log("  - OK: " + track);
        log("initial track size=" + track.size());

        log("removing all track events...");
        while (track.size() > 0) {
            try {
                event = track.get(0);
                log("  ..removing event " + event);
                track.remove(event);
                log("    - OK, track size=" + track.size());
            } catch (Exception e) {
                log("  - FAILED: got exception");
                e.printStackTrace(System.out);
                return false;
            }
        }

        MetaMessage newMsg = new MetaMessage();
        MidiEvent newEvent = new MidiEvent(newMsg, 10);
        log("adding new event...");
        try {
            if (!track.add(newEvent)) {
                log("event hasn't been added");
                return false;
            }
            log("    - OK, track size=" + track.size());
        } catch (Exception e) {
            log("  - FAILED: got exception");
            e.printStackTrace(System.out);
            return false;
        }

        return true;
    }

    public static void main(String args[]) throws Exception {
        bug6416024 This = new bug6416024();
        if (This.test()) {
            log("Test passed sucessfully.");
        } else {
            log("Test FAILED!");
            delay(1000);
            throw new RuntimeException("Test failed!");
        }
    }

    // helper routines
    static long startTime = currentTimeMillis();
    static long currentTimeMillis() {
        //return System.nanoTime() / 1000000L;
        return System.currentTimeMillis();
    }
    static void log(String s) {
        long time = currentTimeMillis() - startTime;
        long ms = time % 1000;
        time /= 1000;
        long sec = time % 60;
        time /= 60;
        long min = time % 60;
        time /= 60;
        System.out.println(""
                + (time < 10 ? "0" : "") + time
                + ":" + (min < 10 ? "0" : "") + min
                + ":" + (sec < 10 ? "0" : "") + sec
                + "." + (ms < 10 ? "00" : (ms < 100 ? "0" : "")) + ms
                + " (" + Thread.currentThread().getName() + ") " + s);
    }
    static void delay(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {}
    }
}
