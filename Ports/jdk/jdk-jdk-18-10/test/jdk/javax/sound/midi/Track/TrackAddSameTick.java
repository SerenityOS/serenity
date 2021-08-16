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
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

/**
 * @test
 * @bug 4941944
 * @summary Track may not have a determined order for inserting events at same
 *          tick time
 */
public class TrackAddSameTick {

    static boolean failed = false;
    static MidiEvent[] evs = new MidiEvent[10];

    public static void main(String argv[]) throws Exception {
        Sequence seq = new Sequence(Sequence.PPQ, 240);
        Track t = seq.createTrack();

        log("add 10 events in random order");
        t.add(createEvent(10, 5));
        t.add(createEvent(0, 0));
        t.add(createEvent(10, 6));
        t.add(createEvent(11, 8));
        t.add(createEvent(10, 7));
        t.add(createEvent(0, 1));
        t.add(createEvent(0, 2));
        t.add(createEvent(15, 9));
        t.add(createEvent(0, 3));
        t.add(createEvent(1, 4));

        // now compare the events.
        // The note param will tell us the
        // the expected position
        long lastTick = 0;
        for (int i = 0; i < t.size(); i++) {
            MidiEvent ev = t.get(i);
            if (ev.getMessage() instanceof ShortMessage) {
                ShortMessage msg = (ShortMessage) ev.getMessage();
                log(""+i+": ShortMessage at tick "+ev.getTick()
                    +" with expected position "+msg.getData1());
                if (ev.getTick() < lastTick) {
                    log("  FAILED: last tick is larger than this event's tick!");
                    failed = true;
                }
                if (i != msg.getData1()) {
                    log("  FAILED: Track did not order correctly.");
                    failed = true;
                }
            }
        }

        if (failed) throw new Exception("Test FAILED!");
        log("Test passed.");
    }

    public static MidiEvent createEvent(long tick, int expectedPos)
        throws Exception {
        ShortMessage msg = new ShortMessage();
        msg.setMessage(0x90, (int) expectedPos, 00);
        MidiEvent ev = new MidiEvent(msg, tick);
        return ev;
    }

    public static void log(String s) {
        System.out.println(s);
    }

    public static void log(Exception e) {
        //System.out.println(s);
        e.printStackTrace();
    }
}
